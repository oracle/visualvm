/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.jvmstat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import sun.jvmstat.monitor.Monitor;


/**
 * @author Tomas Hurka
 */
class Utils {
    private static Method getVariabilityMethod = getMonitorMethod("getVariability");
    private static Method getUnitsMethod = getMonitorMethod("getUnits");
    
    static Object getVariability(Monitor monitor) {
        return invokeGetter(monitor, getVariabilityMethod);
    }
    
    static Object getUnits(Monitor monitor) {
        return invokeGetter(monitor, getUnitsMethod);
    }
    
    private static Object invokeGetter(Monitor monitor, Method method) {
        try {
            return method.invoke(monitor);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static Method getMonitorMethod(String methodName) {
        try {
            return Monitor.class.getDeclaredMethod(methodName);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
