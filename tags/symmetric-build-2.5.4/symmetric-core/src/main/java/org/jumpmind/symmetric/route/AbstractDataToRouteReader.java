/*
 * Licensed to JumpMind Inc under one or more contributor 
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding 
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU Lesser General Public License (the
 * "License"); you may not use this file except in compliance
 * with the License. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see           
 * <http://www.gnu.org/licenses/>.
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */
package org.jumpmind.symmetric.route;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.common.logging.ILog;
import org.jumpmind.symmetric.db.IDbDialect;
import org.jumpmind.symmetric.model.Channel;
import org.jumpmind.symmetric.model.Data;
import org.jumpmind.symmetric.service.IDataService;
import org.jumpmind.symmetric.service.ISqlProvider;
import org.jumpmind.symmetric.util.AppUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * This class is responsible for reading data for the purpose of routing. It
 * reads ahead and tries to keep a blocking queue populated for other threads to
 * process.
 */
abstract public class AbstractDataToRouteReader implements IDataToRouteReader {

    protected ILog log;

    protected BlockingQueue<Data> dataQueue;

    protected ISqlProvider sqlProvider;

    protected JdbcTemplate jdbcTemplate;

    protected ChannelRouterContext context;

    protected IDataService dataService;

    protected boolean reading = true;

    protected IDbDialect dbDialect;

    public AbstractDataToRouteReader(ILog log, ISqlProvider sqlProvider, ChannelRouterContext context,
            IDataService dataService) {
        this.log = log;
        this.jdbcTemplate = dataService != null ? dataService.getJdbcTemplate() : null;
        this.dbDialect = dataService != null ? dataService.getDbDialect() : null;
        this.dataQueue = new LinkedBlockingQueue<Data>(dbDialect != null ? dbDialect.getRouterDataPeekAheadCount() : 1000);
        this.sqlProvider = sqlProvider;
        this.context = context;
        this.dataService = dataService;
    }

    public Data take() {
        Data data = null;
        try {
            int queryTimeout = jdbcTemplate.getQueryTimeout();
            data = dataQueue.poll(queryTimeout < 3600 ? 3600 : queryTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn(e);
        }

        if (data == null) {
            log.warn("RouterDataReaderNotResponding");
        } else if (data instanceof EOD) {
            data = null;
        } 
        return data;
    }

    /**
     * Start out by selecting only the rows that need routed.
     */
    protected PreparedStatement prepareStatment(Connection c) throws SQLException {
        throw new NotImplementedException();
    }

    protected String getSql(String sqlName, Channel channel) {
        String select = sqlProvider.getSql(sqlName);
        if (!channel.isUseOldDataToRoute()) {
            select = select.replace("d.old_data", "''");
        }
        if (!channel.isUseRowDataToRoute()) {
            select = select.replace("d.row_data", "''");
        }
        if (!channel.isUsePkDataToRoute()) {
            select = select.replace("d.pk_data", "''");
        }
        return dbDialect == null ? select : dbDialect.massageDataExtractionSql(select, channel);
    }

    public void run() {
        try {
            execute();
        } catch (Throwable ex) {
            log.error(ex);
        }
    }

    protected void execute() {
        jdbcTemplate.execute(new ConnectionCallback<Integer>() {
            public Integer doInConnection(Connection c) throws SQLException, DataAccessException {
                int dataCount = 0;
                PreparedStatement ps = null;
                ResultSet rs = null;
                boolean autoCommit = c.getAutoCommit();
                try {
                    if (dbDialect.requiresAutoCommitFalseToSetFetchSize()) {
                        c.setAutoCommit(false);
                    }
                    String channelId = context.getChannel().getChannelId();
                    ps = prepareStatment(c);
                    long ts = System.currentTimeMillis();
                    rs = ps.executeQuery();
                    long executeTimeInMs = System.currentTimeMillis() - ts;
                    context.incrementStat(executeTimeInMs, ChannelRouterContext.STAT_QUERY_TIME_MS);
                    if (executeTimeInMs > Constants.LONG_OPERATION_THRESHOLD) {
                        log.warn("RoutedDataSelectedInTime", executeTimeInMs, channelId);
                    } else if (log.isDebugEnabled()) {
                        log.debug("RoutedDataSelectedInTime", executeTimeInMs, channelId);
                    }

                    int maxQueueSize = dbDialect.getRouterDataPeekAheadCount();
                    int toRead = maxQueueSize - dataQueue.size();
                    List<Data> memQueue = new ArrayList<Data>(toRead);
                    ts = System.currentTimeMillis();
                    while (rs.next() && reading) {

                        if (StringUtils.isBlank(rs.getString(13))) {
                            Data data = dataService.readData(rs);
                            context.setLastDataIdForTransactionId(data);
                            memQueue.add(data);
                            dataCount++;
                            context.incrementStat(System.currentTimeMillis() - ts,
                                    ChannelRouterContext.STAT_READ_DATA_MS);
                        } else {
                            context.incrementStat(System.currentTimeMillis() - ts,
                                    ChannelRouterContext.STAT_REREAD_DATA_MS);
                        }

                        ts = System.currentTimeMillis();

                        if (toRead == 0) {
                            copyToQueue(memQueue);
                            toRead = maxQueueSize - dataQueue.size();
                            memQueue = new ArrayList<Data>(toRead);
                        } else {
                            toRead--;
                        }

                        context.incrementStat(System.currentTimeMillis() - ts,
                                ChannelRouterContext.STAT_ENQUEUE_DATA_MS);

                        ts = System.currentTimeMillis();
                    }

                    ts = System.currentTimeMillis();

                    copyToQueue(memQueue);

                    context.incrementStat(System.currentTimeMillis() - ts,
                            ChannelRouterContext.STAT_ENQUEUE_DATA_MS);

                    return dataCount;

                } finally {
                    JdbcUtils.closeResultSet(rs);
                    JdbcUtils.closeStatement(ps);
                    rs = null;
                    ps = null;

                    if (dbDialect.requiresAutoCommitFalseToSetFetchSize()) {
                        c.commit();
                        c.setAutoCommit(autoCommit);
                    }

                    boolean done = false;
                    do {
                        done = dataQueue.offer(new EOD());
                        if (!done) {
                            AppUtils.sleep(50);
                        }
                    } while (!done && reading);

                    reading = false;

                }
            }
        });
    }

    protected void copyToQueue(List<Data> memQueue) {
        while (memQueue.size() > 0 && reading) {
            Data d = memQueue.get(0);
            if (dataQueue.offer(d)) {
                memQueue.remove(0);
            } else {
                AppUtils.sleep(50);
            }
        }
    }

    public boolean isReading() {
        return reading;
    }

    public void setReading(boolean reading) {
        this.reading = reading;
    }

    public BlockingQueue<Data> getDataQueue() {
        return dataQueue;
    }

    class EOD extends Data {
        private static final long serialVersionUID = 1L;
    }
}