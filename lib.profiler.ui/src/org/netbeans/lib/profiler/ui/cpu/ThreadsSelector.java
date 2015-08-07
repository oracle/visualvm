/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.cpu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.FilteringToolbar;
import org.netbeans.lib.profiler.ui.swing.PopupButton;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ThreadsSelector extends PopupButton {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.Bundle"); // NOI18N
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
    
    
    public String getToolTipText() {
        return displayAllThreads ? SELECTED_THREADS_ALL :
               MessageFormat.format(SELECTED_THREADS, selection.size());
    }
    
    
    protected void populatePopup(final JPopupMenu popup) {
        CPUResultsSnapshot snapshot = getSnapshot();
        int[] threadIDs = snapshot == null ? null : snapshot.getThreadIds();
        String[] threadNames = snapshot == null ? null : snapshot.getThreadNames();
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        if (threadIDs == null || threadIDs.length == 0) {
            JLabel noThreads = new JLabel(NO_THREADS);
            noThreads.setOpaque(false);
            content.add(noThreads, BorderLayout.CENTER);
        } else {
            JLabel hint = new JLabel(SELECT_THREADS, JLabel.LEADING);
            hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            content.add(hint, BorderLayout.NORTH);
            
            final SelectedThreadsModel threadsModel = new SelectedThreadsModel();
            final ProfilerTable threadsTable = new ProfilerTable(threadsModel, true, false, null);
            threadsTable.setMainColumn(1);
            threadsTable.setFitWidthColumn(1);
            threadsTable.setDefaultSortOrder(1, SortOrder.ASCENDING);
            threadsTable.setSortColumn(1);
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
            content.add(tableContainer, BorderLayout.CENTER);
            
            JToolBar controls = new FilteringToolbar(FILTER_THREADS) {
                protected void filterChanged(final String filter) {
                    if (filter == null) threadsTable.setRowFilter(null);
                    else threadsTable.setRowFilter(new RowFilter() {
                        public boolean include(RowFilter.Entry entry) {
                            return entry.getStringValue(1).contains(filter);
                        }
                    });
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
        }
        
        popup.add(content);
        
        popup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {}
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                popup.removePopupMenuListener(this);
                if (!displayAllThreads && selection.isEmpty()) {
                    displayAllThreads = true;
                    mergeSelectedThreads = false;
                    fireSelectionChanged();
                }
                allThreadsResetter = null;
            }
            
        });
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
