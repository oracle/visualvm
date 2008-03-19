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

package com.sun.tools.visualvm.jvmstat;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.MonitoredValue;

/**
 *
 * @author Tomas Hurka
 */
class JRockitJvmJvmstatModel extends JvmJvmstatModel {
    private static final String PERM_GEN_PREFIX = "bea.cls.memory.";
    
    JRockitJvmJvmstatModel(Application app,JvmstatModel stat) {
        super(app,stat);
        initMonitoredVales();
    }
    
    private void initMonitoredVales() {
        loadedClasses = jvmstat.findMonitoredValueByName("java.cls.loadedClasses");
        sharedLoadedClasses = jvmstat.findMonitoredValueByName("java.cls.sharedLoadedClasses");
        sharedUnloadedClasses = jvmstat.findMonitoredValueByName("java.cls.sharedUnloadedClasses");
        unloadedClasses = jvmstat.findMonitoredValueByName("java.cls.unloadedClasses");
        threadsDaemon = jvmstat.findMonitoredValueByName("java.threads.daemon");
        threadsLive = jvmstat.findMonitoredValueByName("java.threads.live");
        threadsLivePeak = jvmstat.findMonitoredValueByName("java.threads.livePeak");
        threadsStarted = jvmstat.findMonitoredValueByName("java.threads.started");
        applicationTime = jvmstat.findMonitoredValueByName("sun.rt.applicationTime");
        upTime = jvmstat.findMonitoredValueByName("bea.rt.ticks");
        MonitoredValue osFrequencyMon = jvmstat.findMonitoredValueByName("bea.rt.counterFrequency");
        osFrequency = getLongValue(osFrequencyMon);
        genCapacity = jvmstat.findMonitoredValueByPattern("bea.((gc.heap)|(cls.memory)).committed");
        genUsed = jvmstat.findMonitoredValueByPattern("bea.((gc.heap)|(gc.nursery)|(cls.memory)).used");
        genMaxCapacity=getGenerationSum(jvmstat.findMonitoredValueByPattern("bea.((gc.heap)|(cls.memory)).max"));
    }
    
    protected String getPermGenPrefix() {
        return PERM_GEN_PREFIX;
    }
    
}
