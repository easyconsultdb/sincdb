/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Chris Henson <chenson42@users.sourceforge.net>,
 *               Eric Long <erilong@users.sourceforge.net>
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

package org.jumpmind.symmetric.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.jumpmind.symmetric.load.IDataLoaderContext;
import org.jumpmind.symmetric.load.IDataLoaderStatistics;

public class IncomingBatchHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        OK, ER, SK;
    }

    private String batchId;

    private String nodeId;

    private Status status;

    private static String hostName;

    private long byteCount;

    private long networkMillis;
    
    private long filterMillis;

    private long databaseMillis;

    private long statementCount;

    private long fallbackInsertCount;

    private long fallbackUpdateCount;

    private long missingDeleteCount;
    
    private long failedRowNumber;

    private Date startTime;

    private Date endTime;

    static {
        InetAddress address = null;
        try {
            address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            hostName = "UNKNOWN";
        }
    }

    public IncomingBatchHistory() {
    }

    public IncomingBatchHistory(IDataLoaderContext context) {
        batchId = context.getBatchId();
        nodeId = context.getNodeId();
        status = Status.OK;
        startTime = new Date();
    }
    
    public void setValues(IDataLoaderStatistics statistics, boolean isSuccess) {
        byteCount = statistics.getByteCount();
        networkMillis = statistics.getNetworkMillis();
        filterMillis = statistics.getFilterMillis();
        databaseMillis = statistics.getDatabaseMillis();
        statementCount = statistics.getStatementCount();
        fallbackInsertCount = statistics.getFallbackInsertCount();
        fallbackUpdateCount = statistics.getFallbackUpdateCount();
        missingDeleteCount = statistics.getMissingDeleteCount();
        endTime = new Date();
        if (! isSuccess) {
            status = Status.ER;
            failedRowNumber = statistics.getStatementCount();
        }
    }    

    public String getNodeBatchId() {
        return nodeId + "-" + batchId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getFailedRowNumber() {
        return failedRowNumber;
    }

    public void setFailedRowNumber(long failedRowNumber) {
        this.failedRowNumber = failedRowNumber;
    }

    public long getFallbackInsertCount() {
        return fallbackInsertCount;
    }

    public void setFallbackInsertCount(long fallbackInsertCount) {
        this.fallbackInsertCount = fallbackInsertCount;
    }

    public long getFallbackUpdateCount() {
        return fallbackUpdateCount;
    }

    public void setFallbackUpdateCount(long fallbackUpdateCount) {
        this.fallbackUpdateCount = fallbackUpdateCount;
    }

    public String getHostName() {
        return hostName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public long getStatementCount() {
        return statementCount;
    }

    public void setStatementCount(long statementCount) {
        this.statementCount = statementCount;
    }

    public long getMissingDeleteCount() {
        return missingDeleteCount;
    }

    public void setMissingDeleteCount(long missingDeleteCount) {
        this.missingDeleteCount = missingDeleteCount;
    }

    public long getByteCount() {
        return byteCount;
    }

    public void setByteCount(long byteCount) {
        this.byteCount = byteCount;
    }

    public long getDatabaseMillis() {
        return databaseMillis;
    }

    public void setDatabaseMillis(long databaseMillis) {
        this.databaseMillis = databaseMillis;
    }

    public long getFilterMillis() {
        return filterMillis;
    }

    public void setFilterMillis(long filterMillis) {
        this.filterMillis = filterMillis;
    }

    public long getNetworkMillis() {
        return networkMillis;
    }

    public void setNetworkMillis(long networkMillis) {
        this.networkMillis = networkMillis;
    }

}