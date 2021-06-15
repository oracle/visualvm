/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.threads;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.DataManagerListener;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadsDataManager;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.results.DataView;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.CheckBoxRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class ThreadsPanel extends DataView {
    
    private static ResourceBundle BUNDLE() {
        return ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.threads.Bundle"); // NOI18N
    }
    
    public static enum Filter { ALL, LIVE, FINISHED, SELECTED }
    
    private final ThreadsDataManager dataManager;
    private final ViewManager viewManager;
    
    private DataManagerListener listener;
    
    private ProfilerTable threadsTable;
    private ProfilerTableContainer threadsTableContainer;
    private JPanel bottomPanel;
    private JPanel legendPanel;
    
    private Filter filter = Filter.ALL;
    
    private final Set<Integer> selected = new HashSet();
    private final Set<Integer> selectedApplied = new HashSet();
    
    private Component zoomInAction;
    private Component zoomOutAction;
    private Component fitAction;
    
    private ThreadTimeRelRenderer timeRelRenderer;
    
    private long lastTimestamp;
    
    
    public ThreadsPanel(ThreadsDataManager dataManager, Action saveView) {
        this.dataManager = dataManager;
        lastTimestamp = dataManager.getEndTime();
        viewManager = new ViewManager(2, dataManager) {
            public void columnWidthChanged(int column, int oldW, int newW) {
                if (column == 2 && isFit()) threadsTable.updateColumnPreferredWidth(2);
                super.columnWidthChanged(column, oldW, newW);
            }
            public void columnOffsetChanged(int column, int oldO, int newO) {
                super.columnOffsetChanged(column, oldO, newO);
                if (column == 2) repaintTimeline();
            }
            public void zoomChanged(double oldZoom, double newZoom) {
                super.zoomChanged(oldZoom, newZoom);
                repaintTimeline();
            }
        };
        
        initUI(saveView);
    }
    
    
    public void setFilter(Filter filter) {
        selectedApplied.clear();
        if (Filter.SELECTED.equals(filter)) selectedApplied.addAll(selected);
        
        this.filter = filter;
        threadsTable.addRowFilter(new ThreadsFilter());
        
        filterSelected(filter);
    }
    
    public Filter getFilter() {
        return filter;
    }
    
    protected void filterSelected(Filter filter) {}
    
    public boolean hasSelectedThreads() {
        return !selected.isEmpty();
    }
    
    public void showSelectedColumn() {
        threadsTable.setColumnVisibility(0, true);
    }
    
    
    public void cleanup() {
        dataManager.removeDataListener(listener);
    }
    
    
    private void initUI(Action saveView) {
        setOpaque(true);
        setBackground(new HTMLTextArea().getBackground());
        
        final AbstractTableModel threadsTableModel = new AbstractTableModel() {
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return BUNDLE().getString("COL_Selected"); // NOI18N
                } else if (columnIndex == 1) {
                    return BUNDLE().getString("COL_Name"); // NOI18N
                } else if (columnIndex == 2) {
                    return BUNDLE().getString("COL_Timeline"); // NOI18N
                } else if (columnIndex == 3) {
                    return CommonConstants.THREAD_STATUS_RUNNING_STRING;
                } else if (columnIndex == 4) {
                    return CommonConstants.THREAD_STATUS_SLEEPING_STRING;
                } else if (columnIndex == 5) {
                    return CommonConstants.THREAD_STATUS_WAIT_STRING;
                } else if (columnIndex == 6) {
                    return CommonConstants.THREAD_STATUS_PARK_STRING;
                } else if (columnIndex == 7) {
                    return CommonConstants.THREAD_STATUS_MONITOR_STRING;
                } else if (columnIndex == 8) {
                    return BUNDLE().getString("COL_Total"); // NOI18N
                }
                return null;
            }
            
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                } else if (columnIndex == 1) {
                    return ThreadData.class;
                } else if (columnIndex == 2) {
                    return ViewManager.RowView.class;
                } else {
                    return Long.class;
                }
            }

            public int getRowCount() {
                return dataManager.getThreadsCount();
            }

            public int getColumnCount() {
                return 9;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return selected.contains(rowIndex);
                } else if (columnIndex == 1) {
                    return getData(rowIndex);
                } else if (columnIndex == 2) {
                    return viewManager.getRowView(rowIndex);
                } else if (columnIndex == 3) {
                    return getData(rowIndex).getRunningTime(lastTimestamp);
                } else if (columnIndex == 4) {
                    return getData(rowIndex).getSleepingTime(lastTimestamp);
                } else if (columnIndex == 5) {
                    return getData(rowIndex).getWaitTime(lastTimestamp);
                } else if (columnIndex == 6) {
                    return getData(rowIndex).getParkTime(lastTimestamp);
                } else if (columnIndex == 7) {
                    return getData(rowIndex).getMonitorTime(lastTimestamp);
                } else if (columnIndex == 8) {
                    return getData(rowIndex).getTotalTime(lastTimestamp);
                }
                
                return null;
            }
            
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    if (Boolean.FALSE.equals(aValue)) selected.remove(rowIndex);
                    else selected.add(rowIndex);
                }
            }
            
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 0;
            }
            
            private ThreadData getData(int rowIndex) {
                return dataManager.getThreadData(rowIndex);
            }
        
        };
        
        threadsTable = new ProfilerTable(threadsTableModel, true, true, new int[] { 2 }) {
            protected int computeColumnPreferredWidth(int modelIndex, int viewIndex, int firstRow, int lastRow) {
                if (modelIndex != 2) return super.computeColumnPreferredWidth(modelIndex, viewIndex, firstRow, lastRow);
                
                viewManager.update();
                
                if (viewManager.isFit()) {
                    return getTableHeader().getHeaderRect(viewIndex).width;
                } else {
                    return viewManager.getViewWidth();
                }
            }
            public Object getUserValueForRow(int row) {
                if (row == -1) return null;
                if (row >= getModel().getRowCount()) return null; // #239936
                return Integer.valueOf(convertRowIndexToModel(row));
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                if (userValue != null) {
                    final int row = ((Integer)userValue).intValue();
                    final boolean sel = selected.contains(row);
                    popup.add(new JMenuItem(sel ? BUNDLE().getString("ACT_UnselectThread") :
                                                  BUNDLE().getString("ACT_SelectThread")) { // NOI18N
                        protected void fireActionPerformed(ActionEvent e) {
                            if (sel) selected.remove(row);
                            else selected.add(row);
                            threadsTableModel.fireTableDataChanged();
                            if (!sel) showSelectedColumn();
                        }
                    });

                    popup.addSeparator();
                }
                
                popup.add(createCopyMenuItem());
                popup.addSeparator();
                
                popup.add(new JMenuItem(FilterUtils.ACTION_FILTER) {
                    protected void fireActionPerformed(ActionEvent e) { activateFilter(); }
                });
                popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
                    protected void fireActionPerformed(ActionEvent e) { activateSearch(); }
                });
            }
        };
        threadsTable.setColumnVisibility(0, false);
        threadsTable.setMainColumn(1);
        threadsTable.setColumnToolTips(new String[] { BUNDLE().getString("DESC_Selected"), // NOI18N
                                                      BUNDLE().getString("DESC_Name"), // NOI18N
                                                      BUNDLE().getString("DESC_Timeline"), // NOI18N
                                                      BUNDLE().getString("DESC_Running"), // NOI18N
                                                      BUNDLE().getString("DESC_Sleeping"), // NOI18N
                                                      BUNDLE().getString("DESC_Wait"), // NOI18N
                                                      BUNDLE().getString("DESC_Park"), // NOI18N
                                                      BUNDLE().getString("DESC_Monitor"), // NOI18N
                                                      BUNDLE().getString("DESC_Total") }); // NOI18N
        threadsTable.setDefaultSortOrder(1, SortOrder.ASCENDING);
        threadsTable.setDefaultSortOrder(2, SortOrder.ASCENDING);
        threadsTable.setSecondarySortColumn(1); // Simple way for stable sorting, should use threadID
        threadsTable.setSortColumn(2);
        threadsTable.setFitWidthColumn(2);
        NameStateRenderer nameStateRenderer = new NameStateRenderer();
        nameStateRenderer.setText("THREADnameTOsetupCOLUMNwidth"); // NOI18N
        threadsTable.setColumnRenderer(0, new CheckBoxRenderer());
        threadsTable.setDefaultRenderer(ThreadData.class, nameStateRenderer);
        threadsTable.setDefaultRenderer(ViewManager.RowView.class, new TimelineRenderer(viewManager));
        int w = new JLabel(threadsTable.getColumnName(0)).getPreferredSize().width;
        threadsTable.setDefaultColumnWidth(0, w + 15);
        threadsTable.setDefaultColumnWidth(1, nameStateRenderer.getPreferredSize().width);
        
        final JTableHeader header = threadsTable.getTableHeader();
        TableCellRenderer headerRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer(new TimelineHeaderRenderer(headerRenderer, 2, viewManager));
        
        Number refTime = new Long(1234567);
        
        timeRelRenderer = new ThreadTimeRelRenderer(dataManager);
        timeRelRenderer.setMaxValue(refTime.longValue());
        threadsTable.setDefaultColumnWidth(timeRelRenderer.getNoBarWidth());
        threadsTable.setDefaultRenderer(Long.class, timeRelRenderer);
        
        NumberRenderer numberRenderer = new NumberRenderer(Formatters.millisecondsFormat());
        numberRenderer.setValue(refTime, -1);
        threadsTable.setDefaultColumnWidth(8, numberRenderer.getPreferredSize().width);
        threadsTable.setColumnRenderer(8, numberRenderer);
        
        threadsTable.setColumnVisibility(4, false);
        threadsTable.setColumnVisibility(5, false);
        threadsTable.setColumnVisibility(6, false);
        threadsTable.setColumnVisibility(7, false);
        
        threadsTable.providePopupMenu(true);
        
        threadsTableContainer = new ProfilerTableContainer(threadsTable, false, viewManager);
        
        legendPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 7, 8));
        legendPanel.setOpaque(false);
        
        ThreadStateIcon runningIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_RUNNING, 18, 9);
        JLabel runningLegend = new JLabel(CommonConstants.THREAD_STATUS_RUNNING_STRING, runningIcon, SwingConstants.LEADING);
        runningLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        legendPanel.add(runningLegend);
        ThreadStateIcon sleepingIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_SLEEPING, 18, 9);
        JLabel sleepingLegend = new JLabel(CommonConstants.THREAD_STATUS_SLEEPING_STRING, sleepingIcon, SwingConstants.LEADING);
        sleepingLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        legendPanel.add(sleepingLegend);
        ThreadStateIcon waitIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_WAIT, 18, 9);
        JLabel waitLegend = new JLabel(CommonConstants.THREAD_STATUS_WAIT_STRING, waitIcon, SwingConstants.LEADING);
        waitLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        legendPanel.add(waitLegend);
        ThreadStateIcon parkIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_PARK, 18, 9);
        JLabel parkLegend = new JLabel(CommonConstants.THREAD_STATUS_PARK_STRING, parkIcon, SwingConstants.LEADING);
        parkLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        legendPanel.add(parkLegend);
        ThreadStateIcon monitorIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_MONITOR, 18, 9);
        JLabel monitorLegend = new JLabel(CommonConstants.THREAD_STATUS_MONITOR_STRING, monitorIcon, SwingConstants.LEADING);
        monitorLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        legendPanel.add(monitorLegend);
        
        final Action zoomIn = viewManager.zoomInAction();
        zoomInAction = new JButton(zoomIn) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                Object newOffset = zoomIn.getValue(ViewManager.PROP_NEW_OFFSET);
                if (newOffset != null) {
                    int _newOffset = ((Integer)newOffset).intValue();
                    threadsTable.setColumnOffset(2, _newOffset);
                }
                threadsTableModel.fireTableDataChanged();
            }
            public boolean isEnabled() {
                return threadsTable.isShowing() && super.isEnabled();
            }
        };
        
        final Action zoomOut = viewManager.zoomOutAction();
        zoomOutAction = new JButton(zoomOut) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                Object newOffset = zoomOut.getValue(ViewManager.PROP_NEW_OFFSET);
                if (newOffset != null) {
                    int _newOffset = ((Integer)newOffset).intValue();
                    threadsTable.setColumnOffset(2, _newOffset);
                }
                threadsTableModel.fireTableDataChanged();
            }
            public boolean isEnabled() {
                return threadsTable.isShowing() && super.isEnabled();
            }
        };
        
        fitAction = new JToggleButton(viewManager.fitAction()) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                threadsTableModel.fireTableDataChanged();
            }
        };
        fitAction.setEnabled(false);
        
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(legendPanel, BorderLayout.SOUTH);
        
        setOpaque(true);
        setBackground(UIUtils.getProfilerResultsBackground());
        setLayout(new BorderLayout());
        add(threadsTableContainer, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        listener = new DataManagerListener() {
            private boolean firstChange = true;
            public void dataChanged() {
                lastTimestamp = dataManager.getEndTime();
                if (firstChange) {
                    firstChange = false;
                    repaintTimeline();
                }
                threadsTableModel.fireTableDataChanged();
            }
            public void dataReset() {
                viewManager.reset();
                firstChange = true;
                timeRelRenderer.setMaxValue(0);
                threadsTableModel.fireTableDataChanged();
            }
        };
        dataManager.addDataListener(listener);
        
        registerActions();
    }
    
    private void registerActions() {
        ActionMap map = getActionMap();
        
        map.put(FilterUtils.FILTER_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { activateFilter(); }
        });
        
        map.put(SearchUtils.FIND_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { activateSearch(); }
        });
    }
    
    protected ProfilerTable getResultsComponent() {
        return threadsTable;
    }
    
    protected boolean hasBottomFilterFindMargin() {
        return true;
    }
    
    protected void addFilterFindPanel(JComponent comp) {
        bottomPanel.add(comp, BorderLayout.NORTH);
    }
    
    private void repaintTimeline() {
        JTableHeader header = threadsTable.getTableHeader();
        TableColumn draggedColumn = header.getDraggedColumn();
        if (draggedColumn != null && draggedColumn.getModelIndex() == 2) {
            header.repaint();
        } else {
            int _column = threadsTable.convertColumnIndexToView(2);
            header.repaint(header.getHeaderRect(_column));
        }
    }
    
    public Component getToolbar() {
        return null;
    }
    
    public Component getZoomIn() {
        return zoomInAction;
    }
    
    public Component getZoomOut() {
        return zoomOutAction;
    }
    
    public Component getFitWidth() {
        return fitAction;
    }
    
    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        return threadsTableContainer.createTableScreenshot(onlyVisibleArea);
    }
    
    public boolean fitsVisibleArea() {
        return !threadsTableContainer.tableNeedsScrolling();
    }

    public boolean hasView() {
        return threadsTableContainer.isShowing();
    }
    
    public void threadsMonitoringDisabled() {
        fitAction.setEnabled(false);
    }

    public void threadsMonitoringEnabled() {
        fitAction.setEnabled(true);
    }
    
    public void profilingSessionStarted() {
        selected.clear();
        if (!selectedApplied.isEmpty()) setFilter(Filter.LIVE);
    }
    
    public void profilingSessionFinished() {
    }
    
    public void addThreadsMonitoringActionListener(ActionListener listener) {
    }
    
    
    private final class ThreadsFilter extends RowFilter {
        
        public boolean include(RowFilter.Entry entry) {
            ThreadData data = (ThreadData)entry.getValue(1);
            switch (filter) {
                case LIVE: return ThreadData.isAliveState(data.getLastState());
                case FINISHED: return !ThreadData.isAliveState(data.getLastState());
                case SELECTED: return selectedApplied.contains(entry.getIdentifier());
                default: return true;
            }
        }
        
        public boolean equals(Object o) {
            return o instanceof ThreadsFilter;
        }
        
        public int hashCode() {
            return Integer.MAX_VALUE - 11;
        }
        
    }
    
}
