package org.jumpmind.symmetric.io.data.reader;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.IDatabasePlatform;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.sql.ISqlReadCursor;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.Row;
import org.jumpmind.symmetric.io.data.Batch;
import org.jumpmind.symmetric.io.data.CsvData;
import org.jumpmind.symmetric.io.data.DataContext;
import org.jumpmind.symmetric.io.data.DataEventType;
import org.jumpmind.symmetric.io.data.IDataReader;
import org.jumpmind.symmetric.io.data.IDataWriter;
import org.jumpmind.util.Statistics;

public class BatchCsvDataReader implements IDataReader {

    protected static final CsvDataRowMapper MAPPER = new CsvDataRowMapper();

    protected String selectSql;

    protected IDatabasePlatform platform;

    protected ISqlReadCursor<CsvData> dataCursor;

    protected Map<Batch, Statistics> statistics = new HashMap<Batch, Statistics>();

    protected List<Batch> batchesToSend;

    protected Batch batch;

    protected Table table;

    protected Map<Long, Table> tables;

    protected CsvData data;

    public BatchCsvDataReader(IDatabasePlatform platform, String sql, Map<Long, Table> tables,
            Batch... batches) {
        this.selectSql = sql;
        this.platform = platform;
        this.tables = tables;
        this.batchesToSend = new ArrayList<Batch>(batches.length);
        for (Batch batch : batches) {
            this.batchesToSend.add(batch);
        }
    }

    public <R extends IDataReader, W extends IDataWriter> void open(DataContext<R, W> context) {
    }

    public Batch nextBatch() {
        closeDataCursor();
        if (this.batchesToSend.size() > 0) {
            this.batch = this.batchesToSend.remove(0);
            this.statistics.put(batch, new Statistics());
            dataCursor = platform.getSqlTemplate().queryForCursor(selectSql, MAPPER,
                    new Object[] { batch.getBatchId() }, new int[] { Types.NUMERIC });
            return batch;
        } else {
            this.batch = null;
            return null;
        }

    }

    protected void closeDataCursor() {
        if (this.dataCursor != null) {
            this.dataCursor.close();
            this.dataCursor = null;
        }
    }

    public Table nextTable() {
        table = null;
        data = this.dataCursor.next();
        if (data != null) {
            table = tables.get(data.getAttribute(CsvData.ATTRIBUTE_TABLE_ID));
            if (table == null) {
                // TODO throw exception
            }
        }
        return table;
    }

    public CsvData nextData() {
        if (data == null) {
            data = this.dataCursor.next();
        }

        CsvData returnData = null;
        if (data != null) {
            Table newTable = tables.get(data.getAttribute(CsvData.ATTRIBUTE_TABLE_ID));
            if (newTable != null && newTable.equals(table)) {
                returnData = data;
                data = null;
            }
        }
        return returnData;
    }

    public void close() {
        closeDataCursor();
    }

    public Map<Batch, Statistics> getStatistics() {
        return statistics;
    }

    static class CsvDataRowMapper implements ISqlRowMapper<CsvData> {
        public CsvData mapRow(Row row) {
            CsvData data = new CsvData();
            data.putCsvData(CsvData.ROW_DATA, row.getString("ROW_DATA"));
            data.putCsvData(CsvData.PK_DATA, row.getString("PK_DATA"));
            data.putCsvData(CsvData.OLD_DATA, row.getString("OLD_DATA"));
            data.putAttribute(CsvData.ATTRIBUTE_CHANNEL_ID, row.getString("CHANNEL_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_TX_ID, row.getString("TRANSACTION_ID"));
            data.setDataEventType(DataEventType.getEventType(row.getString("EVENT_TYPE")));
            data.putAttribute(CsvData.ATTRIBUTE_TABLE_ID, row.getInt("TRIGGER_HIST_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_SOURCE_NODE_ID, row.getString("SOURCE_NODE_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_ROUTER_ID, row.getString("ROUTER_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_EXTERNAL_DATA, row.getString("EXTERNAL_DATA"));
            data.putAttribute(CsvData.ATTRIBUTE_DATA_ID, row.getLong("DATA_ID"));
            return data;
        }
    }

}
