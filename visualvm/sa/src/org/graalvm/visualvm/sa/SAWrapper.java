/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sa;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

/**
 *
 * @author Tomas Hurka
 */
class SAWrapper {
    private static final Logger LOGGER = Logger.getLogger(SAWrapper.class.getName());
    
    URLClassLoader loader;
    File libraryPath;
    
    SAWrapper(File jdkHome, File saLibFile) throws MalformedURLException {
        // By default SA agent classes prefer dbx debugger to proc debugger
        // and Windows process debugger to windbg debugger. SA expects
        // special properties to be set to choose other debuggers.
        // We will set those here before attaching to SA agent.
        System.setProperty("sun.jvm.hotspot.debugger.useProcDebugger", "true"); // NOI18N
        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");   // NOI18N
        URL[] saLibUrls;
        if (saLibFile.getName().endsWith(".jmod")) {        // NOI18N
            URL jmodUrl = new URL("jar:file:///"+saLibFile.getAbsolutePath()+"!/classes/");     // NOI18N
            saLibUrls = new URL[]{jmodUrl};
            libraryPath = new File(jdkHome, "lib/");   // NOI18N
        } else {
            saLibUrls = new URL[]{saLibFile.toURI().toURL()};
            String osArch = System.getProperty("os.arch");  // NOI18N
            if ("x86".equals(osArch)) {
                osArch = "i386";
            }
            libraryPath = new File(jdkHome, "jre/lib/" + osArch);   // NOI18N
        }
        LOGGER.fine("Path " + libraryPath.getAbsolutePath());   // NOI18N
        loader = new URLClassLoader(saLibUrls) {
            @Override
            protected String findLibrary(String libname) {
                String name = System.mapLibraryName(libname);
                File library = new File(libraryPath, name);
                LOGGER.fine("Library " + library.getAbsolutePath());    // NOI18N
                if (library.exists() && library.canRead()) {
                    return library.getAbsolutePath();
                }
                return super.findLibrary(libname);
            }
        };
    }
    
    Class classForName(String name) throws ClassNotFoundException {
        return Class.forName(name,true,loader);
    }
    
    Class Tool() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.tools.Tool");  // NOI18N
    }
    
    Class VM() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.runtime.VM");  // NOI18N
    }
    
    Class HotSpotAgent() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.HotSpotAgent");    // NOI18N
    }
    
    Class HeapHprofBinWriter() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.utilities.HeapHprofBinWriter");    // NOI18N
    }
    
    Class Arguments() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.runtime.Arguments");   // NOI18N
    }
}
