/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.svm;

import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.MonitoredData;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.MonitoredValue;

/**
 *
 * @author Tomas Hurka
 */
public class SVMMonitoredDataImpl extends MonitoredData {
    private static final String ProcessCPUTime_COUNTER_NAME = "com.oracle.svm.processCPUTime";   // NOI18N

  SVMMonitoredDataImpl(Jvm jvm, JvmstatModel monitoredVm, JvmJvmstatModel jvmstatModel) {
    this.monitoredVm = jvm;
    loadedClasses = jvmstatModel.getLoadedClasses();
    sharedLoadedClasses = jvmstatModel.getSharedLoadedClasses();
    sharedUnloadedClasses = jvmstatModel.getSharedUnloadedClasses();
    unloadedClasses = jvmstatModel.getUnloadedClasses();
    threadsDaemon = jvmstatModel.getThreadsDaemon();
    threadsLive = jvmstatModel.getThreadsLive();
    threadsLivePeak = jvmstatModel.getThreadsLivePeak();
    threadsStarted = jvmstatModel.getThreadsStarted();
    applicationTime = jvmstatModel.getApplicationTime()/(jvmstatModel.getOsFrequency()/1000);
    upTime = jvmstatModel.getUpTime()/(jvmstatModel.getOsFrequency()/1000);
    genCapacity = jvmstatModel.getGenCapacity();
    genUsed = jvmstatModel.getGenUsed();
    genMaxCapacity = jvmstatModel.getGenMaxCapacity();
    MonitoredValue cpuTimeVal = monitoredVm.findMonitoredValueByName(ProcessCPUTime_COUNTER_NAME);
    if (cpuTimeVal != null) {
        processCpuTime = ((Long)cpuTimeVal.getValue()).longValue();
    }
  }

}
