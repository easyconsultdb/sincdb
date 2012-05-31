package org.jumpmind.symmetric.ddl.platform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.jumpmind.symmetric.ddl.TestPlatformBase;
import org.jumpmind.symmetric.ddl.platform.db2.Db2Platform;

/*
 * Tests the DB2 platform.
 * 
 * @version $Revision: 231110 $
 */
public class TestDB2Platform extends TestPlatformBase
{
    /* The database schema for testing escaping of character sequences. */
    public static final String COLUMN_CHAR_SEQUENCES_TO_ESCAPE =
        "<?xml version='1.0' encoding='ISO-8859-1'?>\n" +
        "<database name='escapetest'>\n" +
        "  <table name='escapedcharacters'>\n" +
        "    <column name='COL_PK' type='INTEGER' primaryKey='true'/>\n" +
        "    <column name='COL_TEXT' type='VARCHAR' size='128' default='&#39;'/>\n" +
        "  </table>\n" +
        "</database>";

    /*
     * {@inheritDoc}
     */
    protected String getDatabaseName()
    {
        return Db2Platform.DATABASENAME;
    }

    /*
     * Tests the column types.
     */
    public void testColumnTypes() throws Exception
    {
        assertEqualsIgnoringWhitespaces(
            "DROP TABLE \"coltype\";\n"+
            "CREATE TABLE \"coltype\"\n"+
            "(\n"+
            "    \"COL_ARRAY\"         BLOB,\n"+
            "    \"COL_BIGINT\"        BIGINT,\n"+
            "    \"COL_BINARY\"        CHAR(254) FOR BIT DATA,\n"+
            "    \"COL_BIT\"           SMALLINT,\n"+
            "    \"COL_BLOB\"          BLOB,\n"+
            "    \"COL_BOOLEAN\"       SMALLINT,\n"+
            "    \"COL_CHAR\"          CHAR(15),\n"+
            "    \"COL_CLOB\"          CLOB,\n"+
            "    \"COL_DATALINK\"      DATALINK,\n"+
            "    \"COL_DATE\"          DATE,\n"+
            "    \"COL_DECIMAL\"       DECIMAL(15,3),\n"+
            "    \"COL_DECIMAL_NOSCALE\" DECIMAL(15,0),\n"+       // only 18 characters allowed for identifiers
            "    \"COL_DISTINCT\"      DISTINCT,\n"+
            "    \"COL_DOUBLE\"        DOUBLE,\n"+
            "    \"COL_FLOAT\"         DOUBLE,\n"+
            "    \"COL_INTEGER\"       INTEGER,\n"+
            "    \"COL_JAVA_OBJECT\"   BLOB,\n"+
            "    \"COL_LONGVARBINARY\" LONG VARCHAR FOR BIT DATA,\n"+
            "    \"COL_LONGVARCHAR\"   LONG VARCHAR,\n"+
            "    \"COL_NULL\"          LONG VARCHAR FOR BIT DATA,\n"+
            "    \"COL_NUMERIC\"       DECIMAL(15,0),\n"+
            "    \"COL_OTHER\"         BLOB,\n"+
            "    \"COL_REAL\"          REAL,\n"+
            "    \"COL_REF\"           REF,\n"+
            "    \"COL_SMALLINT\"      SMALLINT,\n"+
            "    \"COL_STRUCT\"        BLOB,\n"+
            "    \"COL_TIME\"          TIME,\n"+
            "    \"COL_TIMESTAMP\"     TIMESTAMP,\n"+
            "    \"COL_TINYINT\"       SMALLINT,\n"+
            "    \"COL_VARBINARY\"     VARCHAR(15) FOR BIT DATA,\n"+
            "    \"COL_VARCHAR\"       VARCHAR(15)\n"+
            ");\n",
            createTestDatabase(COLUMN_TEST_SCHEMA));
    }


