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
package com.sun.tools.visualvm.core.profiler;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

public final class ProfileApplicationAction extends AbstractAction {
    
    private static ProfileApplicationAction instance;
    
    
    public static synchronized ProfileApplicationAction getInstance() {
        if (instance == null) instance = new ProfileApplicationAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        final Application selectedApplication = getSelectedApplication();
        ProfilerSupport.getInstance().selectProfilerView(selectedApplication);
    }
    
    void updateEnabled() {
        final Application selectedApplication = getSelectedApplication();
        final boolean enabled;
        
        if (selectedApplication == null) {
            enabled = false;
        } else if (Application.CURRENT_APPLICATION.equals(selectedApplication)) {
            enabled = false;
        } else if (selectedApplication.getHost() != Host.LOCALHOST) {
            enabled = false;
        } else if (selectedApplication.isFinished()) {
            enabled = false;
        } else {
            JVM jvm = JVMFactory.getJVMFor(selectedApplication);
            enabled = jvm != null && jvm.isAttachable() && !jvm.is14() && !jvm.is15();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setEnabled(enabled);
            }
        });
    }
    
    private Application getSelectedApplication() {
        DataSource selectedDataSource = ExplorerSupport.sharedInstance().getSelectedDataSource();
        if (selectedDataSource == null) return null;
        return selectedDataSource instanceof Application ? (Application)selectedDataSource : null;
    }
    
    
    private ProfileApplicationAction() {
        putValue(Action.NAME, "Profile");
        putValue(Action.SHORT_DESCRIPTION, "Profile");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(DataSource selected) {
                updateEnabled();
            }
        });
    }
}
