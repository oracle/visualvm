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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.api.project.ProjectStorage;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.ui.LazyComboBox;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
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
    "SnapshotsWindowUI_mode=properties"
})
public final class SnapshotsWindowUI extends TopComponent {
    
    public static final String ID = "SnapshotsWindowUI"; // NOI18N
    
    private static final Lookup.Provider EXTERNAL_PROCESS = new Lookup.Provider() {
        public Lookup getLookup() { return Lookup.EMPTY; }
    };
    
    
    // --- Instance ------------------------------------------------------------
    
    public SnapshotsWindowUI() {
        setName(Bundle.SnapshotsWindowUI_name());
        setIcon(Icons.getImage(ProfilerIcons.SNAPSHOT_TAKE));
        
        putClientProperty(ID, ID);
        
        initUI();
    }
    
    
    // --- Internal API --------------------------------------------------------
    
    public void refreshFolder(FileObject folder, boolean fullRefresh) {
        if (Objects.equals(folder, currentFolder)) {
            if (fullRefresh) refreshSnapshots();
            else repaintSnapshots();
        }
    }
    
    public void setProject(Lookup.Provider project) {
        selector.setSelectedItem(project == null ? EXTERNAL_PROCESS : project);
    }
    
    public void resetProject(Lookup.Provider project) {
        if (selector.getSelectedItem() == project) selector.resetModel();
    }
    
    
    // --- Implementation ------------------------------------------------------
    
    private LazyComboBox<Lookup.Provider> selector;
    private ChangeListener openProjectsListener;
    
    private FileObject currentFolder;
    private final List<Snapshot> snapshots = new ArrayList();

