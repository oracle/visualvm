/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.v2.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.actions.CompareSnapshotsAction;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProfilerStorage;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.ui.ProjectSelector;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "SnapshotsWindowUI_name=Snapshots",
    "#NOI18N",
    "SnapshotsWindowUI_mode=properties",
    "SnapshotsWindowUI_colType=Type",
    "SnapshotsWindowUI_colName=Name",
    "SnapshotsWindowUI_lblProject=Project:",
    "SnapshotsWindowUI_lblSnapshots=Snapshots:",
    "SnapshotsWindowUI_actOpenSnapshots=Open",
    "SnapshotsWindowUI_actExportSnapshot=Export...",
    "SnapshotsWindowUI_actCompareSnapshots=Compare",
    "SnapshotsWindowUI_actRenameSnapshot=Rename...",
    "SnapshotsWindowUI_actDeleteSnapshots=Delete",
    "SnapshotsWindowUI_descOpenSnapshots=Open selected snapshots",
    "SnapshotsWindowUI_descExportSnapshot=Export selected snapshots...",
    "SnapshotsWindowUI_descCompareSnapshots=Compare selected snapshots",
    "SnapshotsWindowUI_descRenameSnapshot=Rename selected snapshot...",
    "SnapshotsWindowUI_descDeleteSnapshots=Delete selected snapshots",
    "SnapshotsWindowUI_msgCannotCompareSnapshots=Selected snapshots cannot be compared.",
    "SnapshotsWindowUI_capRenameSnapshot=Rename Snapshot",
    "SnapshotsWindowUI_msgNameEmpty=Snapshot name cannot be empty.",
    "SnapshotsWindowUI_msgRenameFailed=Failed to rename {0}",
    "SnapshotsWindowUI_capDeleteSnapshots=Confirm Delete",
    "SnapshotsWindowUI_msgDeleteSnapshots=Delete selected snapshots?",
    "SnapshotsWindowUI_msgDeleteFailed=Failed to delete {0}",
    "SnapshotsWindowUI_lblNewFile=&New file name:",
    "SnapshotsWindowUI_ttpSnapshotType=Snapshot type",
    "SnapshotsWindowUI_ttpSnapshotName=Snapshot name"
})
public final class SnapshotsWindowUI extends ProfilerTopComponent {
    
    public static final String ID = "SnapshotsWindowUI"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx("SnapshotsWindow.HelpCtx"); // NOI18N
    
    
    // --- Instance ------------------------------------------------------------
    
    public SnapshotsWindowUI() {
        setName(Bundle.SnapshotsWindowUI_name());
        setIcon(Icons.getImage(ProfilerIcons.SNAPSHOT_TAKE));
        
        putClientProperty(ID, ID);
        
        initUI();
    }
    
    
    // --- Internal API --------------------------------------------------------
    
    public void refreshFolder(FileObject folder, boolean fullRefresh) {
        // Converting to Files as comparing FileObjects doesn't work for global storage
        File f1 = folder == null ? null : FileUtil.toFile(folder);
        File f2 = currentFolder == null ? null : FileUtil.toFile(currentFolder);
        if (Objects.equals(f1, f2)) {
            if (fullRefresh) refreshSnapshots();
            else snapshotsTableModel.fireTableDataChanged();
        }
    }
    
    public void setProject(Lookup.Provider project) {
        selector.setProject(project);
    }
    
    public void resetProject(Lookup.Provider project) {
        selector.resetProject(project);
    }
    
    
    // --- Implementation ------------------------------------------------------
    
    private ProfilerTable snapshotsTable;
    
    private ProjectSelector selector;
    private ChangeListener openProjectsListener;
    
    private FileObject currentFolder;
    private final List<Snapshot> snapshots = new ArrayList();
    
    private Action openA;
    private Action exportA;
    private Action compareA;
    private Action renameA;
    private Action deleteA;

