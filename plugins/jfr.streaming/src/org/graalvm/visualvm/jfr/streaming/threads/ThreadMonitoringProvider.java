/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.streaming.threads;

import java.io.IOException;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.views.ApplicationThreadsResponseProvider;
import org.graalvm.visualvm.jfr.streaming.JFRStream;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = ApplicationThreadsResponseProvider.class)
public class ThreadMonitoringProvider implements ApplicationThreadsResponseProvider {

    private static final String JFR_THREAD_SLEEP = "jdk.ThreadSleep";
    private static final String JFR_THREAD_PARK = "jdk.ThreadPark";
    private static final String JFR_JAVA_MONITOR_ENTER = "jdk.JavaMonitorEnter";
    private static final String JFR_JAVA_MONITOR_WAIT = "jdk.JavaMonitorWait";
    private static final String JFR_THREAD_END = "jdk.ThreadEnd";
    private static final String JFR_THREAD_START = "jdk.ThreadStart";

    @Override
    public ThreadMonitoredDataResponseProvider getMonitoredDataResponseProvider(Application app, ThreadMXBean threadMXBean) {
        try {
            JFRStream rs = JFRStream.getFor(app);
            if (rs != null) {
                JFRThreadDataProvider rp = new JFRThreadDataProvider(rs, threadMXBean);
                rs.enable(JFR_THREAD_START);
                rs.enable(JFR_THREAD_END);
                rs.enable(JFR_JAVA_MONITOR_WAIT).withoutStackTrace();
                rs.enable(JFR_JAVA_MONITOR_ENTER).withoutStackTrace();
                rs.enable(JFR_THREAD_PARK).withoutStackTrace();
                rs.enable(JFR_THREAD_SLEEP).withoutStackTrace();
                rs.onEvent(JFR_THREAD_START, rp.threadStart());
                rs.onEvent(JFR_THREAD_END, rp.threadEnd());
                rs.onEvent(JFR_JAVA_MONITOR_WAIT, rp.javaMonitorWait());
                rs.onEvent(JFR_JAVA_MONITOR_ENTER, rp.javaMonitorEnter());
                rs.onEvent(JFR_THREAD_PARK, rp.threadPark());
                rs.onEvent(JFR_THREAD_SLEEP, rp.threadSleep());
                rs.startAsync();
                return rp;
            }
        } catch (IOException ex) {
            Logger.getLogger(ThreadMonitoringProvider.class.getName()).log(Level.INFO, null, ex);
        }
        return null;
    }
}
