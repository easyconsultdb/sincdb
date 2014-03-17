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

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.Version;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.db.IDbDialect;
import org.jumpmind.symmetric.service.IParameterService;

/**
 * This class represents a node who has registered for sync updates.
 */
public class Node {

    private static final long serialVersionUID = 5228552569658130763L;
    
    private int MAX_VERSION_SIZE = 50;

    private String nodeId;

    private String nodeGroupId;

    private String externalId;

    private String syncURL;

    /**
     * Record the version of the schema. This is recorded and managed by the
     * sync software.
     */
    private String schemaVersion;

    /**
     * Record the type of database the node hosts.
     */
    private String databaseType;

    private String symmetricVersion = Version.version();

    /**
     * Get the version of the database the node hosts.
     */
    private String databaseVersion;

    private boolean syncEnabled;

    private String timezoneOffset;

    private Date heartbeatTime = new Date();

    private String createdByNodeId;

    public Node() {
    }

    public Node(IParameterService runtimeConfig, IDbDialect dbDialect) {
        setNodeGroupId(runtimeConfig.getNodeGroupId());
        setExternalId(runtimeConfig.getExternalId());
        setDatabaseType(dbDialect.getName());
        setDatabaseVersion(dbDialect.getVersion());
        setSyncURL(runtimeConfig.getMyUrl());
        setSchemaVersion(runtimeConfig.getString(ParameterConstants.SCHEMA_VERSION));
    }

    public Node(String nodeId, String syncURL, String version) {
        this.nodeId = nodeId;
        this.syncURL = syncURL;
        this.schemaVersion = version;
    }

    public boolean equals(Object n) {
        return n != null && n instanceof Node && nodeId != null && nodeId.equals(((Node) n).getNodeId());
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getSyncURL() {
        return syncURL;
    }

    public void setSyncURL(String syncURL) {
        this.syncURL = syncURL;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String version) {
        // abbreviate because we do not control the version
        this.schemaVersion = StringUtils.abbreviate(version, MAX_VERSION_SIZE);
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getDatabaseVersion() {
        return databaseVersion;
    }

    public void setDatabaseVersion(String databaseVersion) {
        this.databaseVersion = databaseVersion;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String domainId) {
        this.externalId = domainId;
    }

    public String getNodeGroupId() {
        return nodeGroupId;
    }

    public void setNodeGroupId(String domainName) {
        this.nodeGroupId = domainName;
    }

    public String getSymmetricVersion() {
        return symmetricVersion;
    }

    public void setSymmetricVersion(String symmetricVersion) {
        this.symmetricVersion = symmetricVersion;
    }

    public String toString() {
        return nodeGroupId + ":" + externalId + ":" + (nodeId == null ? "?" : nodeId);
    }

    public Date getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(Date heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public String getTimezoneOffset() {
        return timezoneOffset;
    }

    public void setTimezoneOffset(String timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    public String getCreatedByNodeId() {
        return createdByNodeId;
    }

    public void setCreatedByNodeId(String createdByNodeId) {
        this.createdByNodeId = createdByNodeId;
    }

    public boolean isVersionGreaterThanOrEqualTo(int... targetVersion) {
        if (symmetricVersion != null) {
            if (symmetricVersion.equals("development")) {
                return true;
            }
            int[] currentVersion = Version.parseVersion(symmetricVersion);
            for (int i = 0; i < currentVersion.length; i++) {
                int j = currentVersion[i];
                if (targetVersion.length > i) {
                    if (j > targetVersion[i]) {
                        return true;
                    } else if (j < targetVersion[i]) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

}