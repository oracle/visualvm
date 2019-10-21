/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.truffle.stagent;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 *
 * @author Tomas Hurka
 */
class TruffleClassLoader extends ClassLoader {
    private static final String TRUFFLE_LOCATOR_CLASS_NAME = "com.oracle.truffle.api.impl.TruffleLocator";

    private final Collection<ClassLoader> loaders;

    TruffleClassLoader(ClassLoader parent) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        super(parent);
        Class LocatorClass;
        if (parent == null) {
            LocatorClass = Class.forName(TRUFFLE_LOCATOR_CLASS_NAME);
        } else {
            LocatorClass = parent.loadClass(TRUFFLE_LOCATOR_CLASS_NAME);
        }
        loaders = (Collection<ClassLoader>) LocatorClass.getMethod("loaders", (Class[])null).invoke(null, (Object[])null);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException ex) {
                continue;
            }
        }
        throw new ClassNotFoundException(name);
    }

}
