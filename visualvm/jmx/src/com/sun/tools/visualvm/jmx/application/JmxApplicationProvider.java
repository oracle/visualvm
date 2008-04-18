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

package com.sun.tools.visualvm.jmx.application;

import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.host.HostsSupport;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.management.remote.JMXServiceURL;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 * A provider for Applications added as JMX connections.
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class JmxApplicationProvider {
    private static final Logger LOGGER = Logger.getLogger(JmxApplicationProvider.class.getName());
    
    private static final String SNAPSHOT_VERSION = "snapshot_version";  // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION =
            CURRENT_SNAPSHOT_VERSION_MAJOR +
            SNAPSHOT_VERSION_DIVIDER +
            CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROPERTY_CONNECTION_STRING = "prop_conn_string";    // NOI18N
    private static final String PROPERTY_HOSTNAME = "prop_conn_hostname";   // NOI18N
    private static final String PROPERTY_USERNAME = "prop_username";    // NOI18N
    private static final String PROPERTY_PASSWORD = "prop_password";    // NOI18N

    private static JmxApplicationProvider sharedInstance;
    
    
    private boolean trackingNewHosts;
    private Map<String, Set<Storage>> persistedApplications =
            new HashMap<String, Set<Storage>>();
    
    
    public synchronized static JmxApplicationProvider sharedInstance() {
        if (sharedInstance == null) sharedInstance = new JmxApplicationProvider();
        return sharedInstance;
    }

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
                InetAddress addr = InetAddress.getByName(hostname);
                Host host = HostsSupport.getInstance().getHostByAddress(addr);
                if (host == null) host = HostsSupport.getInstance().createHost(hostname);
                return host;
            }
        }

        // TODO: Connect to the agent and try to get the hostname.
        //       app = JmxApplication(Host.UNKNOWN_HOST, url, storage);
        //       JmxModelFactory.getJmxModelFor(app);

        // WARNING: If a hostname could not be found the JMX application
        //          is added under the <Unknown Host> tree node.
        return Host.UNKNOWN_HOST;
    }
    
    public void createJmxApplication(String connectionName, final String displayName,
            String username, String password, boolean saveCredentials) {
        // Initial check if the provided connectionName can be used for resolving the host/application
        final String normalizedConnectionName = normalizeConnectionName(connectionName);
        final JMXServiceURL serviceURL = getServiceURL(normalizedConnectionName);
        if (serviceURL == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            NbBundle.getMessage(JmxApplicationProvider.class, 
                            "MSG_Invalid_JMX_connection",normalizedConnectionName));    // NOI18N
                }
            });
            return;
        }

        // Create Host & JmxApplication
        ProgressHandle pHandle = null;
        try {
            pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(JmxApplicationProvider.class, "LBL_Adding") + displayName + "...");    // NOI18N
            pHandle.setInitialDelay(0);
            pHandle.start();
            
            Storage storage = new Storage(JmxApplicationsSupport.getStorageDirectory(),
                    System.currentTimeMillis() + Storage.DEFAULT_PROPERTIES_EXT);

            String[] keys = new String[]{
                SNAPSHOT_VERSION,
                PROPERTY_CONNECTION_STRING,
                PROPERTY_HOSTNAME,
                PROPERTY_USERNAME,
                PROPERTY_PASSWORD,
                DataSourceDescriptor.PROPERTY_NAME
            };

            String hostName = getHostName(serviceURL);
            hostName = hostName == null ? "" : hostName;
            String user = "";
            String passwd = "";
            if (saveCredentials) {
                user = username;
                passwd = password;
            }
            String[] values = new String[]{
                CURRENT_SNAPSHOT_VERSION,
                normalizedConnectionName,
                hostName,
                user,
                Utils.encodePassword(passwd),
                displayName
            };

            storage.setCustomProperties(keys, values);
            addJmxApplication(serviceURL, normalizedConnectionName,
                    hostName, username, password, saveCredentials, storage);
        } finally {
            final ProgressHandle pHandleF = pHandle;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (pHandleF != null) {
                        pHandleF.finish();
                    }
                }
            });
        }
    }

    private void addJmxApplication(JMXServiceURL serviceURL,
            final String connectionName, final String hostName,
            String username, String password, boolean saveCredentials,
            Storage storage) {
        // Resolve JMXServiceURL, finish if not resolved
        if (serviceURL == null) serviceURL = getServiceURL(connectionName);
        if (serviceURL == null) {
            storage.deleteCustomPropertiesStorage();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            NbBundle.getMessage(JmxApplicationProvider.class, 
                            "MSG_Invalid_JMX_connection",connectionName));  // NOI18N
                }
            });
            return;
        }
        // Resolve existing Host or create new Host, finish if Host cannot be resolved
        Host host;
        try {
            host = getHost(hostName, serviceURL);
        } catch (Exception e) {
            storage.deleteCustomPropertiesStorage();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            NbBundle.getMessage(JmxApplicationProvider.class, 
                            "MSG_Cannot_resolve_host",hostName));   // NOI18N
                }
            });
            return;            
        }
        // Create the JmxApplication
        final JmxApplication application =
                new JmxApplication(host, serviceURL, username, password, saveCredentials, storage);
        // Check if the given JmxApplication has been already added to the application tree
        final Set<JmxApplication> jmxapps = host.getRepository().getDataSources(JmxApplication.class);
        if (jmxapps.contains(application)) {
            storage.deleteCustomPropertiesStorage();
            JmxApplication tempapp = null;
            for (JmxApplication jmxapp : jmxapps) {
                if (jmxapp.equals(application)) {
                    tempapp = jmxapp;
                    break;
                }
            }
            final JmxApplication app = tempapp;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExplorerSupport.sharedInstance().selectDataSource(application);
                    NetBeansProfiler.getDefaultNB().displayWarning(
                            NbBundle.getMessage(JmxApplicationProvider.class, "MSG_JMX_connection") +   // NOI18N
                            application.getId() + NbBundle.getMessage(JmxApplicationProvider.class, "MSG_already_exists") + // NOI18N
                            DataSourceDescriptorFactory.getDescriptor(app).getName());
                }
            });
            return;
        }
        // Connect to the JMX agent
        JmxModel model = JmxModelFactory.getJmxModelFor(application);
        if (model.getConnectionState() == JmxModel.ConnectionState.DISCONNECTED) {
            storage.deleteCustomPropertiesStorage();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            NbBundle.getMessage(JmxApplicationProvider.class, "MSG_Cannot_connect_using") + connectionName);    // NOI18N
                }
            });
            return;
        }
        // precompute JVM
        application.jvm = JvmFactory.getJVMFor(application);
        // If everything succeeded, add datasource to application tree
        host.getRepository().addDataSource(application);
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
                if ('/' == urlPath.charAt(0)) {
                    hostname = "localhost"; // NOI18N
                } else if ('[' == urlPath.charAt(0)) { // IPv6 address
                    int closingSquareBracketIndex = urlPath.indexOf("]"); // NOI18N
                    if (closingSquareBracketIndex == -1) {
                        hostname = null;
                    } else {
                        hostname = urlPath.substring(0, closingSquareBracketIndex + 1);
                    }
                } else {
                    int colonIndex = urlPath.indexOf(":");
                    int slashIndex = urlPath.indexOf("/");
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

    private JMXServiceURL getServiceURL(String connectionString) {
        try {
            return new JMXServiceURL(connectionString);
        } catch (MalformedURLException e) {
            LOGGER.throwing(JMXServiceURL.class.getName(), "getServiceURL", e); // NOI18N
            return null;
        }
    }

    private void initPersistedApplications() {
        if (!JmxApplicationsSupport.storageDirectoryExists()) return;
        
        File[] files = JmxApplicationsSupport.getStorageDirectory().listFiles(
                new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
                    }
                });
        
        for (File file : files) {
            Storage storage = new Storage(file.getParentFile(), file.getName());
            Set<Storage> storageSet = persistedApplications.get(storage.getCustomProperty(PROPERTY_HOSTNAME));
            if (storageSet == null) {
                storageSet = new HashSet<Storage>();
                persistedApplications.put(storage.getCustomProperty(PROPERTY_HOSTNAME), storageSet);
            }
            storageSet.add(storage);
        }
        
        DataChangeListener<Host> dataChangeListener = new DataChangeListener<Host>() {

            public synchronized void dataChanged(DataChangeEvent<Host> event) {
                Set<Host> hosts = event.getAdded();
                for (Host host : hosts) {
                    String hostName = host.getHostName();
                    Set<Storage> storageSet = persistedApplications.get(hostName);
                    if (storageSet != null) {
                        persistedApplications.remove(hostName);
                        
                        String[] keys = new String[] {
                            PROPERTY_CONNECTION_STRING,
                            PROPERTY_HOSTNAME,
                            PROPERTY_USERNAME,
                            PROPERTY_PASSWORD
                        };

                        for (final Storage storage : storageSet) {
                            final String[] values = storage.getCustomProperties(keys);
                            RequestProcessor.getDefault().post(new Runnable() {
                                public void run() {
                                    addJmxApplication(null, values[0], values[1], values[2], Utils.decodePassword(values[3]), false, storage);
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

    public static void initialize() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                JmxApplicationProvider.sharedInstance().initPersistedApplications();
            }
        });
    }
}
