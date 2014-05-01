package org.jumpmind.symmetric.jdbc.db.oracle;

import org.jumpmind.symmetric.core.db.DmlStatement;
import org.jumpmind.symmetric.core.model.Column;

public class OracleDmlStatement extends DmlStatement {

    public OracleDmlStatement(DmlType type, String catalogName, String schemaName,
            String tableName, Column[] keys, Column[] columns, Column[] preFilteredColumns,
            boolean isDateOverrideToTimestamp, String identifierQuoteString) {
        super(type, catalogName, schemaName, tableName, keys, columns, preFilteredColumns,
                isDateOverrideToTimestamp, identifierQuoteString);
    }
    
    @Override
    public void appendColumnQuestions(StringBuilder sql, Column[] columns) {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i] != null) {
                if (columns[i].getTypeCode() == -101) {
                    sql.append("TO_TIMESTAMP_TZ(?, 'YYYY-MM-DD HH24:MI:SS.FF TZH:TZM')").append(",");
                } else {
                    sql.append("?").append(",");
                }
            }
        }
        
        if (columns.length > 0) {
            sql.replace(sql.length()-1, sql.length(), "");
        }
    }
    
    @Override
    public void appendColumnEquals(StringBuilder sql, Column[] columns, String separator) {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i] != null) {
                if (columns[i].getTypeCode() == -101) {
                    sql.append(quote).append(columns[i].getName()).append(quote)
                            .append(" = TO_TIMESTAMP_TZ(?, 'YYYY-MM-DD HH24:MI:SS.FF TZH:TZM')").append(separator);
                } else {
                    sql.append(quote).append(columns[i].getName()).append(quote).append(" = ?").append(separator);
                }
            }
        }
        
        if (columns.length > 0) {
            sql.replace(sql.length()-separator.length(), sql.length(), "");
        }
    }


}