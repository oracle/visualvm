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
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DesktopUtils;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collection;
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
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.VmIdentifier;
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
    private static final String HOST_ID_KEY = "jvmstat.hostid";
    
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
        
        Collection<Integer> ports = Arrays.asList(1099); // get ports from host
        
        for (Integer port : ports) {
            HostIdentifier hostId = getHostIdentifier(host,port);
            registerJvmstatConnection(host,hostId);
        }
    }
    
    private void processFinishedHost(final Host host) {
        if (host == Host.UNKNOWN_HOST) return;
        
        synchronized (hostsListeners) {
            Map<HostIdentifier,JvmstatConnection> hostListeners = hostsListeners.get(host);
            
            if (hostListeners != null) {
                for (JvmstatConnection listener : hostListeners.values()) {
                    processDisconnectedJvmstat(host, listener);
                }
            }
        }
    } 
    
    private void processDisconnectedJvmstat(Host host, JvmstatConnection listener) {
        HostIdentifier hostId = listener.hostId;
        MonitoredHost monitoredHost = getMonitoredHost(hostId);
        try { monitoredHost.removeHostListener(listener); } catch (MonitorException ex) {}
        removeHostListener(host,hostId);
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
        String hostIdString = hostId.getURI().toString();
        
        for (int applicationPid : applicationPids) {
            // Do not provide instance for Application.CURRENT_APPLICATION
            if (Application.CURRENT_APPLICATION.getPid() == applicationPid && Host.LOCALHOST.equals(host)) {
                Application.CURRENT_APPLICATION.getStorage().setCustomProperty(HOST_ID_KEY,hostIdString);
                continue;
            }
            
            String appId = createId(host, applicationPid);
            JvmstatApplication application = new JvmstatApplication(host, hostId, appId, applicationPid);
            if (!applications.containsKey(appId)) {
                // precompute JVM 
                application.jvm = JvmFactory.getJVMFor(application);
                application.getStorage().setCustomProperty(HOST_ID_KEY,hostIdString);
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
    
    private void addHostListener(Host host,HostIdentifier hostId,JvmstatConnection hostListener) {
        synchronized (hostsListeners) {
            Map<HostIdentifier,JvmstatConnection> hostListeners = hostsListeners.get(host);

            if (hostListeners == null) {
                hostListeners = new HashMap();
                hostsListeners.put(host,hostListeners);
            }
            hostListeners.put(hostId,hostListener);
        }
    }

    private void removeHostListener(Host host,HostIdentifier hostId) {
        synchronized (hostsListeners) {
            Map<HostIdentifier,JvmstatConnection> hostListeners = hostsListeners.get(host);

            assert hostListeners != null;
            hostListeners.remove(hostId);
        }
    }
    
    private void registerJvmstatConnection(Host host, HostIdentifier hostId) {
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
        monitoredHost.setInterval(GlobalPreferences.sharedInstance().getMonitoredHostPoll() * 1000);
        if (host == Host.LOCALHOST) checkForBrokenJps(monitoredHost);
        try {
            // Fetch already running applications on the host
            processNewApplicationsByPids(host, monitoredHost.getHostIdentifier(), monitoredHost.activeVms());
            
            hostListener = new JvmstatConnection(host, monitoredHost);
            monitoredHost.addHostListener(hostListener);
            addHostListener(host,monitoredHost.getHostIdentifier(),hostListener);
        } catch (MonitorException e) {
            ErrorManager.getDefault().notify(ErrorManager.USER,e);
            rescheduleProcessNewHost(host,hostId);
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
    private void checkForBrokenJps(MonitoredHost monitoredHost) {
        try {
            if (monitoredHost.activeVms().size() != 0) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        String link = DesktopUtils.isBrowseAvailable() ? NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jsp_Link")   // NOI18N
                : NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jsp_NoLink");   // NOI18N
        
        String message = NbBundle.getMessage(JvmstatApplicationProvider.class, "MSG_Broken_Jps", link); // NOI18N
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
    
    private HostIdentifier getHostIdentifier(Host host, int port) {
        try {
            String hostIdString = host.getHostName();
            if (port != Registry.REGISTRY_PORT) {
                hostIdString +=":"+port;
            }
            return new HostIdentifier(hostIdString);
        } catch (URISyntaxException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
        return null;
    }
    
    private MonitoredHost getLocalMonitoredHost() {
        try {
            return MonitoredHost.getMonitoredHost("localhost"); // NOI18N
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private MonitoredHost getMonitoredHost(Host host, int port) {
        try {
            String hostIdString = host.getHostName();
            if (port != Registry.REGISTRY_PORT) {
                hostIdString +=":"+port;
            }
            return getMonitoredHost(new HostIdentifier(hostIdString));
        } catch (URISyntaxException ex) {
            // TODO: Host should't be scheduled for later MonitoredHost resolving if URISyntaxException
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
        return null;
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
        int interval = GlobalPreferences.sharedInstance().getMonitoredHostPoll();
        Timer timer = new Timer(interval*1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // do not block EQ - use request processor, processNewHost() can take a long time
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        if (!host.isRemoved()) registerJvmstatConnection(host,hostId);
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

    private class JvmstatConnection implements HostListener {

        // Flag for determining first MonitoredHost event
        private boolean firstEvent = true;
        private Host host;
        private HostIdentifier hostId;

        private JvmstatConnection(Host host, MonitoredHost monitoredHost) {
            this.firstEvent = firstEvent;
            this.host = host;
            hostId = monitoredHost.getHostIdentifier();
        }

        public void vmStatusChanged(final VmStatusChangeEvent e) {            
            if (firstEvent) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Monitored Host (" + host.getHostName() + ") status changed - adding all active applications");
                }
                firstEvent = false;
                processNewApplicationsByPids(host, hostId, e.getActive());
            } else {
                processNewApplicationsByPids(host, hostId, e.getStarted());
                processTerminatedApplicationsByPids(host, e.getTerminated());
            }
        }

        public void disconnected(HostEvent e) {
            processDisconnectedJvmstat(host, this);
            rescheduleProcessNewHost(host,hostId);
        }
    }

}
