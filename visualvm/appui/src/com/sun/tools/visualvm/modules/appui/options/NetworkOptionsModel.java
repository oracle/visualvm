/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.appui.options;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import org.netbeans.api.keyring.Keyring;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class NetworkOptionsModel {

    static final String PROXY_HTTP_HOST = "proxyHttpHost"; // NOI18N
    static final String PROXY_HTTP_PORT = "proxyHttpPort"; // NOI18N
    static final String PROXY_HTTPS_HOST = "proxyHttpsHost"; // NOI18N
    static final String PROXY_HTTPS_PORT = "proxyHttpsPort"; // NOI18N
    static final String PROXY_SOCKS_HOST = "proxySocksHost"; // NOI18N
    static final String PROXY_SOCKS_PORT = "proxySocksPort"; // NOI18N
    static final String NOT_PROXY_HOSTS = "proxyNonProxyHosts"; // NOI18N
    static final String PROXY_TYPE = "proxyType"; // NOI18N
    static final String USE_PROXY_AUTHENTICATION = "useProxyAuthentication"; // NOI18N
    static final String PROXY_AUTHENTICATION_USERNAME = "proxyAuthenticationUsername"; // NOI18N
    static final String PROXY_AUTHENTICATION_PASSWORD = "proxyAuthenticationPassword"; // NOI18N
    static final String USE_PROXY_ALL_PROTOCOLS = "useProxyAllProtocols"; // NOI18N
    static final String DIRECT = "DIRECT"; // NOI18N

    private static String presetNonProxyHosts;
    private final Preferences PREFERENCES = NbPreferences.root().node ("org/netbeans/core"); // NOI18N

    static final int DIRECT_CONNECTION = 0;
    static final int AUTO_DETECT_PROXY = 1;
    static final int MANUAL_SET_PROXY = 2;


    String getHttpHost() {
        return normalizeProxyHost(PREFERENCES.get(PROXY_HTTP_HOST, "")); // NOI18N
    }

    void setHttpHost(String proxyHost) {
        if (!proxyHost.equals(getHttpHost()))
            PREFERENCES.put(PROXY_HTTP_HOST, proxyHost);
    }

    String getHttpPort() {
        return PREFERENCES.get(PROXY_HTTP_PORT, "0"); // NOI18N
    }

    void setHttpPort(String proxyPort) {
        if (!proxyPort.equals(getHttpPort()))
            PREFERENCES.put(PROXY_HTTP_PORT, validatePort(proxyPort) ? proxyPort : ""); // NOI18N
    }

    String getHttpsHost() {
        if (useProxyAllProtocols()) return getHttpHost();
        else return PREFERENCES.get(PROXY_HTTPS_HOST, ""); // NOI18N
    }

    void setHttpsHost(String proxyHost) {
        if (!proxyHost.equals(getHttpsHost()))
            PREFERENCES.put(PROXY_HTTPS_HOST, proxyHost);
    }

    String getHttpsPort() {
        if (useProxyAllProtocols()) return getHttpPort();
        else return PREFERENCES.get(PROXY_HTTPS_PORT, "0"); // NOI18N
    }

    void setHttpsPort(String proxyPort) {
        if (!proxyPort.equals(getHttpsPort()))
            PREFERENCES.put(PROXY_HTTPS_PORT, validatePort(proxyPort) ? proxyPort : ""); // NOI18N
    }

    String getSocksHost() {
        if (useProxyAllProtocols()) return getHttpHost();
        else return PREFERENCES.get(PROXY_SOCKS_HOST, ""); // NOI18N
    }

    void setSocksHost(String proxyHost) {
        if (!proxyHost.equals(getSocksHost()))
            PREFERENCES.put(PROXY_SOCKS_HOST, proxyHost);
    }

    String getSocksPort() {
        if (useProxyAllProtocols()) return getHttpPort();
        else return PREFERENCES.get(PROXY_SOCKS_PORT, "0"); // NOI18N
    }

    void setSocksPort(String proxyPort) {
        if (!proxyPort.equals(getSocksPort()))
            PREFERENCES.put(PROXY_SOCKS_PORT, validatePort(proxyPort) ? proxyPort : ""); // NOI18N
    }

    String getNonProxyHosts() {
        return code2view(PREFERENCES.get(NOT_PROXY_HOSTS, getDefaultUserNonProxyHosts()));
    }

    void setNonProxyHosts(String nonProxy) {
        if (!nonProxy.equals(getNonProxyHosts()))
            PREFERENCES.put(NOT_PROXY_HOSTS, view2code(nonProxy));
    }

    int getProxyType() {
        return PREFERENCES.getInt(PROXY_TYPE, AUTO_DETECT_PROXY);
    }

    void setProxyType(int proxyType) {
        if (proxyType != getProxyType()) PREFERENCES.putInt(PROXY_TYPE, proxyType);
    }

    boolean useAuthentication() {
        return PREFERENCES.getBoolean(USE_PROXY_AUTHENTICATION, false);
    }

    void setUseAuthentication(boolean use) {
        if (use != useAuthentication())
            PREFERENCES.putBoolean(USE_PROXY_AUTHENTICATION, use);
    }

    boolean useProxyAllProtocols() {
        return PREFERENCES.getBoolean(USE_PROXY_ALL_PROTOCOLS, true);
    }

    void setUseProxyAllProtocols(boolean use) {
        if (use != useProxyAllProtocols())
            PREFERENCES.putBoolean(USE_PROXY_ALL_PROTOCOLS, use);
    }

    String getAuthenticationUsername() {
        return PREFERENCES.get(PROXY_AUTHENTICATION_USERNAME, ""); // NOI18N
    }

    void setAuthenticationUsername(String username) {
        PREFERENCES.put(PROXY_AUTHENTICATION_USERNAME, username);
    }

    char[] getAuthenticationPassword() {
        String old = PREFERENCES.get(PROXY_AUTHENTICATION_PASSWORD, null);
        if (old != null) {
            PREFERENCES.remove(PROXY_AUTHENTICATION_PASSWORD);
            setAuthenticationPassword(old.toCharArray());
        }
        char[] pwd = Keyring.read(PROXY_AUTHENTICATION_PASSWORD);
        return pwd != null ? pwd : new char[0];
    }

    void setAuthenticationPassword(char [] password) {
        Keyring.save(PROXY_AUTHENTICATION_PASSWORD, password,
                NbBundle.getMessage(NetworkOptionsModel.class, "NetworkOptionsPanel_Password_Description"));  // NOI18N
    }

//    void addPreferenceChangeListener(PreferenceChangeListener l) {
//        PREFERENCES.addPreferenceChangeListener(l);
//    }
//
//    void removePreferenceChangeListener(PreferenceChangeListener l) {
//        PREFERENCES.removePreferenceChangeListener(l);
//    }

    private static String getSystemNonProxyHosts() {
        String systemProxy = System.getProperty ("netbeans.system_http_non_proxy_hosts"); // NOI18N
        return systemProxy == null ? "" : systemProxy; // NOI18N
    }

    private static String getPresetNonProxyHosts() {
        if (presetNonProxyHosts == null)
            presetNonProxyHosts = System.getProperty ("http.nonProxyHosts", ""); // NOI18N
        return presetNonProxyHosts;
    }

    private static String getDefaultUserNonProxyHosts() {
        return getModifiedNonProxyHosts(getSystemNonProxyHosts());
    }

    private static String getModifiedNonProxyHosts(String systemPreset) {
        String fromSystem = systemPreset.replaceAll(";", "|").replaceAll(",", "|"); //NOI18N
        String fromUser = getPresetNonProxyHosts() == null ? "" : getPresetNonProxyHosts().replaceAll(";", "|").replaceAll(",", "|"); //NOI18N
        if (Utilities.isWindows()) fromSystem = addReguralToNonProxyHosts(fromSystem);
        String nonProxy = fromUser + (fromUser.length() == 0 ? "" : "|") + fromSystem + (fromSystem.length() == 0 ? "" : "|") + "localhost|127.0.0.1"; // NOI18N
        String localhost = ""; // NOI18N
        try {
            localhost = InetAddress.getLocalHost().getHostName();
            if (!"localhost".equals(localhost)) { // NOI18N
                nonProxy = nonProxy + "|" + localhost; // NOI18N
            } else {
                // Avoid this error when hostname == localhost:
                // Error in http.nonProxyHosts system property:  sun.misc.REException: localhost is a duplicate
            }
        }
        catch (UnknownHostException e) {
            // OK. Sometimes a hostname is assigned by DNS, but a computer
            // is later pulled off the network. It may then produce a bogus
            // name for itself which can't actually be resolved. Normally
            // "localhost" is aliased to 127.0.0.1 anyway.
        }
        /* per Milan's agreement it's removed. See issue #89868
        try {
            String localhost2 = InetAddress.getLocalHost().getCanonicalHostName();
            if (!"localhost".equals(localhost2) && !localhost2.equals(localhost)) { // NOI18N
                nonProxy = nonProxy + "|" + localhost2; // NOI18N
            } else {
                // Avoid this error when hostname == localhost:
                // Error in http.nonProxyHosts system property:  sun.misc.REException: localhost is a duplicate
            }
        }
        catch (UnknownHostException e) {
            // OK. Sometimes a hostname is assigned by DNS, but a computer
            // is later pulled off the network. It may then produce a bogus
            // name for itself which can't actually be resolved. Normally
            // "localhost" is aliased to 127.0.0.1 anyway.
        }
         */
        return compactNonProxyHosts(nonProxy);
    }


    // avoid duplicate hosts
    private static String compactNonProxyHosts(String nonProxyHost) {
        StringTokenizer st = new StringTokenizer(nonProxyHost, "|"); //NOI18N
        Set<String> s = new HashSet<String>();
        StringBuilder compactedProxyHosts = new StringBuilder();
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if (s.add(t.toLowerCase(Locale.US))) {
                if (compactedProxyHosts.length() > 0) compactedProxyHosts.append('|');
                compactedProxyHosts.append(t);
            }
        }
        return compactedProxyHosts.toString();
    }

    private static String addReguralToNonProxyHosts(String nonProxyHost) {
        StringTokenizer st = new StringTokenizer(nonProxyHost, "|"); // NOI18N
        StringBuilder reguralProxyHosts = new StringBuilder();
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if (t.indexOf ('*') == -1) t = t + '*'; //NOI18N
            if (reguralProxyHosts.length() > 0) reguralProxyHosts.append('|'); // NOI18N
            reguralProxyHosts.append(t);
        }
        return reguralProxyHosts.toString();
    }

    private static String normalizeProxyHost(String proxyHost) {
        if (proxyHost.toLowerCase(Locale.US).startsWith("http://")) { // NOI18N
            return proxyHost.substring(7, proxyHost.length());
        } else {
            return proxyHost;
        }
    }

    private static boolean validatePort (String port) {
        if (port.trim().length() == 0) return true;

        boolean ok = false;
        try {
            Integer.parseInt(port);
            ok = true;
        } catch (NumberFormatException nfe) {
            assert false : nfe;
        }
        return ok;
    }

    private static String code2view(String code) {
        return code == null ? code : code.replace("|", ", "); // NOI18N
    }

    private static String view2code(String view) {
        return view == null ? view : view.replace(", ", "|"); // NOI18N
    }

}
