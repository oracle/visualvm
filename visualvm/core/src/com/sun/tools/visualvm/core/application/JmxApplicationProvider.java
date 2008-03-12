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

package com.sun.tools.visualvm.core.application;

import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.datasupport.Storage;
import com.sun.tools.visualvm.core.host.HostsSupport;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Set;
import javax.management.remote.JMXServiceURL;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 * A provider for Applications added as JMX connections.
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class JmxApplicationProvider extends DefaultDataSourceProvider<JmxApplication> {
    
    private static final String SNAPSHOT_VERSION = "snapshot_version";
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";
    private static final String CURRENT_SNAPSHOT_VERSION =
            CURRENT_SNAPSHOT_VERSION_MAJOR +
            SNAPSHOT_VERSION_DIVIDER +
            CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROPERTY_CONNECTION_STRING = "prop_conn_string";
    private static final String PROPERTY_HOSTNAME = "prop_conn_hostname";
    private static final String PROPERTY_USERNAME = "prop_username";
    private static final String PROPERTY_PASSWORD = "prop_password";

    private static JmxApplicationProvider sharedInstance;
    
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
    
    public void createJmxApplication(String connectionName, final String displayName) {
        // Initial check if the provided connectionName can be used for resolving the host/application
        final String normalizedConnectionName = normalizeConnectionName(connectionName);
        final JMXServiceURL serviceURL = getServiceURL(normalizedConnectionName);
        if (serviceURL == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            "<html>Invalid JMX connection: " +
                            normalizedConnectionName + "</html>");
                }
            });
            return;
        }

        // Create Host & JmxApplication
        ProgressHandle pHandle = null;
        try {
            pHandle = ProgressHandleFactory.createHandle("Adding " + displayName + "...");
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
            String[] values = new String[]{
                CURRENT_SNAPSHOT_VERSION,
                normalizedConnectionName,
                hostName == null ? "" : hostName,
                "", // Populated from dialog defining the JmxApplication if security is enabled
                "", // Populated from dialog defining the JmxApplication if security is enabled
                displayName
            };

            storage.setCustomProperties(keys, values);
            addJmxApplication(serviceURL, normalizedConnectionName, hostName, storage);
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
            final String connectionName, final String hostName, Storage storage) {
        // Resolve JMXServiceURL, finish if not resolved
        if (serviceURL == null) serviceURL = getServiceURL(connectionName);
        if (serviceURL == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            "<html>Invalid JMX connection: " + connectionName + "</html>");
                }
            });
            storage.deleteCustomPropertiesStorage();
            return;
        }
        
        // Resolve existing Host or create new Host, finish if Host cannot be resolved
        Host host;
        try {
            host = getHost(hostName, serviceURL);
        } catch (Exception e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            "<html>Cannot resolve host " + hostName + "</html>");
                }
            });
            storage.deleteCustomPropertiesStorage();
            return;            
        }
        
        // Create the JmxApplication
        JmxApplication application = new JmxApplication(host, serviceURL, storage);
        
        // Connect to the JMX agent
        JmxModel model = JmxModelFactory.getJmxModelFor(application);
        if (model.getConnectionState() == JmxModel.ConnectionState.DISCONNECTED) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(
                            "<html>Cannot connect using " + connectionName + "</html>");
                }
            });
            storage.deleteCustomPropertiesStorage();
            return;
        }
        
        // If everything succeeded, add datasource to application tree
        host.getRepository().addDataSource(application);
        registerDataSource(application);
    }

    public void removeJmxApplication(JmxApplication app) {
        app.getStorage().deleteCustomPropertiesStorage();
        unregisterDataSource(app);
    }
    
    protected <Y extends JmxApplication> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (JmxApplication app : removed) {
            if (app.getOwner() != null) app.getOwner().getRepository().removeDataSource(app);
            app.finished();
        }
    }
    
    private String normalizeConnectionName(String connectionName) {
        if (connectionName.startsWith("service:jmx:")) return connectionName;
        return "service:jmx:rmi:///jndi/rmi://" + connectionName + "/jmxrmi"; // hostname:port
    }
    
    private String getHostName(JMXServiceURL serviceURL) {
        // Try to compute the hostname instance
        // from the host in the JMXServiceURL.
        String hostname = serviceURL.getHost();
        if (hostname == null || hostname.isEmpty()) {
            hostname = null;
            // Try to compute the Host instance from the JNDI/RMI
            // Registry Service urlPath in the JMXServiceURL.
            if ("rmi".equals(serviceURL.getProtocol()) &&
                    serviceURL.getURLPath().startsWith("/jndi/rmi://")) {
                String urlPath =
                        serviceURL.getURLPath().substring("/jndi/rmi://".length());
                if ("/".equals(urlPath.charAt(0))) {
                    hostname = "localhost";
                } else {
                    int colonIndex = urlPath.indexOf(":");
                    int slashIndex = urlPath.indexOf("/");
                    int min = Math.min(colonIndex, slashIndex); // NOTE: can be -1!!!
                    if (min == -1) {
                        min = 0;
                    }
                    hostname = urlPath.substring(0, min);
                    if (hostname.isEmpty()) {
                        hostname = "localhost";
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
            Exceptions.printStackTrace(e);
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
            
            String[] keys = new String[] {
                PROPERTY_CONNECTION_STRING,
                PROPERTY_HOSTNAME,
                PROPERTY_USERNAME,
                PROPERTY_PASSWORD
            };
            
            String[] values = storage.getCustomProperties(keys);
            addJmxApplication(null, values[0],
                    values[1].length() == 0 ? null : values[1], storage);
        }
    }

    static void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(
                JmxApplicationProvider.sharedInstance());
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                JmxApplicationProvider.sharedInstance().initPersistedApplications();
            }
        });
    }
}
