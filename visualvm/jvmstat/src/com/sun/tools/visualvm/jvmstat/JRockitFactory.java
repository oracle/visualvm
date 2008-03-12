/*
 * Model.java
 *
 * Created on December 18, 2007, 6:13 PM
 *
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

package com.sun.tools.visualvm.jvmstat;

import com.sun.tools.visualvm.jvm.JVM;
import com.sun.tools.visualvm.jvmstat.application.JvmstatApplication;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.application.Application;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.StringMonitor;

/**
 *
 * @author Tomas Hurka
 */
public class JRockitFactory extends SunFactory {

    public JVM createModelFor(Application appl) {
        if (appl instanceof JvmstatApplication) {
            JvmstatApplication app = (JvmstatApplication) appl;
            MonitoredVm vm = null;
            try {
                vm = SunFactory.getMonitoredVm(app);
                if (vm != null) {
                    StringMonitor name = (StringMonitor) vm.findByName("java.property.java.vm.name");   // NOI18N

                    if (name != null && "BEA JRockit(R)".equals(name.stringValue())) {  // NOI18N
                        JRockitVM jvm;
                        StringMonitor vmVersion = (StringMonitor) vm.findByName("java.property.java.vm.version"); // NOI18N

                        if (vmVersion != null && vmVersion.stringValue().contains("1.6.0")) {
                            jvm = new JRockitVM_6(app,vm);
                        } else {
                            jvm = new JRockitVM(app,vm);
                        }
                        app.notifyWhenFinished(jvm);
                        return jvm;
                    }
                }
            } catch (MonitorException ex) {
                ex.printStackTrace();
            }
            if (vm != null) vm.detach();
        }
        return null;
    }
    
}
