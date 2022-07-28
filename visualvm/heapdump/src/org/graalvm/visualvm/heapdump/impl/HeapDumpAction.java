/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapdump.impl;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.coredump.CoreDump;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.core.ui.actions.MultiDataSourceAction;
import org.graalvm.visualvm.heapdump.HeapDumpSupport;
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
class HeapDumpAction extends MultiDataSourceAction<DataSource> {
    
    private Set<Application> lastSelectedApplications = new HashSet<>();
    private final PropertyChangeListener stateListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            updateState(ActionUtils.getSelectedDataSources());
        }
    };
    
    private static HeapDumpAction INSTANCE;
    
    public static synchronized HeapDumpAction instance() {
        if (INSTANCE == null) INSTANCE = new HeapDumpAction();
        return INSTANCE;
    }
    
    
    protected void actionPerformed(Set<DataSource> dataSources, ActionEvent actionEvent) {
        HeapDumpSupport support = HeapDumpSupport.getInstance();
        for (DataSource dataSource : dataSources) {
            if (dataSource instanceof Application) {
                Application application = (Application)dataSource;
                boolean tagged = (actionEvent.getModifiers() & Toolkit.
                        getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
                if (application.isLocalApplication()) {
                    support.takeHeapDump(application, !tagged); 
                } else {
                    support.takeRemoteHeapDump(application, null, !tagged);
                }
            } else if (dataSource instanceof CoreDump) {
                CoreDump coreDump = (CoreDump)dataSource;
                support.takeHeapDump(coreDump, (actionEvent.getModifiers() &
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        }
    }
    
    protected boolean isEnabled(Set<DataSource> dataSources) {
        HeapDumpSupport support = HeapDumpSupport.getInstance();
        for (DataSource dataSource : dataSources)
            if (dataSource instanceof Application) {
                // TODO: Listener should only be registered when heap dump is supported for the application
                Application application = (Application)dataSource;
                lastSelectedApplications.add(application);
                application.addPropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
                if (application.getState() != Stateful.STATE_AVAILABLE) return false;
                if (application.isLocalApplication()) {
                    if (!support.supportsHeapDump(application)) return false;
                } else {
                    if (!support.supportsRemoteHeapDump(application)) return false;
                }
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
    
    
    private HeapDumpAction() {
        super(DataSource.class);
        putValue(NAME, NbBundle.getMessage(HeapDumpAction.class, "MSG_Heap_Dump")); // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(HeapDumpAction.class, "LBL_Heap_Dump"));    // NOI18N
    }
    
}
