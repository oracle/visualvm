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
package org.graalvm.visualvm.profiler;

import org.graalvm.visualvm.application.Application;
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
 */
final class ProfileApplicationAction extends SingleDataSourceAction<Application> {
    
    private Application lastSelectedApplication;
    private final PropertyChangeListener stateListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            updateState(ActionUtils.getSelectedDataSources(Application.class));
        }
    };
    
        
    private static ProfileApplicationAction instance;
    
    public static synchronized ProfileApplicationAction instance() {
        if (instance == null) 
            instance = new ProfileApplicationAction();
        return instance;
    }
    
        
    protected void actionPerformed(Application application, ActionEvent actionEvent) {
        ProfilerSupport.getInstance().selectProfilerView(application);
    }
    
    protected boolean isEnabled(Application application) {
        // TODO: Listener should only be registered when profiling the application is supported
        lastSelectedApplication = application;
        lastSelectedApplication.addPropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        return ProfilerSupport.getInstance().supportsProfiling(application);
    }
    
    protected void updateState(Set<Application> applications) {
        if (lastSelectedApplication != null) {
            lastSelectedApplication.removePropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
            lastSelectedApplication = null;
        }
        super.updateState(applications);
    }
    
    protected void initialize() {
        if (ProfilerSupport.getInstance().isInitialized()) {
            super.initialize();
        } else {
            setEnabled(false);
        }
    }
    
    
    private ProfileApplicationAction() {
        super(Application.class);
        putValue(NAME, NbBundle.getMessage(ProfileApplicationAction.class, "MSG_Profile")); // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(ProfileApplicationAction.class, "DESCR_Profile"));    // NOI18N
    }
}
