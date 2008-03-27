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
package com.sun.tools.visualvm.heapdump.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.coredump.CoreDump;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.MultiDataSourceAction;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

    
/**
 *
 * @author Jiri Sedlacek
 */
class HeapDumpAction extends MultiDataSourceAction<DataSource> {
    
    private Set<Application> lastSelectedApplications = new HashSet();
    private final PropertyChangeListener stateListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            updateState(ActionUtils.getSelectedDataSources());
        }
    };
    
    private static HeapDumpAction instance;
    
    public static synchronized HeapDumpAction instance() {
        if (instance == null) {
            instance = new HeapDumpAction();
            instance.initialize();
    }
        return instance;
    }
    
    
    protected void actionPerformed(Set<DataSource> dataSources, ActionEvent actionEvent) {
        for (DataSource dataSource : dataSources) {
                        if (dataSource instanceof Application) {
                            Application application = (Application)dataSource;
                HeapDumpSupport.getInstance().takeHeapDump(application, (actionEvent.getModifiers() & InputEvent.CTRL_MASK) == 0);
                        } else if (dataSource instanceof CoreDump) {
                            CoreDump coreDump = (CoreDump)dataSource;
                HeapDumpSupport.getInstance().takeHeapDump(coreDump, (actionEvent.getModifiers() & InputEvent.CTRL_MASK) == 0);
                        }
                    }
                }
    
    protected boolean isEnabled(Set<DataSource> dataSources) {
        for (DataSource dataSource : dataSources)
            if (dataSource instanceof Application) {
                // TODO: Listener should only be registered when heap dump is supported for the application
                Application application = (Application)dataSource;
                lastSelectedApplications.add(application);
                application.addPropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
                if (!HeapDumpSupport.getInstance().supportsHeapDump((Application)dataSource)) return false;
            } else if (!(dataSource instanceof CoreDump)) return false;
        return true;
    }
    
    protected void updateState(Set<DataSource> dataSources) {
        if (lastSelectedApplications == null) lastSelectedApplications = new HashSet();
        if (!lastSelectedApplications.isEmpty())
            for (Application application : lastSelectedApplications)
                application.removePropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
        super.updateState(dataSources);
    }
    
    
    private HeapDumpAction() {
        super(DataSource.class);
        putValue(NAME, "Heap Dump");
        putValue(SHORT_DESCRIPTION, "Heap Dump");
            }
    }
