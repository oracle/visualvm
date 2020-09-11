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

package org.graalvm.visualvm.modules.appui.options;

import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.netbeans.api.keyring.Keyring;
import org.openide.util.*;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Rechtacek
 */
public class ProxySettings {
    
    public static final String PROXY_HTTP_HOST = "proxyHttpHost";   // NOI18N
    public static final String PROXY_HTTP_PORT = "proxyHttpPort";   // NOI18N
    public static final String PROXY_HTTPS_HOST = "proxyHttpsHost"; // NOI18N
    public static final String PROXY_HTTPS_PORT = "proxyHttpsPort"; // NOI18N
    public static final String PROXY_SOCKS_HOST = "proxySocksHost"; // NOI18N
    public static final String PROXY_SOCKS_PORT = "proxySocksPort"; // NOI18N
    public static final String NOT_PROXY_HOSTS = "proxyNonProxyHosts";  // NOI18N
    public static final String PROXY_TYPE = "proxyType";                // NOI18N
    public static final String USE_PROXY_AUTHENTICATION = "useProxyAuthentication"; // NOI18N
    public static final String PROXY_AUTHENTICATION_USERNAME = "proxyAuthenticationUsername";   // NOI18N
    public static final String PROXY_AUTHENTICATION_PASSWORD = "proxyAuthenticationPassword";   // NOI18N
    public static final String USE_PROXY_ALL_PROTOCOLS = "useProxyAllProtocols";    // NOI18N
    public static final String DIRECT = "DIRECT";   // NOI18N
    public static final String PAC = "PAC";     // NOI18N
    
    public static final String SYSTEM_PROXY_HTTP_HOST = "systemProxyHttpHost";      // NOI18N
    public static final String SYSTEM_PROXY_HTTP_PORT = "systemProxyHttpPort";      // NOI18N
    public static final String SYSTEM_PROXY_HTTPS_HOST = "systemProxyHttpsHost";    // NOI18N
    public static final String SYSTEM_PROXY_HTTPS_PORT = "systemProxyHttpsPort";    // NOI18N
    public static final String SYSTEM_PROXY_SOCKS_HOST = "systemProxySocksHost";    // NOI18N
    public static final String SYSTEM_PROXY_SOCKS_PORT = "systemProxySocksPort";    // NOI18N
    public static final String SYSTEM_NON_PROXY_HOSTS = "systemProxyNonProxyHosts"; // NOI18N
    public static final String SYSTEM_PAC = "systemPAC";                            // NOI18N
    
    // Only for testing purpose (Test connection in General options panel)
    public static final String TEST_SYSTEM_PROXY_HTTP_HOST = "testSystemProxyHttpHost"; // NOI18N
    public static final String TEST_SYSTEM_PROXY_HTTP_PORT = "testSystemProxyHttpPort"; // NOI18N
    public static final String HTTP_CONNECTION_TEST_URL = "http://netbeans.org";        // NOI18N
    
    private static String presetNonProxyHosts;

    /** No proxy is used to connect. */
    public static final int DIRECT_CONNECTION = 0;
    
    /** Proxy setting is automatically detect in OS. */
    public static final int AUTO_DETECT_PROXY = 1; // as default
    
    /** Manually set proxy host and port. */
    public static final int MANUAL_SET_PROXY = 2;
    
    /** Proxy PAC file automatically detect in OS. */
    public static final int AUTO_DETECT_PAC = 3;
    
    /** Proxy PAC file manually set. */
    public static final int MANUAL_SET_PAC = 4;
    
    private static final Logger LOGGER = Logger.getLogger(ProxySettings.class.getName());
    
    static Preferences getPreferences() {
        return NbPreferences.root().node("org/netbeans/core");
    }
    
    
    public static String getHttpHost () {
        return normalizeProxyHost (getPreferences ().get (PROXY_HTTP_HOST, ""));
    }
    
    public static String getHttpPort () {
        return getPreferences ().get (PROXY_HTTP_PORT, "0");    // NOI18N
    }
    
    public static String getHttpsHost () {
        if (useProxyAllProtocols ()) {
            return getHttpHost ();
        } else {
            return getPreferences ().get (PROXY_HTTPS_HOST, "");
        }
    }
    
    public static String getHttpsPort () {
        if (useProxyAllProtocols ()) {
            return getHttpPort ();
        } else {
            return getPreferences ().get (PROXY_HTTPS_PORT, "0");   // NOI18N
        }
    }
    
    public static String getSocksHost () {
        if (useProxyAllProtocols ()) {
            return getHttpHost ();
        } else {
            return getPreferences ().get (PROXY_SOCKS_HOST, "");    // NOI18N
        }
    }
    
    public static String getSocksPort () {
        if (useProxyAllProtocols ()) {
            return getHttpPort ();
        } else {
            return getPreferences ().get (PROXY_SOCKS_PORT, "0");   // NOI18N
        }
    }
    
    public static String getNonProxyHosts () {
        String hosts = getPreferences ().get (NOT_PROXY_HOSTS, getDefaultUserNonProxyHosts ());
        return compactNonProxyHosts(hosts);
    }
    
