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

import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.event.HostEvent;
import sun.jvmstat.monitor.event.HostListener;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
// A provider for MonitoredHostDS
class MonitoredHostProvider extends DefaultDataSourceProvider<MonitoredHostDS> implements DataChangeListener<Host> {
    
    private static final RequestProcessor processor = new RequestProcessor("MonitoredHostProvider Processor");
    
    private final Map<Host, HostListener> mapping = Collections.synchronizedMap(new HashMap());
    
    private final DataFinishedListener<Host> hostFinishedListener = new DataFinishedListener<Host>() {
        public void dataFinished(Host host) { processFinishedHost(host); }
    };
            
    
    public void dataChanged(final DataChangeEvent<Host> event) {
        processor.post(new Runnable() {
            public void run() {
                Set<Host> newHosts = event.getAdded();
                for (Host host : newHosts) processNewHost(host);
            }
        });
    }
    
    
    private void processNewHost(final Host host) {        
        try {
            MonitoredHostDS monitoredHostDS = new MonitoredHostDS(host);
            host.getRepository().addDataSource(monitoredHostDS);
            registerDataSource(monitoredHostDS);
            final MonitoredHost monitoredHost = monitoredHostDS.getMonitoredHost();
            HostListener monitoredHostListener = new HostListener() {
                public void vmStatusChanged(final VmStatusChangeEvent e) {}
                public void disconnected(HostEvent e) { processFinishedHost(host); }
            };
            mapping.put(host, monitoredHostListener);
            monitoredHost.addHostListener(monitoredHostListener);
            
            host.notifyWhenFinished(hostFinishedListener);
            
        } catch (Exception e) {
            // Host doesn't support jvmstat monitoring (jstatd not running)
            // TODO: maybe display a hint that by running jstatd on that host applications can be discovered automatically
        }
    }
    
    private void processFinishedHost(Host host) {
        Set<MonitoredHostDS> monitoredHosts = host.getRepository().getDataSources(MonitoredHostDS.class);
        host.getRepository().removeDataSources(monitoredHosts);
        unregisterDataSources(monitoredHosts);
        for (MonitoredHostDS monitoredHost : monitoredHosts) {
            try { monitoredHost.getMonitoredHost().removeHostListener(mapping.get(host)); } catch (MonitorException ex) {}
            mapping.remove(host);
        }
    }
    
    protected <Y extends MonitoredHostDS> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (MonitoredHostDS monitoredHost : removed) monitoredHost.finished();
    }
    
    
    void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
        DataSourceRepository.sharedInstance().addDataChangeListener(MonitoredHostProvider.this, Host.class);
    }

}
