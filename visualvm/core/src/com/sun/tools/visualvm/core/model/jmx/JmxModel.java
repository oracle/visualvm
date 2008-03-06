/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.model.jmx;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.visualvm.core.application.ApplicationSecurityConfigurator;
import com.sun.tools.visualvm.core.application.JmxApplication;
import com.sun.tools.visualvm.core.application.JvmstatApplication;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasupport.Storage;
import com.sun.tools.visualvm.core.model.Model;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import com.sun.tools.visualvm.core.model.jvm.JvmstatJVM;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.RemoteRef;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.swing.event.SwingPropertyChangeSupport;
import sun.rmi.server.UnicastRef2;
import sun.rmi.transport.LiveRef;

/**
 * This class encapsulates the JMX functionality of the target Java application.
 *
 * Call {@link JmxModelFactory.getJmxModelFor()} to get an instance of the
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
 * {@link JmxModel.getCachedMBeanServerConnection()} should be called
 * if you want to get a {@link CachedMBeanServerConnection} instead of
 * a plain MBeanServerConnection.
 *
 * In case the JMX connection is not established yet, you could register
 * a listener on the {@code JmxModel} for ConnectionState property changes.
 * The JmxModel notifies any PropertyChangeListeners about the ConnectionState
 * property change to CONNECTED and DISCONNECTED. The JmxModel instance will
 * be the source for any generated events.
 *
 * Polling for the ConnectionState is also possible by calling
 * {@link JmxModel.getConnectionState()}.
 *
 * @author Luis-Miguel Alventosa
 */
public class JmxModel extends Model {

    private static final String PROPERTY_USERNAME = "prop_username";
    private static final String PROPERTY_PASSWORD = "prop_password";
    private final static Logger LOGGER = Logger.getLogger(JmxModel.class.getName());
    private ProxyClient client;
    private SwingPropertyChangeSupport propertyChangeSupport =
            new SwingPropertyChangeSupport(this, true);
    /**
     * The {@link ConnectionState ConnectionState} bound property name.
     */
    public static String CONNECTION_STATE_PROPERTY = "connectionState";

    /**
     * Values for the {@linkplain #CONNECTION_STATE_PROPERTY
     * <i>ConnectionState</i>} bound property.
     */
    public enum ConnectionState {

        /**
         * The connection has been successfully established.
         */
        CONNECTED,
        /**
         * No connection present.
         */
        DISCONNECTED,
        /**
         * The connection is being attempted.
         */
        CONNECTING
    }

