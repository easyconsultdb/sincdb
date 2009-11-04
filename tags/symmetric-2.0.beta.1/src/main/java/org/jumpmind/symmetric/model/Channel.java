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

package org.jumpmind.symmetric.model;

import java.util.Collection;

/**
 * Definition of a channel and it's priority. A channel is a group of tables
 * that get synchronized together.
 */
public class Channel {

    private static final long serialVersionUID = -8183376200537307264L;

    private String id;

    private int processingOrder;

    private int maxBatchSize;

    private int maxBatchToSend;

    private boolean enabled;

    private String batchAlgorithm = "default";

    private long extractPeriodMillis = 0;

    public Channel() {
    }

    public Channel(String id, int processingOrder) {
        this.id = id;
        this.processingOrder = processingOrder;
    }

    public Channel(String id, int processingOrder, int maxBatchSize, int maxBatchToSend, boolean enabled,
            long extractPeriodMillis) {
        this(id, processingOrder);
        this.maxBatchSize = maxBatchSize;
        this.maxBatchToSend = maxBatchToSend;
        this.enabled = enabled;
        this.extractPeriodMillis = extractPeriodMillis;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getProcessingOrder() {
        return processingOrder;
    }

    public void setProcessingOrder(int priority) {
        this.processingOrder = priority;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxNumberOfEvents) {
        this.maxBatchSize = maxNumberOfEvents;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxBatchToSend() {
        return maxBatchToSend;
    }

    public void setMaxBatchToSend(int maxBatchToSend) {
        this.maxBatchToSend = maxBatchToSend;
    }

    /**
     * Check to see if this channel id matches one of the channels in the
     * collection
     * 
     * @return true if a match is found
     */
    public boolean isInList(Collection<? extends NodeChannel> channels) {
        if (channels != null) {
            for (NodeChannel channel : channels) {
                if (channel.getId().equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setBatchAlgorithm(String batchAlgorithm) {
        this.batchAlgorithm = batchAlgorithm;
    }

    public String getBatchAlgorithm() {
        return batchAlgorithm;
    }

    public long getExtractPeriodMillis() {
        return extractPeriodMillis;
    }

    public void setExtractPeriodMillis(long extractPeriodMillis) {
        this.extractPeriodMillis = extractPeriodMillis;
    }

}
