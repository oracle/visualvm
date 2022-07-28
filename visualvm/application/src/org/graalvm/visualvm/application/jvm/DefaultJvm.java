/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.jvm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Tomas Hurka
 */
class DefaultJvm extends Jvm {
    
    
    DefaultJvm() {
        
    }
    
    public boolean is14() {
        return false;
    }
    
    public boolean is15() {
        return false;
    }
    
    public boolean is16() {
        return false;
    }
    
    public boolean is17() {
        return false;
    }
        
    public boolean is18() {
        return false;
    }
    
    public boolean is19() {
        return false;
    }
    
    public boolean is100() {
        return false;
    }

    public boolean is110() {
        return false;
    }

    public boolean isAttachable() {
        return false;
    }
    
    public String getCommandLine() {
        throw new UnsupportedOperationException();
    }
    
    public String getJvmArgs() {
        throw new UnsupportedOperationException();
    }
    
    public String getJvmFlags() {
        throw new UnsupportedOperationException();
    }
    
    public String getMainArgs() {
        throw new UnsupportedOperationException();
    }
    
    public String getMainClass() {
        throw new UnsupportedOperationException();
    }
    
    public String getVmVersion() {
        throw new UnsupportedOperationException();
    }
    
    public String getJavaVersion() {
        throw new UnsupportedOperationException();
    }
    
    public String getJavaHome() {
        throw new UnsupportedOperationException();
    }
    
    public String getVmInfo() {
        throw new UnsupportedOperationException();
    }
    
    public String getVmName() {
        throw new UnsupportedOperationException();
    }
    
    public String getVmVendor() {
        throw new UnsupportedOperationException();
    }
    
    public Properties getSystemProperties() {
        throw new UnsupportedOperationException();
    }
    
    public synchronized void addMonitoredDataListener(MonitoredDataListener l) {
        throw new UnsupportedOperationException();
    }
    
    public synchronized void removeMonitoredDataListener(MonitoredDataListener l) {
        throw new UnsupportedOperationException();
    }
    
    public String[] getGenName() {
        throw new UnsupportedOperationException();
    }
    
    public boolean isDumpOnOOMEnabled() {
        throw new UnsupportedOperationException();
    }
    
    public void setDumpOnOOMEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }
    
    public boolean takeHeapDump(File outoutFile) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public String takeThreadDump() {
        throw new UnsupportedOperationException();
    }
    
    public HeapHistogram takeHeapHistogram() {
        return null;
    }
    
    public boolean isBasicInfoSupported() {
        return false;
    }
    
    public boolean isMonitoringSupported() {
        return isClassMonitoringSupported() || isThreadMonitoringSupported() || isMemoryMonitoringSupported();
    }
    
    public boolean isClassMonitoringSupported() {
        return false;
    }
    
    public boolean isThreadMonitoringSupported() {
        return false;
    }
    
    public boolean isMemoryMonitoringSupported() {
        return false;
    }
    
    public boolean isGetSystemPropertiesSupported() {
        return false;
    }
    
    public boolean isDumpOnOOMEnabledSupported() {
        return false;
    }
    
    public boolean isTakeHeapDumpSupported() {
        return false;
    }
    
    public boolean isTakeThreadDumpSupported() {
        return false;
    }
    
    public boolean isCpuMonitoringSupported() {
        return false;
    }
    
    public boolean isCollectionTimeSupported() {
        return false;
    }
    
    public MonitoredData getMonitoredData() {
        return null;
    }

    public boolean isJfrAvailable() {
        return false;
    }

    public List<Long> jfrCheck() {
        return Collections.emptyList();
    }

    public String takeJfrDump(long recording, String fileName) {
        throw new UnsupportedOperationException();
    }

    public boolean startJfrRecording(String name, String[] settings, String delay,
            String duration, Boolean disk, String path, String maxAge, String maxSize,
            Boolean dumpOnExit) {
        throw new UnsupportedOperationException();
    }

    public boolean stopJfrRecording() {
        throw new UnsupportedOperationException();
    }

}
