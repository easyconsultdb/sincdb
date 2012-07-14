package org.jumpmind.symmetric.service.impl;

import java.util.Map;

import org.jumpmind.db.platform.IDatabasePlatform;

public class AcknowledgeServiceSqlMap extends AbstractSqlMap {

    public AcknowledgeServiceSqlMap(IDatabasePlatform platform,
            Map<String, String> replacementTokens) {
        super(platform, replacementTokens);
        putSql("selectDataIdSql",
                "select data_id from $(data_event) b where batch_id = ? order by data_id   ");
    }

}