/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.symmetric.service.impl;

import static org.jumpmind.symmetric.service.ClusterConstants.HEARTBEAT;
import static org.jumpmind.symmetric.service.ClusterConstants.PULL;
import static org.jumpmind.symmetric.service.ClusterConstants.PURGE_DATA_GAPS;
import static org.jumpmind.symmetric.service.ClusterConstants.PURGE_INCOMING;
import static org.jumpmind.symmetric.service.ClusterConstants.PURGE_OUTGOING;
import static org.jumpmind.symmetric.service.ClusterConstants.PURGE_STATISTICS;
import static org.jumpmind.symmetric.service.ClusterConstants.PUSH;
import static org.jumpmind.symmetric.service.ClusterConstants.ROUTE;
import static org.jumpmind.symmetric.service.ClusterConstants.STAGE_MANAGEMENT;
import static org.jumpmind.symmetric.service.ClusterConstants.STATISTICS;
import static org.jumpmind.symmetric.service.ClusterConstants.SYNCTRIGGERS;
import static org.jumpmind.symmetric.service.ClusterConstants.WATCHDOG;
import static org.jumpmind.symmetric.service.ClusterConstants.FILE_SYNC_PULL;
import static org.jumpmind.symmetric.service.ClusterConstants.FILE_SYNC_PUSH;
import static org.jumpmind.symmetric.service.ClusterConstants.FILE_SYNC_TRACKER;
import static org.jumpmind.symmetric.service.ClusterConstants.INITIAL_LOAD_EXTRACT;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jumpmind.db.sql.ConcurrencySqlException;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.sql.UniqueKeyException;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.SystemConstants;
import org.jumpmind.symmetric.db.ISymmetricDialect;
import org.jumpmind.symmetric.model.Lock;
import org.jumpmind.symmetric.service.IClusterService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.util.AppUtils;

/**
 * @see IClusterService
 */
public class ClusterService extends AbstractService implements IClusterService {

    private String serverId = null;

    public ClusterService(IParameterService parameterService, ISymmetricDialect dialect) {
        super(parameterService, dialect);
        setSqlMap(new ClusterServiceSqlMap(symmetricDialect.getPlatform(),
                createSqlReplacementTokens()));
    }

    public void init() {
        initLockTable(ROUTE);
        initLockTable(PULL);
        initLockTable(PUSH);
        initLockTable(HEARTBEAT);
        initLockTable(PURGE_INCOMING);
        initLockTable(PURGE_OUTGOING);
        initLockTable(PURGE_STATISTICS);
        initLockTable(SYNCTRIGGERS);
        initLockTable(PURGE_DATA_GAPS);
        initLockTable(STAGE_MANAGEMENT);
        initLockTable(WATCHDOG);
        initLockTable(STATISTICS);
        initLockTable(FILE_SYNC_PULL);
        initLockTable(FILE_SYNC_PUSH);
        initLockTable(FILE_SYNC_TRACKER);
        initLockTable(INITIAL_LOAD_EXTRACT);
    }

    public void initLockTable(final String action) {
        try {
            sqlTemplate.update(getSql("insertLockSql"), new Object[] { action });
            log.debug("Inserted into the NODE_LOCK table for {}", action);
        } catch (UniqueKeyException ex) {
            log.debug(
                    "Failed to insert to the NODE_LOCK table for {}.  Must be initialized already.",
                    action);
        }
    }

    public void clearAllLocks() {
        sqlTemplate.update(getSql("clearAllLocksSql"));
    }

    public boolean lock(final String action) {
        if (isClusteringEnabled()) {
            final Date timeout = DateUtils.add(new Date(), Calendar.MILLISECOND,
                    (int) -parameterService.getLong(ParameterConstants.CLUSTER_LOCK_TIMEOUT_MS));
            return lock(action, timeout, new Date(), getServerId());
        } else {
            return true;
        }
    }

