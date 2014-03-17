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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.math.random.RandomDataImpl;
import org.jumpmind.symmetric.common.SecurityConstants;
import org.jumpmind.symmetric.service.ISecurityService;

public class SecurityService extends AbstractService implements ISecurityService {

    private SecretKey secretKey;

    public String encrypt(String plainText) {
        try {
            byte[] bytes = plainText.getBytes(SecurityConstants.CHARSET);
            byte[] enc = getCipher(Cipher.ENCRYPT_MODE).doFinal(bytes);
            return new String(Base64.encodeBase64(enc), SecurityConstants.CHARSET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encText) {
        try {
            byte[] dec = Base64.decodeBase64(encText.getBytes());
            byte[] bytes = getCipher(Cipher.DECRYPT_MODE).doFinal(dec);
            return new String(bytes, SecurityConstants.CHARSET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Cipher getCipher(int mode) throws Exception {
        if (secretKey == null) {
            secretKey = getSecretKey();
        }
        Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm());
        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(SecurityConstants.SALT,
                SecurityConstants.ITERATION_COUNT);
        cipher.init(mode, secretKey, paramSpec);
        return cipher;
    }

    private SecretKey getSecretKey() throws Exception {
        String password = System.getProperty(SecurityConstants.SYSPROP_KEYSTORE_PASSWORD);
        password = (password != null) ? password : SecurityConstants.KEYSTORE_PASSWORD;
        KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(password.toCharArray());
        KeyStore ks = getKeyStore(password);
        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(SecurityConstants.ALIAS_SYM_SECRET_KEY,
                param);
        if (entry == null) {
            log.debug("SecretKeyGenerating");
            String keyPassword = new RandomDataImpl().nextSecureHexString(16);
            KeySpec keySpec = new PBEKeySpec(keyPassword.toCharArray(), SecurityConstants.SALT,
                    SecurityConstants.ITERATION_COUNT);
            SecretKey key = SecretKeyFactory.getInstance(SecurityConstants.ALGORITHM).generateSecret(keySpec);
            entry = new KeyStore.SecretKeyEntry(key);
            ks.setEntry(SecurityConstants.ALIAS_SYM_SECRET_KEY, entry, param);
            saveKeyStore(ks, password);
        } else {
            log.debug("SecretKeyRetrieving");
        }
        return entry.getSecretKey();
    }

    private KeyStore getKeyStore(String password) throws Exception {
        KeyStore ks = KeyStore.getInstance(SecurityConstants.KEYSTORE_TYPE);
        FileInputStream is = new FileInputStream(System.getProperty(SecurityConstants.SYSPROP_KEYSTORE));
        ks.load(is, password.toCharArray());
        is.close();
        return ks;
    }

    private void saveKeyStore(KeyStore ks, String password) throws Exception {
        FileOutputStream os = new FileOutputStream(System.getProperty(SecurityConstants.SYSPROP_KEYSTORE));
        ks.store(os, password.toCharArray());
        os.close();
    }

}