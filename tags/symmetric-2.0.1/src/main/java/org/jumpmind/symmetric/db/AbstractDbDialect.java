/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Chris Henson <chenson42@users.sourceforge.net>,
 *               Eric Long <erilong@users.sourceforge.net>
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
package org.jumpmind.symmetric.db;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.IndexColumn;
import org.apache.ddlutils.model.NonUniqueIndex;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.model.UniqueIndex;
import org.apache.ddlutils.platform.DatabaseMetaDataWrapper;
import org.apache.ddlutils.platform.MetaDataColumnDescriptor;
import org.apache.ddlutils.platform.SqlBuilder;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.logging.ILog;
import org.jumpmind.symmetric.common.logging.LogFactory;
import org.jumpmind.symmetric.db.mssql.MsSqlDbDialect;
import org.jumpmind.symmetric.load.IColumnFilter;
import org.jumpmind.symmetric.model.DataEventType;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.model.TriggerHistory;
import org.jumpmind.symmetric.model.TriggerRouter;
import org.jumpmind.symmetric.service.IParameterService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

abstract public class AbstractDbDialect implements IDbDialect {

    final ILog logger = LogFactory.getLog(getClass());

    public static final String REQUIRED_FIELD_NULL_SUBSTITUTE = " ";

    public static final String[] TIMESTAMP_PATTERNS = { "yyyy-MM-dd HH:mm:ss.S",
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };

    public static final String[] TIME_PATTERNS = { "HH:mm:ss.S", "HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd HH:mm:ss" };

    public static final FastDateFormat JDBC_TIMESTAMP_FORMATTER = FastDateFormat
            .getInstance("yyyy-MM-dd hh:mm:ss.SSS");

    protected final ILog log = LogFactory.getLog(getClass());

    public static final int MAX_SYMMETRIC_SUPPORTED_TRIGGER_SIZE = 50;

    protected JdbcTemplate jdbcTemplate;

    protected Platform platform;

    protected DatabaseModel cachedModel = new DatabaseModel();

    protected SqlTemplate sqlTemplate;

    protected SQLErrorCodeSQLExceptionTranslator sqlErrorTranslator;

    private Map<Integer, String> _defaultSizes;

    protected IParameterService parameterService;

    protected String tablePrefix;

    protected int streamingResultsFetchSize;

    protected Boolean supportsGetGeneratedKeys;

    protected TransactionTemplate transactionTemplate;

    protected String databaseName;

    protected int databaseMajorVersion;

    protected int databaseMinorVersion;

    protected String databaseProductVersion;

    protected String identifierQuoteString;

    protected Set<String> sqlKeywords;

    protected String defaultSchema;

    protected LobHandler lobHandler;

    protected boolean supportsTransactionViews = false;

    protected AbstractDbDialect() {
        _defaultSizes = new HashMap<Integer, String>();
        _defaultSizes.put(new Integer(1), "254");
        _defaultSizes.put(new Integer(12), "254");
        _defaultSizes.put(new Integer(-1), "254");
        _defaultSizes.put(new Integer(-2), "254");
        _defaultSizes.put(new Integer(-3), "254");
        _defaultSizes.put(new Integer(-4), "254");
        _defaultSizes.put(new Integer(4), "32");
        _defaultSizes.put(new Integer(-5), "64");
        _defaultSizes.put(new Integer(7), "7,0");
        _defaultSizes.put(new Integer(6), "15,0");
        _defaultSizes.put(new Integer(8), "15,0");
        _defaultSizes.put(new Integer(3), "15,15");
        _defaultSizes.put(new Integer(2), "15,15");
    }

    public String toFormattedTimestamp(java.util.Date time) {
        StringBuilder ts = new StringBuilder("{ts '");
        ts.append(JDBC_TIMESTAMP_FORMATTER.format(time));
        ts.append("'}");
        return ts.toString();
    }

    public IColumnFilter getDatabaseColumnFilter() {
        return null;
    }

    public void prepareTableForDataLoad(Table table) {
    }

    public void cleanupAfterDataLoad(Table table) {
    }

    protected boolean allowsNullForIdentityColumn() {
        return true;
    }

    public void resetCachedTableModel() {
        synchronized (this.getClass()) {
            this.cachedModel.resetTableIndexCache();
        	Table[] tables = this.cachedModel.getTables();
            if (tables != null) {
                for (Table table : tables) {
                    this.cachedModel.removeTable(table);
                }
            }
        }
    }

    /**
     * Provide a default implementation of this method using DDLUtils,
     * getMaxColumnNameLength()
     */
    public int getMaxTriggerNameLength() {
        int max = getPlatform().getPlatformInfo().getMaxColumnNameLength();
        return max < MAX_SYMMETRIC_SUPPORTED_TRIGGER_SIZE && max > 0 ? max
                : MAX_SYMMETRIC_SUPPORTED_TRIGGER_SIZE;
    }

