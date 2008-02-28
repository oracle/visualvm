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

package com.sun.tools.visualvm.core.model.jvm;

import com.sun.tools.visualvm.core.application.JvmstatApplication;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.core.tools.StackTrace;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import sun.jvmstat.monitor.LongMonitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;

/**
 *
 * @author Tomas Hurka
 */
public class JRockitVM extends JvmstatJVM {
    private static final String PERM_GEN_PREFIX = "bea.cls.memory.";
    
    protected Boolean attachAvailable;
    
    JRockitVM(JvmstatApplication app,MonitoredVm vm) {
        super(app,vm);
    }
    
    public boolean is14() {
        return getVmVersion().contains("1.4.2");
    }
    
    public boolean is15() {
        return getVmVersion().contains("1.5.0");
    }
    
    String getPermGenPrefix() {
        return PERM_GEN_PREFIX;
    }
    
    public boolean isMemoryMonitoringSupported() {
        return true;
    }
    
    public boolean isClassMonitoringSupported() {
        return true;
    }
    
    public boolean isThreadMonitoringSupported() {
        return true;
    }
    
    void initListeners() {
        try {
            loadedClasses = (LongMonitor) monitoredVm.findByName("java.cls.loadedClasses");
            unloadedClasses = (LongMonitor) monitoredVm.findByName("java.cls.unloadedClasses");
            applicationTime = (LongMonitor) monitoredVm.findByName("sun.rt.applicationTime");
            threadsDaemon = (LongMonitor) monitoredVm.findByName("java.threads.daemon");
            threadsLive = (LongMonitor) monitoredVm.findByName("java.threads.live");
            threadsLivePeak = (LongMonitor) monitoredVm.findByName("java.threads.livePeak");
            threadsStarted = (LongMonitor) monitoredVm.findByName("java.threads.started");
            upTime = (LongMonitor) monitoredVm.findByName("bea.rt.ticks");
            LongMonitor osFrequencyMon = ((LongMonitor)monitoredVm.findByName("bea.rt.counterFrequency"));
            osFrequency = osFrequencyMon.longValue();
            genCapacity = monitoredVm.findByPattern("bea.((gc.heap)|(cls.memory)).committed");
            genUsed = monitoredVm.findByPattern("bea.((gc.heap)|(gc.nursery)|(cls.memory)).used");
            genMaxCapacity = getGenerationSum(monitoredVm.findByPattern("bea.((gc.heap)|(cls.memory)).max"));
            monitoredVm.addVmListener(this);
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
    }    
    
    protected String getStackTrace() {
        try {
            return StackTrace.runThreadDump(application.getPid());
        }  catch (Exception ex) {
            ex.printStackTrace();
            return "Cannot get thread dump "+ex.getLocalizedMessage(); // NOI18N
        }
    }
    
    protected boolean isAttachAvailable() {
        if (attachAvailable == null) {
            boolean canAttach = Host.LOCALHOST.equals(application.getHost()) && isAttachable();
            attachAvailable = Boolean.valueOf(canAttach);
        }
        return attachAvailable.booleanValue();
    }
    
    public boolean isTakeThreadDumpSupported() {
        return isAttachAvailable();
    }
    
    public File takeThreadDump() throws IOException {
        String dump = getStackTrace();
        File snapshotDir = application.getStorage();
        String name = ThreadDumpSupport.getInstance().getCategory().createFileName();
        File dumpFile = new File(snapshotDir,name);
        OutputStream os = new FileOutputStream(dumpFile);
        os.write(dump.getBytes("UTF-8"));
        os.close();
        return dumpFile;
    }
    
}
