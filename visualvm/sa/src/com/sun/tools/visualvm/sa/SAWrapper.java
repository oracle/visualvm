/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.sa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

/**
 *
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
class SAWrapper {
    private static final Logger LOGGER = Logger.getLogger(SAWrapper.class.getName());
    
    URLClassLoader loader;
    File libraryPath;
    
    SAWrapper(File jdkHome, File saJarFile) throws MalformedURLException {
        // By default SA agent classes prefer dbx debugger to proc debugger
        // and Windows process debugger to windbg debugger. SA expects
        // special properties to be set to choose other debuggers.
        // We will set those here before attaching to SA agent.
        System.setProperty("sun.jvm.hotspot.debugger.useProcDebugger", "true");
        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");
        URL[] saJarUrls = new URL[]{saJarFile.toURI().toURL()};
        String osArch = System.getProperty("os.arch");
        if ("x86".equals(osArch)) {
        }
        libraryPath = new File(jdkHome, "jre/lib/" + osArch);
        LOGGER.fine("Path " + libraryPath.getAbsolutePath());
        loader = new URLClassLoader(saJarUrls) {
            @Override
            protected String findLibrary(String libname) {
                String name = System.mapLibraryName(libname);
                File library = new File(libraryPath, name);
                LOGGER.fine("Library " + library.getAbsolutePath());
                if (library.exists() && library.canRead()) {
                    String absPath = temporaryLibrary(library);
                    if (absPath != null) {
                        return absPath;
                    }
                }
                return super.findLibrary(libname);
            }
            
            /**
             * Copy the specified native library into the temporary directory
             * and return the absolute path. Returns null, if we fail to copy
             * the library.
             */
            private synchronized String temporaryLibrary(File library) {
                try {
                    InputStream is = new FileInputStream(library);
                    if (is != null) {
                        File file =
                                File.createTempFile(library.getName() + ".", null);
                        file.deleteOnExit();
                        FileOutputStream fileOutput = new FileOutputStream(file);
                        int c;
                        while ((c = is.read()) != -1) {
                            fileOutput.write(c);
                        }
                        is.close();
                        fileOutput.close();
                        if (file.exists()) {
                            return file.getAbsolutePath();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning("Failed to load library (" +
                            library.getName() + "): " + e);
                    return null;
                }
                return null;
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
    
    Class BugspotAgent() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.bugspot.BugSpotAgent");    // NOI18N
    }
    
    Class HeapHprofBinWriter() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.utilities.HeapHprofBinWriter");    // NOI18N
    }
    
    Class Arguments() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.runtime.Arguments");
    }
}
