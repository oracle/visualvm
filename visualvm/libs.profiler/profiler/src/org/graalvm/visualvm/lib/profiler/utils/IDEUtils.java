/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.utils;

import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.openide.util.NbBundle;
import org.graalvm.visualvm.lib.common.Profiler;
import org.openide.util.HelpCtx;


/**
 * Utilities for interaction with the NetBeans IDE
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "IDEUtils_CreateNewConfigurationHint=<Create new configuration>",
    "IDEUtils_SelectSettingsConfigurationLabelText=Select the settings configuration to use:",
    "IDEUtils_SelectSettingsConfigurationDialogCaption=Select Settings Configuration",
    "IDEUtils_InvalidTargetJVMExeFileError=Invalid target JVM executable file specified: {0}\n{1}",
    "IDEUtils_ErrorConvertingProfilingSettingsMessage=Error occurred during automatic conversion of old Profiler configuration file\n   {0}\nto a new version\n   {1}.\n\nOperating system message:\n{2}",
    "IDEUtils_ListAccessName=List of available settings configurations.",
    "IDEUtils_OkButtonText=OK"
})
public final class IDEUtils {

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static HelpCtx HELP_CTX = new HelpCtx("SelectSettingsConfiguration.HelpCtx"); // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getAntProfilerStartArgument15(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_15_STRING);
    }

    public static String getAntProfilerStartArgument16(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_16_STRING);
    }

    public static String getAntProfilerStartArgument17(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_17_STRING);
    }

    public static String getAntProfilerStartArgument18(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_18_STRING);
    }

    public static String getAntProfilerStartArgument19(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_19_STRING);
    }

    public static String getAntProfilerStartArgument100(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_100_STRING);
    }

    public static String getAntProfilerStartArgument110(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_110_STRING);
    }

    public static String getAntProfilerStartArgument120(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_120_STRING);
    }

    public static String getAntProfilerStartArgument130(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_130_STRING);
    }

    public static String getAntProfilerStartArgument140(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_140_STRING);
    }

    public static String getAntProfilerStartArgument150(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_150_STRING);
    }

    public static String getAntProfilerStartArgument160(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_160_STRING);
    }

    public static String getAntProfilerStartArgument170(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_170_STRING);
    }

//    // Searches for a localized help. The default directory is <profiler_cluster>/docs/profiler,
//    // localized help is in <profiler_cluster>/docs/profiler_<locale_suffix> as obtained by NbBundle.getLocalizingSuffixes()
//    // see Issue 65429 (http://www.netbeans.org/issues/show_bug.cgi?id=65429)
//    public static String getHelpDir() {
//        Iterator suffixesIterator = NbBundle.getLocalizingSuffixes();
//        File localizedHelpDir = null;
//
//        while (suffixesIterator.hasNext() && (localizedHelpDir == null)) {
//            localizedHelpDir = InstalledFileLocator.getDefault()
//                                                   .locate("docs/profiler" + suffixesIterator.next(),
//                                                           "org.graalvm.visualvm.lib.profiler", false); //NOI18N
//        }
//
//        if (localizedHelpDir == null) {
//            return null;
//        } else {
//            return localizedHelpDir.getPath();
//        }
//    }


    private static String getAntProfilerStartArgument(int port, int architecture, String jdkVersion) {
        String ld = Profiler.getDefault().getLibsDir();
        String nativeLib = Platform.getAgentNativeLibFullName(ld, false, jdkVersion, architecture);
        
        if (ld.contains(" ")) { // NOI18N
            ld = "\"" + ld + "\""; // NOI18N
        }
        if (nativeLib.contains(" ")) { // NOI18N
            nativeLib = "\"" + nativeLib + "\""; // NOI18N
        }
        
        // -agentpath:D:/Testing/41 userdir/lib/deployed/jdk15/windows/profilerinterface.dll=D:\Testing\41 userdir\lib,5140
        return "-agentpath:" // NOI18N
               + nativeLib + "=" // NOI18N
               + ld + "," // NOI18N
               + port + "," // NOI18N
               + System.getProperty("profiler.agent.connect.timeout", "10"); // NOI18N // 10 seconds timeout by default
    }
    
}
