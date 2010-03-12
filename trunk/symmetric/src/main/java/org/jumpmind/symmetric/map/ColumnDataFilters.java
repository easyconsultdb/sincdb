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
package org.jumpmind.symmetric.map;

import java.util.List;

import org.jumpmind.symmetric.ext.INodeGroupExtensionPoint;
import org.jumpmind.symmetric.load.IDataLoaderContext;
import org.jumpmind.symmetric.load.IDataLoaderFilter;

public class ColumnDataFilters implements IDataLoaderFilter, INodeGroupExtensionPoint {

    private boolean autoRegister = true;

    private String[] nodeGroupIdsToApplyTo;

    List<TableColumnValueFilter> filters;

    protected void filterColumnValues(IDataLoaderContext context, String[] columnValues) {
        if (filters != null) {
            for (TableColumnValueFilter filteredColumn : filters) {
                if (filteredColumn.getTableName().equals(context.getTableName())) {
                    int index = context.getColumnIndex(filteredColumn.getColumnName());
                    if (index >= 0) {
                        columnValues[index] = filteredColumn.getFilter().filter(
                                columnValues[index], context.getContextCache());
                    }
                }
            }
        }
    }

    public boolean filterDelete(IDataLoaderContext context, String[] keyValues) {
        return true;
    }

    public boolean filterInsert(IDataLoaderContext context, String[] columnValues) {
        filterColumnValues(context, columnValues);
        return true;
    }

    public boolean filterUpdate(IDataLoaderContext context, String[] columnValues,
            String[] keyValues) {
        filterColumnValues(context, columnValues);
        return true;
    }

    public void setFilters(List<TableColumnValueFilter> filters) {
        this.filters = filters;
    }

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }

    public String[] getNodeGroupIdsToApplyTo() {
        return this.nodeGroupIdsToApplyTo;
    }

    public void setNodeGroupIdsToApplyTo(String[] nodeGroupIdsToApplyTo) {
        this.nodeGroupIdsToApplyTo = nodeGroupIdsToApplyTo;
    }
}
