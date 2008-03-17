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

package com.sun.tools.visualvm.jvmstat;

import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import java.net.URISyntaxException;
import org.openide.ErrorManager;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

/**
 *
 * @author Tomas Hurka
 */
public class JvmstatModelProvider extends AbstractModelProvider<JvmstatModel, Application> {
    
    static MonitoredVm getMonitoredVm(Application app) throws MonitorException {
        if (app.isRemoved() || app.getPid() == Application.UNKNOWN_PID) return null;
        
        String vmId = "//" + app.getPid() + "?mode=r";
        try {
            MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(app.getHost().getHostName());
            MonitoredVm mvm = monitoredHost.getMonitoredVm(new VmIdentifier(vmId));
            mvm.setInterval(GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000);
            return mvm;
        } catch (URISyntaxException ex) {
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION,ex);
            return null;
        }
    }
    
    public JvmstatModel createModelFor(Application app) {
        MonitoredVm vm = null;
        try {
            vm = getMonitoredVm(app);
            if (vm != null) {
                JvmstatModelImpl jvmstat = new JvmstatModelImpl(app,vm);
                app.notifyWhenRemoved(jvmstat);
                return jvmstat;
            }
        } catch (MonitorException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
        if (vm != null) {
            vm.detach();
        }
        return null;
    }
    
}
