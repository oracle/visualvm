/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.jmx.EnvironmentProvider;
import org.graalvm.visualvm.tools.attach.AttachModelFactory;
import org.graalvm.visualvm.tools.jmx.CachedMBeanServerConnection;
import org.graalvm.visualvm.tools.jmx.CachedMBeanServerConnectionFactory;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModelFactory;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
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
import org.graalvm.visualvm.core.VisualVM;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 * This class encapsulates the JMX functionality of the target Java application.
 *
 * Call {@link JmxModelFactory#getJmxModelFor()} to get an instance of the
 * {@link JmxModel} class.
 *
 * Usually this class will be used as follows:
 *
 * <pre>
 * JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
 * MBeanServerConnection mbsc = jmx.getMBeanServerConnection();
 * if (mbsc != null) {
 *    // Invoke JMX operations...
 * }
 * </pre>
 *
 * Several factory methods are available in {@link CachedMBeanServerConnectionFactory}
 * that can be used to work with a {@link CachedMBeanServerConnection} instead of a
 * plain {@link MBeanServerConnection}.
 *
 * In case the JMX connection is not established yet, you could register
 * a listener on the {@code JmxModel} for ConnectionState property changes.
 * The JmxModel notifies any PropertyChangeListeners about the ConnectionState
 * property change to CONNECTED and DISCONNECTED. The JmxModel instance will
 * be the source for any generated events.
 *
 * Polling for the ConnectionState is also possible by calling
 * {@link JmxModel#getConnectionState()}.
 *
 * @author Luis-Miguel Alventosa
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class JmxModelImpl extends JmxModel {
//    private static final String PROPERTY_USERNAME = "prop_username";    // NOI18N
//    private static final String PROPERTY_PASSWORD = "prop_password";    // NOI18N
    private final static Logger LOGGER = Logger.getLogger(JmxModelImpl.class.getName());
    private ProxyClient client;
    private ApplicationRemovedListener removedListener;
    private ApplicationAvailabilityListener availabilityListener;
    private JmxSupport jmxSupport;
    private final Object jmxSupportLock = new Object();
    
    /**
     * Creates an instance of {@code JmxModel} for a {@link JvmstatApplication}.
     *
     * @param application the {@link JvmstatApplication}.
     */
    public JmxModelImpl(Application application, JvmstatModel jvmstat) {
        try {
            JvmJvmstatModel jvmstatModel = JvmJvmstatModelFactory.getJvmstatModelFor(application);
            // Create ProxyClient (i.e. create the JMX connection to the JMX agent)
            ProxyClient proxyClient = null;
            if (Application.CURRENT_APPLICATION.equals(application)) {
                // Monitor self
                proxyClient = new ProxyClient(this);
            } else if (application.isLocalApplication()) {
                // Create a ProxyClient from local pid
                String connectorAddress = jvmstat.findByName("sun.management.JMXConnectorServer.address"); // NOI18N
                String javaHome = jvmstat.findByName("java.property.java.home");    // NOI18N
                LocalVirtualMachine lvm = new LocalVirtualMachine(application.getPid(), AttachModelFactory.getAttachFor(application) != null, connectorAddress, javaHome);
                if (!lvm.isManageable()) {
                    if (lvm.isAttachable()) {
                        proxyClient = new ProxyClient(this, lvm);
                    } else {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.warning("The JMX management agent " +    // NOI18N
                                    "cannot be enabled in this application (pid " + // NOI18N
                                    application.getPid() + ")");  // NOI18N
                        }
                    }
                } else {
                    proxyClient = new ProxyClient(this, lvm);
                }
            }
            if (proxyClient == null) {
                // Create a ProxyClient for the remote out-of-the-box
                // JMX management agent using the port and security
                // related information retrieved through jvmstat.
                List<String> urls = jvmstat.findByPattern("sun.management.JMXConnectorServer.[0-9]+.remoteAddress"); // NOI18N
                if (urls.size() != 0) {
                    List<String> auths = jvmstat.findByPattern("sun.management.JMXConnectorServer.[0-9]+.authenticate"); // NOI18N
                    proxyClient = new ProxyClient(this, urls.get(0));
                    if ("true".equals(auths.get(0))) {  // NOI18N
                        supplyCredentials(application, proxyClient);
                    }
                } else {
                    // Create a ProxyClient for the remote out-of-the-box
                    // JMX management agent using the port specified in
                    // the -Dcom.sun.management.jmxremote.port=<port>
                    // system property
                    String jvmArgs = jvmstatModel.getJvmArgs();
                    StringTokenizer st = new StringTokenizer(jvmArgs);
                    int port = -1;
                    boolean authenticate = false;
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if (token.startsWith("-Dcom.sun.management.jmxremote.port=")) { // NOI18N
                            port = Integer.parseInt(token.substring(token.indexOf("=") + 1)); // NOI18N
                        } else if (token.equals("-Dcom.sun.management.jmxremote.authenticate=true")) { // NOI18N
                            authenticate = true;
                        }
                    }
                    if (port != -1) {
                        proxyClient = new ProxyClient(this, application.getHost(), port);
                        if (authenticate) {
                            supplyCredentials(application, proxyClient);
                        }
                    }
                }
            }
            if (proxyClient != null) {
                client = proxyClient;
                removedListener = new ApplicationRemovedListener();
                availabilityListener = new ApplicationAvailabilityListener();
                connect(application, proxyClient, removedListener, availabilityListener);
            }
        } catch (Exception e) {
            LOGGER.throwing(JmxModelImpl.class.getName(), "<init>", e); // NOI18N
            client = null;
        }
    }

    /**
     * Creates an instance of {@code JmxModel} for a {@link JmxApplication}.
     *
     * @param application the {@link JmxApplication}.
     */
    public JmxModelImpl(JmxApplication application) {
        try {
            final ProxyClient proxyClient = new ProxyClient(this, application);
            client = proxyClient;
            removedListener = new ApplicationRemovedListener();
            availabilityListener = new ApplicationAvailabilityListener();
            connect(application, proxyClient, removedListener, availabilityListener);
        } catch (Exception e) {
            LOGGER.throwing(JmxModelImpl.class.getName(), "<init>", e); // NOI18N
            client = null;
        }
    }

    private void connect(Application application, ProxyClient proxyClient,
            ApplicationRemovedListener listener, ApplicationAvailabilityListener aListener) {
        while (true) {
            try {
                proxyClient.connect();
                application.notifyWhenRemoved(listener);
                if (getConnectionState() == ConnectionState.CONNECTED) {
                    application.addPropertyChangeListener(Stateful.PROPERTY_STATE, aListener);
                }
                break;
            } catch (SecurityException e) {
                LOGGER.log(Level.INFO, "connect", e);   // NOI18N
                if (proxyClient.hasSSLStubCheck()) {
                    Storage storage = application.getStorage();
                    String noSSLProp = JmxApplicationProvider.PROPERTY_RETRY_WITHOUT_SSL;
                    String noSSL = storage.getCustomProperty(noSSLProp);
                    if (noSSL != null && Boolean.parseBoolean(noSSL)) { // NOI18N
                        proxyClient.setInsecure();
                        continue;
                    } else {
                        String conn = storage.getCustomProperty(DataSourceDescriptor.PROPERTY_NAME);
                        if (conn == null) conn = storage.getCustomProperty(ApplicationType.PROPERTY_SUGGESTED_NAME);
                        if (conn == null) conn = proxyClient.getUrl().toString();
                        String msg = NbBundle.getMessage(JmxModelImpl.class, "MSG_Insecure_SSL", conn);  // NOI18N
                        String title = NbBundle.getMessage(JmxModelImpl.class, "Title_Insecure_SSL");   // NOI18N
                        String retry = NbBundle.getMessage(JmxModelImpl.class, "Retry_Insecure_SSL");   // NOI18N
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
                            proxyClient.setInsecure();
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (supplyCredentials(application, proxyClient) == null) {
                    break;
                }
            }
        }
    }

    /**
     *  Ask for security credentials.
     */
    private CredentialsConfigurator supplyCredentials(
            Application application, ProxyClient proxyClient) {
        String displayName = application.getStorage().getCustomProperty(DataSourceDescriptor.PROPERTY_NAME);
        if (displayName == null) displayName = proxyClient.getUrl().toString();
        CredentialsConfigurator jsc =
                CredentialsConfigurator.supplyCredentials(displayName);
        if (jsc != null)
            proxyClient.setCredentials(jsc.getUsername(), jsc.getPassword());
        return jsc;
    }

    /**
     * Returns the current connection state.
     *
     * @return the current connection state.
     */
    public ConnectionState getConnectionState() {
        if (client != null) {
            return client.getConnectionState();
        }
        return ConnectionState.DISCONNECTED;
    }

    /**
     * Returns the {@link MBeanServerConnection} for the connection to
     * an application. The returned {@code MBeanServerConnection} object
     * becomes invalid when the connection state is changed to the
     * {@link ConnectionState#DISCONNECTED DISCONNECTED} state.
     *
     * @return the {@code MBeanServerConnection} for the
     * connection to an application. It returns {@code null}
     * if the JMX connection couldn't be established.
     */
    public MBeanServerConnection getMBeanServerConnection() {
        if (client != null) {
            return client.getMBeanServerConnection();
        }
        return null;
    }

    /**
     * Returns the {@link JMXServiceURL} associated to this (@code JmxModel}.
     *
     * @return the {@link JMXServiceURL} associated to this (@code JmxModel}.
     */
    public JMXServiceURL getJMXServiceURL() {
        if (client != null) {
            return client.getUrl();
        }
        return null;        
    }

    public Properties getSystemProperties() {
        return getJmxSupport().getSystemProperties();
    }

    public boolean takeHeapDump(String fileName) {
        return getJmxSupport().takeHeapDump(fileName);
    }

    public String takeThreadDump() {
        return getJmxSupport().takeThreadDump();
    }

    public String takeThreadDump(long[] threadIds) {
        return getJmxSupport().takeThreadDump(threadIds);
    }    

    public HeapHistogram takeHeapHistogram() {
        return getJmxSupport().takeHeapHistogram();
    }

    public String getFlagValue(String name) {
        return getJmxSupport().getFlagValue(name);
    }

    public void setFlagValue(String name, String value) {
        getJmxSupport().setFlagValue(name,value);
    }

    public boolean isTakeHeapDumpSupported() {
        JmxSupport support = getJmxSupport();
        return support.getHotSpotDiagnostic() != null && !support.isReadOnlyConnection();
    }
    
    public boolean isTakeThreadDumpSupported() {
        JmxSupport support = getJmxSupport();
        return support.getThreadBean() != null && !support.isReadOnlyConnection();
    }

    public String getCommandLine() {
        return getJmxSupport().getCommandLine();
    }

    private JmxSupport getJmxSupport() {
        synchronized (jmxSupportLock) {
            if (jmxSupport == null) {
                jmxSupport = new JmxSupport(this);
            }
            return jmxSupport;
        }
    }

    /**
     * Disconnect from JMX agent when the application is removed.
     */
    private class ApplicationRemovedListener implements DataRemovedListener<Application> {

        public void dataRemoved(Application application) {
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    client.markAsDead();
                    removedListener = null;
                }
            });
        }
    }
    
    private class ApplicationAvailabilityListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (!evt.getNewValue().equals(Stateful.STATE_AVAILABLE)) {
                ((Application)evt.getSource()).removePropertyChangeListener(
                        Stateful.PROPERTY_STATE, this);
                client.disconnectImpl(false);
                availabilityListener = null;
            }
        }
    }
    
    private static class ProxyClient implements NotificationListener {

        private static final int MODE_SELF = 0;
        private static final int MODE_LOCAL = 1;
        private static final int MODE_GENERIC = 2;

        private final int mode;

        private ConnectionState connectionState = ConnectionState.DISCONNECTED;
        private volatile boolean isDead = true;
        private String user = null;
        private char[] pword = null;
        private LocalVirtualMachine lvm;
        private JMXServiceURL jmxUrl = null;
        private Application app;
        private EnvironmentProvider envProvider = null;
        private MBeanServerConnection conn = null;
        private JMXConnector jmxc = null;
        private static final SslRMIClientSocketFactory sslRMIClientSocketFactory =
                new SslRMIClientSocketFactory();
        private final JmxModelImpl model;
        private boolean insecure;   // do not check for SSL-protected RMI registry
        private boolean checkSSLStub;


        // Self attach
        public ProxyClient(JmxModelImpl model) throws IOException {
            this.mode = MODE_SELF;
            this.model = model;
        }

        // Local attach
        public ProxyClient(JmxModelImpl model, LocalVirtualMachine lvm) throws IOException {
            this.mode = MODE_LOCAL;
            this.model = model;
            this.lvm = lvm;
        }

        // Generic attach - host/port
        public ProxyClient(JmxModelImpl model, Host host, int port) throws IOException {
            this(model, new JMXServiceURL("rmi", "", 0, createUrl(host.getHostName(), // NOI18N
                                          port)), null, null);
        }

        // Generic attach - connection string
        public ProxyClient(JmxModelImpl model, String url) throws IOException {
            this(model, new JMXServiceURL(url), null, null);
        }

        // Generic attach - JmxApplication
        public ProxyClient(JmxModelImpl model, JmxApplication jmxApp) throws IOException {
            this(model, jmxApp.getJMXServiceURL(), jmxApp, jmxApp.getEnvironmentProvider());
        }

        // Generic attach - JMXServiceURL
        private ProxyClient(JmxModelImpl model, JMXServiceURL url, Application app,
                           EnvironmentProvider envProvider) throws IOException {
            this.mode = MODE_GENERIC;
            this.model = model;
            this.jmxUrl = url;
            this.app = app;
            this.envProvider = envProvider;
        }

        public void setCredentials(String user, char[] pword) {
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
        
        private static String createUrl(String hostName, int port) {
            return "/jndi/rmi://" + hostName + ":" + port + "/jmxrmi";  // NOI18N
        }

        private void setConnectionState(ConnectionState state) {
            ConnectionState oldState = connectionState;
            connectionState = state;
            model.propertyChangeSupport.firePropertyChange(
                    JmxModelImpl.CONNECTION_STATE_PROPERTY, oldState, state);
        }

        public ConnectionState getConnectionState() {
            return connectionState;
        }

        void connect() {
            setConnectionState(ConnectionState.CONNECTING);
            try {
                tryConnect();
                setConnectionState(ConnectionState.CONNECTED);
            } catch (SecurityException e) {
                setConnectionState(ConnectionState.DISCONNECTED);
                throw e;
            } catch (SaslException e) {
                // Workaround for JBoss/WildFly authentication failed exception     
                throw new SecurityException(e);
            } catch (Exception e) {
                setConnectionState(ConnectionState.DISCONNECTED);
                // Workaround for GlassFish's LoginException class not found
                if (e.toString().contains("com.sun.enterprise.security.LoginException")) {  // NOI18N
                    throw new SecurityException("Authentication failed! Invalid username or password"); // NOI18N
                }
                if (LOGGER.isLoggable(Level.INFO))  {
                    // Try to provide info on the target
                    //    Use PID when attach was used to connect,
                    //    Use JMXServiceURL otherwise...
                    final String param = 
                            (lvm != null) ? String.valueOf(lvm.vmid())
                            : ((jmxUrl != null) ? jmxUrl.toString() : ""); // NOI18N
                    LOGGER.log(Level.INFO, "connect(" + param + ")", e); // NOI18N
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

                Map<String, Object> env = new HashMap();
                if (envProvider != null)
                    env.putAll(envProvider.getEnvironment(app, app.getStorage()));
                if (user != null || pword != null)
                    env.put(JMXConnector.CREDENTIALS, new String[] { user, new String(pword) });

                if (!insecure && mode != MODE_LOCAL && env.get(JMXConnector.CREDENTIALS) != null) {
                    env.put("jmx.remote.x.check.stub", "true");     // NOI18N
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
                conn = Checker.newChecker(this, mbsc);
            }
            isDead = false;
        }

        public MBeanServerConnection getMBeanServerConnection() {
            return conn;
        }

        public JMXServiceURL getUrl() {
            return jmxUrl;
        }
        
        public void disconnect() {
            disconnectImpl(true);
        }

        private synchronized void disconnectImpl(boolean sendClose) {
            // Close MBeanServer connection
            if (jmxc != null) {
                try {
                    jmxc.removeConnectionNotificationListener(this);
                    if (sendClose) jmxc.close();
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
                setConnectionState(ConnectionState.DISCONNECTED);
            }
        }

        public synchronized void markAsDead() {
            disconnect();
        }

        public boolean isDead() {
            return isDead;
        }

        boolean isConnected() {
            return !isDead();
        }

        public void handleNotification(Notification n, Object hb) {
            if (n instanceof JMXConnectionNotification) {
                if (JMXConnectionNotification.FAILED.equals(n.getType()) ||
                        JMXConnectionNotification.CLOSED.equals(n.getType())) {
                    markAsDead();
                }
            }
        }
    }

    private static class Checker {

        private Checker() {
        }

        public static MBeanServerConnection newChecker(
                ProxyClient client, MBeanServerConnection mbsc) {
            final InvocationHandler ih = new CheckerInvocationHandler(mbsc);
            return (MBeanServerConnection) Proxy.newProxyInstance(
                    Checker.class.getClassLoader(),
                    new Class[]{MBeanServerConnection.class},
                    ih);
        }
    }

    private static class CheckerInvocationHandler implements InvocationHandler {

        private final MBeanServerConnection conn;

        CheckerInvocationHandler(MBeanServerConnection conn) {
            this.conn = conn;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (LOGGER.isLoggable(Level.FINE)) {
                // Check if MBeanServerConnection call is performed on EDT
                if (EventQueue.isDispatchThread()) {
                    Throwable thrwbl = new Throwable();

                    LOGGER.log(Level.FINE, createTracedMessage("MBeanServerConnection call " +  // NOI18N
                            "performed on Event Dispatch Thread!", thrwbl));    // NOI18N
                }
            }
            // Invoke MBeanServerConnection call
            try {
                return method.invoke(conn, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private String createTracedMessage(String message, Throwable thrwbl) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);
            pw.println(message);
            thrwbl.printStackTrace(pw);
            pw.flush();
            return baos.toString();
        }
    }

    private static class LocalVirtualMachine {

        private static final String ENABLE_LOCAL_AGENT_JCMD = "ManagementAgent.start_local";  // NOI18N
        
        private int vmid;
        private boolean isAttachSupported;
        private String javaHome;
        
        // @GuardedBy this
        volatile private String address;

        public LocalVirtualMachine(int vmid, boolean canAttach, String connectorAddress, String home) {
            this.vmid = vmid;
            this.address = connectorAddress;
            this.isAttachSupported = canAttach;
            this.javaHome = home;
        }

        public int vmid() {
            return vmid;
        }

        public synchronized boolean isManageable() {
            return (address != null);
        }

        public boolean isAttachable() {
            return isAttachSupported;
        }

        public synchronized void startManagementAgent() throws IOException {
            if (address != null) {
                // already started
                return;
            }

            if (!isAttachable()) {
                throw new IOException("This virtual machine \"" + vmid +    // NOI18N
                        "\" does not support dynamic attach."); // NOI18N
            }

            loadManagementAgent();
            // fails to load or start the management agent
            if (address == null) {
                // should never reach here
                throw new IOException("Fails to find connector address");   // NOI18N
            }
        }

        public synchronized String connectorAddress() {
            // return null if not available or no JMX agent
            return address;
        }
        private static final String LOCAL_CONNECTOR_ADDRESS_PROP =
                "com.sun.management.jmxremote.localConnectorAddress";   // NOI18N

        private synchronized void loadManagementAgent() throws IOException {
            VirtualMachine vm = null;
            String name = String.valueOf(vmid);
            try {
                vm = VirtualMachine.attach(name);
            } catch (AttachNotSupportedException x) {
                throw new IOException(x);
            }
            // try to enable local JMX via jcmd command
            if (!loadManagementAgentViaJcmd(vm)) {
                // load the management agent into the target VM
                loadManagementAgentViaJar(vm);
            }

            // get the connector address
            Properties agentProps = vm.getAgentProperties();
            address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);

            vm.detach();
        }

        private void loadManagementAgentViaJar(VirtualMachine vm) throws IOException {
            // Normally in ${java.home}/jre/lib/management-agent.jar but might
            // be in ${java.home}/lib in build environments.

            String agent = javaHome + File.separator + "jre" + File.separator + // NOI18N
                    "lib" + File.separator + "management-agent.jar";    // NOI18N
            File f = new File(agent);
            if (!f.exists()) {
                agent = javaHome + File.separator + "lib" + File.separator +    // NOI18N
                        "management-agent.jar"; // NOI18N
                f = new File(agent);
                if (!f.exists()) {
                    throw new IOException("Management agent not found");    // NOI18N
                }
            }

            agent = f.getCanonicalPath();
            try {
                vm.loadAgent(agent, "com.sun.management.jmxremote");    // NOI18N
            } catch (AgentLoadException x) {
                throw new IOException(x);
            } catch (AgentInitializationException x) {
                throw new IOException(x);
            }
        }

        private boolean loadManagementAgentViaJcmd(VirtualMachine vm) throws IOException {
            if (vm instanceof HotSpotVirtualMachine) {
                HotSpotVirtualMachine hsvm = (HotSpotVirtualMachine) vm;
                InputStream in = null;
                try {
                    byte b[] = new byte[256];
                    int n;
                    
                    in = hsvm.executeJCmd(ENABLE_LOCAL_AGENT_JCMD);
                    do {
                        n = in.read(b);
                        if (n > 0) {
                            String s = new String(b, 0, n, "UTF-8");    // NOI18N
                            System.out.print(s);
                        }
                    } while (n > 0);
                    return true;
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, "jcmd command \""+ENABLE_LOCAL_AGENT_JCMD+"\" for PID "+vmid+" failed", ex); // NOI18N
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
            return false;
        }
    }
}
