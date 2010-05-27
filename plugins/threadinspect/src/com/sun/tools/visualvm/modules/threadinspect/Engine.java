/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.threadinspect;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jiri Sedlacek
 */
final class Engine {

    private static final Logger LOGGER = Logger.getLogger(Engine.class.getName());


    static ThreadMXBean resolveThreadBean(Application application) {
        ThreadMXBean threadBean = null;

        try {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
            if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel,
                        GlobalPreferences.sharedInstance().getThreadsPoll() * 1000);
                if (mxbeans != null) threadBean = mxbeans.getThreadMXBean();
                return mxbeans == null ? null : mxbeans.getThreadMXBean();
            }
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Problem resolving ThreadMXBean", t); // NOI18N
        }

        return threadBean;
    }

    static List<ThreadInfo> getThreadInfos(ThreadMXBean threadBean) {
        List<ThreadInfo> tinfosList = null;

        try {
            long[] threadIds = threadBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds);

            tinfosList = new ArrayList(threadInfos.length);
            for (ThreadInfo tinfo : threadInfos)
                if (tinfo != null && tinfo.getThreadName() != null)
                    tinfosList.add(tinfo);
            Collections.sort(tinfosList, new Comparator<ThreadInfo>() {
                public int compare(ThreadInfo ti1, ThreadInfo ti2) {
                    return ti1.getThreadName().compareTo(ti2.getThreadName());
                }
            });
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Problem resolving ThreadInfos", t); // NOI18N
        }

        return tinfosList;
    }

    static String getStackTraces(ThreadMXBean threadBean, List<Long> threadIdsL) {
        long[] threadIds = new long[threadIdsL.size()];
        for (int i = 0; i < threadIds.length; i++) threadIds[i] = threadIdsL.get(i);

        boolean limitedJvm = true;
        ThreadInfo[] threadInfos = null;

        try {
            threadInfos = threadBean.getThreadInfo(threadIds, true, true);
            limitedJvm = false;
        } catch (Throwable t1) {
            // likely a 1.5 JVM
            try {
                threadInfos = threadBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
            } catch (Throwable t2) {
                LOGGER.log(Level.INFO, "Problem resolving extended ThreadInfos", t2); // NOI18N
            }
        }

        if (threadInfos == null) return null;

        StringBuilder b = new StringBuilder();
        for (ThreadInfo ti : threadInfos) if (ti != null)
            b.append(limitedJvm ? stackTrace15(ti) : stackTrace16(ti, threadBean));

        return "<pre>" + transform(htmlize(b.toString())) + "</pre>"; // NOI18N
    }

    private static String stackTrace15(ThreadInfo thread) {
        StringBuilder sb = new StringBuilder();

        if (thread != null) {
            sb.append("\"" + thread.getThreadName() + // NOI18N
                    "\" - Thread t@" + thread.getThreadId() + "\n");    // NOI18N
            sb.append("   java.lang.Thread.State: " + thread.getThreadState()); // NOI18N
            if (thread.getLockName() != null) {
                sb.append(" on " + thread.getLockName());   // NOI18N
                if (thread.getLockOwnerName() != null) {
                    sb.append(" owned by: " + thread.getLockOwnerName());   // NOI18N
                }
            }
            sb.append("\n"); // NOI18N
            for (StackTraceElement st : thread.getStackTrace()) {
                sb.append("        at " + st.toString() + "\n");    // NOI18N
            }
        }

        sb.append("\n");  // NOI18N
        return sb.toString();
    }

    private static String stackTrace16(ThreadInfo thread, ThreadMXBean threadBean) {
        StringBuilder sb = new StringBuilder();

        MonitorInfo[] monitors = null;
        if (threadBean.isObjectMonitorUsageSupported())
            monitors = thread.getLockedMonitors();
        sb.append("\"" + thread.getThreadName() + // NOI18N
                "\" - Thread t@" + thread.getThreadId() + "\n");    // NOI18N
        sb.append("   java.lang.Thread.State: " + thread.getThreadState()); // NOI18N
        if (thread.getLockName() != null) {
            sb.append(" on " + thread.getLockName());   // NOI18N
            if (thread.getLockOwnerName() != null) {
                sb.append(" owned by: " + thread.getLockOwnerName());   // NOI18N
            }
        }
        sb.append("\n"); // NOI18N
        int index = 0;
        for (StackTraceElement st : thread.getStackTrace()) {
            sb.append("        at " + st.toString() + "\n");  // NOI18N
            if (monitors != null) {
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == index) {
                        sb.append("        - locked " + mi.toString() + "\n");    // NOI18N
                    }
                }
            }
            index++;
        }
        if (threadBean.isSynchronizerUsageSupported()) {
            sb.append("\n   Locked ownable synchronizers:");    // NOI18N
            LockInfo[] synchronizers = thread.getLockedSynchronizers();
            if (synchronizers == null || synchronizers.length == 0) {
                sb.append("\n        - None\n");  // NOI18N
            } else {
                for (LockInfo li : synchronizers) {
                    sb.append("\n        - locked " + li.toString() + "\n");  // NOI18N
                }
            }
        }

        sb.append("\n");  // NOI18N
        return sb.toString();
    }

    private static String htmlize(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;");     // NOI18N
    }

    private static String transform(String value) {
        StringBuilder sb = new StringBuilder();
        String[] result = value.split("\\n"); // NOI18N
        for (int i = 0; i < result.length; i++) {
            String line = result[i];
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))) {
                sb.append("<span style=\"color: #0033CC\">"); // NOI18N
                sb.append(line);
                sb.append("</span><br>"); // NOI18N
            } else {
                sb.append(line);
                sb.append("<br>"); // NOI18N
            }
        }
        return sb.toString();
    }


    private Engine() {}

}
