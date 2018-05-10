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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author poonam
 */
class SAPluginClassLoader extends URLClassLoader {

    private static ClassLoader parent;
    private boolean classPathSet;
    private File libraryPath;

    SAPluginClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        this.parent = parent;
    }

    SAPluginClassLoader(ClassLoader parent, File jdkHome, File saJarFile) {
        this(parent);
        this.classPathSet = true;
        try {
            addURL(saJarFile.toURI().toURL());
        } catch(MalformedURLException mue) {
            throw new RuntimeException(mue);
        }
        libraryPath = new File(jdkHome, "jre/bin/");

    }

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
             */

            if ( ( name.startsWith("sun.jvm.hotspot.") ||
                    name.startsWith("com.sun.java.swing.") ) &&
                    !name.startsWith("sun.jvm.hotspot.debugger.")) {
                return findClass(name);
            }
            if (parent != null) {
                c = parent.loadClass(name);
            } else {
                c = findSystemClass(name);
            }
        }
        return c;
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        if (classPathSet) {
            return super.findClass(name);
        } else {
            byte[] b = null;
            try {
                InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
                // Read until end of stream is reached
                b = new byte[1024];
                int total = 0;
                int len = 0;
                while ((len = in.read(b, total, b.length - total)) != -1) {
                    total += len;
                    if (total >= b.length) {
                        byte[] tmp = new byte[total * 2];
                        System.arraycopy(b, 0, tmp, 0, total);
                        b = tmp;
                    }
                }
                // Trim array to correct size, if necessary
                if (total != b.length) {
                    byte[] tmp = new byte[total];
                    System.arraycopy(b, 0, tmp, 0, total);
                    b = tmp;
                }
            } catch (Exception exp) {
                throw (ClassNotFoundException) new ClassNotFoundException().initCause(exp);
            }
            return defineClass(name, b, 0, b.length);
        }
    }
    protected String findLibrary(String libname) {
        String name = System.mapLibraryName(libname);
        File library = new File(libraryPath, name);
        //   LOGGER.fine("Library " + library.getAbsolutePath());
        String f = library.getAbsolutePath();// NOI18N

        if (library.exists() && library.canRead()) {            
            return library.getAbsolutePath();
        }
        return super.findLibrary(libname);
    }
}

