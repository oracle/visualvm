/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.cpu;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 *
 * @author Tomas Hurka
 */
public class ThreadsCPU {
    private static final ObjectName THREAD_NAME = getThreadName();
    private static final Logger LOGGER = Logger.getLogger(ThreadsCPU.class.getName());
    
    private final ThreadMXBean threadBean;
    private final MBeanServerConnection connection;
    private boolean useBulkOperation = true;

    public ThreadsCPU(ThreadMXBean bean, MBeanServerConnection conn) {
        threadBean = bean;
        connection = conn;
    }
    
    public ThreadsCPUInfo getThreadsCPUInfo() throws MBeanException, ReflectionException, IOException, InstanceNotFoundException {
        long[] ids = threadBean.getAllThreadIds();
        ThreadInfo[] tids = threadBean.getThreadInfo(ids);
        long[] tinfo;

        if (useBulkOperation) {
            Object[] args = new Object[] {ids};
            String[] sigs = new String[] {"[J"};  // NOI18N

            try {
                tinfo = (long[])connection.invoke(THREAD_NAME, "getThreadCpuTime", args, sigs);  // NOI18N
            } catch (javax.management.ReflectionException ex) {
                LOGGER.log(Level.INFO, "getThreadCpuTime failed", ex);
                useBulkOperation = false;
                return getThreadsCPUInfo();
            }
        } else {
            tinfo = new long[ids.length];

            for (int i = 0; i < ids.length; i++) {
                tinfo[i] = threadBean.getThreadCpuTime(ids[i]);
            }
        }
        long time = System.currentTimeMillis();
        
        return new ThreadsCPUInfo(time,tids,tinfo);
    }   

    private static ObjectName getThreadName() {
        try {
            return new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            throw new RuntimeException(ex);
        }
    }
}
