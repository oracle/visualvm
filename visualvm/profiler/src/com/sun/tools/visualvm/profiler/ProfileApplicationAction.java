/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */
package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.JVM;
import com.sun.tools.visualvm.application.JVMFactory;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.RequestProcessor;

public final class ProfileApplicationAction extends AbstractAction {
    
    private static ProfileApplicationAction instance;
    
    
    public static synchronized ProfileApplicationAction getInstance() {
        if (instance == null) instance = new ProfileApplicationAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        final Application selectedApplication = getSelectedApplication();
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (isAvailable(selectedApplication)) {
                    ProfilerSupport.getInstance().selectProfilerView(selectedApplication);
                } else {
                    NetBeansProfiler.getDefaultNB().displayError("Cannot profile " + DataSourceDescriptorFactory.getDescriptor(selectedApplication).getName());
                }
            }
        });
    }
    
    private void updateEnabled() {
        final Application selectedApplication = getSelectedApplication();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                setEnabled(isEnabled(selectedApplication));
            }
        });
    }
    
    // Safe to be called from AWT EDT (the result doesn't mean the action is really available)
    boolean isEnabled(Application application) {
        if (application == null) return false;
        if (application.getHost() != Host.LOCALHOST) return false;
        if (Application.CURRENT_APPLICATION.equals(application)) return false;
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        
        return true;
    }
    
    // Not to be called from AWT EDT (the result reflects that the action can/cannot be invoked)
    boolean isAvailable(Application application) {
        if (!isEnabled(application)) return false;
        
        JVM jvm = JVMFactory.getJVMFor(application);
        return jvm != null && jvm.isAttachable() && !jvm.is14() && !jvm.is15();
    }
    
    private Application getSelectedApplication() {
        DataSource selectedDataSource = ExplorerSupport.sharedInstance().getSelectedDataSource();
        return (selectedDataSource != null && selectedDataSource instanceof Application) ? (Application)selectedDataSource : null;
    }
    
    
    private ProfileApplicationAction() {
        putValue(Action.NAME, "Profile");
        putValue(Action.SHORT_DESCRIPTION, "Profile");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                updateEnabled();
            }
        });
    }
}
