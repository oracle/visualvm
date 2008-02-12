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

import com.sun.tools.visualvm.core.model.Model;
import java.io.File;
import java.io.IOException;
import java.util.Properties;


/**
 *
 * @author Tomas Hurka
 */
public abstract class JVM extends Model  {

  public abstract boolean is14();

  public abstract boolean is15();

  public abstract boolean is16();

  public abstract boolean is17();

  public abstract boolean isAttachable();

  public abstract boolean isBasicInfoSupported();
  public abstract String getCommandLine();
  public abstract String getJvmArgs();
  public abstract String getJvmFlags();
  public abstract String getMainArgs();
  public abstract String getMainClass();
  public abstract String getVmVersion();
  public abstract String getJavaHome();
  public abstract String getVMInfo();
  public abstract String getVMName();

  public abstract boolean isMonitoringSupported();
  public abstract boolean isClassMonitoringSupported();
  public abstract boolean isThreadMonitoringSupported();
  public abstract boolean isMemoryMonitoringSupported();
  public abstract void addMonitoredDataListener(MonitoredDataListener l);
  public abstract void removeMonitoredDataListener(MonitoredDataListener l);  
  
  public abstract boolean isGetSystemPropertiesSupported();
  public abstract Properties getSystemProperties();
  
  public abstract boolean isDumpOnOOMEnabledSupported();
  public abstract boolean isDumpOnOOMEnabled();
  public abstract void setDumpOnOOMEnabled(boolean enabled);
  
  public abstract boolean isTakeHeapDumpSupported();
  public abstract File takeHeapDump() throws IOException;
  
  public abstract boolean isTakeThreadDumpSupported();
  public abstract File takeThreadDump() throws IOException;

}
