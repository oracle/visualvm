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

package com.sun.tools.visualvm.modules.jconsole;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;

/**
 * @author Luis-Miguel Alventosa
 * @author Tomas Hurka
 */
class JConsoleClassLoader extends URLClassLoader {

    private final PermissionCollection permissions = new Permissions();
    private static final String PACKAGE_NAME = "com.sun.tools.visualvm.modules.jconsole."; // NOI18N

    JConsoleClassLoader(URL moduleJar, URL jconsoleJar) {
        super(new URL[]{moduleJar, jconsoleJar}, Install.class.getClassLoader());
        permissions.add(new AllPermission());
    }

    @Override
    protected Class loadClass(String className, boolean resolve) throws ClassNotFoundException {
        if (className.startsWith(PACKAGE_NAME)) {
            // Do not delegate to parent classloader
            Class clazz = findLoadedClass(className);
            if (clazz != null) {
                return clazz;
            }
            clazz = findClass(className);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        } else {
            return super.loadClass(className, resolve);
        }
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        return permissions;
    }
}
