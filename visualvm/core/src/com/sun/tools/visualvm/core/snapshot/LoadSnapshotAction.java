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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.openide.util.Utilities;

public final class LoadSnapshotAction extends AbstractAction {
    
    private static LoadSnapshotAction instance;
    
    private static final String ICON_PATH = "com/sun/tools/visualvm/core/ui/resources/loadSnapshot.png";
    private static final Image ICON =  Utilities.loadImage(ICON_PATH);
    
    private String lastFile = null;
    
    
    public static synchronized LoadSnapshotAction getInstance() {
        if (instance == null) instance = new LoadSnapshotAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        List<SnapshotCategory> categories = RegisteredSnapshotCategories.sharedInstance().getOpenSnapshotCategories();
        if (categories.isEmpty()) return; // TODO: should display a notification dialog
        
        List<FileFilter> fileFilters = new ArrayList();
        for (SnapshotCategory category : categories) fileFilters.add(category.getFileFilter());
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load");
        if (lastFile != null) chooser.setSelectedFile(new File(lastFile));
        chooser.setAcceptAllFileFilterUsed(false);
        for (FileFilter fileFilter : fileFilters) chooser.addChoosableFileFilter(fileFilter);
        chooser.setFileFilter(fileFilters.get(0));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            lastFile = selectedFile.getAbsolutePath();
            categories.get(fileFilters.indexOf(chooser.getFileFilter())).openSnapshot(selectedFile);
        }
    }
    
    void updateEnabled() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setEnabled(!RegisteredSnapshotCategories.sharedInstance().getOpenSnapshotCategories().isEmpty());
            }
        });
    }
    
    
    private LoadSnapshotAction() {
        putValue(Action.NAME, "Load...");
        putValue(Action.SHORT_DESCRIPTION, "Load Snapshot");
        putValue(Action.SMALL_ICON, new ImageIcon(ICON));
        putValue("iconBase", ICON_PATH);
        
        updateEnabled();
    }
}
