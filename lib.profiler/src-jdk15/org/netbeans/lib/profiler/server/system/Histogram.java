/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.server.system;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author Tomas Hurka
 */
public class Histogram {
    private static final String LIVE_OBJECTS_OPTION = "-live";  // NOI18N
    private static final String ALL_OBJECTS_OPTION = "-all";    // NOI18N    
    private static final String VIRTUAL_MACHINE_CLASS = "com.sun.tools.attach.VirtualMachine"; // NOI18N
    private static final String HS_VIRTUAL_MACHINE_CLASS = "sun.tools.attach.HotSpotVirtualMachine"; // NOI18N
    private static final String VIRTUAL_MACHINE_ATTACH_METHOD = "attach";   // NOI18N
    private static final String VIRTUAL_MACHINE_HEAPHISTO_METHOD = "heapHisto";   // NOI18N    
    private static String selfPid;
    private static Method vmAttach;
    private static Method vmHisto;
    private static Object virtualMachine;
    
    public static boolean initialize() {
        try {
            Class vmClass;
            ClassLoader toolsJar = null;
            try {
                vmClass = Class.forName(VIRTUAL_MACHINE_CLASS);
            } catch (ClassNotFoundException ex) {
                toolsJar = getToolsJar();
                if (toolsJar == null) {
                    return false;
                }
                vmClass = Class.forName(VIRTUAL_MACHINE_CLASS, true, toolsJar);
            }
            Class hsVmClass = toolsJar == null ? Class.forName(HS_VIRTUAL_MACHINE_CLASS) : Class.forName(HS_VIRTUAL_MACHINE_CLASS, true, toolsJar);
            vmAttach = vmClass.getMethod(VIRTUAL_MACHINE_ATTACH_METHOD, String.class);
            vmHisto = hsVmClass.getMethod(VIRTUAL_MACHINE_HEAPHISTO_METHOD, Object[].class);
        } catch (NoSuchMethodException ex) {
            return false;
        } catch (SecurityException ex) {
            return false;
        } catch (ClassNotFoundException ex) {
            return false;
        }
        String selfName = ManagementFactory.getRuntimeMXBean().getName();
        selfPid = selfName.substring(0, selfName.indexOf('@')); // NOI18N
        return true;
    }
    
    public static InputStream getRawHistogram() {
        try {
            if (virtualMachine == null) {
                virtualMachine = vmAttach.invoke(null, selfPid);
            }
            Object ret = vmHisto.invoke(virtualMachine, new Object[]{new Object[]{ALL_OBJECTS_OPTION}});
            if (ret instanceof InputStream) {
                return (InputStream)ret;
            }
        } catch (IllegalAccessException ex) {
            return null;
        } catch (IllegalArgumentException ex) {
            return null;
        } catch (InvocationTargetException ex) {
            return null;
        }
        return null;
    }

    private static ClassLoader getToolsJar() {
        File home = getJavaHome();
        File toolsJar = new File(home,"lib/tools.jar"); // NOI18N
        if (toolsJar.exists() && toolsJar.isFile()) {
            try {
                return new URLClassLoader(new URL[] {toolsJar.toURI().toURL()});
            } catch (MalformedURLException ex) {
                return null;
            }
        }
        return null;
    }
    
    private static File getJavaHome() {
        File jdkHome = new File(System.getProperty("java.home")); // NOI18N
        if ("jre".equals(jdkHome.getName())) {  // NOI18N
           jdkHome = jdkHome.getParentFile(); 
        }
        return jdkHome;
    }
}
