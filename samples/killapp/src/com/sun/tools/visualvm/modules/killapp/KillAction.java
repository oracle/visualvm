/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.killapp;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.MultiDataSourceAction;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
public final class KillAction extends MultiDataSourceAction<Application> {
    
    private final Set<Application> lastSelectedApplications;
    private final RequestProcessor killRP;
    private final PropertyChangeListener stateListener;

    @NbBundle.Messages({"CTL_KillAction=Kill Application"})
    public KillAction() {
        super(Application.class);
        putValue(NAME, Bundle.CTL_KillAction());
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
        lastSelectedApplications = new HashSet();
        killRP = new RequestProcessor("KillAction processor", 5);   // NOI18N
        stateListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateState(ActionUtils.getSelectedDataSources(Application.class));
            }
        };
    }

    @Override
    protected void actionPerformed(Set<Application> dataSources, ActionEvent ae) {
        for (Application dataSource : dataSources) {
            killApplication((Application)dataSource);
        }
    }

    @Override
    protected boolean isEnabled(Set<Application> dataSources) {
        for (DataSource dataSource : dataSources) {
            Application application = (Application)dataSource;
                lastSelectedApplications.add(application);
            application.addPropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
            if (application.getState() != Stateful.STATE_AVAILABLE) return false;
            if (!isEnabled(application)) return false;
        }
        return true;
    }

    @Override
    protected void updateState(Set<Application> dataSources) {
        if (!lastSelectedApplications.isEmpty())
            for (Application application : lastSelectedApplications)
                application.removePropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
        lastSelectedApplications.clear();
        super.updateState(dataSources);
    }

    private void killApplication(final Application app) {
        final String pidString = String.valueOf(app.getPid());
        final String[] command = getCommand(pidString, false);

        if (command == null) {
            return;
        }

        final Progress handle = new Progress(pidString);
        app.notifyWhenRemoved(handle);
        killRP.post(new Runnable() {
            public void run() {
                try {
                    Runtime.getRuntime().exec(command);
                    Thread.sleep(5000);
                    if (app.getState() == Stateful.STATE_AVAILABLE) {
                        // application is still alive, try to kill it hard way
                        Runtime.getRuntime().exec(getCommand(pidString, true));
                        refreshJvms();
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    handle.finish();
                }
            }
        });
    }

    private void refreshJvms() throws IOException {
        String javaSub = Utilities.isWindows() ? "bin\\java.exe" : "bin/java"; // NOI18N
        File java = new File(System.getProperty("java.home"), javaSub); // NOI18N

        if (java.isFile()) {
            String command[] = {java.getAbsolutePath(), "-version"};
            Runtime.getRuntime().exec(command);
        }
    }

    private boolean isEnabled(Application application) {
        if (Application.CURRENT_APPLICATION.equals(application)) {
            // don't commit suicide
            return false;
        }
        if (!Host.LOCALHOST.equals(application.getHost())) {
            // we cannot kill remote applications
            return false;
        }
        return true;
    }

    private String[] getCommand(String pidString, boolean force) {
        if (Utilities.isWindows()) {
            if (force) {
                return new String[]{"taskkill", "/F", "/PID", pidString};    // NOI18N                
            } else {
                return new String[]{"taskkill", "/PID", pidString};    // NOI18N
            }
        } else if (Utilities.isUnix()) {
            if (force) {
                return new String[]{"kill", "-9", pidString};   // NOI18N                
            } else {
                return new String[]{"kill", pidString};   // NOI18N
            }
        } else {
            assert false : "strange os";  // NOI18N
            return null;
        }
    }

    @NbBundle.Messages({"MSG_Kill=Killing application with PID {0}"})
    private static class Progress implements DataRemovedListener<Application>{

        ProgressHandle handle;
        boolean running;
        
        private Progress(String pid) {
            handle = ProgressHandleFactory.createHandle(Bundle.MSG_Kill(pid));
            handle.setInitialDelay(500);
            handle.start();
            running = true;
        }

        private synchronized void finish() {
            if (running) {
                running = false;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        handle.finish();
                        handle = null;
                    }
                });
            }
        }

        public void dataRemoved(Application x) {
            finish();
        }
    }
}
