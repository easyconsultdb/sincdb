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
package org.jumpmind.symmetric.route;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ddlutils.model.Column;
import org.jumpmind.symmetric.db.IDbDialect;
import org.jumpmind.symmetric.model.DataMetaData;
import org.jumpmind.symmetric.model.Node;

public abstract class AbstractDataRouter implements IDataRouter {

    private boolean autoRegister = true;

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }
    
    protected Map<String, String> getNewDataAsString(DataMetaData dataMetaData, IDbDialect dbDialect) {
        String[] rowData = dataMetaData.getData().getParsedRowData();
        Column[] columns = dataMetaData.getTable().getColumns();
        Map<String, String> map = new HashMap<String, String>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            Column c = columns[i];
            map.put(c.getName(), rowData[i]);
        }
        return map;        
    }
  
    protected Map<String, Object> getNewData(DataMetaData dataMetaData, IDbDialect dbDialect) {
        String[] rowData = dataMetaData.getData().getParsedRowData();
        Column[] columns = dataMetaData.getTable().getColumns();
        Map<String, Object> map = new HashMap<String, Object>(columns.length);
        Object[] objects = dbDialect.getObjectValues(dbDialect.getBinaryEncoding(), rowData, columns);
        for (int i = 0; i < columns.length; i++) {
            Column c = columns[i];
            map.put(c.getName(), objects[i]);
        }
        return map;        
    }

    protected Set<String> toNodeIds(Set<Node> nodes) {
        Set<String> nodeIds = new HashSet<String>(nodes.size());
        for (Node node : nodes) {
            nodeIds.add(node.getNodeId());
        }
        return nodeIds;
    }
    

}
