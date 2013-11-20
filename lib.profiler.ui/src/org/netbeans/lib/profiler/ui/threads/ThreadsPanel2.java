/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.threads;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.threads.ThreadData;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class ThreadsPanel2 extends JPanel {
    
    private final ThreadsDataManager dataManager;
    private final ViewManager viewManager;
    
    private ProfilerToolbar threadsToolbar;
    private ProfilerTable threadsTable;
    private ProfilerTableContainer threadsTableContainer;
    private JComboBox threadStateFilter;
    private JPanel legendPanel;
    
    
    public ThreadsPanel2(ThreadsDataManager dataManager, Action saveView) {
        this.dataManager = dataManager;
        viewManager = new ViewManager(1, dataManager) {
            public void columnWidthChanged(int column, int oldW, int newW) {
                if (column == 1 && isFit()) threadsTable.updateColumnPreferredWidth(1);
                super.columnWidthChanged(column, oldW, newW);
            }
            public void columnOffsetChanged(int column, int oldO, int newO) {
                super.columnOffsetChanged(column, oldO, newO);
                if (column == 1) repaintTimeline();
            }
            public void zoomChanged(double oldZoom, double newZoom) {
                super.zoomChanged(oldZoom, newZoom);
                repaintTimeline();
            }
            private void repaintTimeline() {
                int _column = threadsTable.convertColumnIndexToView(1);
                JTableHeader header = threadsTable.getTableHeader();
                header.repaint(header.getHeaderRect(_column));
            }
        };
        
        initUI(saveView);
    }
    
    
    private void initUI(Action saveView) {
        
        final AbstractTableModel threadsTableModel = new AbstractTableModel() {
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Name";
                } else if (columnIndex == 1) {
                    return "Timeline";
                } else if (columnIndex == 2) {
                    return "Running";
                } else if (columnIndex == 3) {
                    return "Sleeping";
                } else if (columnIndex == 4) {
                    return "Wait";
                } else if (columnIndex == 5) {
                    return "Park";
                } else if (columnIndex == 6) {
                    return "Monitor";
                } else if (columnIndex == 7) {
                    return "Total";
                }
                return null;
            }
            
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ThreadData.class;
                } else if (columnIndex == 1) {
                    return ViewManager.RowView.class;
                } else {
                    return Long.class;
                }
            }

            public int getRowCount() {
                return dataManager.getThreadsCount();
            }

            public int getColumnCount() {
                return 8;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return getData(rowIndex);
                } else if (columnIndex == 1) {
                    return viewManager.getRowView(rowIndex);
                } else if (columnIndex == 2) {
                    return getData(rowIndex).getRunningTime();
                } else if (columnIndex == 3) {
                    return getData(rowIndex).getSleepingTime();
                } else if (columnIndex == 4) {
                    return getData(rowIndex).getWaitTime();
                } else if (columnIndex == 5) {
                    return getData(rowIndex).getParkTime();
                } else if (columnIndex == 6) {
                    return getData(rowIndex).getMonitorTime();
                } else if (columnIndex == 7) {
                    return getData(rowIndex).getTotalTime();
                }
                
                return null;
            }
            
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
            
            private ThreadData getData(int rowIndex) {
                return dataManager.getThreadData(rowIndex);
            }
        
        };
        
        threadsTable = new ProfilerTable(threadsTableModel, true, true, new int[] { 1 }, true) {
            protected int computeColumnPreferredWidth(int modelIndex, int viewIndex, int firstRow, int lastRow) {
                if (modelIndex != 1) return super.computeColumnPreferredWidth(modelIndex, viewIndex, firstRow, lastRow);
                
                viewManager.update();
                
                if (viewManager.isFit()) {
                    return getTableHeader().getHeaderRect(viewIndex).width;
                } else {
                    return viewManager.getViewWidth();
                }
            }
        };
        threadsTable.setColumnToolTips(new String[] { "Thread name",
                                                      "Thread states timeline",
                                                      "Time spent in Running state",
                                                      "Time spent in Sleeping state",
                                                      "Time spent in Wait state",
                                                      "Time spent in Park state",
                                                      "Time spent in Monitor state",
                                                      "Total thread time" });
        threadsTable.setDefaultSortOrder(1, SortOrder.ASCENDING);
        threadsTable.setSortColumn(1);
        threadsTable.setFitWidthColumn(1);
        NameStateRenderer nameStateRenderer = new NameStateRenderer();
        nameStateRenderer.setText("THREADnameTOsetupCOLUMNwidth"); // NOI18N
        threadsTable.setDefaultColumnWidth(0, nameStateRenderer.getPreferredSize().width);
        threadsTable.setDefaultRenderer(ThreadData.class, nameStateRenderer);
        threadsTable.setDefaultRenderer(ViewManager.RowView.class, new TimelineRenderer(viewManager));
        final LabelRenderer labelRenderer = new LabelRenderer();
        labelRenderer.setText("123456789 ms"); // NOI18N
        labelRenderer.setHorizontalAlignment(SwingConstants.TRAILING);
        threadsTable.setDefaultColumnWidth(labelRenderer.getPreferredSize().width);
        threadsTable.setDefaultRenderer(Long.class, new TableCellRenderer() {
            {
                labelRenderer.setOpaque(true);
                labelRenderer.setMargin(3, 3, 3, 3);
            }
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                labelRenderer.setText(value.toString() + " ms");
                return labelRenderer;
            }
        });
        
        final JTableHeader header = threadsTable.getTableHeader();
        TableCellRenderer headerRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer(new TimelineHeaderRenderer(headerRenderer, 1, viewManager));
        
        threadsTable.setColumnVisibility(3, false);
        threadsTable.setColumnVisibility(4, false);
        threadsTable.setColumnVisibility(5, false);
        threadsTable.setColumnVisibility(6, false);
        
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
        
        threadsToolbar = ProfilerToolbar.create(true);
        threadsToolbar.add(saveView);
        threadsToolbar.addSeparator();
        
        final Action zoomInAction = viewManager.zoomInAction();
        threadsToolbar.add(new JButton(zoomInAction) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                Object newOffset = zoomInAction.getValue(ViewManager.PROP_NEW_OFFSET);
                if (newOffset != null) {
                    int _newOffset = ((Integer)newOffset).intValue();
                    threadsTable.setColumnOffset(1, _newOffset);
                }
                threadsTableModel.fireTableDataChanged();
            }
        });
        
        final Action zoomOutAction = viewManager.zoomOutAction();
        threadsToolbar.add(new JButton(zoomOutAction) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                Object newOffset = zoomOutAction.getValue(ViewManager.PROP_NEW_OFFSET);
                if (newOffset != null) {
                    int _newOffset = ((Integer)newOffset).intValue();
                    threadsTable.setColumnOffset(1, _newOffset);
                }
                threadsTableModel.fireTableDataChanged();
            }
        });
        
        threadsToolbar.add(new JToggleButton(viewManager.fitAction()) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                threadsTableModel.fireTableDataChanged();
            }
        });
        
        threadsToolbar.addSeparator();
        
        threadsToolbar.addSpace(3);
        threadsToolbar.add(new JLabel("View:"));
        threadsToolbar.addSpace(5);
        
        threadStateFilter = new JComboBox(new String[] { "All threads", "Live threads", "Finished threads" }) {
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
            protected void fireActionEvent() {
                super.fireActionEvent();
                updateFilter();
            }
        };
        threadsToolbar.add(threadStateFilter);
        
        setLayout(new BorderLayout());
        add(threadsTableContainer, BorderLayout.CENTER);
        add(legendPanel, BorderLayout.SOUTH);
        
        dataManager.addDataListener(new DataManagerListener() {
            private boolean firstChange = true;
            public void dataChanged() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (firstChange) {
                            firstChange = false;
                            int _column = threadsTable.convertColumnIndexToView(1);
                            JTableHeader header = threadsTable.getTableHeader();
                            header.repaint(header.getHeaderRect(_column));
                        }
                        threadsTableModel.fireTableDataChanged();
                    }
                });
            }
            public void dataReset() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        viewManager.reset();
                        firstChange = true;
                        threadsTableModel.fireTableDataChanged();
                    }
                });
            }
        });
        
        updateFilter();
    }
    
    private void updateFilter() {
        RowFilter filter = null;
        switch (threadStateFilter.getSelectedIndex()) {
            case 1:
                filter = new RowFilter() {
                    public boolean include(RowFilter.Entry entry) {
                        ThreadData data = (ThreadData)entry.getValue(0);
                        return data.getLastState() != CommonConstants.THREAD_STATUS_ZOMBIE;
                    }
                };
                break;
            case 2:
                filter = new RowFilter() {
                    public boolean include(RowFilter.Entry entry) {
                        ThreadData data = (ThreadData)entry.getValue(0);
                        return data.getLastState() == CommonConstants.THREAD_STATUS_ZOMBIE;
                    }
                };
                break;
        }
        TableRowSorter sorter = (TableRowSorter)threadsTable.getRowSorter();
        sorter.setRowFilter(filter);
    }
    
    
    public Component getToolbar() {
        return threadsToolbar.getComponent();
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
//        threadsMonitoringEnabled = false;
//        ((CardLayout) (contentPanel.getLayout())).show(contentPanel, ENABLE_THREADS_PROFILING);
//        updateZoomButtonsEnabledState();
//        threadsSelectionCombo.setEnabled(false);
    }

    public void threadsMonitoringEnabled() {
//        threadsMonitoringEnabled = true;
//        ((CardLayout) (contentPanel.getLayout())).show(contentPanel, THREADS_TABLE);
//        updateZoomButtonsEnabledState();
//        threadsSelectionCombo.setEnabled(true);
    }
    
    public void profilingSessionStarted() {
//        enableThreadsMonitoringButton.setEnabled(true);
//        enableThreadsMonitoringLabel1.setVisible(true);
//        enableThreadsMonitoringButton.setVisible(true);
//        enableThreadsMonitoringLabel3.setVisible(false);
    }
    
    public void profilingSessionFinished() {
//        enableThreadsMonitoringButton.setEnabled(false);
//        enableThreadsMonitoringLabel1.setVisible(false);
//        enableThreadsMonitoringButton.setVisible(false);
//        enableThreadsMonitoringLabel3.setVisible(true);
    }
    
    public void addThreadsMonitoringActionListener(ActionListener listener) {
//        enableThreadsMonitoringButton.addActionListener(listener);
    }
    
}
