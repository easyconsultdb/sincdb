package org.jumpmind.symmetric.core.db.oracle;

import java.util.Map;

import org.jumpmind.symmetric.core.db.IDbPlatform;
import org.jumpmind.symmetric.core.db.TriggerBuilder;

public class OracleTriggerBuilder extends TriggerBuilder {

    public OracleTriggerBuilder(IDbPlatform dbPlatform) {
        super(dbPlatform);
    }

    @Override
    protected String getClobColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getNewTriggerValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getOldTriggerValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getBlobColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getWrappedBlobColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getInitialLoadSql() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Map<String, String> getFunctionTemplatesToInstall() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getFunctionInstalledSql() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getEmptyColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getStringColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getXmlColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getArrayColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getNumberColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getDatetimeColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getBooleanColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getTimeColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getDateColumnTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getTriggerConcatCharacter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getOldColumnPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getNewColumnPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPostTriggerTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getInsertTriggerTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getUpdateTriggerTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getDeleteTriggerTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getTransactionTriggerExpression() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean isTransactionIdOverrideSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected String preProcessTriggerSqlClause(String sqlClause) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getDataHasChangedCondition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getSourceNodeExpression() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getSyncTriggersExpression() {
        // TODO Auto-generated method stub
        return null;
    }

}
