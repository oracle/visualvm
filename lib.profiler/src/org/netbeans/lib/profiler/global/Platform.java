/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.global;

import java.io.File;
import java.io.IOException;


/**
 * Determination of the current platform (OS, hardware) and related services.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class Platform implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    /**
     * Operating system is Windows NT.
     */
    public static final int OS_WINNT = 1;

    /**
     * Operating system is Windows 95.
     */
    public static final int OS_WIN95 = 2;

    /**
     * Operating system is Windows 98.
     */
    public static final int OS_WIN98 = 4;

    /**
     * Operating system is Solaris.
     */
    public static final int OS_SOLARIS = 8;

    /**
     * Operating system is Linux.
     */
    public static final int OS_LINUX = 16;

    /**
     * Operating system is HP-UX.
     */
    public static final int OS_HP = 32;

    /**
     * Operating system is IBM AIX.
     */
    public static final int OS_AIX = 64;

    /**
     * Operating system is SGI IRIX.
     */
    public static final int OS_IRIX = 128;

    /**
     * Operating system is Sun OS.
     */
    public static final int OS_SUNOS = 256;

    /**
     * Operating system is Compaq TRU64 Unix
     */
    public static final int OS_TRU64 = 512;

    /**
     * Operating system is OS/2.
     */
    public static final int OS_OS2 = 1024;

    /**
     * Operating system is Mac.
     */
    public static final int OS_MAC = 2048;

    /**
     * Operating system is Windows 2000.
     */
    public static final int OS_WIN2000 = 4096;

    /**
     * Operating system is Compaq OpenVMS
     */
    public static final int OS_VMS = 8192;

    /**
     * Operating system is one of the Windows variants but we don't know which one it is
     */
    public static final int OS_WIN_OTHER = 16384;

    /**
     * Operating system is unknown.
     */
    public static final int OS_OTHER = 65536;

    /**
     * A mask for Windows platforms.
     */
    public static final int OS_WINDOWS_MASK = OS_WINNT | OS_WIN95 | OS_WIN98 | OS_WIN2000 | OS_WIN_OTHER;

    /**
     * A mask for Unix platforms.
     */
    public static final int OS_UNIX_MASK = OS_SOLARIS | OS_LINUX | OS_HP | OS_AIX | OS_IRIX | OS_SUNOS | OS_TRU64 | OS_MAC;

    /**
     * The operating system on which the tool runs
     */
    private static int operatingSystem = -1;
    private static String jdkDenoteString;
    private static int jdkVersion;
    private static int sysArch; // 32/64bit architecture

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Given the name of the directory containing the JFluid native libaries (either just the root JFluid libs dir,
     * or really the full path, depending on fullPathToLibSpecified parameter), return the full platform-dependent
     * name for the "profilerinterface" library contained in that directory. If non-null jdkString is specified, it
     * is used in the resulting path; otherwise the VM is queried for its version and the resulting string is used.
     *
     * @param fullAgentPath          The path to the library
     * @param fullPathToLibSpecified whether or not a full path is specified (ending at the platform level)
     * @param jdkString              CommonConstants.JDK_15_STRING
     * @return A path to the native library to be used for this platform
     */
    public static String getAgentNativeLibFullName(String fullAgentPath, boolean fullPathToLibSpecified, String jdkString,
                                                   int architecture) {
        boolean is64bitArch;

        if (jdkString == null) {
            jdkString = getJDKVersionString();
        }

        if (architecture == -1) {
            architecture = getSystemArchitecture();
        }

        is64bitArch = architecture == ARCH_64;

        if (jdkString.equals(JDK_17_STRING)) {
            // for now, we use the same libs for 1.6 and 1.7 
            jdkString = JDK_16_STRING;
        }

        String libPrefix = ""; // NOI18N

        if (!isWindows()) { // Mac and UNIXes
            libPrefix = "lib"; // NOI18N
        }

        String libSuffix = ""; // NOI18N

        if (isWindows()) {
            libSuffix = ".dll"; // Windows // NOI18N
        } else if (isMac()) {
            libSuffix = ".jnilib"; // Mac // NOI18N
        } else if (isHpux()) {
            libSuffix = ".sl"; // HP-UX // NOI18N
        } else {
            libSuffix = ".so"; // UNIXes // NOI18N
        }

        String libSubPath = "/"; // NOI18N

        if (!fullPathToLibSpecified) {
            String libSubDir;
            if (isWindows()) {
               libSubDir = "windows"; // NOI18N
            } else if (isMac()) {
               libSubDir = "mac";
            } else if (isLinux()) {
               libSubDir = "linux"; // NOI18N
            } else if (isHpux()) {
               libSubDir = "hpux"; // NOI18N
            } else {
               libSubDir = "solaris"; // NOI18N
            }
            String procArch = null;

            if (is64bitArch) {
                if (isLinux() || isWindows() || isSolarisIntel()) {
                    procArch = "amd64"; // NOI18N
                } else if (isSolarisSparc()) {
                    procArch = "sparcv9"; // NOI18N
                } else if (isHpux()) {
                    procArch = "pa_risc2.0w"; // NOI18N
                }
            } else { // 32bit

                if (isSolarisIntel()) {
                    procArch = "i386"; // NOI18N
                } else if (isSolarisSparc()) {
                    procArch = "sparc"; // NOI18N
                } else if (isHpux()) {
                    procArch = "pa_risc2.0"; // NOI18N
                }
            }

            if (procArch != null) {
                libSubDir += ("-" + procArch);
            }

            libSubPath = "/deployed/" + jdkString + "/" + libSubDir + "/"; // NOI18N
        }

        String fullPath = fullAgentPath;

        if (fullAgentPath.startsWith("\"")) { // NOI18N
            fullPath = fullAgentPath.substring(1, fullAgentPath.length() - 1);
        }

        fullPath = fullPath.replace('\\', '/'); // NOI18N

        return fullPath + libSubPath + libPrefix + "profilerinterface" + libSuffix; // NOI18N
    }

    /**
     * Returns JDK minor version
     */
    public static int getJDKMinorNumber(String jdkVersionString) {
        if (jdkVersionString == null) {
            return 0;
        }

        int minorIndex = jdkVersionString.lastIndexOf('_'); // NOI18N

        if ((minorIndex > 0) && (minorIndex < (jdkVersionString.length() - 1))) {
            String minorString = jdkVersionString.substring(minorIndex + 1);
            int subverIndex = minorString.indexOf('-'); // NOI18N

            if (subverIndex != -1) {
                minorString = minorString.substring(0, subverIndex);
            }

            return Integer.parseInt(minorString);
        }

        return 0;
    }

    /**
     * Returns the JFluid-internal JDK version number
     */
    public static int getJDKVersionNumber() {
        if (jdkVersion == 0) {
            String javaVersion = System.getProperty("java.version"); // NOI18N

            if (javaVersion.startsWith("1.5")) { // NOI18N
                jdkVersion = JDK_15;
            } else if (javaVersion.startsWith("1.6")) { // NOI18N
                jdkVersion = JDK_16;
            } else if (javaVersion.startsWith("1.7")) { // NOI18N
                jdkVersion = JDK_17;
            } else if (javaVersion.startsWith("phoneme_advanced")) { // NOI18N
                jdkVersion = JDK_CVM;
            } else {
                jdkVersion = JDK_UNSUPPORTED;
            }
        }

        return jdkVersion;
    }

    /**
     * Returns the string for, essentially, JFluid directory corresponding to a particular JDK version the TA runs on.
     * Currently it's "jdk15" for JDK 1.5 version and "jdk16" for JDK 1.6 version.
     */
    public static String getJDKVersionString(String javaVersionString) {
        if (javaVersionString == null) {
            return JDK_UNSUPPORTED_STRING;
        }

        if (javaVersionString.startsWith("1.5")) { // NOI18N

            return JDK_15_STRING;
        } else if (javaVersionString.startsWith("1.6")) { // NOI18N

            return JDK_16_STRING;
        } else if (javaVersionString.startsWith("1.7")) { // NOI18N

            return JDK_17_STRING;
         } else if (javaVersionString.startsWith("phoneme_advanced")) {// NOI18N
	    return JDK_CVM_STRING;
        } else {
            return JDK_UNSUPPORTED_STRING;
        }
    }

    /**
     * Returns the string for, essentially, JFluid directory corresponding to a particular JDK version the TA runs on.
     * Currently it's "jdk15" for JDK 1.5 version, "jdk16" for JDK 1.6 version and jdk17 for JDK 1.7 version.
     */
    public static String getJDKVersionString() {
        if (jdkDenoteString == null) {
            jdkDenoteString = getJDKVersionString(System.getProperty("java.version")); // NOI18N
        }

        return jdkDenoteString;
    }

    public static String getJFluidNativeLibDirName(String fullJFluidPath, String jdkString, int architecture) {
        String jFluidNativeLibFullName = getAgentNativeLibFullName(fullJFluidPath, false, jdkString, architecture);

        return jFluidNativeLibFullName.substring(0, jFluidNativeLibFullName.lastIndexOf('/')); // NOI18N
    }

    /**
     * Test whether we are running on Linux
     */
    public static boolean isLinux() {
        return (getOperatingSystem() == OS_LINUX);
    }

    /**
     * Test whether the supplied OS name is Linux
     */
    public static boolean isLinux(String osName) {
        return (getOperatingSystem(osName) == OS_LINUX);
    }

    public static boolean isMac() {
        return (getOperatingSystem() == OS_MAC);
    }
    
    public static boolean isHpux() {
        return (getOperatingSystem() == OS_HP);
    }
    
    /**
     * Get the operating system on which we are is running.
     * Returns one of the <code>OS_*</code> constants (such as {@link #OS_WINNT})
     */
    public static int getOperatingSystem() {
        if (operatingSystem == -1) {
            String osName = System.getProperty("os.name"); // NOI18N
            operatingSystem = getOperatingSystem(osName);
        }

        return operatingSystem;
    }

    public static int getOperatingSystem(String osName) {
        if ("Windows NT".equals(osName)) { // NOI18N

            return OS_WINNT;
        } else if ("Windows 95".equals(osName)) { // NOI18N

            return OS_WIN95;
        } else if ("Windows 98".equals(osName)) { // NOI18N

            return OS_WIN98;
        } else if ("Windows 2000".equals(osName)) { // NOI18N

            return OS_WIN2000;
        } else if (osName.startsWith("Windows ")) { // NOI18N

            return OS_WIN_OTHER;
        } else if ("Solaris".equals(osName)) { // NOI18N

            return OS_SOLARIS;
        } else if (osName.startsWith("SunOS")) { // NOI18N

            return OS_SOLARIS;
        } else if (osName.endsWith("Linux")) { // NOI18N

            return OS_LINUX;
        } else if ("HP-UX".equals(osName)) { // NOI18N

            return OS_HP;
        } else if ("AIX".equals(osName)) { // NOI18N

            return OS_AIX;
        } else if ("Irix".equals(osName)) { // NOI18N

            return OS_IRIX;
        } else if ("SunOS".equals(osName)) { // NOI18N

            return OS_SOLARIS;
        } else if ("Digital UNIX".equals(osName)) { // NOI18N

            return OS_TRU64;
        } else if ("OS/2".equals(osName)) { // NOI18N

            return OS_OS2;
        } else if ("OpenVMS".equals(osName)) { // NOI18N

            return OS_VMS;
        } else if (osName.equalsIgnoreCase("mac os x")) { // NOI18N

            return OS_MAC;
        } else if (osName.startsWith("Darwin")) { // NOI18N

            return OS_MAC;
        } else {
            return OS_OTHER;
        }
    }

    public static String getProfilerUserDir() throws IOException {
        String customDir = System.getProperty("nbprofiler.home"); // NOI18N

        if (customDir != null) {
            File d = new File(customDir);

            if (!d.exists()) {
                if (!d.mkdir()) {
                    throw new IOException("Could not create directory" + customDir); // NOI18N
                }
            }

            return customDir;
        } else {
            // use default location
            String dir = System.getProperty("user.home") + File.separator + ".nbprofiler"; // NOI18N
            File d = new File(dir);

            if (!d.exists()) {
                if (!d.mkdir()) {
                    throw new IOException("Could not create directory" + dir); // NOI18N
                }
            }

            return dir;
        }
    }

    /**
     * Test whether we are running on Solaris
     */
    public static boolean isSolaris() {
        return (getOperatingSystem() == OS_SOLARIS);
    }

    /**
     * Test whether we are running on Solaris on Intel processor
     */
    public static boolean isSolarisIntel() {
        String procArch = System.getProperty("os.arch"); // NOI18N

        return isSolaris() && (procArch.endsWith("86") || procArch.equals("amd64")); // NOI18N
    }

    /**
     * Test whether we are running on Solaris on SPARC processor
     */
    public static boolean isSolarisSparc() {
        String procArch = System.getProperty("os.arch"); // NOI18N

        return isSolaris() && procArch.startsWith("sparc"); // NOI18N
    }

    /**
     * Returns system architecture: 32/64bit
     */
    public static int getSystemArchitecture() {
        if (sysArch == 0) {
            String architecture = System.getProperty("sun.arch.data.model"); // NOI18N

            if (architecture == null) {
                sysArch = ARCH_32;
            } else if ("32".equals(architecture)) { // NOI18N
                sysArch = ARCH_32; // defined 32bit environment
            } else if ("64".equals(architecture)) { // NOI18N
                sysArch = ARCH_64; // defined 64bit environment
            } else {
                sysArch = ARCH_32; // unknown environment, 32bit by default
            }
        }

        return sysArch;
    }

    /**
     * Test whether we are running on some variant of Unix. Linux is included as well as the commercial vendors.
     */
    public static boolean isUnix() {
        return (getOperatingSystem() & OS_UNIX_MASK) != 0;
    }

    /**
     * Test whether we are is running on some variant of Windows
     */
    public static boolean isWindows() {
        return (getOperatingSystem() & OS_WINDOWS_MASK) != 0;
    }

    /**
     * Test whether the supplied OS name is some variant of Windows
     */
    public static boolean isWindows(String osName) {
        return (getOperatingSystem(osName) & OS_WINDOWS_MASK) != 0;
    }

    /**
     * Returns true if current system architecture is 32bit
     */
    public static boolean is32bitArchitecture() {
        return getSystemArchitecture() == ARCH_32;
    }

    /**
     * Returns true if current system architecture is 64bit
     */
    public static boolean is64bitArchitecture() {
        return getSystemArchitecture() == ARCH_64;
    }

    /**
     * Returns true if the given JVM version supports dynaimc attach
     */
    public static boolean supportsDynamicAttach(String jdkVersionString) {
        return CommonConstants.JDK_16_STRING.equals(jdkVersionString) || CommonConstants.JDK_17_STRING.equals(jdkVersionString);
    }

    /**
     * Returns true if the given JVM version passed as String correctly reports "sleeping" state
     */
    public static boolean supportsThreadSleepingStateMonitoring(String jdkVersionString) {
        return ((jdkVersionString != null)
               && (jdkVersionString.equals(JDK_15_STRING) ||
		   jdkVersionString.equals(JDK_16_STRING) ||
		   jdkVersionString.equals(JDK_17_STRING) ||
		   jdkVersionString.equals(JDK_CVM_STRING)));
    }

    /**
     * Returns true if the current JVM correctly reports "sleeping" state
     */
    public static boolean thisVMSupportsThreadSleepingStateMonitoring() {
        return supportsThreadSleepingStateMonitoring(getJDKVersionNumber());
    }

    /**
     * Returns true if the given JVM version number correctly reports "sleeping" state
     */
    private static boolean supportsThreadSleepingStateMonitoring(int jdkVersionNumber) {
        return ((jdkVersionNumber == JDK_15) ||
		(jdkVersionNumber == JDK_16) ||
		(jdkVersionNumber == JDK_17) ||
		(jdkVersionNumber == JDK_CVM));
    }
}
