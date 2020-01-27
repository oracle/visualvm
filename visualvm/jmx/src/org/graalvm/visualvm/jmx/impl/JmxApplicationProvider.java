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

package org.graalvm.visualvm.jmx.impl;

import org.graalvm.visualvm.jmx.CredentialsProvider;
import org.graalvm.visualvm.jmx.EnvironmentProvider;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.host.HostsSupport;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.jmx.JmxApplicationException;
import org.graalvm.visualvm.jmx.JmxApplicationsSupport;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.remote.JMXServiceURL;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.graalvm.visualvm.core.VisualVM;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 * A provider for Applications added as JMX connections.
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
public class JmxApplicationProvider {
//    private static final Logger LOGGER = Logger.getLogger(JmxApplicationProvider.class.getName());
    
    // --- Snapshot format history ---------------------------------------------
    //
    // 1.0:     initial snapshot version
    // 1.1:     added PROPERTY_ENV_PROVIDER_ID
    // 1.2:     added PROPERTY_RETRY_WITHOUT_SSL
    //
    // -------------------------------------------------------------------------
    
    private final static Logger LOGGER = Logger.getLogger(ProxyClient.class.getName());

    private static final String SNAPSHOT_VERSION = "snapshot_version";  // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";         // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "2";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION =
                                                CURRENT_SNAPSHOT_VERSION_MAJOR +
                                                SNAPSHOT_VERSION_DIVIDER +
                                                CURRENT_SNAPSHOT_VERSION_MINOR;
    
    public static final String PROPERTY_RETRY_WITHOUT_SSL = "prop_retry_without_ssl"; // NOI18N
    private static final String PROPERTY_CONNECTION_STRING = "prop_conn_string";    // NOI18N
    private static final String PROPERTY_HOSTNAME = "prop_conn_hostname";   // NOI18N
    private static final String PROPERTY_ENV_PROVIDER_ID = "prop_env_provider_id"; // NOI18N
    
    
    private static final String PROPERTIES_FILE = "jmxapplication" + Storage.DEFAULT_PROPERTIES_EXT;  // NOI18N
    static final String JMX_SUFFIX = ".jmx";  // NOI18N
    
    private static final String DNSA_KEY = "JMXApplicationProvider_NotifyUnresolved"; // NOI18N
    
    
    private volatile boolean trackingNewHosts;
    private Map<String, Set<Storage>> persistedApplications =
            new HashMap<String, Set<Storage>>();
    

