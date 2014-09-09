/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.server.system;

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
        return compositeData.toArray(new CompositeData[compositeData.size()]);
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