    protected boolean lock(String action, Date timeToBreakLock, Date timeLockAquired,
            String serverId) {
        try {
            return sqlTemplate.update(getSql("aquireLockSql"), new Object[] { serverId,
                    timeLockAquired, action, timeToBreakLock, serverId }) == 1;
        } catch (ConcurrencySqlException ex) {
            log.debug("Ignoring concurrency error and reporting that we failed to get the cluster lock: {}", ex.getMessage());
            return false;
        }
    }

    public Map<String, Lock> findLocks() {
        final Map<String, Lock> locks = new HashMap<String, Lock>();
        if (isClusteringEnabled()) {
            sqlTemplate.query(getSql("findLocksSql"), new ISqlRowMapper<Lock>() {
                public Lock mapRow(Row rs) {
                    Lock lock = new Lock();
                    lock.setLockAction(rs.getString("lock_action"));
                    lock.setLockingServerId(rs.getString("locking_server_id"));
                    lock.setLockTime(rs.getDateTime("lock_time"));
                    lock.setLastLockingServerId(rs.getString("last_locking_server_id"));
                    lock.setLastLockTime(rs.getDateTime("last_lock_time"));
                    locks.put(lock.getLockAction(), lock);
                    return lock;
                }
            });
        }
        return locks;
    }

    /**
     * Get a unique identifier that represents the JVM instance this server is
     * currently running in.
     */
    public String getServerId() {
        if (StringUtils.isBlank(serverId)) {
            serverId = parameterService.getString(ParameterConstants.CLUSTER_SERVER_ID);
            if (StringUtils.isBlank(serverId)) {
                serverId = System.getProperty(SystemConstants.SYSPROP_CLUSTER_SERVER_ID, null);
            }

            if (StringUtils.isBlank(serverId)) {
                // JBoss uses this system property to identify a server in a
                // cluster
                serverId = System.getProperty("bind.address", null);
            }

            if (StringUtils.isBlank(serverId)) {
                // JBoss uses this system property to identify a server in a
                // cluster
                serverId = System.getProperty("jboss.bind.address", null);
            }

            if (StringUtils.isBlank(serverId)) {
                try {
                    serverId = AppUtils.getHostName();
                } catch (Exception ex) {
                    serverId = "unknown";
                }
            }
            
            log.info("This node picked a server id of {}", serverId);
        }
        return serverId;
    }

    public void unlock(final String action) {
        if (isClusteringEnabled()) {
            if (!unlock(action, getServerId())) {
                log.warn("Failed to release lock for action:{} server:{}", action, getServerId());
            }
        }
    }

    protected boolean unlock(String action, String serverId) {
        String lastLockingServerId = serverId.equals(Lock.STOPPED) ? null : serverId;
        return sqlTemplate.update(getSql("releaseLockSql"), new Object[] { lastLockingServerId, action,
                serverId }) > 0;
    }

    public boolean isClusteringEnabled() {
        return parameterService.is(ParameterConstants.CLUSTER_LOCKING_ENABLED);
    }

    public boolean isInfiniteLocked(String action) {
        Map<String, Lock> locks = findLocks();
        Lock lock = locks.get(action);
        if (lock != null && lock.getLockTime() != null && new Date().before(lock.getLockTime())
                && Lock.STOPPED.equals(lock.getLockingServerId())) {
            return true;
        } else {
            return false;
        }
    }

    public void aquireInfiniteLock(String action) {
        if (isClusteringEnabled()) {
            int tries = 600;
            Date futureTime = DateUtils.add(new Date(), Calendar.YEAR, 100);
            while (tries > 0) {
                if (!lock(action, new Date(), futureTime, Lock.STOPPED)) {
                    AppUtils.sleep(50);
                    tries--;
                } else {
                    tries = 0;
                }
            }
        }
    }

    public void clearInfiniteLock(String action) {
        Map<String, Lock> all = findLocks();
        Lock lock = all.get(action);
        if (lock != null && Lock.STOPPED.equals(lock.getLockingServerId())) {
            unlock(action, Lock.STOPPED);
        }
    }

}