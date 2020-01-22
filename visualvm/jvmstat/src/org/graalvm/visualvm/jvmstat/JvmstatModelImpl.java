/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmstatListener;
import org.graalvm.visualvm.tools.jvmstat.MonitoredValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.graalvm.visualvm.core.VisualVM;
import org.openide.ErrorManager;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.MonitorStatusChangeEvent;
import sun.jvmstat.monitor.event.VmEvent;
import sun.jvmstat.monitor.event.VmListener;

/**
 *
 * @author Tomas Hurka
 */
public class JvmstatModelImpl extends JvmstatModel implements VmListener, DataRemovedListener<Application> {
    private static final String Variability_CONSTANT = "Constant";  // NOI18N
    Application application;
    MonitoredVm monitoredVm;
    Set<JvmstatListener> listeners;
    private Map<String,String> valueCache;
    private Integer pid;
    private MonitoredHost monitoredHost;

    JvmstatModelImpl(Application app,MonitoredVm vm) {
        application = app;
        pid = Integer.valueOf(vm.getVmIdentifier().getLocalVmId());
        monitoredVm = vm;
        valueCache = new HashMap();
        listeners = new HashSet();
    }
               
    public void addJvmstatListener(JvmstatListener l) {
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                initListeners();
            }
            listeners.add(l);
        }
    }
    
    public void removeJvmstatListener(JvmstatListener l) {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                listeners.remove(l);
                if (listeners.isEmpty()) {
                    disableListeners();
                }
            }
        }
    }
    
    public String findByName(String name) {
        String value = valueCache.get(name);
        if (value != null) return value;
        
        try {
            Monitor mon = monitoredVm.findByName(name);
            if (mon != null) {
                value = mon.getValue().toString();
                if (Utils.getVariability(mon).toString().equals(Variability_CONSTANT)) {
                    valueCache.put(name,value);
                }
            }
            return value;
        } catch (MonitorException ex) {
            ErrorManager.getDefault().notify(ErrorManager.WARNING,ex);
        }
        return null;
    }
    
    public MonitoredValue findMonitoredValueByName(String name) {
        try {
            Monitor mon = monitoredVm.findByName(name);
            if (mon != null) {
                return new MonitoredValueImpl(mon);
            }
        } catch (MonitorException ex) {
            ErrorManager.getDefault().notify(ErrorManager.WARNING,ex);
        }
        return null;
    }

    
    public List<String> findByPattern(String pattern) {
        try {
            List<Monitor> monitorList = monitoredVm.findByPattern(pattern);
            List<String> monitorStrList = new ArrayList<String>(monitorList.size());
            for (Monitor monitor : monitorList) {
                monitorStrList.add(monitor.getValue().toString());
            }
            return monitorStrList;
        } catch (MonitorException ex) {
            ErrorManager.getDefault().notify(ErrorManager.WARNING,ex);
        }
        return null;
    }
    
    public List<MonitoredValue> findMonitoredValueByPattern(String pattern) {
        try {
            List<Monitor> monitorList = monitoredVm.findByPattern(pattern);
            List<MonitoredValue> monitoredValueList = new ArrayList(monitorList.size());
            for (Monitor monitor : monitorList) {
                monitoredValueList.add(new MonitoredValueImpl(monitor));
            }
            return monitoredValueList;
        } catch (MonitorException ex) {
            ErrorManager.getDefault().notify(ErrorManager.WARNING,ex);
        }
        return null;  
    }
    
    void initListeners() {
        try {
            monitoredHost = MonitoredHost.getMonitoredHost(monitoredVm.getVmIdentifier());
            monitoredVm.addVmListener(this);
        } catch (MonitorException ex) {
            ErrorManager.getDefault().notify(ErrorManager.WARNING,ex);
        }
    }
    
    void disableListeners() {
        try {
            monitoredVm.removeVmListener(this);
        } catch (MonitorException ex) {
             ErrorManager.getDefault().notify(ErrorManager.WARNING,ex);
        }
        monitoredHost = null;
    }
        
    /**
     * Invoked when instrumentation objects are inserted into or removed
     * from the MonitoredVm.
     *
     * @param event the object describing the event.
     */
    public void monitorStatusChanged(MonitorStatusChangeEvent event) {
        
    }
    
    /**
     * Invoked when instrumentation objects are updated. This event is
     * generated at a fixed interval as determined by the polling rate
     * of the MonitoredVm that the VmListener is registered with.
     *
     * @param event the object describing the event.
     */
    public void monitorsUpdated(VmEvent event) {
        assert event.getMonitoredVm().equals(monitoredVm);
        try {
            // check that the application is still alive
            if (monitoredHost.activeVms().contains(pid)) {
                List<JvmstatListener> listenersCopy;
                synchronized  (listeners) {
                    listenersCopy = new ArrayList(listeners);
                }
                for (JvmstatListener listener : listenersCopy) {
                    listener.dataChanged(this);
                }
            } else { // application is not alive
                disableListeners();
                monitoredVm.detach();
            }
        } catch (MonitorException ex) {
             ErrorManager.getDefault().notify(ErrorManager.WARNING,ex);
             disableListeners();
             monitoredVm.detach();
        }
    }
    
    /**
     * Invoked when the connection to the MonitoredVm has disconnected
     * due to communication errors.
     *
     * @param event the object describing the event.
     */
    public void disconnected(VmEvent event) {
        ErrorManager.getDefault().log("Disconnect "+event.getMonitoredVm().getVmIdentifier());  // NOI18N
        disableListeners();
        monitoredVm.detach();
    }
    
    public void dataRemoved(Application dataSource) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                disableListeners();
                monitoredVm.detach();
            }
        });
    }

    public String getConnectionId() {
        return monitoredVm.getVmIdentifier().getURI().toString();
    }
    
}
