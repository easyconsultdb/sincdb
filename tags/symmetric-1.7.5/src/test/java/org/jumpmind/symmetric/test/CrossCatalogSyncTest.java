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
package org.jumpmind.symmetric.test;

import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.service.IConfigurationService;
import org.jumpmind.symmetric.test.ParameterizedSuite.ParameterMatcher;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class CrossCatalogSyncTest extends AbstractDatabaseTest {

    public CrossCatalogSyncTest(String db) {
        super(db);
    }

    public CrossCatalogSyncTest() throws Exception {
        super();
    }

    @Test
    @ParameterMatcher("mysql")
    public void testCrossCatalogSyncOnMySQL() {
        testCrossCatalogSyncOnMySQL(false, true);
    }

    @Ignore
    @Test
    @ParameterMatcher("mysql")
    public void testCrossSchemaSyncOnMySQL() {
        // there really is no such thing as a schema on mysql.  this should behave just like setting the catalog element.
        testCrossCatalogSyncOnMySQL(true, false);
    }

    @Ignore
    @Test
    @ParameterMatcher("mysql")
    public void testCrossSchemaCatalogSyncOnMySQL() {
        testCrossCatalogSyncOnMySQL(true, true);
    }

    protected void testCrossCatalogSyncOnMySQL(boolean schema, boolean catalog) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        jdbcTemplate.update("drop database if exists other ");
        jdbcTemplate.update("create database other");
        String db = (String) jdbcTemplate.queryForObject("select database()", String.class);
        jdbcTemplate.update("use other");
        jdbcTemplate.update("create table other_table (id char(5) not null, name varchar(40), primary key(id))");
        jdbcTemplate.update("use " + db);
        IConfigurationService configService = find(Constants.CONFIG_SERVICE);
        Trigger trigger = new Trigger();
        trigger.setChannelId("other");
        trigger.setSourceGroupId(TestConstants.TEST_CONTINUOUS_NODE_GROUP);
        trigger.setTargetGroupId(TestConstants.TEST_CONTINUOUS_NODE_GROUP);
        if (catalog) {
            trigger.setSourceCatalogName("other");
        }
        if (schema) {
            trigger.setSourceSchemaName("other");
        }
        trigger.setSourceTableName("other_table");
        trigger.setSyncOnInsert(true);
        trigger.setSyncOnUpdate(true);
        trigger.setSyncOnDelete(true);
        configService.insert(trigger);
        getSymmetricEngine().syncTriggers();
        jdbcTemplate.update("insert into other.other_table values('00000','first row')");
        Assert.assertEquals("The data event from the other database's other_table was not captured.", jdbcTemplate
                .queryForInt("select count(*) from sym_data_event where channel_id='other'"), 1);
    }

    @Ignore
    @Test
    @ParameterMatcher("mssql")
    public void testCrossCatalogSyncOnMsSql() {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        try {
            jdbcTemplate.update("drop database other");
        } catch (Exception e) { }
        jdbcTemplate.update("create database other");
        String db = (String) jdbcTemplate.queryForObject("select db_name()", String.class);
        jdbcTemplate.update("use other");
        jdbcTemplate.update("create table other_table (id char(5) not null, name varchar(40), primary key(id))");
        jdbcTemplate.update("use " + db);
        IConfigurationService configService = find(Constants.CONFIG_SERVICE);
        Trigger trigger = new Trigger();
        trigger.setChannelId("other");
        trigger.setSourceGroupId(TestConstants.TEST_CONTINUOUS_NODE_GROUP);
        trigger.setTargetGroupId(TestConstants.TEST_CONTINUOUS_NODE_GROUP);
        trigger.setSourceCatalogName("other");
        trigger.setSourceSchemaName("dbo");
        trigger.setSourceTableName("other_table");
        trigger.setSyncOnInsert(true);
        trigger.setSyncOnUpdate(true);
        trigger.setSyncOnDelete(true);
        configService.insert(trigger);
        getSymmetricEngine().syncTriggers();
        jdbcTemplate.update("insert into other.dbo.other_table values('00000','first row')");
        Assert.assertEquals("The data event from the other database's other_table was not captured.", 1, jdbcTemplate
                .queryForInt("select count(*) from sym_data_event where channel_id='other'"));
    }

    @Ignore
    @Test
    @ParameterMatcher("mssql")
    public void testCrossSchemaSyncOnMsSql() {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        try {            
            jdbcTemplate.update("drop table other.other_table2");
        } catch (Exception e) { }
        try {            
            jdbcTemplate.update("drop schema other");
        } catch (Exception e) { }
        jdbcTemplate.update("create schema other");
        jdbcTemplate.update("create table other.other_table2 (id char(5) not null, name varchar(40), primary key(id))");
        IConfigurationService configService = find(Constants.CONFIG_SERVICE);
        Trigger trigger = new Trigger();
        trigger.setChannelId("other");
        trigger.setSourceGroupId(TestConstants.TEST_CONTINUOUS_NODE_GROUP);
        trigger.setTargetGroupId(TestConstants.TEST_CONTINUOUS_NODE_GROUP);
        trigger.setSourceSchemaName("other");
        trigger.setSourceTableName("other_table2");
        trigger.setSyncOnInsert(true);
        trigger.setSyncOnUpdate(true);
        trigger.setSyncOnDelete(true);
        configService.insert(trigger);
        getSymmetricEngine().syncTriggers();
        jdbcTemplate.update("insert into other.other_table2 values('00000','first row')");
        Assert.assertEquals("The data event from the other database's other_table was not captured.", 1, jdbcTemplate
                .queryForInt("select count(*) from sym_data where table_name='other_table2'"));
    }

}