    private final AbstractTableModel snapshotsTableModel = new AbstractTableModel() {
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return Bundle.SnapshotsWindowUI_colType();
            } else if (columnIndex == 1) {
                return Bundle.SnapshotsWindowUI_colName();
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Integer.class;
            } else if (columnIndex == 1) {
                return Snapshot.class;
            }
            return null;
        }

        public int getRowCount() {
            return snapshots.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return snapshots.get(rowIndex).getSnapshotType();
            } else if (columnIndex == 1) {
                return snapshots.get(rowIndex);
            }
            return null;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

    };
    
    
    void refreshSnapshots() {
        final Lookup.Provider project = selector.getProject();
        SnapshotsWindowHelper.PROCESSOR.post(new Runnable() {
            public void run() {
                ResultsManager rm = ResultsManager.getDefault();
                final List<Snapshot> _snapshots = new ArrayList();
                for (FileObject fo : rm.listSavedSnapshots(project, null))
                    _snapshots.add(new Snapshot(fo) {
                        boolean alternativeSorting() {
                            return snapshotsTable.getSortColumn() == 0;
                        }
                    });
                for (FileObject fo : rm.listSavedHeapdumps(project, null))
                    _snapshots.add(new Snapshot(fo) {
                        boolean alternativeSorting() {
                            return snapshotsTable.getSortColumn() == 0;
                        }
                    });
                FileObject __currentFolder = null;
                try {
                    __currentFolder = ProfilerStorage.getProjectFolder(project, false);
                } catch (IOException e) {}
                final FileObject _currentFolder = __currentFolder;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        currentFolder = _currentFolder;
                        snapshots.clear();
                        snapshots.addAll(_snapshots);
                        snapshotsTableModel.fireTableDataChanged();
                    }
                });
            }
        });
    }
    
    
    // --- UI ------------------------------------------------------------------
    
    private void initUI() {
        JPanel contents = new JPanel(new GridBagLayout());
        contents.setOpaque(true);
        contents.setBackground(UIUtils.getProfilerResultsBackground());
        
        GridBagConstraints c;
        int y = 0;
        
        JLabel projectSelectL = new JLabel(Bundle.SnapshotsWindowUI_lblProject(), JLabel.LEADING);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 10, 0, 10);
        contents.add(projectSelectL, c);
        
        ProjectSelector.Populator populator = new ProjectSelector.Populator() {
            protected Lookup.Provider initialProject() {
                ProfilerSession ps = ProfilerSession.currentSession();
                return ps == null ? ProjectUtilities.getMainProject() : ps.getProject();
            }
            protected Collection<Lookup.Provider> additionalProjects() {
                ProfilerSession ps = ProfilerSession.currentSession();
                Lookup.Provider cp = ps == null ? null : ps.getProject();
                if (cp != null) return Collections.singleton(cp);
                else return super.additionalProjects();
            }
        };
        
        selector = new ProjectSelector(populator) {
            protected void selectionChanged() { refreshSnapshots(); }
        };
        
        c = new GridBagConstraints();
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 10, 0, 10);
        contents.add(selector, c);
        
        JLabel snapshotsListL = new JLabel(Bundle.SnapshotsWindowUI_lblSnapshots(), JLabel.LEADING);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 10, 0, 10);
        contents.add(snapshotsListL, c);
        
        openA = new AbstractAction(Bundle.SnapshotsWindowUI_actOpenSnapshots()) {
            {
                putValue("BTN_TOOLTIP", Bundle.SnapshotsWindowUI_descOpenSnapshots()); // NOI18N
            }
            public void actionPerformed(ActionEvent e) {
                openSnapshots(snapshotsTable.getSelectedValues(1));
            }
        };
        
        exportA = new AbstractAction(Bundle.SnapshotsWindowUI_actExportSnapshot()) {
            {
                putValue("BTN_TOOLTIP", Bundle.SnapshotsWindowUI_descExportSnapshot()); // NOI18N
            }
            public void actionPerformed(ActionEvent e) {
                exportSnapshots(snapshotsTable.getSelectedValues(1));
            }
        };
        
        compareA = new AbstractAction(Bundle.SnapshotsWindowUI_actCompareSnapshots()) {
            {
                putValue("BTN_TOOLTIP", Bundle.SnapshotsWindowUI_descCompareSnapshots()); // NOI18N
            }
            public void actionPerformed(ActionEvent e) {
                List<Snapshot> snapshots = snapshotsTable.getSelectedValues(1);
                compareSnapshots(snapshots.get(0), snapshots.get(1));
            }
        };
        
        renameA = new AbstractAction(Bundle.SnapshotsWindowUI_actRenameSnapshot()) {
            {
                putValue("BTN_TOOLTIP", Bundle.SnapshotsWindowUI_descRenameSnapshot()); // NOI18N
            }
            public void actionPerformed(ActionEvent e) {
                Snapshot s = (Snapshot)snapshotsTable.getSelectedValue(1);
                renameSnapshot(s, snapshotsTableModel);
            }
        };
        
        deleteA = new AbstractAction(Bundle.SnapshotsWindowUI_actDeleteSnapshots()) {
            {
                putValue("BTN_TOOLTIP", Bundle.SnapshotsWindowUI_descDeleteSnapshots()); // NOI18N
            }
            public void actionPerformed(ActionEvent e) {
                deleteSnapshots(snapshotsTable.getSelectedValues(1));
            }
        };
        
        snapshotsTable = new ProfilerTable(snapshotsTableModel, true, true, null) {
            protected void populatePopup(final JPopupMenu popup, Object value, Object userValue) {
                popup.add(new JMenuItem(openA) {
                    { setFont(popup.getFont().deriveFont(Font.BOLD)); }
                });
                popup.add(new JMenuItem(exportA));
                popup.add(new JMenuItem(compareA));
                popup.add(new JMenuItem(renameA));
                popup.add(new JMenuItem(deleteA));
            }
        };
        snapshotsTable.providePopupMenu(true);
        snapshotsTable.setMainColumn(1);
        snapshotsTable.setDefaultSortOrder(SortOrder.ASCENDING);
        snapshotsTable.setSecondarySortColumn(1);
        snapshotsTable.setSortColumn(0);
        snapshotsTable.setFitWidthColumn(1);
        snapshotsTable.setDefaultColumnWidth(0, new JLabel(Bundle.SnapshotsWindowUI_colType()).getPreferredSize().width + 30);      
        snapshotsTable.setColumnRenderer(0, new LabelRenderer() {
            {
                setHorizontalAlignment(CENTER);
            }
            public void setValue(Object value, int row) {
                setIcon(Icons.getIcon(Snapshot.getIconName((Integer)value)));
            }
        });
        snapshotsTable.setColumnRenderer(1, new LabelRenderer() {
            private final Font plain;
            private final Font bold;
            {
                plain = getFont().deriveFont(Font.PLAIN);
                bold = plain.deriveFont(Font.BOLD);
            }
            public void setValue(Object value, int row) {
                Snapshot s = (Snapshot)value;
                setText(s.getDisplayName());
                if (isOpen(s)) setFont(bold); else setFont(plain);
            }
        });
        snapshotsTable.setColumnToolTips(new String[] { Bundle.SnapshotsWindowUI_ttpSnapshotType(),
                                                        Bundle.SnapshotsWindowUI_ttpSnapshotName() });
        snapshotsTable.setDefaultAction(openA);
        snapshotsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        snapshotsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtons(snapshotsTable.getSelectedValues(1));
            }
        });
        
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 10, 10, 10);
        contents.add(new ProfilerTableContainer(snapshotsTable, true, null), c);
        
        setLayout(new BorderLayout());
        add(contents, BorderLayout.CENTER);
        
        class ThinButton extends JButton {
            public ThinButton(Action action, Icon icon) {
                super(action);
                setText(null);
                setIcon(icon);
                setToolTipText(action.getValue("BTN_TOOLTIP").toString()); // NOI18N
                setOpaque(false);
            }
            public Dimension getMinimumSize() {
                Dimension d = super.getMinimumSize();
                d.width = 5;
                return d;
            }
        }
        
        JPanel actions = new JPanel(new ButtonsLayout());
        actions.setOpaque(true);
        actions.setBackground(UIUtils.getProfilerResultsBackground());
        actions.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        actions.add(new ThinButton(openA, Icons.getIcon(ProfilerIcons.SNAPSHOT_OPEN)));
        actions.add(new ThinButton(exportA, Icons.getIcon(GeneralIcons.EXPORT)));
        actions.add(new ThinButton(compareA, Icons.getIcon(ProfilerIcons.SNAPSHOTS_COMPARE)));
        actions.add(new ThinButton(renameA, Icons.getIcon(GeneralIcons.RENAME)));
        actions.add(new ThinButton(deleteA, Icons.getIcon(ProfilerIcons.RUN_GC)));
        add(actions, BorderLayout.SOUTH);
        
        updateButtons(Collections.EMPTY_LIST);
    }
    
    private void updateButtons(List<Snapshot> selectedSnapshots) {
        int selected = selectedSnapshots.size();
        openA.setEnabled(selected > 0);
        exportA.setEnabled(selected > 0);
        compareA.setEnabled(selected == 2 && !selectedSnapshots.get(0).isHeapDump()
                                          && !selectedSnapshots.get(1).isHeapDump());
        renameA.setEnabled(selected == 1);
        deleteA.setEnabled(selected > 0);
        
    }
    
    private static boolean hasSnapshots(ProfilerSession session) {
//        ResultsManager rm = ResultsManager.getDefault();
//        Lookup.Provider project = session.getProject();
//        return rm.hasSnapshotsFor(project);
        
        return true;
    }
    
    private static boolean isOpen(Snapshot s) {
        File f = FileUtil.toFile(s.getFile());
        if (f == null) return false; // #236480

        if (s.isHeapDump()) {
            Set<TopComponent> tcs = WindowManager.getDefault().getRegistry().getOpened();
            for (TopComponent tc : tcs) {
                if (f.equals(tc.getClientProperty(ProfilerTopComponent.RECENT_FILE_KEY)))
                    return true;
            }
        } else {
            LoadedSnapshot ls = ResultsManager.getDefault().findLoadedSnapshot(f);
            if (ls != null) return true;
        }
        return false;
    }
    
    
    private static void openSnapshots(final Collection<Snapshot> snapshots) {
        SnapshotsWindowHelper.PROCESSOR.post(new Runnable() {
            public void run() {
                for (Snapshot snapshot : snapshots)
                    ResultsManager.getDefault().openSnapshot(snapshot.getFile());
            }
        });
    }
    
    private static void exportSnapshots(final Collection<Snapshot> snapshots) {
        FileObject[] files = new FileObject[snapshots.size()];
        int idx = 0;
        for (Snapshot snapshot : snapshots) files[idx++] = snapshot.getFile();
        ResultsManager.getDefault().exportSnapshots(files);
    }
    
    private static void compareSnapshots(final Snapshot snapshot1, final Snapshot snapshot2) {
        SnapshotsWindowHelper.PROCESSOR.post(new Runnable() {
            public void run() {
                final FileObject file1 = snapshot1.getFile();
                final FileObject file2 = snapshot2.getFile();
                if (CompareSnapshotsAction.areComparableSnapshots(file1, file2)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { ResultsManager.getDefault().compareSnapshots(file1, file2); }
                    });
                } else {
                    ProfilerDialogs.displayError(Bundle.SnapshotsWindowUI_msgCannotCompareSnapshots());
                }   
            }
        });
    }
    
    private static void renameSnapshot(final Snapshot snapshot, final AbstractTableModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final FileObject file = snapshot.getFile();
                String origName = file.getName();
                RenameSnapshotPanel panel = new RenameSnapshotPanel();
                panel.setSnapshotName(origName);
                DialogDescriptor dd = new DialogDescriptor(panel, Bundle.SnapshotsWindowUI_capRenameSnapshot(),
                            true, new Object[] { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION },
                            DialogDescriptor.OK_OPTION, 0, null, null);
                Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                d.setVisible(true);
                
                if (dd.getValue() != DialogDescriptor.OK_OPTION) return;
                
                final String newName = panel.getSnapshotName();
                if (!origName.equals(newName)) {
                    if (newName.length() == 0) {
                        ProfilerDialogs.displayError(Bundle.SnapshotsWindowUI_msgNameEmpty());
                        renameSnapshot(snapshot, model);
                    } else {
                        SnapshotsWindowHelper.PROCESSOR.post(new Runnable() {
                            public void run() {
                                FileLock lock = null;
                                try {
                                    lock = file.lock();
                                    final LoadedSnapshot ls = ResultsManager.getDefault().findLoadedSnapshot(
                                            FileUtil.toFile(file));
                                    file.rename(lock, newName, file.getExt());
                                    if (ls != null) ls.setFile(FileUtil.toFile(file));
                                    snapshot.loadDetails();
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() { model.fireTableDataChanged(); }
                                    });
                                } catch (IOException e) {
                                    ProfilerDialogs.displayError(Bundle.SnapshotsWindowUI_msgRenameFailed(snapshot.getDisplayName()));
                                    e.printStackTrace();
                                    renameSnapshot(snapshot, model);
                                } finally {
                                    if (lock != null) lock.releaseLock();
                                }
                            }
                        });
                    }
                }
            }
        });
    }
    
    private void deleteSnapshots(final Collection<Snapshot> snapshots) {
        SnapshotsWindowHelper.PROCESSOR.post(new Runnable() {
            public void run() {
                if (ProfilerDialogs.displayConfirmation(Bundle.SnapshotsWindowUI_msgDeleteSnapshots(), Bundle.SnapshotsWindowUI_capDeleteSnapshots())) {
                    ResultsManager rm = null;
                    for (Snapshot snapshot : snapshots) try {
                        if (!snapshot.isHeapDump()) {
                            if (rm == null) rm = ResultsManager.getDefault();
                            LoadedSnapshot ls = rm.findLoadedSnapshot(FileUtil.toFile(snapshot.getFile()));
                            if (ls != null) rm.closeSnapshot(ls);
                        }
                        DataObject.find(snapshot.getFile()).delete();
                    } catch (Throwable t) {
                        ProfilerDialogs.displayError(Bundle.SnapshotsWindowUI_msgDeleteFailed(snapshot.getDisplayName()));
                        t.printStackTrace();
                    }
                    refreshSnapshots();
                }
            }
        });
    }
    
    
    // --- TopComponent --------------------------------------------------------
    
    public void open() {
        WindowManager wmanager = WindowManager.getDefault();
        if (wmanager.findMode(this) == null) { // needs docking
            Mode _mode = wmanager.findMode(Bundle.SnapshotsWindowUI_mode());
            if (_mode != null) _mode.dockInto(this);
        }
        super.open();
    }
    
    protected void componentOpened() {
        super.componentOpened();
        
        refreshSnapshots();
        
        openProjectsListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) { selector.resetModel(); }
        };
        ProjectUtilities.addOpenProjectsListener(openProjectsListener);
    }
    
    protected void componentClosed() {
        ProjectUtilities.removeOpenProjectsListener(openProjectsListener);
        
        super.componentClosed();
    }
    
    public int getPersistenceType() {
        return PERSISTENCE_ALWAYS;
    }
    
    protected String preferredID() {
        return ID;
    }
    
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }
    
    
    // --- ProfilerTopComponent ------------------------------------------------
    
    protected Component defaultFocusOwner() {
        return snapshotsTable;
    }
    
    
    // --- Snapshot wrapper  ---------------------------------------------------
    
    private static abstract class Snapshot implements Comparable {
        
        private final FileObject fo;
        private String displayName;
        private boolean customName;
        private long timestamp;
        private int snapshotType;
        private boolean isHeapDump;

        Snapshot(FileObject fo) {
            this.fo = fo;
            loadDetails();
        }
        
        abstract boolean alternativeSorting();
        
        public String getDisplayName() {
            return displayName;
        }
        
        // Snapshot types (internal):
        // 1: CPU snapshot
        // 2: Memory snapshot
        // 3: Thread dump
        // 4: Heap dump
        
        public int getSnapshotType() {
            return snapshotType;
        }

        public static String getIconName(int type) {
            switch (type) {
                case 1: return ProfilerIcons.CPU;
                case 2: return ProfilerIcons.MEMORY;
                case 3: return ProfilerIcons.SNAPSHOT_THREADS;
                case 4: return ProfilerIcons.HEAP_DUMP;
                default: return null;
            }
        }

        public FileObject getFile() {
            return fo;
        }

        public boolean isHeapDump() {
            return isHeapDump;
        }

        void loadDetails() {
            String fileName = fo.getName();
            if (fo.getExt().equalsIgnoreCase(ResultsManager.HEAPDUMP_EXTENSION)) {
                // Heap Dump
                snapshotType = 4;
                displayName = ResultsManager.getDefault().getHeapDumpDisplayName(fileName);
                isHeapDump = true;
//            } else if (fo.getExt().equalsIgnoreCase(ResultsManager.THREADDUMP_EXTENSION)) {
//                // Thread Dump
////                this.icon = Icons.getIcon(ProfilerIcons.HEAP_DUMP);
////                this.icon =  ProfilerIcons.SNAPSHOT_THREADS;
//                this.snapshotType = 4;
//                this.displayName = ResultsManager.getDefault().getHeapDumpDisplayName(fo.getName());
//                this.isHeapDump = false;
            } else {
                int type = ResultsManager.getDefault().getSnapshotType(fo);
                snapshotType = type == LoadedSnapshot.SNAPSHOT_TYPE_CPU ? 1 : 2;
                displayName = ResultsManager.getDefault().getSnapshotDisplayName(fileName, type);
                isHeapDump = false;
            }
            customName = fileName.equals(displayName);
            if (!customName) {
                String _timestamp = fileName.substring(fileName.lastIndexOf("-")); // NOI18N
                try { timestamp = Long.parseLong(_timestamp); } catch (NumberFormatException e) {}
            }
        }

        public boolean equals(Object o) {
            return fo.equals(((Snapshot)o).fo);
        }

        public int hashCode() {
            return fo.hashCode();
        }

        public int compareTo(Object o) {
            Snapshot s = (Snapshot)o;
            // Alternative sorting: when sorting by snapshot type, the secondary
            // sorting sorts custom-named snapshots alphabetically and default-named
            // snapshots by timestamp, newest first. Custom-named snapshots display
            // above the default-named snapshots.
            if (alternativeSorting()) {
                if (customName) {
                    if (!s.customName) return -1;
                    else return Collator.getInstance().compare(getDisplayName(), s.getDisplayName());
                } else {
                    if (s.customName) return 1;
                    else return Long.compare(timestamp, s.timestamp);
                }
            } else {
                return Collator.getInstance().compare(getDisplayName(), s.getDisplayName());
            }
        }
    }
    
    
    // --- Rename panel --------------------------------------------------------
    
    private static final class RenameSnapshotPanel extends JPanel {
        //~ Instance fields ----------------------------------------------------------------------------------------------------

        private JTextField textField;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        RenameSnapshotPanel() {
            initComponents();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        String getSnapshotName() {
            return textField.getText().trim();
        }

        void setSnapshotName(final String text) {
            textField.setText(text);
            textField.selectAll();
        }

        private void initComponents() {
            GridBagConstraints gridBagConstraints;
            
            JLabel textLabel = new JLabel();
            Mnemonics.setLocalizedText(textLabel, Bundle.SnapshotsWindowUI_lblNewFile());
            textLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

            textField = new JTextField();
            textLabel.setLabelFor(textField);
            textField.setPreferredSize(new Dimension(350, textField.getPreferredSize().height));
            textField.requestFocus();            
            textField.setAlignmentX(JLabel.LEFT_ALIGNMENT);

            setLayout(new GridBagLayout());
            
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(15, 10, 5, 10);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            add(textLabel, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 15, 10);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            add(textField, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;            
            add(new JPanel(), gridBagConstraints);
            
            getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(
                    NotifyDescriptor.class, "ACSD_InputPanel")); // NOI18N
            textField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(
                    NotifyDescriptor.class, "ACSD_InputField")); // NOI18N
                    
        }
    };
    
    
    // --- Buttons Layout ------------------------------------------------------
    
    private static final class ButtonsLayout implements LayoutManager {
        
        private static final int HGAP = 2;
        private static final float MAX_WIDTH_FACTOR = 1.8f;
        
        public void layoutContainer(Container parent) {
            int c = parent.getComponentCount();
            
            Insets insets = parent.getInsets();
            Dimension size = parent.getSize();
            size.width = Math.min(size.width, maximumLayoutSize(parent).width);
            
            int x = insets.left;
            int y = insets.top;
            int w = size.width - x - insets.right - HGAP * (c - 1);
            int h = size.height - y - insets.bottom;
            
            int m = w % c;
            w /= c;

            for (int i = 0; i < c; i++) {
                int o = i < m ? 1 : 0;
                parent.getComponent(i).setBounds(x, y, w + o, h);
                x += w + o + HGAP;
            }
        }

        public Dimension preferredLayoutSize(Container parent) {
            int prefw = 0;
            int prefh = 0;
            for (Component c : parent.getComponents()) {
                Dimension pref = c.getPreferredSize();
                prefw += pref.width;
                prefh = Math.max(prefh, pref.height);
            }
            prefw += HGAP * (parent.getComponentCount() - 1);
            
            Insets i = parent.getInsets();
            prefw += i.left + i.right;
            prefh += i.top + i.bottom;
            
            return new Dimension(prefw, prefh);
        }

        public Dimension minimumLayoutSize(Container parent) {
            int minw = 0;
            int minh = 0;
            for (Component c : parent.getComponents()) {
                Dimension min = c.getMinimumSize();
                minw += min.width;
                minh = Math.max(minh, min.height);
            }
            minw += HGAP * (parent.getComponentCount() - 1);
            return new Dimension(minw, minh);
        }
        
        private Dimension maximumLayoutSize(Container parent) {
            int maxw = 0;
            int maxh = 0;
            for (Component c : parent.getComponents()) {
                Dimension pref = c.getPreferredSize();
                maxw += pref.height * MAX_WIDTH_FACTOR;
                maxh = Math.max(maxh, pref.height);
            }
            maxw += HGAP * (parent.getComponentCount() - 1);
            
            Insets i = parent.getInsets();
            maxw += i.left + i.right;
            maxh += i.top + i.bottom;
            
            return new Dimension(maxw, maxh);
        }
        
        public void addLayoutComponent(String name, Component comp) {}

        public void removeLayoutComponent(Component comp) {}
        
    }
    
}
