/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.host.model;

import com.sun.management.OperatingSystemMXBean;
import org.graalvm.visualvm.host.Host;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.openide.util.NbBundle;


/**
 *
 * @author Tomas Hurka
 */
class LocalHostOverview extends HostOverview  {
  private OperatingSystemMXBean osMXBean;
  private boolean loadAverageAvailable;

  LocalHostOverview() {
    osMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    loadAverageAvailable = osMXBean.getSystemLoadAverage() >= 0;
  }

  public String getName() {
    return osMXBean.getName();
  }

  public String getVersion() {
    return osMXBean.getVersion();
  }

  public String getPatchLevel() {
    return System.getProperty("sun.os.patch.level", ""); // NOI18N
  }
  
  public int getAvailableProcessors() {
    return osMXBean.getAvailableProcessors();
  }
  
  public String getArch() {
    String arch = osMXBean.getArch();
    String bits = System.getProperty("sun.arch.data.model"); // NOI18N
    if (bits != null) {
      arch += " "+bits+"bit";   // NOI18N
    }
    return arch;
  }
  
  public String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException ex) {
      return NbBundle.getMessage(Host.class, "LBL_Unknown");   // NOI18N
    }
  }
  
  public double getSystemLoadAverage() {
    if (loadAverageAvailable)
      return osMXBean.getSystemLoadAverage();
    return -1;
  }
  
  public long getTotalPhysicalMemorySize() {
    return osMXBean.getTotalPhysicalMemorySize();
  }
  
  public long getFreePhysicalMemorySize() {
    return osMXBean.getFreePhysicalMemorySize();
  }
  
  public long getTotalSwapSpaceSize() {
    return osMXBean.getTotalSwapSpaceSize();
  }
  
  public long getFreeSwapSpaceSize() {
    return osMXBean.getFreeSwapSpaceSize();
  }

  public String getHostAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException ex) {
      return "127.0.0.1";   // NOI18N
    }
  }

}
