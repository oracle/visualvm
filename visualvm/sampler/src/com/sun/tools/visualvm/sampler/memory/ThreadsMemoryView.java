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

import com.sun.tools.visualvm.sampler.AbstractSamplerSupport;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Icon;
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
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberRenderer;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ThreadsMemoryView extends JPanel {
    
    private final AbstractSamplerSupport.Refresher refresher;
    private boolean forceRefresh = false;
    
    private final MemoryMXBean memoryBean;
    private final MemorySamplerSupport.HeapDumper heapDumper;
    
    private List<ThreadInfo> threads;
    private List<Long> allocatedBytes;
    private List<Long> allocatedBytesPerSec;
    private ThreadsMemoryInfo currentThreadsInfo;
    private ThreadsMemoryInfo baseThreadsInfo;
    
    private int totalThreads = -1;
    private long totalBytes = -1;
    
    ThreadsMemoryView(AbstractSamplerSupport.Refresher refresher, MemoryMXBean memoryBean, MemorySamplerSupport.HeapDumper heapDumper) {    
        this.refresher = refresher;
        this.memoryBean = memoryBean;
        this.heapDumper = heapDumper;
        
        threads = Collections.EMPTY_LIST;
        allocatedBytes = Collections.EMPTY_LIST;
        allocatedBytesPerSec = Collections.EMPTY_LIST;
        
        initComponents();
        
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) ThreadsMemoryView.this.refresher.refresh();
                }
            }
        });
    }    
    
    void initSession() {
    }
    
    boolean isPaused() {
        return lrPauseButton.isSelected() && !forceRefresh;
    }
    
    boolean isEmpty() {
        return tableModel.getRowCount() == 0;
    }
    
    void refresh(ThreadsMemoryInfo info) {
        if (isPaused()) return;
        forceRefresh = false;
        threads = info.getThreads();
        allocatedBytes = info.getAllocatedBytes();
        totalBytes = info.getTotalBytes();
        if (currentThreadsInfo != null) {
            allocatedBytesPerSec = currentThreadsInfo.getAllocatedBytesPerSecond(info);
            renderers[1].setMaxValue(currentThreadsInfo.getTotalAllocatedBytesPerSecond());
        }
        currentThreadsInfo = info;
        
        boolean diff = lrDeltasButton.isSelected();
        if (diff) {
            if (baseThreadsInfo == null) baseThreadsInfo = info;
            totalThreads = threads.size() - baseThreadsInfo.getThreads().size();
            allocatedBytes = baseThreadsInfo.getAllocatedDiffBytes(info);
            totalBytes = baseThreadsInfo.getTotalDiffBytes();
        } else {
            if (baseThreadsInfo != null) baseThreadsInfo = null;
            allocatedBytes = info.getAllocatedBytes();
            totalBytes = info.getTotalBytes();
            totalThreads = threads.size();
        }
        
        renderers[0].setDiffMode(diff);
        renderers[0].setMaxValue(totalBytes);
        
        threadsCount.setDiffMode(diff);
        threadsCount.setValue(totalThreads, -1);
        
        threadsTotalBytes.setDiffMode(diff);
        threadsTotalBytes.setValue(totalBytes, -1);

        tableModel.fireTableDataChanged();
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
    
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private JToggleButton lrDeltasButton;
    private ProfilerToolbar toolbar;
    
    private AbstractButton gcButton;
    private AbstractButton heapdumpButton;
    
    private boolean popupPause;
    
    private JComponent bottomPanel;
    private JComponent filterPanel;
    private JComponent searchPanel;
    
    private NumberRenderer threadsCount;
    private NumberRenderer threadsTotalBytes;
    
    private TreadsAllocTableModel tableModel;
    private ProfilerTable table;
    
    private HideableBarRenderer[] renderers;
    
    private void initComponents() {
        tableModel = new TreadsAllocTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
//            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
//                return ThreadsMemoryView.this.getUserValueForRow(row);
//            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                popup.add(createCopyMenuItem());
                popup.addSeparator();
                
                popup.add(new JMenuItem(FilterUtils.ACTION_FILTER) {
                    protected void fireActionPerformed(ActionEvent e) { ThreadsMemoryView.this.activateFilter(); }
                });
                popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
                    protected void fireActionPerformed(ActionEvent e) { ThreadsMemoryView.this.activateSearch(); }
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
        
        table.setColumnToolTips(new String[] { NbBundle.getMessage(ThreadsMemoryView.class, "ThreadsMemoryView_TOOLTIP_Col_name"), // NOI18N
                                               NbBundle.getMessage(ThreadsMemoryView.class, "ThreadsMemoryView_TOOLTIP_Col_bytes"), // NOI18N
                                               NbBundle.getMessage(ThreadsMemoryView.class, "ThreadsMemoryView_TOOLTIP_Col_bytessec") // NOI18N
                                });
        
        table.providePopupMenu(true);
        
        table.setMainColumn(0);
        table.setFitWidthColumn(0);
        
        table.setSortColumn(1);
        table.setDefaultSortOrder(1, SortOrder.DESCENDING);
        
        renderers = new HideableBarRenderer[2];
        
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        
        LabelRenderer threadRenderer = new LabelRenderer();
        threadRenderer.setIcon(Icons.getIcon(ProfilerIcons.THREAD));
        threadRenderer.setFont(threadRenderer.getFont().deriveFont(Font.BOLD));
        
        table.setColumnRenderer(0, threadRenderer);
        table.setColumnRenderer(1, renderers[0]);
        table.setColumnRenderer(2, renderers[1]);
        
        long refTime = 12345678;
        renderers[0].setMaxValue(refTime);
        renderers[1].setMaxValue(refTime);
        table.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
        table.setDefaultColumnWidth(2, renderers[1].getOptimalWidth());
        
        renderers[1].setMaxValue(1000 * 1000);
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        
        // --- Toolbar ---------------------------------------------------------
        
        lrLabel = new GrayLabel(NbBundle.getMessage(ThreadsMemoryView.class, "ThreadsMemoryView_LBL_Results")); // NOI18N
            
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
        
        toolbar = ProfilerToolbar.create(true);

//        toolbar.addSpace(2);
//        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(lrLabel);
        toolbar.addSpace(2);
        toolbar.add(lrPauseButton);
        toolbar.add(lrRefreshButton);
        
        toolbar.addSpace(5);
        toolbar.add(lrDeltasButton);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel(NbBundle.getMessage(ThreadsMemoryView.class, "ThreadsMemoryView_LBL_Statistics"))); // NOI18N
        toolbar.addSpace(5);
        
        toolbar.add(new JLabel(NbBundle.getMessage(ThreadsMemoryView.class, "ThreadsMemoryView_LBL_TCount"))); // NOI18N
        final Dimension tcDim = new Dimension(-1, -1);
        final JLabel threadsCountL = new JLabel() {
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                
                if (tcDim.width >= 0) {
                    dim.width = Math.max(dim.width, tcDim.width);
                    dim.height = Math.max(dim.height, tcDim.height);
                }
                
                return dim;
            }
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        threadsCount = new NumberRenderer() {
            public void setText(String text) {
                super.setText(text);
                threadsCountL.setText(super.getText());
            }
        };
        threadsCount.setDiffMode(true);
        threadsCount.setValue(99, -1);
        tcDim.setSize(threadsCountL.getPreferredSize());
        threadsCount.setDiffMode(false);
        threadsCount.setValue(0, -1);
        toolbar.addSpace(3);
        toolbar.add(threadsCountL);
        
        toolbar.addSpace(5);
        
        toolbar.add(new JLabel(NbBundle.getMessage(ThreadsMemoryView.class, "ThreadsMemoryView_LBL_Total_bytes"))); // NOI18N
        final JLabel threadsTotalBytesL = new JLabel();
        threadsTotalBytes = new NumberRenderer(Formatters.bytesFormat()) {
            public void setText(String text) {
                super.setText(text);
                threadsTotalBytesL.setText(super.getText());
            }
        };
        toolbar.addSpace(3);
        toolbar.add(threadsTotalBytesL);
        
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
    
    
    private static final String COL_NAME = NbBundle.getMessage(MemoryView.class, "COL_Thread_name"); // NOI18N
    private static final String COL_BYTES = NbBundle.getMessage(MemoryView.class, "COL_ABytes"); // NOI18N
    private static final String COL_BYTES_SEC = NbBundle.getMessage(MemoryView.class, "COL_ABytes_Sec"); // NOI18N
    
    private class TreadsAllocTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return COL_NAME;
            } else if (columnIndex == 1) {
                return COL_BYTES;
            } else if (columnIndex == 2) {
                return COL_BYTES_SEC;
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
            return threads.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return threads.get(rowIndex).getThreadName();
            } else if (columnIndex == 1) {
                return allocatedBytes.get(rowIndex).longValue();
            } else if (columnIndex == 2) {
                return allocatedBytesPerSec.isEmpty() ? 0 :
                       allocatedBytesPerSec.get(rowIndex).longValue();
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
    
}
