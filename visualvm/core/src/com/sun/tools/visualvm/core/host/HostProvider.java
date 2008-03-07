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
import java.util.concurrent.Semaphore;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

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
    
    private static final String PROPERTY_HOSTNAME = "prop_hostname";

    private HostImpl LOCALHOST = null;
    private Host UNKNOWN_HOST = null;
    
    private boolean initializingHosts = true;
    private Semaphore initializingHostsSemaphore = new Semaphore(1);


    public Host getLocalhost() {
        return LOCALHOST;
    }
    
    public Host getUnknownHost() {
        return UNKNOWN_HOST;
    }


    public Host createHost(final HostProperties hostDescriptor, final boolean interactive) {
        waitForInitialization();
        
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
                    PROPERTY_HOSTNAME,
                    DataSourceDescriptor.PROPERTY_NAME };
                String[] propValues = new String[] {
                    CURRENT_SNAPSHOT_VERSION,
                    hostName,
                    hostDescriptor.getDisplayName() };
                
                File customPropertiesStorage = Utils.getUniqueFile(HostsSupport.getStorageDirectory(), ipString, Storage.DEFAULT_PROPERTIES_EXT);
                Storage storage = new Storage(customPropertiesStorage.getParentFile(), customPropertiesStorage.getName());
                storage.setCustomProperties(propNames, propValues);
                
                HostImpl newHost = null;
                try {
                    newHost = new HostImpl(hostName, storage);
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
        waitForInitialization();
        
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
    
    HostImpl getHostByAddress(InetAddress inetAddress) {
        waitForInitialization();
        
        Set<HostImpl> knownHosts = getDataSources(HostImpl.class);
        for (HostImpl knownHost : knownHosts)
            if (knownHost.getInetAddress().equals(inetAddress)) return knownHost;
        return null;
    }
    
    // Here the Host instances for localhost and persisted remote hosts should be created
    private void initHosts() {
        initLocalHost();
        initUnknownHost();
        RequestProcessor.getDefault().post(new Runnable() {
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
    
    private void initUnknownHost() {
        try {
            // Create a "placeholder" InetAddress instance
            InetAddress address = InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }); 
            
            // Create host instance
            UNKNOWN_HOST = new AbstractHost("unknown", address) {};
            
            // Only show the host when there's some DataSource in repository
            UNKNOWN_HOST.getRepository().addDataChangeListener(new DataChangeListener() {
                public void dataChanged(DataChangeEvent event) {
                    UNKNOWN_HOST.setVisible(!event.getCurrent().isEmpty());
                }
            }, DataSource.class);
            
            // Host will appear under Remote container
            RemoteHostsContainer.sharedInstance().getRepository().addDataSource(UNKNOWN_HOST);
        } catch (UnknownHostException e) {
            System.err.println("Failure: cannot resolve <unknown> host");
        }
    }
    
    private void initPersistedHosts() {
        if (HostsSupport.storageDirectoryExists()) {
            File[] files = HostsSupport.getStorageDirectory().listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
                }
            });

            Set<HostImpl> hosts = new HashSet();
            for (File file : files) {
                Storage storage = new Storage(file.getParentFile(), file.getName());
                String hostName = storage.getCustomProperty(PROPERTY_HOSTNAME);

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
        
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                initializingHostsSemaphore.release();
                initializingHosts = false;
            }
        });
    }
    
    
    private void waitForInitialization() {
        if (initializingHosts) try {
            initializingHostsSemaphore.acquire();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    
    void initialize() {
        initHosts();
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
    }
    
    HostProvider() {
        try {
            initializingHostsSemaphore.acquire();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
