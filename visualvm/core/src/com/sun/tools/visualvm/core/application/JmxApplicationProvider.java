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
import com.sun.tools.visualvm.core.host.HostsSupport;
import com.sun.tools.visualvm.core.host.RemoteHostsContainer;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Set;
import javax.management.remote.JMXServiceURL;
import javax.swing.SwingWorker;
import org.openide.util.Exceptions;

/**
 * A provider for Applications added as JMX connections.
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class JmxApplicationProvider extends DefaultDataSourceProvider<JmxApplication> {

    private static JmxApplicationProvider sharedInstance;
    
    public synchronized static JmxApplicationProvider sharedInstance() {
        if (sharedInstance == null) sharedInstance = new JmxApplicationProvider();
        return sharedInstance;
    }

    private static boolean isLocalHost(String hostname) {
        try {
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
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
            return false;
        }
    }

    private void computeHost(String hostname, JMXServiceURL url, String displayName) {
        Set<Host> hosts = RemoteHostsContainer.sharedInstance().getRepository().getDataSources(Host.class);
        // Try to compute the Host instance from a hostname.
        if (hostname != null) {
            if (hostname.isEmpty() || isLocalHost(hostname)) {
                addHost(Host.LOCALHOST, url, hostname, displayName);
                return;
            } else {
                try {
                    InetAddress addr = InetAddress.getByName(hostname);
                    for (Host host : hosts) {
                        if (addr.getHostAddress().equals(host.getInetAddress().getHostAddress())) {
                            addHost(host, url, hostname, displayName);
                            return;
                        }
                    }
                    addHost(null, url, hostname, displayName);
                    return;
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                    return;
                }
            }
        }
        // Try to compute the Host instance from the JMXServiceURL.
        String urlHost = url.getHost();
        if (urlHost != null && !urlHost.isEmpty()) {
            if (isLocalHost(urlHost)) {
                addHost(Host.LOCALHOST, url, urlHost, displayName);
                return;
            } else {
                try {
                    InetAddress addr = InetAddress.getByName(urlHost);
                    for (Host host : hosts) {
                        if (addr.getHostAddress().equals(host.getInetAddress().getHostAddress())) {
                            addHost(host, url, urlHost, displayName);
                            return;
                        }
                    }
                    addHost(null, url, urlHost, displayName);
                    return;
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                    return;
                }
            }
        }
        // TODO: Try to compute the Host instance from the JMXServiceURL
        //       using the RMIRegistry/COSNaming info in urlPath
        // TODO: Connect to the agent and try to get the hostname
        addHost(Host.LOCALHOST, url, urlHost, displayName);
        return;
    }

    private void addHost(final Host host, final JMXServiceURL url,
            final String hostname, final String displayName) {
        new SwingWorker<JmxApplication, Void>() {
            @Override
            public JmxApplication doInBackground() {
                if (host == null) {
                    Host newHost = HostsSupport.getInstance().createHost(hostname);
                    return new JmxApplication(newHost, displayName, url);
                } else {
                    return new JmxApplication(host, displayName, url);
                }
            }
            @Override
            protected void done() {
                try {
                    JmxApplication app = get();
                    app.getHost().getRepository().addDataSource(app);
                    registerDataSource(app);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                }
            }
        }.execute();
    }

    public void addJmxApplication(String connectionName, String displayName) {
        try {
            String hostname = null;
            if (!connectionName.startsWith("service:jmx:")) { // hostname:port
                hostname = connectionName.substring(0, connectionName.indexOf(":"));
                connectionName = "service:jmx:rmi:///jndi/rmi://" +
                        connectionName + "/jmxrmi";
            }
            computeHost(hostname, new JMXServiceURL(connectionName), displayName);
        } catch (MalformedURLException e) {
            Exceptions.printStackTrace(e);
        }
    }

    public void removeJmxApplication(JmxApplication app) {
        app.getHost().getRepository().removeDataSource(app);
        unregisterDataSource(app);
    }

    static void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(
                JmxApplicationProvider.sharedInstance());
    }
}
