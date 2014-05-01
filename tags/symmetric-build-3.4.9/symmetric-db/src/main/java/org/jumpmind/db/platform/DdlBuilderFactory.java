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
package org.jumpmind.db.platform;

import org.jumpmind.db.platform.db2.Db2DdlBuilder;
import org.jumpmind.db.platform.derby.DerbyDdlBuilder;
import org.jumpmind.db.platform.firebird.FirebirdDdlBuilder;
import org.jumpmind.db.platform.greenplum.GreenplumDdlBuilder;
import org.jumpmind.db.platform.h2.H2DdlBuilder;
import org.jumpmind.db.platform.hsqldb.HsqlDbDdlBuilder;
import org.jumpmind.db.platform.hsqldb2.HsqlDb2DdlBuilder;
import org.jumpmind.db.platform.informix.InformixDdlBuilder;
import org.jumpmind.db.platform.interbase.InterbaseDdlBuilder;
import org.jumpmind.db.platform.mssql.MsSqlDdlBuilder;
import org.jumpmind.db.platform.mysql.MySqlDdlBuilder;
import org.jumpmind.db.platform.oracle.OracleDdlBuilder;
import org.jumpmind.db.platform.postgresql.PostgreSqlDdlBuilder;
import org.jumpmind.db.platform.sqlite.SqliteDdlBuilder;
import org.jumpmind.db.platform.sybase.SybaseDdlBuilder;

/**
 * Factory that creates {@link IDdlBuilder} from {@link DatabaseNamesConstants} values.
 */
final public class DdlBuilderFactory {

    private DdlBuilderFactory() {
    }

    /**
     * @param databaseName see {@link DatabaseNamesConstants}
     * @return the associated ddl builder 
     */
    public static final IDdlBuilder createDdlBuilder(String databaseName) {
        if (DatabaseNamesConstants.DB2.equals(databaseName)) {
            return new Db2DdlBuilder();
        } else if (DatabaseNamesConstants.DERBY.equals(databaseName)) {
            return new DerbyDdlBuilder();
        } else if (DatabaseNamesConstants.FIREBIRD.equals(databaseName)) {
            return new FirebirdDdlBuilder();
        } else if (DatabaseNamesConstants.GREENPLUM.equals(databaseName)) {
            return new GreenplumDdlBuilder();
        } else if (DatabaseNamesConstants.H2.equals(databaseName)) {
            return new H2DdlBuilder();
        } else if (DatabaseNamesConstants.HSQLDB.equals(databaseName)) {
            return new HsqlDbDdlBuilder();
        } else if (DatabaseNamesConstants.HSQLDB2.equals(databaseName)) {
            return new HsqlDb2DdlBuilder();
        } else if (DatabaseNamesConstants.INFORMIX.equals(databaseName)) {
            return new InformixDdlBuilder();
        } else if (DatabaseNamesConstants.INTERBASE.equals(databaseName)) {
            return new InterbaseDdlBuilder();
        } else if (DatabaseNamesConstants.MSSQL.equals(databaseName)) {
            return new MsSqlDdlBuilder();
        } else if (DatabaseNamesConstants.MYSQL.equals(databaseName)) {
            return new MySqlDdlBuilder();
        } else if (DatabaseNamesConstants.ORACLE.equals(databaseName)) {
            return new OracleDdlBuilder();
        } else if (DatabaseNamesConstants.POSTGRESQL.equals(databaseName)) {
            return new PostgreSqlDdlBuilder();
        } else if (DatabaseNamesConstants.SQLITE.equals(databaseName)) {
            return new SqliteDdlBuilder();  
        } else if (DatabaseNamesConstants.SYBASE.equals(databaseName)) {
            return new SybaseDdlBuilder();
        } else {
            return null;
        }
    }

}