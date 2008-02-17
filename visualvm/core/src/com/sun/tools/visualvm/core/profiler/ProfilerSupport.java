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

package com.sun.tools.visualvm.core.profiler;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.util.Set;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerIDESettings;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerSupport {

    private static ProfilerSupport instance;
    
    private Application profiledApplication;
    private ApplicationProfilerViewProvider profilerViewProvider;


    public static synchronized ProfilerSupport getInstance() {
        if (instance == null) instance = new ProfilerSupport();
        return instance;
    }
    
    
    void setProfiledApplication(Application profiledApplication) {
        this.profiledApplication = profiledApplication;
    }
  
    Application getProfiledApplication() {
        return profiledApplication;
    }
    
    void selectActiveProfilerView() {
        if (profiledApplication == null) return;
        Set<? extends DataSourceView> activeViewSet = profilerViewProvider.getViews(profiledApplication);
        if (activeViewSet.isEmpty()) return;
        DataSourceWindowManager.sharedInstance().addView(profiledApplication, activeViewSet.iterator().next());
    }
    
    
    // TODO: move to JVM?
    private static String getJDKExecutable() {
        String jdkPath = System.getProperty("java.home"); // NOI18N
        if (jdkPath == null || jdkPath.trim().length() == 0) return null;
        String jreSuffix = File.separator + "jre"; // NOI18N
        if (jdkPath.endsWith(jreSuffix)) jdkPath = jdkPath.substring(0, jdkPath.length() - jreSuffix.length());
        String jdkExe = jdkPath + File.separator + "bin" + File.separator + "java" + (Platform.isWindows() ? ".exe" : ""); // NOI18N
        return jdkExe;
    }

    private boolean checkCalibration() {
        // Get ProfilerEngineSettings instance
        org.netbeans.lib.profiler.ProfilerEngineSettings pes = NetBeansProfiler.getDefaultNB().getTargetAppRunner().getProfilerEngineSettings();

        // Save current state
        int savedPort = pes.getPortNo();
        org.netbeans.lib.profiler.global.InstrumentationFilter savedInstrFilter = pes.getInstrumentationFilter();
        String savedJVMExeFile = pes.getTargetJVMExeFile();
        String savedJDKVersionString = pes.getTargetJDKVersionString();
        int savedArch = pes.getSystemArchitecture();
        String savedCP = pes.getMainClassPath();

        // Set JVM properties
        String jvmExecutable = getJDKExecutable();
        if (jvmExecutable == null) return false;
        String jdkString = Platform.getJDKVersionString();
        int architecture = Platform.getSystemArchitecture();

        boolean result = true;

        // Setup ProfilerEngineSettings
        pes.setTargetJVMExeFile(jvmExecutable);
        pes.setTargetJDKVersionString(jdkString);
        pes.setSystemArchitecture(architecture);
        pes.setPortNo(ProfilerIDESettings.getInstance().getCalibrationPortNo());
        pes.setInstrumentationFilter(new org.netbeans.lib.profiler.global.InstrumentationFilter());
        pes.setMainClassPath(""); //NOI18N

        // Perform calibration if necessary
        if (!NetBeansProfiler.getDefaultNB().getTargetAppRunner().readSavedCalibrationData()) result = calibrateJVM();

        // Restore original ProfilerEngineSettings
        pes.setPortNo(savedPort);
        pes.setInstrumentationFilter(savedInstrFilter);
        pes.setTargetJDKVersionString(savedJDKVersionString);
        pes.setSystemArchitecture(savedArch);
        pes.setTargetJVMExeFile(savedJVMExeFile);
        pes.setMainClassPath(savedCP);

        return result;
    }

    private boolean calibrateJVM() {
        // TODO: this should be performed after all modules are loaded & initialized to not bias the calibration!!!
        
        // Display blocking notification
        NetBeansProfiler.getDefaultNB().displayInfoAndWait("<html><b>Calibration will be performed.</b><br><br>Profiler will perform initial JDK calibration. Please make sure that other applications<br>are not placing a noticeable load on your machine at this time and click the OK button.</html>");

        // Perform calibration
        boolean result = NetBeansProfiler.getDefaultNB().runConfiguredCalibration();

        return result;
    }
    
    
    private ProfilerSupport() {
        profilerViewProvider = new ApplicationProfilerViewProvider();
        profilerViewProvider.initialize();
        
        checkCalibration();
    }

}
