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

package com.sun.tools.visualvm.core.host;

import com.sun.tools.visualvm.core.Install;
import com.sun.tools.visualvm.core.datasource.AbstractHost;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
// A provider for Hosts
class HostProvider extends DefaultDataSourceProvider<HostImpl> {
    
    private static final String SNAPSHOT_VERSION = "snapshot_version";
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROPERTY_IP = "prop_ip";

    private HostImpl LOCALHOST = null;
    private Host UNKNOWN_HOST = createUnknownHost();


    public Host getLocalhost() {
        return LOCALHOST;
    }
    
    public Host getUnknownHost() {
        return UNKNOWN_HOST;
    }


    public Host createHost(final HostProperties hostDescriptor, final boolean interactive) {
        final String hostName = hostDescriptor.getHostName();
        InetAddress inetAddress = null;
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandleFactory.createHandle("Searching for host " + hostName);
            pHandle.setInitialDelay(0);
            pHandle.start();
            try {
                inetAddress = InetAddress.getByName(hostName);
            } catch (UnknownHostException e) {
                if (interactive) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            NetBeansProfiler.getDefaultNB().displayError("<html><b>Cannot resolve host " + hostName + "</b><br><br>Make sure you have entered correct<br>host name or address.</html>");
                        }
                    });
                }
            }
        } finally {
            final ProgressHandle pHandleF = pHandle;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { if (pHandleF != null) pHandleF.finish(); }
            });
        }

        if (inetAddress != null) {
            final HostImpl knownHost = getHostByAddress(inetAddress);
            if (knownHost != null) {
                if (interactive) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ExplorerSupport.sharedInstance().selectDataSource(knownHost);
                            NetBeansProfiler.getDefaultNB().displayWarning("<html>Host " + hostName + " already monitored as " + DataSourceDescriptorFactory.getDescriptor(knownHost).getName() + "</html>");
                        }
                    });
                }
                return knownHost;
            } else {
                String ipString = inetAddress.getHostAddress();
                
                String[] propNames = new String[] {
                    SNAPSHOT_VERSION,
                    PROPERTY_IP,
                    DataSourceDescriptor.PROPERTY_NAME };
                String[] propValues = new String[] {
                    CURRENT_SNAPSHOT_VERSION,
                    ipString,
                    hostDescriptor.getDisplayName() };
                
                File customPropertiesStorage = Utils.getUniqueFile(HostsSupport.getStorageDirectory(), ipString, Storage.DEFAULT_PROPERTIES_EXT);
                Storage storage = new Storage(customPropertiesStorage.getParentFile(), customPropertiesStorage.getName());
                storage.setCustomProperties(propNames, propValues);
                
                HostImpl newHost = null;
                try {
                    newHost = new HostImpl(ipString, storage);
                } catch (Exception e) {
                    System.err.println("Error creating host: " + e.getMessage()); // Should never happen
                }
                
                if (newHost != null) {
                    RemoteHostsContainer.sharedInstance().getRepository().addDataSource(newHost);
                    registerDataSource(newHost);
                }
                return newHost;
            }
        }
        return null;
    }
    
    void removeHost(HostImpl host, boolean interactive) {
        // TODO: if interactive, show a Do-Not-Show-Again confirmation dialog
        unregisterDataSource(host);
        host.getStorage().deleteCustomPropertiesStorage();
    }
    
    
    protected <Y extends HostImpl> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (HostImpl host : removed) {
            RemoteHostsContainer.sharedInstance().getRepository().removeDataSource(host);
            host.finished();
        }
    }
    
    private HostImpl getHostByAddress(InetAddress inetAddress) {
        Set<HostImpl> knownHosts = getDataSources(HostImpl.class);
        for (HostImpl knownHost : knownHosts)
            if (knownHost.getInetAddress().equals(inetAddress)) return knownHost;
        return null;
    }
    
    // Here the Host instances for localhost and persisted remote hosts should be created
    private void initHosts() {
        initLocalHost();
        Install.LAZY_INIT_QUEUE.post(new Runnable() {
            public void run() {
                initPersistedHosts();
            }
        });
    }
    
    private void initLocalHost() {
        try {
            LOCALHOST = new HostImpl();
            DataSource.ROOT.getRepository().addDataSource(LOCALHOST);
            registerDataSource(LOCALHOST);
        } catch (UnknownHostException e) {
            System.err.println("Critical failure: cannot resolve localhost");
            NetBeansProfiler.getDefaultNB().displayError("Unable to resolve localhost!");
        }
    }
    
    private void initPersistedHosts() {
        if (!HostsSupport.storageDirectoryExists()) return;
        
        File[] files = HostsSupport.getStorageDirectory().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
            }
        });
        
        Set<HostImpl> hosts = new HashSet();
        for (File file : files) {
            Storage storage = new Storage(file.getParentFile(), file.getName());
            String hostName = storage.getCustomProperty(PROPERTY_IP);
            
            HostImpl persistedHost = null;
            try {
                persistedHost = new HostImpl(hostName, storage);
            } catch (Exception e) {
                System.err.println("Error loading persisted host: " + e.getMessage());
            }
            
            if (persistedHost != null) hosts.add(persistedHost);
        }
        
        RemoteHostsContainer.sharedInstance().getRepository().addDataSources(hosts);
        registerDataSources(hosts);
    }
    
    
    void initialize() {
        initHosts();
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
    }
    
    private Host createUnknownHost() {
        try {
            // Create a "placeholder" InetAddress instance
            // TODO: should be implemented differently!!!
            InetAddress address = InetAddress.getLocalHost(); 
            
            // Create host instance
            final Host host = new AbstractHost("unknown", address) {};
            
            // Only show the host when there's some DataSource in repository
            host.getRepository().addDataChangeListener(new DataChangeListener() {
                public void dataChanged(DataChangeEvent event) {
                    host.setVisible(!event.getCurrent().isEmpty());
                }
            }, DataSource.class);
            
            // Host will appear under Remote container
            RemoteHostsContainer.sharedInstance().getRepository().addDataSource(host);
            
            // Return host instance
            return host;
        } catch (UnknownHostException ex) {
            Exceptions.printStackTrace(ex); // Should never happen
            return null;
        }
    }

}