    private final AbstractTableModel threadsTableModel = new AbstractTableModel() {
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Type";
            } else if (columnIndex == 1) {
                return "Name";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return String.class;
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
                return snapshots.get(rowIndex).getIcon();
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
        Lookup.Provider project = (Lookup.Provider)selector.getSelectedItem();
        final Lookup.Provider _project = project == EXTERNAL_PROCESS ? null : project;
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ResultsManager rm = ResultsManager.getDefault();
                final List<Snapshot> _snapshots = new ArrayList();
                for (FileObject fo : rm.listSavedSnapshots(_project, null))
                    _snapshots.add(new Snapshot(fo));
                for (FileObject fo : rm.listSavedHeapdumps(_project, null))
                    _snapshots.add(new Snapshot(fo));
                FileObject __currentFolder = null;
                try {
                    __currentFolder = ProjectStorage.getSettingsFolder(_project, false);
                } catch (IOException e) {}
                final FileObject _currentFolder = __currentFolder;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        currentFolder = _currentFolder;
                        snapshots.clear();
                        snapshots.addAll(_snapshots);
                        threadsTableModel.fireTableDataChanged();
                    }
                });
            }
        });
    }
    
    void repaintSnapshots() {
        threadsTableModel.fireTableDataChanged();
    }
    
    
    // --- UI ------------------------------------------------------------------
    
    private void initUI() {
        JPanel contents = new JPanel(new GridBagLayout());
        contents.setOpaque(true);
        contents.setBackground(UIUtils.getProfilerResultsBackground());
        
        GridBagConstraints c;
        int y = 0;
        
        JLabel projectSelectL = new JLabel("Project:", JLabel.LEADING);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 10, 0, 10);
        contents.add(projectSelectL, c);
        
        selector = new LazyComboBox<Lookup.Provider>(new LazyComboBox.Populator<Lookup.Provider>() {
            protected Lookup.Provider initial() {
                ProfilerSession ps = ProfilerSession.currentSession();
                Lookup.Provider lp = ps == null ? null : ps.getProject();
                return lp == null ? EXTERNAL_PROCESS : lp;
            }
            protected Lookup.Provider[] populate() {
                // Set of open & profiled projects
                Set<Lookup.Provider> s = new HashSet();
                // Add all open projects
                for (Lookup.Provider p : ProjectUtilities.getOpenedProjects())
                    s.add(p);
                // Add currently profiled project (can be closed)
                ProfilerSession ps = ProfilerSession.currentSession();
                Lookup.Provider cp = ps == null ? null : ps.getProject();
                if (cp != null) s.add(cp);
                
                List<Lookup.Provider> l = new ArrayList();
                Lookup.Provider[] pa = s.toArray(new Lookup.Provider[s.size()]);
                l.add(EXTERNAL_PROCESS);
                l.addAll(Arrays.asList(ProjectUtilities.getSortedProjects(pa)));
                return l.toArray(new Lookup.Provider[l.size()]);
            }
        }) {
            protected void selectionChanged() {
                refreshSnapshots();
            }
        };
        selector.setRenderer(new ProjectNameRenderer());
        
        c = new GridBagConstraints();
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 10, 0, 10);
        contents.add(selector, c);
        
        JLabel snapshotsListL = new JLabel("Snapshots:", JLabel.LEADING);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 10, 0, 10);
        contents.add(snapshotsListL, c);
        
        final ProfilerTable snapshotsTable = new ProfilerTable(threadsTableModel, true, true, null);
        snapshotsTable.setMainColumn(1);
        snapshotsTable.setFitWidthColumn(1);
        snapshotsTable.setDefaultColumnWidth(0, new JLabel("Type").getPreferredSize().width + 10);      
        snapshotsTable.setColumnRenderer(0, new LabelRenderer() {
            {
                setHorizontalAlignment(CENTER);
            }
            public void setValue(Object value, int row) {
                setIcon(Icons.getIcon(value.toString()));
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
        snapshotsTable.setDefaultAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Snapshot s = (Snapshot)snapshotsTable.getSelectedValue(1);
                final FileObject fo = s == null ? null : s.getFile();
                if (fo != null) RequestProcessor.getDefault().post(new Runnable() {
                    public void run() { ResultsManager.getDefault().openSnapshot(fo); }
                });
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
        
        refreshSnapshots();
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
    
    
    // --- Snapshot wrapper  ---------------------------------------------------
    
    private static final class Snapshot implements Comparable {
        private final FileObject fo;
        private String displayName;
        private String icon;
        private boolean isHeapDump;

        Snapshot(FileObject fo) {
            this.fo = fo;
            loadDetails();
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public FileObject getFile() {
            return fo;
        }

        public boolean isHeapDump() {
            return isHeapDump;
        }

        private void loadDetails() {
            if (fo.getExt().equalsIgnoreCase(ResultsManager.HEAPDUMP_EXTENSION)) {
                // Heap Dump
//                this.icon = Icons.getIcon(ProfilerIcons.HEAP_DUMP);
                this.icon =  ProfilerIcons.HEAP_DUMP;
                this.displayName = ResultsManager.getDefault().getHeapDumpDisplayName(fo.getName());
                this.isHeapDump = true;
            } else {
                int snapshotType = ResultsManager.getDefault().getSnapshotType(fo);
                this.displayName = ResultsManager.getDefault().getSnapshotDisplayName(fo.getName(), snapshotType);
                this.icon = getIcon(snapshotType);
                this.isHeapDump = false;
            }
        }

        private static String getIcon(int snapshotType) {
            switch (snapshotType) {
                case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
//                    return Icons.getIcon(ProfilerIcons.CPU);
                    return ProfilerIcons.CPU;
                case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
//                    return Icons.getIcon(ProfilerIcons.FRAGMENT);
                    return ProfilerIcons.FRAGMENT;
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
//                    return Icons.getIcon(ProfilerIcons.MEMORY);
                    return ProfilerIcons.MEMORY;
                default:
                    return null;
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
            return getDisplayName().compareTo(s.getDisplayName());
        }
    }
    
    
    // --- ProjectNameRenderer -------------------------------------------------
    
    private static final class ProjectNameRenderer extends DefaultListCellRenderer {

        private Renderer renderer = new Renderer();
        private boolean firstFontSet = false;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel rendererOrig = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            renderer.setComponentOrientation(rendererOrig.getComponentOrientation());
            renderer.setFontEx(rendererOrig.getFont());
            renderer.setOpaque(rendererOrig.isOpaque());
            renderer.setForeground(rendererOrig.getForeground());
            renderer.setBackground(rendererOrig.getBackground());
            renderer.setEnabled(rendererOrig.isEnabled());
            renderer.setBorder(rendererOrig.getBorder());

            if (value != EXTERNAL_PROCESS) {
                Lookup.Provider p = (Lookup.Provider) value;
                renderer.setText(ProjectUtilities.getDisplayName(p));
                renderer.setIcon(ProjectUtilities.getIcon(p));

                if (ProjectUtilities.getMainProject() == value) {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.BOLD)); // bold for main project
                } else {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.PLAIN));
                }
            } else {
                renderer.setText("External process");
                renderer.setIcon(Icons.getIcon(GeneralIcons.JAVA_PROCESS));
            }

            return renderer;
        }
        
        private static class Renderer extends DefaultListCellRenderer {
            public void setFont(Font font) {}
            public void setFontEx(Font font) { super.setFont(font); }
        }
        
    }
    
}
