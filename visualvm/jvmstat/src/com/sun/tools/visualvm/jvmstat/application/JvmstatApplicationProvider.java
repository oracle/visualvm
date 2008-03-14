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

package com.sun.tools.visualvm.jvmstat.application;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DesktopUtils;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.util.RequestProcessor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.event.HostEvent;
import sun.jvmstat.monitor.event.HostListener;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;



/**
 * A provider for Applications discovered by jvmstat.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class JvmstatApplicationProvider implements DataChangeListener<Host> {
    
    private final Map<Integer, JvmstatApplication> applications = new HashMap();
    
    private final Map<Host, HostListener> mapping = Collections.synchronizedMap(new HashMap());
    
    private final DataRemovedListener<Host> hostFinishedListener = new DataRemovedListener<Host>() {
        public void dataRemoved(Host host) { processFinishedHost(host); }
    };
    
    public void dataChanged(DataChangeEvent<Host> event) {
        Set<Host> newHosts = event.getAdded();
        for (Host host : newHosts) processNewHost(host);
    }
    
    //    TODO: check that applications are not removed twice from the host, unregister MonitoredHostListener!!!
    
    private boolean processNewHost(final Host host) {
        if (host == Host.UNKNOWN_HOST) return true;
            
        // Flag for determining first MonitoredHost event
        final boolean firstEvent[] = new boolean[] { true };
        
        // Monitor the Host for new/finished Applications
        // NOTE: the code relies on the fact that the provider is the first listener registered in MonitoredHost of the Host
        // in which case the first obtained event contains all applications already running on the Host
        HostListener hostListener = null;
        
        // Get the MonitoredHost for Host
        final MonitoredHost monitoredHost = getMonitoredHost(host);
        
        if (monitoredHost == null) { // monitored host not available reshedule
            rescheduleProcessNewHost(host);
            return false;
        }
        monitoredHost.setInterval(GlobalPreferences.sharedInstance().getMonitoredHostPoll() * 1000);
        if (host == Host.LOCALHOST) checkForBrokenJps(monitoredHost);
        try {
            // Fetch already running applications on the host
            processNewApplicationsByIds(host, monitoredHost.activeVms());
            
            hostListener = new HostListener() {
                
                public void vmStatusChanged(final VmStatusChangeEvent e) {
                    if (firstEvent[0]) {
                        // First event for this Host
                        // NOTE: already existing applications are treated as new on this host
                        firstEvent[0] = false;
                        processNewApplicationsByIds(host, e.getActive());
                    } else {
                        processNewApplicationsByIds(host, e.getStarted());
                        processTerminatedApplicationsByIds(host, e.getTerminated());
                    }
                }
                
                public void disconnected(HostEvent e) {
                    rescheduleProcessNewHost(host);
                }
            };
            monitoredHost.addHostListener(hostListener);
            mapping.put(host, hostListener);
        } catch (MonitorException e) {
            ErrorManager.getDefault().notify(ErrorManager.USER,e);
            return false;
        }
        return true;
    }
    
    private void processFinishedHost(Host host) {
        HostListener hostListener = mapping.get(host);
        mapping.remove(host);
        try { getMonitoredHost(host).removeHostListener(hostListener); } catch (MonitorException ex) {}
        processAllTerminatedApplications(host);
    }
    
    private void processNewApplicationsByIds(Host host, Set<Integer> applicationIds) {
        Set<JvmstatApplication> newApplications = new HashSet();
        
        for (int applicationId : applicationIds) {
            // Do not provide instance for Application.CURRENT_APPLICATION
            if (Application.CURRENT_APPLICATION.getPid() == applicationId) continue;
            
            if (!applications.containsKey(applicationId)) {
                JvmstatApplication application = new JvmstatApplication(host, applicationId);
                applications.put(applicationId, application);
                newApplications.add(application);
            }
        }
        
        host.getRepository().addDataSources(newApplications);
    }
    
    private void processTerminatedApplicationsByIds(Host host, Set<Integer> applicationIds) {
        Set<JvmstatApplication> finishedApplications = new HashSet();
        
        for (int applicationId : applicationIds) {
            // Do not remove instance for Application.CURRENT_APPLICATION
            if (Application.CURRENT_APPLICATION.getPid() == applicationId) continue;
            
            if (applications.containsKey(applicationId)) {
                JvmstatApplication application = applications.get(applicationId);
                applications.remove(applicationId);
                finishedApplications.add(application);
            }
        }
        
        host.getRepository().removeDataSources(finishedApplications);
//        unregisterDataSources(finishedApplications);
    }
    
    private void processAllTerminatedApplications(Host host) {
        Set<JvmstatApplication> applicationsSet = host.getRepository().getDataSources(JvmstatApplication.class);
        Set<JvmstatApplication> finishedApplications = new HashSet();
        
        for (JvmstatApplication application : applicationsSet)
            if (applications.containsKey(application.getPid())) {
                applications.remove(application.getPid());
                finishedApplications.add(application);
            }
        
        host.getRepository().removeDataSources(finishedApplications);
//        unregisterDataSources(finishedApplications);
    }
    
//    protected <Y extends JvmstatApplication> void unregisterDataSources(final Set<Y> removed) {
//        for (JvmstatApplication application : removed) application.removed();
//    }
    
    // Checks broken jps according to http://www.netbeans.org/issues/show_bug.cgi?id=115490
    private void checkForBrokenJps(MonitoredHost monitoredHost) {
        try {
            if (monitoredHost.activeVms().size() != 0) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        
        String message = DesktopUtils.isBrowseAvailable() ? "<html><b>Local Java applications cannot be detected.</b><br><br>" +
                "Please see the Troubleshooting guide for VisualVM for more<br>" +
                "information and steps to fix the problem.<br><br>" +
                "<a href=\"https://visualvm.dev.java.net/troubleshooting.html#jpswin\">https://visualvm.dev.java.net/troubleshooting.html#jpswin</a></html>"
                :
            "<html><b>Local applications cannot be detected.</b><br><br>" +
            "Please see the Troubleshooting guide for VisualVM for more<br>" +
            "information and steps to fix the problem.<br><br>" +
            "<nobr>https://visualvm.dev.java.net/troubleshooting.html#jpswin</nobr></html>";
        final HTMLLabel label = new HTMLLabel(message) {
            protected void showURL(URL url) {
                try {
                    DesktopUtils.browse(url.toURI());
                } catch (Exception e) {
                }
            }
        };
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                NotifyDescriptor nd = new NotifyDescriptor.Message(label, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            }
        });
    }
    
    private MonitoredHost getMonitoredHost(Host host) {
        try {
            return MonitoredHost.getMonitoredHost(host.getHostName());
        } catch (URISyntaxException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        } catch (MonitorException ex) {
            ErrorManager.getDefault().log(ErrorManager.WARNING,ex.getLocalizedMessage());
        }
        return null;
    }
    
    private void rescheduleProcessNewHost(final Host host) {
        int interval = GlobalPreferences.sharedInstance().getMonitoredHostPoll();
        Timer timer = new Timer(interval*1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        if (host.isVisible()) processNewHost(host);
                    }
                });
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    public static void register() {
        JvmstatApplicationProvider provider = new JvmstatApplicationProvider();
        DataSourceRepository.sharedInstance().addDataChangeListener(provider, Host.class);
    }

}
