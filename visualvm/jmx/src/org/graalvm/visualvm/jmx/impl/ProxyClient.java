/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jmx.impl;

import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.security.sasl.SaslException;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.jmx.EnvironmentProvider;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Luis-Miguel Alventosa
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ProxyClient implements NotificationListener {

    private final static Logger LOGGER = Logger.getLogger(ProxyClient.class.getName());

    private static final int MODE_SELF = 0;
    private static final int MODE_LOCAL = 1;
    private static final int MODE_GENERIC = 2;
    private final int mode;
    private JmxModel.ConnectionState connectionState = JmxModel.ConnectionState.DISCONNECTED;
    private volatile boolean isDead = true;
    private String user = null;
    private char[] pword = null;
    private JmxModelImpl.LocalVirtualMachine lvm;
    private JMXServiceURL jmxUrl = null;
    private Application app;
    private EnvironmentProvider envProvider = null;
    private MBeanServerConnection conn = null;
    private JMXConnector jmxc = null;
    private static final SslRMIClientSocketFactory sslRMIClientSocketFactory = new SslRMIClientSocketFactory();
    private boolean insecure; // do not check for SSL-protected RMI registry
    private boolean checkSSLStub;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    // Self attach
    ProxyClient(Application app) throws IOException {
        this.app = app;
        this.mode = MODE_SELF;
    }

    // Local attach
    ProxyClient(Application app, JmxModelImpl.LocalVirtualMachine lvm) throws IOException {
        this.app = app;
        this.mode = MODE_LOCAL;
        this.lvm = lvm;
    }

    // Generic attach - host/port
    ProxyClient(Application app, int port) throws IOException {
        this(app, new JMXServiceURL("rmi", "", 0, createUrl(app.getHost().getHostName(), // NOI18N
        port)), null);
    }

    // Generic attach - connection string
    ProxyClient(Application app, String url) throws IOException {
        this(app, new JMXServiceURL(url), null);
    }

    // Generic attach - JmxApplication
    ProxyClient(JmxApplication jmxApp) throws IOException {
        this(jmxApp, jmxApp.getJMXServiceURL(), jmxApp.getEnvironmentProvider());
    }

    // Generic attach - JMXServiceURL
    private ProxyClient(Application app, JMXServiceURL url, EnvironmentProvider envProvider) throws IOException {
        this.mode = MODE_GENERIC;
        this.jmxUrl = url;
        this.app = app;
        this.envProvider = envProvider;
    }

    void setCredentials(String user, char[] pword) {
        this.user = user;
        this.pword = pword;
    }

    boolean hasSSLStubCheck() {
        return checkSSLStub;
    }

    void setInsecure() {
        insecure = true;
    }

    boolean isInsecure() {
        return insecure;
    }

    void addConnectionStateListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    void removeConnectionStateListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private static String createUrl(String hostName, int port) {
        return "/jndi/rmi://" + hostName + ":" + port + "/jmxrmi"; // NOI18N
    }

    private void setConnectionState(JmxModel.ConnectionState state) {
        JmxModel.ConnectionState oldState = connectionState;
        connectionState = state;
        propertyChangeSupport.firePropertyChange(JmxModelImpl.CONNECTION_STATE_PROPERTY, oldState, state);
    }

    JmxModel.ConnectionState getConnectionState() {
        return connectionState;
    }

    void connect() {
        while (true) {
            try {
                connectImpl();
                break;
            } catch (SecurityException e) {
                LOGGER.log(Level.INFO, "connect", e);   // NOI18N
                if (hasSSLStubCheck()) {
                    Storage storage = app.getStorage();
                    String noSSLProp = JmxApplicationProvider.PROPERTY_RETRY_WITHOUT_SSL;
                    String noSSL = storage.getCustomProperty(noSSLProp);
                    if (noSSL != null && Boolean.parseBoolean(noSSL)) { // NOI18N
                        setInsecure();
                        continue;
                    } else {
                        String conn = storage.getCustomProperty(DataSourceDescriptor.PROPERTY_NAME);
                        if (conn == null) conn = storage.getCustomProperty(ApplicationType.PROPERTY_SUGGESTED_NAME);
                        if (conn == null) conn = getUrl().toString();
                        String msg = NbBundle.getMessage(ProxyClient.class, "MSG_Insecure_SSL", conn);  // NOI18N
                        String title = NbBundle.getMessage(ProxyClient.class, "Title_Insecure_SSL");   // NOI18N
                        String retry = NbBundle.getMessage(ProxyClient.class, "Retry_Insecure_SSL");   // NOI18N
                        JLabel l = new JLabel(msg);
                        JCheckBox c = new JCheckBox();
                        Mnemonics.setLocalizedText(c, retry);
                        c.setSelected(noSSL == null);
                        JPanel p = new JPanel(new BorderLayout(0, 20));
                        p.add(l, BorderLayout.CENTER);
                        p.add(c, BorderLayout.SOUTH);
                        NotifyDescriptor dd = new NotifyDescriptor.Confirmation(p, title, NotifyDescriptor.YES_NO_OPTION);
                        if (DialogDisplayer.getDefault().notify(dd) == NotifyDescriptor.YES_OPTION) {
                            storage.setCustomProperty(noSSLProp, Boolean.toString(c.isSelected()));
                            setInsecure();
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (supplyCredentials() == null) {
                    break;
                }
            }
        }
    }

    /**
     *  Ask for security credentials.
     */
    CredentialsConfigurator supplyCredentials() {
        String displayName = app.getStorage().getCustomProperty(DataSourceDescriptor.PROPERTY_NAME);
        if (displayName == null) displayName = getUrl().toString();
        CredentialsConfigurator jsc = CredentialsConfigurator.supplyCredentials(displayName);
        if (jsc != null) setCredentials(jsc.getUsername(), jsc.getPassword());
        else if (app instanceof JmxApplication) ((JmxApplication)app).disableHeartbeat();
        return jsc;
    }


    private void connectImpl() {
        setConnectionState(JmxModel.ConnectionState.CONNECTING);
        try {
            tryConnect();
            setConnectionState(JmxModel.ConnectionState.CONNECTED);
        } catch (SecurityException e) {
            setConnectionState(JmxModel.ConnectionState.DISCONNECTED);
            throw e;
        } catch (SaslException e) {
            // Workaround for JBoss/WildFly authentication failed exception
            throw new SecurityException(e);
        } catch (Exception e) {
            setConnectionState(JmxModel.ConnectionState.DISCONNECTED);
            // Workaround for GlassFish's LoginException class not found
            if (e.toString().contains("com.sun.enterprise.security.LoginException")) {
                // NOI18N
                throw new SecurityException("Authentication failed! Invalid username or password"); // NOI18N
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                // Try to provide info on the target
                //    Use PID when attach was used to connect,
                //    Use JMXServiceURL otherwise...
                final String param = (lvm != null) ? String.valueOf(lvm.vmid()) : ((jmxUrl != null) ? jmxUrl.toString() : ""); // NOI18N
                LOGGER.log(Level.FINE, "connect(" + param + ")", e); // NOI18N
            }
        }
    }

    private void tryConnect() throws IOException {
        if (mode == MODE_SELF) {
            jmxc = null;
            conn = ManagementFactory.getPlatformMBeanServer();
        } else {
            if (mode == MODE_LOCAL) {
                if (!lvm.isManageable()) {
                    lvm.startManagementAgent();
                    if (!lvm.isManageable()) {
                        // FIXME: what to throw
                        throw new IOException(lvm + " not manageable"); // NOI18N
                    }
                }
                if (jmxUrl == null) {
                    jmxUrl = new JMXServiceURL(lvm.connectorAddress());
                }
            }
            Map<String, Object> env = new HashMap<>();
            if (envProvider != null) {
                env.putAll(envProvider.getEnvironment(app, app.getStorage()));
            }
            if (user != null || pword != null) {
                env.put(JMXConnector.CREDENTIALS, new String[]{user, new String(pword)});
            }
            if (!insecure && mode != MODE_LOCAL && env.get(JMXConnector.CREDENTIALS) != null) {
                env.put("jmx.remote.x.check.stub", "true"); // NOI18N
                checkSSLStub = true;
            } else {
                checkSSLStub = false;
            }
            jmxc = JMXConnectorFactory.newJMXConnector(jmxUrl, env);
            jmxc.addConnectionNotificationListener(this, null, null);
            try {
                jmxc.connect(env);
            } catch (java.io.IOException e) {
                // Likely a SSL-protected RMI registry
                if ("rmi".equals(jmxUrl.getProtocol())) { // NOI18N
                    env.put("com.sun.jndi.rmi.factory.socket", sslRMIClientSocketFactory); // NOI18N
                    jmxc.connect(env);
                } else {
                    throw e;
                }
            }
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            conn = JmxModelImpl.Checker.newChecker(this, mbsc);
        }
        isDead = false;
    }

    MBeanServerConnection getMBeanServerConnection() {
        return conn;
    }

    JMXServiceURL getUrl() {
        return jmxUrl;
    }

    void disconnect() {
        disconnectImpl(true);
    }

    synchronized void disconnectImpl(boolean sendClose) {
        // Close MBeanServer connection
        if (jmxc != null) {
            try {
                jmxc.removeConnectionNotificationListener(this);
                if (sendClose) {
                    jmxc.close();
                }
            } catch (IOException e) {
                // Ignore...
            } catch (ListenerNotFoundException e) {
                LOGGER.log(Level.INFO, "disconnectImpl", e); // NOI18N
            }
            jmxc = null;
        }
        // Set connection state to DISCONNECTED
        if (!isDead) {
            isDead = true;
            setConnectionState(JmxModel.ConnectionState.DISCONNECTED);
        }
    }

    synchronized void markAsDead() {
        disconnect();
    }

    boolean isDead() {
        return isDead;
    }

    boolean isConnected() {
        return !isDead();
    }

    public void handleNotification(Notification n, Object hb) {
        if (n instanceof JMXConnectionNotification) {
            if (JMXConnectionNotification.FAILED.equals(n.getType()) || JMXConnectionNotification.CLOSED.equals(n.getType())) {
                markAsDead();
            }
        }
    }
}
