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

package com.sun.tools.visualvm.sampler.cpu;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tomas Hurka
 */
public class ThreadInfoProvider {

    private static final Logger LOGGER = Logger.getLogger(ThreadInfoProvider.class.getName());
    
    final private String status;
    private boolean useGetThreadInfo;
    private ThreadMXBean threadBean;
    
    public ThreadInfoProvider(Application app) {
        status = initialize(app);
    }

    public String getStatus() {
        return status;
    }

    private String initialize(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) {
            return "Not available.";
        }
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null) {
            return "Not available. Cannot initialize JMX connection to target application. Use 'Add JMX Connection' action to attach to the application.";
        }
        if (jmxModel.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
           return "Not available. Failed to create JMX connection to target application. Use 'Add JMX Connection' action to attach to the application.";
        }
        JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
        if (mxbeans == null) {
            LOGGER.log(Level.WARNING, "JvmMXBeansFactory.getJvmMXBeans(jmxModel) returns null for " + application); // NOI18N
            return "Not available. Cannot access threads in target application. Check the logfile for details (use Help | About | Logfile).";
        }
        threadBean = mxbeans.getThreadMXBean();
        if (threadBean == null) {
            LOGGER.log(Level.WARNING, "mxbeans.getThreadMXBean() returns null for " + application); // NOI18N
            return "Not available. Cannot access threads in target application. Check the logfile for details (use Help | About | Logfile).";
        }
        useGetThreadInfo = JvmFactory.getJVMFor(application).is15();
        try {
            dumpAllThreads();
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "threadBean.getThreadInfo(ids, maxDepth) throws SecurityException for " + application, e); // NOI18N
            return "Not available. Failed to access threads in target application. Check the logfile for details (use Help | About | Logfile).";
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "threadBean.getThreadInfo(ids, maxDepth) throws Throwable for " + application, t); // NOI18N
            return "Not available. Failed to access threads in target application. Check the logfile for details (use Help | About | Logfile).";
        }
        return null;
    }

    ThreadInfo[] dumpAllThreads() {
        if (useGetThreadInfo) {
            return threadBean.getThreadInfo(threadBean.getAllThreadIds(), Integer.MAX_VALUE);
        }
        return threadBean.dumpAllThreads(false,false);
    }
}
