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

package org.graalvm.visualvm.jvm;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;

/**
 *
 * @author Tomas Hurka
 */
class JRockitJVMImpl extends JVMImpl {
 
    JRockitJVMImpl(Application app,JvmstatModel jvms) {
        super(app,jvms);
    }
    
    JRockitJVMImpl(Application app) {
        super(app);
    }
        
    @Override
    public boolean is14() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.4.2")) { // NOI18N
            return true;
        }
        return false;
    }
    
    @Override
    public boolean is15() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.5.0")) { // NOI18N
            return true;
        }
        return false;
    }
    
    @Override
    public boolean is16() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.6.0")) { // NOI18N
            return true;
        }
        return false;
    }
    
    @Override
    public boolean is17() {
        return false;
    }
    
    @Override
    public boolean isDumpOnOOMEnabled() {
        return false;
    }
        
    @Override
    public boolean isGetSystemPropertiesSupported() {
        return (is16() && getAttach() != null) || jmxSupport.getRuntime() != null;
    }
        
    @Override
    public boolean isDumpOnOOMEnabledSupported() {
        return false;
    }
        
    @Override
    public boolean isTakeHeapDumpSupported() {
        return false;
    }
        
    @Override
    public boolean isTakeThreadDumpSupported() {
        if (getAttach() != null) {
            return true;
        }
        JmxModel jmx = getJmxModel();
        if (jmx == null) {
            return false;
        }
        return jmx.isTakeThreadDumpSupported();
    }

}
