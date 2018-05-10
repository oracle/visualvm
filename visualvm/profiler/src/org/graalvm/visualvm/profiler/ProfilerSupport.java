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

package org.graalvm.visualvm.profiler;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.profiling.presets.ProfilerPreset;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.common.SessionSettings;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.profiler.NetBeansProfiler;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerSupport {
    
//    private static final Logger LOGGER = Logger.getLogger(ProfilerSupport.class.getName());
    
    private static final boolean FORCE_PROFILING_SUPPORTED =
            Boolean.getBoolean("org.graalvm.visualvm.profiler.SupportAllVMs");   // NOI18N
    private static final String HOTSPOT_VM_NAME_PREFIX = "Java HotSpot";    // NOI18N
    private static final String OPENJDK_VM_NAME_PREFIX = "OpenJDK ";    // NOI18N
    private static final String SAPJDK_VM_NAME_PREFIX = "SAP Java ";    // NOI18N
    private static final String SUN_VM_VENDOR_PREFIX = "Sun ";  // NOI18N
    private static final String ORACLE_VM_VENDOR_PREFIX = "Oracle ";  // NOI18N
    private static final String APPLE_VM_VENDOR_PREFIX = "Apple ";  // NOI18N
    private static final String HP_VM_VENDOR_PREFIX = "\"Hewlett-Packard "; // NOI18N
    private static final String AZUL_VM_VENDOR_PREFIX = "Azul ";  // NOI18N
    private static final String SAP_VM_VENDOR_PREFIX = "SAP AG";  // NOI18N
    private static final String GRAAL_VM_VENDOR_PREFIX = "GraalVM ";  // NOI18N
    private static final String ORACLE1_VM_VENDOR_PREFIX = "\"Oracle ";  // NOI18N
    
    private static final String JAVA_RT_16_PREFIX = "1.6.0";  // NOI18N
    private static final String JAVA_RT_17_PREFIX = "1.7.0";  // NOI18N
    private static final String JAVA_RT_18_PREFIX = "1.8.0";  // NOI18N
    private static final String JAVA_RT_19_PREFIX = "1.9.0";  // NOI18N
    private static final String JAVA_RT_9_PREFIX = "9";  // NOI18N
    private static final String JAVA_RT_100_PREFIX = "10";  // NOI18N
    
    private static ProfilerSupport instance;
    
    private boolean isInitialized;
    
    private Application profiledApplication;
    private final ApplicationProfilerViewProvider profilerViewProvider;


    public static synchronized ProfilerSupport getInstance() {
        if (instance == null) instance = new ProfilerSupport();
        return instance;
    }
    
    
    boolean isInitialized() {
        return isInitialized;
    }
    
    public String getProfiledApplicationName() {
        String name = NbBundle.getMessage(ProfilerSupport.class, "STR_Externaly_started_app"); // NOI18N
        Application a = getProfiledApplication();
        if (a == null) {
            int state = NetBeansProfiler.getDefaultNB().getProfilingState();
            return state == NetBeansProfiler.PROFILING_INACTIVE ? null : name;
        }
        DataSourceDescriptor d = DataSourceDescriptorFactory.getDescriptor(a);
        return d != null ? d.getName() : name;
    }
    
    public int getDefaultPort() {
        return ProfilerIDESettings.getInstance().getPortNo();
    }
    
    public boolean hasSupportedJavaPlatforms() {
        for (int i = 0; i < 5; i++) {
            String code = "jdk1" + (5 + i); // NOI18N
            if (supportsProfiling(code, 32) || supportsProfiling(code, 64)) return true;
        }
        return false;
    }
    
    public String[][] getSupportedJavaPlatforms() {
        List<String> codesl = new ArrayList();
        
        for (int i = 0; i < 5; i++) {
            String code = "jdk1" + (5 + i); // NOI18N
            if (supportsProfiling(code, 32) || supportsProfiling(code, 64)) codesl.add(code);
        }
        String code = "jdk100"; // NOI18N
        if (supportsProfiling(code, 32) || supportsProfiling(code, 64)) codesl.add(code);
        
        String[] names = new String[codesl.size()];
        String[] codes = new String[codesl.size()];
        String current = null;
        for (int i = 0; i < codesl.size(); i++) {
            codes[i] = codesl.get(i);
            names[i] = getJavaName(codes[i]);
            if (Platform.getJDKVersionString().equals(codes[i])) current = names[i];
        }
        
        return new String[][] { names, codes, { current } };
    }
    
    public String[][] getSupportedArchitectures(String java) {
        List<String> codesl = new ArrayList();
        
        if (supportsProfiling(java, 32)) codesl.add(Integer.toString(32));
        if (supportsProfiling(java, 64)) codesl.add(Integer.toString(64));
        
        String[] names = new String[codesl.size()];
        String[] codes = new String[codesl.size()];
        String current = null;
        for (int i = 0; i < codesl.size(); i++) {
            codes[i] = codesl.get(i);
            names[i] = getArchName(Integer.parseInt(codes[i]));
            if (Integer.toString(Platform.getSystemArchitecture()).equals(codes[i])) current = names[i];
        }
        
        return new String[][] { names, codes, { current } };
    }
    
    static String getJavaName(String code) {
        if (Platform.JDK_15_STRING.equals(code))
            return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_platform_name", 5); // NOI18N
        if (Platform.JDK_16_STRING.equals(code))
            return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_platform_name", 6); // NOI18N
        if (Platform.JDK_17_STRING.equals(code))
            return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_platform_name", 7); // NOI18N
        if (Platform.JDK_18_STRING.equals(code))
            return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_platform_name", 8); // NOI18N
        if (Platform.JDK_19_STRING.equals(code))
            return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_platform_name", 9); // NOI18N
        if (Platform.JDK_100_STRING.equals(code))
            return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_platform_name", 10); // NOI18N
        throw new IllegalArgumentException("Unknown java code " + code); // NOI18N
    }
    
    static String getArchName(int arch) {
        if (32 == arch) return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_arch_name", 32); // NOI18N
        if (64 == arch) return NbBundle.getMessage(ProfilerSupport.class, "STR_Java_arch_name", 64); // NOI18N
        throw new IllegalArgumentException("Unsupported architecture " + arch); // NOI18N
    }
    
    public boolean supportsProfiling(String java, int architecture) {
        String ld = Profiler.getDefault().getLibsDir();
        String nativeLib = Platform.getAgentNativeLibFullName(ld, false, java, architecture);
        return new File(nativeLib).isFile();
    }
    
    public String getStartupParameter(String java, int architecture, int port) {
        String ld = Profiler.getDefault().getLibsDir();
        if (ld.contains(" ")) ld = "\"" + ld + "\""; // NOI18N
        
        String nativeLib = Platform.getAgentNativeLibFullName(ld, false, java, architecture);        
        if (nativeLib.contains(" ")) nativeLib = "\"" + nativeLib + "\""; // NOI18N
        
        return "-agentpath:" + nativeLib + "=" + ld + "," + port; // NOI18N
    }
    
    public void profileProcessStartup(final String java, final int architecture, final int port,
                                      ProfilerSettingsSupport settings, final ProfilerPreset preset) {
        
        if (!CalibrationSupport.checkCalibration(java, architecture, null, null)) return;
        
        final ProfilingSettings pSettings = settings.getSettings();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {                
        // Perform the actual attach
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (!checkStartedApp(port)) return;

                final RequestProcessor processor = new RequestProcessor("Startup Profiler @ " + port); // NOI18N
                Host.LOCALHOST.getRepository().addDataChangeListener(
                    new DataChangeListener<Application>() {
                        public void dataChanged(final DataChangeEvent<Application> event) {
                            final DataChangeListener listener = this;
                            processor.post(new Runnable() {
                                public void run() {
                                    if (!event.getAdded().equals(event.getCurrent())) // filter-out initial sync event
                                        for (Application a : event.getAdded()) {
                                            if (isProfiledApplication(a, port)) {
                                                Host.LOCALHOST.getRepository().removeDataChangeListener(listener);

                                                setProfiledApplication(a);
                                                selectProfilerView(a, preset, pSettings);

                                                break;
                                            }
                                        }
                                }
                            });
                        }
                    }, Application.class);

                ProfilingSettings ps = pSettings;
                SessionSettings ss = createSessionSettings(java, architecture, port);
                NetBeansProfiler.getDefaultNB().connectToStartedApp(ps, ss);
                resetTerminateDialogs();
            }
        });
        }
        });
    }
    
    private static boolean checkStartedApp(int port) {
        String homeDir = System.getProperty("user.home"); // NOI18N
        File agentF = new File(homeDir + File.separator + ".nbprofiler" + File.separator + port); // NOI18N
        if (!agentF.isFile()) return true;
        
        String caption = NbBundle.getMessage(ProfilerSupport.class, "CAP_Warning"); // NOI18N
        String message = NbBundle.getMessage(ProfilerSupport.class, "MSG_StartedTooSoon"); // NOI18N
        NotifyDescriptor nd = new NotifyDescriptor(message, caption, NotifyDescriptor.OK_CANCEL_OPTION,
                                  NotifyDescriptor.WARNING_MESSAGE, null, NotifyDescriptor.OK_OPTION);
        if (DialogDisplayer.getDefault().notify(nd) != NotifyDescriptor.OK_OPTION) return false;
        return checkStartedApp(port);
    }
    
    private static void resetTerminateDialogs() {
        String dnsaKey = "NetBeansProfiler.handleShutdown.noResults"; // NOI18N
        ProfilerIDESettings.getInstance().setDoNotShowAgain(dnsaKey, null);
        dnsaKey = "NetBeansProfiler.handleShutdown"; // NOI18N
        String dnsa = ProfilerIDESettings.getInstance().getDoNotShowAgain(dnsaKey);
        if ("NO_OPTION".equals(dnsa)) ProfilerIDESettings.getInstance().setDoNotShowAgain(dnsaKey, null); // NOI18N        }
    }
    
    private static SessionSettings createSessionSettings(String java, int architecture, int port) {
        SessionSettings ss = new SessionSettings();
        ss.setJavaVersionString(java);
        ss.setSystemArchitecture(architecture);
        ss.setPortNo(port);
        ss.setJavaExecutable(JavaInfo.getCurrentJDKExecutable()); // Workaround for calibration check, not used for profiling
        return ss;
    }
    
    private static boolean isProfiledApplication(Application a, int port) {
        Jvm jvm = JvmFactory.getJVMFor(a);
        if (!jvm.isBasicInfoSupported()) return false;
        String args = jvm.getJvmArgs();
        return args.contains("-agentpath:") && args.contains("," + port); // NOI18N
    }
    
    
    boolean supportsProfiling(Application application) {
        // Application already being profiled (Startup Profiler)
        if (application == getProfiledApplication()) return true;
        
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
        
//////        //TODO
//////        // temporary disabled until profiling is rewritten to NB 90
//////        return false;

        // Profiled application needs to be running JDK 6.0 or 7.0 or 8.0 or 9.0
        // or 10
        if (!jvm.is16() && !jvm.is17() && !jvm.is18()
             && !jvm.is19() && !jvm.is100()) return false;
        
        String vmName = jvm.getVmName();
        String vmVendor = jvm.getVmVendor();
        
        // VM has to be a HotSpot VM or OpenJDK by Sun Microsystems Inc. or 
        // Oracle Co. or Apple Inc. or Hewlett-Packard Co. or 
        // Azul Systems, Inc. or SAP AG or Graal VM
        return vmName != null && (vmName.startsWith(HOTSPOT_VM_NAME_PREFIX) || vmName.startsWith(OPENJDK_VM_NAME_PREFIX)
                               || vmName.startsWith(SAPJDK_VM_NAME_PREFIX) || vmName.startsWith(GRAAL_VM_VENDOR_PREFIX)) &&
               vmVendor != null && (vmVendor.startsWith(ORACLE_VM_VENDOR_PREFIX) || vmVendor.startsWith(SUN_VM_VENDOR_PREFIX)
                                 || vmVendor.startsWith(APPLE_VM_VENDOR_PREFIX) || vmVendor.startsWith(HP_VM_VENDOR_PREFIX)
                                 || vmVendor.startsWith(AZUL_VM_VENDOR_PREFIX) || vmVendor.startsWith(SAP_VM_VENDOR_PREFIX)
                                 || vmVendor.startsWith(ORACLE1_VM_VENDOR_PREFIX));
    }
    
    static boolean classSharingBreaksProfiling(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;

        Jvm jvm = JvmFactory.getJVMFor(application);
        String vmInfo = jvm.getVmInfo();
        boolean classSharing = vmInfo.contains("sharing"); // NOI18N
        
        if (!jvm.isGetSystemPropertiesSupported()) return classSharing;
        Properties properties = jvm.getSystemProperties();
        if (properties == null) return classSharing;
        
        String javaRTVersion = properties.getProperty("java.runtime.version"); // NOI18N
        if (javaRTVersion == null) return classSharing;
        
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
                if (updateNumber == 0) {
                    if (buildNumber >= 26) return false;
                } else {
                    return false;
                }
            } else if (javaRTVersion.startsWith(JAVA_RT_18_PREFIX)) {
                return false;
            } else if (javaRTVersion.startsWith(JAVA_RT_19_PREFIX)) {
                return false;
            } else if (javaRTVersion.startsWith(JAVA_RT_9_PREFIX)) {
                return false;
            } else if (javaRTVersion.startsWith(JAVA_RT_100_PREFIX)) {
                return false;
            }
        // OpenJDK -------------------------------------------------------------
        } else if(vmName.startsWith(OPENJDK_VM_NAME_PREFIX)) {
            // OpenJDK 6 is OK from Build 11
            if (javaRTVersion.startsWith(JAVA_RT_16_PREFIX)) {
                if (updateNumber == 0) {
                    if (buildNumber >= 11) return false;
                } else {
                    return false;
                }
            // OpenJDK 7 is assumed to be OK from Build 26 (not tested)
            } else if (javaRTVersion.startsWith(JAVA_RT_17_PREFIX)) {
                if (updateNumber == 0) {
                    if (buildNumber >= 26) return false;
                } else {
                    return false;
                }
            }
            // OpenJDK 8 should be OK
            else if (javaRTVersion.startsWith(JAVA_RT_18_PREFIX)) {
                return false;
            } else if (javaRTVersion.startsWith(JAVA_RT_19_PREFIX)) {
                return false;
            } else if (javaRTVersion.startsWith(JAVA_RT_9_PREFIX)) {
                return false;
            } else if (javaRTVersion.startsWith(JAVA_RT_100_PREFIX)) {
                return false;
            }
        }
        
        return classSharing;
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
    
    synchronized void setProfiledApplication(Application profiledApplication) {
        this.profiledApplication = profiledApplication;
    }
  
    synchronized Application getProfiledApplication() {
        return profiledApplication;
    }
    
    void selectActiveProfilerView() {
        selectProfilerView(getProfiledApplication());
    }
    
    void selectProfilerView(Application application) {
        selectProfilerView(application, null, null);
    }
    
    private void selectProfilerView(Application application, final ProfilerPreset preset, final ProfilingSettings settings) {
        if (application == null) return;
        
        final DataSourceView activeView = profilerViewProvider.view(application);
        if (activeView == null) return;
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (preset != null && settings != null)
                    ((ApplicationProfilerView)activeView).selectPreset(preset, settings);
                DataSourceWindowManager.sharedInstance().selectView(activeView);
            }
        });
    }
    
    
    private ProfilerSupport() {
        isInitialized = NetBeansProfiler.isInitialized();
        
        if (isInitialized) {
            profilerViewProvider = new ApplicationProfilerViewProvider();
            profilerViewProvider.initialize();

            ProfilerIDESettings.getInstance().setAutoOpenSnapshot(false);
            ProfilerIDESettings.getInstance().setAutoSaveSnapshot(true);
        } else {
            profilerViewProvider = null;
        }
    }

}
