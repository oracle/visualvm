/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.cpu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.table.AbstractTableModel;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.swing.FilteringToolbar;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;
import org.graalvm.visualvm.lib.ui.swing.ProfilerPopup;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.renderer.CheckBoxRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ThreadsSelector extends PopupButton {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.cpu.Bundle"); // NOI18N
    private static final String SELECT_THREADS = messages.getString("ThreadsSelector_SelectThreads"); // NOI18N
    private static final String SELECTED_THREADS = messages.getString("ThreadsSelector_SelectedThreads"); // NOI18N
    private static final String SELECTED_THREADS_ALL = messages.getString("ThreadsSelector_SelectedThreadsAll"); // NOI18N
    private static final String NO_THREADS = messages.getString("ThreadsSelector_NoThreads"); // NOI18N
    private static final String ALL_THREADS = messages.getString("ThreadsSelector_AllThreads"); // NOI18N
    private static final String FILTER_THREADS = messages.getString("ThreadsSelector_FilterThreads"); // NOI18N
    private static final String MERGE_THREADS = messages.getString("ThreadsSelector_MergeThreads"); // NOI18N
    private static final String ALL_THREADS_TOOLTIP = messages.getString("ThreadsSelector_AllThreadsToolTip"); // NOI18N
    private static final String MERGE_THREADS_TOOLTIP = messages.getString("ThreadsSelector_MergeThreadsToolTip"); // NOI18N
    private static final String MERGE_THREADS_TOOLTIP_DISABLED = messages.getString("ThreadsSelector_MergeThreadsToolTipDisabled"); // NOI18N
    private static final String COLUMN_SELECTED = messages.getString("ThreadsSelector_ColumnSelected"); // NOI18N
    private static final String COLUMN_THREAD = messages.getString("ThreadsSelector_ColumnThread"); // NOI18N
    private static final String COLUMN_SELECTED_TOOLTIP = messages.getString("ThreadsSelector_ColumnSelectedToolTip"); // NOI18N
    private static final String COLUMN_THREAD_TOOLTIP = messages.getString("ThreadsSelector_ColumnThreadToolTip"); // NOI18N
    // -----
    
    
    private final Set<Integer> selection = new HashSet();
    
    private boolean displayAllThreads = true;
    private boolean mergeSelectedThreads = false;
    
    private Runnable allThreadsResetter;
    
    
    public ThreadsSelector() {
        super(Icons.getIcon(ProfilerIcons.ALL_THREADS));
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    
    protected abstract CPUResultsSnapshot getSnapshot();
    
    protected abstract void selectionChanged(Collection<Integer> selected, boolean mergeThreads);
    
    
    void reset() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                displayAllThreads = true;
                mergeSelectedThreads = false;
                selection.clear();
            }
        });
    }
    
    void addThread(final int id, final boolean exclusive) {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (exclusive) selection.clear();
                if (selection.add(id)) {
                    displayAllThreads = false;
                    fireSelectionChanged();
                }
            }
        });
    }
    
    void removeThread(final int id) {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (displayAllThreads) {
                    Set<Integer> threads = new HashSet();
                    CPUResultsSnapshot snapshot = getSnapshot();
                    if (snapshot != null)
                        for (int i = 0; i < snapshot.getNThreads(); i++)
                            threads.add(snapshot.getThreadIds()[i]);
                    
                    if (!threads.remove(id)) return;
                    selection.clear();
                    selection.addAll(threads);
                } else {
                    if (!selection.remove(id)) return;
                }
                
                displayAllThreads = false;
                fireSelectionChanged();
            }
        });
    }
    
    
    public String getToolTipText() {
        return displayAllThreads ? SELECTED_THREADS_ALL :
               MessageFormat.format(SELECTED_THREADS, selection.size());
    }
    
    
    protected void displayPopup() {
        CPUResultsSnapshot snapshot = getSnapshot();
        int[] threadIDs = snapshot == null ? null : snapshot.getThreadIds();
//        String[] threadNames = snapshot == null ? null : snapshot.getThreadNames();
        
        int resizeMode;
        JComponent content;
        
        if (threadIDs == null || threadIDs.length == 0) {
            content = new JLabel(NO_THREADS);
            content.setBorder(BorderFactory.createEmptyBorder(9, 6, 9, 6));
            resizeMode = ProfilerPopup.RESIZE_NONE;
        } else {
            content = new JPanel(new BorderLayout());
            
            JLabel hint = new JLabel(SELECT_THREADS, JLabel.LEADING);
            hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
            content.add(hint, BorderLayout.NORTH);
            
            final SelectedThreadsModel threadsModel = new SelectedThreadsModel();
            final ProfilerTable threadsTable = new ProfilerTable(threadsModel, true, false, null);
            threadsTable.setColumnToolTips(new String[] { COLUMN_SELECTED_TOOLTIP, COLUMN_THREAD_TOOLTIP });
            threadsTable.setMainColumn(1);
            threadsTable.setFitWidthColumn(1);
            threadsTable.setDefaultSortOrder(1, SortOrder.ASCENDING);
            threadsTable.setSortColumn(1);
            threadsTable.setFixedColumnSelection(0); // #268298 - make sure SPACE always hits the Boolean column
            threadsTable.setColumnRenderer(0, new CheckBoxRenderer());
            LabelRenderer threadsRenderer = new LabelRenderer();
            threadsRenderer.setIcon(Icons.getIcon(ProfilerIcons.THREAD));
            threadsRenderer.setFont(threadsRenderer.getFont().deriveFont(Font.BOLD));
            threadsTable.setColumnRenderer(1, threadsRenderer);
            int w = new JLabel(threadsTable.getColumnName(0)).getPreferredSize().width;
            threadsTable.setDefaultColumnWidth(0, w + 15);
            int h = threadsTable.getRowHeight() * 8;
            h += threadsTable.getTableHeader().getPreferredSize().height;
            threadsRenderer.setValue("Inactive RequestProcessor thread [Was:Just template/AWT-EventQueue-0]", -1); // NOI18N
            Dimension prefSize = new Dimension(threadsRenderer.getPreferredSize().width, h);
            threadsTable.setPreferredScrollableViewportSize(prefSize);
            ProfilerTableContainer tableContainer = new ProfilerTableContainer(threadsTable, true, null);
            JPanel tableContent = new JPanel(new BorderLayout());
            tableContent.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            tableContent.add(tableContainer, BorderLayout.CENTER);
            content.add(tableContent, BorderLayout.CENTER);

            JToolBar controls = new FilteringToolbar(FILTER_THREADS, true) {
                protected void filterChanged() {
                    if (isAll()) threadsTable.setRowFilter(null);
                    else threadsTable.setRowFilter(new RowFilter() {
                        public boolean include(RowFilter.Entry entry) {
                            return passes(entry.getStringValue(1));
                        }
                    });
                }

                @Override
                protected void selectAllChanged() {
                    if(selection.size() > 0) {
                        selection.clear();
                    } else {
                        CPUResultsSnapshot snapshot = getSnapshot();
                        Set<Integer> snapshopSelection = IntStream.range(0,snapshot.getNThreads())
                                        .filter(i -> passes(snapshot.getThreadNameForId(i)))
                                        .boxed()
                                        .collect(Collectors.toSet());
                        selection.addAll(snapshopSelection);
                    }

                    threadsModel.fireTableDataChanged();
                    fireSelectionChanged();
                }
            };
            
            controls.add(Box.createHorizontalStrut(2));
            controls.addSeparator();
            controls.add(Box.createHorizontalStrut(3));
            
            final JCheckBox mergeThreads = new JCheckBox(MERGE_THREADS, mergeSelectedThreads) {
                protected void fireItemStateChanged(ItemEvent e) {
                    mergeSelectedThreads = isSelected() && !displayAllThreads;
                    fireSelectionChanged();
                }
                public String getToolTipText() {
                    return isEnabled() ? super.getToolTipText() : MERGE_THREADS_TOOLTIP_DISABLED;
                }
            };
            mergeThreads.setToolTipText(MERGE_THREADS_TOOLTIP);
            
            final boolean[] resetterEvent = new boolean[1];
            final JCheckBox allThreads = new JCheckBox(ALL_THREADS, displayAllThreads) {
                protected void fireItemStateChanged(ItemEvent e) {
                    if (resetterEvent[0]) return;
                    displayAllThreads = isSelected();
                    CPUResultsSnapshot snapshot = getSnapshot();
                    if (snapshot != null && displayAllThreads)
                        for (int i = 0; i < snapshot.getNThreads(); i++)
                            selection.add(snapshot.getThreadIds()[i]);
                    else selection.clear();
                    mergeThreads.setEnabled(!displayAllThreads);
                    if (displayAllThreads) {
                        mergeThreads.setSelected(false);
                        mergeSelectedThreads = false;
                    }
                    threadsModel.fireTableDataChanged();
                    fireSelectionChanged();
                }
            };
            allThreads.setToolTipText(ALL_THREADS_TOOLTIP);
            allThreadsResetter = new Runnable() {
                public void run() {
                    resetterEvent[0] = true;
                    allThreads.setSelected(false);
                    mergeThreads.setEnabled(true);
                    resetterEvent[0] = false;
                }
            };
            controls.add(allThreads);
            
            controls.add(Box.createHorizontalStrut(7));
            
            controls.add(mergeThreads);
            
            controls.add(Box.createHorizontalStrut(20));
            
            content.add(controls, BorderLayout.SOUTH);
            
            resizeMode = ProfilerPopup.RESIZE_BOTTOM | ProfilerPopup.RESIZE_RIGHT;
        }

        ProfilerPopup.Listener listener = new ProfilerPopup.Listener() {
            protected void popupHidden() {
                if (!displayAllThreads && selection.isEmpty()) {
                    displayAllThreads = true;
                    mergeSelectedThreads = false;
                    fireSelectionChanged();
                }
                allThreadsResetter = null;
            }
        };
        
        ProfilerPopup.createRelative(this, content, SwingConstants.SOUTH_WEST, resizeMode, listener).show();
    }
    
    
    private void fireSelectionChanged() {
        Collection<Integer> selected = displayAllThreads ? null : new HashSet(selection);
        selectionChanged(selected, mergeSelectedThreads);
    }
    
    
    private class SelectedThreadsModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return COLUMN_SELECTED;
            } else if (columnIndex == 1) {
                return COLUMN_THREAD;
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            } else if (columnIndex == 1) {
                return String.class;
            }
            return null;
        }

        public int getRowCount() {
            CPUResultsSnapshot snapshot = getSnapshot();
            return snapshot == null ? 0 : snapshot.getNThreads();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return selection.contains(getSnapshot().getThreadIds()[rowIndex]);
            } else if (columnIndex == 1) {
                return getSnapshot().getThreadNames()[rowIndex];
            }
            return null;
        }
        
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                int threadId = getSnapshot().getThreadIds()[rowIndex];
                if (Boolean.TRUE.equals(aValue)) selection.add(threadId);
                else selection.remove(threadId);
                if (allThreadsResetter != null) allThreadsResetter.run();
                displayAllThreads = false;
                fireSelectionChanged();
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }
        
    }
    
}
