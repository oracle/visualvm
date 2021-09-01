/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.attach;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.tools.attach.AttachModel;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModelFactory;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
public final class AttachModelProvider extends AbstractModelProvider<AttachModel, Application>  {
    
    AttachModelProvider() {
    }
    
    public AttachModel createModelFor(Application app) {
        if (app.isLocalApplication()) {
            JvmJvmstatModel jvmstat = JvmJvmstatModelFactory.getJvmstatModelFor(app);
            
            if (jvmstat != null && jvmstat.isAttachable()) {
                if (Utilities.isWindows()) {
                    // on Windows Attach API can only attach to the process of the same
                    // architecture ( 32bit / 64bit )
                    Boolean this64bitArch = is64BitArchitecture();
                    Boolean app64bitArch = is64BitArchitecture(jvmstat);
                    if (this64bitArch != null && app64bitArch != null) {
                        if (!this64bitArch.equals(app64bitArch)) {
                            return null;
                        }
                    }
                }
                // check that application is running under the same user as VisualVM
                String pid = String.valueOf(app.getPid());
                for (VirtualMachineDescriptor descr : VirtualMachine.list()) {
                    if (pid.equals(descr.id())) {
                        String vmName = jvmstat.getVmName();
                        if (vmName != null) {
                            if ("BEA JRockit(R)".equals(vmName)) {  // NOI18N
                                return new JRockitAttachModelImpl(app);
                            }
                            if ("Oracle JRockit(R)".equals(vmName)) {  // NOI18N
                                return new OracleJRockitAttachModelImpl(app);
                            }                            
                        }
                        return new AttachModelImpl(app);
                    }
                }
            }
        }
        return null;
    }
    
    private static Boolean is64BitArchitecture(JvmJvmstatModel jvmstat) {
        String name = jvmstat.getVmName();
        if (name != null) {
            return name.toLowerCase().contains("64-bit");   // NOI18N
        }
        return null;
    }
    
    private static Boolean is64BitArchitecture() {
        String thisArch = System.getProperty("sun.arch.data.model");    // NOI18N
        if (thisArch != null) {
            return Boolean.valueOf("64".equals(thisArch));  // NOI18N
        }
        return null;
    }
}
