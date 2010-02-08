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

package org.jumpmind.symmetric.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.PreparedStatementCreator;

public class MaxRowsStatementCreator implements PreparedStatementCreator {

    private String sql;

    private int maxRows;

    public MaxRowsStatementCreator(String sql, int maxRows) {
        this.sql = sql;
        this.maxRows = maxRows;
    }

    public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
        PreparedStatement st = conn.prepareStatement(sql);
        st.setMaxRows(maxRows);
        return st;
    }

}
