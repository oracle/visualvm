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
import com.sun.tools.visualvm.core.host.MonitoredHostDS;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.model.ModelFactory;
import com.sun.tools.visualvm.core.model.ModelProvider;
import com.sun.tools.visualvm.core.datasource.Application;
import java.net.URISyntaxException;
import java.util.Set;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;


/**
 *
 * @author Tomas Hurka
 */
public class JVMFactory extends ModelFactory<JVM,Application> implements ModelProvider<JVM,Application> {

    private static JVMFactory jvmFactory;

    private JVMFactory() {
    }
    
    public static synchronized JVMFactory getDefault() {
        if (jvmFactory == null) {
            jvmFactory = new JVMFactory();
            jvmFactory.registerFactory(jvmFactory);
            jvmFactory.registerFactory(new JRockitFactory());
        }
        return jvmFactory;
    }
    
    public static JVM getJVMFor(Application app) {
        return getDefault().getModel(app);
    }
    
    static MonitoredVm getMonitoredVm(JvmstatApplication app) throws MonitorException {
        if (app.isFinished() || app.getMonitoredHost() == null) return null;
        
        String vmId = "//" + app.getPid() + "?mode=r";
        try {
            MonitoredHostDS monitoredHostDs = app.getMonitoredHost();
            return monitoredHostDs.getMonitoredHost().getMonitoredVm(new VmIdentifier(vmId));
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public JVM createModelFor(Application appl) {
        if (appl instanceof JvmstatApplication) {
            JvmstatApplication app = (JvmstatApplication) appl;
            MonitoredVm vm = null;
            try {
                vm = getMonitoredVm(app);
                String vmVersion = MonitoredVmUtil.vmVersion(vm);
                if (vmVersion != null) {
                    SunJVM_4 jvm = null;
                    // Check for Sun VM (and maybe other?)
                    if (vmVersion.startsWith("1.4.")) jvm = new SunJVM_4(app,vm); // NOI18N
                    
                    else if (vmVersion.startsWith("1.5.")) jvm = new SunJVM_5(app,vm); // NOI18N
                    
                    else if (vmVersion.startsWith("1.6.")) jvm = new SunJVM_6(app,vm); // NOI18N
                    else if (vmVersion.startsWith("10.0")) jvm = new SunJVM_6(app,vm); // NOI18N // Sun HotSpot Express
                    
                    else if (vmVersion.startsWith("1.7.")) jvm = new SunJVM_7(app,vm); // NOI18N
                    else if (vmVersion.startsWith("11.0")) jvm = new SunJVM_7(app,vm); // NOI18N
                    else if (vmVersion.startsWith("12.0")) jvm = new SunJVM_7(app,vm); // NOI18N // Sun HotSpot Express
                    
                    if (jvm != null) {
                        app.notifyWhenFinished(jvm);
                        return jvm;
                    }
                }
            } catch (MonitorException ex) {
                ex.printStackTrace();
            }
            if (vm != null) {
                vm.detach();
            }
        }
        return new DefaultJVM(appl);
    }
}
