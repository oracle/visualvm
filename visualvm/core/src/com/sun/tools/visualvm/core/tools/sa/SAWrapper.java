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

package com.sun.tools.visualvm.core.tools.sa;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


/**
 *
 * @author Tomas Hurka
 */
class SAWrapper {
    URLClassLoader loader;
    File libraryPath;

    SAWrapper(File jdkHome,File saJarFile) throws MalformedURLException {
        URL[] saJarUrls = new URL[]{saJarFile.toURI().toURL()};
        libraryPath = new File(jdkHome,"jre/lib/"+System.getProperty("os.arch"));
        System.out.println("Path "+libraryPath.getAbsolutePath());
        loader = new URLClassLoader(saJarUrls) {
            protected String findLibrary(String libname) {
                String name = System.mapLibraryName(libname);
                File library = new File(libraryPath,name);
                System.out.println("Library "+library.getAbsolutePath());
                        
                if (library.exists()) {
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
    
    Class BugspotAgent() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.bugspot.BugSpotAgent");    // NOI18N
    }
    
    Class HeapHprofBinWriter() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.utilities.HeapHprofBinWriter");    // NOI18N
    }
    
    Class Argumets() throws ClassNotFoundException {
        return classForName("sun.jvm.hotspot.runtime.Arguments");
    }
}
