/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.visualvm.sampler.memory;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.HeapHistogram;
import com.sun.tools.visualvm.application.jvm.HeapHistogram.ClassInfo;
import com.sun.tools.visualvm.profiling.actions.ProfiledSourceSelection;
import com.sun.tools.visualvm.profiling.actions.ProfilerPopupCustomizer;
import com.sun.tools.visualvm.sampler.AbstractSamplerSupport;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.FilterUtils;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class MemoryView extends JPanel {

    static final int MODE_HEAP = 1;
    static final int MODE_PERMGEN = 2;
    
    private final int mode;

    private final AbstractSamplerSupport.Refresher refresher;
    private boolean forceRefresh = false;
    
    private final MemoryMXBean memoryBean;
    private final MemorySamplerSupport.HeapDumper heapDumper;
    private final MemorySamplerSupport.SnapshotDumper snapshotDumper;
    
    private List<ClassInfo> classes = new ArrayList();
    private List<ClassInfo> baseClasses = new ArrayList(); // Needed to correctly setup table renderers
//    private int totalClasses = -1;
    private long totalBytes, baseTotalBytes = -1;
    private long totalInstances, baseTotalInstances = -1;


    MemoryView(Application application, AbstractSamplerSupport.Refresher refresher, int mode,
               MemoryMXBean memoryBean, MemorySamplerSupport.SnapshotDumper snapshotDumper,
               MemorySamplerSupport.HeapDumper heapDumper) {

        this.refresher = refresher;
        this.mode = mode;

        this.memoryBean = memoryBean;
        this.snapshotDumper = snapshotDumper;
        this.heapDumper = heapDumper;
        
        initComponents(application);

        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) MemoryView.this.refresher.refresh();
                }
            }
        });
    }


    void initSession() {
        if (pdSnapshotButton != null) pdSnapshotButton.setEnabled(false);
    }

    boolean isPaused() {
        return lrPauseButton.isSelected() && !forceRefresh;
    }
    
    boolean isEmpty() {
        return tableModel.getRowCount() == 0;
    }
    
    void refresh(HeapHistogram histogram) {
        if (histogram == null || isPaused()) return;
        forceRefresh = false;
        
        boolean diff = lrDeltasButton.isSelected();
        if (diff) {
            if (baseClasses == null) {
                baseClasses = new ArrayList(classes);
                baseTotalBytes = totalBytes;
                baseTotalInstances = totalInstances;
            }

            Collection<ClassInfo> newClasses = getHistogram(histogram);
            classes = computeDeltaClasses(baseClasses, newClasses);

//            totalClasses = baseClasses.size() - newClasses.size();
            totalBytes = getTotalBytes(histogram) - baseTotalBytes;
            totalInstances = getTotalInstances(histogram) - baseTotalInstances;

            long maxAbsDiffBytes = 0;
            for (ClassInfo cInfo : classes)
                maxAbsDiffBytes = Math.max(maxAbsDiffBytes, Math.abs(cInfo.getBytes()));
            
        } else {
            if (baseClasses != null) {
                baseClasses = null;
                baseTotalBytes = -1;
                baseTotalInstances = -1;
            }
            classes.clear();
            classes.addAll(getHistogram(histogram));

//            totalClasses = classes.size();
            totalBytes = getTotalBytes(histogram);
            totalInstances = getTotalInstances(histogram);
        }
        
        renderers[0].setDiffMode(diff);
        renderers[0].setMaxValue(totalBytes);
        
        renderers[1].setDiffMode(diff);
        renderers[1].setMaxValue(totalInstances);

        tableModel.fireTableDataChanged();

        if (pdSnapshotButton != null) pdSnapshotButton.setEnabled(true);
    }

    void starting() {
        lrPauseButton.setEnabled(true);
        lrRefreshButton.setEnabled(false);
        lrDeltasButton.setEnabled(true);
    }
    
    void stopping() {
        lrPauseButton.setEnabled(false);
        lrRefreshButton.setEnabled(false);
        lrDeltasButton.setEnabled(false);
    }
    
    void terminated() {
        lrPauseButton.setEnabled(false);
        lrRefreshButton.setEnabled(false);
        lrDeltasButton.setEnabled(false);
        gcButton.setEnabled(false);
        heapdumpButton.setEnabled(false);
    }

    private Collection getHistogram(HeapHistogram histogram) {
        if (mode == MODE_HEAP) return histogram.getHeapHistogram();
        if (mode == MODE_PERMGEN) return histogram.getPermGenHistogram();
        return null;
    }

    private long getTotalBytes(HeapHistogram histogram) {
        if (mode == MODE_HEAP) return histogram.getTotalHeapBytes();
        if (mode == MODE_PERMGEN) return histogram.getTotalPermGenHeapBytes();
        return -1;
    }

    private long getTotalInstances(HeapHistogram histogram) {
        if (mode == MODE_HEAP) return histogram.getTotalHeapInstances();
        if (mode == MODE_PERMGEN) return histogram.getTotalPerGenInstances();
        return -1;
    }

    private static List<ClassInfo> computeDeltaClasses(Collection<ClassInfo> basis, Collection<ClassInfo> changed) {

        Map<String, DeltaClassInfo> deltaMap = new HashMap((int)(basis.size() * 1.3));

        for (ClassInfo cInfo : basis)
            deltaMap.put(cInfo.getName(), new DeltaClassInfo(cInfo, true));

        for (ClassInfo cInfo : changed) {
            DeltaClassInfo bInfo = deltaMap.get(cInfo.getName());
            if (bInfo != null) bInfo.add(cInfo);
            else deltaMap.put(cInfo.getName(), new DeltaClassInfo(cInfo, false));
        }

        return new ArrayList(deltaMap.values());
    }
    
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private JToggleButton lrDeltasButton;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
//    private JButton pdResetResultsButton;
    
    private AbstractButton gcButton;
    private AbstractButton heapdumpButton;
    
    private boolean popupPause;
    
    private JComponent bottomPanel;
    private JComponent filterPanel;
    private JComponent searchPanel;
    