    /*
     * Tests the column constraints.
     */
    public void testColumnConstraints() throws Exception
    {
        assertEqualsIgnoringWhitespaces(
            "DROP TABLE \"constraints\";\n" +
            "CREATE TABLE \"constraints\"\n"+
            "(\n"+
            "    \"COL_PK\"            VARCHAR(32),\n"+
            "    \"COL_PK_AUTO_INCR\"  INTEGER GENERATED BY DEFAULT AS IDENTITY,\n"+
            "    \"COL_NOT_NULL\"      CHAR(100) FOR BIT DATA NOT NULL,\n"+
            "    \"COL_NOT_NULL_DEFAULT\" DOUBLE DEFAULT -2.0 NOT NULL,\n"+   // only 18 characters allowed for identifiers
            "    \"COL_DEFAULT\"       CHAR(4) DEFAULT 'test',\n"+
            "    \"COL_AUTO_INCR\"     BIGINT GENERATED BY DEFAULT AS IDENTITY,\n"+
            "    PRIMARY KEY (\"COL_PK\", \"COL_PK_AUTO_INCR\")\n"+
            ");\n",
            createTestDatabase(COLUMN_CONSTRAINT_TEST_SCHEMA));
    }


    /*
     * Tests the table constraints.
     */
    public void testTableConstraints() throws Exception
    {
        assertEqualsIgnoringWhitespaces(
            "ALTER TABLE \"table3\" DROP CONSTRAINT \"testfk\";\n"+
            "ALTER TABLE \"table2\" DROP CONSTRAINT \"table2_FK_COL_FK_1_COL_FK_2_table1\";\n"+
            "DROP TABLE \"table3\";\n"+
            "DROP TABLE \"table2\";\n"+
            "DROP TABLE \"table1\";\n"+
            "CREATE TABLE \"table1\"\n"+
            "(\n"+
            "    \"COL_PK_1\"    VARCHAR(32) NOT NULL,\n"+
            "    \"COL_PK_2\"    INTEGER,\n"+
            "    \"COL_INDEX_1\" CHAR(100) FOR BIT DATA NOT NULL,\n"+
            "    \"COL_INDEX_2\" DOUBLE NOT NULL,\n"+
            "    \"COL_INDEX_3\" CHAR(4),\n"+
            "    PRIMARY KEY (\"COL_PK_1\", \"COL_PK_2\")\n"+
            ");\n"+
            "CREATE INDEX \"testindex1\" ON \"table1\" (\"COL_INDEX_2\");\n"+
            "CREATE UNIQUE INDEX \"testindex2\" ON \"table1\" (\"COL_INDEX_3\", \"COL_INDEX_1\");\n"+
            "CREATE TABLE \"table2\"\n"+
            "(\n"+
            "    \"COL_PK\"   INTEGER,\n"+
            "    \"COL_FK_1\" INTEGER,\n"+
            "    \"COL_FK_2\" VARCHAR(32) NOT NULL,\n"+
            "    PRIMARY KEY (\"COL_PK\")\n"+
            ");\n"+
            "CREATE TABLE \"table3\"\n"+
            "(\n"+
            "    \"COL_PK\" VARCHAR(16),\n"+
            "    \"COL_FK\" INTEGER NOT NULL,\n"+
            "    PRIMARY KEY (\"COL_PK\")\n"+
            ");\n"+
            "ALTER TABLE \"table2\" ADD CONSTRAINT \"table2_FK_COL_FK_1_COL_FK_2_table1\" FOREIGN KEY (\"COL_FK_1\", \"COL_FK_2\") REFERENCES \"table1\" (\"COL_PK_2\", \"COL_PK_1\");\n"+
            "ALTER TABLE \"table3\" ADD CONSTRAINT \"testfk\" FOREIGN KEY (\"COL_FK\") REFERENCES \"table2\" (\"COL_PK\");\n",
            createTestDatabase(TABLE_CONSTRAINT_TEST_SCHEMA));
    }

    /*
     * Tests the proper escaping of character sequences where Db2 requires it.
     */
    public void testCharacterEscaping() throws Exception
    {
        assertEqualsIgnoringWhitespaces(
            "DROP TABLE \"escapedcharacters\";\n"+
            "CREATE TABLE \"escapedcharacters\"\n"+
            "(\n"+
            "    \"COL_PK\"   INTEGER,\n"+
            "    \"COL_TEXT\" VARCHAR(128) DEFAULT '\'\'',\n"+
            "    PRIMARY KEY (\"COL_PK\")\n"+
            ");\n",
            createTestDatabase(COLUMN_CHAR_SEQUENCES_TO_ESCAPE));
    }
}
