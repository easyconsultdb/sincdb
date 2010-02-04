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
package org.jumpmind.symmetric.integrate;

import java.util.Map;

import org.jumpmind.symmetric.common.logging.ILog;
import org.jumpmind.symmetric.common.logging.LogFactory;
import org.jumpmind.symmetric.ext.INodeGroupExtensionPoint;
import org.jumpmind.symmetric.load.IDataLoader;
import org.jumpmind.symmetric.load.IDataLoaderContext;
import org.jumpmind.symmetric.model.IncomingBatch;
import org.springframework.beans.factory.BeanNameAware;

/**
 * An abstract convenience class meant to be implemented by classes that need to
 * publish text messages
 */
abstract public class AbstractTextPublisherDataLoaderFilter implements IPublisherFilter, INodeGroupExtensionPoint,
        BeanNameAware {

    private static final ILog log = LogFactory.getLog(AbstractTextPublisherDataLoaderFilter.class);

    private final String MSG_CACHE = "msg_CACHE" + hashCode();

    protected IPublisher publisher;

    private boolean loadDataInTargetDatabase = true;

    protected String tableName;

    private boolean autoRegister = true;

    private String[] nodeGroupIdsToApplyTo;

    private int messagesSinceLastLogOutput = 0;

    private long minTimeInMsBetweenLogOutput = 30000;

    private long lastTimeInMsOutputLogged = System.currentTimeMillis();

    private String beanName;

    protected abstract String addTextHeader(IDataLoaderContext ctx);

    protected abstract String addTextElementForDelete(IDataLoaderContext ctx, String[] keys);

    protected abstract String addTextElementForUpdate(IDataLoaderContext ctx, String[] data, String[] keys);

    protected abstract String addTextElementForInsert(IDataLoaderContext ctx, String[] data);

    protected abstract String addTextFooter(IDataLoaderContext ctx);

    public void setBeanName(String name) {
        this.beanName = name;
    }

    public boolean filterDelete(IDataLoaderContext ctx, String[] keys) {
        if (tableName != null && tableName.equals(ctx.getTableName())) {
            String msg = addTextElementForDelete(ctx, keys);
            if (msg != null) {
                getFromCache(ctx).append(msg);
            }
        }
        return loadDataInTargetDatabase;
    }

    public boolean filterUpdate(IDataLoaderContext ctx, String[] data, String[] keys) {
        if (tableName != null && tableName.equals(ctx.getTableName())) {
            String msg = addTextElementForUpdate(ctx, data, keys);
            if (msg != null) {
                getFromCache(ctx).append(msg);
            }
        }
        return loadDataInTargetDatabase;
    }

    public boolean filterInsert(IDataLoaderContext ctx, String[] data) {
        if (tableName != null && tableName.equals(ctx.getTableName())) {
            String msg = addTextElementForInsert(ctx, data);
            if (msg != null) {
                getFromCache(ctx).append(msg);
            }
        }
        return loadDataInTargetDatabase;
    }

    protected StringBuilder getFromCache(IDataLoaderContext ctx) {
        Map<String, Object> cache = ctx.getContextCache();
        StringBuilder msgCache = (StringBuilder) cache.get(MSG_CACHE);
        if (msgCache == null) {
            msgCache = new StringBuilder(addTextHeader(ctx));
            cache.put(MSG_CACHE, msgCache);
        }
        return msgCache;
    }

    protected boolean doesTextExistToPublish(IDataLoaderContext ctx) {
        Map<String, Object> cache = ctx.getContextCache();
        StringBuilder msgCache = (StringBuilder) cache.get(MSG_CACHE);
        return msgCache != null && msgCache.length() > 0;
    }

    private void finalizeAndPublish(IDataLoaderContext ctx) {
        StringBuilder msg = getFromCache(ctx);
        if (msg.length() > 0) {
            msg.append(addTextFooter(ctx));
            log.debug("TextMessagePublishing", msg);
            ctx.getContextCache().remove(MSG_CACHE);
            publisher.publish(ctx, msg.toString());
        }
    }

    public void batchComplete(IDataLoader loader, IncomingBatch batch) {
        IDataLoaderContext ctx = loader.getContext();
        if (doesTextExistToPublish(ctx)) {
            finalizeAndPublish(ctx);
            logCount();
        }
    }

    protected void logCount() {
        messagesSinceLastLogOutput++;
        long timeInMsSinceLastLogOutput = System.currentTimeMillis() - lastTimeInMsOutputLogged;
        if (timeInMsSinceLastLogOutput > minTimeInMsBetweenLogOutput) {
            log.info("TextMessagePubished", beanName, messagesSinceLastLogOutput, timeInMsSinceLastLogOutput);
            lastTimeInMsOutputLogged = System.currentTimeMillis();
            messagesSinceLastLogOutput = 0;
        }
    }

    public void setLoadDataInTargetDatabase(boolean loadDataInTargetDatabase) {
        this.loadDataInTargetDatabase = loadDataInTargetDatabase;
    }

    public void setPublisher(IPublisher publisher) {
        this.publisher = publisher;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public String[] getNodeGroupIdsToApplyTo() {
        return nodeGroupIdsToApplyTo;
    }

    public void setNodeGroupIdToApplyTo(String nodeGroupdId) {
        this.nodeGroupIdsToApplyTo = new String[] { nodeGroupdId };
    }

    public void setMessagesSinceLastLogOutput(int messagesSinceLastLogOutput) {
        this.messagesSinceLastLogOutput = messagesSinceLastLogOutput;
    }

    public void setMinTimeInMsBetweenLogOutput(long timeInMsBetweenLogOutput) {
        this.minTimeInMsBetweenLogOutput = timeInMsBetweenLogOutput;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void batchCommitted(IDataLoader loader, IncomingBatch batch) {
    }

    public void batchRolledback(IDataLoader loader, IncomingBatch batch) {
    }

    public void earlyCommit(IDataLoader loader, IncomingBatch batch) {
    }
}
