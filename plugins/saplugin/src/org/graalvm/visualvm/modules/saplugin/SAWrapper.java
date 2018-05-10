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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;


class SAWrapper {
    private static final Logger LOGGER = Logger.getLogger(SAWrapper.class.getName());
    
    URLClassLoader loader;
    private static ClassLoader parent;
    File libraryPath;
    
    SAWrapper(File jdkHome, File saJarFile) throws MalformedURLException {
        // By default SA agent classes prefer dbx debugger to proc debugger
        // and Windows process debugger to windbg debugger. SA expects
        // special properties to be set to choose other debuggers.
        // We will set those here before attaching to SA agent.
        System.setProperty("sun.jvm.hotspot.debugger.useProcDebugger", "true"); // NOI18N
        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");   // NOI18N
        //File maf = new File(jdkHome, "lib/maf-1_0.jar");
        URL[] saJarUrls = new URL[]{saJarFile.toURI().toURL()/*, maf.toURI().toURL()*/};
        
//        String osArch = System.getProperty("os.arch");  // NOI18N
//        if ("x86".equals(osArch)) {
//            osArch = "i386";
//        }
        //libraryPath = new File("lib/" + osArch);   // NOI18N   /// Changed this
        libraryPath = new File(jdkHome, "jre/bin/");
        LOGGER.fine("Path " + libraryPath.getAbsolutePath());   // NOI18N
        
        //We want only one parent for all the SAPluginClassLoader instances.
        if (parent == null)
            parent = new URLClassLoader(new URL[]{saJarFile.toURI().toURL()});
        loader = new SAPluginClassLoader(parent, jdkHome, saJarFile);
        
/*        loader = new URLClassLoader(saJarUrls, parent) {
            @Override
            protected String findLibrary(String libname) {
                String name = System.mapLibraryName(libname);
                File library = new File(libraryPath, name);
                LOGGER.fine("Library " + library.getAbsolutePath());
                String f = library.getAbsolutePath();// NOI18N
                
                if (library.exists() && library.canRead()) {
                    //return "D:/Java/jdk1.6.0_03/jre/lib/i386/sawindbg.dll";
                    return library.getAbsolutePath();
                }
                return super.findLibrary(libname);
            }

            @Override
            public synchronized Class loadClass(String name)
                    throws ClassNotFoundException {
                // First, check if the class has already been loaded
                Class c = findLoadedClass(name);
                if (c == null) {
                    /* If we are loading any class in 'sun.jvm.hotspot.'  or any of the
                     * sub-packages (except for 'debugger' sub-pkg. please refer below),
                     * we load it by 'this' loader. Or else, we forward the request to
                     * 'parent' loader, system loader etc. (rest of the code follows
                     * the patten in java.lang.ClassLoader.loadClass).
                     *
                     * 'sun.jvm.hotspot.debugger.' and sub-package classes are
                     * also loaded by parent loader. This is done for two reasons:
                     *
                     * 1. to avoid code bloat by too many classes.
                     * 2. to avoid loading same native library multiple times
                     *    from multiple class loaders (which results in getting a
                     *    UnsatisifiedLinkageError from System.loadLibrary).
                     *

                    if (name.startsWith("sun.jvm.hotspot.") &&
                            !name.startsWith("sun.jvm.hotspot.debugger.")) {
                        return findClass(name);
                    }
                    if (name.startsWith("sun.jvm.hotspot.debugger.")) {
                        c = parent.loadClass(name);
                    } else  if (parent != null) {
                        c = parent.loadClass(name);
                    } else {
                        c = findSystemClass(name);
                    }
                }
                return c;
            }
        };
    }
*/
    }
    Class classForName(String name) throws ClassNotFoundException {
       return Class.forName(name, true, loader);
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

    Class HotspotAgent() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.HotSpotAgent");    // NOI18N
    }

    Class HeapHprofBinWriter() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.utilities.HeapHprofBinWriter");    // NOI18N
    }

    Class Arguments() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.runtime.Arguments");   // NOI18N
    }

    ///////////////////////////////////
    Class OopInspector() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.Inspector");    // NOI18N
    }

    Class JavaStackTracePanel() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.JavaStackTracePanel");    // NOI18N
    }

    Class JavaThreadsPanel() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.JavaThreadsPanel");    // NOI18N
    }

    Class SAListener() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.SAListener");    // NOI18N
    }

    Class OopTreeNodeAdapter() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.tree.OopTreeNodeAdapter");    // NOI18N
    }

    Class SimpleTreeNode() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.tree.SimpleTreeNode");    // NOI18N
    }

    Class CodeViewerPanel() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.classbrowser.CodeViewerPanel");    // NOI18N
    }
    Class FindPanel() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.FindPanel");    // NOI18N
    }
    Class FindInHeapPanel() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.FindInHeapPanel");    // NOI18N
    }
    Class FindInCodeCachePanel() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.ui.FindInCodeCachePanel");    // NOI18N
    }
        
    
    //Non-ui
    Class Oop() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.oops.Oop");    // NOI18N
    }
    Class JavaThread() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.runtime.JavaThread");    // NOI18N
    }
    Class FieldIdentifier() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.oops.FieldIdentifier");    // NOI18N
    }
    
}
