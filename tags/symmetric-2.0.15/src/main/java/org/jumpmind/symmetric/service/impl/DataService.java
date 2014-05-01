/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Eric Long <erilong@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.jumpmind.symmetric.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.Version;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.common.Message;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.TableConstants;
import org.jumpmind.symmetric.db.JdbcBatchPreparedStatementCallback;
import org.jumpmind.symmetric.db.SequenceIdentifier;
import org.jumpmind.symmetric.ext.IHeartbeatListener;
import org.jumpmind.symmetric.load.IReloadListener;
import org.jumpmind.symmetric.model.Data;
import org.jumpmind.symmetric.model.DataEvent;
import org.jumpmind.symmetric.model.DataEventType;
import org.jumpmind.symmetric.model.DataRef;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.NodeGroupLink;
import org.jumpmind.symmetric.model.NodeGroupLinkAction;
import org.jumpmind.symmetric.model.OutgoingBatch;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.model.TriggerHistory;
import org.jumpmind.symmetric.model.TriggerRouter;
import org.jumpmind.symmetric.model.OutgoingBatch.Status;
import org.jumpmind.symmetric.service.ClusterConstants;
import org.jumpmind.symmetric.service.IClusterService;
import org.jumpmind.symmetric.service.IConfigurationService;
import org.jumpmind.symmetric.service.IDataService;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IOutgoingBatchService;
import org.jumpmind.symmetric.service.IPurgeService;
import org.jumpmind.symmetric.service.ITriggerRouterService;
import org.jumpmind.symmetric.util.AppUtils;
import org.jumpmind.symmetric.util.CsvUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.csvreader.CsvWriter;

public class DataService extends AbstractService implements IDataService {

    private ITriggerRouterService triggerRouterService;

    private INodeService nodeService;

    private IPurgeService purgeService;

    private IClusterService clusterService;
    
    private IConfigurationService configurationService;

    private IOutgoingBatchService outgoingBatchService;

    private List<IReloadListener> reloadListeners;

    private List<IHeartbeatListener> heartbeatListeners;

    protected Map<IHeartbeatListener, Long> lastHeartbeatTimestamps = new HashMap<IHeartbeatListener, Long>();

    public void insertReloadEvent(final Node targetNode, final TriggerRouter triggerRouter) {
        insertReloadEvent(targetNode, triggerRouter, null);
    }

    public void insertReloadEvent(final Node targetNode, final TriggerRouter triggerRouter,
            final String overrideInitialLoadSelect) {
        TriggerHistory history = lookupTriggerHistory(triggerRouter.getTrigger());
        // initial_load_select for table can be overridden by populating the
        // row_data
        Data data = new Data(history.getSourceTableName(), DataEventType.RELOAD,
                overrideInitialLoadSelect != null ? overrideInitialLoadSelect : triggerRouter.getInitialLoadSelect(), null, history, Constants.CHANNEL_RELOAD, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(), triggerRouter.getRouter().getRouterId());
    }

    public void insertResendConfigEvent(final Node targetNode) {
        Data data = new Data(Constants.NA, DataEventType.CONFIG, null, null, null, Constants.CHANNEL_CONFIG, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(), Constants.UNKNOWN_ROUTER_ID);
    }

    private TriggerHistory lookupTriggerHistory(Trigger trigger) {
        TriggerHistory history = triggerRouterService.getNewestTriggerHistoryForTrigger(trigger.getTriggerId());

        if (history == null) {
            throw new RuntimeException("Cannot find history for trigger " + trigger.getTriggerId() + ", "
                    + trigger.getSourceTableName());
        }
        return history;
    }

    public void insertPurgeEvent(final Node targetNode, final TriggerRouter triggerRouter) {
        String sql = dbDialect.createPurgeSqlFor(targetNode, triggerRouter);
        insertSqlEvent(targetNode, triggerRouter.getTrigger(), sql);
    }

    public void insertSqlEvent(final Node targetNode, final Trigger trigger, String sql) {
        TriggerHistory history = triggerRouterService.getNewestTriggerHistoryForTrigger(trigger.getTriggerId());
        Data data = new Data(trigger.getSourceTableName(), DataEventType.SQL, CsvUtils.escapeCsvData(sql), null,
                history, Constants.CHANNEL_RELOAD, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(), Constants.UNKNOWN_ROUTER_ID);
    }

