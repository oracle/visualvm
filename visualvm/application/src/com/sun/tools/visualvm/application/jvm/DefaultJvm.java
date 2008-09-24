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

package com.sun.tools.visualvm.application.jvm;

import java.io.File;
import java.io.IOException;
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
  
  public boolean isDumpOnOOMEnabled() {
    throw new UnsupportedOperationException();
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

}
