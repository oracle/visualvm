/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.threadinspect;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jmx.JvmMXBeansFactory;
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
 * @author Tomas Hurka
 */
final class Engine {

    private static final Logger LOGGER = Logger.getLogger(Engine.class.getName());

    private JmxModel jmxModel;
    private ThreadMXBean threadBean;
    
    static Engine getEngine(Application application) {
         try {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
            if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel,
                        GlobalPreferences.sharedInstance().getThreadsPoll() * 1000);
                if (jmxModel != null) {
                    ThreadMXBean tbean = mxbeans.getThreadMXBean();
                    
                    if (tbean != null) {
                        return new Engine(jmxModel,tbean);
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Problem resolving ThreadMXBean", t); // NOI18N
        }
        return null;       
    }
    
    Engine(JmxModel model, ThreadMXBean tbean) {
        jmxModel = model;
        threadBean = tbean;
    }
    
    List<ThreadInfo> getThreadInfos() {
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

    String getStackTraces(List<Long> threadIdsL) {
        long[] threadIds = new long[threadIdsL.size()];
        for (int i = 0; i < threadIds.length; i++) threadIds[i] = threadIdsL.get(i);
        String stackTraces = jmxModel.takeThreadDump(threadIds);

        return "<pre>" + transform(htmlize(stackTraces)) + "</pre>"; // NOI18N
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
}
