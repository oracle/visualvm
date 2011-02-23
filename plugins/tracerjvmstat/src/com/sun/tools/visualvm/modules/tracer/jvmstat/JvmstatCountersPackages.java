/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.modules.tracer.jvmstat;

import com.sun.management.UnixOperatingSystemMXBean;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModelFactory;
import static com.sun.tools.visualvm.modules.tracer.jvmstat.JvmstatCounterFormatter.*;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.VmIdentifier;

/**
 *
 * @author Tomas Hurka
 */
class JvmstatCountersPackages  {
    private static final Logger LOGGER = Logger.getLogger(JvmstatCountersPackage.class.getName());
    private static final String Variability_INVALID = "Invalid";    // NOI18N
    private static final String Variability_CONSTANT = "Constant";  // NOI18N

    private Application application;
    private MonitoredVm monitoredVm;
    
    JvmstatCountersPackages(Application app) {
        application = app;
        monitoredVm = getMonitoredHost();
    }

    TracerPackage<Application>[] getPackages() {
        if (monitoredVm != null) {
            return computePackages();
        }
        return new TracerPackage[0];        
    }
    
    private MonitoredVm getMonitoredHost() {
        JvmstatModel jvmstat = JvmstatModelFactory.getJvmstatFor(application);
        if (jvmstat != null) {
            String connectionId = jvmstat.getConnectionId();
            try {
                VmIdentifier vmId = new VmIdentifier(connectionId);
                MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(vmId);
                return monitoredHost.getMonitoredVm(vmId);
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            } catch (Exception ex) {
                // MonitoredHostProvider.getMonitoredVm can throw java.lang.Exception on Windows,
                // when opening shared memory file (java.lang.Exception: Could not open PerfMemory)
                LOGGER.log(Level.INFO,"getMonitoredVm failed",ex);  // NOI18N
            }
        }
        return null;
    }
    
    private TracerPackage<Application>[] computePackages() {
        List counters;
        Iterator it;
        Map<String,JvmstatCountersPackage> packages = new HashMap();
        
        try {
            counters = monitoredVm.findByPattern(".*");
            Collections.sort(counters,new Comparator<Monitor>() {
                public int compare(Monitor o1, Monitor o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            it = counters.iterator();
            for (int i=0;it.hasNext();i++) {
                Monitor monitor = (Monitor) it.next();
                String unitsName = Utils.getUnits(monitor).toString();
                String var = Utils.getVariability(monitor).toString();
                String name = monitor.getName();
                String baseName = monitor.getBaseName();
                
                if (unitsName.equals(Units_STRING) || unitsName.equals(Units_INVALID) || unitsName.equals(Units_NONE)) {
                    continue;
                }
                if (var.equals(Variability_INVALID) || var.equals(Variability_CONSTANT)) {
                    continue;
                }
                if (monitor.isVector()) {
                    continue;
                }
                getPackage(packages,monitor,i,name);
             }
        } catch (MonitorException ex) {
            LOGGER.log(Level.INFO,"findByPattern failed",ex);  // NOI18N
        }
        return packages.values().toArray(new TracerPackage[0]);
    }

    private void getPackage(Map<String,JvmstatCountersPackage> packages, Monitor monitor, int pos, String name) {
        String pckName;
        String probeName;
        JvmstatCountersPackage pck;
        int dots = 0;
        int i;
        
        for (i = 0; i< name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '.') {
                dots++;
                if (dots == 2) {
                    break;
                }
            }    
        }
        if (dots == 2) {
            pckName = name.substring(0,i);
            probeName = name.substring(i+1);
            pck = packages.get(pckName);
            if (pck == null) {
                pck = new JvmstatCountersPackage(this,pckName,packages.size());
                packages.put(pckName,pck);
            }
            pck.addProbe(monitor,pos,probeName);
        }
    }

    void setInterval(int refresh) {
        monitoredVm.setInterval(refresh);
//        System.out.println("New refresh "+refresh);
    }

}
