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

import com.sun.tools.visualvm.core.model.jmx.JvmJmxModel;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Tomas Hurka
 */
class JmxJVM extends DefaultJVM {
    JvmJmxModel jmxModel;
    Properties systemProperties;
    String jvmArgs;
    
    JmxJVM(JvmJmxModel model) {
        jmxModel = model;
    }
    
    public boolean is14() {
        return getVmVersion().startsWith("1.4.");
    }
    
    public boolean is15() {
        return getVmVersion().startsWith("1.5.");
    }
    
    public boolean is16() {
        return getVmVersion().startsWith("1.6.");
    }
    
    public boolean is17() {
        return getVmVersion().startsWith("1.7.");
    }
    
    public boolean isAttachable() {
        return false;
    }
    
    public String getCommandLine() {
        return "Unknown";
    }
    
    public synchronized String getJvmArgs() {
        if (jvmArgs == null) {
            StringBuilder buf = new StringBuilder();
            List<String> args = jmxModel.getRuntimeMXBean().getInputArguments();
            for (String arg : args) {
                buf.append(arg).append(' ');
            }
            jvmArgs = buf.toString();
        }
        return jvmArgs;
    }
    
    public String getJvmFlags() {
        return null;
    }
    
    public String getMainArgs() {
        return null;
    }
    
    public String getMainClass() {
        return null;
    }
    
    public String getVmVersion() {
        return findByName("java.vm.version");
    }
    
    public String getJavaHome() {
        return findByName("java.home");
    }
    
    public String getVMInfo() {
        return findByName("java.vm.info");
    }
    
    public String getVMName() {
        return findByName("java.vm.name");
    }
    
    public synchronized Properties getSystemProperties() {
        if (systemProperties == null) {
            Map propMap = jmxModel.getRuntimeMXBean().getSystemProperties();
            systemProperties = new Properties();
            systemProperties.putAll(propMap);
        }
        return systemProperties;
    }
    
    public synchronized void addMonitoredDataListener(MonitoredDataListener l) {
        throw new UnsupportedOperationException();
    }
    
    public synchronized void removeMonitoredDataListener(MonitoredDataListener l) {
        throw new UnsupportedOperationException();
    }
    
    public boolean isDumpOnOOMEnabled() {
        return false;
    }
    
    public void setDumpOnOOMEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }
    
    public File takeHeapDump() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public File takeThreadDump() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public boolean isBasicInfoSupported() {
        return true;
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
        return true;
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
    
    private String findByName(String key) {
        Properties p = getSystemProperties();
        if (p == null)
            return null;
        return p.getProperty(key);
    }
    
}