    public static int getProxyType () {
        int type = getPreferences ().getInt (PROXY_TYPE, AUTO_DETECT_PROXY);
        if (AUTO_DETECT_PROXY == type) {
            type = ProxySettings.getSystemPac() != null ? AUTO_DETECT_PAC : AUTO_DETECT_PROXY;
        }
        return type;
    }
    
    
    public static String getSystemHttpHost() {
        return getPreferences().get(SYSTEM_PROXY_HTTP_HOST, "");
    }
    
    public static String getSystemHttpPort() {
        return getPreferences().get(SYSTEM_PROXY_HTTP_PORT, "");
    }
    
    public static String getSystemHttpsHost() {
        return getPreferences().get(SYSTEM_PROXY_HTTPS_HOST, "");
    }
    
    public static String getSystemHttpsPort() {
        return getPreferences().get(SYSTEM_PROXY_HTTPS_PORT, "");
    }
    
    public static String getSystemSocksHost() {
        return getPreferences().get(SYSTEM_PROXY_SOCKS_HOST, "");
    }
    
    public static String getSystemSocksPort() {
        return getPreferences().get(SYSTEM_PROXY_SOCKS_PORT, "");
    }
    
    public static String getSystemNonProxyHosts() {
        return getPreferences().get(SYSTEM_NON_PROXY_HOSTS, getModifiedNonProxyHosts(""));
    }
    
    public static String getSystemPac() {
        return getPreferences().get(SYSTEM_PAC, null);
    }
    
    
    public static String getTestSystemHttpHost() {
        return getPreferences().get(TEST_SYSTEM_PROXY_HTTP_HOST, "");
    }
    
    public static String getTestSystemHttpPort() {
        return getPreferences().get(TEST_SYSTEM_PROXY_HTTP_PORT, "");
    }
    
    
    public static boolean useAuthentication () {
        return getPreferences ().getBoolean (USE_PROXY_AUTHENTICATION, false);
    }
    
    public static boolean useProxyAllProtocols () {
        return getPreferences ().getBoolean (USE_PROXY_ALL_PROTOCOLS, false);
    }
    
    public static String getAuthenticationUsername () {
        return getPreferences ().get (PROXY_AUTHENTICATION_USERNAME, "");
    }
    
    public static char[] getAuthenticationPassword () {
        String old = getPreferences().get(PROXY_AUTHENTICATION_PASSWORD, null);
        if (old != null) {
            getPreferences().remove(PROXY_AUTHENTICATION_PASSWORD);
            setAuthenticationPassword(old.toCharArray());
        }
        char[] pwd = Keyring.read(PROXY_AUTHENTICATION_PASSWORD);
        return pwd != null ? pwd : new char[0];
    }
    
    public static void setAuthenticationPassword(char[] password) {
        Keyring.save(ProxySettings.PROXY_AUTHENTICATION_PASSWORD, password,
                // XXX consider including getHttpHost and/or getHttpsHost
                NbBundle.getMessage(ProxySettings.class, "ProxySettings.password.description"));    // NOI18N
    }

    public static void addPreferenceChangeListener (PreferenceChangeListener l) {
        getPreferences ().addPreferenceChangeListener (l);
    }
    
    public static void removePreferenceChangeListener (PreferenceChangeListener l) {
        getPreferences ().removePreferenceChangeListener (l);
    }
    
    private static String getPresetNonProxyHosts () {
        if (presetNonProxyHosts == null) {
            presetNonProxyHosts = System.getProperty ("http.nonProxyHosts", "");    // NOI18N
        }
        return presetNonProxyHosts;
    }
    
