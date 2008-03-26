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
package com.sun.tools.visualvm.core.ui.actions;

import com.sun.tools.visualvm.core.snapshot.Snapshot;
import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import org.openide.util.Utilities;


/**
 *
 * @author Jiri Sedlacek
 */
class SaveSnapshotAsAction extends SingleDataSourceAction<Snapshot> {
    
    private static final String ICON_PATH = "com/sun/tools/visualvm/core/ui/resources/saveSnapshot.png";
    private static final Image ICON = Utilities.loadImage(ICON_PATH);
    
    
    public static SaveSnapshotAsAction create() {
        SaveSnapshotAsAction action = new SaveSnapshotAsAction();
        action.initialize();
        return action;
    }
    
    
    protected void actionPerformed(Snapshot snapshot, ActionEvent actionEvent) {
        snapshot.saveAs();
    }

    protected boolean isEnabled(Snapshot snapshot) {
        return snapshot.supportsSaveAs();
    }
    
    
    private SaveSnapshotAsAction() {
        super(Snapshot.class);
        putValue(NAME, "Save As...");
        putValue(SHORT_DESCRIPTION, "Save Snapshot As");
        putValue(SMALL_ICON, new ImageIcon(ICON));
        putValue("iconBase", ICON_PATH);
    }
}
