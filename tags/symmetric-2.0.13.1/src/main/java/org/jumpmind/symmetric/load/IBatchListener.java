/*
 * symmetric is an open source database synchronization solution.
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
package org.jumpmind.symmetric.load;

import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.ext.IExtensionPoint;
import org.jumpmind.symmetric.model.IncomingBatch;

/**
 * This extension point is called whenever a batch has completed loading but before
 * the transaction has committed.
 */
public interface IBatchListener extends IExtensionPoint {

    /**
     * If the {@link ParameterConstants#DATA_LOADER_MAX_ROWS_BEFORE_COMMIT} property is set and the max number of 
     * rows is reached and a commit is about to happen, then this method is called.
     */
    public void earlyCommit(IDataLoader loader, IncomingBatch batch);
    
    /**
     * This method is called after a batch has been successfully processed. It
     * is called in the scope of the transaction that controls the batch commit.
     */
    public void batchComplete(IDataLoader loader, IncomingBatch batch);
    
    /**
     * This method is called after the database transaction for the batch has been committed.
     */
    public void batchCommitted(IDataLoader loader, IncomingBatch batch);
    
    /**
     * This method is called after the database transaction for the batch has been rolled back.
     */
    public void batchRolledback(IDataLoader loader, IncomingBatch batch, Exception ex); 
}