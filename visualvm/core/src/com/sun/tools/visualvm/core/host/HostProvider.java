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

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.net.UnknownHostException;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
// A provider for Hosts
class HostProvider extends DefaultDataSourceProvider<HostImpl> {

    private HostImpl LOCALHOST = null;


    public Host getLocalhost() {
        return LOCALHOST;
    }


    public void createHost(final HostProperties hostDescriptor, final boolean interactive) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                createHostImpl(hostDescriptor, interactive);
            }
        });
    }
    
    void removeHost(HostImpl host, boolean interactive) {
        // TODO: if interactive, show a Do-Not-Show-Again confirmation dialog
        unregisterDataSource(host);
    }
    
    
    protected <Y extends HostImpl> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (HostImpl host : removed) {
            RemoteHostsContainer.sharedInstance().getRepository().removeDataSource(host);
            host.finished();
        }
    }
    
    
    private void createHostImpl(HostProperties hostDescriptor, boolean interactive) {
        final String hostName = hostDescriptor.getHostName();
        HostImpl newHost = null;
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandleFactory.createHandle("Searching for host " + hostName);
            pHandle.setInitialDelay(0);
            pHandle.start();
            try {
                newHost = new HostImpl(hostName, hostDescriptor.getDisplayName());
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

        if (newHost != null) {
            final Set<HostImpl> knownHosts = getDataSources(HostImpl.class);
            if (knownHosts.contains(newHost)) {
                if (interactive) {
                    final HostImpl newHostF = newHost;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            HostImpl existingHost = null;
                            for (HostImpl knownHost : knownHosts) if (knownHost.equals(newHostF)) { existingHost = knownHost; break; }
                            ExplorerSupport.sharedInstance().selectDataSource(existingHost);
                            NetBeansProfiler.getDefaultNB().displayWarning("<html>Host " + hostName + " already monitored as " + existingHost.getDisplayName() + "</html>");
                        }
                    });
                }
            } else {
                RemoteHostsContainer.sharedInstance().getRepository().addDataSource(newHost);
                registerDataSource(newHost);
            }
        }
    }
    
    // Here the Host instances for localhost and persisted remote hosts should be created
    private void initHosts() {
        initLocalHost();
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                initPersistedHosts();
            }
        });
    }
    
    private void initLocalHost() {
        try {
            LOCALHOST = new HostImpl("localhost", "Local");
            DataSource.ROOT.getRepository().addDataSource(LOCALHOST);
            registerDataSource(LOCALHOST);
        } catch (UnknownHostException e) {
            System.err.println("Critical failure: cannot resolve localhost");
            NetBeansProfiler.getDefaultNB().displayError("Unable to resolve localhost!");
        }
    }
    
    private void initPersistedHosts() {
//        HostDescriptor localhostDescriptor = new HostDescriptor("msedliak-ws.czech.sun.com", "sedlak");
//        createHostImpl(localhostDescriptor, false);
//        HostDescriptor localhostDescriptor2 = new HostDescriptor("129.157.21.26", "sedlak2");
//        createHostImpl(localhostDescriptor2, false);
        // TODO: read persisted hosts from some persistent storage/nbpreferences
    }
    
    
    void initialize() {
        initHosts();
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
    }

}
