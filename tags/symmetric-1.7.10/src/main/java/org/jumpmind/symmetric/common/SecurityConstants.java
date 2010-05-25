/**
 * Copyright (C) 2005 Big Lots Inc.
 */
package org.jumpmind.symmetric.common;

/**
 * @author elong
 * 
 */
public class SecurityConstants {

    public static final String PREFIX_ENC = "enc:";

    public static final String ALGORITHM = "PBEWithMD5AndTripleDES";

    public static final int ITERATION_COUNT = 3;

    public static final String CHARSET = "UTF8";

    public static final String KEYSTORE_PASSWORD = "changeit";
    
    public static final String KEYSTORE_TYPE = "JCEKS";

    public static final byte[] SALT = { (byte) 0x01, (byte) 0x03, (byte) 0x05, (byte) 0x07, (byte) 0xA2,
            (byte) 0xB4, (byte) 0xC6, (byte) 0xD8 };
    
    public static final String ALIAS_SYM_PRIVATE_KEY = "sym";
    
    public static final String ALIAS_SYM_SECRET_KEY = "sym.secret";
    
    public static final String SYSPROP_KEYSTORE = "sym.keystore.file";
    
    public static final String SYSPROP_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

}
