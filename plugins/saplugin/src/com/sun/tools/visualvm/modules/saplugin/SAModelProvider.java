/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.saplugin;

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


public class SAModelProvider extends AbstractModelProvider<SaModel, DataSource>  {
    private static final Logger LOGGER = Logger.getLogger(SAModelProvider.class.getName());
    
    private static final String SA_JAR = "lib/sa-jdi.jar";  // NOI18N
    
    
    SAModelProvider() {
        
    }
    
    public SaModel createModelFor(DataSource ds) {
        if (ds instanceof Application) {
            Application app = (Application) ds;
            if (Host.LOCALHOST.equals(app.getHost())) {
                JvmJvmstatModel jvmstat = JvmJvmstatModelFactory.getJvmstatModelFor(app);
                File jdkHome = new File(jvmstat.getJavaHome());
                if ("jre".equals(jdkHome.getName())) {
                    jdkHome = jdkHome.getParentFile();
                }
                File saJar = getSaJar(jdkHome);

                if (saJar == null) {
                    return null;
                }
                try {
                    return new SAModelImpl(jdkHome,saJar,app.getPid());
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO, "Error getting SA agent", ex);   // NOI18N
                }                
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
                    return new SAModelImpl(jdkHome,saJar,executable,coreFile);
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO, "Unable to retrieve SA agent", ex);  // NOI18N
                }
               
            }
        }
        return null;
    }
    
    static File getSaJar(File jdkHome) {
        File saJar = new File(jdkHome,SA_JAR);
        try {
            if (saJar.exists()) {
                return saJar.getCanonicalFile();
            }
            saJar = new File( SA_JAR);  //Changed this
            if (saJar.exists()) {
                return saJar.getCanonicalFile();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, saJar.getPath(), ex);            
        }
        return null;
    }
    
      
}
