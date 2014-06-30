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
package com.sun.tools.visualvm.core.ui.actions;

import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import java.awt.event.ActionEvent;
import java.util.Set;
import org.openide.util.NbBundle;


/**
 *
 * @author Jiri Sedlacek
 */
class DeleteSnapshotAction extends MultiDataSourceAction<Snapshot> {
    
//    private static final Image ICON_16 =  Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/saveSnapshot.png");
//    private static final Image ICON_24 =  Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/saveSnapshot24.png");
    
    private static DeleteSnapshotAction instance;
    
    public static DeleteSnapshotAction instance() {
        if (instance == null) 
            instance = new DeleteSnapshotAction();
        return instance;
    }
    
    
    protected void actionPerformed(Set<Snapshot> snapshots, ActionEvent actionEvent) {
        for (Snapshot snapshot : snapshots) snapshot.delete();
    }

    protected boolean isEnabled(Set<Snapshot> snapshots) {
        for (Snapshot snapshot : snapshots)
            if (!snapshot.supportsDelete()) return false;
        return Utils.areDataSourcesIndependent(snapshots);
    }
    
    
    private DeleteSnapshotAction() {
        super(Snapshot.class);
        putValue(NAME, NbBundle.getMessage(DeleteSnapshotAction.class, "LBL_Delete"));  // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(DeleteSnapshotAction.class, "LBL_Delete_Snapshot")); // NOI18N
//        putValue(SMALL_ICON, new ImageIcon(ICON_16));
//        putValue("iconBase", new ImageIcon(ICON_24));
    }
}
