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
import com.sun.tools.visualvm.core.heapdump.HeapDumpSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.core.tools.HeapDump;
import com.sun.tools.visualvm.core.tools.StackTrace;
import com.sun.tools.visualvm.core.tools.SystemProperties;
import com.sun.tools.visualvm.core.tools.VMOption;
import sun.jvmstat.monitor.MonitoredVm;

/**
 *
 * @author Tomas Hurka
 */
public class SunJVM_6 extends SunJVM_5 {
  private Boolean attachAvailable;

  SunJVM_6(JvmstatApplication app, MonitoredVm vm) {
    super(app,vm);
  }

  public boolean is16() {
    return true;
  }

  public boolean is15() {
    return false;
  }

  public boolean isGetSystemPropertiesSupported() {
    return isAttachAvailable();
  }
  
  public Properties getSystemProperties() {
    try {
      return SystemProperties.getSystemProperties(application.getPid());
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  String getStackTrace() {
    try {
      return StackTrace.runThreadDump(application.getPid());
    } catch (Exception ex) {
      ex.printStackTrace();
      return "Cannot get thread dump"+ex.getLocalizedMessage(); // NOI18N
    }
  }
  
  String takeHeapDump(String fileName) {
    try {
      return HeapDump.takeHeapDump(application.getPid(),fileName);
    } catch (Exception ex) {
      ex.printStackTrace();
      return "Cannot get heap dump"+ex.getLocalizedMessage(); // NOI18N
    }
  }
  
  String enableHeapDumpOnOOMErrorFlag(boolean enable) {
    try {
      return VMOption.setFlag(application.getPid(),"HeapDumpOnOutOfMemoryError",enable?"1":"0");
    } catch (Exception ex) {
      ex.printStackTrace();
      return "Cannot get enableHeapDumpOnOOMErrorFlag"+ex.getLocalizedMessage(); // NOI18N
    }
  }
  
  String getHeapDumpOnOOMErrorFlag() {
    try {
      return VMOption.getFlag(application.getPid(),"HeapDumpOnOutOfMemoryError");
    } catch (Exception ex) {
      ex.printStackTrace();
      return null; // NOI18N
    }
  }  
  
  String setHeapDumpPath(String path) {
    try {
      return VMOption.setFlag(application.getPid(),"HeapDumpPath",path);
    } catch (Exception ex) {
      ex.printStackTrace();
      return "Cannot get setHeapDumpPath"+ex.getLocalizedMessage(); // NOI18N
    }
  }

  public boolean isDumpOnOOMEnabledSupported() {
    return isAttachAvailable();
  }

  public boolean isDumpOnOOMEnabled() {
    if (isAttachAvailable()) {
      String val = getHeapDumpOnOOMErrorFlag().trim();
      if ("-XX:-HeapDumpOnOutOfMemoryError".equals(val)) {
        return false;
      }
      if ("-XX:+HeapDumpOnOutOfMemoryError".equals(val)) {
        return true;
      }
      System.out.println("Invalid return value "+val);
      return false;
    }
    return super.isDumpOnOOMEnabled();
  }

  public void setDumpOnOOMEnabled(boolean enabled) {
    String err="";
    if (enabled) {
      err=setHeapDumpPath(application.getStorage().getAbsolutePath());
    }
    err=err.concat(enableHeapDumpOnOOMErrorFlag(enabled));
    if (err.length()!=0) {
      System.out.println("setDumpOnOOMEnabled "+err);
    }
  }

  public boolean isTakeHeapDumpSupported() {
    return isAttachAvailable();
  }

  public File takeHeapDump() throws IOException {
    File snapshotDir = application.getStorage();
    String name = HeapDumpSupport.getInstance().getCategory().createFileName();
    File dumpFile = new File(snapshotDir,name);
    String dump = takeHeapDump(dumpFile.getAbsolutePath());
    System.out.println("Dump "+dump);
    return dumpFile;
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
  
  boolean isAttachAvailable() {
    if (attachAvailable == null) {
      boolean canAttach = Host.LOCALHOST.equals(application.getHost()) && isAttachable();
      attachAvailable = Boolean.valueOf(canAttach);
    }
    return attachAvailable.booleanValue();
  }

}