//    private NumberRenderer threadsCount;
//    private NumberRenderer threadsTotalBytes;
    
    private HistogramTableModel tableModel;
    private ProfilerTable table;
    
    private HideableBarRenderer[] renderers;
    
    private void initComponents(final Application application) {
        tableModel = new HistogramTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
//            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
//                return ThreadsMemoryView.this.getUserValueForRow(row);
//            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                String selectedClass = value == null ? null : value.toString();
                if (snapshotDumper != null && selectedClass != null) {
                    JMenuItem[] customItems = createCustomMenuItems(application, selectedClass);
                    if (customItems != null) {
                        for (JMenuItem customItem : customItems) popup.add(customItem);
                        popup.addSeparator();
                    }
                }
                
                popup.add(createCopyMenuItem());
                popup.addSeparator();
                
                popup.add(new JMenuItem(FilterUtils.ACTION_FILTER) {
                    protected void fireActionPerformed(ActionEvent e) { MemoryView.this.activateFilter(); }
                });
                popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
                    protected void fireActionPerformed(ActionEvent e) { MemoryView.this.activateSearch(); }
                });
            }
            protected void popupShowing() {
                if (lrPauseButton.isEnabled() && !lrRefreshButton.isEnabled()) {
                    popupPause = true;
                    lrPauseButton.setSelected(true);
                }
            }
            protected void popupHidden() {
                if (lrPauseButton.isEnabled() && popupPause) {
                    popupPause = false;
                    lrPauseButton.setSelected(false);
                }
            }
        };
        
        table.setColumnToolTips(new String[] { NbBundle.getMessage(MemoryView.class, "MemoryView_TOOLTIP_Col_name"), // NOI18N
                                               NbBundle.getMessage(MemoryView.class, "MemoryView_TOOLTIP_Col_size"), // NOI18N
                                               NbBundle.getMessage(MemoryView.class, "MemoryView_TOOLTIP_Col_count") // NOI18N
                                });
        
        table.providePopupMenu(true);
        
        table.setMainColumn(0);
        table.setFitWidthColumn(0);
        
        table.setSortColumn(1);
        table.setDefaultSortOrder(1, SortOrder.DESCENDING);
        
        renderers = new HideableBarRenderer[2];
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        
        renderers[0].setMaxValue(123456789);
        renderers[1].setMaxValue(12345678);
        
        table.setColumnRenderer(0, new JavaNameRenderer(Icons.getIcon(LanguageIcons.CLASS)));
        table.setColumnRenderer(1, renderers[0]);
        table.setColumnRenderer(2, renderers[1]);
        
        table.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
        table.setDefaultColumnWidth(2, renderers[1].getMaxNoBarWidth());
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        
        // --- Toolbar ---------------------------------------------------------
        
        lrLabel = new GrayLabel(NbBundle.getMessage(MemoryView.class, "MemoryView_LBL_Results")); // NOI18N
            
        lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
            protected void fireItemStateChanged(ItemEvent event) {
                boolean paused = isSelected();
                lrRefreshButton.setEnabled(paused && !popupPause);
                if (!paused) refresher.refresh();
            }
        };
        lrPauseButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Pause_results")); // NOI18N

        lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
            protected void fireActionPerformed(ActionEvent e) {
                forceRefresh = true;
                refresher.refresh();
            }
        };
        lrRefreshButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Update_results")); // NOI18N
        lrRefreshButton.setEnabled(false);
        
        Icon icon = Icons.getIcon(ProfilerIcons.DELTA_RESULTS);
        lrDeltasButton = new JToggleButton(icon) {
            protected void fireActionPerformed(ActionEvent e) {
                if (!lrPauseButton.isSelected()) {
                    forceRefresh = true;
                    refresher.refresh();
                }
            }
        };
        lrDeltasButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Deltas")); // NOI18N
        
        if (snapshotDumper != null) {
            pdLabel = new GrayLabel(NbBundle.getMessage(MemoryView.class, "MemoryView_LBL_Data")); // NOI18N
            
            pdSnapshotButton = new JButton(NbBundle.getMessage(MemoryView.class,
                        "LBL_Snapshot"), new ImageIcon(ImageUtilities.loadImage( // NOI18N
                        "com/sun/tools/visualvm/sampler/resources/snapshot.png", true))) { // NOI18N
                protected void fireActionPerformed(ActionEvent event) {
                    snapshotDumper.takeSnapshot((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
                }
            };
            pdSnapshotButton.setText(NbBundle.getMessage(MemoryView.class, "MemoryView_LBL_Snapshot")); // NOI18N
        }
        
        ProfilerToolbar toolbar = ProfilerToolbar.create(true);

//        toolbar.addSpace(2);
//        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(lrLabel);
        toolbar.addSpace(2);
        toolbar.add(lrPauseButton);
        toolbar.add(lrRefreshButton);
        
        toolbar.addSpace(5);
        toolbar.add(lrDeltasButton);
        
        if (pdSnapshotButton != null) {
        
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);

            toolbar.add(pdLabel);
            toolbar.addSpace(2);
            toolbar.add(pdSnapshotButton);
        
        }
        
        toolbar.addFiller();
        
        gcButton = new JButton(NbBundle.getMessage(MemoryView.class, "LBL_Gc")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                new RequestProcessor("GC Processor").post(new Runnable() { // NOI18N
                    public void run() {
                        try { memoryBean.gc(); } catch (Exception e) {
                            setEnabled(false);
                            Exceptions.printStackTrace(e);
                        }
                    };
                });
            }
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width += 5;
                return dim;
            }
        };
        gcButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Gc")); // NOI18N
        gcButton.setOpaque(false);
        gcButton.setEnabled(heapDumper != null);
        toolbar.add(gcButton);
        
        heapdumpButton = new JButton(NbBundle.getMessage(MemoryView.class, "LBL_HeapDump")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                heapDumper.takeHeapDump((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width += 5;
                return dim;
            }
        };
        heapdumpButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_HeapDump")); // NOI18N
        heapdumpButton.setOpaque(false);
        heapdumpButton.setEnabled(heapDumper != null);
        toolbar.add(heapdumpButton);
        
        
        setLayout(new BorderLayout());
        add(toolbar.getComponent(), BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);
    }
    
    private JComponent getBottomPanel() {
        if (bottomPanel == null) {
            bottomPanel = new JPanel(new FilterFindLayout());
            bottomPanel.setOpaque(true);
            bottomPanel.setBackground(UIManager.getColor("controlShadow")); // NOI18N
            add(bottomPanel, BorderLayout.SOUTH);
        }
        return bottomPanel;
    }
    
    private void activateFilter() {
        JComponent panel = getBottomPanel();
        
        if (filterPanel == null) {
            filterPanel = FilterUtils.createFilterPanel(table, null);
            panel.add(filterPanel);
            Container parent = panel.getParent();
            parent.invalidate();
            parent.revalidate();
            parent.repaint();
        }
        
        panel.setVisible(true);
        
        filterPanel.setVisible(true);
        filterPanel.requestFocusInWindow();
    }
    
    private void activateSearch() {
        JComponent panel = getBottomPanel();
        
        if (searchPanel == null) {
            searchPanel = SearchUtils.createSearchPanel(table);
            panel.add(searchPanel);
            Container parent = panel.getParent();
            parent.invalidate();
            parent.revalidate();
            parent.repaint();
        }
        
        panel.setVisible(true);
        
        searchPanel.setVisible(true);
        searchPanel.requestFocusInWindow();
    }
    
    private JMenuItem[] createCustomMenuItems(Application application, String className) {
        Collection<? extends ProfilerPopupCustomizer> customizers = Lookup.getDefault().lookupAll(ProfilerPopupCustomizer.class);
        if (customizers.isEmpty()) return null;
        
        ProfiledSourceSelection pss = new ProfiledSourceSelection(application, className, Wildcards.ALLWILDCARD, null);
        
        List<JMenuItem> menuItems = new ArrayList(customizers.size());
        
        for (ProfilerPopupCustomizer customizer : customizers) {
            if (customizer.supportsDataView(ProfilerPopupCustomizer.View.MEMORY, ProfilerPopupCustomizer.Mode.LIVE)) {
                JMenuItem[] items = customizer.getMenuItems(pss, ProfilerPopupCustomizer.View.MEMORY, ProfilerPopupCustomizer.Mode.LIVE);
                if (items != null) Collections.addAll(menuItems, items);
            }
        }
        
        return menuItems.isEmpty() ? null : menuItems.toArray(new JMenuItem[0]);
    }
    
    
    private static final String COL_NAME = NbBundle.getMessage(MemoryView.class, "COL_Class_name"); // NOI18N
    private static final String COL_BYTES = NbBundle.getMessage(MemoryView.class, "COL_Bytes"); // NOI18N
    private static final String COL_INSTANCES = NbBundle.getMessage(MemoryView.class, "COL_Instances"); // NOI18N
    
    private class HistogramTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return COL_NAME;
            } else if (columnIndex == 1) {
                return COL_BYTES;
            } else if (columnIndex == 2) {
                return COL_INSTANCES;
            }
            
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return String.class;
            } else {
                return Long.class;
            }
        }

        public int getRowCount() {
            return classes.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return classes.get(rowIndex).getName();
            } else if (columnIndex == 1) {
                return classes.get(rowIndex).getBytes();
            } else if (columnIndex == 2) {
                return classes.get(rowIndex).getInstancesCount();
            }

            return null;
        }
        
    }
    
    
    private final class FilterFindLayout implements LayoutManager {

        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            JComponent filter = filterPanel;
            if (filter != null && !filter.isVisible()) filter = null;
            
            JComponent search = searchPanel;
            if (search != null && !search.isVisible()) search = null;
            
            Dimension dim = new Dimension();
            
            if (filter != null && search != null) {
                Dimension dim1 = filter.getPreferredSize();
                Dimension dim2 = search.getPreferredSize();
                dim.width = dim1.width + dim2.width + 1;
                dim.height = Math.max(dim1.height, dim2.height);
            } else if (filter != null) {
                dim = filter.getPreferredSize();
            } else if (search != null) {
                dim = search.getPreferredSize();
            }
            
            if ((filter != null || search != null) /*&& hasBottomFilterFindMargin()*/)
                dim.height += 1;
            
            return dim;
        }

        public Dimension minimumLayoutSize(Container parent) {
            JComponent filter = filterPanel;
            if (filter != null && !filter.isVisible()) filter = null;
            
            JComponent search = searchPanel;
            if (search != null && !search.isVisible()) search = null;
            
            Dimension dim = new Dimension();
            
            if (filter != null && search != null) {
                Dimension dim1 = filter.getMinimumSize();
                Dimension dim2 = search.getMinimumSize();
                dim.width = dim1.width + dim2.width + 1;
                dim.height = Math.max(dim1.height, dim2.height);
            } else if (filter != null) {
                dim = filter.getMinimumSize();
            } else if (search != null) {
                dim = search.getMinimumSize();
            }
            
            if ((filter != null || search != null) /*&& hasBottomFilterFindMargin()*/)
                dim.height += 1;
            
            return dim;
        }

        public void layoutContainer(Container parent) {
            JComponent filter = filterPanel;
            if (filter != null && !filter.isVisible()) filter = null;
            
            JComponent search = searchPanel;
            if (search != null && !search.isVisible()) search = null;
            
            int bottomOffset = /* hasBottomFilterFindMargin() ? 1 :*/ 0;
            
            if (filter != null && search != null) {
                Dimension size = parent.getSize();
                int w = (size.width - 1) / 2;
                filter.setBounds(0, 0, w, size.height - bottomOffset);
                search.setBounds(w + 1, 0, size.width - w - 1, size.height - bottomOffset);
            } else if (filter != null) {
                Dimension size = parent.getSize();
                filter.setBounds(0, 0, size.width, size.height - bottomOffset);
            } else if (search != null) {
                Dimension size = parent.getSize();
                search.setBounds(0, 0, size.width, size.height - bottomOffset);
            }
        }
        
    }
    

    private static class DeltaClassInfo extends ClassInfo {

        String name;
        long instancesCount;
        long bytes;

        DeltaClassInfo(ClassInfo cInfo, boolean negative) {
            name = cInfo.getName();
            instancesCount = negative ? -cInfo.getInstancesCount() : cInfo.getInstancesCount();
            bytes = negative ? -cInfo.getBytes() : cInfo.getBytes();
        }

        void add(ClassInfo cInfo) {
            instancesCount += cInfo.getInstancesCount();
            bytes += cInfo.getBytes();
        }

        public String getName() { return name; }
        public long getInstancesCount() { return instancesCount; }
        public long getBytes() { return bytes; }

    }

}
