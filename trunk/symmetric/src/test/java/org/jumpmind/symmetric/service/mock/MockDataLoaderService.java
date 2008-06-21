/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Keith Naas <knaas@users.sourceforge.net>
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

package org.jumpmind.symmetric.service.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.jumpmind.symmetric.load.IBatchListener;
import org.jumpmind.symmetric.load.IColumnFilter;
import org.jumpmind.symmetric.load.IDataLoader;
import org.jumpmind.symmetric.load.IDataLoaderFilter;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.service.IDataLoaderService;
import org.jumpmind.symmetric.transport.IIncomingTransport;
import org.jumpmind.symmetric.transport.ITransportManager;

public class MockDataLoaderService implements IDataLoaderService {

    public void addColumnFilter(String tableName, IColumnFilter filter) {

    }

    public void loadDataBatch(String batchData) throws IOException {
    }

    public IDataLoader openDataLoader(BufferedReader reader) throws IOException {
        return null;
    }

    public void addDataLoaderFilter(IDataLoaderFilter filter) {

    }

    public boolean loadData(Node remote, Node local) throws IOException {
        return true;
    }

    public boolean loadData(IIncomingTransport reader) {

        return false;
    }

    public void loadData(InputStream in, OutputStream out) throws IOException {

    }

    public void removeDataLoaderFilter(IDataLoaderFilter filter) {

    }

    public void setDataLoaderFilters(List<IDataLoaderFilter> filters) {

    }

    public void setTransportManager(ITransportManager transportManager) {

    }

    public void addBatchListener(IBatchListener listener) {

    }

}
