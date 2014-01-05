/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Chris Henson <chenson42@users.sourceforge.net>
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

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.Table;
import org.jumpmind.symmetric.load.IColumnFilter;
import org.jumpmind.symmetric.model.DataEventType;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.model.TriggerHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

public interface IDbDialect {

    public void initTrigger(DataEventType dml, Trigger config,
            TriggerHistory audit, String tablePrefix, Table table);

    @Deprecated
    public void removeTrigger(String schemaName, String triggerName);
    
    /**
     * Get the name of this symmetric instance.  This can be set in symmetric.properties using the symmetric.runtime.engine.name property.
     */
    public String getEngineName();
    
    public void removeTrigger(String schemaName, String triggerName, String tableName);

    /**
     * This is called by the data loader each time the table context changes, giving the dialect an opportunity to do any pre loading work.  Only one 
     * table is active at any one point.
     */
    public void prepareTableForDataLoad(Table table);

    /**
     * This is called by the data loader each time the table context changes away from a table or when the the data loader is closed, giving the dialect
     * an opportunity to do any post loading work for the given table. 
     */
    public void cleanupAfterDataLoad(Table table);

    public void initConfigDb(String tablePrefix);

    public Platform getPlatform();
    
    public String getName();
    
    public String getVersion();
    
    public BinaryEncoding getBinaryEncoding();

    public boolean doesTriggerExist(String schema, String tableName, String triggerName);

    public Table getMetaDataFor(String catalog, String schema, final String tableName, boolean useCache);
    
    public String getTransactionTriggerExpression();

    public String createInitalLoadSqlFor(Node node, Trigger config);
    
    public String createPurgeSqlFor(Node node, Trigger trig);
    
    public String createCsvDataSql(Trigger trig, String whereClause);
    
    public String createCsvPrimaryKeySql(Trigger trig, String whereClause);

    public boolean isCharSpacePadded();
    
    public boolean isCharSpaceTrimmed();
    
    public boolean isEmptyStringNulled();
    
    /**
     * Get the maximum size the name of a trigger can be for the database platform.  If the generated symmetric trigger name is greater
     * than the max trigger name, symmetric will truncate the name, then log a warning suggesting that you might want to provide your own
     * name.
     */
    public int getMaxTriggerNameLength();    
    
    public boolean supportsMixedCaseNamesInCatalog();
    
    public boolean supportsTransactionId();
    
    public boolean requiresSavepointForFallback();

    public Object createSavepoint();

    public Object createSavepointForFallback();
    
    public void rollbackToSavepoint(Object savepoint);
    
    public void releaseSavepoint(Object savepoint);
    
    public IColumnFilter getDatabaseColumnFilter();

    /**
     * Implement this if the database has some type of cleanup functionality that needs to be 
     * run when dropping database objects.  An example is Oracle's 'purge recyclebin'
     */
    public void purge();
    
    public SQLErrorCodeSQLExceptionTranslator getSqlErrorTranslator();
    
    public void disableSyncTriggers();

    public void enableSyncTriggers();
    
    public String getSyncTriggersExpression();

    public String getDefaultSchema();
    
    public String getDefaultCatalog();
    
    public int getStreamingResultsFetchSize();
    
    public JdbcTemplate getJdbcTemplate();
    
    public String getCreateSymmetricDDL();
    
    public String getCreateTableXML(Trigger trig);
    
    public String getCreateTableSQL(Trigger trig);
    
    public boolean isBlobSyncSupported();
    
    public boolean isBlobOverrideToBinary();
    
    public boolean isClobSyncSupported();
    
    public void createTables(String xml);
    
    public String getSelectLastInsertIdSql(String sequenceName);
    
    public long insertWithGeneratedKey(final String sql, final String sequenceName);
    
    public long insertWithGeneratedKey(final String sql, final String sequenceName,
            final PreparedStatementCallback psCallback);
    
    /**
     * Get the string prepended to the Symmetric configuration tables.
     * @return
     */
    public String getTablePrefix();
    
    /**
     * Give access to the templating mechanism that is used for trigger creation.
     */
    public String replaceTemplateVariables(DataEventType dml, Trigger trigger,
            TriggerHistory history, String targetString);
    
}