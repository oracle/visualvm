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

package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import com.sun.tools.visualvm.host.Host;
import java.io.File;
import java.util.Properties;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class ProfilerSupport {
    private static final boolean FORCE_PROFILING_SUPPORTED = Boolean.getBoolean("com.sun.tools.visualvm.profiler.SupportAllVMs");   // NOI18N
    private static final String HOTSPOT_VM_NAME_PREFIX = "Java HotSpot";    // NOI18N
    private static final String OPENJDK_VM_NAME_PREFIX = "OpenJDK ";    // NOI18N
    private static final String SUN_VM_VENDOR_PREFIX = "Sun ";  // NOI18N
    private static final String APPLE_VM_VENDOR_PREFIX = "Apple ";  // NOI18N
    private static final String HP_VM_VENDOR_PREFIX = "Hewlett-Packard "; // NOI18N
    
    private static final String JAVA_RT_16_PREFIX = "1.6.0";  // NOI18N
    private static final String JAVA_RT_17_PREFIX = "1.7.0";  // NOI18N
    
    private static ProfilerSupport instance;
    
    private boolean isInitialized;
    
    private Application profiledApplication;
    private ProfilerSnapshotCategory category;
    private ApplicationProfilerViewProvider profilerViewProvider;
    private ProfilerSnapshotProvider profilerSnapshotsProvider;


    public static synchronized ProfilerSupport getInstance() {
        if (instance == null) instance = new ProfilerSupport();
        return instance;
    }
    
    
    public SnapshotCategory getCategory() {
        return category;
    }
    
    
    boolean isInitialized() {
        return isInitialized;
    }
    
    boolean supportsProfiling(Application application) {
        // Remote profiling is not supported
        if (application.getHost() != Host.LOCALHOST) return false;
        
        // Profiling current VisualVM instance is not supported
        if (Application.CURRENT_APPLICATION.equals(application)) return false;
        
        // Profiled application has to be running
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        
        
        Jvm jvm = JvmFactory.getJVMFor(application);
        
        // Basic info has to be supported and VM has to be attachable
        if (!jvm.isBasicInfoSupported() || !jvm.isAttachable()) return false;
        
        // User explicitly requests to profile any VM
        if (FORCE_PROFILING_SUPPORTED) return true;
        
        // Profiled application needs to be running JDK 6.0 or 7.0
        if (!jvm.is16() && !jvm.is17()) return false;
        
        String vmName = jvm.getVmName();
        String vmVendor = jvm.getVmVendor();
        
        // VM has to be a HotSpot VM or OpenJDK by Sun Microsystems Inc. or Apple Inc. or Hewlett-Packard Co.
        return vmName != null && (vmName.startsWith(HOTSPOT_VM_NAME_PREFIX) || vmName.startsWith(OPENJDK_VM_NAME_PREFIX)) && 
               vmVendor != null && (vmVendor.startsWith(SUN_VM_VENDOR_PREFIX) || vmVendor.startsWith(APPLE_VM_VENDOR_PREFIX) ||
                                    vmVendor.startsWith(HP_VM_VENDOR_PREFIX));
    }
    
    boolean classSharingBreaksProfiling(Application application) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        Properties properties = jvm.getSystemProperties();
        if (properties == null) return true;
        
        String javaRTVersion = properties.getProperty("java.runtime.version"); // NOI18N
        if (javaRTVersion == null) return true;
        
        int updateNumber = getUpdateNumber(javaRTVersion);
        int buildNumber = getBuildNumber(javaRTVersion);
        String vmName = jvm.getVmName();
        
        // Sun JDK & derived JDKs ----------------------------------------------
        if (vmName.startsWith(HOTSPOT_VM_NAME_PREFIX)) {
            // JDK 6.0 is OK from Update 6 except of Update 10 Build 23 and lower
            if (javaRTVersion.startsWith(JAVA_RT_16_PREFIX)) {
                if (updateNumber == 10) {
                    if (buildNumber >= 24) return false;
                } else if (updateNumber >= 6) return false;
            // JDK 7.0 is OK from Build 26
            } else if (javaRTVersion.startsWith(JAVA_RT_17_PREFIX)) {
                if (buildNumber >= 26) return false;
            }
        // OpenJDK -------------------------------------------------------------
        } else if(vmName.startsWith(OPENJDK_VM_NAME_PREFIX)) {
            // OpenJDK 6 is OK from Build 11
            if (javaRTVersion.startsWith(JAVA_RT_16_PREFIX)) {
                if (buildNumber >= 11) return false;
            // OpenJDK 7 is assumed to be OK from Build 26 (not tested)
            } else if (javaRTVersion.startsWith(JAVA_RT_17_PREFIX)) {
                if (buildNumber >= 26) return false;
            }
        }
        
        String vmInfo = jvm.getVmInfo();
        return vmInfo.contains("sharing"); // NOI18N
    }
    
    private static int getUpdateNumber(String javaRTVersion) {
        int underscoreIndex = javaRTVersion.indexOf("_"); // NOI18N
        if (underscoreIndex == -1) return 0; // Assumes no update, may be incorrect for unexpected javaRTVersion format
        
        try {
            String updateNumberString = javaRTVersion.substring(underscoreIndex + "_".length(), javaRTVersion.indexOf("-")); // NOI18N
            return Integer.parseInt(updateNumberString);
        } catch (Exception e) {}
        
        return -1;
    }
    
    private static int getBuildNumber(String javaRTVersion) {
        try {
            String buildNumberString = javaRTVersion.substring(javaRTVersion.indexOf("-b") + "-b".length()); // NOI18N
            return Integer.parseInt(buildNumberString);
        } catch (Exception e) {}
        
        return -1;
    }
    
    ProfilerSnapshotProvider getSnapshotsProvider() {
        return profilerSnapshotsProvider;
    }
    
    void setProfiledApplication(Application profiledApplication) {
        this.profiledApplication = profiledApplication;
    }
  
    Application getProfiledApplication() {
        return profiledApplication;
    }
    
    void selectActiveProfilerView() {
        selectProfilerView(profiledApplication);
    }
    
    void selectProfilerView(Application application) {
        if (application == null) return;
        DataSourceView activeView = profilerViewProvider.view(application);
        if (activeView == null) return;
        DataSourceWindowManager.sharedInstance().selectView(activeView);
    }
    
    
    // TODO: move to JVM?
    private static String getCurrentJDKExecutable() {
        return getJDKExecutable(System.getProperty("java.home")); // NOI18N
    }
    
    private static String getJDKExecutable(String jdkHome) {
        if (jdkHome == null || jdkHome.trim().length() == 0) return null;
        String jreSuffix = File.separator + "jre"; // NOI18N
        if (jdkHome.endsWith(jreSuffix)) jdkHome = jdkHome.substring(0, jdkHome.length() - jreSuffix.length());
        String jdkExe = jdkHome + File.separator + "bin" + File.separator + "java" + (Platform.isWindows() ? ".exe" : ""); // NOI18N
        return jdkExe;
    }
    
    private boolean checkCurrentJDKCalibration() {
        return checkCalibration(getCurrentJDKExecutable(), Platform.getJDKVersionString(),
                Platform.getSystemArchitecture(), null, null);
    }
    
    boolean checkJDKCalibration(Application application, Runnable preCalibrator, Runnable postCalibrator) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        Properties properties = jvm.getSystemProperties();
        if (properties == null) return false;
        
        return checkCalibration(getJDKExecutable(properties.getProperty("java.home")), // NOI18N
                Platform.getJDKVersionString(properties.getProperty("java.version")), // NOI18N
                Platform.getSystemArchitecture(), preCalibrator, postCalibrator);
    }

    private boolean checkCalibration(String jvmExecutable, String jdkString, int architecture,
                                     Runnable preCalibrator, Runnable postCalibrator) {
        if (jvmExecutable == null) return false;
        
        // Get ProfilerEngineSettings instance
        org.netbeans.lib.profiler.ProfilerEngineSettings pes = NetBeansProfiler.getDefaultNB().getTargetAppRunner().getProfilerEngineSettings();

        // Save current state
        int savedPort = pes.getPortNo();
        org.netbeans.lib.profiler.global.InstrumentationFilter savedInstrFilter = pes.getInstrumentationFilter();
        String savedJVMExeFile = pes.getTargetJVMExeFile();
        String savedJDKVersionString = pes.getTargetJDKVersionString();
        int savedArch = pes.getSystemArchitecture();
        String savedCP = pes.getMainClassPath();

        boolean result = true;

        // Setup ProfilerEngineSettings
        pes.setTargetJVMExeFile(jvmExecutable);
        pes.setTargetJDKVersionString(jdkString);
        pes.setSystemArchitecture(architecture);
        pes.setPortNo(ProfilerIDESettings.getInstance().getCalibrationPortNo());
        pes.setInstrumentationFilter(new org.netbeans.lib.profiler.global.InstrumentationFilter());
        pes.setMainClassPath(""); // NOI18N

        // Perform calibration if necessary
        if (!NetBeansProfiler.getDefaultNB().getTargetAppRunner().readSavedCalibrationData()) {
            if (preCalibrator != null) preCalibrator.run();
            result = calibrateJVM();
            if (postCalibrator != null) postCalibrator.run();
        }

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
        NetBeansProfiler.getDefaultNB().displayInfoAndWait(NbBundle.getMessage(ProfilerSupport.class, "MSG_Calibration")); // NOI18N

        // Perform calibration
        boolean result = false;
        try {
            result = NetBeansProfiler.getDefaultNB().runConfiguredCalibration();
        } catch (Exception e) {
            System.err.println("Failed to calibrate profiler:"); // NOI18N
            e.printStackTrace();
        }

        return result;
    }
    
    
    private ProfilerSupport() {
        isInitialized = Profiler.getDefault() != null && checkCurrentJDKCalibration();
        
        if (isInitialized) {
        
            DataSourceDescriptorFactory.getDefault().registerProvider(new ProfilerSnapshotDescriptorProvider());
            profilerViewProvider = new ApplicationProfilerViewProvider();
            profilerViewProvider.initialize();

            new ProfilerSnapshotViewProvider().initialize();
            new SnapshotDiffViewProvider().initialize();

            category = new ProfilerSnapshotCategory();
            RegisteredSnapshotCategories.sharedInstance().registerCategory(category);

            profilerSnapshotsProvider = new ProfilerSnapshotProvider();
            profilerSnapshotsProvider.initialize();

            ProfilerIDESettings.getInstance().setAutoOpenSnapshot(false);
            ProfilerIDESettings.getInstance().setAutoSaveSnapshot(true);
        
        }
    }

}
