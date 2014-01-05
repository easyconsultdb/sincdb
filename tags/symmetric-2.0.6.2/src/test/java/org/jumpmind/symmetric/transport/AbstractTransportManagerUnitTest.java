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

import junit.framework.Assert;

import org.junit.Test;

public class AbstractTransportManagerUnitTest {

    @Test
    public void testChooseURL() {
        AbstractTransportManager tm = getMockTransportManager();
        Assert.assertEquals("test",tm.resolveURL("ext://me/", null));
    }
    
    @Test
    public void testChooseBadURL() {
        AbstractTransportManager tm = getMockTransportManager();
        String notFound = "ext://notfound/";
        Assert.assertEquals(notFound,tm.resolveURL(notFound, null));
    }
    
    protected AbstractTransportManager getMockTransportManager() {
        AbstractTransportManager tm = new AbstractTransportManager() {};
       
        tm.addExtensionSyncUrlHandler("me", new ISyncUrlExtension() {
            public String resolveUrl(URI url) {
                return "test";
            }
            public boolean isAutoRegister() {
                return false;
            }
        });
        return tm;
    }
}