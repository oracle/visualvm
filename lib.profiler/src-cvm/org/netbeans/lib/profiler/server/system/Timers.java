/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.server.system;

/**
 * Provides methods for obtaining various high-resolution system times.
 * A version for CVM 
 *
 * @author  Misha Dmitriev
 */
public class Timers {
  
  /** Should be called at earliest possible time */
  public static void initialize() {
    getThreadCPUTimeInNanos();
  }

  
  /** 
   * "counts" instead of nanoseconds in this method are for compatibility with the previous
   * versions of JFluid, that call a native method for system timer, which, in turn, returns
   * the result in sub-microsecond "counts" on Windows.
   */
  public static native long getCurrentTimeInCounts();
  
  public static long getNoOfCountsInSecond() {
    return 1000000000;
  }

  
  public static native long getThreadCPUTimeInNanos();


  /** 
   * WORKS ONLY ON UNIX, calls nanosleep(). On Solaris, this is more precise than the built-in Thread.sleep() call
   * implementation that, at least in JDK 1.4.2, goes to select(3C). On Linux, it should be more precise, but it
   * turns out that nanosleep() in this OS, at least in version 7.3 that I tested, has a resolution of at least 20ms.
   * This seems to be a known issue; hopefully they fix it in future.
   */
  public static native void osSleep(int ns);
 
  
  /** 
   * This is relevant only on Solaris. By default, the resolution of the thread local CPU timer is 10 ms. If we enable
   * micro state accounting, it enables significantly (but possibly at a price of some overhead). So I turn it on only
   * when thread CPU timestamps are really collected.
   */
  public static native void enableMicrostateAccounting(boolean v);
}
