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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.Unsafe;

/**
 *
 * @author Tomas Hurka
 */
class TruffleClassLoader extends ClassLoader {
    private static final String TRUFFLE_LOCATOR_CLASS_NAME = "com.oracle.truffle.api.impl.TruffleLocator";
    private static final String GRAALVM_LOCATOR_CLASS_NAME = "com.oracle.graalvm.locator.GraalVMLocator";

    private Collection<ClassLoader> loaders;

    TruffleClassLoader(ClassLoader parent, Unsafe unsafe) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        super(parent);
        loaders = getTruffleLocatorLoaders(parent);
        if (loaders == null) {
            // try different way
            loaders = getGraalVMLocatorLoaders(parent, unsafe);
        }
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

    private static Class getClass(ClassLoader cl, String className) throws ClassNotFoundException {
        if (cl == null) {
            return Class.forName(className);
        } else {
            return cl.loadClass(className);
        }
    }

    private static Collection<ClassLoader> getTruffleLocatorLoaders(ClassLoader cl) throws ClassNotFoundException {
        Class LocatorClass = getClass(cl, TRUFFLE_LOCATOR_CLASS_NAME);
        try {
            return (Collection<ClassLoader>) LocatorClass.getMethod("loaders", (Class[])null).invoke(null, (Object[])null);
        } catch (Exception ex) {
            if (TruffleJMX.DEBUG) {
                Logger.getLogger(TruffleClassLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    private static Collection<ClassLoader> getGraalVMLocatorLoaders(ClassLoader cl, Unsafe unsafe) throws ClassNotFoundException {
        Class LocatorClass = getClass(cl, GRAALVM_LOCATOR_CLASS_NAME);
        try {
            Field f = LocatorClass.getDeclaredField("loader");
            Object base = unsafe.staticFieldBase(f);
            ClassLoader loader = (ClassLoader) unsafe.getObject(base, unsafe.staticFieldOffset(f));
            return Collections.singletonList(loader);
        } catch (Exception ex) {
            Logger.getLogger(TruffleClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
