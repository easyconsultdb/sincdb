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
package org.jumpmind.symmetric.transport;

import java.net.URI;

import org.jumpmind.symmetric.ext.IExtensionPoint;

/**
 * This {@link IExtensionPoint} is used to select an appropriate URL based on
 * the URI provided in the sync_url column of sym_node.
 * <p/>
 * To use this extension point configure the sync_url for a node with the
 * protocol of ext://beanName. The beanName is the name you give the extension
 * point in the extension xml file.
 * 
 * @see HttpBandwidthUrlSelector
 */
public interface ISyncUrlExtension extends IExtensionPoint {

    public String resolveUrl(URI url);

}