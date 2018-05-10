/*
 *  Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.awt.Mnemonics;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.Presenter;

/**
 * Support for reopening already opened snapshots.
 *
 * @author Jiri Sedlacek
 * 
 * @since VisualVM 1.4.1
 */
class LoadRecentSnapshot implements Presenter.Menu {
    
    private static final String PROP_LOAD_RECENT = "LoadRecentSnapshot.item."; // NOI18N
    
    private static final int MAX_RECENT_ITEMS = 9;
    
    private static LoadRecentSnapshot INSTANCE;
    
    private final Preferences prefs;
    private final List<String> files;
    
    private JMenu menu;
    
    
    public static synchronized LoadRecentSnapshot instance() {
        if (INSTANCE == null) INSTANCE = new LoadRecentSnapshot();
        return INSTANCE;
    }
    
    void setupMenu() {
        assert SwingUtilities.isEventDispatchThread();
        menu.removeAll();
        if (files.isEmpty()) {
            menu.add(new JMenuItem(NbBundle.getMessage(LoadRecentSnapshot.class, "LoadRecentSnapshot_NoRecentSnapshots")) {{ setEnabled(false); }}); // NOI18N
        } else {
            int i = 0;
            for (String file : files) menu.add(new RecentFileItem(file, ++i));
            menu.addSeparator();
            menu.add(new ClearRecentItem());
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        if (menu == null) {
            menu = new JMenu();
            Mnemonics.setLocalizedText(menu, NbBundle.getMessage(LoadRecentSnapshot.class, "LoadRecentSnapshot_LoadRecentItem")); // NOI18N
        }
        setupMenu();
        return menu;
    }
    
    
    void addFile(File file) {
        String path = file.getAbsolutePath();
        
        if (files.contains(path)) {
            files.remove(path);
            files.add(0, path);
        } else {
            if (files.size() == MAX_RECENT_ITEMS) files.remove(MAX_RECENT_ITEMS - 1);
            files.add(0, path);
        }
        
        saveFiles(prefs, files);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setupMenu();
            }
        });
    }
    
    
    private static List<String> loadFiles(Preferences p) {
        List<String> f = new ArrayList(MAX_RECENT_ITEMS);
        
        int i = 0;
        String s = p.get(PROP_LOAD_RECENT + i, null);
        while (s != null && i < MAX_RECENT_ITEMS) {
            f.add(s);
            s = p.get(PROP_LOAD_RECENT + ++i, null);
        }
        
        return f;
    }
    
    private static void saveFiles(Preferences p, List<String> f) {
        int i = 0;
        while (p.get(PROP_LOAD_RECENT + i, null) != null) p.remove(PROP_LOAD_RECENT + i++);
        
        i = 0;
        for (String s : f) p.put(PROP_LOAD_RECENT + i++, s);
    }
    
    
    private LoadRecentSnapshot() {
        prefs = NbPreferences.forModule(LoadRecentSnapshot.class);
        files = loadFiles(prefs);
    }
    
    
    private class RecentFileItem extends JMenuItem {
        
        private final String f;
        private final File file;
        
        RecentFileItem(String f, int idx) {
            this.f = f;
            file = new File(f);
            Mnemonics.setLocalizedText(this, "&" + idx + ". " + file.getName()); // NOI18N
        }
        
        @Override
        protected void fireStateChanged() {
            boolean active = isSelected() || isArmed();
            StatusDisplayer.getDefault().setStatusText(active ? file.getAbsolutePath() : null);
            super.fireStateChanged();
        }
        
        @Override
        protected void fireActionPerformed(ActionEvent e) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    if (file.exists()) {
                        List<SnapshotCategory> categories = RegisteredSnapshotCategories.sharedInstance().getOpenSnapshotCategories();
                        List<FileFilter> fileFilters = new ArrayList();
                        for (SnapshotCategory category : categories) fileFilters.add(category.getFileFilter());
                        
                        for (FileFilter ff : fileFilters) if (ff.accept(file)) {
                            categories.get(fileFilters.indexOf(ff)).openSnapshot(file);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    files.remove(f);
                                    files.add(0, f);
                                    saveFiles(prefs, files);
                                    setupMenu();
                                }
                            });
                            return;
                        }
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                // NOTE: this happens when the appropriate SnapshotCategory is not registered
                                //       should the snapshot be kept in the list?
                                files.remove(f);
                                saveFiles(prefs, files);
                                setupMenu();
                                ProfilerDialogs.displayError(MessageFormat.format(NbBundle.getMessage(LoadRecentSnapshot.class, "LoadRecentSnapshot_CannotLoadMsg"), file.getName()));
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                files.remove(f);
                                saveFiles(prefs, files);
                                setupMenu();
                                ProfilerDialogs.displayError(MessageFormat.format(NbBundle.getMessage(LoadRecentSnapshot.class, "LoadRecentSnapshot_NotAvailableMsg"), file.getName()));
                            }
                        });
                    }
                }
            });
        }
        
    }
    
    private class ClearRecentItem extends JMenuItem {
        
        ClearRecentItem() {
            Mnemonics.setLocalizedText(this, NbBundle.getMessage(LoadRecentSnapshot.class, "LoadRecentSnapshot_ClearRecentSnapshots")); // NOI18N
        }
        
        @Override
        protected void fireActionPerformed(ActionEvent e) {
            files.clear();
            saveFiles(prefs, files);
            setupMenu();
        }
        
    }
    
}
