/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

/**
 *
 * @author Jiri Sedlacek
 */
final class JavaInfo {
    
    static String getCurrentJDKExecutable() {
        return getJDKExecutable(System.getProperty("java.home")); // NOI18N
    }
    
    static String getJDKExecutable(String jdkHome) {
        if (jdkHome == null || jdkHome.trim().length() == 0) return null;
        String jreSuffix = File.separator + "jre"; // NOI18N
        if (jdkHome.endsWith(jreSuffix)) jdkHome = jdkHome.substring(0, jdkHome.length() - jreSuffix.length());
        String jdkExe = jdkHome + File.separator + "bin" + File.separator + "java" + (Platform.isWindows() ? ".exe" : ""); // NOI18N
        return jdkExe;
    }    
    
    static String[] getSystemProperties(File java, String... keys) {
        if (keys.length == 0) return new String[0];
        
        try {
            List<String> list = new ArrayList();
            list.add(java.getAbsolutePath());
            list.add("-jar"); // NOI18N
            list.add(getProbeJar());
            list.addAll(Arrays.asList(keys));

            Process p = Runtime.getRuntime().exec(list.toArray(new String[list.size()]));
            
            list.clear();
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            while (line != null) {
                list.add(line);
                line = br.readLine();
            }
            br.close();
            
            return list.toArray(new String[list.size()]);
        } catch (Throwable t) {
            System.err.println("Error getting system properties from " + java.toString() + ": " + t.getMessage()); // NOI18N
            t.printStackTrace(System.err);
            return null;
        }
    }
    
    
    private static final String PROBE_PATH = "modules/ext/profilerprobe.jar";   // NOI18N
    private static String PROBE_JAR;
    private static synchronized String getProbeJar() {
        if (PROBE_JAR == null) {
            InstalledFileLocator loc = InstalledFileLocator.getDefault();
            ModuleInfo info = Modules.getDefault().ownerOf(JavaInfo.class);
            File jar = loc.locate(PROBE_PATH, info.getCodeNameBase(), false);
            PROBE_JAR = jar.getAbsolutePath();
        }
        return PROBE_JAR;
    }
    
    private JavaInfo() {}
    
}
