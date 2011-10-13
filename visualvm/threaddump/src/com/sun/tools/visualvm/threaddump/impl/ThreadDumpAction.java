/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package com.sun.tools.visualvm.threaddump.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.coredump.CoreDump;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.MultiDataSourceAction;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import org.openide.util.NbBundle;

    
/**
 *
 * @author Jiri Sedlacek
 */
class ThreadDumpAction extends MultiDataSourceAction<DataSource> {
    
    private Set<Application> lastSelectedApplications = new HashSet();
    private final PropertyChangeListener stateListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            updateState(ActionUtils.getSelectedDataSources());
        }
    };
    
    private static ThreadDumpAction instance;
    
    public static synchronized ThreadDumpAction instance() {
        if (instance == null) 
            instance = new ThreadDumpAction();
        return instance;
    }
    
    
    protected void actionPerformed(Set<DataSource> dataSources, ActionEvent actionEvent) {
        for (DataSource dataSource : dataSources) {
            if (dataSource instanceof Application) {
                Application application = (Application)dataSource;
                ThreadDumpSupport.getInstance().takeThreadDump(application, (actionEvent.getModifiers() &
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            } else if (dataSource instanceof CoreDump) {
                CoreDump coreDump = (CoreDump)dataSource;
                ThreadDumpSupport.getInstance().takeThreadDump(coreDump, (actionEvent.getModifiers() &
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        }
    }
    
    
    protected boolean isEnabled(Set<DataSource> dataSources) {
        for (DataSource dataSource : dataSources)
            if (dataSource instanceof Application) {
                // TODO: Listener should only be registered when thread dump is supported for the application
                Application application = (Application)dataSource;
                    lastSelectedApplications.add(application);
                application.addPropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
                if (application.getState() != Stateful.STATE_AVAILABLE) return false;
                if (!ThreadDumpSupport.getInstance().supportsThreadDump((Application)dataSource)) return false;
            } else if (!(dataSource instanceof CoreDump)) return false;
        return true;
    }
    
    protected void updateState(Set<DataSource> dataSources) {
        if (!lastSelectedApplications.isEmpty())
            for (Application application : lastSelectedApplications)
                application.removePropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
        lastSelectedApplications.clear();
        super.updateState(dataSources);
    }
    
    
    private ThreadDumpAction() {
        super(DataSource.class);
        putValue(NAME, NbBundle.getMessage(ThreadDumpAction.class, "MSG_Thread_Dump"));  // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(ThreadDumpAction.class, "LBL_Thread_Dump"));     // NOI18N
    }
}
