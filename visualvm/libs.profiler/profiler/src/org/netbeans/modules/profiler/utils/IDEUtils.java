/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.utils;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.openide.util.NbBundle;
import org.netbeans.lib.profiler.common.Profiler;
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
//                                                           "org.netbeans.modules.profiler", false); //NOI18N
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