    /**
     * Creates an instance of {@code JmxModel} for a {@link JvmstatApplication}.
     *
     * @param application the {@link JvmstatApplication}.
     */
    public JmxModel(JvmstatApplication application) {
        try {
            JvmstatJVM jvm = (JvmstatJVM) JVMFactory.getJVMFor(application);
            Storage storage = application.getStorage();
            String username = storage.getCustomProperty(PROPERTY_USERNAME);
            String password = storage.getCustomProperty(PROPERTY_PASSWORD);
            // Create ProxyClient (i.e. create the JMX connection to the JMX agent)
            ProxyClient proxyClient = null;
            if (Application.CURRENT_APPLICATION.equals(application)) {
                // Monitor self
                proxyClient = new ProxyClient(this, "localhost", 0, null, null); // NOI18N
            } else if (application.isLocalApplication()) {
                // Create a ProxyClient from local pid
                String connectorAddress = jvm.findByName("sun.management.JMXConnectorServer.address"); // NOI18N
                LocalVirtualMachine lvm = new LocalVirtualMachine(application.getPid(), jvm.isAttachable(), connectorAddress);
                if (!lvm.isManageable()) {
                    if (lvm.isAttachable()) {
                        proxyClient = new ProxyClient(this, lvm);
                    } else {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.warning("The JMX management agent " +
                                    "cannot be enabled in this application (pid " +
                                    application.getPid() + ")"); // NOI18N
                        }
                    }
                } else {
                    proxyClient = new ProxyClient(this, lvm);
                }
            } else {
                // Create a ProxyClient for the remote out-of-the-box
                // JMX management agent using the port and security
                // related information retrieved through jvmstat.
                List<String> urls = jvm.findByPattern("sun.management.JMXConnectorServer.[0-9]+.address"); // NOI18N
                if (urls.size() != 0) {
                    List<String> auths = jvm.findByPattern("sun.management.JMXConnectorServer.[0-9]+.authenticate"); // NOI18N
                    proxyClient = new ProxyClient(this, urls.get(0), username, password);
                    if (username != null && "true".equals(auths.get(0))) {
                        supplyCredentials(application, proxyClient);
                    }
                } else {
                    // Create a ProxyClient for the remote out-of-the-box
                    // JMX management agent using the port specified in
                    // the -Dcom.sun.management.jmxremote.port=<port>
                    // system property
                    String jvmArgs = jvm.getJvmArgs();
                    StringTokenizer st = new StringTokenizer(jvmArgs);
                    int port = -1;
                    boolean authenticate = false;
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if (token.startsWith("-Dcom.sun.management.jmxremote.port=")) { // NOI18N
                            port = Integer.parseInt(token.substring(token.indexOf("=") + 1));
                        } else if (token.equals("-Dcom.sun.management.jmxremote.authenticate=true")) { // NOI18N
                            authenticate = true;
                        }
                    }
                    if (port != -1) {
                        proxyClient = new ProxyClient(this,
                                application.getHost().getHostName(),
                                port, username, password);
                        if (username != null && authenticate) {
                            supplyCredentials(application, proxyClient);
                        }
                    }
                }
            }
            if (proxyClient != null) {
                client = proxyClient;
                connect(application, proxyClient);
            }
        } catch (Exception e) {
            client = null;
            e.printStackTrace();
        }
    }

    /**
     * Creates an instance of {@code JmxModel} for a {@link JmxApplication}.
     *
     * @param application the {@link JmxApplication}.
     */
    public JmxModel(JmxApplication application) {
        try {
            JMXServiceURL url = application.getJMXServiceURL();
            Storage storage = application.getStorage();
            String username = storage.getCustomProperty(PROPERTY_USERNAME);
            String password = storage.getCustomProperty(PROPERTY_PASSWORD);
            final ProxyClient proxyClient =
                    new ProxyClient(this, url.toString(), username, password);
            client = proxyClient;
            connect(application, proxyClient);
        } catch (Exception e) {
            client = null;
            e.printStackTrace();
        }
    }

    private void connect(Application application, ProxyClient proxyClient) {
        while (true) {
            try {
                proxyClient.connect();
                break;
            } catch (SecurityException e) {
                if (supplyCredentials(application, proxyClient) == null) {
                    break;
                }
            }
        }
    }

    /**
     *  Ask for security credentials.
     */
    private ApplicationSecurityConfigurator supplyCredentials(
            Application application, ProxyClient proxyClient) {
        ApplicationSecurityConfigurator jsc =
                ApplicationSecurityConfigurator.supplyCredentials(proxyClient.getUrl().toString());
        if (jsc != null) {
            proxyClient.setParameters(proxyClient.getUrl(), jsc.getUsername(), jsc.getPassword());
            Storage storage = application.getStorage();
            storage.setCustomProperty(PROPERTY_USERNAME, jsc.getUsername());
            storage.setCustomProperty(PROPERTY_PASSWORD, jsc.getPassword());
        }
        return jsc;
    }

    /**
     * Add a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If {@code listener} is {@code null}, no exception is thrown and
     * no action is taken.
     *
     * @param listener the {@code PropertyChangeListener} to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    private void addWeakPropertyChangeListener(PropertyChangeListener listener) {
        if (!(listener instanceof WeakPCL)) {
            listener = new WeakPCL(listener);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * from the listener list. This
     * removes a {@code PropertyChangeListener} that was registered for all
     * properties. If {@code listener} was added more than once to the same
     * event source, it will be notified one less time after being removed. If
     * {@code listener} is {@code null}, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener the {@code PropertyChangeListener} to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (!(listener instanceof WeakPCL)) {
            // Search for the WeakPCL holding this listener (if any)
            for (PropertyChangeListener pcl : propertyChangeSupport.getPropertyChangeListeners()) {
                if (pcl instanceof WeakPCL && ((WeakPCL) pcl).get() == listener) {
                    listener = pcl;
                    break;
                }
            }
        }
        propertyChangeSupport.removePropertyChangeListener(listener);
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
     * Returns the {@link MBeanServerConnection MBeanServerConnection} for the
     * connection to an application. The returned {@code MBeanServerConnection}
     * object becomes invalid when the connection state is changed to the
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
     * Returns the {@link CachedMBeanServerConnection cached MBeanServerConnection}
     * for the connection to an application. The returned {@code CachedMBeanServerConnection}
     * object becomes invalid when the connection state is changed to the
     * {@link ConnectionState#DISCONNECTED DISCONNECTED} state.
     *
     * @return the {@code CachedMBeanServerConnection} for the
     * connection to an application. It returns {@code null}
     * if the JMX connection couldn't be established.
     */
    public CachedMBeanServerConnection getCachedMBeanServerConnection() {
        if (client != null) {
            return client.getCachedMBeanServerConnection();
        }
        return null;
    }

    static class ProxyClient implements NotificationListener {

        private ConnectionState connectionState = ConnectionState.DISCONNECTED;
        private volatile boolean isDead = true;
        private String hostName = null;
        private int port = 0;
        private String userName = null;
        private String password = null;
        private LocalVirtualMachine lvm;
        private JMXServiceURL jmxUrl = null;
        private MBeanServerConnection conn = null;
        private CachedMBeanServerConnection cachedConn = null;
        private JMXConnector jmxc = null;
        private RMIServer stub = null;
        private static final SslRMIClientSocketFactory sslRMIClientSocketFactory =
                new SslRMIClientSocketFactory();
        private String registryHostName = null;
        private int registryPort = 0;
        private boolean vmConnector = false;
        private boolean sslRegistry = false;
        private boolean sslStub = false;
        private final String connectionName;
        private final String displayName;
        private final JmxModel model;

        public ProxyClient(JmxModel model, String hostName, int port,
                String userName, String password) throws IOException {
            this.model = model;
            this.connectionName = getConnectionName(hostName, port, userName);
            this.displayName = connectionName;
            if (hostName.equals("localhost") && port == 0) {
                // Monitor self
                this.hostName = hostName;
                this.port = port;
            } else {
                // Create an RMI connector client and connect it to
                // the RMI connector server
                final String urlPath = "/jndi/rmi://" + hostName + ":" + port +
                        "/jmxrmi";
                JMXServiceURL url = new JMXServiceURL("rmi", "", 0, urlPath);
                setParameters(url, userName, password);
                vmConnector = true;
                registryHostName = hostName;
                registryPort = port;
                checkSslConfig();
            }
        }

        public ProxyClient(JmxModel model, String url,
                String userName, String password) throws IOException {
            this.model = model;
            this.connectionName = getConnectionName(url, userName);
            this.displayName = connectionName;
            setParameters(new JMXServiceURL(url), userName, password);
        }

        public ProxyClient(JmxModel model, LocalVirtualMachine lvm)
                throws IOException {
            this.model = model;
            this.lvm = lvm;
            this.connectionName = getConnectionName(lvm);
            this.displayName = "pid: " + lvm.vmid();
        }

        private void setParameters(JMXServiceURL url,
                String userName, String password) {
            this.jmxUrl = url;
            if (jmxUrl != null) {
                this.hostName = jmxUrl.getHost();
                this.port = jmxUrl.getPort();
            }
            this.userName = userName;
            this.password = password;
        }

        private static void checkStub(Remote stub,
                Class<? extends Remote> stubClass) {
            // Check remote stub is from the expected class.
            //
            if (stub.getClass() != stubClass) {
                if (!Proxy.isProxyClass(stub.getClass())) {
                    throw new SecurityException(
                            "Expecting a " + stubClass.getName() + " stub!");
                } else {
                    InvocationHandler handler = Proxy.getInvocationHandler(stub);
                    if (handler.getClass() != RemoteObjectInvocationHandler.class) {
                        throw new SecurityException(
                                "Expecting a dynamic proxy instance with a " +
                                RemoteObjectInvocationHandler.class.getName() +
                                " invocation handler!");
                    } else {
                        stub = (Remote) handler;
                    }
                }
            }
            // Check RemoteRef in stub is from the expected class
            // "sun.rmi.server.UnicastRef2".
            //
            RemoteRef ref = ((RemoteObject) stub).getRef();
            if (ref.getClass() != UnicastRef2.class) {
                throw new SecurityException(
                        "Expecting a " + UnicastRef2.class.getName() +
                        " remote reference in stub!");
            }
            // Check RMIClientSocketFactory in stub is from the expected class
            // "javax.rmi.ssl.SslRMIClientSocketFactory".
            //
            LiveRef liveRef = ((UnicastRef2) ref).getLiveRef();
            RMIClientSocketFactory csf = liveRef.getClientSocketFactory();
            if (csf == null || csf.getClass() != SslRMIClientSocketFactory.class) {
                throw new SecurityException(
                        "Expecting a " + SslRMIClientSocketFactory.class.getName() +
                        " RMI client socket factory in stub!");
            }
        }
        private static final String rmiServerImplStubClassName =
                "javax.management.remote.rmi.RMIServerImpl_Stub";
        private static final Class<? extends Remote> rmiServerImplStubClass;
        

        static {
            Class<? extends Remote> serverStubClass = null;
            try {
                serverStubClass = Class.forName(
                        rmiServerImplStubClassName).asSubclass(Remote.class);
            } catch (ClassNotFoundException e) {
                // should never reach here
                throw (InternalError) new InternalError(e.getMessage()).initCause(e);
            }
            rmiServerImplStubClass = serverStubClass;
        }

        private void checkSslConfig() throws IOException {
            // Get the reference to the RMI Registry and lookup RMIServer stub
            //
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(registryHostName,
                        registryPort, sslRMIClientSocketFactory);
                try {
                    stub = (RMIServer) registry.lookup("jmxrmi");
                } catch (NotBoundException nbe) {
                    throw (IOException) new IOException(nbe.getMessage()).initCause(nbe);
                }
                sslRegistry = true;
            } catch (IOException e) {
                registry =
                        LocateRegistry.getRegistry(registryHostName, registryPort);
                try {
                    stub = (RMIServer) registry.lookup("jmxrmi");
                } catch (NotBoundException nbe) {
                    throw (IOException) new IOException(nbe.getMessage()).initCause(nbe);
                }
                sslRegistry = false;
            }
            // Perform the checks for secure stub
            //
            try {
                checkStub(stub, rmiServerImplStubClass);
                sslStub = true;
            } catch (SecurityException e) {
                sslStub = false;
            }
        }

        /**
         * Returns true if the underlying RMI registry is SSL-protected.
         *
         * @exception UnsupportedOperationException If this {@code ProxyClient}
         * does not denote a JMX connector for a JMX VM agent.
         */
        public boolean isSslRmiRegistry() {
            // Check for VM connector
            //
            if (!isVmConnector()) {
                throw new UnsupportedOperationException(
                        "ProxyClient.isSslRmiRegistry() is only supported if this " +
                        "ProxyClient is a JMX connector for a JMX VM agent");
            }
            return sslRegistry;
        }

        /**
         * Returns true if the retrieved RMI stub is SSL-protected.
         *
         * @exception UnsupportedOperationException If this {@code ProxyClient}
         * does not denote a JMX connector for a JMX VM agent.
         */
        public boolean isSslRmiStub() {
            // Check for VM connector
            //
            if (!isVmConnector()) {
                throw new UnsupportedOperationException(
                        "ProxyClient.isSslRmiStub() is only supported if this " +
                        "ProxyClient is a JMX connector for a JMX VM agent");
            }
            return sslStub;
        }

        /**
         * Returns true if this {@code ProxyClient} denotes
         * a JMX connector for a JMX VM agent.
         */
        public boolean isVmConnector() {
            return vmConnector;
        }

        private void setConnectionState(ConnectionState state) {
            ConnectionState oldState = connectionState;
            connectionState = state;
            model.propertyChangeSupport.firePropertyChange(
                    JmxModel.CONNECTION_STATE_PROPERTY, oldState, state);
        }

        public ConnectionState getConnectionState() {
            return connectionState;
        }

        void flush() {
            if (cachedConn != null) {
                cachedConn.flush();
            }
        }

        void connect() {
            setConnectionState(ConnectionState.CONNECTING);
            try {
                tryConnect();
                setConnectionState(ConnectionState.CONNECTED);
            } catch (SecurityException e) {
                e.printStackTrace();
                setConnectionState(ConnectionState.DISCONNECTED);
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                setConnectionState(ConnectionState.DISCONNECTED);
                // Workaround for GlassFish's LoginException class not found
                if (e.toString().contains("com.sun.enterprise.security.LoginException")) {
                    throw new SecurityException("Authentication failed! Invalid username or password");
                }
            }
        }

        private void tryConnect() throws IOException {
            if (jmxUrl == null && "localhost".equals(hostName) && port == 0) {
                jmxc = null;
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                conn = mbs;
                cachedConn = Snapshot.newSnapshot(mbs);
            } else {
                if (lvm != null) {
                    if (!lvm.isManageable()) {
                        lvm.startManagementAgent();
                        if (!lvm.isManageable()) {
                            // FIXME: what to throw
                            throw new IOException(lvm + " not manageable");
                        }
                    }
                    if (jmxUrl == null) {
                        jmxUrl = new JMXServiceURL(lvm.connectorAddress());
                    }
                }
                // Need to pass in credentials ?
                if (userName == null && password == null) {
                    if (isVmConnector()) {
                        // Check for SSL config on reconnection only
                        if (stub == null) {
                            checkSslConfig();
                        }
                        jmxc = new RMIConnector(stub, null);
                        jmxc.addConnectionNotificationListener(this, null, null);
                        jmxc.connect();
                    } else {
                        jmxc = JMXConnectorFactory.newJMXConnector(jmxUrl, null);
                        jmxc.addConnectionNotificationListener(this, null, null);
                        jmxc.connect();
                    }
                } else {
                    Map<String, String[]> env = new HashMap<String, String[]>();
                    env.put(JMXConnector.CREDENTIALS,
                            new String[]{userName, password});
                    if (isVmConnector()) {
                        // Check for SSL config on reconnection only
                        if (stub == null) {
                            checkSslConfig();
                        }
                        jmxc = new RMIConnector(stub, null);
                        jmxc.addConnectionNotificationListener(this, null, null);
                        jmxc.connect(env);
                    } else {
                        jmxc = JMXConnectorFactory.newJMXConnector(jmxUrl, env);
                        jmxc.addConnectionNotificationListener(this, null, null);
                        jmxc.connect(env);
                    }
                }
                MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
                conn = Checker.newChecker(this, mbsc);
                cachedConn = Snapshot.newSnapshot(conn);
            }
            isDead = false;
        }

        public static String getConnectionName(LocalVirtualMachine lvm) {
            return Integer.toString(lvm.vmid());
        }

        public static String getConnectionName(String url, String userName) {
            if (userName != null && userName.length() > 0) {
                return userName + "@" + url;
            } else {
                return url;
            }
        }

        public static String getConnectionName(String hostName, int port, String userName) {
            String name = hostName + ":" + port;
            if (userName != null && userName.length() > 0) {
                return userName + "@" + name;
            } else {
                return name;
            }
        }

        public String connectionName() {
            return connectionName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public MBeanServerConnection getMBeanServerConnection() {
            return conn;
        }

        public CachedMBeanServerConnection getCachedMBeanServerConnection() {
            return cachedConn;
        }

        public JMXServiceURL getUrl() {
            return jmxUrl;
        }

        public String getHostName() {
            return hostName;
        }

        public int getPort() {
            return port;
        }

        public int getVmid() {
            return (lvm != null) ? lvm.vmid() : 0;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }

        public void disconnect() {
            // Reset remote stub
            stub = null;
            // Close MBeanServer connection
            if (jmxc != null) {
                try {
                    jmxc.close();
                } catch (IOException e) {
                    // Ignore...
                } finally {
                    try {
                        jmxc.removeConnectionNotificationListener(this);
                    } catch (Exception e) {
                        // Ignore...
                    }
                }
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
                if (JMXConnectionNotification.FAILED.equals(n.getType())) {
                    markAsDead();
                }
            }
        }
    }

    /**
     * The PropertyChangeListener is handled via a WeakReference
     * so as not to pin down the listener.
     */
    class WeakPCL extends WeakReference<PropertyChangeListener>
            implements PropertyChangeListener {

        WeakPCL(PropertyChangeListener referent) {
            super(referent);
        }

        public void propertyChange(PropertyChangeEvent pce) {
            PropertyChangeListener pcl = get();

            if (pcl == null) {
                // The referent listener was GC'ed, we're no longer
                // interested in PropertyChanges, remove the listener.
                dispose();
            } else {
                pcl.propertyChange(pce);
            }
        }

        private void dispose() {
            JmxModel.this.removePropertyChangeListener(this);
        }
    }

    static class Snapshot {

        private Snapshot() {
        }

        public static CachedMBeanServerConnection newSnapshot(MBeanServerConnection mbsc) {
            final InvocationHandler ih = new SnapshotInvocationHandler(mbsc);
            return (CachedMBeanServerConnection) Proxy.newProxyInstance(
                    Snapshot.class.getClassLoader(),
                    new Class[]{CachedMBeanServerConnection.class},
                    ih);
        }
    }

    static class SnapshotInvocationHandler implements InvocationHandler {

        private final MBeanServerConnection conn;
        private Map<ObjectName, NameValueMap> cachedValues = newMap();
        private Map<ObjectName, Set<String>> cachedNames = newMap();

        @SuppressWarnings("serial")
        private static final class NameValueMap
                extends HashMap<String, Object> {
        }

        SnapshotInvocationHandler(MBeanServerConnection conn) {
            this.conn = conn;
        }

        synchronized void flush() {
            cachedValues = newMap();
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            final String methodName = method.getName();
            if (methodName.equals("getAttribute")) {
                return getAttribute((ObjectName) args[0], (String) args[1]);
            } else if (methodName.equals("getAttributes")) {
                return getAttributes((ObjectName) args[0], (String[]) args[1]);
            } else if (methodName.equals("flush")) {
                flush();
                return null;
            } else {
                try {
                    return method.invoke(conn, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        private Object getAttribute(ObjectName objName, String attrName)
                throws MBeanException, InstanceNotFoundException,
                AttributeNotFoundException, ReflectionException, IOException {
            final NameValueMap values = getCachedAttributes(
                    objName, Collections.singleton(attrName));
            Object value = values.get(attrName);
            if (value != null || values.containsKey(attrName)) {
                return value;
            }
            // Not in cache, presumably because it was omitted from the
            // getAttributes result because of an exception.  Following
            // call will probably provoke the same exception.
            return conn.getAttribute(objName, attrName);
        }

        private AttributeList getAttributes(
                ObjectName objName, String[] attrNames) throws
                InstanceNotFoundException, ReflectionException, IOException {
            final NameValueMap values = getCachedAttributes(
                    objName,
                    new TreeSet<String>(Arrays.asList(attrNames)));
            final AttributeList list = new AttributeList();
            for (String attrName : attrNames) {
                final Object value = values.get(attrName);
                if (value != null || values.containsKey(attrName)) {
                    list.add(new Attribute(attrName, value));
                }
            }
            return list;
        }

        private synchronized NameValueMap getCachedAttributes(
                ObjectName objName, Set<String> attrNames) throws
                InstanceNotFoundException, ReflectionException, IOException {
            NameValueMap values = cachedValues.get(objName);
            if (values != null && values.keySet().containsAll(attrNames)) {
                return values;
            }
            attrNames = new TreeSet<String>(attrNames);
            Set<String> oldNames = cachedNames.get(objName);
            if (oldNames != null) {
                attrNames.addAll(oldNames);
            }
            values = new NameValueMap();
            final AttributeList attrs = conn.getAttributes(
                    objName,
                    attrNames.toArray(new String[attrNames.size()]));
            for (Attribute attr : attrs.asList()) {
                values.put(attr.getName(), attr.getValue());
            }
            cachedValues.put(objName, values);
            cachedNames.put(objName, attrNames);
            return values;
        }

        // See http://www.artima.com/weblogs/viewpost.jsp?thread=79394
        private static <K, V> Map<K, V> newMap() {
            return new HashMap<K, V>();
        }
    }

    static class Checker {

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

    static class CheckerInvocationHandler implements InvocationHandler {

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

                    LOGGER.log(Level.FINE, createTracedMessage("MBeanServerConnection call " +
                            "performed on Event Dispatch Thread!", thrwbl));
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

    static class LocalVirtualMachine {

        private int vmid;
        private boolean isAttachSupported;
        private String address;

        public LocalVirtualMachine(int vmid, boolean canAttach, String connectorAddress) {
            this.vmid = vmid;
            this.address = connectorAddress;
            this.isAttachSupported = canAttach;
        }

        public int vmid() {
            return vmid;
        }

        public boolean isManageable() {
            return (address != null);
        }

        public boolean isAttachable() {
            return isAttachSupported;
        }

        public void startManagementAgent() throws IOException {
            if (address != null) {
                // already started
                return;
            }

            if (!isAttachable()) {
                throw new IOException("This virtual machine \"" + vmid +
                        "\" does not support dynamic attach.");
            }

            loadManagementAgent();
            // fails to load or start the management agent
            if (address == null) {
                // should never reach here
                throw new IOException("Fails to find connector address");
            }
        }

        public String connectorAddress() {
            // return null if not available or no JMX agent
            return address;
        }
        private static final String LOCAL_CONNECTOR_ADDRESS_PROP =
                "com.sun.management.jmxremote.localConnectorAddress";

        // load the management agent into the target VM
        private void loadManagementAgent() throws IOException {
            VirtualMachine vm = null;
            String name = String.valueOf(vmid);
            try {
                vm = VirtualMachine.attach(name);
            } catch (AttachNotSupportedException x) {
                IOException ioe = new IOException(x.getMessage());
                ioe.initCause(x);
                throw ioe;
            }

            String home = vm.getSystemProperties().getProperty("java.home");

            // Normally in ${java.home}/jre/lib/management-agent.jar but might
            // be in ${java.home}/lib in build environments.

            String agent = home + File.separator + "jre" + File.separator +
                    "lib" + File.separator + "management-agent.jar";
            File f = new File(agent);
            if (!f.exists()) {
                agent = home + File.separator + "lib" + File.separator +
                        "management-agent.jar";
                f = new File(agent);
                if (!f.exists()) {
                    throw new IOException("Management agent not found");
                }
            }

            agent = f.getCanonicalPath();
            try {
                vm.loadAgent(agent, "com.sun.management.jmxremote");
            } catch (AgentLoadException x) {
                IOException ioe = new IOException(x.getMessage());
                ioe.initCause(x);
                throw ioe;
            } catch (AgentInitializationException x) {
                IOException ioe = new IOException(x.getMessage());
                ioe.initCause(x);
                throw ioe;
            }

            // get the connector address
            Properties agentProps = vm.getAgentProperties();
            address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);

            vm.detach();
        }
    }
}
