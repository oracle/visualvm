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

package com.sun.tools.visualvm.attach;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import java.io.InputStream;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 *
 * @author Tomas Hurka
 */
public class StackTrace {

  // Attach to pid and perform a thread dump
  public static String runThreadDump(int pid) throws Exception {
    VirtualMachine vm = null;
    try {
      vm = VirtualMachine.attach(Integer.toString(pid));
    } catch (Exception x) {
      String msg = x.getMessage();
      if (msg != null) {
        System.err.println(pid + ": " + msg);
      } else {
        x.printStackTrace();
      }
      if (x instanceof AttachNotSupportedException) {
//        return new SAStackTrace(pid).getStackTrace();
      }
      return null;
    }

    // Cast to HotSpotVirtualMachine as this is implementation specific
    // method.
    InputStream in = ((HotSpotVirtualMachine)vm).remoteDataDump("-l");
    StringBuffer buffer = new StringBuffer(1024);
    
    // read to EOF and just print output
    byte b[] = new byte[256];
    int n;
    do {
      n = in.read(b);
      if (n > 0) {
        String s = new String(b, 0, n, "UTF-8");
        
        buffer.append(s);
      }
    } while (n > 0);
    in.close();
    vm.detach();
    return buffer.toString();
  }
  
  
}
