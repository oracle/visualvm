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
import com.sun.tools.visualvm.core.model.ModelFactory;
import com.sun.tools.visualvm.core.model.ModelProvider;
import com.sun.tools.visualvm.core.datasource.Application;
import java.net.URISyntaxException;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;


/**
 * The JVMFactory class is a factory class for getting the
 * {@link JVM} representation  for the {@link Application}.
 * 
 * @author Tomas Hurka
 */
public final class JVMFactory extends ModelFactory<JVM,Application> implements ModelProvider<JVM,Application> {

    private static JVMFactory jvmFactory;

    private JVMFactory() {
    }
    
    /**
     * Getter for the default version of the JVMFactory.
     * @return instance of {@link JVMFactory}.
     */
    public static synchronized JVMFactory getDefault() {
        if (jvmFactory == null) {
            jvmFactory = new JVMFactory();
            jvmFactory.registerFactory(jvmFactory);
            jvmFactory.registerFactory(new JRockitFactory());
            jvmFactory.registerFactory(new JmxFactory());
        }
        return jvmFactory;
    }
    
    /**
     * Factory method for obtaining {@link JVM} for {@link Application}. Note that there
     * is only one instance of {@link JVM} for a concrete application. This {@link JVM}
     * instance is cached.
     * @param app application 
     * @return {@link JVM} instance which encapsulates application's JVM.
     */
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
    
    /**
     * Default {@link ModelProvider} implementation, which creates 
     * dummy {@link JVM} instances. If you want to extend JVMFactory use 
     * {@link JVMFactory#registerFactory()} to register the new instances
     * of {@link ModelProvider} for the different types of {@link Application}.
     * @param app application
     * @return dummy instance of {@link JVM}
     */
    public JVM createModelFor(Application app) {
        JvmstatApplication appl = (JvmstatApplication) app;
        MonitoredVm vm = null;
        try {
            vm = getMonitoredVm(appl);
            if (vm != null) {
                String vmVersion = MonitoredVmUtil.vmVersion(vm);
                if (vmVersion != null) {
                    SunJVM_4 jvm = null;
                    // Check for Sun VM (and maybe other?)
                    if (vmVersion.startsWith("1.4.")) jvm = new SunJVM_4(appl,vm); // NOI18N

                    else if (vmVersion.startsWith("1.5.")) jvm = new SunJVM_5(appl,vm); // NOI18N

                    else if (vmVersion.startsWith("1.6.")) jvm = new SunJVM_6(appl,vm); // NOI18N
                    else if (vmVersion.startsWith("10.0")) jvm = new SunJVM_6(appl,vm); // NOI18N // Sun HotSpot Express

                    else if (vmVersion.startsWith("1.7.")) jvm = new SunJVM_7(appl,vm); // NOI18N
                    else if (vmVersion.startsWith("11.0")) jvm = new SunJVM_7(appl,vm); // NOI18N
                    else if (vmVersion.startsWith("12.0")) jvm = new SunJVM_7(appl,vm); // NOI18N // Sun HotSpot Express

                    if (jvm != null) {
                        appl.notifyWhenFinished(jvm);
                        return jvm;
                    }
                }
            }
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
        if (vm != null) {
            vm.detach();
        }
        return new DefaultJVM();
    }
}
