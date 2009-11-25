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

import com.sun.tools.visualvm.jmx.JmxApplicationsSupport;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.host.HostsSupport;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.jmx.JmxApplicationException;
import com.sun.tools.visualvm.jmx.PasswordAuthJmxEnvironmentFactory;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * A provider for Applications added as JMX connections.
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 * @author Michal Bachorik
 */
public class JmxApplicationProvider {
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
    private static final String PROPERTY_ENV_KEYS = "prop_env_keys";    // NOI18N
    private static final String PROPERTIES_FILE = "jmxapplication" + Storage.DEFAULT_PROPERTIES_EXT;  // NOI18N
    static final String JMX_SUFFIX = ".jmx";  // NOI18N


    private boolean trackingNewHosts;
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

    public JmxApplication createJmxApplication(String connectionName, String displayName,
            final String username, final String password, boolean saveCredentials, boolean persistent) throws JmxApplicationException {
        Map<String, Object> env = PasswordAuthJmxEnvironmentFactory.getSharedInstance().createJmxEnvironment(new CallbackHandler() {

            public void handle(Callback[] callbacks) {
                for (Callback c : callbacks) {
                    if (c instanceof NameCallback) {
                        NameCallback ncb = (NameCallback) c;
                        ncb.setName(username);
                    } else if (c instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) c;
                        pcb.setPassword(password.toCharArray());
                    }
                }
            }
        });
        return createJmxApplication(connectionName, displayName, env, saveCredentials, persistent);
    }

    public JmxApplication createJmxApplication(String connectionName, final String displayName,
            Map<String, Object> environment, boolean saveCredentials, boolean persistent) throws JmxApplicationException {
        // Initial check if the provided connectionName can be used for resolving the host/application
        final String normalizedConnectionName = normalizeConnectionName(connectionName);
        final JMXServiceURL serviceURL;
        try {
            serviceURL = getServiceURL(normalizedConnectionName);
        } catch (MalformedURLException ex) {
            throw new JmxApplicationException(NbBundle.getMessage(JmxApplicationProvider.class,
                                "MSG_Invalid_JMX_connection",normalizedConnectionName),ex); // NOI18N
        }

        String hostName = getHostName(serviceURL);
        hostName = hostName == null ? "" : hostName; // NOI18N

        Storage storage = null;

        if (persistent) {
            try {
                storage = persistJmxApplication(null, connectionName, displayName, environment);
            } catch (IOException ex) {
                throw new JmxApplicationException("some message here", ex);
            }
        }

        return addJmxApplication(serviceURL, displayName, environment, normalizedConnectionName,
                    hostName, saveCredentials, storage);
    }

    private JmxApplication addJmxApplication(JMXServiceURL serviceURL, String displayName, Map<String, Object> serviceEnvironment,
            final String connectionName, final String hostName,
            boolean saveCredentials, Storage storage) throws JmxApplicationException {
        if (serviceURL == null) {
            try {
                serviceURL = getServiceURL(connectionName);
            } catch (MalformedURLException ex) {
                if (storage != null) {
                    File appStorage = storage.getDirectory();
                    if (appStorage.isDirectory()) Utils.delete(appStorage, true);
                }
                throw new JmxApplicationException(NbBundle.getMessage(JmxApplicationProvider.class,
                                    "MSG_Invalid_JMX_connection",connectionName),ex); // NOI18N
            }
        }
        // Resolve existing Host or create new Host, finish if Host cannot be resolved
        Set<Host> hosts = DataSourceRepository.sharedInstance().getDataSources(Host.class);
        Host host = null;
        try {
            host = getHost(hostName, serviceURL);
        } catch (Exception e) {
            if (storage != null) {
                File appStorage = storage.getDirectory();
                if (appStorage.isDirectory()) Utils.delete(appStorage, true);
            }
            cleanupCreatedHost(hosts, host);
            throw new JmxApplicationException(NbBundle.getMessage(JmxApplicationProvider.class,
                                       "MSG_Cannot_resolve_host",hostName),e); // NOI18N
        }
        // Create the JmxApplication
        if (storage != null)
            storage.setCustomProperty(PROPERTY_HOSTNAME, host.getHostName());

        final JmxApplication application =
                new JmxApplication(host, serviceURL, serviceEnvironment, saveCredentials, storage);
        // Check if the given JmxApplication has been already added to the application tree
        final Set<JmxApplication> jmxapps = host.getRepository().getDataSources(JmxApplication.class);
        if (jmxapps.contains(application)) {
            if (storage != null) {
                File appStorage = storage.getDirectory();
                if (appStorage.isDirectory()) Utils.delete(appStorage, true);
            }
            JmxApplication tempapp = null;
            for (JmxApplication jmxapp : jmxapps) {
                if (jmxapp.equals(application)) {
                    tempapp = jmxapp;
                    break;
                }
            }
            cleanupCreatedHost(hosts, host);
            throw new JmxApplicationException(NbBundle.getMessage(JmxApplicationProvider.class, "MSG_JMX_connection") +   // NOI18N
                                application.getId() + NbBundle.getMessage(JmxApplicationProvider.class, "MSG_already_exists") + // NOI18N
                                DataSourceDescriptorFactory.getDescriptor(tempapp).getName());
        }
        // Connect to the JMX agent
        JmxModel model = JmxModelFactory.getJmxModelFor(application);
        if (model == null || model.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
            application.setStateImpl(Stateful.STATE_UNAVAILABLE);
            if (storage != null) {
                File appStorage = storage.getDirectory();
                if (appStorage.isDirectory()) Utils.delete(appStorage, true);
            }
            cleanupCreatedHost(hosts, host);
            throw new JmxApplicationException(NbBundle.getMessage(JmxApplicationProvider.class,
                                       "MSG_Cannot_connect_using") + connectionName); // NOI18N
        }
        model.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() == ConnectionState.CONNECTED) {
                    application.setStateImpl(Stateful.STATE_AVAILABLE);
                } else {
                    application.setStateImpl(Stateful.STATE_UNAVAILABLE);
                }
            }
        });
        // set displayname if not peristent (already set for such case)
        if (storage == null)
            application.getStorage().setCustomProperty(DataSourceDescriptor.PROPERTY_NAME, displayName);
        // precompute JVM
        application.jvm = JvmFactory.getJVMFor(application);
        // If everything succeeded, add datasource to application tree
        host.getRepository().addDataSource(application);

        return application;
    }

    private void cleanupCreatedHost(Set<Host> hosts, Host host) {
        // NOTE: this is not absolutely failsafe, if resolving the JMX application
        // took a long time and its host has been added by the user/plugin, it may
        // be removed by this call. Hopefully just a hypothetical case...
        if (host != null && !hosts.contains(host) && host.getOwner() != null)
            host.getOwner().getRepository().removeDataSource(host);
    }

    private static String normalizeConnectionName(String connectionName) {
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

        for (File file : files) {
            if (file.isDirectory()) {
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
                Set<Host> hosts = event.getAdded();
                for (Host host : hosts) {
                    String hostName = host.getHostName();
                    Set<Storage> storageSet = persistedApplications.get(hostName);
                    if (storageSet != null) {
                        persistedApplications.remove(hostName);

                        String[] keys = new String[] {
                            PROPERTY_CONNECTION_STRING,
                            PROPERTY_HOSTNAME,
                            PROPERTY_ENV_KEYS
                        };

                        for (final Storage storage : storageSet) {
                            final String[] values = storage.getCustomProperties(keys);
                            List<String> envKeyList = Collections.<String>emptyList();
                            String env_keys_snap = values[2];
                            if (env_keys_snap != null && env_keys_snap.length() != 0) {
                                try {
                                    envKeyList = JmxApplicationProvider.<LinkedList<String>>revertEnviromentObject(env_keys_snap);
                                } catch (final IOException e) {
                                    SwingUtilities.invokeLater(new Runnable() {

                                        public void run() {
                                            NetBeansProfiler.getDefaultNB().displayError(e.getMessage());
                                        }
                                    });
                                }
                            }
                            final Map<String, Object> environment = new HashMap<String, Object>(envKeyList.size());
                            for (String env_key : envKeyList) {
                                String env_value_snap = storage.getCustomProperty(env_key);
                                try {
                                    Object env_value = revertEnviromentObject(env_value_snap);
                                    environment.put(env_key, env_value);
                                } catch (final IOException e) {
                                    SwingUtilities.invokeLater(new Runnable() {

                                        public void run() {
                                            NetBeansProfiler.getDefaultNB().displayError(e.getMessage());
                                        }
                                    });
                                }
                            }
                            RequestProcessor.getDefault().post(new Runnable() {

                                public void run() {
                                    try {
                                        addJmxApplication(null, null, environment, values[0], values[1], false, storage);
                                    } catch (final JmxApplicationException e) {
                                        SwingUtilities.invokeLater(new Runnable() {
                                            public void run() {
                                                NetBeansProfiler.getDefaultNB().displayError(e.getMessage());
                                            }
                                        });
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

    public void initialize() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        initPersistedApplications();
                    }
                });
            }
        });
    }

    /**
     * A helper method for persisting the data related to JmxApplication. To
     * be used only for storing the data for jmx applications that are using
     * password-based authentication.
     *
     * @param storage a storage to be used (if null, new storage will be created)
     * @param connectionName a string holding the jmx service url, can be null
     * @param displayName a string hodling the user-friendly display name, can be null
     * @param username a name of the user for jmx authentication
     * @param password a password of the user for jmx authentication
     * @return an instance of storage with stored data
     * @throws java.io.IOException if any problem occurs during saving the data
     */
    public static Storage persistJmxApplication(Storage storage, String connectionName, String displayName, String username, String password) throws IOException {
        Map<String, Object> env = Collections.<String, Object>emptyMap();
        try {
            env = PasswordAuthJmxEnvironmentFactory.getSharedInstance().createJmxEnvironment(new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (JmxApplicationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return persistJmxApplication(storage, connectionName, displayName, env);
    }

    /**
     * A helper method for persisting the data related to JmxApplication.
     *
     * @param storage a storage to be used (if null, new storage will be created)
     * @param connectionName a string holding the jmx service url, can be null
     * @param displayName a string hodling the user-friendly display name, can be null
     * @param environment a jmx environment holding other jmx connection attributes
     * @return an instance of storage with stored data
     * @throws java.io.IOException if any problem occurs during saving the data
     */
    public static Storage persistJmxApplication(Storage storage, String connectionName, String displayName, Map<String, Object> environment) throws IOException {
        if (storage == null) {
            File storageDirectory = Utils.getUniqueFile(JmxApplicationsSupport.getStorageDirectory(),
                    "" + System.currentTimeMillis(), JMX_SUFFIX);    // NOI18N
            Utils.prepareDirectory(storageDirectory);
            storage = new Storage(storageDirectory, PROPERTIES_FILE);
        }

        int storagePropsSize = 4;
        if (environment != null) {
            storagePropsSize += environment.size();
        }

        List<String> keysList = new ArrayList<String>(storagePropsSize);
        List<String> valuesList = new ArrayList<String>(storagePropsSize);

        keysList.add(SNAPSHOT_VERSION);
        valuesList.add(CURRENT_SNAPSHOT_VERSION);

        if (connectionName != null && connectionName.length()!=0) {
            keysList.add(PROPERTY_CONNECTION_STRING);
            valuesList.add(normalizeConnectionName(connectionName));
        }

        if (displayName != null && displayName.length()!=0) {
            keysList.add(DataSourceDescriptor.PROPERTY_NAME);
            valuesList.add(displayName);
        }

        if (environment != null) {
            LinkedList<String> env_keys = new LinkedList<String>(environment.keySet());
            String env_keys_snap = makeEnvironmentObjectSnapShot(env_keys);
            // snapshot of environment keys was successful
            keysList.add(PROPERTY_ENV_KEYS);
            valuesList.add(env_keys_snap);
            for (String env_key : environment.keySet()) {
                if (environment.get(env_key) != null) {
                    String env_value_snap = makeEnvironmentObjectSnapShot(environment.get(env_key));
                    keysList.add(env_key);
                    valuesList.add(env_value_snap);
                }
            }
        }

        storage.setCustomProperties(
                keysList.toArray(new String[keysList.size()]),
                valuesList.toArray(new String[valuesList.size()]));

        return storage;
    }

    /**
     * A helper method to encode the object to string to use with <code>Storage</code>.
     * Method will save the object to byte stream which is then encoded using Base64
     * algorithm.
     *
     * To successfully encode the object, it has implement <code>Serializable</code> interface.
     *
     * @param object an object to encode
     * @return a string representation of encoded object
     * @throws java.io.IOException if object is not serializable
     */
    public static String makeEnvironmentObjectSnapShot(Object object) throws IOException {
        byte[] snapshot = null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(object);
        oos.flush();
        snapshot = bos.toByteArray();
        bos.close();
        oos.close();

        return new BASE64Encoder().encode(snapshot);
    }

    /**
     * A helper method to decode the object from string that was encoded using
     * the <code>JmxApplicationProvider.makeEnvironmentObjectSnapShot(Object object)</code>
     * method.
     *
     * @param <T> type of the object
     * @param object string representation of encoded object
     * @return a decoded object
     * @throws java.io.IOException if object can not be decoded
     */
    public static <T> T revertEnviromentObject(String object) throws IOException {
        byte[] snapshot = new BASE64Decoder().decodeBuffer(object);
        ByteArrayInputStream bis = new ByteArrayInputStream(snapshot);
        ObjectInputStream ois = new ObjectInputStream(bis);

        Object data = null;
        try {
            data = ois.readObject();
        } catch (ClassNotFoundException cnfe) {
            throw new IOException(cnfe);
        } finally {
            bis.close();
            ois.close();
        }

        return (T) data;
    }
}
