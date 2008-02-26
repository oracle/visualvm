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

import com.sun.tools.visualvm.core.host.MonitoredHostDS;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
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
import com.sun.tools.visualvm.core.ui.DesktopUtils;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * A provider for Applications discovered by jvmstat.
 *
 * @author Jiri Sedlacek
 */
class JvmstatApplicationProvider extends DefaultDataSourceProvider<JvmstatApplication> implements DataChangeListener<MonitoredHostDS> {
    
    private final Map<Integer, JvmstatApplication> applications = new HashMap();
    
    private final Map<MonitoredHostDS, HostListener> mapping = Collections.synchronizedMap(new HashMap());
    
    private final DataFinishedListener<MonitoredHostDS> hostFinishedListener = new DataFinishedListener<MonitoredHostDS>() {
        public void dataFinished(MonitoredHostDS host) { processFinishedHost(host); }
    };
    
    
    // Not to be called from user code, use Application.CURRENT_APPLICATION instead
    synchronized Application getCurrentApplication() {    
        String selfName = ManagementFactory.getRuntimeMXBean().getName();
        Set<JvmstatApplication> localApplications = Host.LOCALHOST.getRepository().getDataSources(JvmstatApplication.class);
        for (JvmstatApplication localApplication : localApplications)
            if (localApplication.getState() == DataSource.STATE_AVAILABLE) {
                String pid = Integer.toString(localApplication.getPid());
                if (selfName.startsWith(pid.concat("@"))) return localApplication;
            }
        return null;
    }
    
    
    public void dataChanged(DataChangeEvent<MonitoredHostDS> event) {
        Set<MonitoredHostDS> newHosts = event.getAdded();
        for (MonitoredHostDS host : newHosts) processNewHost(host);
    }
    
//    TODO: check that applications are not removed twice from the host, unregister MonitoredHostListener!!!
    
    private void processNewHost(final MonitoredHostDS hostDs) {
        
        // Get the MonitoredHost for Host
        final MonitoredHost monitoredHost = hostDs.getMonitoredHost();
        final Host host = hostDs.getHost();
        
        if (host == Host.LOCALHOST && Platform.isWindows()) checkForBrokenJps(monitoredHost);

        // Flag for determining first MonitoredHost event
        final boolean firstEvent[] = new boolean[] { true };
        
        // Monitor the Host for new/finished Applications
        // NOTE: the code relies on the fact that the provider is the first listener registered in MonitoredHost of the Host
        // in which case the first obtained event contains all applications already running on the Host
        HostListener hostListener = null;
        try {
            // Fetch already running applications on the host
            processNewApplicationsByIds(host, hostDs, monitoredHost.activeVms());
            
            hostListener = new HostListener() {

                public void vmStatusChanged(final VmStatusChangeEvent e) {
//                    RequestProcessor.getDefault().post(new Runnable() {
//                        public void run() {
                            if (firstEvent[0]) {
                                // First event for this Host
                                // NOTE: already existing applications are treated as new on this host
                                firstEvent[0] = false;
                                processNewApplicationsByIds(host, hostDs, e.getActive());
                            } else {
                                processNewApplicationsByIds(host, hostDs, e.getStarted());
                                processTerminatedApplicationsByIds(host, e.getTerminated());
                            }
//                        }
//                    });
                }

                public void disconnected(HostEvent e) {}
                
            };
            monitoredHost.addHostListener(hostListener);
        } catch (MonitorException e) {
            System.err.println("[" + this.getClass().getName() + "] " + "Unable to monitor host " + hostDs.getHost().getHostName());
            e.printStackTrace();
        }
        
        if (hostListener != null) {
            mapping.put(hostDs, hostListener);
            hostDs.notifyWhenFinished(hostFinishedListener);
        }
    }
    
    private void processFinishedHost(MonitoredHostDS host) {
        HostListener hostListener = mapping.get(host);
        mapping.remove(host);
        try { host.getMonitoredHost().removeHostListener(hostListener); } catch (MonitorException ex) {}
        processAllTerminatedApplications(host.getHost());
    }
    
    private void processNewApplicationsByIds(Host host, MonitoredHostDS monitoredHost, Set<Integer> applicationIds) {
        Set<JvmstatApplication> newApplications = new HashSet();
        
        for (int applicationId : applicationIds)
            if (!applications.containsKey(applicationId)) {
                JvmstatApplication application = new JvmstatApplication(host, monitoredHost, applicationId);
                applications.put(applicationId, application);
                newApplications.add(application);
            }
        
        host.getRepository().addDataSources(newApplications);
        registerDataSources(newApplications);
    }
    
    private void processTerminatedApplicationsByIds(Host host, Set<Integer> applicationIds) {
        Set<JvmstatApplication> finishedApplications = new HashSet();
        
        for (int applicationId : applicationIds)
            if (applications.containsKey(applicationId)) {
                JvmstatApplication application = applications.get(applicationId);
                applications.remove(applicationId);
                finishedApplications.add(application);
            }
        
        host.getRepository().removeDataSources(finishedApplications);
        unregisterDataSources(finishedApplications);
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
        unregisterDataSources(finishedApplications);
    }
    
    protected <Y extends JvmstatApplication> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (JvmstatApplication application : removed) application.removed();
    }
    
    
    // Checks broken jps according to http://www.netbeans.org/issues/show_bug.cgi?id=115490
      private void checkForBrokenJps(MonitoredHost monitoredHost) {
          try { if (monitoredHost.activeVms().size() != 0) return; } catch (Exception e) { return; }
          
          String message = DesktopUtils.isBrowseAvailable() ? "<html><b>Local Java applications cannot be detected.</b><br><br>" +
                    "Please see the Troubleshooting guide for VisualVM for more<br>" +
                    "information and steps to fix the problem.<br><br>" +
                    "<a href=\"https://visualvm.dev.java.net/troubleshooting.html#jpswin\">https://visualvm.dev.java.net/troubleshooting.html#jpswin</a></html>" :
                "<html><b>Local applications cannot be detected.</b><br><br>" +
                    "Please see the Troubleshooting guide for VisualVM for more<br>" +
                    "information and steps to fix the problem.<br><br>" +
                    "<nobr>https://visualvm.dev.java.net/troubleshooting.html#jpswin</nobr></html>";
          final HTMLLabel label = new HTMLLabel(message) {
              protected void showURL(URL url) {
                 try { DesktopUtils.browse(url.toURI()); } catch (Exception e) {}
             }
          };
          
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                  NotifyDescriptor nd = new NotifyDescriptor.Message(label, NotifyDescriptor.ERROR_MESSAGE);
                  DialogDisplayer.getDefault().notify(nd);
              }
          });
      }
    
    
    void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
        DataSourceRepository.sharedInstance().addDataChangeListener(JvmstatApplicationProvider.this, MonitoredHostDS.class);
    }

}
