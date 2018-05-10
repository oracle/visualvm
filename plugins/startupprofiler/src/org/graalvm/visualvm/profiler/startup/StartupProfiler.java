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

import org.graalvm.visualvm.profiler.ProfilerSettingsSupport;
import org.graalvm.visualvm.profiler.ProfilerSupport;
import org.graalvm.visualvm.profiling.presets.ProfilerPreset;
import java.awt.Dialog;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "CAP_ProfileStartup=Profile Startup",
    "MSG_AnotherSessionRunning=<html><b>Another profiling session in progress.</b><br><br>Please finish profiling of {0}<br>before starting a new profiling session.</html>"
})
final class StartupProfiler {
    
    private static StartupProfiler sharedInstance;
    
    private StartupConfigurator configurator;

    static synchronized StartupProfiler sharedInstance() {
        if (sharedInstance == null) sharedInstance = new StartupProfiler();
        return sharedInstance;
    }
    
    
    void profileStartup() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String profiledApp = ProfilerSupport.getInstance().getProfiledApplicationName();
                if (profiledApp != null) {
                    Dialogs.show(Dialogs.warning(Bundle.CAP_ProfileStartup(),
                            Bundle.MSG_AnotherSessionRunning(profiledApp)));
                    return;
                }

                if (configurator == null) configurator = new StartupConfigurator();

                Dialog d = Dialogs.dialog(Bundle.CAP_ProfileStartup(), configurator.getUI());
                d.pack();
                Dialogs.show(d);

                if (configurator.accepted()) attachToProcess();
            }
        });
    }
    
    
    private void attachToProcess() {
        final int port = configurator.getPort();
        final String java = configurator.getJavaPlatform();
        final int architecture = configurator.getArchitecture();
        
        final ProfilerSettingsSupport settings = configurator.getSettings();
        final ProfilerPreset preset = configurator.getPreset();
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProfilerSupport.getInstance().profileProcessStartup(java, architecture, port,
                                                                    settings, preset);
            }
        });
    }

}