    public void init(Platform pf) {
        log.info("DbDialectInUse", this.getClass().getName());
        this.jdbcTemplate = new JdbcTemplate(pf.getDataSource());
        this.platform = pf;
        this.sqlErrorTranslator = new SQLErrorCodeSQLExceptionTranslator(pf.getDataSource());
        this.identifierQuoteString = "\"";
        jdbcTemplate.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(Connection c) throws SQLException, DataAccessException {
                DatabaseMetaData meta = c.getMetaData();
                databaseName = meta.getDatabaseProductName();
                databaseMajorVersion = meta.getDatabaseMajorVersion();
                databaseMinorVersion = meta.getDatabaseMinorVersion();
                databaseProductVersion = meta.getDatabaseProductVersion();
                return null;
            }
        });
    }

    abstract protected void initTablesAndFunctionsForSpecificDialect();

    public void initTablesAndFunctions() {
        initTablesAndFunctionsForSpecificDialect();
        createTablesIfNecessary();
        createRequiredFunctions();
        resetCachedTableModel();
    }

    final public boolean doesTriggerExist(String catalogName, String schema, String tableName,
            String triggerName) {
        try {
            return doesTriggerExistOnPlatform(catalogName, schema, tableName, triggerName);
        } catch (Exception ex) {
            log.warn("TriggerMayExist", ex);
            return false;
        }
    }

    protected void createRequiredFunctions() {
        String[] functions = sqlTemplate.getFunctionsToInstall();
        for (int i = 0; i < functions.length; i++) {
            String funcName = tablePrefix + "_" + functions[i];
            if (jdbcTemplate.queryForInt(sqlTemplate.getFunctionInstalledSql(funcName,
                    defaultSchema)) == 0) {
                jdbcTemplate.update(sqlTemplate.getFunctionSql(functions[i], funcName,
                        defaultSchema));
                log.info("FunctionInstalled", funcName);
            }
        }
    }

    public BinaryEncoding getBinaryEncoding() {
        return BinaryEncoding.NONE;
    }

    public boolean isDateOverrideToTimestamp() {
        return false;
    }

    abstract protected boolean doesTriggerExistOnPlatform(String catalogName, String schema,
            String tableName, String triggerName);

    public String getTransactionTriggerExpression(String defaultCatalog, String defaultSchema,
            Trigger trigger) {
        return "null";
    }

    public String createInitalLoadSqlFor(Node node, TriggerRouter trigger, Table table) {
        return sqlTemplate.createInitalLoadSql(node, this, trigger, table).trim();
    }

    public String createPurgeSqlFor(Node node, TriggerRouter triggerRouter) {
        return sqlTemplate.createPurgeSql(node, this, triggerRouter);
    }

    public String createCsvDataSql(Trigger trigger, String whereClause) {
        return sqlTemplate.createCsvDataSql(
                this,
                trigger,
                getTable(trigger.getSourceCatalogName(), trigger.getSourceSchemaName(), trigger
                        .getSourceTableName(), true), whereClause).trim();
    }

    public String createCsvPrimaryKeySql(Trigger trigger, String whereClause) {
        return sqlTemplate.createCsvPrimaryKeySql(
                this,
                trigger,
                getTable(trigger.getSourceCatalogName(), trigger.getSourceSchemaName(), trigger
                        .getSourceTableName(), true), whereClause).trim();
    }

    public Table getTable(Trigger trigger, boolean useCache) {
        return getTable(trigger.getSourceCatalogName(), trigger.getSourceSchemaName(), trigger
                .getSourceTableName(), useCache);
    }

    /**
     * This method uses the ddlutil's model reader which uses the jdbc metadata
     * to lookup up table metadata.
     * <p/>
     * Dialect may optionally override this method to more efficiently lookup up
     * table metadata directly against information schemas.
     */
    public Table getTable(String catalogName, String schemaName, String tableName, boolean useCache) {
        Table retTable = cachedModel.findTable(catalogName, schemaName, tableName);
        if (retTable == null || !useCache) {
            synchronized (this.getClass()) {
                try {
                    Table table = getTable(catalogName, schemaName, tableName);

                    if (retTable != null) {
                        cachedModel.removeTable(retTable);
                    }

                    if (table != null) {
                        cachedModel.addTable(table);
                    }

                    retTable = table;
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return retTable;
    }

    public Set<String> getSqlKeywords() {
        if (sqlKeywords == null) {
            jdbcTemplate.execute(new ConnectionCallback<Object>() {
                public Object doInConnection(Connection con) throws SQLException,
                        DataAccessException {
                    DatabaseMetaData metaData = con.getMetaData();
                    sqlKeywords = new HashSet<String>(Arrays.asList(metaData.getSQLKeywords()
                            .split(",")));
                    return null;
                }
            });
        }
        return sqlKeywords;
    }

    /**
     * Returns a new {@link Table} object.
     */
    protected Table getTable(String catalogName, String schemaName, String tblName) {
        if (parameterService.is(ParameterConstants.DB_METADATA_IGNORE_CASE)) {
            Table table = getTableCaseSensitive(StringUtils.upperCase(catalogName), StringUtils
                    .upperCase(schemaName), StringUtils.upperCase(tblName));
            if (table == null) {
                table = getTableCaseSensitive(StringUtils.lowerCase(catalogName), StringUtils
                        .lowerCase(schemaName), StringUtils.lowerCase(tblName));
                if (table == null) {
                    table = getTableCaseSensitive(catalogName, schemaName, StringUtils
                            .upperCase(tblName));
                    if (table == null) {
                        table = getTableCaseSensitive(catalogName, schemaName, StringUtils
                                .lowerCase(tblName));
                        if (table == null) {
                            table = getTableCaseSensitive(catalogName, schemaName,
                                    getPlatformTableName(catalogName, schemaName, tblName));
                        }
                    }
                }
            }
            return table;
        } else {
            return getTableCaseSensitive(catalogName, schemaName, tblName);
        }
    }

    protected String getPlatformTableName(String catalogName, String schemaName, String tblName) {
        return tblName;
    }

    protected Table getTableCaseSensitive(String catalogName, String schemaName,
            final String tblName) {
        // If we don't provide a default schema or catalog, then on some
        // databases multiple results will be found in the metadata from
        // multiple schemas/catalogs
        final String schema = StringUtils.isBlank(schemaName) ? getDefaultSchema() : schemaName;
        final String catalog = StringUtils.isBlank(catalogName) ? getDefaultCatalog() : catalogName;
        return (Table) jdbcTemplate.execute(new ConnectionCallback<Table>() {
            public Table doInConnection(Connection c) throws SQLException, DataAccessException {
                Table table = null;
                DatabaseMetaDataWrapper metaData = new DatabaseMetaDataWrapper();
                metaData.setMetaData(c.getMetaData());
                metaData.setCatalog(catalog);
                metaData.setSchemaPattern(schema);
                metaData.setTableTypes(null);
                String tableName = tblName;
                if (storesUpperCaseNamesInCatalog()) {
                    tableName = tblName.toUpperCase();
                } else if (storesLowerCaseNamesInCatalog()) {
                    tableName = tblName.toLowerCase();
                }

                ResultSet tableData = null;
                try {
                    tableData = metaData.getTables(getTableNamePattern(tableName));
                    while (tableData != null && tableData.next()) {
                        Map<String, Object> values = readColumns(tableData, initColumnsForTable());
                        table = readTable(metaData, values);
                    }
                } finally {
                    JdbcUtils.closeResultSet(tableData);
                }

                makeAllColumnsPrimaryKeysIfNoPrimaryKeysFound(table);

                return table;
            }
        });
    }

    protected String getTableNamePattern(String tableName) {
        return tableName;
    }

    /**
     * Treat tables with no primary keys as a table with all primary keys.
     */
    protected void makeAllColumnsPrimaryKeysIfNoPrimaryKeysFound(Table table) {
        if (table != null && table.getPrimaryKeyColumns() != null
                && table.getPrimaryKeyColumns().length == 0) {
            Column[] allCoumns = table.getColumns();
            for (Column column : allCoumns) {
                if (!column.isOfBinaryType()) {
                    column.setPrimaryKey(true);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected Table readTable(DatabaseMetaDataWrapper metaData, Map values) throws SQLException {
        String tableName = (String) values.get("TABLE_NAME");
        Table table = null;
        if (tableName != null && tableName.length() > 0) {
            table = new Table();
            table.setName(tableName);
            table.setType((String) values.get("TABLE_TYPE"));
            table.setCatalog((String) values.get("TABLE_CAT"));
            table.setSchema((String) values.get("TABLE_SCHEM"));
            table.setDescription((String) values.get("REMARKS"));
            table.addColumns(readColumns(metaData, tableName));
            if (parameterService.is(ParameterConstants.AUTO_CREATE_SCHEMA_BEFORE_RELOAD)) {
                table.addIndices(readIndices(metaData, tableName));
            }
            Collection primaryKeys = readPrimaryKeyNames(metaData, tableName);
            for (Iterator it = primaryKeys.iterator(); it.hasNext(); table.findColumn(
                    (String) it.next(), true).setPrimaryKey(true))
                ;

            if (this instanceof MsSqlDbDialect) {
                determineAutoIncrementFromResultSetMetaData(table, table.getColumns());
            }
        }
        return table;
    }

    protected List<MetaDataColumnDescriptor> initColumnsForTable() {
        List<MetaDataColumnDescriptor> result = new ArrayList<MetaDataColumnDescriptor>();
        result.add(new MetaDataColumnDescriptor("TABLE_NAME", 12));
        result.add(new MetaDataColumnDescriptor("TABLE_TYPE", 12, "UNKNOWN"));
        result.add(new MetaDataColumnDescriptor("TABLE_CAT", 12));
        result.add(new MetaDataColumnDescriptor("TABLE_SCHEM", 12));
        result.add(new MetaDataColumnDescriptor("REMARKS", 12));
        return result;
    }

    protected List<MetaDataColumnDescriptor> initColumnsForColumn() {
        List<MetaDataColumnDescriptor> result = new ArrayList<MetaDataColumnDescriptor>();
        result.add(new MetaDataColumnDescriptor("COLUMN_DEF", 12));
        result.add(new MetaDataColumnDescriptor("TABLE_NAME", 12));
        result.add(new MetaDataColumnDescriptor("COLUMN_NAME", 12));
        result.add(new MetaDataColumnDescriptor("TYPE_NAME", 12));
        result.add(new MetaDataColumnDescriptor("DATA_TYPE", 4, new Integer(1111)));
        result.add(new MetaDataColumnDescriptor("NUM_PREC_RADIX", 4, new Integer(10)));
        result.add(new MetaDataColumnDescriptor("DECIMAL_DIGITS", 4, new Integer(0)));
        result.add(new MetaDataColumnDescriptor("COLUMN_SIZE", 12));
        result.add(new MetaDataColumnDescriptor("IS_NULLABLE", 12, "YES"));
        result.add(new MetaDataColumnDescriptor("REMARKS", 12));
        return result;
    }

    protected List<MetaDataColumnDescriptor> initColumnsForPK() {
        List<MetaDataColumnDescriptor> result = new ArrayList<MetaDataColumnDescriptor>();
        result.add(new MetaDataColumnDescriptor("COLUMN_NAME", 12));
        result.add(new MetaDataColumnDescriptor("TABLE_NAME", 12));
        result.add(new MetaDataColumnDescriptor("PK_NAME", 12));
        return result;
    }

    @SuppressWarnings("unchecked")
    protected Collection<Column> readColumns(DatabaseMetaDataWrapper metaData, String tableName)
            throws SQLException {
        ResultSet columnData = null;
        try {
            columnData = metaData.getColumns(getTableNamePattern(tableName), null);
            List<Column> columns = new ArrayList<Column>();
            Map values = null;
            for (; columnData.next(); columns.add(readColumn(metaData, values))) {
                values = readColumns(columnData, initColumnsForColumn());
            }
            return columns;
        } finally {
            JdbcUtils.closeResultSet(columnData);
        }
    }

    @SuppressWarnings("unchecked")
    protected Integer overrideJdbcTypeForColumn(Map values) {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Column readColumn(DatabaseMetaDataWrapper metaData, Map values) throws SQLException {
        Column column = new Column();
        column.setName((String) values.get("COLUMN_NAME"));
        column.setDefaultValue((String) values.get("COLUMN_DEF"));
        Integer jdbcType = overrideJdbcTypeForColumn(values);
        if (jdbcType != null) {
            column.setTypeCode(jdbcType);
        } else {
            column.setTypeCode(((Integer) values.get("DATA_TYPE")).intValue());
        }

        column.setPrecisionRadix(((Integer) values.get("NUM_PREC_RADIX")).intValue());
        String size = (String) values.get("COLUMN_SIZE");
        int scale = ((Integer) values.get("DECIMAL_DIGITS")).intValue();
        if (size == null)
            size = (String) _defaultSizes.get(new Integer(column.getTypeCode()));
        column.setSize(size);
        if (scale != 0)
            column.setScale(scale);
        column.setRequired("NO".equalsIgnoreCase(((String) values.get("IS_NULLABLE")).trim()));
        column.setDescription((String) values.get("REMARKS"));
        return column;
    }

    protected void determineAutoIncrementFromResultSetMetaData(Table table,
            final Column columnsToCheck[]) throws SQLException {
        StringBuilder query;
        if (columnsToCheck == null || columnsToCheck.length == 0) {
            return;
        }
        query = new StringBuilder();
        query.append("SELECT ");
        for (int idx = 0; idx < columnsToCheck.length; idx++) {
            if (idx > 0)
                query.append(",");
            query.append("t.").append("\"").append(columnsToCheck[idx].getName()).append("\"");
        }

        query.append(" FROM ");
        if (table.getCatalog() != null && !table.getCatalog().trim().equals("")) {
            query.append("\"").append(table.getCatalog()).append("\".");
        }
        if (table.getSchema() != null && !table.getSchema().trim().equals("")) {
            query.append("\"").append(table.getSchema()).append("\".");
        }
        query.append("\"").append(table.getName()).append("\" t WHERE 1 = 0");

        final String finalQuery = query.toString();
        jdbcTemplate.execute(new StatementCallback<Object>() {
            public Object doInStatement(Statement stmt) throws SQLException, DataAccessException {
                ResultSet rs = stmt.executeQuery(finalQuery);
                ResultSetMetaData rsMetaData = rs.getMetaData();
                for (int idx = 0; idx < columnsToCheck.length; idx++)
                    if (rsMetaData.isAutoIncrement(idx + 1))
                        columnsToCheck[idx].setAutoIncrement(true);
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> readColumns(ResultSet resultSet, List columnDescriptors)
            throws SQLException {
        HashMap<String, Object> values = new HashMap<String, Object>();
        MetaDataColumnDescriptor descriptor;
        for (Iterator it = columnDescriptors.iterator(); it.hasNext(); values.put(descriptor
                .getName(), descriptor.readColumn(resultSet)))
            descriptor = (MetaDataColumnDescriptor) it.next();

        return values;
    }

    @SuppressWarnings("unchecked")
    protected Collection<String> readPrimaryKeyNames(DatabaseMetaDataWrapper metaData,
            String tableName) throws SQLException {
        ResultSet pkData = null;
        try {
            List<String> pks = new ArrayList<String>();
            Map values;
            for (pkData = metaData.getPrimaryKeys(getTableNamePattern(tableName)); pkData.next(); pks
                    .add(readPrimaryKeyName(metaData, values))) {
                values = readColumns(pkData, initColumnsForPK());
            }
            return pks;
        } finally {
            JdbcUtils.closeResultSet(pkData);
        }

    }

    @SuppressWarnings("unchecked")
    protected String readPrimaryKeyName(DatabaseMetaDataWrapper metaData, Map values)
            throws SQLException {
        return (String) values.get("COLUMN_NAME");
    }

    @SuppressWarnings("unchecked")
    protected List initColumnsForIndex() {
        List result = new ArrayList();

        result.add(new MetaDataColumnDescriptor("INDEX_NAME", Types.VARCHAR));
        // we're also reading the table name so that a model reader impl can
        // filter manually
        result.add(new MetaDataColumnDescriptor("TABLE_NAME", Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("NON_UNIQUE", Types.BIT, Boolean.TRUE));
        result.add(new MetaDataColumnDescriptor("ORDINAL_POSITION", Types.TINYINT, new Short(
                (short) 0)));
        result.add(new MetaDataColumnDescriptor("COLUMN_NAME", Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("TYPE", Types.TINYINT));

        return result;
    }

    @SuppressWarnings("unchecked")
    protected Collection readIndices(DatabaseMetaDataWrapper metaData, String tableName)
            throws SQLException {
        Map indices = new ListOrderedMap();
        ResultSet indexData = null;

        try {
            indexData = metaData.getIndices(getTableNamePattern(tableName), false, false);

            while (indexData.next()) {
                Map values = readColumns(indexData, initColumnsForIndex());

                readIndex(metaData, values, indices);
            }
        } finally {
            if (indexData != null) {
                indexData.close();
            }
        }
        return indices.values();
    }

    @SuppressWarnings("unchecked")
    protected void readIndex(DatabaseMetaDataWrapper metaData, Map values, Map knownIndices)
            throws SQLException {
        Short indexType = (Short) values.get("TYPE");

        // we're ignoring statistic indices
        if ((indexType != null) && (indexType.shortValue() == DatabaseMetaData.tableIndexStatistic)) {
            return;
        }

        String indexName = (String) values.get("INDEX_NAME");

        if (indexName != null) {
            Index index = (Index) knownIndices.get(indexName);

            if (index == null) {
                if (((Boolean) values.get("NON_UNIQUE")).booleanValue()) {
                    index = new NonUniqueIndex();
                } else {
                    index = new UniqueIndex();
                }

                index.setName(indexName);
                knownIndices.put(indexName, index);
            }

            IndexColumn indexColumn = new IndexColumn();

            indexColumn.setName((String) values.get("COLUMN_NAME"));
            if (values.containsKey("ORDINAL_POSITION")) {
                indexColumn.setOrdinalPosition(((Short) values.get("ORDINAL_POSITION")).intValue());
            }
            index.addColumn(indexColumn);
        }
    }

    public void removeTrigger(StringBuilder sqlBuffer, String catalogName, String schemaName,
            String triggerName, String tableName, TriggerHistory oldHistory) {
        schemaName = schemaName == null ? "" : (schemaName + ".");
        final String sql = "drop trigger " + schemaName + triggerName;
        logSql(sql, sqlBuffer);
        if (parameterService.is(ParameterConstants.AUTO_SYNC_TRIGGERS)) {
            try {
                jdbcTemplate.update(sql);
            } catch (Exception e) {
                log.warn("TriggerDoesNotExist");
            }
        }
    }

    final protected void logSql(String sql, StringBuilder sqlBuffer) {
        if (sqlBuffer != null) {
            sqlBuffer.append(sql);
            sqlBuffer.append(System.getProperty("line.separator"));
            sqlBuffer.append(System.getProperty("line.separator"));
        }
    }

    /**
     * Create the configured trigger. The catalog will be changed to the source
     * schema if the source schema is configured.
     */
    public void createTrigger(final StringBuilder sqlBuffer, final DataEventType dml,
            final Trigger trigger, final TriggerHistory hist, final String tablePrefix,
            final Table table) {
        jdbcTemplate.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(Connection con) throws SQLException, DataAccessException {
                log.info("TriggerCreating", hist.getTriggerNameForDmlType(dml), trigger
                        .getSourceTableName());

                String previousCatalog = null;
                String sourceCatalogName = trigger.getSourceCatalogName();
                String defaultCatalog = getDefaultCatalog();
                String defaultSchema = getDefaultSchema();
                try {
                    previousCatalog = switchCatalogForTriggerInstall(sourceCatalogName, con);

                    String triggerSql = sqlTemplate.createTriggerDDL(AbstractDbDialect.this, dml,
                            trigger, hist, tablePrefix, table, defaultCatalog, defaultSchema);

                    if (parameterService.is(ParameterConstants.AUTO_SYNC_TRIGGERS)) {
                        Statement stmt = con.createStatement();
                        try {
                            log.debug("Sql", triggerSql);
                            stmt.executeUpdate(triggerSql);
                        } catch (SQLException ex) {
                            log.error("TriggerCreateFailed", triggerSql);
                            throw ex;
                        }
                        String postTriggerDml = createPostTriggerDDL(dml, trigger, hist,
                                tablePrefix, table);
                        if (postTriggerDml != null) {
                            try {
                                stmt.executeUpdate(postTriggerDml);
                            } catch (SQLException ex) {
                                log.error("PostTriggerCreateFailed", postTriggerDml);
                                throw ex;
                            }
                        }
                        stmt.close();
                    }

                    logSql(triggerSql, sqlBuffer);

                } finally {
                    if (sourceCatalogName != null
                            && !sourceCatalogName.equalsIgnoreCase(previousCatalog)) {
                        switchCatalogForTriggerInstall(previousCatalog, con);
                    }
                }
                return null;
            }
        });
    }

    /**
     * Provide the option switch a connection's schema for trigger installation.
     */
    protected String switchCatalogForTriggerInstall(String catalog, Connection c)
            throws SQLException {
        return null;
    }

    public String createPostTriggerDDL(DataEventType dml, Trigger trigger, TriggerHistory hist,
            String tablePrefix, Table table) {
        return sqlTemplate.createPostTriggerDDL(this, dml, trigger, hist, tablePrefix, table,
                getDefaultCatalog(), getDefaultSchema());
    }

    public String getCreateSymmetricDDL() {
        Database db = readSymmetricSchemaFromXml();
        prefixConfigDatabase(db);
        return platform.getCreateTablesSql(db, true, true);
    }

    public String getCreateTableSQL(TriggerRouter trig) {
        Table table = getTable(null, trig.getTrigger().getSourceSchemaName(), trig.getTrigger()
                .getSourceTableName(), true);
        String sql = null;
        try {
            StringWriter buffer = new StringWriter();
            platform.getSqlBuilder().setWriter(buffer);
            platform.getSqlBuilder().createTable(cachedModel, table);
            sql = buffer.toString();
        } catch (IOException e) {
        }
        return sql;
    }

    public String getCreateTableXML(TriggerRouter triggerRouter) {
        Table table = getTable(null, triggerRouter.getTrigger().getSourceSchemaName(),
                triggerRouter.getTrigger().getSourceTableName());
        table.setName(triggerRouter.getTargetTable());
        Database db = new Database();
        db.setName(triggerRouter.getTargetSchema(getDefaultSchema()));
        if (db.getName() == null) {
            db.setName(getDefaultCatalog());
        }
        db.addTable(table);
        StringWriter buffer = new StringWriter();
        DatabaseIO xmlWriter = new DatabaseIO();
        xmlWriter.write(db, buffer);
        // TODO: remove when these bugs are fixed in DdlUtils
        String xml = buffer.toString().replaceAll("&apos;", "");
        xml = xml.replaceAll("default=\"empty_blob\\(\\) *\"", "");
        xml = xml.replaceAll("unique name=\"PRIMARY\"", "unique name=\"PRIMARYINDEX\"");
        return xml;
    }

    public void createTables(String xml) {
        StringReader reader = new StringReader(xml);
        Database db = new DatabaseIO().read(reader);
        platform.createTables(db, true, true);
    }

    public boolean doesDatabaseNeedConfigured() {
        return prefixConfigDatabase(readSymmetricSchemaFromXml());
    }

    protected boolean prefixConfigDatabase(Database targetTables) {
        try {
            String tblPrefix = this.tablePrefix + "_";

            Table[] tables = targetTables.getTables();

            boolean createTables = false;
            for (Table table : tables) {
                table.setName(tblPrefix + table.getName());
                fixForeignKeys(table, tblPrefix, false);

                if (getTable(getDefaultCatalog(), getDefaultSchema(), table.getName(), false) == null) {
                    createTables = true;
                }
            }

            return createTables;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public Database readPlatformDatabase(boolean includeSymmetricTables) {
        String schema = getDefaultSchema();
        String catalog = getDefaultCatalog();
        Database database = platform.readModelFromDatabase(!StringUtils.isBlank(schema) ? schema
                : (!StringUtils.isBlank(catalog) ? catalog : "database"), catalog, schema, null);
        if (!includeSymmetricTables) {
            Database symmetricTables = readSymmetricSchemaFromXml();
            Table[] tables = symmetricTables.getTables();
            for (Table symTable : tables) {
                for (Table table : database.getTables()) {
                    if (table.getName().equalsIgnoreCase(symTable.getName())) {
                        database.removeTable(table);
                    }
                }
            }

            Table[] allTables = database.getTables();
            for (Table table : allTables) {
                // Remove SYM_ON_ trigger tables for embedded databases
                if (table.getName().startsWith(tablePrefix.toUpperCase() + "_ON_")) {
                    database.removeTable(table);
                }
            }
        }

        return database;
    }

    /**
     * @return true if SQL was executed.
     */
    protected boolean createTablesIfNecessary() {
        Database symmetricTables = readSymmetricSchemaFromXml();
        try {
            log.info("TablesAutoUpdatingStart");
            String alterSql = getAlterSql(symmetricTables);
            if (!StringUtils.isBlank(alterSql)) {
                new SqlScript(alterSql, jdbcTemplate.getDataSource(), true, platform
                        .getPlatformInfo().getSqlCommandDelimiter(), null).execute();
                return true;
            } else {
                return false;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String getAlterSql(Database desiredModel) throws IOException {
        Database currentModel = new Database();
        Table[] tables = desiredModel.getTables();
        Database existingModel = readPlatformDatabase(true);
        for (Table table : tables) {
            Table currentVersion = existingModel.findTable(table.getName());
            if (currentVersion != null) {
                currentModel.addTable(currentVersion);
            }
        }
        SqlBuilder builder = platform.getSqlBuilder();
        StringWriter writer = new StringWriter();
        builder.setWriter(writer);
        builder.alterDatabase(currentModel, desiredModel, null);
        return writer.toString();
    }

    protected Database readSymmetricSchemaFromXml() {
        try {
            Database database = new DatabaseIO().read(new InputStreamReader(AbstractDbDialect.class
                    .getResource("/symmetric-schema.xml").openStream()));
            if (prefixConfigDatabase(database)) {
                log.info("TablesMissing");
            }
            return database;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void fixForeignKeys(Table table, String tablePrefix, boolean clone)
            throws CloneNotSupportedException {
        ForeignKey[] keys = table.getForeignKeys();
        for (ForeignKey key : keys) {
            if (clone) {
                table.removeForeignKey(key);
                key = (ForeignKey) key.clone();
                table.addForeignKey(key);
            }
            String prefixedName = tablePrefix + key.getForeignTableName();
            key.setForeignTableName(prefixedName);
            key.setName(tablePrefix + key.getName());
        }
    }

    public Platform getPlatform() {
        return this.platform;
    }

    public String getName() {
        return databaseName;
    }

    public String getVersion() {
        return databaseMajorVersion + "." + databaseMinorVersion;
    }

    public int getMajorVersion() {
        return databaseMajorVersion;
    }

    public int getMinorVersion() {
        return databaseMinorVersion;
    }

    public String getProductVersion() {
        return databaseProductVersion;
    }

    public String replaceTemplateVariables(DataEventType dml, Trigger trigger,
            TriggerHistory history, String targetString) {
        return sqlTemplate.replaceTemplateVariables(this, dml, trigger, history, tablePrefix,
                getTable(trigger.getSourceCatalogName(), trigger.getSourceSchemaName(), trigger
                        .getSourceTableName(), true), getDefaultCatalog(), getDefaultSchema(),
                targetString);
    }

    public boolean supportsGetGeneratedKeys() {
        if (supportsGetGeneratedKeys == null) {
            supportsGetGeneratedKeys = jdbcTemplate.execute(new ConnectionCallback<Boolean>() {
                public Boolean doInConnection(Connection conn) throws SQLException,
                        DataAccessException {
                    return conn.getMetaData().supportsGetGeneratedKeys();
                }
            });
        }
        return supportsGetGeneratedKeys;
    }

    public boolean supportsTransactionViews() {
        return supportsTransactionViews;
    }

    public boolean supportsReturningKeys() {
        return false;
    }

    public String getSelectLastInsertIdSql(String sequenceName) {
        throw new UnsupportedOperationException();
    }

    public long insertWithGeneratedKey(final String sql, final SequenceIdentifier sequenceId) {
        return insertWithGeneratedKey(jdbcTemplate, sql, sequenceId, null);
    }

    protected String getSequenceName(SequenceIdentifier identifier) {
        switch (identifier) {
        case OUTGOING_BATCH:
            return "sym_outgoing_batch_batch_id";
        case DATA:
            return "sym_data_data_id";
        case TRIGGER_HIST:
            return "sym_trigger_his_ger_hist_id";
        }
        return null;
    }

    protected String getSequenceKeyName(SequenceIdentifier identifier) {
        switch (identifier) {
        case OUTGOING_BATCH:
            return "batch_id";
        case DATA:
            return "data_id";
        case TRIGGER_HIST:
            return "trigger_hist_id";
        }
        return null;
    }

    protected Column[] orderColumns(String[] columnNames, Table table) {
        Column[] unorderedColumns = table.getColumns();
        Column[] orderedColumns = new Column[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            String name = columnNames[i];
            for (Column column : unorderedColumns) {
                if (column.getName().equalsIgnoreCase(name)) {
                    orderedColumns[i] = column;
                    break;
                }
            }
        }
        return orderedColumns;
    }

    public Object[] getObjectValues(BinaryEncoding encoding, Table table, String[] columnNames,
            String[] values) {
        Column[] metaData = orderColumns(columnNames, table);
        return getObjectValues(encoding, values, metaData);
    }

    public Object[] getObjectValues(BinaryEncoding encoding, String[] values,
            Column[] orderedMetaData) {
        List<Object> list = new ArrayList<Object>(values.length);
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            Object objectValue = value;
            Column column = orderedMetaData[i];

            if (column != null) {
                int type = column.getTypeCode();
                if ((value == null || (isEmptyStringNulled() && value.equals("")))
                        && column.isRequired() && column.isOfTextType()) {
                    objectValue = REQUIRED_FIELD_NULL_SUBSTITUTE;
                }
                if (value != null) {
                    if (type == Types.DATE && !isDateOverrideToTimestamp()) {
                        objectValue = new Date(getTime(value, TIMESTAMP_PATTERNS));
                    } else if (type == Types.TIMESTAMP
                            || (type == Types.DATE && isDateOverrideToTimestamp())) {
                        objectValue = new Timestamp(getTime(value, TIMESTAMP_PATTERNS));
                    } else if (type == Types.CHAR && isCharSpacePadded()) {
                        objectValue = StringUtils.rightPad(value.toString(), column.getSizeAsInt(),
                                ' ');
                    } else if (type == Types.INTEGER || type == Types.SMALLINT || type == Types.BIT) {
                        objectValue = Integer.valueOf(value);
                    } else if (type == Types.NUMERIC || type == Types.DECIMAL
                            || type == Types.FLOAT || type == Types.DOUBLE) {
                        // The number will have either one period or one comma
                        // for the decimal point, but we need a period
                        objectValue = new BigDecimal(value.replace(',', '.'));
                    } else if (type == Types.BOOLEAN) {
                        objectValue = value.equals("1") ? Boolean.TRUE : Boolean.FALSE;
                    } else if (type == Types.BLOB || type == Types.LONGVARBINARY
                            || type == Types.BINARY || type == Types.VARBINARY) {
                        if (encoding == BinaryEncoding.NONE) {
                            objectValue = value.getBytes();
                        } else if (encoding == BinaryEncoding.BASE64) {
                            objectValue = Base64.decodeBase64(value.getBytes());
                        } else if (encoding == BinaryEncoding.HEX) {
                            try {
                                objectValue = Hex.decodeHex(value.toCharArray());
                            } catch (DecoderException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else if (type == Types.TIME) {
                        objectValue = new Time(getTime(value, TIME_PATTERNS));
                    }
                }
                list.add(objectValue);
            }
        }
        return list.toArray();
    }

    private long getTime(String value, String[] pattern) {
        try {
            return DateUtils.parseDate(value, pattern).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public long insertWithGeneratedKey(final String sql, final SequenceIdentifier sequenceId,
            final PreparedStatementCallback<Object> callback) {
        return insertWithGeneratedKey(jdbcTemplate, sql, sequenceId, callback);
    }

    public long insertWithGeneratedKey(final JdbcTemplate template, final String sql,
            final SequenceIdentifier sequenceId, final PreparedStatementCallback<Object> callback) {
        return template.execute(new ConnectionCallback<Long>() {
            public Long doInConnection(Connection conn) throws SQLException, DataAccessException {

                long key = 0;
                PreparedStatement ps = null;
                try {
                    boolean supportsGetGeneratedKeys = supportsGetGeneratedKeys();
                    boolean supportsReturningKeys = supportsReturningKeys();
                    if (allowsNullForIdentityColumn()) {
                        if (supportsGetGeneratedKeys) {
                            ps = conn.prepareStatement(sql, new int[] { 1 });
                        } else if (supportsReturningKeys) {
                            ps = conn.prepareStatement(sql + " returning "
                                    + getSequenceKeyName(sequenceId));
                        } else {
                            ps = conn.prepareStatement(sql);
                        }
                    } else {
                        String replaceSql = sql.replaceFirst("\\(\\w*,", "(").replaceFirst(
                                "\\(null,", "(");
                        if (supportsGetGeneratedKeys) {
                            ps = conn.prepareStatement(replaceSql, Statement.RETURN_GENERATED_KEYS);
                        } else {
                            ps = conn.prepareStatement(replaceSql);
                        }
                    }
                    ps.setQueryTimeout(template.getQueryTimeout());
                    if (callback != null) {
                        callback.doInPreparedStatement(ps);
                    }

                    ResultSet rs = null;
                    if (supportsGetGeneratedKeys) {
                        ps.executeUpdate();
                        try {
                            rs = ps.getGeneratedKeys();
                            if (rs.next()) {
                                key = rs.getLong(1);
                            }
                        } finally {
                            JdbcUtils.closeResultSet(rs);
                        }
                    } else if (supportsReturningKeys) {
                        try {
                            rs = ps.executeQuery();
                            if (rs.next()) {
                                key = rs.getLong(1);
                            }
                        } finally {
                            JdbcUtils.closeResultSet(rs);
                        }
                    } else {
                        Statement st = null;
                        ps.executeUpdate();
                        try {
                            st = conn.createStatement();
                            rs = st
                                    .executeQuery(getSelectLastInsertIdSql(getSequenceName(sequenceId)));
                            if (rs.next()) {
                                key = rs.getLong(1);
                            }
                        } finally {
                            JdbcUtils.closeResultSet(rs);
                            JdbcUtils.closeStatement(st);
                        }
                    }
                } finally {
                    JdbcUtils.closeStatement(ps);
                }
                return key;
            }
        });
    }

    public Object createSavepoint() {
        return transactionTemplate.execute(new TransactionCallback<Object>() {
            public Object doInTransaction(TransactionStatus transactionstatus) {
                return transactionstatus.createSavepoint();
            }
        });
    }

    public Object createSavepointForFallback() {
        if (requiresSavepointForFallback()) {
            return createSavepoint();
        }
        return null;
    }

    public void rollbackToSavepoint(final Object savepoint) {
        if (savepoint != null) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionstatus) {
                    transactionstatus.rollbackToSavepoint(savepoint);
                }
            });
        }
    }

    public void releaseSavepoint(final Object savepoint) {
        if (savepoint != null) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionstatus) {
                    transactionstatus.releaseSavepoint(savepoint);
                }
            });
        }
    }

    public boolean requiresSavepointForFallback() {
        return false;
    }

    public void disableSyncTriggers() {
        disableSyncTriggers(null);
    }

    public boolean supportsTransactionId() {
        return false;
    }

    public boolean isBlobSyncSupported() {
        return true;
    }

    public boolean isClobSyncSupported() {
        return true;
    }

    public boolean isTransactionIdOverrideSupported() {
        return true;
    }

    public boolean storesUpperCaseNamesInCatalog() {
        return false;
    }

    public boolean storesLowerCaseNamesInCatalog() {
        return false;
    }

    public void setSqlTemplate(SqlTemplate sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    public SQLErrorCodeSQLExceptionTranslator getSqlErrorTranslator() {
        return sqlErrorTranslator;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public int getStreamingResultsFetchSize() {
        return streamingResultsFetchSize;
    }

    public void setStreamingResultsFetchSize(int streamingResultsFetchSize) {
        this.streamingResultsFetchSize = streamingResultsFetchSize;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public String getEngineName() {
        return parameterService.getString(ParameterConstants.ENGINE_NAME).toLowerCase();
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setParameterService(IParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public String getIdentifierQuoteString() {
        return identifierQuoteString;
    }

    public boolean supportsOpenCursorsAcrossCommit() {
        return true;
    }

    public String getInitialLoadTableAlias() {
        return "t";
    }

    public String preProcessTriggerSqlClause(String sqlClause) {
        return sqlClause;
    }

    public int getRouterDataPeekAheadCount() {
        return parameterService.getInt(ParameterConstants.ROUTING_PEEK_AHEAD_WINDOW);
    }

    /**
     * Returns the current schema name
     * 
     * @return String
     */
    public String getDefaultSchema() {
        return StringUtils.isBlank(defaultSchema) ? null : defaultSchema;
    }

    /**
     * Sets the current schema name from properties file
     * 
     * @param currentSchema
     */
    public void setDefaultSchema(String currentSchema) {
        this.defaultSchema = currentSchema;
    }

    public void truncateTable(String tableName) {
        jdbcTemplate.update("truncate table " + tableName);
    }

    /**
     * @return the lobHandler.
     */
    public LobHandler getLobHandler() {
        return lobHandler;
    }

    /**
     * @param lobHandler
     *            The lobHandler to set.
     */
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }

    public boolean areDatabaseTransactionsPendingSince(long time) {
        throw new UnsupportedOperationException();
    }

    public long getDatabaseTime() {
        try {
            return jdbcTemplate.queryForObject(
                    "select current_timestamp from " + tablePrefix + "_node_identity",
                    java.util.Date.class).getTime();

        } catch (Exception ex) {
            log.error(ex);
            return System.currentTimeMillis();
        }
    }
    
    public String getSourceNodeExpression() {
        return null;
    }
}