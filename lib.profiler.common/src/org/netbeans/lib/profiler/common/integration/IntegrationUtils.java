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

package org.netbeans.lib.profiler.common.integration;

import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import java.io.*;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utils for platform- and settings-specific integration instructions.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class IntegrationUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.integration.Bundle"); // NOI18N
    public static final String PLATFORM_JAVA_50 = messages.getString("IntegrationUtils_PlatformJava50"); // NOI18N
    public static final String PLATFORM_JAVA_60 = messages.getString("IntegrationUtils_PlatformJava60"); // NOI18N
    public static final String PLATFORM_JAVA_70 = messages.getString("IntegrationUtils_PlatformJava70"); // NOI18N
    public static final String PLATFORM_JAVA_80 = messages.getString("IntegrationUtils_PlatformJava80"); // NOI18N
    public static final String PLATFORM_JAVA_CVM = messages.getString("IntegrationUtils_PlatformJavaCvm"); // NOI18N
    public static final String PLATFORM_WINDOWS_OS = messages.getString("IntegrationUtils_PlatformWindowsOs"); // NOI18N
    public static final String PLATFORM_WINDOWS_AMD64_OS = messages.getString("IntegrationUtils_PlatformWindowsAmd64Os"); // NOI18N
    public static final String PLATFORM_LINUX_OS = messages.getString("IntegrationUtils_PlatformLinuxOs"); // NOI18N
    public static final String PLATFORM_LINUX_AMD64_OS = messages.getString("IntegrationUtils_PlatformLinuxAmd64Os"); // NOI18N
    public static final String PLATFORM_SOLARIS_INTEL_OS = messages.getString("IntegrationUtils_PlatformSolarisIntelOs"); // NOI18N
    public static final String PLATFORM_SOLARIS_AMD64_OS = messages.getString("IntegrationUtils_PlatformSolarisAmd64Os"); // NOI18N
    public static final String PLATFORM_SOLARIS_SPARC_OS = messages.getString("IntegrationUtils_PlatformSolarisSparcOs"); // NOI18N
    public static final String PLATFORM_SOLARIS_SPARC64_OS = messages.getString("IntegrationUtils_PlatformSolarisSparc64Os"); // NOI18N
    public static final String PLATFORM_MAC_OS = messages.getString("IntegrationUtils_PlatformMacOs"); // NOI18N
    public static final String MODIFIED_FOR_PROFILER_STRING = messages.getString("IntegrationUtils_ModifiedForProfilerString"); // NOI18N
    public static final String ORIGINAL_BACKUP_LOCATION_STRING = messages.getString("IntegrationUtils_OriginalBackupLocationString"); // NOI18N
    private static final String APPLICATION_STRING = messages.getString("IntegrationUtils_ApplicationString"); // NOI18N
    private static final String APPLET_STRING = messages.getString("IntegrationUtils_AppletString"); // NOI18N
    private static final String SERVER_STRING = messages.getString("IntegrationUtils_ServerString"); // NOI18N
    private static final String DATABASE_STRING = messages.getString("IntegrationUtils_DatabaseString"); // NOI18N
    private static final String TARGET_STRING = messages.getString("IntegrationUtils_TargetString"); // NOI18N
    private static final String JDK_50_NAME = messages.getString("IntegrationUtils_Jdk50Name"); // NOI18N
    private static final String JDK_60_NAME = messages.getString("IntegrationUtils_Jdk60Name"); // NOI18N
    private static final String JDK_70_NAME = messages.getString("IntegrationUtils_Jdk70Name"); // NOI18N
    private static final String JDK_80_NAME = messages.getString("IntegrationUtils_Jdk80Name"); // NOI18N
    private static final String JDK_CVM_NAME = messages.getString("IntegrationUtils_JdkCvmName"); // NOI18N
    private static final String HTML_REMOTE_STRING = "&lt;" + messages.getString("IntegrationUtils_RemoteString") + "&gt;"; // NOI18N
    private static final String EXPORT_SETENV_MESSAGE = messages.getString("IntegrationUtils_ExportSetenvMessage"); // NOI18N
    private static final String REDUCE_OVERHEAD_MESSAGE = messages.getString("IntegrationUtils_ReduceOverheadMessage"); // NOI18N
    private static final String COPY_FILE_NOT_FOUND_MESSAGE = messages.getString("IntegrationUtils_CopyFileNotFoundMessage"); // NOI18N
    private static final String COPY_CANNOT_DELETE_FILE_MESSAGE = messages.getString("IntegrationUtils_CopyCannotDeleteFileMessage"); // NOI18N
    private static final String COPY_ERROR_MESSAGE = messages.getString("IntegrationUtils_CopyErrorMessage"); // NOI18N
    private static final String BACKUP_FILE_NOT_FOUND_MESSAGE = messages.getString("IntegrationUtils_BackupFileNotFoundMessage"); // NOI18N
    private static final String BACKUP_CANNOT_DELETE_FILE_MESSAGE = messages.getString("IntegrationUtils_BackupCannotDeleteFileMessage"); // NOI18N
    private static final String BACKUP_ERROR_MESSAGE = messages.getString("IntegrationUtils_BackupErrorMessage"); // NOI18N
    private static final String BACKUP_ERROR_COPY_FILE_MESSAGE = messages.getString("IntegrationUtils_BackupErrorCopyFileMessage"); // NOI18N
    private static final String RESTORE_FILE_NOT_FOUND_MESSAGE = messages.getString("IntegrationUtils_RestoreFileNotFoundMessage"); // NOI18N
    private static final String RESTORE_CANNOT_DELETE_FILE_MESSAGE = messages.getString("IntegrationUtils_RestoreCannotDeleteFileMessage"); // NOI18N
    private static final String RESTORE_ERROR_MESSAGE = messages.getString("IntegrationUtils_RestoreErrorMessage"); // NOI18N
    private static final String MANUAL_REMOTE_STEP1_MESSAGE = messages.getString("IntegrationUtils_ManualRemoteStep1Message"); // NOI18N
    private static final String MANUAL_REMOTE_STEP2_MESSAGE = messages.getString("IntegrationUtils_ManualRemoteStep2Message"); // NOI18N
    private static final String REMOTE_ABSOLUTE_PATH_HINT = messages.getString("IntegrationUtils_RemoteAbsolutePathHint"); // NOI18N
    private static final String SPACES_IN_PATH_WARNING_MSG = messages.getString("IntegrationUtils_SpacesInPathWarningMsg"); // NOI18N

    // -----
    public static final String FILE_BACKUP_EXTENSION = ".backup"; //NOI18N
    private static final String BINARIES_TMP_PREFIX = "NBProfiler";
    private static final String BINARIES_TMP_EXT = ".link";

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // Returns command for adding profiler native libraries to path
    public static String getAddProfilerLibrariesToPathString(String targetPlatform, String targetJVM, boolean isRemote,
                                                             boolean formatAsHTML) {
        return getExportEnvVariableValueString(targetPlatform, getNativePathEnvVariableString(targetPlatform),
                                               getNativeLibrariesPath(targetPlatform, targetJVM, isRemote)
                                               + getClassPathSeparator(targetPlatform)
                                               + getEnvVariableReference(getNativePathEnvVariableString(targetPlatform),
                                                                         targetPlatform), formatAsHTML);
    }

    public static String getAssignEnvVariableValueString(String targetPlatform, String variableName, String variableValue) {
        variableValue = variableValue.trim();
        if (isWindowsPlatform(targetPlatform)) {
            return getExportCommandString(targetPlatform) + " " + variableName + "=" + variableValue; //NOI18N
        }

        return variableName + "=" + (variableValue.contains(" ") ? "\"" + variableValue + "\"" : variableValue); //NOI18N
    }

    // Returns batch file extension bat / sh according to current / selected OS
    public static String getBatchExtensionString(String targetPlatform, String customExt) {
        if (isWindowsPlatform(targetPlatform)) {
            return customExt + ".bat"; //NOI18N
        }
        return customExt + ".sh"; //NOI18N
    }

    public static String getBatchExtensionString(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return ".bat"; //NOI18N
        }

        return ".sh"; //NOI18N
    }

    // Returns HTML-formatted hint about how to reduce CPU profiling overhead
    public static String getCPUReduceOverheadHint() {
        return REDUCE_OVERHEAD_MESSAGE;
    }

    // Returns "\" or "/" according to provided platform
    public static String getDirectorySeparator(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "\\"; //NOI18N
        }

        return "/"; //NOI18N
    }

    // Returns reference to given environment variable according to current / selected OS
    public static String getEnvVariableReference(String envVariable, String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "%" + envVariable + "%"; //NOI18N
        }

        return "$" + envVariable; //NOI18N
    }

    // Returns SET / export command  according to current / selected OS
    public static String getExportCommandString(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "SET"; //NOI18N
        }

        return "export"; //NOI18N
    }

    // Returns expression for exporting environment variable value
    public static String getExportEnvVariableValueString(String targetPlatform, String variableName, String variableValue,
                                                         boolean formatAsHTML) {
        if (isWindowsPlatform(targetPlatform)) {
            return getAssignEnvVariableValueString(targetPlatform, variableName, variableValue);
        }

        if (targetPlatform.equals(PLATFORM_LINUX_OS)) {
            return getExportCommandString(targetPlatform) + " "
                   + getAssignEnvVariableValueString(targetPlatform, variableName, variableValue); //NOI18N
        }

        return getAssignEnvVariableValueString(targetPlatform, variableName, variableValue)
               + (formatAsHTML ? "<br>" : getLineBreak(targetPlatform)) + getExportCommandString(targetPlatform) + " "
               + variableName; //NOI18N
    }

    // Returns HTML-formatted note about export vs. setenv on UNIXes
    public static String getExportVSSetenvNote() {
        return EXPORT_SETENV_MESSAGE;
    }

    public static boolean isFileModifiedForProfiler(File file) {
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            // check if first line contains Profiler header
            String line = br.readLine();

            if (line == null) {
                br.close();

                return false;
            }

            if (line.indexOf(MODIFIED_FOR_PROFILER_STRING) != -1) {
                br.close();

                return true;
            }

            // check if second line contains Profiler header (first line can contain #!/bin/sh for UNIX scripts or <?xml version...> for xml file)
            line = br.readLine();

            if (line == null) {
                br.close();

                return false;
            }

            if (line.indexOf(MODIFIED_FOR_PROFILER_STRING) != -1) {
                br.close();

                return true;
            }

            br.close();

            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    // converts CommonConstants.JDK_15_STRING and CommonConstants.JDK_16_STRING to
    // IntegrationUtils.PLATFORM_JAVA_50 and IntegrationUtils.PLATFORM_JAVA_60.
    public static String getJavaPlatformFromJavaVersionString(String javaVersionString) {
        if (javaVersionString.equals(CommonConstants.JDK_15_STRING)) {
            return PLATFORM_JAVA_50;
        } else if (javaVersionString.equals(CommonConstants.JDK_16_STRING)) {
            return PLATFORM_JAVA_60;
        } else if (javaVersionString.equals(CommonConstants.JDK_17_STRING)) {
            return PLATFORM_JAVA_70;
        } else if (javaVersionString.equals(CommonConstants.JDK_18_STRING)) {
            return PLATFORM_JAVA_80;
        } else if (javaVersionString.equals(CommonConstants.JDK_CVM_STRING)) {
            return PLATFORM_JAVA_CVM;
        }
        return null;
    }

    // Returns friendly java platform name
    public static String getJavaPlatformName(String javaPlatform) {
        if (javaPlatform.equals(PLATFORM_JAVA_50)) {
            return JDK_50_NAME;
        } else if (javaPlatform.equals(PLATFORM_JAVA_60)) {
            return JDK_60_NAME;
        } else if (javaPlatform.equals(PLATFORM_JAVA_70)) {
            return JDK_70_NAME;
        } else if (javaPlatform.equals(PLATFORM_JAVA_80)) {
            return JDK_80_NAME;
        } else if (javaPlatform.equals(PLATFORM_JAVA_CVM)) {
            return JDK_CVM_NAME;
        }

        return javaPlatform;
    }

    // Returns java platform-specific directory: jdk15 / jdk16
    public static String getJavaPlatformNativeLibrariesDirectoryName(String javaPlatform) {
        if (javaPlatform.equals(PLATFORM_JAVA_50)) {
            return "jdk15"; //NOI18N
        } else if (javaPlatform.equals(PLATFORM_JAVA_60)) {
            return "jdk16"; //NOI18N 
        } else if (javaPlatform.equals(PLATFORM_JAVA_70)) {
            return "jdk16"; //NOI18N // for JDK 7.0 we use the same as for 6.0 for now
        } else if (javaPlatform.equals(PLATFORM_JAVA_80)) {
            return "jdk16"; //NOI18N // for JDK 8.0 we use the same as for 6.0 for now
        } else if (javaPlatform.equals(PLATFORM_JAVA_CVM)) {
            return "cvm";  // NOI18N
        }

        throw new IllegalArgumentException("Unsupported platform " + javaPlatform); // NOI18N
    }

    // Returns the path to agent native libraries according to current / selected OS
    public static String getLibsDir(String targetPlatform, boolean isRemote) {
        if (isRemote) {
            return HTML_REMOTE_STRING + getDirectorySeparator(targetPlatform) + "lib"; //NOI18N
        }

        return Profiler.getDefault().getLibsDir();
    }

    public static String getLineBreak(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "\r\n"; //NOI18N
        }

        return "\n"; //NOI18N
    }

    // Returns current underlying Java platform
    public static String getLocalJavaPlatform() {
        int jdkVersion = Platform.getJDKVersionNumber();

        if (jdkVersion == Platform.JDK_15) {
            return PLATFORM_JAVA_50;
        } else if (jdkVersion == Platform.JDK_16) {
            return PLATFORM_JAVA_60;
        } else if (jdkVersion == Platform.JDK_17) {
            return PLATFORM_JAVA_70;
        } else if (jdkVersion == Platform.JDK_18) {
            return PLATFORM_JAVA_80;
        }

        return null;
    }

    // Returns locally running OS platform
    public static String getLocalPlatform(int architecture) {
        if (architecture == -1) {
            architecture = Platform.getSystemArchitecture();
        }

        if (architecture == Platform.ARCH_32) {
            if (Platform.isWindows()) {
                return PLATFORM_WINDOWS_OS;
            } else if (Platform.isLinux()) {
                return PLATFORM_LINUX_OS;
            } else if (Platform.isSolarisIntel()) {
                return PLATFORM_SOLARIS_INTEL_OS;
            } else if (Platform.isSolarisSparc()) {
                return PLATFORM_SOLARIS_SPARC_OS;
            } else if (Platform.isMac()) {
                return PLATFORM_MAC_OS;
            }

            return PLATFORM_SOLARIS_SPARC_OS; // Not supported platform => assume UNIX
        } else {
            if (Platform.isWindows()) {
                return PLATFORM_WINDOWS_AMD64_OS;
            } else if (Platform.isLinux()) {
                return PLATFORM_LINUX_AMD64_OS;
            } else if (Platform.isSolarisIntel()) {
                return PLATFORM_SOLARIS_AMD64_OS;
            } else if (Platform.isSolarisSparc()) {
                return PLATFORM_SOLARIS_SPARC64_OS;
            } else if (Platform.isMac()) {
                return PLATFORM_MAC_OS;
            }

            return PLATFORM_SOLARIS_SPARC64_OS; // Not supported platform => assume UNIX
        }
    }

    public static String getManualRemoteStep1(String targetOS, String targetJVM) {
        return MessageFormat.format(MANUAL_REMOTE_STEP1_MESSAGE, new Object[] { "JDK 5.0/6.0/7.0/8.0", targetOS, HTML_REMOTE_STRING }); //NOI18N
    }

    public static String getManualRemoteStep2(String targetOS, String targetJVM) {
        return MessageFormat.format(MANUAL_REMOTE_STEP2_MESSAGE, new Object[] { getRemoteCalibrateCommandString(targetOS, targetJVM) }); //NOI18N
    }

    // Returns getLibsDir()/deployed/jdk<15>/<OS> appropriate for current / selected OS
    public static String getNativeLibrariesPath(String targetPlatform, String targetJVM, boolean isRemote) {
        return getLibsDir(targetPlatform, isRemote) + getDirectorySeparator(targetPlatform) + "deployed" //NOI18N
               + getDirectorySeparator(targetPlatform) + getJavaPlatformNativeLibrariesDirectoryName(targetJVM)
               + getDirectorySeparator(targetPlatform) + getOSPlatformNativeLibrariesDirectoryName(targetPlatform, isRemote);
    }

    // Returns name of the environment variable for system path to Profiler native libraries appropriate for current / selected OS
    public static String getNativePathEnvVariableString(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "Path"; //NOI18N
        }

        return "LD_LIBRARY_PATH"; //NOI18N
    }

    // returns OS platform- and location-specific directory
    public static String getOSPlatformNativeLibrariesDirectoryName(String targetPlatform, boolean isRemote) {
        if (targetPlatform.equals(PLATFORM_WINDOWS_OS)) {
            return "windows"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_WINDOWS_AMD64_OS)) {
            return "windows-amd64"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_LINUX_OS)) {
            return "linux"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_LINUX_AMD64_OS)) {
            return "linux-amd64"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_SOLARIS_INTEL_OS)) {
            return "solaris-i386"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_SOLARIS_AMD64_OS)) {
            return "solaris-amd64"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_SOLARIS_SPARC_OS)) {
            return "solaris-sparc"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_SOLARIS_SPARC64_OS)) {
            return "solaris-sparcv9"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_MAC_OS)) {
            return "mac"; //NOI18N
        }

        return null;
    }

    /**
     * The separator used in the classpath construction
     * @return Returns ";" or ":" according to provided platform
     */ 
    public static String getClassPathSeparator(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return ";"; //NOI18N
        }

        return ":"; //NOI18N
    }

    // Returns extra command line arguments required when attaching on startup
    public static String getProfilerAgentCommandLineArgs(String targetPlatform, String targetJVM, boolean isRemote, int portNumber) {
        return getProfilerAgentCommandLineArgs(targetPlatform, targetJVM, isRemote, portNumber, true);
    }
    
    public static String getProfilerAgentCommandLineArgs(String targetPlatform, String targetJVM, boolean isRemote, int portNumber, boolean createTemp) {
        if ((getNativeLibrariesPath(targetPlatform, targetJVM, isRemote).indexOf(' ') == -1)) {
            return getProfilerAgentCommandLineArgsWithoutQuotes(targetPlatform, targetJVM, isRemote, portNumber); //NOI18N
        }
        if (!isWindowsPlatform(targetPlatform)) { 
            // Profiler is installed in directory with space on Unix (Linux, Solaris, Mac OS X)
            // create temporary link in /tmp directory and use it instead of directory with space
            String libsDirPath = getLibsDir(targetPlatform, isRemote);
            String args = getProfilerAgentCommandLineArgsWithoutQuotes(targetPlatform, targetJVM, isRemote, portNumber);
            return fixLibsDirPath(libsDirPath, args, createTemp);
        }

        return "-agentpath:" + "\"" + getNativeLibrariesPath(targetPlatform, targetJVM, isRemote)
               + getDirectorySeparator(targetPlatform) + getProfilerAgentLibraryFile(targetPlatform) + "=" //NOI18N
               + getLibsDir(targetPlatform, isRemote) + "\"" + "," + portNumber; //NOI18N
    }

    public static String fixLibsDirPath(final String libsDirPath, final String args) {
        return fixLibsDirPath(libsDirPath, args, true);
    }
    
    public static String fixLibsDirPath(final String libsDirPath, final String args, boolean createTmp) {
        if (createTmp) {
            try {
                File tmpFile = File.createTempFile(BINARIES_TMP_PREFIX, BINARIES_TMP_EXT);
                String tmpPath = tmpFile.getAbsolutePath();
                tmpFile.delete();
                Runtime.getRuntime().exec(new String[]{"/bin/ln","-s",libsDirPath,tmpPath});    // NOI18N
                new File(tmpPath).deleteOnExit();
                return args.replace(libsDirPath,tmpPath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return args.replaceAll("agentpath:(.*?)=(.*?),(.*)", "agentpath:\"$1\"=\"$2\",$3");
        }
    }

    // Returns extra command line arguments without additional quotes required when attaching on startup
    public static String getProfilerAgentCommandLineArgsWithoutQuotes(String targetPlatform, String targetJVM, boolean isRemote,
                                                                      int portNumber) {
        StringBuilder args = new StringBuilder();
        
        if ((targetJVM.equals(PLATFORM_JAVA_60) || targetJVM.equals(PLATFORM_JAVA_70) || targetJVM.equals(PLATFORM_JAVA_80)) && 
            (targetPlatform.equals(PLATFORM_LINUX_OS) || targetPlatform.equals(PLATFORM_LINUX_AMD64_OS))) {
            args.append(" -XX:+UseLinuxPosixThreadCPUClocks "); // NOI18N
        }
        args.append("-agentpath:").append(getNativeLibrariesPath(targetPlatform, targetJVM, isRemote)). // NOI18N
               append(getDirectorySeparator(targetPlatform)).append(getProfilerAgentLibraryFile(targetPlatform)).append("="). //NOI18N
               append(getLibsDir(targetPlatform, isRemote)).append(",").append(portNumber); //NOI18N
        return args.toString();
    }
    
    public static String getProfilerAgentCommandLineArgsWithoutQuotes(String targetPlatform, String targetJVM, boolean isRemote,
                                                                      int portNumber, String pathSpaceChar) {
        StringBuilder args = new StringBuilder();
        
        if ((targetJVM.equals(PLATFORM_JAVA_60) || targetJVM.equals(PLATFORM_JAVA_70) || targetJVM.equals(PLATFORM_JAVA_80)) && 
            (targetPlatform.equals(PLATFORM_LINUX_OS) || targetPlatform.equals(PLATFORM_LINUX_AMD64_OS))) {
            args.append(" -XX:+UseLinuxPosixThreadCPUClocks "); // NOI18N
        }
        String natLibs = getNativeLibrariesPath(targetPlatform, targetJVM, isRemote).replace(" ", pathSpaceChar != null ? pathSpaceChar : " ");
        String libsDir = getLibsDir(targetPlatform, isRemote).replace(" ", pathSpaceChar != null ? pathSpaceChar : " ");
        String agentFile = getProfilerAgentLibraryFile(targetPlatform).replace(" ", pathSpaceChar != null ? pathSpaceChar : " ");
        args.append("-agentpath:").append(natLibs). // NOI18N
               append(getDirectorySeparator(targetPlatform)).append(agentFile).append("="). //NOI18N
               append(libsDir).append(",").append(portNumber); //NOI18N
        return args.toString();
    }

    // Returns filename of profiler agent library
    public static String getProfilerAgentLibraryFile(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "profilerinterface.dll"; //NOI18N
        } else if (targetPlatform.equals(PLATFORM_MAC_OS)) {
            return "libprofilerinterface.jnilib"; //NOI18N
        } else {
            return "libprofilerinterface.so"; //NOI18N
        }
    }

    public static String getProfilerModifiedFileHeader(String targetPlatform) {
        return getSilentScriptCommentSign(targetPlatform) + " " + MODIFIED_FOR_PROFILER_STRING + getLineBreak(targetPlatform); //NOI18N
    }

    public static String getProfilerModifiedReplaceFileHeader(String targetPlatform) {
        return getProfilerModifiedFileHeader(targetPlatform) + getSilentScriptCommentSign(targetPlatform) + " " // NOI18N
               + ORIGINAL_BACKUP_LOCATION_STRING + getLineBreak(targetPlatform); //NOI18N
    }

    public static String getRemoteAbsolutePathHint() {
        return MessageFormat.format(REMOTE_ABSOLUTE_PATH_HINT, new Object[] { HTML_REMOTE_STRING });
    }

    // Returns calibration batch filename
    public static String getRemoteCalibrateCommandString(String targetPlatform, String targetJava) {
        String customExt = isLinuxPlatform(targetPlatform) ? (PLATFORM_JAVA_60.equals(targetJava) ? "-16" : "-15") : ""; // NOI18N
        return HTML_REMOTE_STRING + getDirectorySeparator(targetPlatform) + "bin" + getDirectorySeparator(targetPlatform) // NOI18N
               + "calibrate" + getBatchExtensionString(targetPlatform,  customExt); //NOI18N
    }

    // Returns profile batch filename
    public static String getRemoteProfileCommandString(String targetPlatform, String targetJava) {
        String customExt = PLATFORM_JAVA_50.equals(targetJava) ? "-15" : "-16"; // NOI18N
        return HTML_REMOTE_STRING + getDirectorySeparator(targetPlatform) + "bin" + getDirectorySeparator(targetPlatform) // NOI18N
               + "profile" + getBatchExtensionString(targetPlatform, customExt); //NOI18N
    }

    // returns "rem" or "#" according to provided platform
    public static String getScriptCommentSign(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "rem"; //NOI18N
        }

        return "#"; //NOI18N
    }

    // returns "@rem" or "#" according to provided platform
    public static String getSilentScriptCommentSign(String targetPlatform) {
        if (isWindowsPlatform(targetPlatform)) {
            return "@rem"; //NOI18N
        }

        return "#"; //NOI18N
    }

    public static String getSpacesInPathWarning() {
        return SPACES_IN_PATH_WARNING_MSG;
    }

    public static boolean isWindowsPlatform(String targetPlatform) {
        return targetPlatform.equals(PLATFORM_WINDOWS_OS) || targetPlatform.equals(PLATFORM_WINDOWS_AMD64_OS);
    }

    public static boolean isLinuxPlatform(String targetPlatform) {
        return targetPlatform.equals(PLATFORM_LINUX_OS) || targetPlatform.equals(PLATFORM_LINUX_AMD64_OS);
    }

    public static String getXMLCommendEndSign() {
        return "-->"; //NOI18N
    }

    public static String getXMLCommentStartSign() {
        return "<!--"; //NOI18N
    }

    public static boolean backupFile(File file) {
        File source = new File(file.getAbsolutePath());
        File target = new File(source.getAbsolutePath() + FILE_BACKUP_EXTENSION);

        if (!source.exists()) {
            ProfilerLogger.severe(MessageFormat.format(BACKUP_FILE_NOT_FOUND_MESSAGE, new Object[] { source.getAbsolutePath() })); //NOI18N

            return false;
        }

        if (target.exists()) {
            if (!target.delete()) {
                ProfilerLogger.severe(MessageFormat.format(BACKUP_CANNOT_DELETE_FILE_MESSAGE,
                                                           new Object[] { target.getAbsolutePath() })); //NOI18N

                return false;
            }
        }

        // move source to target to correctly preserve file permissions
        if (!source.renameTo(target)) {
            ProfilerLogger.severe(MessageFormat.format(BACKUP_ERROR_MESSAGE,
                                                       new Object[] { source.getAbsolutePath(), target.getAbsolutePath() })); //NOI18N

            return false;
        }

        // re-create source file for further processing
        try {
            source = new File(file.getAbsolutePath());
            source.createNewFile();
            target = new File(source.getAbsolutePath() + FILE_BACKUP_EXTENSION);

            FileChannel sourceChannel = new FileOutputStream(source).getChannel();
            try {
                FileChannel targetChannel = new FileInputStream(target).getChannel();
                try {
                    targetChannel.transferTo(0, targetChannel.size(), sourceChannel);
                    return true;
                } finally {
                    targetChannel.close();
                }
            } finally {
                sourceChannel.close();
            }
        } catch (Exception ex) {
            ProfilerLogger.severe(MessageFormat.format(BACKUP_ERROR_COPY_FILE_MESSAGE,
                                                       new Object[] { target.getAbsolutePath(), source.getAbsolutePath(), ex })); //NOI18N

            return false;
        }
    }

    public static boolean copyFile(File sourceFile, File targetFile) {
        if (!sourceFile.exists()) {
            ProfilerLogger.severe(MessageFormat.format(COPY_FILE_NOT_FOUND_MESSAGE, new Object[] { sourceFile.getAbsolutePath() })); //NOI18N

            return false;
        }

        if (targetFile.exists()) {
            if (!targetFile.delete()) {
                ProfilerLogger.severe(MessageFormat.format(COPY_CANNOT_DELETE_FILE_MESSAGE,
                                                           new Object[] { targetFile.getAbsolutePath() })); //NOI18N

                return false;
            }
        }

        try {
            FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
            try {
                FileChannel destinationChannel = new FileOutputStream(targetFile).getChannel();
                try {
                    sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
                    return true;
                } finally {
                    destinationChannel.close();
                }
            } finally {
                sourceChannel.close();                
            }
        } catch (Exception ex) {
            ProfilerLogger.log(ex);
            ProfilerLogger.severe(MessageFormat.format(COPY_ERROR_MESSAGE,
                                                       new Object[] { sourceFile.getAbsolutePath(), targetFile.getAbsolutePath() })); //NOI18N

            return false;
        }
    }

    public static boolean fileBackupExists(File file) {
        File target = new File(file.getAbsolutePath()); // file to be restored
        File source = new File(target.getAbsolutePath() + FILE_BACKUP_EXTENSION); // backup image of this file (file.backup)

        return source.exists();
    }

    public static boolean restoreFile(File file) {
        File target = file;
        File source = new File(target.getAbsolutePath() + FILE_BACKUP_EXTENSION);

        if (!source.exists()) {
            ProfilerLogger.severe(MessageFormat.format(RESTORE_FILE_NOT_FOUND_MESSAGE,
                                                       new Object[] { source.getAbsolutePath(), target.getAbsolutePath() })); //NOI18N

            return false;
        }

        if (target.exists()) {
            if (!target.delete()) {
                ProfilerLogger.severe(MessageFormat.format(RESTORE_CANNOT_DELETE_FILE_MESSAGE,
                                                           new Object[] { target.getAbsolutePath() })); //NOI18N

                return false;
            }
        }

        if (!source.renameTo(target)) {
            ProfilerLogger.severe(MessageFormat.format(RESTORE_ERROR_MESSAGE,
                                                       new Object[] { source.getAbsolutePath(), target.getAbsolutePath() })); //NOI18N

            return false;
        }

        return true;
    }
    
    public static String getTemporaryBinariesLink(String agentCmds) {
        Pattern p = Pattern.compile("(/.*?" + BINARIES_TMP_PREFIX + ".*?" + BINARIES_TMP_EXT + ")");
        Matcher m = p.matcher(agentCmds);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }
}
