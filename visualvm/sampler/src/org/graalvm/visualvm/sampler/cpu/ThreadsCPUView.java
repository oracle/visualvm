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

package org.graalvm.visualvm.sampler.cpu;

import org.graalvm.visualvm.sampler.AbstractSamplerSupport;
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
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.McsTimeRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberPercentRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ThreadsCPUView extends JPanel {
    private static final double NSEC_TO_USEC = 1000.0;
    
    private final AbstractSamplerSupport.Refresher refresher;
    private final CPUSamplerSupport.ThreadDumper threadDumper;
    private boolean forceRefresh = false;
    
    private List<ThreadInfo> threads;
    private List<Long> threadCPUInfo;
    private List<Long> threadCPUInfoPerSec;
    private ThreadsCPUInfo currentThreadsInfo;
    private ThreadsCPUInfo baseThreadsInfo;
    
    private int totalThreads = -1;
    private long totalCPUTime = -1;
    
    ThreadsCPUView(AbstractSamplerSupport.Refresher refresher, CPUSamplerSupport.ThreadDumper threadDumper) {    
        this.refresher = refresher;
        this.threadDumper = threadDumper;
        
        threads = Collections.EMPTY_LIST;
        threadCPUInfo = Collections.EMPTY_LIST;
        threadCPUInfoPerSec = Collections.EMPTY_LIST;
        
        initComponents();
        
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) ThreadsCPUView.this.refresher.refresh();
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
    
    void refresh(ThreadsCPUInfo info) {
        if (!isShowing() || (lrPauseButton.isSelected() && !forceRefresh)) return;
        
        forceRefresh = false;
        threads = info.getThreads();
        threadCPUInfo = info.getThreadCPUTime();
        totalCPUTime = info.getTotalCPUTime();
        if (currentThreadsInfo != null) {
            threadCPUInfoPerSec = currentThreadsInfo.getCPUTimePerSecond(info);
        }
        currentThreadsInfo = info;
        
        boolean diff = lrDeltasButton.isSelected();
        if (diff) {
            if (baseThreadsInfo == null) baseThreadsInfo = info;
            totalThreads = threads.size() - baseThreadsInfo.getThreads().size();
            threadCPUInfo = baseThreadsInfo.getThreadCPUTimeDiff(info);
            totalCPUTime = baseThreadsInfo.getTotalDiffCPUTime();
        } else {
            if (baseThreadsInfo != null) baseThreadsInfo = null;
            threadCPUInfo = info.getThreadCPUTime();
            totalCPUTime = info.getTotalCPUTime();
            totalThreads = threads.size();
        }
        
        renderers[0].setDiffMode(diff);
        renderers[0].setMaxValue((long)Math.ceil(totalCPUTime / NSEC_TO_USEC));
        
        threadsCount.setDiffMode(diff);
        threadsCount.setValue(totalThreads, -1);
        
        threadsTotalTime.setDiffMode(diff);
        threadsTotalTime.setValue(Math.ceil(totalCPUTime / NSEC_TO_USEC), -1);

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
        threaddumpButton.setEnabled(false);
    }
    
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private JToggleButton lrDeltasButton;
    private ProfilerToolbar toolbar;
    
    private AbstractButton threaddumpButton;
    
    private boolean popupPause;
    
    private JComponent bottomPanel;
    private JComponent filterPanel;
    private JComponent searchPanel;
    
    private NumberRenderer threadsCount;
    private McsTimeRenderer threadsTotalTime;
    
    private TreadsCPUTableModel tableModel;
    private ProfilerTable table;
    
    private HideableBarRenderer[] renderers;
    
    private void initComponents() {
        tableModel = new TreadsCPUTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
//            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
//                return ThreadsCPUView.this.getUserValueForRow(row);
//            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                popup.add(createCopyMenuItem());
                popup.addSeparator();
                
                popup.add(new JMenuItem(FilterUtils.ACTION_FILTER) {
                    protected void fireActionPerformed(ActionEvent e) { ThreadsCPUView.this.activateFilter(); }
                });
                popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
                    protected void fireActionPerformed(ActionEvent e) { ThreadsCPUView.this.activateSearch(); }
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
        
        table.setColumnToolTips(new String[] { NbBundle.getMessage(ThreadsCPUView.class, "ThreadsCPUView_TOOLTIP_Col_name"), // NOI18N
                                               NbBundle.getMessage(ThreadsCPUView.class, "ThreadsCPUView_TOOLTIP_Col_time"), // NOI18N
                                               NbBundle.getMessage(ThreadsCPUView.class, "ThreadsCPUView_TOOLTIP_Col_timesec") // NOI18N
                                });
        
        table.providePopupMenu(true);
        
        table.setMainColumn(0);
        table.setFitWidthColumn(0);
        
        table.setSortColumn(1);
        table.setDefaultSortOrder(1, SortOrder.DESCENDING);
        
        renderers = new HideableBarRenderer[2];
        
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer()));
        
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
        
        lrLabel = new GrayLabel(Bundle.MethodsFeatureUI_liveResults());
            
        lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
            protected void fireItemStateChanged(ItemEvent event) {
                boolean paused = isSelected();
                lrRefreshButton.setEnabled(paused && !popupPause);
                if (!paused) refresher.refresh();
            }
        };
        lrPauseButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Pause_results")); // NOI18N

        lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
            protected void fireActionPerformed(ActionEvent e) {
                forceRefresh = true;
                refresher.refresh();
            }
        };
        lrRefreshButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Update_results")); // NOI18N
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
        lrDeltasButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Deltas")); // NOI18N
        
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
        
        toolbar.add(new GrayLabel(NbBundle.getMessage(ThreadsCPUView.class, "ThreadsCPUView_LBL_Statistics"))); // NOI18N
        toolbar.addSpace(5);
        
        toolbar.add(new JLabel(NbBundle.getMessage(ThreadsCPUView.class, "ThreadsCPUView_LBL_TCount"))); // NOI18N
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
        
        toolbar.add(new JLabel(NbBundle.getMessage(ThreadsCPUView.class, "ThreadsCPUView_LBL_Total_time"))); // NOI18N
        final JLabel threadsTotalTimeL = new JLabel();
        threadsTotalTime = new McsTimeRenderer() {
            public void setText(String text) {
                super.setText(text);
                threadsTotalTimeL.setText(super.getText());
            }
        };
        toolbar.addSpace(3);
        toolbar.add(threadsTotalTimeL);
        
        toolbar.addFiller();
        
        threaddumpButton = new JButton(NbBundle.getMessage(CPUView.class, "LBL_Thread_dump")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                threadDumper.takeThreadDump((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width += 5;
                return dim;
            }
        };
        threaddumpButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Thread_dump")); // NOI18N
        threaddumpButton.setOpaque(false);
        threaddumpButton.setEnabled(threadDumper != null);
        toolbar.add(threaddumpButton);
        
        
        setOpaque(false);
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
    
    
    private static final String COL_NAME = NbBundle.getMessage(CPUView.class, "COL_Thread_name"); // NOI18N
    private static final String COL_BYTES = NbBundle.getMessage(CPUView.class, "COL_ABytes"); // NOI18N
    private static final String COL_BYTES_SEC = NbBundle.getMessage(CPUView.class, "COL_ABytes_Sec"); // NOI18N
    
    private class TreadsCPUTableModel extends AbstractTableModel {
        
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
                long threadCPUtime = threadCPUInfo.get(rowIndex).longValue();
                return threadCPUtime / NSEC_TO_USEC;
            } else if (columnIndex == 2) {
                return threadCPUInfoPerSec.isEmpty() ? 0 :
                       Math.min(threadCPUInfoPerSec.get(rowIndex).longValue() / NSEC_TO_USEC, 1000000);
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
