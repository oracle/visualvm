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
package com.sun.tools.visualvm.host.impl;

import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import com.sun.tools.visualvm.host.HostsSupport;
import com.sun.tools.visualvm.host.RemoteHostsContainer;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.ImageIcon;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddRemoteHostAction extends SingleDataSourceAction<RemoteHostsContainer> {
    
    private static final String ICON_PATH = "com/sun/tools/visualvm/host/resources/addRemoteHost.png";
    private static final Image ICON =  Utilities.loadImage(ICON_PATH);
    
    private boolean tracksSelection = false;
    
    private static AddRemoteHostAction alwaysEnabled;
    private static AddRemoteHostAction toolbarInstance;
    private static AddRemoteHostAction selectionAware;
    
    public static synchronized AddRemoteHostAction alwaysEnabled() {
        if (alwaysEnabled == null) {
            alwaysEnabled = new AddRemoteHostAction();
            alwaysEnabled.initialize();
    }
        return alwaysEnabled;
    }
    
    public static synchronized AddRemoteHostAction toolbarInstance() {
        if (toolbarInstance == null) {
            toolbarInstance = new AddRemoteHostAction();
            toolbarInstance.putValue(SMALL_ICON, new ImageIcon(ICON));
            toolbarInstance.putValue("iconBase", ICON_PATH);
            toolbarInstance.initialize();
    }
        return toolbarInstance;
    }
    
    public static synchronized AddRemoteHostAction selectionAware() {
        if (selectionAware == null) {
            selectionAware = new AddRemoteHostAction().trackSelection();
            selectionAware.initialize();
    }
        return selectionAware;
    }
    
    
    protected void actionPerformed(RemoteHostsContainer remoteHostsContainer, ActionEvent actionEvent) {
        final HostProperties hostDescriptor = HostCustomizer.defineHost();
        if (hostDescriptor != null) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    HostsSupport.getInstance().getHostProvider().createHost(hostDescriptor, true);
                }
            });
        }
    }
    
    protected boolean isEnabled(RemoteHostsContainer remoteHostsContainer) {
        return true;
    }
    
    protected void updateState(Set<RemoteHostsContainer> remoteHostsContainerSet) {
        if (tracksSelection) super.updateState(remoteHostsContainerSet);
    }
    
    
    private AddRemoteHostAction trackSelection() {
        tracksSelection = true;
        updateState(ActionUtils.getSelectedDataSources(RemoteHostsContainer.class));
        return this;
    }
    
    
    private AddRemoteHostAction() {
        super(RemoteHostsContainer.class);
        putValue(NAME, "Add Remote Host...");
        putValue(SHORT_DESCRIPTION, "Add Remote Host");
    }
}
