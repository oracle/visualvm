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

import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategoriesListener;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.uisupport.UISupport;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

class LoadSnapshotAction extends AbstractAction {
    
    private static final String ICON_PATH = "com/sun/tools/visualvm/core/ui/resources/loadSnapshot.png";    // NOI18N
    private static final Image ICON =  ImageUtilities.loadImage(ICON_PATH);
    
    private String lastFile = null;
    private String lastFilter = null;
    
    
    private static LoadSnapshotAction instance;
        
    public static synchronized LoadSnapshotAction instance() {
        if (instance == null) {
            instance = new LoadSnapshotAction();

            instance.updateEnabled(true);
            RegisteredSnapshotCategories.sharedInstance().addCategoriesListener(new SnapshotCategoriesListener() {
                public void categoryRegistered(SnapshotCategory category) { instance.updateEnabled(false); }
                public void categoryUnregistered(SnapshotCategory category) { instance.updateEnabled(false); }
        });
    }
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        List<SnapshotCategory> categories = RegisteredSnapshotCategories.sharedInstance().getOpenSnapshotCategories();
        if (categories.isEmpty()) return; // TODO: should display a notification dialog
        
        final List<FileFilter> fileFilters = new ArrayList();
        for (SnapshotCategory category : categories) fileFilters.add(category.getFileFilter());
        
        JFileChooser chooser = new JFileChooser() {
            public void setSelectedFile(File file) {
                super.setSelectedFile(file);

                // safety check
                if (!(getUI() instanceof BasicFileChooserUI)) {
                    return;
                }

                // grab the ui and set the filename
                BasicFileChooserUI ui = (BasicFileChooserUI) getUI();
                ui.setFileName(file == null ? "" : file.getName());  // NOI18N
            }            
        };
        chooser.setDialogTitle(NbBundle.getMessage(LoadSnapshotAction.class, "LBL_Load"));  // NOI18N
        chooser.setAcceptAllFileFilterUsed(false);
        FileFilter allFilesFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                for (FileFilter ff : fileFilters)
                    if (ff.accept(f)) return true;
                return false;
            }
            @Override
            public String getDescription() {
                return NbBundle.getMessage(LoadSnapshotAction.class, "LBL_All_Snapshots");  // NOI18N
            }
            
        };
        chooser.addChoosableFileFilter(allFilesFilter);
        int filterIndex = -1;
        for (int i = 0; i < fileFilters.size(); i++) {
            FileFilter fileFilter = fileFilters.get(i);
            chooser.addChoosableFileFilter(fileFilter);
            if (fileFilter.getDescription().equals(lastFilter)) filterIndex = i;
        }
        if (lastFile != null) chooser.setSelectedFile(new File(lastFile));
        chooser.setFileFilter(filterIndex == -1 ? allFilesFilter : fileFilters.get(filterIndex));
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile == null || !selectedFile.exists()) {
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                                            NbBundle.getMessage(LoadSnapshotAction.class,
                                            "MSG_Selected_file_does_not_exist"), // NOI18N
                                            NotifyDescriptor.ERROR_MESSAGE));
            } else {
                FileFilter fileFilter = chooser.getFileFilter();
                if (fileFilter == allFilesFilter) {
                    for (FileFilter ff : fileFilters)
                        if (ff.accept(selectedFile)) {
                            lastFile = selectedFile.getAbsolutePath();
                            lastFilter = null;
                            categories.get(fileFilters.indexOf(ff)).openSnapshot(selectedFile);
                            return;
                        }
                } else if (fileFilter.accept(selectedFile)) {
                    lastFile = selectedFile.getAbsolutePath();
                    lastFilter = fileFilter.getDescription();
                    categories.get(fileFilters.indexOf(fileFilter)).openSnapshot(selectedFile);
                    return;
                }
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                                        NbBundle.getMessage(LoadSnapshotAction.class,
                                        "MSG_Selected_file_does_not_match_snapshot_type"), // NOI18N
                                        NotifyDescriptor.ERROR_MESSAGE));
            }
        }
    }
    
    private void updateEnabled(boolean lazily) {
        final boolean isEnabled = !RegisteredSnapshotCategories.sharedInstance().getOpenSnapshotCategories().isEmpty();
        
        Runnable updater = new Runnable() {
            public void run() { setEnabled(isEnabled); }
        };
        if (lazily) UISupport.runInEventDispatchThread(updater);
        else UISupport.runInEventDispatchThreadAndWait(updater);
    }
    
    
    private LoadSnapshotAction() {
        putValue(NAME, NbBundle.getMessage(LoadSnapshotAction.class, "LBL_Load1")); // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(LoadSnapshotAction.class, "LBL_Load_Snapshot"));    // NOI18N
        putValue(SMALL_ICON, new ImageIcon(ICON));
        putValue("iconBase", ICON_PATH);    // NOI18N
    }
}
