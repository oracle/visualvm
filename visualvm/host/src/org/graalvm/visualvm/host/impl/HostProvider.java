/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.host.impl;

import org.graalvm.visualvm.host.HostsSupport;
import org.graalvm.visualvm.host.RemoteHostsContainer;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.DataSourceContainer;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.explorer.ExplorerSupport;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FilenameFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.VisualVM;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
// A provider for Hosts
public class HostProvider {
    private static final Logger LOGGER = Logger.getLogger(HostProvider.class.getName());

    private static final String SNAPSHOT_VERSION = "snapshot_version";  // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;

    private static final String PROPERTY_HOSTNAME = "prop_hostname";    // NOI18N
    
    private static final String DNSA_KEY = "HostProvider_NotifyUnresolved"; // NOI18N

    private static InetAddress localhostAddress2;


    private Semaphore hostsLockedSemaphore = new Semaphore(1);


    public Host createHost(final HostProperties hostDescriptor, final boolean createOnly, final boolean interactive) {
        try {

            lockHosts();

            final String hostName = hostDescriptor.getHostName();
            InetAddress inetAddress = null;
            ProgressHandle pHandle = null;

            try {
                pHandle = ProgressHandle.createHandle(NbBundle.getMessage(HostProvider.class, "LBL_Searching_for_host") + hostName); // NOI18N
                pHandle.setInitialDelay(0);
                pHandle.start();
                try {
                    inetAddress = InetAddress.getByName(hostName);
                } catch (UnknownHostException e) {
                    if (interactive) {
                        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                                Message(NbBundle.getMessage(HostProvider.class,
                                "MSG_Wrong_Host", hostName), NotifyDescriptor. // NOI18N
                                ERROR_MESSAGE));
                    }
                }
            } finally {
                final ProgressHandle pHandleF = pHandle;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { if (pHandleF != null) pHandleF.finish(); }
                });
            }

            if (inetAddress != null) {
                final Host knownHost = getHostByAddressImpl(inetAddress);
                if (knownHost != null) {
                    if (interactive && createOnly) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ExplorerSupport.sharedInstance().selectDataSource(knownHost);
                                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                                Message(NbBundle.getMessage(HostProvider.class,
                                        "MSG_Already_Monitored",new Object[] // NOI18N
                                        {hostName,DataSourceDescriptorFactory.
                                        getDescriptor(knownHost).getName()}),
                                        NotifyDescriptor.WARNING_MESSAGE));
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

                    Host newHost = null;
                    try {
                        newHost = new RemoteHostImpl(hostName, storage);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error creating host", e); // Should never happen  // NOI18N
                    }

                    if (newHost != null) {
                        DataSourceContainer remoteHosts = RemoteHostsContainer.sharedInstance().getRepository();
                        Set<Host> remoteHostsSet = remoteHosts.getDataSources(Host.class);
                        if (!createOnly && remoteHostsSet.contains(newHost)) {
                            storage.deleteCustomPropertiesStorage();
                            Iterator<Host> existingHosts = remoteHostsSet.iterator();
                            while (existingHosts.hasNext()) {
                                Host existingHost = existingHosts.next();
                                if (existingHost.equals(newHost)) {
                                    newHost = existingHost;
                                    break;
                                }
                            }
                        } else {
                            if (hostDescriptor.getPropertiesCustomizer() != null)
                                hostDescriptor.getPropertiesCustomizer().propertiesDefined(newHost);
                            remoteHosts.addDataSource(newHost);
                        }
                    }
                    return newHost;
                }
            }
            return null;

        } catch (InterruptedException ex) {
            LOGGER.throwing(HostProvider.class.getName(), "createHost", ex);    // NOI18N
            return null;
        } finally {
            unlockHosts();
        }
    }

    void removeHost(RemoteHostImpl host, boolean interactive) {
        try {
            lockHosts();

            // TODO: if interactive, show a Do-Not-Show-Again confirmation dialog
            DataSource owner = host.getOwner();
            if (owner != null) owner.getRepository().removeDataSource(host);

        } catch (InterruptedException ex) {
            LOGGER.throwing(HostProvider.class.getName(), "removeHost", ex);    // NOI18N
        } finally {
            unlockHosts();
        }
    }

    public Host getHostByAddress(InetAddress inetAddress) {
        try {
            lockHosts();
            return getHostByAddressImpl(inetAddress);
        } catch (InterruptedException ex) {
            LOGGER.throwing(HostProvider.class.getName(), "getHostByAddress", ex);    // NOI18N
            return null;
        } finally {
            unlockHosts();
        }
    }

    private Host getHostByAddressImpl(InetAddress inetAddress) {
        Set<RemoteHostImpl> knownHosts = DataSourceRepository.sharedInstance().getDataSources(RemoteHostImpl.class);
        for (RemoteHostImpl knownHost : knownHosts)
            if (knownHost.getInetAddress().equals(inetAddress)) return knownHost;

        if (inetAddress.equals(Host.LOCALHOST.getInetAddress())) return Host.LOCALHOST;
        if (inetAddress.equals(localhostAddress2)) return Host.LOCALHOST;
        if (inetAddress.isLoopbackAddress()) return Host.LOCALHOST;

        return null;
    }

    public Host createLocalHost() {
        try {
            return new LocalHostImpl();
        } catch (UnknownHostException e) {
            LOGGER.severe("Critical failure: cannot resolve localhost");    // NOI18N
            return null;
        }
    }

    public Host createUnknownHost() {
        try {
            return new Host("unknown", InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 })) {}; // NOI18N
        } catch (UnknownHostException e) {
            LOGGER.severe("Failure: cannot resolve <unknown> host");    // NOI18N
            return null;
        }
    }

    private void initLocalHost() {
        try {
            localhostAddress2 = InetAddress.getLocalHost();
        } catch (java.net.UnknownHostException e) {}

        if (Host.LOCALHOST != null)
            DataSource.ROOT.getRepository().addDataSource(Host.LOCALHOST);
    }

    private void initUnknownHost() {
        final Host unknownhost = Host.UNKNOWN_HOST;
        if (unknownhost != null) {
            unknownhost.getRepository().addDataChangeListener(new DataChangeListener() {
                public void dataChanged(DataChangeEvent event) {
                    unknownhost.setVisible(!event.getCurrent().isEmpty());
                }
            }, DataSource.class);
            RemoteHostsContainer.sharedInstance().getRepository().addDataSource(unknownhost);
        }
    }

    private void initPersistedHosts() {
        if (HostsSupport.storageDirectoryExists()) {
            File storageDir = HostsSupport.getStorageDirectory();
            File[] files = storageDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
                }
            });

            Set<File> unresolvedHostsF = new HashSet();
            Set<String> unresolvedHostsS = new HashSet();

            Set<RemoteHostImpl> hosts = new HashSet();
            for (File file : files) {
                if (HostsSupportImpl.LOCALHOST_PROPERTIES_FILENAME.equals(file.getName()))
                    continue;

                Storage storage = new Storage(storageDir, file.getName());
                String hostName = storage.getCustomProperty(PROPERTY_HOSTNAME);

                RemoteHostImpl persistedHost = null;
                try {
                    persistedHost = new RemoteHostImpl(hostName, storage);
                } catch (Exception e) {
                    LOGGER.throwing(HostProvider.class.getName(), "initPersistedHosts", e);    // NOI18N
                    unresolvedHostsF.add(file);
                    unresolvedHostsS.add(hostName);
                }

                if (persistedHost != null) hosts.add(persistedHost);
            }

            if (!unresolvedHostsF.isEmpty()) notifyUnresolvedHosts(unresolvedHostsF, unresolvedHostsS);

            RemoteHostsContainer.sharedInstance().getRepository().addDataSources(hosts);
        }
    }

    private static void notifyUnresolvedHosts(final Set<File> unresolvedHostsF, final Set<String> unresolvedHostsS) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                String s = GlobalPreferences.sharedInstance().getDoNotShowAgain(DNSA_KEY);
                Boolean b = s == null ? null : Boolean.parseBoolean(s);
                
                if (b == null) {
                    JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
                    messagePanel.add(new JLabel(NbBundle.getMessage(HostProvider.class, "MSG_Unresolved_Hosts")), BorderLayout.NORTH); // NOI18N
                    JList list = new JList(unresolvedHostsS.toArray());
                    list.setVisibleRowCount(4);
                    messagePanel.add(new JScrollPane(list), BorderLayout.CENTER);
                    JCheckBox dnsa = new JCheckBox();
                    Mnemonics.setLocalizedText(dnsa, NbBundle.getMessage(HostProvider.class, "LBL_RememberAction")); // NOI18N
                    dnsa.setToolTipText(NbBundle.getMessage(HostProvider.class, "TTP_RememberAction")); // NOI18N
                    JPanel p = new JPanel(new BorderLayout());
                    p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 20));
                    p.add(dnsa, BorderLayout.WEST);
                    messagePanel.add(p, BorderLayout.SOUTH);
                    NotifyDescriptor dd = new NotifyDescriptor(
                            messagePanel, NbBundle.getMessage(HostProvider.class, "Title_Unresolved_Hosts"), // NOI18N
                            NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.ERROR_MESSAGE,
                            null, NotifyDescriptor.YES_OPTION);
                    Object ret = DialogDisplayer.getDefault().notify(dd);
                    
                    if (ret == NotifyDescriptor.NO_OPTION) b = Boolean.FALSE;
                    else if (ret == NotifyDescriptor.YES_OPTION) b = Boolean.TRUE;
                    
                    if (dnsa.isSelected() && b != null) GlobalPreferences.sharedInstance().setDoNotShowAgain(DNSA_KEY, b.toString());
                }
                
                if (Boolean.FALSE.equals(b))
                    for (File file : unresolvedHostsF) Utils.delete(file, true);

                unresolvedHostsF.clear();
                unresolvedHostsS.clear();
            }
        }, 1000);
    }


    private void lockHosts() throws InterruptedException {
        hostsLockedSemaphore.acquire();
    }

    private void unlockHosts() {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                hostsLockedSemaphore.release();
            }
        });
    }


    public void initialize() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                VisualVM.getInstance().runTask(new Runnable() {
                    public void run() {
                        initLocalHost();
                        initUnknownHost();
                        initPersistedHosts();
                        unlockHosts();
                    }
                });
            }
        });
    }


    public HostProvider() {
        try {
            lockHosts(); // Immediately lock the hosts, will be released after initialize()
        } catch (InterruptedException ex) {
            LOGGER.throwing(HostProvider.class.getName(), "<init>", ex);    // NOI18N
        }
    }

}
