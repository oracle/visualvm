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
package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.utils.IDEUtils;

public final class DeleteSnapshotAction extends AbstractAction {
    
    private static DeleteSnapshotAction instance;
    
//    private static final Image ICON_16 =  Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/saveSnapshot.png");
//    private static final Image ICON_24 =  Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/saveSnapshot24.png");
    
    
    public static synchronized DeleteSnapshotAction getInstance() {
        if (instance == null) instance = new DeleteSnapshotAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        Snapshot selectedSnapshot = getSelectedSnapshot();
        if (selectedSnapshot != null && selectedSnapshot.supportsDelete()) selectedSnapshot.delete();
    }
    
    void updateEnabled() {
        Snapshot selectedSnapshot = getSelectedSnapshot();
        final boolean isEnabled = selectedSnapshot != null && selectedSnapshot.supportsDelete();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() { setEnabled(isEnabled); }
        });
    }
    
    private Snapshot getSelectedSnapshot() {
        DataSource selectedDataSource = ExplorerSupport.sharedInstance().getSelectedDataSource();
        if (selectedDataSource == null) return null;
        return selectedDataSource instanceof Snapshot ? (Snapshot)selectedDataSource : null;
    }
    
    
    private DeleteSnapshotAction() {
        putValue(Action.NAME, "Delete");
        putValue(Action.SHORT_DESCRIPTION, "Delete Snapshot");
//        putValue(Action.SMALL_ICON, new ImageIcon(ICON_16));
//        putValue("iconBase", new ImageIcon(ICON_24));
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(DataSource selected) {
                updateEnabled();
            }
        });
    }
}
