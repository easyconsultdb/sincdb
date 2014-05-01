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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.security.INodePasswordFilter;
import org.jumpmind.symmetric.service.IRegistrationService;

public class MockRegistrationService implements IRegistrationService {

    public String getRedirectionUrlFor(String externalId) {
        return null;
    }
    
    public void setNodePasswordFilter(INodePasswordFilter nodePasswordFilter) {
    }

    public boolean isAutoRegistration() {
        return false;
    }

    public String openRegistration(String nodeGroupId, String externalId) {
        return null;
    }

    public void markNodeAsRegistered(String nodeId) {

    }

    public void reOpenRegistration(String nodeId) {

    }

    public boolean registerNode(Node node, OutputStream out, boolean isRequestedRegistration) throws IOException {
        return false;
    }

    public void registerWithServer() {
    }

    public boolean isRegisteredWithServer() {
        return true;
    }

    public void saveRegistrationRedirect(String externalIdToRedirect, String nodeIdToRedirectTo) {
    }

    public Map<String, String> getRegistrationRedirectMap() {
        return null;
    }
}