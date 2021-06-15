/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.server.system;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.management.openmbean.CompositeData;

/**
 *
 * @author Tomas Hurka
 */
public class ThreadDump {

    private static final String[][] methods = new String[][]{
        {"sun.management.ThreadInfoCompositeData", "toCompositeData"}, // NOI18N Sun JVM
        {"com.ibm.lang.management.ManagementUtils", "toThreadInfoCompositeData"} // NOI18N IBM J9
    };

    private static Method toCompositeDataMethod;
    private static ThreadMXBean threadBean;
    private static boolean runningOnJdk15;

    public static void initialize(boolean jdk15) {
        runningOnJdk15 = jdk15;
        threadBean = ManagementFactory.getThreadMXBean();
        for (String[] method : methods) {
            String className = method[0];
            String methodName = method[1];
            try {
                Class clazz = Class.forName(className);
                toCompositeDataMethod = clazz.getMethod(methodName, ThreadInfo.class);
                if (toCompositeDataMethod != null) {
                    break;
                }
            } catch (ClassNotFoundException ex) {
            } catch (NoSuchMethodException ex) {
            } catch (SecurityException ex) {
            }
        }
    }

    public static Object[] takeThreadDump() {
        ThreadInfo[] threads = (runningOnJdk15) ? takeThreadDump15() : takeThreadDump16();
        List compositeData = new ArrayList(threads.length);

        for (int i = 0; i < threads.length; i++) {
            ThreadInfo ti = threads[i];
            if (ti != null) {
                compositeData.add(toCompositeData(ti));
            }
        }
        return compositeData.toArray(new CompositeData[0]);
    }

    public static boolean isJDK15() {
        return runningOnJdk15;
    }
    
    private static CompositeData toCompositeData(ThreadInfo tinfo) {
        try {
            return (CompositeData) toCompositeDataMethod.invoke(null, tinfo);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static ThreadInfo[] takeThreadDump15() {
        long[] tids = threadBean.getAllThreadIds();
        return threadBean.getThreadInfo(tids, Integer.MAX_VALUE);
    }

    private static ThreadInfo[] takeThreadDump16() {
        return threadBean.dumpAllThreads(true, true);
    }

}
