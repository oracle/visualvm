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

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.application.jvm.MonitoredDataListener;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import com.sun.tools.visualvm.tools.attach.AttachModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmJmxModel;
import com.sun.tools.visualvm.tools.jmx.JvmJmxModelFactory;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatListener;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModelFactory;
import com.sun.tools.visualvm.tools.sa.SaModel;
import com.sun.tools.visualvm.tools.sa.SaModelFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author Tomas Hurka
 */
public class JRockitJVMImpl extends JVMImpl {
 
    JRockitJVMImpl(Application app,JvmstatModel jvms) {
        super(app,jvms);
    }
    
    JRockitJVMImpl(Application app,JvmJmxModel jmx) {
        super(app,jmx);
    }
        
    public boolean is14() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.4.2")) {
            return true;
        }
        return false;
    }
    
    public boolean is15() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.5.0")) {
            return true;
        }
        return false;
    }
    
    public boolean is16() {
        String ver = getVmVersion();
        if (ver != null && ver.contains("1.6.0")) {
            return true;
        }
        return false;
    }
    
    public boolean is17() {
        return false;
    }
    
    public boolean isDumpOnOOMEnabled() {
        return false;
    }
        
    public boolean isGetSystemPropertiesSupported() {
        return (is16() && getAttach() != null) || jmxSupport.getRuntime() != null;
    }
        
    public boolean isDumpOnOOMEnabledSupported() {
        return false;
    }
        
    public boolean isTakeHeapDumpSupported() {
        return false;
    }
        
    public boolean isTakeThreadDumpSupported() {
        return getAttach() != null || jmxSupport.getRuntime() != null;
    }

}
