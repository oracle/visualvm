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
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DesktopUtils;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.rmi.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.HostIdentifier;
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
    private static final Logger LOGGER = Logger.getLogger(JvmstatApplicationProvider.class.getName());
    
    private static JvmstatApplicationProvider instance;
    
    private final Map<String, JvmstatApplication> applications = new HashMap();
    private final Map<Host,Map<HostIdentifier,JvmstatConnection>> hostsListeners = new HashMap();
    
    static synchronized JvmstatApplicationProvider sharedInstance() {
        if (instance == null) {
            instance = new JvmstatApplicationProvider();
        }
        return instance;
    }
    
    public void dataChanged(DataChangeEvent<Host> event) {
        Set<Host> newHosts = event.getAdded();
        for (final Host host : newHosts) {
            // run new host in request processor, since it will take
            // a long time that there is no jstatd running on new host
            // we do not want to block DataSource.EVENT_QUEUE for long time
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    processNewHost(host);
                }
            });
        }
        Set<Host> removedHosts = event.getRemoved();
        for (final Host host : removedHosts) {
            // run removed host in request processor, since it can take
            // a long time and we do not want to block DataSource.EVENT_QUEUE
            // for long time
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    processFinishedHost(host);
                }
            });
        }
    }
    
    private void processNewHost(final Host host) {
        if (host == Host.UNKNOWN_HOST) return;
        
        Set<ConnectionDescriptor> descrs = HostPropertiesProvider.descriptorsForHost(host);
        registerJvmstatConnections(host, descrs);
    }
    
    private void registerJvmstatConnections(final Host host, final Set<ConnectionDescriptor> descrs) {
        for (ConnectionDescriptor desc : descrs) {
            int interval = (int)(desc.getRefreshRate()*1000);
            HostIdentifier hostId = desc.createHostIdentifier(host);
            registerJvmstatConnection(host,hostId,interval);
        }
    }
    
    private void processChangedJvmstatConnection(Host host, ConnectionDescriptor changedConnection) {
        HostIdentifier hostId = changedConnection.createHostIdentifier(host);
        MonitoredHost monitoredHost = getMonitoredHost(hostId);
        if (monitoredHost != null) {
            int interval = (int)(changedConnection.getRefreshRate()*1000);
            monitoredHost.setInterval(interval);
        }
    }
    
    private void processFinishedHost(final Host host) {
        if (host == Host.UNKNOWN_HOST) return;
        
        synchronized (hostsListeners) {
            Map<HostIdentifier,JvmstatConnection> hostListeners = hostsListeners.get(host);
            
            if (hostListeners != null) {
                for (JvmstatConnection listener : new ArrayList<JvmstatConnection>(hostListeners.values())) {
                    processDisconnectedJvmstat(host, listener);
                }
            }
        }
    }
    
    private void processRemovedJvmstatConnection(final Host host, HostIdentifier hostId) {
        if (host == Host.UNKNOWN_HOST) return;
        
        synchronized (hostsListeners) {
            Map<HostIdentifier,JvmstatConnection> hostListeners = hostsListeners.get(host);
            
            if (hostListeners != null) {
                JvmstatConnection listener = hostListeners.get(hostId);
                if (listener != null) {
                    processDisconnectedJvmstat(host, listener);
                }
            }
        }
    }
    
    private void processDisconnectedJvmstat(Host host, JvmstatConnection listener) {
        HostIdentifier hostId = listener.monitoredHost.getHostIdentifier();
        try { listener.monitoredHost.removeHostListener(listener); } catch (MonitorException ex) {}
        unregisterHostListener(host,hostId);
        Set<JvmstatApplication> jvmstatApplications = host.getRepository().getDataSources(JvmstatApplication.class);
        Iterator<JvmstatApplication> appIt = jvmstatApplications.iterator();
        while (appIt.hasNext()) {
            JvmstatApplication application = appIt.next();
            
            if (application.getHostIdentifier().equals(hostId)) {
                application.setStateImpl(Stateful.STATE_UNAVAILABLE);
            } else {
                appIt.remove();
            }
        }
        host.getRepository().removeDataSources(jvmstatApplications);
    }
    
    private void processNewApplicationsByPids(Host host, HostIdentifier hostId, Set<Integer> applicationPids) {
        Set<JvmstatApplication> newApplications = new HashSet();
        
        for (int applicationPid : applicationPids) {
            // Do not provide instance for Application.CURRENT_APPLICATION
            if (Application.CURRENT_APPLICATION.getPid() == applicationPid && Host.LOCALHOST.equals(host)) {
                continue;
            }
            
            String appId = createId(host, applicationPid);
            JvmstatApplication application = new JvmstatApplication(host, hostId, appId, applicationPid);
            if (!applications.containsKey(appId)) {
                // precompute JVM
                application.jvm = JvmFactory.getJVMFor(application);
                applications.put(appId, application);
                newApplications.add(application);
            }
        }
        
        host.getRepository().addDataSources(newApplications);
    }
    
    private void processTerminatedApplicationsByPids(Host host, Set<Integer> applicationPids) {
        Set<JvmstatApplication> finishedApplications = new HashSet();
        
        for (int applicationPid : applicationPids) {
            String appId = createId(host, applicationPid);
            if (applications.containsKey(appId)) {
                JvmstatApplication application = applications.get(appId);
                if (application != null) {
                    finishedApplications.add(application);
                    application.setStateImpl(Stateful.STATE_UNAVAILABLE);
                }
                applications.remove(appId);
            }
        }
        
        host.getRepository().removeDataSources(finishedApplications);
    }
    
    private void registerHostListener(Host host,HostIdentifier hostId,JvmstatConnection hostListener) {
        synchronized (hostsListeners) {
            Map<HostIdentifier,JvmstatConnection> hostListeners = hostsListeners.get(host);
            
            if (hostListeners == null) {
                hostListeners = new HashMap();
                hostsListeners.put(host,hostListeners);
            }
            hostListeners.put(hostId,hostListener);
        }
    }
    
    private void unregisterHostListener(Host host,HostIdentifier hostId) {
        synchronized (hostsListeners) {
            Map<HostIdentifier,JvmstatConnection> hostListeners = hostsListeners.get(host);
            
            assert hostListeners != null;
            hostListeners.remove(hostId);
        }
    }
    
    private void registerJvmstatConnection(Host host, HostIdentifier hostId, int interval) {
        // Monitor the Host for new/finished Applications
        // NOTE: the code relies on the fact that the provider is the first listener registered in MonitoredHost of the Host
        // in which case the first obtained event contains all applications already running on the Host
        JvmstatConnection hostListener = null;
        
        // Get the MonitoredHost for Host
        final MonitoredHost monitoredHost = getMonitoredHost(hostId);
        
        if (monitoredHost == null) { // monitored host not available reschedule
            rescheduleProcessNewHost(host,hostId);
            return;
        }
        hostId = monitoredHost.getHostIdentifier();
        monitoredHost.setInterval(interval);
        if (host == Host.LOCALHOST) checkForBrokenLocalJps(monitoredHost);
        try {
            // Fetch already running applications on the host
            processNewApplicationsByPids(host, hostId, monitoredHost.activeVms());
            hostListener = new JvmstatConnection(host, monitoredHost);
            monitoredHost.addHostListener(hostListener);
            registerHostListener(host, hostId, hostListener);
        } catch (MonitorException e) {
            Throwable t = e.getCause();
            monitoredHost.setLastException(e);
            if (!(t instanceof ConnectException)) {
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                        NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jvmstat", // NOI18N
                        DataSourceDescriptorFactory.getDescriptor(host).getName()),
                        NotifyDescriptor.ERROR_MESSAGE));
                LOGGER.log(Level.INFO, "Jvmstat connection to " + host + " failed.", t); // NOI18N
            } else {
                rescheduleProcessNewHost(host,hostId);
            }
        }
    }
    
    private String createId(Host host, int pid) {
        return host.getHostName() + "-" + pid;
    }
    
    void removeFromMap(JvmstatApplication jvmstatApplication) {
        applications.remove(jvmstatApplication.getId());
    }
    
    // TODO: reimplement to listen for Host.getState() == STATE_UNAVAILABLE
    //    private void processAllTerminatedApplications(Host host) {
    //        Set<JvmstatApplication> applicationsSet = host.getRepository().getDataSources(JvmstatApplication.class);
    //        Set<JvmstatApplication> finishedApplications = new HashSet();
    //
    //        for (JvmstatApplication application : applicationsSet)
    //            if (applications.containsKey(application.getPid())) {
    //                applications.remove(application.getPid());
    //                finishedApplications.add(application);
    //            }
    //
    //        host.getRepository().removeDataSources(finishedApplications);
    //    }
    
    // Checks broken jps according to http://www.netbeans.org/issues/show_bug.cgi?id=115490
    // Checks broken jps according to https://visualvm.dev.java.net/issues/show_bug.cgi?id=311
    private void checkForBrokenLocalJps(MonitoredHost monitoredHost) {
        try {
            if (monitoredHost.activeVms().size() != 0) {

                if (Utilities.isWindows()) {
                    String perf = "hsperfdata_" + System.getProperty("user.name"); // NOI18N
                    File perfCorrect = new File(System.getProperty("java.io.tmpdir"), perf); // NOI18N
                    File perfCurrent = perfCorrect.getCanonicalFile(); // Resolves real capitalization
                    if (!perfCorrect.getName().equals(perfCurrent.getName())) {
                        String link = DesktopUtils.isBrowseAvailable() ? NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jps2_Link")   // NOI18N
                                : NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jsp2_NoLink");   // NOI18N
                        String message = NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jps2", link); // NOI18N
                        notifyBrokenJps(message);
                    }
                }

                return;
            }
        } catch (Exception e) {
            return;
        }

        String link = DesktopUtils.isBrowseAvailable() ? NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jps_Link")   // NOI18N
                : NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jsp_NoLink");   // NOI18N
        String message = NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jps", link); // NOI18N
        notifyBrokenJps(message);
    }

    private static void notifyBrokenJps(String message) {
        final HTMLLabel label = new HTMLLabel(message) {
            protected void showURL(URL url) {
                try {
                    DesktopUtils.browse(url.toURI());
                } catch (Exception e) {}
            }
        };

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                NotifyDescriptor nd = new NotifyDescriptor.Message(label, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            }
        });
    }
    
    private MonitoredHost getLocalMonitoredHost() {
        try {
            return MonitoredHost.getMonitoredHost("localhost"); // NOI18N
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private MonitoredHost getMonitoredHost(HostIdentifier hostId) {
        try {
            return MonitoredHost.getMonitoredHost(hostId);
        } catch (MonitorException ex) {
            // NOTE: valid state, jstatd not running, Host will be scheduled for later MonitoredHost resolving
            //            ErrorManager.getDefault().log(ErrorManager.WARNING,ex.getLocalizedMessage());
        }
        return null;
    }
    
    public MonitoredHost getMonitoredHost(Application app) {
        JvmstatApplication japp;
        
        if (Application.CURRENT_APPLICATION.equals(app)) {
            return getLocalMonitoredHost();
        }
        if (!(app instanceof JvmstatApplication)) {
            String appId = createId(app.getHost(),app.getPid());
            japp = applications.get(appId);
        } else {
            japp = (JvmstatApplication) app;
        }
        if (japp != null) {
            return getMonitoredHost(japp.getHostIdentifier());
        }
        return null;
    }
    
    private void rescheduleProcessNewHost(final Host host,final HostIdentifier hostId) {
        int timerInterval = GlobalPreferences.sharedInstance().getMonitoredHostPoll();
        Timer timer = new Timer(timerInterval*1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // do not block EQ - use request processor, processNewHost() can take a long time
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        if (!host.isRemoved()) {
                            Set<ConnectionDescriptor> descriptors = HostPropertiesProvider.descriptorsForHost(host);
                            
                            for (ConnectionDescriptor desc : descriptors) {
                                if (hostId.equals(desc.createHostIdentifier(host))) {
                                    int interval = (int)(desc.getRefreshRate()*1000);
                                    registerJvmstatConnection(host,hostId,interval);
                                }
                            }
                        }
                    }
                });
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    public static void register() {
        DataSourceRepository.sharedInstance().addDataChangeListener(sharedInstance(), Host.class);
    }
    
    public static MonitoredHost findMonitoredHost(Application app) {
        return sharedInstance().getMonitoredHost(app);
    }
    
    // Invoked from AWT thread
    void connectionsChanged(final Host host, final Set<ConnectionDescriptor> added, final Set<ConnectionDescriptor> removed, final Set<ConnectionDescriptor> changed) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                registerJvmstatConnections(host,added);
                for (ConnectionDescriptor removedConnection : removed) {
                    processRemovedJvmstatConnection(host, removedConnection.createHostIdentifier(host));
                }
                for (ConnectionDescriptor changedConnection : changed) {
                    processChangedJvmstatConnection(host, changedConnection);
                }
            }
        });
    }
    
    private class JvmstatConnection implements HostListener {
        
        // Flag for determining first MonitoredHost event
        private boolean firstEvent = true;
        private Host host;
        private MonitoredHost monitoredHost;
        
        private JvmstatConnection(Host host, MonitoredHost mHost) {
            this.host = host;
            monitoredHost = mHost;
        }
        
        public void vmStatusChanged(final VmStatusChangeEvent e) {
            if (firstEvent) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Monitored Host (" + host.getHostName() + ") status changed - adding all active applications");
                }
                firstEvent = false;
                processNewApplicationsByPids(host, monitoredHost.getHostIdentifier(), e.getActive());
            } else {
                processNewApplicationsByPids(host, monitoredHost.getHostIdentifier(), e.getStarted());
                processTerminatedApplicationsByPids(host, e.getTerminated());
            }
        }
        
        public void disconnected(HostEvent e) {
            processDisconnectedJvmstat(host, this);
            rescheduleProcessNewHost(host,monitoredHost.getHostIdentifier());
        }
    }
    
}