    public void insertSqlEvent(final Node targetNode, String sql) {
        Data data = new Data(Constants.NA, DataEventType.SQL, CsvUtils.escapeCsvData(sql), null, null,
                Constants.CHANNEL_RELOAD, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(), Constants.UNKNOWN_ROUTER_ID);
    }
    
    public int countDataInRange(long firstDataId, long secondDataId) {
        return jdbcTemplate.queryForInt(getSql("countDataInRangeSql"), firstDataId, secondDataId);
    }

    public void insertCreateEvent(final Node targetNode, final TriggerRouter triggerRouter, String xml) {
        TriggerHistory history = triggerRouterService.getNewestTriggerHistoryForTrigger(triggerRouter.getTrigger()
                .getTriggerId());
        Data data = new Data(triggerRouter.getTrigger().getSourceTableName(), DataEventType.CREATE, CsvUtils
                .escapeCsvData(xml), null, history, Constants.CHANNEL_RELOAD, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(), Constants.UNKNOWN_ROUTER_ID);
    }

    public long insertData(final Data data) {
        long id = dbDialect.insertWithGeneratedKey(getSql("insertIntoDataSql"), SequenceIdentifier.DATA,
                new PreparedStatementCallback<Object>() {
                    public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                        ps.setString(1, data.getTableName());
                        ps.setString(2, data.getEventType().getCode());
                        ps.setString(3, data.getRowData());
                        ps.setString(4, data.getPkData());
                        ps.setString(5, data.getOldData());
                        ps.setLong(6, data.getTriggerHistory() != null ? data.getTriggerHistory().getTriggerHistoryId()
                                : -1);
                        ps.setString(7, data.getChannelId());
                        return null;
                    }
                });
        data.setDataId(id);
        return id;
    }

    public void insertDataEvent(DataEvent dataEvent) {
        this.insertDataEvent(jdbcTemplate, dataEvent.getDataId(), dataEvent.getBatchId(), dataEvent.getRouterId());
    }

    public void insertDataEvent(long dataId, long batchId, String routerId) {
        this.insertDataEvent(jdbcTemplate, dataId, batchId, routerId);
    }

    public void insertDataEvent(JdbcTemplate template, long dataId, long batchId, String routerId) {
        try {
            template.update(getSql("insertIntoDataEventSql"), new Object[] { dataId, batchId,
                StringUtils.isBlank(routerId) ? Constants.UNKNOWN_ROUTER_ID : routerId }, new int[] { Types.NUMERIC,
                Types.NUMERIC, Types.VARCHAR });
        } catch (RuntimeException ex) {
            log.error("DataEventInsertFailed", ex, dataId, batchId, routerId);
            throw ex;
        }
    }
    
    public void insertDataEvents(JdbcTemplate template, final List<DataEvent> events) {
        if (events.size() > 0) {
            JdbcBatchPreparedStatementCallback callback = new JdbcBatchPreparedStatementCallback(
                    dbDialect, new BatchPreparedStatementSetter() {

                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            DataEvent event = events.get(i);
                            ps.setLong(1, event.getDataId());
                            ps.setLong(2, event.getBatchId());
                            ps
                                    .setString(
                                            3,
                                            StringUtils.isBlank(event.getRouterId()) ? Constants.UNKNOWN_ROUTER_ID
                                                    : event.getRouterId());
                        }

                        public int getBatchSize() {
                            return events.size();
                        }
                    }, parameterService.getInt(ParameterConstants.JDBC_EXECUTE_BATCH_SIZE));

            template.execute(getSql("insertIntoDataEventSql"), callback);
        }

    }

    public void insertDataAndDataEventAndOutgoingBatch(Data data, String channelId, List<Node> nodes, String routerId) {
        long dataId = insertData(data);
        for (Node node : nodes) {
            insertDataEventAndOutgoingBatch(dataId, channelId, node.getNodeId(), routerId);
        }
    }

    public void insertDataAndDataEventAndOutgoingBatch(Data data, String nodeId, String routerId) {
        long dataId = insertData(data);
        insertDataEventAndOutgoingBatch(dataId, data.getChannelId(), nodeId, routerId);
    }

    public void insertDataEventAndOutgoingBatch(long dataId, String channelId, String nodeId, String routerId) {
        OutgoingBatch outgoingBatch = new OutgoingBatch(nodeId, channelId, OutgoingBatch.Status.NE);
        outgoingBatchService.insertOutgoingBatch(outgoingBatch);
        insertDataEvent(new DataEvent(dataId, outgoingBatch.getBatchId(), routerId));
    }

    public String reloadNode(String nodeId) {
        Node targetNode = nodeService.findNode(nodeId);
        if (targetNode == null) {
            return Message.get("NodeUnknown", nodeId);
        }
        if (nodeService.setInitialLoadEnabled(nodeId, true)) {
            return Message.get("NodeInitialLoadOpened", nodeId);
        } else {
            return Message.get("NodeInitialLoadFailed", nodeId);
        }
    }

    public void insertReloadEvent(Node targetNode) {
        
        // outgoing data events are pointless because we are reloading all
        // data
        outgoingBatchService.markAllAsSentForNode(targetNode);
        
        if (parameterService.is(ParameterConstants.DATA_RELOAD_IS_BATCH_INSERT_TRANSACTIONAL)) {
            newTransactionTemplate.execute(new TransactionalInsertReloadEventsDelegate(targetNode));
        } else {
            new TransactionalInsertReloadEventsDelegate(targetNode).doInTransaction(null);
        }
        
        // remove all incoming events from the node are starting a reload for.
        purgeService.purgeAllIncomingEventsForNode(targetNode.getNodeId());
    }
    
    class TransactionalInsertReloadEventsDelegate implements TransactionCallback<Object> {

        Node targetNode;

        public TransactionalInsertReloadEventsDelegate(Node targetNode) {
            this.targetNode = targetNode;
        }

        public Object doInTransaction(TransactionStatus status) {
            Node sourceNode = nodeService.findIdentity();
            if (reloadListeners != null) {
                for (IReloadListener listener : reloadListeners) {
                    listener.beforeReload(targetNode);
                }
            }

            // insert node security so the client doing the initial load knows
            // that
            // an initial load is currently happening
            insertNodeSecurityUpdate(targetNode, true);

            List<TriggerRouter> triggerRouters = triggerRouterService
                    .getAllTriggerRoutersForReloadForCurrentNode(sourceNode.getNodeGroupId(),
                            targetNode.getNodeGroupId());

            if (parameterService.is(ParameterConstants.AUTO_CREATE_SCHEMA_BEFORE_RELOAD)) {
                for (TriggerRouter triggerRouter : triggerRouters) {
                    String xml = dbDialect.getCreateTableXML(triggerRouter);
                    insertCreateEvent(targetNode, triggerRouter, xml);
                }
            }

            if (parameterService.is(ParameterConstants.AUTO_DELETE_BEFORE_RELOAD)) {
                for (ListIterator<TriggerRouter> iterator = triggerRouters
                        .listIterator(triggerRouters.size()); iterator.hasPrevious();) {
                    TriggerRouter triggerRouter = iterator.previous();
                    insertPurgeEvent(targetNode, triggerRouter);
                }
            }

            for (TriggerRouter trigger : triggerRouters) {
                insertReloadEvent(targetNode, trigger);
            }

            if (reloadListeners != null) {
                for (IReloadListener listener : reloadListeners) {
                    listener.afterReload(targetNode);
                }
            }

            nodeService.setInitialLoadEnabled(targetNode.getNodeId(), false);
            insertNodeSecurityUpdate(targetNode, true);

            return null;
        }

    }

    private void insertNodeSecurityUpdate(Node node, boolean isReload) {
        Data data = createData(tablePrefix + "_node_security", " t.node_id = '" + node.getNodeId() + "'", isReload);
        if (data != null) {
            insertDataAndDataEventAndOutgoingBatch(data, node.getNodeId(), Constants.UNKNOWN_ROUTER_ID);
        }
    }

    public void sendScript(String nodeId, String script) {
        Node targetNode = nodeService.findNode(nodeId);
        Data data = new Data(Constants.NA, DataEventType.BSH, CsvUtils.escapeCsvData(script), null, null,
                Constants.CHANNEL_RELOAD, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(), Constants.UNKNOWN_ROUTER_ID);
    }

    public String sendSQL(String nodeId, String tableName, String sql) {
        Node sourceNode = nodeService.findIdentity();
        Node targetNode = nodeService.findNode(nodeId);
        if (targetNode == null) {
            // TODO message bundle
            return "Unknown node " + nodeId;
        }

        TriggerRouter trigger = triggerRouterService.findTriggerRouterForCurrentNode(tableName);
        if (trigger == null) {
            // TODO message bundle
            return "Trigger for table " + tableName + " does not exist from node " + sourceNode.getNodeGroupId();
        }

        insertSqlEvent(targetNode, trigger.getTrigger(), sql);
        // TODO message bundle
        return "Successfully create SQL event for node " + targetNode.getNodeId();
    }

    public String reloadTable(String nodeId, String tableName) {
        return reloadTable(nodeId, tableName, null);
    }

    public String reloadTable(String nodeId, String tableName, String overrideInitialLoadSelect) {
        Node sourceNode = nodeService.findIdentity();
        Node targetNode = nodeService.findNode(nodeId);
        if (targetNode == null) {
            // TODO message bundle
            return "Unknown node " + nodeId;
        }

        TriggerRouter triggerRouter = triggerRouterService.findTriggerRouterForCurrentNode(tableName);
        if (triggerRouter == null) {
            // TODO message bundle
            return "Trigger for table " + tableName + " does not exist from node " + sourceNode.getNodeGroupId();
        }

        if (parameterService.is(ParameterConstants.AUTO_CREATE_SCHEMA_BEFORE_RELOAD)) {
            String xml = dbDialect.getCreateTableXML(triggerRouter);
            insertCreateEvent(targetNode, triggerRouter, xml);
        } else if (parameterService.is(ParameterConstants.AUTO_DELETE_BEFORE_RELOAD)) {
            insertPurgeEvent(targetNode, triggerRouter);
        }

        insertReloadEvent(targetNode, triggerRouter, overrideInitialLoadSelect);

        // TODO message bundle
        return "Successfully created event to reload table " + tableName + " for node " + targetNode.getNodeId();
    }

    /**
     * Because we can't add a trigger on the _node table, we are artificially
     * generating heartbeat events.
     * 
     * @param node
     */
    public void insertHeartbeatEvent(Node node, boolean isReload) {
        String tableName = TableConstants.getTableName(tablePrefix, TableConstants.SYM_NODE);
        List<NodeGroupLink> links = configurationService.getGroupLinksFor(parameterService.getNodeGroupId());
        for (NodeGroupLink nodeGroupLink : links) {
            if (nodeGroupLink.getDataEventAction() == NodeGroupLinkAction.P) {
                TriggerRouter triggerRouter = triggerRouterService
                .getTriggerRouterForTableForCurrentNode(nodeGroupLink, tableName, false);
                if (triggerRouter != null) {
                    Data data = createData(triggerRouter.getTrigger(), String.format(" t.node_id = '%s'", node.getNodeId()), false);
                    if (data != null) {
                        insertData(data);
                    } else {
                        log.warn("TableGeneratingEventsFailure", tableName);
                    }
                } else {
                    log.warn("TableGeneratingEventsFailure", tableName);
                }                                
            }
        }
    }

    public Data createData(String tableName, boolean isReload) {
        return createData(tableName, null, isReload);
    }

    public Data createData(String tableName, String whereClause, boolean isReload) {
        Data data = null;
        TriggerRouter trigger = triggerRouterService.getTriggerRouterForTableForCurrentNode(tableName, false);
        if (trigger != null) {
            data = createData(trigger.getTrigger(), whereClause, isReload);
        }
        return data;
    }

    public Data createData(Trigger trigger, String whereClause, boolean isReload) {
        Data data = null;
        if (trigger != null) {
            String rowData = null;
            String pkData = null;
            if (whereClause != null) {
                rowData = (String) jdbcTemplate.queryForObject(dbDialect.createCsvDataSql(trigger, whereClause),
                        String.class);
                pkData = (String) jdbcTemplate.queryForObject(dbDialect.createCsvPrimaryKeySql(trigger, whereClause),
                        String.class);
            }
            TriggerHistory history = triggerRouterService.getNewestTriggerHistoryForTrigger(trigger.getTriggerId());
            if (history == null) {
                history = triggerRouterService.findTriggerHistory(trigger.getSourceTableName());
                if (history == null) {
                    history = triggerRouterService.findTriggerHistory(trigger.getSourceTableName().toUpperCase());
                }
            }
            if (history != null) {
                data = new Data(trigger.getSourceTableName(), DataEventType.UPDATE, rowData, pkData, history,
                        isReload ? Constants.CHANNEL_RELOAD : trigger.getChannelId(), null, null);
            }
        }
        return data;
    }

    public DataRef getDataRef() {
        List<DataRef> refs = getSimpleTemplate().query(getSql("findDataRefSql"), new RowMapper<DataRef>() {
            public DataRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new DataRef(rs.getLong(1), rs.getDate(2));
            }
        });
        if (refs.size() > 0) {
            return refs.get(0);
        } else {
            return new DataRef(-1, new Date());
        }
    }

    public void saveDataRef(DataRef dataRef) {
        if (0 >= jdbcTemplate.update(getSql("updateDataRefSql"), new Object[] { dataRef.getRefDataId(),
                dataRef.getRefTime() }, new int[] { Types.NUMERIC, Types.TIMESTAMP })) {
            jdbcTemplate.update(getSql("insertDataRefSql"),
                    new Object[] { dataRef.getRefDataId(), dataRef.getRefTime() }, new int[] { Types.NUMERIC,
                            Types.TIMESTAMP });
        }
    }

    public Date findCreateTimeOfEvent(long dataId) {
        return (Date) jdbcTemplate.queryForObject(getSql("findDataEventCreateTimeSql"), new Object[] { dataId },
                new int[] { Types.NUMERIC }, Date.class);
    }
    
    public Date findCreateTimeOfData(long dataId) {        
        return (Date) jdbcTemplate.queryForObject(getSql("findDataCreateTimeSql"), new Object[] { dataId },
                new int[] { Types.NUMERIC }, Date.class);
    }

    public Map<String, String> getRowDataAsMap(Data data) {
        Map<String, String> map = new HashMap<String, String>();
        String[] columnNames = CsvUtils.tokenizeCsvData(data.getTriggerHistory().getColumnNames());
        String[] columnData = CsvUtils.tokenizeCsvData(data.getRowData());
        for (int i = 0; i < columnNames.length; i++) {
            map.put(columnNames[i].toLowerCase(), columnData[i]);
        }
        return map;
    }

    public void setRowDataFromMap(Data data, Map<String, String> map) {
        String[] columnNames = CsvUtils.tokenizeCsvData(data.getTriggerHistory().getColumnNames());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CsvWriter writer = new CsvWriter(new OutputStreamWriter(out), ',');
        writer.setEscapeMode(CsvWriter.ESCAPE_MODE_BACKSLASH);
        for (String columnName : columnNames) {
            try {
                writer.write(map.get(columnName.toLowerCase()), true);
            } catch (IOException e) {
            }
        }
        writer.close();
        data.setRowData(out.toString());
    }

    /**
     * Get a list of {@link IHeartbeatListener}s that are ready for a heartbeat
     * according to
     * {@link IHeartbeatListener#getTimeBetweenHeartbeatsInSeconds()}
     * 
     * @param force
     *            if true, then return the entire list of
     *            {@link IHeartbeatListener}s
     */
    protected List<IHeartbeatListener> getHeartbeatListeners(boolean force) {
        if (force) {
            return this.heartbeatListeners;
        } else {
            List<IHeartbeatListener> listeners = new ArrayList<IHeartbeatListener>();
            if (listeners != null) {
                long ts = System.currentTimeMillis();
                for (IHeartbeatListener iHeartbeatListener : this.heartbeatListeners) {
                    Long lastHeartbeatTimestamp = lastHeartbeatTimestamps.get(iHeartbeatListener);
                    if (lastHeartbeatTimestamp == null
                            || lastHeartbeatTimestamp <= ts
                                    - (iHeartbeatListener.getTimeBetweenHeartbeatsInSeconds() * 1000)) {
                        listeners.add(iHeartbeatListener);
                    }
                }
            }
            return listeners;
        }
    }

    protected void updateLastHeartbeatTime(List<IHeartbeatListener> listeners) {
        if (listeners != null) {
            Long ts = System.currentTimeMillis();
            for (IHeartbeatListener iHeartbeatListener : listeners) {
                lastHeartbeatTimestamps.put(iHeartbeatListener, ts);
            }
        }
    }

    /**
     * @see IDataService#heartbeat()
     */
    public void heartbeat(boolean force) {
        List<IHeartbeatListener> listeners = getHeartbeatListeners(force);
        if (listeners.size() > 0) {
            if (clusterService.lock(ClusterConstants.HEARTBEAT)) {
                try {
                    Node me = nodeService.findIdentity();
                    if (me != null) {
                        log.info("NodeVersionUpdating");
                        Calendar now = Calendar.getInstance();
                        now.set(Calendar.MILLISECOND, 0);
                        me.setHeartbeatTime(now.getTime());
                        me.setTimezoneOffset(AppUtils.getTimezoneOffset());
                        me.setSymmetricVersion(Version.version());
                        me.setDatabaseType(dbDialect.getName());
                        me.setDatabaseVersion(dbDialect.getVersion());
                        me.setBatchToSendCount(outgoingBatchService.countOutgoingBatchesWithStatus(Status.NE));
                        me.setBatchInErrorCount(outgoingBatchService.countOutgoingBatchesWithStatus(Status.ER));
                        if (parameterService.is(ParameterConstants.AUTO_UPDATE_NODE_VALUES)) {
                            log.info("NodeConfigurationUpdating");
                            me.setSchemaVersion(parameterService.getString(ParameterConstants.SCHEMA_VERSION));
                            me.setExternalId(parameterService.getExternalId());
                            me.setNodeGroupId(parameterService.getNodeGroupId());
                            if (!StringUtils.isBlank(parameterService.getSyncUrl())) {
                                me.setSyncUrl(parameterService.getSyncUrl());
                            }
                        }

                        nodeService.updateNode(me);
                        nodeService.updateNodeHostForCurrentNode();
                        log.info("NodeVersionUpdated");

                        Set<Node> children = nodeService.findNodesThatOriginatedFromNodeId(me.getNodeId());
                        for (IHeartbeatListener l : listeners) {
                            l.heartbeat(me, children);
                        }

                    }

                } finally {
                    updateLastHeartbeatTime(listeners);
                    clusterService.unlock(ClusterConstants.HEARTBEAT);
                }

            } else {
                log.info("HeartbeatUpdatingFailure");
            }
        }
    }

    public void setReloadListeners(List<IReloadListener> listeners) {
        this.reloadListeners = listeners;
    }

    public void addReloadListener(IReloadListener listener) {
        if (reloadListeners == null) {
            reloadListeners = new ArrayList<IReloadListener>();
        }
        reloadListeners.add(listener);
    }

    public boolean removeReloadListener(IReloadListener listener) {
        if (reloadListeners != null) {
            return reloadListeners.remove(listener);
        } else {
            return false;
        }
    }

    public void setHeartbeatListeners(List<IHeartbeatListener> listeners) {
        this.heartbeatListeners = listeners;
    }

    public void addHeartbeatListener(IHeartbeatListener listener) {
        if (heartbeatListeners == null) {
            heartbeatListeners = new ArrayList<IHeartbeatListener>();
        }
        heartbeatListeners.add(listener);
    }

    public boolean removeHeartbeatListener(IHeartbeatListener listener) {
        if (heartbeatListeners != null) {
            return heartbeatListeners.remove(listener);
        } else {
            return false;
        }
    }

    public void setTriggerRouterService(ITriggerRouterService triggerService) {
        this.triggerRouterService = triggerService;
    }

    public void setNodeService(INodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPurgeService(IPurgeService purgeService) {
        this.purgeService = purgeService;
    }

    public void setOutgoingBatchService(IOutgoingBatchService outgoingBatchService) {
        this.outgoingBatchService = outgoingBatchService;
    }

    public void setClusterService(IClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public Data readData(ResultSet results) throws SQLException {
        Data data = new Data();
        data.setDataId(results.getLong(1));
        data.setTableName(results.getString(2));
        data.setEventType(DataEventType.getEventType(results.getString(3)));
        data.setRowData(results.getString(4));
        data.setPkData(results.getString(5));
        data.setOldData(results.getString(6));
        data.setCreateTime(results.getDate(7));
        int histId = results.getInt(8);
        data.setTriggerHistory(triggerRouterService.getTriggerHistory(histId));
        data.setChannelId(results.getString(9));
        data.setTransactionId(results.getString(10));
        data.setSourceNodeId(results.getString(11));
        data.setExternalData(results.getString(12));
        // TODO Be careful add more columns.  Callers might not be expecting them.
        return data;
    }

    public void setConfigurationService(IConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}