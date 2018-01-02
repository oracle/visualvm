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

package com.sun.tools.visualvm.sa;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.coredump.CoreDump;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModelFactory;
import com.sun.tools.visualvm.tools.sa.SaModel;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
public class SaModelProvider extends AbstractModelProvider<SaModel, DataSource>  {
    private static final Logger LOGGER = Logger.getLogger(SaModelProvider.class.getName());
    
    private static final String SA_JAR = "lib/sa-jdi.jar";  // NOI18N

    SaModelProvider() {
    }
    
    public SaModel createModelFor(DataSource ds) {
        if (ds instanceof Application) {
            Application app = (Application) ds;
            if (Host.LOCALHOST.equals(app.getHost())) {
                JvmJvmstatModel jvmstat = JvmJvmstatModelFactory.getJvmstatModelFor(app);
            
                if (jvmstat != null && Utilities.isWindows()) {
                    // on Windows, SA can only attach to the process of the same
                    // architecture ( 32bit / 64bit )
                    Boolean this64bitArch = is64BitArchitecture();
                    Boolean app64bitArch = is64BitArchitecture(jvmstat);
                    if (this64bitArch != null && app64bitArch != null) {
                        if (!this64bitArch.equals(app64bitArch)) {
                            return null;
                        }
                    }
                }
                File jdkHome = getJdkHome(jvmstat);
                File saJar = getSaJar(jdkHome);

                if (saJar == null) {
                    return null;
                }
                try {
                    return new SaModelImpl(jdkHome,saJar,app.getPid());
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO, "Error getting SA agent", ex);   // NOI18N
                }
                return null;
            }
        } else if (ds instanceof CoreDump) {
            CoreDump coredump = (CoreDump) ds;
            File executable = new File(coredump.getExecutable());
            File coreFile = coredump.getFile();
            if (executable.exists() && coreFile.exists()) {
                File jdkHome = executable.getParentFile().getParentFile();
                File saJar = getSaJar(jdkHome);
                
                if (saJar == null) {
                    return null;
                }
                try {
                    return new SaModelImpl(jdkHome,saJar,executable,coreFile);
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO, "Unable to retrieve SA agent", ex);  // NOI18N
                }
                return null;
            }
        }
        return null;
    }

    private File getJdkHome(final JvmJvmstatModel jvmstat) {
        String javaHome = jvmstat.getJavaHome();
        if (javaHome != null) {
            File jdkHome = new File(javaHome);
            if ("jre".equals(jdkHome.getName())) {  // NOI18N
               jdkHome = jdkHome.getParentFile();
            }
            return jdkHome;
        }
        return null;
    }
    
    static File getSaJar(File jdkHome) {
        if (jdkHome != null) {
            File saJar = new File(jdkHome,SA_JAR);
            try {
                if (saJar.exists()) {
                    return saJar.getCanonicalFile();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, saJar.getPath(), ex);
            }
        }
        return null;
    }
    
    private static Boolean is64BitArchitecture(JvmJvmstatModel jvmstat) {
        String name = jvmstat.getVmName();
        if (name != null) {
            return name.toLowerCase().contains("64-bit");   // NOI18N
        }
        return null;
    }
    
    private static Boolean is64BitArchitecture() {
        String thisArch = System.getProperty("sun.arch.data.model");    // NOI18N
        if (thisArch != null) {
            return Boolean.valueOf("64".equals(thisArch));  // NOI18N
        }
        return null;
    }
    
}
