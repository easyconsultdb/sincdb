package org.jumpmind.symmetric.ddlutils.firebird;

public class FirebirdPlatform extends org.apache.ddlutils.platform.firebird.FirebirdPlatform {

    public FirebirdPlatform() {
        super();
        setModelReader(new FirebirdModelReader(this));
    }
}