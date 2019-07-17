/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.application;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.core.ui.actions.MultiDataSourceAction;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.host.RemoteHostsContainer;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class RemoveFinishedApplicationsAction extends MultiDataSourceAction<Host> {
    
    private static RemoveFinishedApplicationsAction alwaysEnabled;
    private static RemoveFinishedApplicationsAction selectionAware;
    
    
    private final boolean tracksSelection;
    
    
    static synchronized RemoveFinishedApplicationsAction alwaysEnabled() {
        if (alwaysEnabled == null) alwaysEnabled = new RemoveFinishedApplicationsAction(false);
        return alwaysEnabled;
    }
    
    static synchronized RemoveFinishedApplicationsAction selectionAware() {
        if (selectionAware == null) selectionAware = new RemoveFinishedApplicationsAction(true);
        return selectionAware;
    }
    

    @Override
    protected void actionPerformed(final Set<Host> hosts, ActionEvent actionEvent) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                if (tracksSelection) {
                    removeApplications(hosts);
                } else {
                    Set<Host> _hosts = new HashSet();
                    _hosts.add(Host.LOCALHOST);
                    _hosts.addAll(RemoteHostsContainer.sharedInstance().getRepository().getDataSources(Host.class));
                    removeApplications(_hosts);
                }
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateState(ActionUtils.getSelectedDataSources(getScope()));
                    }
                });
            }
        });
    }

    @Override
    protected boolean isEnabled(Set<Host> hosts) {
        for (Host host : hosts) {
            Set<Application> applications = host.getRepository().getDataSources(Application.class);
            for (Application application : applications)
                if (isRemovable(application)) return true;
        }
        
        return false;
    }
    
    
    protected void updateState(Set<Host> hosts) {
        if (tracksSelection) super.updateState(hosts);
    }
    
    
    private void removeApplications(Set<Host> hosts) {
        for (Host host : hosts) {
            Set<Application> applications = host.getRepository().getDataSources(Application.class);
            for (Application application : applications) {
                if (isRemovable(application)) host.getRepository().removeDataSource(application);
            }
        }
    }
    
    
    private static boolean isRemovable(Application application) {
        return application.getState() != Stateful.STATE_AVAILABLE && application.supportsUserRemove();
    }
    
    
    private RemoveFinishedApplicationsAction(boolean tracksSelection) {
        super(Host.class);
        
        this.tracksSelection = tracksSelection;
        
        putValue(NAME, NbBundle.getMessage(RemoveFinishedApplicationsAction.class, "ACT_RemoveFinishedName"));  // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(RemoveFinishedApplicationsAction.class, "ACT_RemoveFinishedDescr")); // NOI18N
    }
    
}
