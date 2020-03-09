/*
 *  Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.profiler.startup;

import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.common.SessionSettings;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.profiler.NetBeansProfiler;
import org.graalvm.visualvm.profiler.ProfilerSettingsSupport;
import org.graalvm.visualvm.profiler.ProfilerSupport;
import org.graalvm.visualvm.profiling.presets.ProfilerPreset;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Hurka
 */
class ProfilerSupportOverlay {

    ProfilerSupport support;
    Method checkStartedApp;
    Method resetTerminateDialogs;
    Method createSessionSettings;
    Method isProfiledApplication;
    Method setProfiledApplication;
    Method selectProfilerView;
    Method checkCalibration;

    ProfilerSupportOverlay() {
        support = ProfilerSupport.getInstance();
        try {
            checkStartedApp = ProfilerSupport.class.getDeclaredMethod("checkStartedApp", int.class);
            checkStartedApp.setAccessible(true);
            resetTerminateDialogs = ProfilerSupport.class.getDeclaredMethod("resetTerminateDialogs");
            resetTerminateDialogs.setAccessible(true);
            createSessionSettings = ProfilerSupport.class.getDeclaredMethod("createSessionSettings", String.class, int.class, int.class);
            createSessionSettings.setAccessible(true);
            isProfiledApplication = ProfilerSupport.class.getDeclaredMethod("isProfiledApplication", Application.class, int.class);
            isProfiledApplication.setAccessible(true);
            setProfiledApplication = ProfilerSupport.class.getDeclaredMethod("setProfiledApplication", Application.class);
            setProfiledApplication.setAccessible(true);
            selectProfilerView = ProfilerSupport.class.getDeclaredMethod("selectProfilerView", Application.class, ProfilerPreset.class, ProfilingSettings.class);
            selectProfilerView.setAccessible(true);
            Class cs = Class.forName("org.graalvm.visualvm.profiler.CalibrationSupport");
            checkCalibration = cs.getDeclaredMethod("checkCalibration", String.class, int.class, Runnable.class, Runnable.class);
            checkCalibration.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    void profileProcessStartup(final String java, final int architecture, final int port,
            ProfilerSettingsSupport settings, final ProfilerPreset preset) {

        try {
            if (!checkCalibration(java, architecture, null, null)) {
                return;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

        final ProfilingSettings pSettings = settings.getSettings();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Perform the actual attach
                VisualVM.getInstance().runTask(new Runnable() {
                    public void run() {
                        try {
                            if (!checkStartedApp(port)) {
                                return;
                            }

                            final RequestProcessor processor = new RequestProcessor("Startup Profiler @ " + port); // NOI18N
                            Host.LOCALHOST.getRepository().addDataChangeListener(
                                    new DataChangeListener<Application>() {
                                public void dataChanged(final DataChangeEvent<Application> event) {
                                    final DataChangeListener listener = this;
                                    processor.post(new Runnable() {
                                        public void run() {
                                            try {
                                                if (!event.getAdded().equals(event.getCurrent())) { // filter-out initial sync event
                                                    for (Application a : event.getAdded()) {
                                                        if (isProfiledApplication(a, port)) {
                                                            Host.LOCALHOST.getRepository().removeDataChangeListener(listener);

                                                            setProfiledApplication(a);
                                                            selectProfilerView(a, preset, pSettings);

                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                Exceptions.printStackTrace(ex);
                                            }
                                        }
                                    });
                                }
                            }, Application.class);

                            ProfilingSettings ps = pSettings;
                            SessionSettings ss = createSessionSettings(CommonConstants.JDK_CVM_STRING, architecture, port);
                            NetBeansProfiler.getDefaultNB().connectToStartedApp(ps, ss);
                            ss.setJavaVersionString(java);
                            resetTerminateDialogs();
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                });
            }
        });
    }

    private boolean checkStartedApp(int port) throws Exception {
        return (Boolean) checkStartedApp.invoke(support, port);
    }

    private void resetTerminateDialogs() throws Exception {
        resetTerminateDialogs.invoke(support);
    }

    private SessionSettings createSessionSettings(String javaString, int architecture, int port) throws Exception {
        return (SessionSettings) createSessionSettings.invoke(support, javaString, architecture, port);
    }

    private boolean isProfiledApplication(Application app, int port) throws Exception {
        return (Boolean) isProfiledApplication.invoke(support, app, port);
    }

    private void setProfiledApplication(Application app) throws Exception {
        setProfiledApplication.invoke(support, app);
    }

    private void selectProfilerView(Application app, ProfilerPreset preset, ProfilingSettings pSettings) throws Exception {
        selectProfilerView.invoke(support, app, preset, pSettings);
    }

    private boolean checkCalibration(String java, int architecture, Runnable before, Runnable after) throws Exception {
        return (Boolean) checkCalibration.invoke(null, java, architecture, before, after);
    }

}
