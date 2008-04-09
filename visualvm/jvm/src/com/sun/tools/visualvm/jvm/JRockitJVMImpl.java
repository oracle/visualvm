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

package com.sun.tools.visualvm.jvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;

/**
 *
 * @author Tomas Hurka
 */
public class JRockitJVMImpl extends JVMImpl {
 
    JRockitJVMImpl(Application app,JvmstatModel jvms) {
        super(app,jvms);
    }
    
    JRockitJVMImpl(Application app) {
        super(app);
    }
        
    @Override
    public boolean is14() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.4.2")) {
            return true;
        }
        return false;
    }
    
    @Override
    public boolean is15() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.5.0")) {
            return true;
        }
        return false;
    }
    
    @Override
    public boolean is16() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.6.0")) {
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
        return getAttach() != null || jmxSupport.getRuntime() != null;
    }

}
