/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CalibrationDataFileIO;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.profiler.NetBeansProfiler;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;

/**
 *
 * @author Jiri Sedlacek
 */
final class CalibrationSupport {
    
    private CalibrationSupport() {};
    
    // --- Internal API --------------------------------------------------------
    
    static boolean checkCalibration(Application app, Runnable before, Runnable after) {
        Jvm jvm = JvmFactory.getJVMFor(app);
        if (!jvm.isGetSystemPropertiesSupported()) return false;
        Properties properties = jvm.getSystemProperties();
        if (properties == null) return false;
        
        String java = Platform.getJDKVersionString(properties.getProperty("java.version"));  // NOI18N        
        int arch = Platform.getSystemArchitecture(properties.getProperty("sun.arch.data.model"));   // NOI18N
        if (checkCalibration(java, arch)) return true;
        
        String executable = JavaInfo.getJDKExecutable(properties.getProperty("java.home")); // NOI18N
        return calibrate(executable, java, arch, before, after);
    }
    
    static boolean checkCalibration(String java, int arch, Runnable before, Runnable after) {
        if (checkCalibration(java, arch)) return true;
        return calibrate(java, arch, before, after);
    }
    
    static boolean calibrate(String java, int arch, Runnable before, Runnable after) {
        return calibrate(null, java, arch, before, after);
    }
    
    
    // TODO: will be public in NetBeans 8.0: CalibrationDataFileIO.getCalibrationDataFileName(version)
    static String getCalibrationDataFileName(String targetJDKVerString) {
        String fileName = "machinedata" + "." + targetJDKVerString; // NOI18N
        try { return Platform.getProfilerUserDir() + File.separator + fileName; }
        catch (IOException ex) { return null; }
    }
    
    
    // --- Implementation ------------------------------------------------------
    
    private static boolean checkCalibration(String jdkString, int arch) {
        ProfilingSessionStatus status = NetBeansProfiler.getDefaultNB().
                getTargetAppRunner().getProfilingSessionStatus();
        status.targetJDKVersionString = jdkString;
        return CalibrationDataFileIO.readSavedCalibrationData(status) == 0;
    }
    
    private static boolean calibrate(String executable, String java, int arch, Runnable before, Runnable after) {
        boolean noarch = arch == -1;
        File executableF = executable == null ? null : new File(executable);
        
        if (!isFile(executableF)) {
            String javaName = ProfilerSupport.getJavaName(java);
            String archName = noarch ? null : ProfilerSupport.getArchName(arch);
            executable = JavaPlatformSelector.selectJavaBinary(javaName, archName,
                                              java, noarch ? null : Integer.toString(arch));
            
            executableF = executable == null ? null : new File(executable);
            if (!isFile(executableF)) return false;
        }
        
        if (noarch) {
            String[] archS = JavaInfo.getSystemProperties(executableF, "sun.arch.data.model"); // NOI18N
            try { arch = Integer.parseInt(archS[0]); } catch (Exception e) {} // AIOOBE, NFE
            if (arch == -1) return false;
        }
        
        // Get ProfilerEngineSettings instance
        TargetAppRunner runner = NetBeansProfiler.getDefaultNB().getTargetAppRunner();
        if (runner == null) return false;
        ProfilerEngineSettings pes = runner.getProfilerEngineSettings();

        // Save current state
        int savedPort = pes.getPortNo();
        InstrumentationFilter savedInstrFilter = pes.getInstrumentationFilter();
        String savedJVMExeFile = pes.getTargetJVMExeFile();
        String savedJDKVersionString = pes.getTargetJDKVersionString();
        int savedArch = pes.getSystemArchitecture();
        String savedCP = pes.getMainClassPath();

        // Setup ProfilerEngineSettings
        pes.setTargetJVMExeFile(executable);
        pes.setTargetJDKVersionString(java);
        pes.setSystemArchitecture(arch);
        pes.setPortNo(ProfilerIDESettings.getInstance().getCalibrationPortNo());
        pes.setInstrumentationFilter(new InstrumentationFilter());
        pes.setMainClassPath(""); // NOI18N
        
        // Perform calibration
        if (before != null) before.run();
        boolean result = calibrateJVM();
        if (after != null) after.run();
        
        // Restore original ProfilerEngineSettings
        pes.setPortNo(savedPort);
        pes.setInstrumentationFilter(savedInstrFilter);
        pes.setTargetJDKVersionString(savedJDKVersionString);
        pes.setSystemArchitecture(savedArch);
        pes.setTargetJVMExeFile(savedJVMExeFile);
        pes.setMainClassPath(savedCP);

        return result;
    }

    private static boolean calibrateJVM() {
        try {
            return NetBeansProfiler.getDefaultNB().runConfiguredCalibration();
        } catch (Exception e) {
            System.err.println(">>> Profiler calibration failed: " + e.getMessage()); // NOI18N
            e.printStackTrace(System.err);
        }

        return false;
    }
    
    private static boolean isFile(File file) {
        return file != null && file.isFile();
    }
    
}
