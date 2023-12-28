/*
 * Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jvmstat;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.jvmstat.application.JvmstatApplicationProvider;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

/**
 *
 * @author Tomas Hurka
 */
public class JvmstatModelProvider extends AbstractModelProvider<JvmstatModel, Application> {
    private final static Logger LOGGER = Logger.getLogger(JvmstatModelProvider.class.getName());
    
    static MonitoredVm getMonitoredVm(Application app) throws MonitorException {
        if (app.isRemoved() || app.getPid() == Application.UNKNOWN_PID) return null;
        
        String vmId = "//" + app.getPid();  // NOI18N
        try {
            MonitoredHost monitoredHost = JvmstatApplicationProvider.findMonitoredHost(app);
            if (monitoredHost != null) {
                int refreshInterval = GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000;
                return monitoredHost.getMonitoredVm(new VmIdentifier(vmId),refreshInterval);
            }
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
        } catch (Exception ex) { 
            // MonitoredHostProvider.getMonitoredVm can throw java.lang.Exception on Windows, 
            // when opening shared memory file (java.lang.Exception: Could not open PerfMemory)
            LOGGER.log(Level.INFO,"getMonitoredVm failed",ex);  // NOI18N
        }
        return null;
    }
    
    public JvmstatModel createModelFor(Application app) {
        MonitoredVm vm = null;
        try {
            vm = getMonitoredVm(app);
            if (vm != null) {
                Monitor vmEndTimeMon;
                long vmEndTime = 0;
                long oldVmEndTime = 0;
                do {
                    // check that the target VM is accessible
                    if (vm.findByName("java.property.java.vm.version") != null) {   // NOI18N
                        JvmstatModelImpl jvmstat = new JvmstatModelImpl(app,vm);
                        app.notifyWhenRemoved(jvmstat);
                        return jvmstat;
                    } else {
                        // maybe it is too early and VM is not fully initialized
                        LOGGER.log(Level.INFO, app.getId()+" java.property.java.vm.version is null"); // NOI18N
                        vmEndTimeMon = vm.findByName("sun.rt.createVmEndTime");     // NOI18N
                        if (vmEndTimeMon != null) {
                            LOGGER.log(Level.INFO, app.getId()+" "+vmEndTimeMon.getName()+" = "+vmEndTimeMon.getValue());   // NOI18N
                            oldVmEndTime = vmEndTime;
                            vmEndTime = ((Long)vmEndTimeMon.getValue()).longValue();
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            // ignore
                        }
                    }
                } while (vmEndTimeMon != null && oldVmEndTime == 0);
                LOGGER.log(Level.INFO, "sun.rt.createVmEndTime is null "+oldVmEndTime+" "+vmEndTime); // NOI18N
            }
        } catch (MonitorException ex) {
            LOGGER.log(Level.INFO, "Could not get MonitoredVM", ex); // NOI18N
        }
        if (vm != null) {
            vm.detach();
        }
        return null;
    }
    
}
