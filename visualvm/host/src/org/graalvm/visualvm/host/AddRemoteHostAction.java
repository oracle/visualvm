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
package org.graalvm.visualvm.host;

import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import org.graalvm.visualvm.host.impl.HostCustomizer;
import org.graalvm.visualvm.host.impl.HostProperties;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.core.VisualVM;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddRemoteHostAction extends SingleDataSourceAction<RemoteHostsContainer> {
    
    private static final String ICON_PATH = "org/graalvm/visualvm/host/resources/addRemoteHost.png";  // NOI18N
    private static final Image ICON =  ImageUtilities.loadImage(ICON_PATH);
    
    private boolean tracksSelection = false;
    
    private static AddRemoteHostAction alwaysEnabled;
    private static AddRemoteHostAction selectionAware;
    
    
    public static synchronized AddRemoteHostAction alwaysEnabled() {
        if (alwaysEnabled == null) {
            alwaysEnabled = new AddRemoteHostAction();
            alwaysEnabled.putValue(SMALL_ICON, new ImageIcon(ICON));
            alwaysEnabled.putValue("iconBase", ICON_PATH);  // NOI18N
        }
        return alwaysEnabled;
    }
    
    public static synchronized AddRemoteHostAction selectionAware() {
        if (selectionAware == null) {
            selectionAware = new AddRemoteHostAction();
            selectionAware.tracksSelection = true;
        }
        return selectionAware;
    }
    
    
    protected void actionPerformed(RemoteHostsContainer remoteHostsContainer, ActionEvent actionEvent) {
        final HostProperties hostDescriptor = HostCustomizer.defineHost();
        if (hostDescriptor != null) {
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    HostsSupport.getInstance().createHost(hostDescriptor, true, true);
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
    
    
    private AddRemoteHostAction() {
        super(RemoteHostsContainer.class);
        putValue(NAME, NbBundle.getMessage(AddRemoteHostAction.class, "LBL_Add_Remote_Host"));  // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(AddRemoteHostAction.class, "ToolTip_Add_Remote_Host")); // NOI18N
    }
}
