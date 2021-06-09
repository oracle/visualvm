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
package org.graalvm.visualvm.application.snapshot;

import org.graalvm.visualvm.core.snapshot.SnapshotsContainer;
import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddApplicationSnapshotAction extends SingleDataSourceAction<SnapshotsContainer> {
    
    private static final String ICON_PATH = "org/graalvm/visualvm/application/resources/addApplicationSnapshot.png";  // NOI18N
    private static final Image ICON =  ImageUtilities.loadImage(ICON_PATH);
    
    private boolean tracksSelection = false;
    
    
    private static AddApplicationSnapshotAction alwaysEnabled;
    private static AddApplicationSnapshotAction selectionAware;
    
    
    static synchronized AddApplicationSnapshotAction alwaysEnabled() {
        if (alwaysEnabled == null) {
            alwaysEnabled = new AddApplicationSnapshotAction();
            alwaysEnabled.putValue(SMALL_ICON, new ImageIcon(ICON));
            alwaysEnabled.putValue("iconBase", ICON_PATH);  // NOI18N
        }
        return alwaysEnabled;
    }
    
    static synchronized AddApplicationSnapshotAction selectionAware() {
        if (selectionAware == null) {
            selectionAware = new AddApplicationSnapshotAction();
            selectionAware.tracksSelection = true;
        }
        return selectionAware;
    }
    
    
    protected void actionPerformed(SnapshotsContainer snapshotsContainer, ActionEvent actionEvent) {
        ApplicationSnapshotConfigurator newSnapshotConfiguration = ApplicationSnapshotConfigurator.defineSnapshot();
        if (newSnapshotConfiguration != null) {
            ApplicationSnapshotProvider provider = ApplicationSnapshotsSupport.getInstance().getSnapshotProvider();
            provider.addSnapshotArchive(newSnapshotConfiguration.getSnapshotFile(), newSnapshotConfiguration.deleteSourceFile());
        }
    }
    
    protected boolean isEnabled(SnapshotsContainer snapshotsContainer) {
        return true;
    }
    
    protected void updateState(Set<SnapshotsContainer> snapshotsContainerSet) {
        if (tracksSelection) super.updateState(snapshotsContainerSet);
    }
    
    
    private AddApplicationSnapshotAction() {
        super(SnapshotsContainer.class);
        putValue(Action.NAME, NbBundle.getMessage(AddApplicationSnapshotAction.class, "LBL_Add_Application_Snapshot")); // NOI18N
        putValue(Action.SHORT_DESCRIPTION, NbBundle.getMessage(AddApplicationSnapshotAction.class, "ToolTip_Add_Application_Snapshot"));    // NOI18N
    }
}
