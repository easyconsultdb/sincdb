/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Eric Long <erilong@users.sourceforge.net>
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

package org.jumpmind.symmetric.service.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.Version;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IUpgradeService;
import org.jumpmind.symmetric.upgrade.IUpgradeTask;

public class UpgradeService extends AbstractService implements IUpgradeService {

    private static final Log logger = LogFactory.getLog(UpgradeService.class);

    private INodeService nodeService;

    private Map<String, List<IUpgradeTask>> upgradeTaskMap;

    public boolean isUpgradeNecessary() {
        boolean isUpgradeNecessary = false;
        String symmetricVersion = nodeService.findSymmetricVersion();
        if (!StringUtils.isBlank(symmetricVersion) && !symmetricVersion.equals("development")) {
            if (Version.isOlderMinorVersion(symmetricVersion)) {
                isUpgradeNecessary = true;
            }
        }
        return isUpgradeNecessary;
    }

    public void upgrade() {
        String symmetricVersion = nodeService.findSymmetricVersion();
        String nodeId = nodeService.findMyNodeId();
        if (symmetricVersion != null && nodeId != null) {
            int[] fromVersion = Version.parseVersion(symmetricVersion);
            if (Version.isOlderMinorVersion(symmetricVersion)) {
                runUpgrade(nodeId, fromVersion);
                Node node = nodeService.findIdentity();
                node.setSymmetricVersion(Version.version());
                nodeService.updateNode(node);
            }
        } else {
            logger.warn("Cannot upgrade an unregistered node");
        }
    }

    private void runUpgrade(String nodeId, int[] fromVersion) {
        String majorMinorVersion = fromVersion[0] + "." + fromVersion[1];
        List<IUpgradeTask> upgradeTaskList = upgradeTaskMap.get(majorMinorVersion);
        logger.warn("Starting upgrade from version " + majorMinorVersion + " to " + Version.version());
        boolean isRegistrationServer = StringUtils.isEmpty(parameterService.getRegistrationUrl());
        if (upgradeTaskList != null) {
            for (IUpgradeTask upgradeTask : upgradeTaskList) {
                if ((isRegistrationServer && upgradeTask.isUpgradeRegistrationServer())
                        || (!isRegistrationServer && upgradeTask.isUpgradeNonRegistrationServer())) {
                    upgradeTask.upgrade(nodeId, parameterService, fromVersion);
                }
            }
        }
        logger.warn("Completed upgrade");
    }

    public void setUpgradeTaskMap(Map<String, List<IUpgradeTask>> upgradeTaskMap) {
        this.upgradeTaskMap = upgradeTaskMap;
    }

    public void setNodeService(INodeService nodeService) {
        this.nodeService = nodeService;
    }

}