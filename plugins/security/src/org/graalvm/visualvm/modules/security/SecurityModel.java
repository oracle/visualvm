/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.modules.security;

import java.util.prefs.Preferences;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
class SecurityModel {

    static final String KEYSTORE_LOCATION     = "javax.net.ssl.keyStore"; // NOI18N
    static final String KEYSTORE_TYPE         = "javax.net.ssl.keyStoreType"; // NOI18N
    static final String KEYSTORE_PASSWORD     = "javax.net.ssl.keyStorePassword"; // NOI18N

    static final String TRUSTSTORE_LOCATION   = "javax.net.ssl.trustStore"; // NOI18N
    static final String TRUSTSTORE_TYPE       = "javax.net.ssl.trustStoreType"; // NOI18N
    static final String TRUSTSTORE_PASSWORD   = "javax.net.ssl.trustStorePassword"; // NOI18N

    static final String ENABLED_CIPHER_SUITES = "javax.rmi.ssl.client.enabledCipherSuites"; // NOI18N
    static final String ENABLED_PROTOCOLS     = "javax.rmi.ssl.client.enabledProtocols"; // NOI18N

    private static final String CUSTOMIZED_MSG = "Environment already customized by command line"; // NOI18N

    private static SecurityModel INSTANCE;

    private final boolean environmentCustomized;

    private final Preferences prefs;


    static synchronized SecurityModel getInstance() {
        if (INSTANCE == null) INSTANCE = new SecurityModel();
        return INSTANCE;
    }


    boolean environmentCustomized() {
        return environmentCustomized;
    }

    boolean customizeEnvironment() {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        boolean customized = false;

        String keyStore = getKeyStore();
        if (keyStore != null) {
            customized = true;

            System.setProperty(KEYSTORE_LOCATION, keyStore);

            char[] keyStorePassword = getKeyStorePassword();
            if (keyStorePassword != null)
                System.setProperty(KEYSTORE_PASSWORD, new String(keyStorePassword));

            String keyStoreType = getKeyStoreType();
            if (keyStoreType != null)
                System.setProperty(KEYSTORE_TYPE, keyStoreType);
        }

        String trustStore = getTrustStore();
        if (trustStore != null) {
            customized = true;

            System.setProperty(TRUSTSTORE_LOCATION, trustStore);

            char[] trustStorePassword = getTrustStorePassword();
            if (trustStorePassword != null)
                System.setProperty(TRUSTSTORE_PASSWORD, new String(trustStorePassword));

            String trustStoreType = getTrustStoreType();
            if (trustStoreType != null)
                System.setProperty(TRUSTSTORE_TYPE, trustStoreType);
        }

        String enabledProtocols = getEnabledProtocols();
        if (enabledProtocols != null) {
            customized = true;
            System.setProperty(ENABLED_PROTOCOLS, enabledProtocols);
        }

        String enabledCipherSuites = getEnabledCipherSuites();
        if (enabledCipherSuites != null) {
            customized = true;
            System.setProperty(ENABLED_CIPHER_SUITES, enabledCipherSuites);
        }

        return customized;
    }


    String getKeyStore() {
        if (environmentCustomized) return getKeyStoreEnv();
        else return prefs.get(KEYSTORE_LOCATION, null);
    }

    static String getKeyStoreEnv() {
        return System.getProperty(KEYSTORE_LOCATION);
    }

