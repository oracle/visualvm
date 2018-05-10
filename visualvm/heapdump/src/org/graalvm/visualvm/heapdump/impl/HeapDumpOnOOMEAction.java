/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.heapdump.impl;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
class HeapDumpOnOOMEAction extends SingleDataSourceAction<Application> {
    
    private boolean oomeEnabled;
    private Application lastSelectedApplication;
    private final PropertyChangeListener stateListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            updateState(ActionUtils.getSelectedDataSources(Application.class));
        }
    };
    
    private static HeapDumpOnOOMEAction instance;
    
    public static synchronized HeapDumpOnOOMEAction instance() {
        if (instance == null) 
            instance = new HeapDumpOnOOMEAction();
        return instance;
    }
    

    protected void actionPerformed(Application application, ActionEvent actionEvent) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        jvm.setDumpOnOOMEnabled(!oomeEnabled);
        updateState(jvm);
    }

    protected boolean isEnabled(Application application) {
        lastSelectedApplication = application;
        lastSelectedApplication.addPropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (!jvm.isDumpOnOOMEnabledSupported()) return false;
        updateState(jvm);
        return true;
    }
    
    
    private void updateState(Jvm jvm) {
        oomeEnabled = jvm.isDumpOnOOMEnabled();
        if (oomeEnabled) {
            putValue(NAME, NbBundle.getMessage(HeapDumpOnOOMEAction.class, "LBL_Disable_Heap_Dump_on_OOME"));  // NOI18N
            putValue(SHORT_DESCRIPTION, NbBundle.getMessage(HeapDumpOnOOMEAction.class, "DESCR_Disable_Heap_Dump_on_OOME"));  // NOI18N
        } else {
            putValue(NAME, NbBundle.getMessage(HeapDumpOnOOMEAction.class, "LBL_Enable_Heap_Dump_on_OOME"));  // NOI18N
            putValue(SHORT_DESCRIPTION, NbBundle.getMessage(HeapDumpOnOOMEAction.class, "DESCR_Enable_Heap_Dump_on_OOME"));  // NOI18N
        }
    }
    
    protected void updateState(Set<Application> applications) {
        if (lastSelectedApplication != null) {
            lastSelectedApplication.removePropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
            lastSelectedApplication = null;
        }
        super.updateState(applications);
    }
    
    
    private HeapDumpOnOOMEAction() {
        super(Application.class);
    }

}
