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
package org.jumpmind.symmetric.db.hsqldb;

import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.db.AbstractEmbeddedDbDialect;
import org.jumpmind.symmetric.db.BinaryEncoding;
import org.jumpmind.symmetric.db.IDbDialect;
import org.jumpmind.symmetric.ddl.Platform;
import org.jumpmind.symmetric.ddl.model.Table;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.model.TriggerHistory;

public class HsqlDbDialect extends AbstractEmbeddedDbDialect implements IDbDialect {

    public static String DUAL_TABLE = "DUAL";

    private boolean enforceStrictSize = true;
   
    @Override
    public void init(Platform pf, int queryTimeout) {
        super.init(pf, queryTimeout);
        jdbcTemplate.update("SET WRITE_DELAY 100 MILLIS");
        jdbcTemplate.update("SET PROPERTY \"hsqldb.default_table_type\" 'cached'");
        jdbcTemplate.update("SET PROPERTY \"sql.enforce_strict_size\" " + enforceStrictSize);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                jdbcTemplate.update("SHUTDOWN");
            }
        });
        createDummyDualTable();        
    }

    @Override
    protected boolean doesTriggerExistOnPlatform(String catalogName, String schemaName, String tableName,
            String triggerName) {
        boolean exists = (jdbcTemplate.queryForInt(
                "select count(*) from INFORMATION_SCHEMA.SYSTEM_TRIGGERS WHERE TRIGGER_NAME = ?",
                new Object[] { triggerName }) > 0)
                || (jdbcTemplate.queryForInt(
                        "select count(*) from INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TABLE_NAME = ?",
                        new Object[] { String.format("%s_CONFIG", triggerName) }) > 0);
        return exists;
    }

    /**
     * This is for use in the java triggers so we can create a virtual table w/
     * old and new columns values to bump SQL expressions up against.
     */
    private void createDummyDualTable() {
        Table table = getTable(null, null, DUAL_TABLE, false);
        if (table == null) {
            jdbcTemplate.update("CREATE MEMORY TABLE " + DUAL_TABLE + "(DUMMY VARCHAR(1))");
            jdbcTemplate.update("INSERT INTO " + DUAL_TABLE + " VALUES(NULL)");
            jdbcTemplate.update("SET TABLE " + DUAL_TABLE + " READONLY TRUE");
        }

    }

    @Override
    public void removeTrigger(StringBuilder sqlBuffer, String catalogName, String schemaName, String triggerName,
            String tableName, TriggerHistory oldHistory) {
        final String dropSql = String.format("DROP TRIGGER %s", triggerName);
        logSql(dropSql, sqlBuffer);

        final String dropTable = String.format("DROP TABLE IF EXISTS %s_CONFIG", triggerName);
        logSql(dropTable, sqlBuffer);

        if (parameterService.is(ParameterConstants.AUTO_SYNC_TRIGGERS)) {
            try {
                int count = jdbcTemplate.update(dropSql);
                if (count > 0) {
                    log.info("TriggerDropped", triggerName);
                }
            } catch (Exception e) {
                log.warn("TriggerDropError", triggerName, e.getMessage());
            }
            try {
                int count = jdbcTemplate.update(dropTable);
                if (count > 0) {
                    log.info("TableDropped", triggerName);
                }
            } catch (Exception e) {
                log.warn("TriggerDropError", triggerName, e.getMessage());
            }
        }
    }

    @Override
    public boolean isBlobSyncSupported() {
        return true;
    }

    @Override
    public boolean isClobSyncSupported() {
        return true;
    }

    public void disableSyncTriggers(String nodeId) {
        jdbcTemplate.execute("CALL " + tablePrefix + "_set_session('sync_prevented','1')");
        jdbcTemplate.execute("CALL " + tablePrefix + "_set_session('node_value','"+nodeId+"')");
    }

    public void enableSyncTriggers() {
        jdbcTemplate.execute("CALL " + tablePrefix + "_set_session('sync_prevented',null)");
        jdbcTemplate.execute("CALL " + tablePrefix + "_set_session('node_value',null)");
    }

    public String getSyncTriggersExpression() {
        return " " + tablePrefix + "_get_session(''sync_prevented'') is null ";
    }

    /**
     * An expression which the java trigger can string replace
     */
    @Override
    public String getTransactionTriggerExpression(String defaultCatalog, String defaultSchema, Trigger trigger) {
        // TODO Get I use a temporary table and a randomly generated GUID?
        return "null";
    }

    @Override
    public String getSelectLastInsertIdSql(String sequenceName) {
        return "call IDENTITY()";
    }

    @Override
    public BinaryEncoding getBinaryEncoding() {
        return BinaryEncoding.BASE64;
    }

    public boolean isNonBlankCharColumnSpacePadded() {
        return enforceStrictSize;
    }

    public boolean isCharColumnSpaceTrimmed() {
        return false;
    }

    public boolean isEmptyStringNulled() {
        return false;
    }

    @Override
    public boolean storesUpperCaseNamesInCatalog() {
        return true;
    }

    @Override
    public boolean supportsTransactionId() {
        return false;
    }

    @Override
    protected boolean allowsNullForIdentityColumn() {
        return false;
    }

    @Override
    public void truncateTable(String tableName) {
        jdbcTemplate.update("delete from " + tableName);
    }

    @Override
    public boolean canGapsOccurInCapturedDataIds() {
        return false;
    }

}