    void setKeyStore(String keyStore) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        if (keyStore == null) prefs.remove(KEYSTORE_LOCATION);
        else prefs.put(KEYSTORE_LOCATION, keyStore);
    }

    char[] getKeyStorePassword() {
        if (environmentCustomized) return getKeyStorePasswordEnv();
        String password = prefs.get(KEYSTORE_PASSWORD, null);
        return password == null ? null : Utils.decodePassword(password).toCharArray();
    }

    static char[] getKeyStorePasswordEnv() {
        String password = System.getProperty(KEYSTORE_PASSWORD);
        return password == null ? null : password.toCharArray();
    }

    void setKeyStorePassword(char[] keyStorePassword) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        if (keyStorePassword == null) prefs.remove(KEYSTORE_PASSWORD);
        else prefs.put(KEYSTORE_PASSWORD, Utils.encodePassword(
                       new String(keyStorePassword)));
    }

    String getKeyStoreType() {
        if (environmentCustomized) return getKeyStoreTypeEnv();
        else return prefs.get(KEYSTORE_TYPE, null);
    }

    static String getKeyStoreTypeEnv() {
        return System.getProperty(KEYSTORE_TYPE);
    }

    void setKeyStoreType(String keyStoreType) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        if (keyStoreType == null) prefs.remove(KEYSTORE_TYPE);
        else prefs.put(KEYSTORE_TYPE, keyStoreType);
    }

    String getTrustStore() {
        if (environmentCustomized) return getTrustStoreEnv();
        else return prefs.get(TRUSTSTORE_LOCATION, null);
    }

    static String getTrustStoreEnv() {
        return System.getProperty(TRUSTSTORE_LOCATION);
    }

    void setTrustStore(String trustStore) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        if (trustStore == null) prefs.remove(TRUSTSTORE_LOCATION);
        else prefs.put(TRUSTSTORE_LOCATION, trustStore);
    }

    char[] getTrustStorePassword() {
        if (environmentCustomized) return getTrustStorePasswordEnv();
        String password = prefs.get(TRUSTSTORE_PASSWORD, null);
        return password == null ? null : Utils.decodePassword(password).toCharArray();
    }

    static char[] getTrustStorePasswordEnv() {
        String password = System.getProperty(TRUSTSTORE_PASSWORD);
        return password == null ? null : password.toCharArray();
    }

    void setTrustStorePassword(char[] trustStorePassword) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        if (trustStorePassword == null) prefs.remove(TRUSTSTORE_PASSWORD);
        else prefs.put(TRUSTSTORE_PASSWORD, Utils.encodePassword(
                       new String(trustStorePassword)));
    }

    String getTrustStoreType() {
        if (environmentCustomized) return getTrustStoreTypeEnv();
        else return prefs.get(TRUSTSTORE_TYPE, null);
    }

    static String getTrustStoreTypeEnv() {
        return System.getProperty(TRUSTSTORE_TYPE);
    }

    void setTrustStoreType(String trustStoreType) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        if (trustStoreType == null) prefs.remove(TRUSTSTORE_TYPE);
        else prefs.put(TRUSTSTORE_TYPE, trustStoreType);
    }

    String getEnabledProtocols() {
        if (environmentCustomized) return getEnabledProtocolsEnv();
        else return prefs.get(ENABLED_PROTOCOLS, null);
    }

    static String getEnabledProtocolsEnv() {
        return System.getProperty(ENABLED_PROTOCOLS);
    }

    void setEnabledProtocols(String enabledProtocols) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);

        if (enabledProtocols == null) prefs.remove(ENABLED_PROTOCOLS); // NOI18N
        else prefs.put(ENABLED_PROTOCOLS, enabledProtocols);
    }

    String getEnabledCipherSuites() {
        if (environmentCustomized) return getEnabledCipherSuitesEnv();
        else return prefs.get(ENABLED_CIPHER_SUITES, null);
    }

    static String getEnabledCipherSuitesEnv() {
        return System.getProperty(ENABLED_CIPHER_SUITES);
    }

    void setEnabledCipherSuites(String enabledCipherSuites) {
        if (environmentCustomized)
            throw new UnsupportedOperationException(CUSTOMIZED_MSG);
        
        if (enabledCipherSuites == null) prefs.remove(ENABLED_CIPHER_SUITES); // NOI18N
        else prefs.put(ENABLED_CIPHER_SUITES, enabledCipherSuites);
    }


    private static boolean environmentCustomizedImpl() {
        if (getKeyStoreEnv() != null) return true;
        else if (getKeyStorePasswordEnv() != null) return true;
        else if (getKeyStoreTypeEnv() != null) return true;
        else if (getTrustStoreEnv() != null) return true;
        else if (getTrustStorePasswordEnv() != null) return true;
        else if (getTrustStoreTypeEnv() != null) return true;
        else if (getEnabledProtocolsEnv() != null) return true;
        else if (getEnabledCipherSuitesEnv() != null) return true;
        return false;
    }


    private SecurityModel() {
        environmentCustomized = environmentCustomizedImpl();
        prefs = NbPreferences.forModule(SecurityModel.class);
    }

}