    private static String getDefaultUserNonProxyHosts () {
        return getModifiedNonProxyHosts (getSystemNonProxyHosts ());
    }

  
    private static String concatProxies(String... proxies) {
        StringBuilder sb = new StringBuilder();
        for (String n : proxies) {
            if (n == null) {
                continue;
            }
            n = n.trim();
            if (n.isEmpty()) {
                continue;
            }
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '|') { // NOI18N
                if (!n.startsWith("|")) {   // NOI18N
                    sb.append('|');         // NOI18N
                }
            }
            sb.append(n);
        }
        return sb.toString();
    }

    private static String getModifiedNonProxyHosts (String systemPreset) {
        String fromSystem = systemPreset.replaceAll (";", "|").replaceAll (",", "|"); //NOI18N
        String fromUser = getPresetNonProxyHosts () == null ? "" : getPresetNonProxyHosts ().replaceAll (";", "|").replaceAll (",", "|"); //NOI18N
        if (Utilities.isWindows ()) {
            fromSystem = addRegularToNonProxyHosts (fromSystem);
        }
        final String staticNonProxyHosts = NbBundle.getMessage(ProxySettings.class, "StaticNonProxyHosts"); // NOI18N
        String nonProxy = concatProxies(fromUser, fromSystem, staticNonProxyHosts); // NOI18N
        String localhost;
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
        return compactNonProxyHosts (nonProxy);
    }


    // avoid duplicate hosts
    private static String compactNonProxyHosts (String hosts) {
        StringTokenizer st = new StringTokenizer(hosts, ","); //NOI18N
        StringBuilder nonProxyHosts = new StringBuilder();
        while (st.hasMoreTokens()) {
            String h = st.nextToken().trim();
            if (h.length() == 0) {
                continue;
            }
            if (nonProxyHosts.length() > 0) {
                nonProxyHosts.append("|"); // NOI18N
            }
            nonProxyHosts.append(h);
        }
        st = new StringTokenizer (nonProxyHosts.toString(), "|"); //NOI18N
        Set<String> set = new HashSet<String> (); 
        StringBuilder compactedProxyHosts = new StringBuilder();
        while (st.hasMoreTokens ()) {
            String t = st.nextToken ();
            if (set.add (t.toLowerCase (Locale.US))) {
                if (compactedProxyHosts.length() > 0) {
                    compactedProxyHosts.append('|');    // NOI18N
                }
                compactedProxyHosts.append(t);
            }
        }
        return compactedProxyHosts.toString();
    }
    
    private static String addRegularToNonProxyHosts (String nonProxyHost) {
        StringTokenizer st = new StringTokenizer (nonProxyHost, "|");   // NOI18N
        StringBuilder regularProxyHosts = new StringBuilder();
        while (st.hasMoreTokens ()) {
            String t = st.nextToken ();
            if (t.indexOf ('*') == -1) { //NOI18N
                t = t + '*'; //NOI18N
            }
            if (regularProxyHosts.length() > 0) 
                regularProxyHosts.append('|');  // NOI18N
            regularProxyHosts.append(t);
        }

        return regularProxyHosts.toString();
    }

    public static String normalizeProxyHost (String proxyHost) {
        if (proxyHost.toLowerCase (Locale.US).startsWith ("http://")) { // NOI18N
            return proxyHost.substring (7, proxyHost.length ());
        } else {
            return proxyHost;
        }
    }
    
    private static InetSocketAddress analyzeProxy(URI uri) {
        Parameters.notNull("uri", uri);     // NOI18N
        List<Proxy> proxies = ProxySelector.getDefault().select(uri);
        assert proxies != null : "ProxySelector cannot return null for " + uri;     // NOI18N
        assert !proxies.isEmpty() : "ProxySelector cannot return empty list for " + uri;    // NOI18N
        String protocol = uri.getScheme();
        Proxy p = proxies.get(0);
        if (Proxy.Type.DIRECT == p.type()) {
            // return null for DIRECT proxy
            return null;
        }
        if (protocol == null
                || ((protocol.startsWith("http") || protocol.equals("ftp")) && Proxy.Type.HTTP == p.type()) // NOI18N
                || !(protocol.startsWith("http") || protocol.equals("ftp"))) {  // NOI18N
            if (p.address() instanceof InetSocketAddress) {
                // check is
                //assert ! ((InetSocketAddress) p.address()).isUnresolved() : p.address() + " must be resolved address.";
                return (InetSocketAddress) p.address();
            } else {
                LOGGER.log(Level.INFO, p.address() + " is not instanceof InetSocketAddress but " + p.address().getClass()); // NOI18N
                return null;
            }
        } else {
            return null;
        }
    }
    
    public static void reload() {
        Reloader reloader = Lookup.getDefault().lookup(Reloader.class);
        reloader.reload();
    }

    @ServiceProvider(service = NetworkSettings.ProxyCredentialsProvider.class, position = 1000)
    public static class NbProxyCredentialsProvider extends NetworkSettings.ProxyCredentialsProvider {

        @Override
        public String getProxyHost(URI u) {
            if (getPreferences() == null) {
                return null;
            }
            InetSocketAddress sa = analyzeProxy(u);
            return sa == null ? null : sa.getHostName();
        }

        @Override
        public String getProxyPort(URI u) {
            if (getPreferences() == null) {
                return null;
            }
            InetSocketAddress sa = analyzeProxy(u);
            return sa == null ? null : Integer.toString(sa.getPort());
        }

        @Override
        protected String getProxyUserName(URI u) {
            if (getPreferences() == null) {
                return null;
            }
            return ProxySettings.getAuthenticationUsername();
        }

        @Override
        protected char[] getProxyPassword(URI u) {
            if (getPreferences() == null) {
                return null;
            }
            return ProxySettings.getAuthenticationPassword();
        }

        @Override
        protected boolean isProxyAuthentication(URI u) {
            if (getPreferences() == null) {
                return false;
            }
            return getPreferences().getBoolean(USE_PROXY_AUTHENTICATION, false);
        }

    }
    
    /** A bridge between <code>o.n.core</code> and <code>core.network</code>.
     * An implementation of this class brings a facility to reload Network Proxy Settings
     * from underlying OS.
     * The module <code>core.network</code> provides a implementation which may be accessible
     * via <code>Lookup.getDefault()</code>. It's not guaranteed any implementation is found on all distribution. 
     * 
     * @since 3.40
     */
    public abstract static class Reloader {
        
        /** Reloads Network Proxy Settings from underlying system.
         *
         */
        public abstract void reload();
    }
}