    private static boolean isLocalHost(String hostname) throws IOException {
        InetAddress remoteAddr = InetAddress.getByName(hostname);
        // Retrieve all the network interfaces on this host.
        Enumeration<NetworkInterface> nis =
                NetworkInterface.getNetworkInterfaces();
        // Walk through the network interfaces to see
        // if any of them matches the client's address.
        // If true, then the client's address is local.
        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress localAddr = addrs.nextElement();
                if (localAddr.equals(remoteAddr)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Resolves existing host based on hostname, JMXServiceURL
    // or by using the JMXServiceURL to connect to the agent
    // and retrieve the hostname information.
    private Host getHost(String hostname, JMXServiceURL url)
            throws IOException {
        // Try to compute the Host instance from hostname
        if (hostname != null) {
            if (hostname.isEmpty() || isLocalHost(hostname)) {
                return Host.LOCALHOST;
            } else {
                return HostsSupport.getInstance().getOrCreateHost(hostname, false);
            }
        }

        // TODO: Connect to the agent and try to get the hostname.
        //       app = JmxApplication(Host.UNKNOWN_HOST, url, storage);
        //       JmxModelFactory.getJmxModelFor(app);

        // WARNING: If a hostname could not be found the JMX application
        //          is added under the <Unknown Host> tree node.
        return Host.UNKNOWN_HOST;
    }

    public static String getConnectionString(JmxApplication application) {
        return application.getStorage().getCustomProperty(PROPERTY_CONNECTION_STRING);
    }
    
    public static String getSuggestedName(String displayName, String connectionString,
                                          String username) {
        // User-provided displayName always first
        if (displayName != null) return displayName;
        
        // Generated name 'connectionString' or 'user@connectionString'
        if (username == null) username = ""; // NOI18N
        return (username.isEmpty() ? "" : username + "@") + connectionString; // NOI18N
    }

    public JmxApplication createJmxApplication(String connectionString, String displayName,
                                               String suggestedName, EnvironmentProvider provider,
                                               boolean persistent, boolean allowsInsecure)
                                               throws JmxApplicationException {
        // Initial check if the provided connectionName can be used for resolving the host/application
        final String normalizedConnectionName = normalizeConnectionName(connectionString);
        final JMXServiceURL serviceURL;
        try {
            serviceURL = getServiceURL(normalizedConnectionName);
        } catch (MalformedURLException ex) {
            throw new JmxApplicationException(NbBundle.getMessage(JmxApplicationProvider.class,
                                "MSG_Invalid_JMX_connection", normalizedConnectionName),ex); // NOI18N
        }

        String hostName = getHostName(serviceURL);
        hostName = hostName == null ? "" : hostName; // NOI18N

        Storage storage = null;

        if (persistent) {
            File storageDirectory = Utils.getUniqueFile(JmxApplicationsSupport.getStorageDirectory(),
                    "" + System.currentTimeMillis(), JMX_SUFFIX);    // NOI18N
            Utils.prepareDirectory(storageDirectory);
            storage = new Storage(storageDirectory, PROPERTIES_FILE);
            storage.setCustomProperty(SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);
        }

        try {
            JmxApplication app = addJmxApplication(true, serviceURL, normalizedConnectionName,
                                 displayName, suggestedName, hostName, provider,
                                 storage, Boolean.toString(allowsInsecure), true);
            return app;
        } catch (JMXException e) {
            if (storage != null) {
                File appStorage = storage.getDirectory();
                if (appStorage.isDirectory()) Utils.delete(appStorage, true);
            }
            throw new JmxApplicationException(e.getMessage(), e.getCause());
        }
    }
    
    private JmxApplication addJmxApplication(boolean newApp, JMXServiceURL serviceURL,
            String connectionName, String displayName, String suggestedName, String hostName,
            EnvironmentProvider provider, Storage storage, String allowsInsecure, boolean lazy) throws JMXException {
        
        if (lazy) return addLazyJmxApplication(newApp, serviceURL, connectionName, displayName, suggestedName,
                                               hostName, provider, storage, allowsInsecure, true);
        else throw new RuntimeException("Only lazy JMX connections currently implemented!"); // NOI18N
    }

    private JmxApplication addLazyJmxApplication(boolean newApp, JMXServiceURL serviceURL, String connectionName,
            String displayName, String suggestedName, String hostName, EnvironmentProvider provider,
            Storage storage, String allowsInsecure, boolean scheduleHeartbeat) throws JMXException {

        // Resolve JMXServiceURL, finish if not resolved
        if (serviceURL == null) {
            try {
                serviceURL = getServiceURL(connectionName);
            } catch (MalformedURLException ex) {
                throw new JMXException(true, NbBundle.getMessage(JmxApplicationProvider.class,
                                    "MSG_Invalid_JMX_connection", connectionName), ex); // NOI18N
            }
        }

        // Resolve existing Host or create new Host, finish if Host cannot be resolved
        Set<Host> hosts = DataSourceRepository.sharedInstance().getDataSources(Host.class);
        Host host = null;
        try {
            host = getHost(hostName, serviceURL);
        } catch (Exception e) {
            cleanupCreatedHost(hosts, host);
            throw new JMXException(false, NbBundle.getMessage(JmxApplicationProvider.class,
                                    "MSG_Cannot_resolve_host", hostName), e); // NOI18N
        }

        // Update persistent storage and EnvironmentProvider
        if (storage != null) {
            if (newApp) {
                storage.setCustomProperty(PROPERTY_HOSTNAME, host.getHostName());
                if (provider != null) {
                    storage.setCustomProperty(PROPERTY_ENV_PROVIDER_ID, provider.getId());
                    provider.saveEnvironment(storage);
                }
            } else {
                if (provider != null) provider.loadEnvironment(storage);
            }
        }
        
        // Create the JmxApplication
        final JmxApplication application = new JmxApplication(host, serviceURL, provider, storage);

        // Update display name and new EnvironmentProvider for non-persistent storage
        if (newApp) {
            Storage s = application.getStorage();
            String[] keys = new String[] {
                PROPERTY_CONNECTION_STRING,
                displayName != null ?
                    DataSourceDescriptor.PROPERTY_NAME :
                    ApplicationType.PROPERTY_SUGGESTED_NAME
            };

            String[] values = new String[] {
                connectionName,
                displayName != null ?
                    displayName :
                    suggestedName
            };

            s.setCustomProperties(keys, values);
            if (provider != null) provider.saveEnvironment(s);
        }
        
        // Check if the given JmxApplication has been already added to the application tree
        final Set<JmxApplication> jmxapps = host.getRepository().getDataSources(JmxApplication.class);
        if (jmxapps.contains(application)) {
            JmxApplication tempapp = null;
            for (JmxApplication jmxapp : jmxapps) {
                if (jmxapp.equals(application)) {
                    tempapp = jmxapp;
                    break;
                }
            }
            cleanupCreatedHost(hosts, host);
            throw new JMXException(true, NbBundle.getMessage(JmxApplicationProvider.class,
                                    "MSG_connection_already_exists", new Object[] { // NOI18N
                                    application.getId(), DataSourceDescriptorFactory.
                                    getDescriptor(tempapp).getName() }));
        }
        
        // Setup whether the SSL connection is required or not
        application.getStorage().setCustomProperty(PROPERTY_RETRY_WITHOUT_SSL, allowsInsecure);
        
//        // NOTE: 'lazyCheck' currently always true!
//
//        // Connect to the JMX agent
//        JmxModel model = lazyCheck ? null : JmxModelFactory.getJmxModelFor(application);
//        
//        if (model == null || model.getConnectionState() != ConnectionState.CONNECTED) {
//            application.setStateImpl(Stateful.STATE_UNAVAILABLE);
////            cleanupCreatedHost(hosts, host);
////            throw new JMXException(false, NbBundle.getMessage(JmxApplicationProvider.class,
////                                    "MSG_Cannot_connect_using", new Object[] { // NOI18N
////                                    displayName != null ? displayName : suggestedName,
////                                    connectionName }));
//        }
//
//        // Update application state according to the connection state
//        if (model != null) model.addPropertyChangeListener(new PropertyChangeListener() {
//            public void propertyChange(PropertyChangeEvent evt) {
//                if (evt.getNewValue() == ConnectionState.CONNECTED) {
//                    application.setStateImpl(Stateful.STATE_AVAILABLE);
//                } else {
//                    application.setStateImpl(Stateful.STATE_UNAVAILABLE);
//                    synchronized (unavailableApps) { unavailableApps.add(application); }
//                    scheduleHeartbeat();
//                    // TODO: remove listener from model once not needed!
//                }
//            }
//        });
//
//        // precompute JVM
//        if (model != null && model.getConnectionState() == ConnectionState.CONNECTED)
//            application.jvm = JvmFactory.getJVMFor(application);
//
//        // If everything succeeded, add datasource to application tree
        host.getRepository().addDataSource(application);
        
//        if (model == null || model.getConnectionState() != ConnectionState.CONNECTED) {
        synchronized (unavailableApps) { unavailableApps.add(application); }
        if (scheduleHeartbeat) scheduleHeartbeat();
//        }

        return application;
    }
    
    
    private static final int HEARTBEAT_DELAY = 100;
    private static final int HEARTBEAT_POLL_DELAY = 5000;
    private static final int HEARTBEAT_MAX_THREADS = 10;
    private final Set<JmxApplication> unavailableApps = new HashSet();
    
    private volatile boolean heartbeatRunning;
    private volatile boolean anotherHeartbeatPending;
    
    private void scheduleHeartbeat() {
        scheduleHeartbeatImpl(true);
    }
    
    private void scheduleHeartbeatImpl(boolean immediately) {
        if (heartbeatRunning) {
            anotherHeartbeatPending = true;
            return;
        } else {
            heartbeatRunning = true;
        }
        
        Set<JmxApplication> apps = new HashSet();
        synchronized (unavailableApps) {
            apps.addAll(unavailableApps);
            unavailableApps.clear();
        }
        
        Iterator<JmxApplication> appsI = apps.iterator();
        while (appsI.hasNext()) if (appsI.next().isRemoved()) appsI.remove();
        if (apps.isEmpty()) {
            heartbeatRunning = false;
            if (anotherHeartbeatPending) { // just a safe fallback, likely not needed at all
                anotherHeartbeatPending = false;
                scheduleHeartbeatImpl(false);
            } else {
                return;
            }
        }
        
        final AtomicInteger counter = new AtomicInteger(apps.size());
        RequestProcessor processor = new RequestProcessor("JMX Heartbeat Processor", Math.min(counter.intValue(), HEARTBEAT_MAX_THREADS)); // NOI18N
//        System.err.println(">>> Heartbeat for " + counter + " targets at " + java.time.LocalTime.now());
        for (final JmxApplication app : apps) {
            processor.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean connected = false;
                        try {
                            ProxyClient client = new ProxyClient(app);
                            client.connect();
                            if (client.getConnectionState() == ConnectionState.CONNECTED) {
                                app.setClient(client);
                                connected = true;
                            }
                        } catch (IOException ex) {
                            LOGGER.log(Level.FINE, "ProxyClient.connect", ex);
                        }
                        if (!connected) {
                            synchronized (unavailableApps) { unavailableApps.add(app); }
                        } else {
                            app.setStateImpl(Stateful.STATE_AVAILABLE);

                            app.jmxModel = JmxModelFactory.getJmxModelFor(app);
                            app.jvm = JvmFactory.getJVMFor(app);

                            app.jmxModel.addPropertyChangeListener(new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent evt) {
                                    if (evt.getNewValue() == ConnectionState.CONNECTED) {
                                        app.setStateImpl(Stateful.STATE_AVAILABLE);
                                    } else {
                                        app.setStateImpl(Stateful.STATE_UNAVAILABLE);
                                        if (!app.isRemoved()) {
                                            synchronized (unavailableApps) { unavailableApps.add(app); }
                                            scheduleHeartbeatImpl(true);
                                        }
                                        // TODO: remove listener from model once not needed!
                                    }
                                }
                            });
                        }
                    } finally {
                        if (counter.decrementAndGet() == 0) {
                            boolean pendingApps;
                            
                            synchronized (unavailableApps) {
                                Iterator<JmxApplication> appsI = unavailableApps.iterator();
                                while (appsI.hasNext()) if (appsI.next().isRemoved()) appsI.remove();
                                pendingApps = !unavailableApps.isEmpty();
                                heartbeatRunning = false;
                            }

                            if (anotherHeartbeatPending || pendingApps) {
                                anotherHeartbeatPending = false;
                                scheduleHeartbeatImpl(false);
                            }
                        }
                    }
                }
            }, immediately ? HEARTBEAT_DELAY : HEARTBEAT_POLL_DELAY);
        }
    }
    
    

    private void cleanupCreatedHost(Set<Host> hosts, Host host) {
        // NOTE: this is not absolutely failsafe, if resolving the JMX application
        // took a long time and its host has been added by the user/plugin, it may
        // be removed by this call. Hopefully just a hypothetical case...
        if (host != null && !Host.LOCALHOST.equals(host) && !hosts.contains(host))
            host.getOwner().getRepository().removeDataSource(host);
    }
    
    private String normalizeConnectionName(String connectionName) {
        if (connectionName.startsWith("service:jmx:")) return connectionName;   // NOI18N
        return "service:jmx:rmi:///jndi/rmi://" + connectionName + "/jmxrmi";   // NOI18N  hostname:port
    }
    
    private String getHostName(JMXServiceURL serviceURL) {
        // Try to compute the hostname instance
        // from the host in the JMXServiceURL.
        String hostname = serviceURL.getHost();
        if (hostname == null || hostname.isEmpty()) {
            hostname = null;
            // Try to compute the Host instance from the JNDI/RMI
            // Registry Service urlPath in the JMXServiceURL.
            if ("rmi".equals(serviceURL.getProtocol()) &&   // NOI18N
                    serviceURL.getURLPath().startsWith("/jndi/rmi://")) {   // NOI18N
                String urlPath =
                        serviceURL.getURLPath().substring("/jndi/rmi://".length()); // NOI18N
                if ('/' == urlPath.charAt(0)) { // NOI18N
                    hostname = "localhost"; // NOI18N
                } else if ('[' == urlPath.charAt(0)) { // IPv6 address  // NOI18N
                    int closingSquareBracketIndex = urlPath.indexOf("]"); // NOI18N
                    if (closingSquareBracketIndex == -1) {
                        hostname = null;
                    } else {
                        hostname = urlPath.substring(0, closingSquareBracketIndex + 1);
                    }
                } else {
                    int colonIndex = urlPath.indexOf(":"); // NOI18N
                    int slashIndex = urlPath.indexOf("/"); // NOI18N
                    int min = Math.min(colonIndex, slashIndex); // NOTE: can be -1!!!
                    if (min == -1) {
                        min = 0;
                    }
                    hostname = urlPath.substring(0, min);
                    if (hostname.isEmpty()) {
                        hostname = "localhost"; // NOI18N
                    }
                }
            }
        }
        return hostname;
    }

    private JMXServiceURL getServiceURL(String connectionString) throws MalformedURLException {
        return new JMXServiceURL(connectionString);
    }

    private void initPersistedApplications() {
        if (!JmxApplicationsSupport.storageDirectoryExists()) return;
        
        File[] files = JmxApplicationsSupport.getStorageDirectory().listFiles(
                new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(JMX_SUFFIX);
                    }
                });
        
        final int[] persistedAppsCount = new int[1];
        for (File file : files) {
            if (file.isDirectory()) {
                persistedAppsCount[0]++;
                Storage storage = new Storage(file, PROPERTIES_FILE);
                Set<Storage> storageSet = persistedApplications.get(storage.getCustomProperty(PROPERTY_HOSTNAME));
                if (storageSet == null) {
                    storageSet = new HashSet<Storage>();
                    persistedApplications.put(storage.getCustomProperty(PROPERTY_HOSTNAME), storageSet);
                }
                storageSet.add(storage);
            }
        }
        
        DataChangeListener<Host> dataChangeListener = new DataChangeListener<Host>() {

            public synchronized void dataChanged(DataChangeEvent<Host> event) {
                final Set<String> failedAppsN = Collections.synchronizedSet(new HashSet());
                final Set<Storage> failedAppsS = Collections.synchronizedSet(new HashSet());
                Set<Host> hosts = event.getAdded();
                for (Host host : hosts) {
                    String hostName = host.getHostName();
                    Set<Storage> storageSet = persistedApplications.get(hostName);
                    int storageSetSize = storageSet == null ? 0 : storageSet.size();
                    if (storageSetSize > 0) {
                        persistedApplications.remove(hostName);
                        
                        String[] keys = new String[] {
                            PROPERTY_CONNECTION_STRING,
                            PROPERTY_HOSTNAME,
                            DataSourceDescriptor.PROPERTY_NAME,
                            ApplicationType.PROPERTY_SUGGESTED_NAME,
                            PROPERTY_ENV_PROVIDER_ID,
                            PROPERTY_RETRY_WITHOUT_SSL
                        };
                        
                        final AtomicInteger counter = new AtomicInteger(storageSetSize);
                        RequestProcessor processor = new RequestProcessor("JMX Persistence Processor", Math.min(counter.intValue(), HEARTBEAT_MAX_THREADS)); // NOI18N

                        for (final Storage storage : storageSet) {
                            final String[] values = storage.getCustomProperties(keys);
                            processor.post(new Runnable() {
                                public void run() {
                                    try {
                                        String epid = values[4];
                                        if (epid == null) {
                                            // Check for ver 1.0 which didn't support PROPERTY_ENVIRONMENT_PROVIDER
                                            String sv = storage.getCustomProperty(SNAPSHOT_VERSION);
                                            if ("1.0".equals(sv)) epid = CredentialsProvider.class.getName(); // NOI18N
                                        }
                                        EnvironmentProvider ep = epid == null ? null :
                                                                 JmxConnectionSupportImpl.
                                                                 getProvider(epid);
                                        addLazyJmxApplication(false, null, values[0], values[2], values[3],
                                                              values[1], ep, storage, values[5], false);
                                    } catch (final JMXException e) {
                                        if (e.isConfig()) {
                                            DialogDisplayer.getDefault().notifyLater(
                                                new NotifyDescriptor.Message(e.
                                                getMessage(), NotifyDescriptor.
                                                ERROR_MESSAGE));
                                        } else {
                                            String name = values[2];
                                            if (name == null || name.trim().isEmpty()) name = values[3];
                                            failedAppsN.add(name);
                                            failedAppsS.add(storage);
                                        }
                                    } finally {
                                        if (counter.decrementAndGet() == 0) scheduleHeartbeat();
                                    }
                                    synchronized (persistedAppsCount) {
                                        persistedAppsCount[0]--;
                                        if (persistedAppsCount[0] == 0 && !failedAppsN.isEmpty())
                                            notifyUnresolvedApplications(failedAppsN, failedAppsS);
                                    }
                                }
                            });
                        }
                    }
                }
                
                if (trackingNewHosts && persistedApplications.isEmpty()) {
                    trackingNewHosts = false;
                    DataSourceRepository.sharedInstance().removeDataChangeListener(this);
                }
            }
            
        };
        
        if (!persistedApplications.isEmpty()) {
            trackingNewHosts = true;
            DataSourceRepository.sharedInstance().addDataChangeListener(dataChangeListener, Host.class);
        }
    }
    
    private static void notifyUnresolvedApplications(final Set<String> failedHostsN, final Set<Storage> failedHostsS) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                String s = GlobalPreferences.sharedInstance().getDoNotShowAgain(DNSA_KEY);
                Boolean b = s == null ? null : Boolean.parseBoolean(s);
                
                if (b == null) {
                    JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
                    messagePanel.add(new JLabel(NbBundle.getMessage(JmxApplicationProvider.class, "MSG_Unresolved_JMX")), BorderLayout.NORTH); // NOI18N
                    JList list = new JList(failedHostsN.toArray());
                    list.setVisibleRowCount(4);
                    messagePanel.add(new JScrollPane(list), BorderLayout.CENTER);
                    JCheckBox dnsa = new JCheckBox();
                    Mnemonics.setLocalizedText(dnsa, NbBundle.getMessage(JmxApplicationProvider.class, "LBL_RememberAction")); // NOI18N
                    dnsa.setToolTipText(NbBundle.getMessage(JmxApplicationProvider.class, "TTP_RememberAction")); // NOI18N
                    JPanel p = new JPanel(new BorderLayout());
                    p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 20));
                    p.add(dnsa, BorderLayout.WEST);
                    messagePanel.add(p, BorderLayout.SOUTH);
                    NotifyDescriptor dd = new NotifyDescriptor(
                            messagePanel, NbBundle.getMessage(JmxApplicationProvider.class, "Title_Unresolved_JMX"), // NOI18N
                            NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.ERROR_MESSAGE,
                            null, NotifyDescriptor.YES_OPTION);
                    Object ret = DialogDisplayer.getDefault().notify(dd);
                    
                    if (ret == NotifyDescriptor.NO_OPTION) b = Boolean.FALSE;
                    else if (ret == NotifyDescriptor.YES_OPTION) b = Boolean.TRUE;
                    
                    if (dnsa.isSelected() && b != null) GlobalPreferences.sharedInstance().setDoNotShowAgain(DNSA_KEY, b.toString());
                }
                
                if (Boolean.FALSE.equals(b))
                    for (Storage storage : failedHostsS) {
                        File appStorage = storage.getDirectory();
                        if (appStorage.isDirectory()) Utils.delete(appStorage, true);
                    }

                failedHostsS.clear();
            }
        }, 1000);
    }

    public void initialize() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                VisualVM.getInstance().runTask(new Runnable() {
                    public void run() {
                        initPersistedApplications();
                    }
                });
            }
        });
    }
    
    
    private static class JMXException  extends Exception {
        private final boolean isConfig;
        public JMXException(boolean config, String message) { super(message); isConfig = config; }
        public JMXException(boolean config, String message, Throwable cause) { super(message,cause); isConfig = config; }
        public boolean isConfig() { return isConfig; }
    }
}
