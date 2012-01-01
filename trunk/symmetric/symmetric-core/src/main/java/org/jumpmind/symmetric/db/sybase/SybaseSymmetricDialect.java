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


package org.jumpmind.symmetric.db.sybase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jumpmind.db.BinaryEncoding;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.db.AbstractSymmetricDialect;
import org.jumpmind.symmetric.db.ISymmetricDialect;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.model.TriggerHistory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

/*
 * Sybase dialect was tested with jconn4 JDBC driver.
 * Based on the MsSqlSymmetricDialect.
 *
 *  disk resize name = master, size = 16384
 *  create database symmetricclient on master = '30M'
 *  
 */
public class SybaseSymmetricDialect extends AbstractSymmetricDialect implements ISymmetricDialect {
    
   

    public SybaseSymmetricDialect() {
        this.triggerText = new SybaseTriggerText();
    }
    
    @Override
    protected boolean allowsNullForIdentityColumn() {
        return false;
    }  

    @Override
    public void removeTrigger(StringBuilder sqlBuffer, final String catalogName, String schemaName,
            final String triggerName, String tableName, TriggerHistory oldHistory) {
        schemaName = schemaName == null ? "" : (schemaName + ".");
        final String sql = "drop trigger " + schemaName + triggerName;
        logSql(sql, sqlBuffer);
        if (parameterService.is(ParameterConstants.AUTO_SYNC_TRIGGERS)) {
            jdbcTemplate.execute(new ConnectionCallback<Object>() {
                public Object doInConnection(Connection con) throws SQLException, DataAccessException {
                    String previousCatalog = con.getCatalog();
                    Statement stmt = null;
                    try {
                        if (catalogName != null) {
                            con.setCatalog(catalogName);
                        }
                        stmt = con.createStatement();
                        stmt.execute(sql);
                    } catch (Exception e) {
                        log.warn("TriggerDropError", triggerName, e.getMessage());
                    } finally {
                        if (catalogName != null) {
                            con.setCatalog(previousCatalog);
                        }
                        try {
                            stmt.close();
                        } catch (Exception e) {
                        }
                    }
                    return Boolean.FALSE;
                }
            });
        }
    }

    @Override
    protected String switchCatalogForTriggerInstall(String catalog, Connection c) throws SQLException {
        if (catalog != null) {
            String previousCatalog = c.getCatalog();
            c.setCatalog(catalog);
            return previousCatalog;
        } else {
            return null;
        }
    }

    @Override
    public BinaryEncoding getBinaryEncoding() {
        return BinaryEncoding.BASE64;
    }

    @Override
    protected boolean doesTriggerExistOnPlatform(final String catalogName, String schema, String tableName,
            final String triggerName) {
        return jdbcTemplate.execute(new ConnectionCallback<Boolean>() {
            public Boolean doInConnection(Connection con) throws SQLException, DataAccessException {
                String previousCatalog = con.getCatalog();
                PreparedStatement stmt = con
                        .prepareStatement("select count(*) from sysobjects where type = 'TR' AND name = ?");
                try {
                    if (catalogName != null) {
                        con.setCatalog(catalogName);
                    }
                    stmt.setString(1, triggerName);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        return count > 0;
                    }
                } finally {
                    if (catalogName != null) {
                        con.setCatalog(previousCatalog);
                    }
                    stmt.close();
                }
                return Boolean.FALSE;
            }
        });
    }

    public void disableSyncTriggers(ISqlTransaction transaction, String nodeId) {
        if (nodeId == null) {
            nodeId = "";
        }
        transaction.execute("exec set clientapplname 'SymmetricDS'");
        transaction.execute("exec set clientname '" + nodeId + "'");
    }

    public void enableSyncTriggers(ISqlTransaction transaction) {
        transaction.execute("exec set clientapplname null");
        transaction.execute("exec set clientname null");
    }

    public String getSyncTriggersExpression() {
        return "$(defaultCatalog)dbo."+tablePrefix+"_triggers_disabled(0) = 0";
    }

    @Override
    public String getTransactionTriggerExpression(String defaultCatalog, String defaultSchema, Trigger trigger) {
    	return platform.getDefaultCatalog() + ".dbo."+tablePrefix+"_txid(0)";
    }

    @Override
    public boolean supportsTransactionId() {
        return true;
    }

    @Override
    public boolean isTransactionIdOverrideSupported() {
        return true;
    }

    public void purge() {
    }

    public boolean needsToSelectLobData() {
        return true;
    }

}