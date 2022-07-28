/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.sampler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.profiler.api.ActionsSupport;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberPercentRenderer;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class MemorySamplerViewSupport {
    
    static final class HeapViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean hasData;
        
        private Map<String, Long[]> eventData;
        
        private String[] names;
        private long[] sizes;
        private long[] counts;
        
        private HeapTableModel tableModel;
        private ProfilerTable table;
        private HideableBarRenderer[] renderers;
        
        private JComponent bottomPanel;
        private JComponent filterPanel;
        private JComponent searchPanel;
        
        
        HeapViewSupport(JFRModel model) {
            hasData = model.containsEvent(JFRSnapshotSamplerViewProvider.ObjectCountChecker.class);
            
            initComponents();
        }
        
        
        @Override
        public void init() {
            if (hasData) eventData = new HashMap<>();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (!hasData) return true;
            
            if (JFRSnapshotSamplerViewProvider.EVENT_OBJECT_COUNT.equals(typeName)) {
                try {
                    eventData.put(event.getClass("objectClass").getName(), new Long[] { event.getLong("totalSize"), event.getLong("count") }); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {
                    System.err.println(">>> " + e);
                }
            }
            return false;
        }

        @Override
        public void done() {
            if (hasData) SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    long total1 = 0, total2 = 0;
                    long max1 = 0, max2 = 0;
                    
                    names = new String[eventData.size()];
                    sizes = new long[eventData.size()];
                    counts = new long[eventData.size()];

                    int i = 0;
                    for (Map.Entry<String, Long[]> entry : eventData.entrySet()) {
                        names[i] = decodeClassName(entry.getKey());
                        sizes[i] = entry.getValue()[0];
                        counts[i] = entry.getValue()[1];
                        max1 = sizes[i] > max1 ? sizes[i] : max1;
                        total1 += sizes[i];
                        max2 = counts[i] > max2 ? counts[i] : max2;
                        total2 += counts[i];
                        i++;
                    }
                    
                    renderers[0].setMaxValue(max1);
                    table.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
                    renderers[0].setMaxValue(total1);
                    
                    renderers[1].setMaxValue(max2);
                    table.setDefaultColumnWidth(2, renderers[1].getOptimalWidth());
                    renderers[1].setMaxValue(total2);
                    
                    tableModel.fireTableDataChanged();

                    eventData.clear();
                    eventData = null;
                }
            });
        }
        
        private static String decodeClassName(String className) {
            className = StringUtils.userFormClassName(className);
            
            if (className.startsWith("L") && className.contains(";")) // NOI18N
                className = className.substring(1, className.length()).replace(";", ""); // NOI18N
            
            return className;
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerViewSupport.class, "LBL_Heap_histogram"), null, 10, this, null); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (!hasData) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("Heap histogram", JFRSnapshotSamplerViewProvider.ObjectCountChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                tableModel = new HeapTableModel();
                table = new ProfilerTable(tableModel, true, true, null) {
                    protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                        final String selectedClass = value == null ? null : value.toString();

                        if (GoToSource.isAvailable()) {
                            popup.add(new JMenuItem(NbBundle.getMessage(MemorySamplerViewSupport.class, "MemoryView_Context_GoToSource")) { // NOI18N
                                { setEnabled(selectedClass != null); setFont(getFont().deriveFont(Font.BOLD)); }
                                protected void fireActionPerformed(ActionEvent e) { GoToSource.openSource(null, selectedClass, null, null); }
                            });
                            popup.addSeparator();
                        }

                        popup.add(createCopyMenuItem());
                        popup.addSeparator();

                        popup.add(new JMenuItem(FilterUtils.ACTION_FILTER) {
                            protected void fireActionPerformed(ActionEvent e) { HeapViewSupport.this.activateFilter(); }
                        });
                        popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
                            protected void fireActionPerformed(ActionEvent e) { HeapViewSupport.this.activateSearch(); }
                        });
                    }
                };
                
                table.providePopupMenu(true);

                table.setMainColumn(0);
                table.setFitWidthColumn(0);

                table.setSortColumn(1);
                table.setDefaultSortOrder(SortOrder.DESCENDING);
                table.setDefaultSortOrder(0, SortOrder.ASCENDING);

                renderers = new HideableBarRenderer[2];
                renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
                renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());

                JavaNameRenderer classRenderer = new JavaNameRenderer(Icons.getIcon(LanguageIcons.CLASS));

                table.setColumnRenderer(0, classRenderer);
                table.setColumnRenderer(1, renderers[0]);
                table.setColumnRenderer(2, renderers[1]);

                add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
                
                InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
                ActionMap actionMap = getActionMap();

                final String filterKey = org.graalvm.visualvm.lib.ui.swing.FilterUtils.FILTER_ACTION_KEY;
                Action filterAction = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        HeapViewSupport.this.activateFilter();
                    }
                };
                ActionsSupport.registerAction(filterKey, filterAction, actionMap, inputMap);

                final String findKey = SearchUtils.FIND_ACTION_KEY;
                Action findAction = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        HeapViewSupport.this.activateSearch();
                    }
                };
                ActionsSupport.registerAction(findKey, findAction, actionMap, inputMap);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { SearchUtils.enableSearchActions(table); }
                });
            }
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
        
        
        private class HeapTableModel extends AbstractTableModel {
        
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Name";
                } else if (columnIndex == 1) {
                    return "Bytes";
                } else if (columnIndex == 2) {
                    return "Objects";
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
                return names == null ? 0 : names.length;
            }

            public int getColumnCount() {
                return 3;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return names[rowIndex];
                } else if (columnIndex == 1) {
                    return sizes[rowIndex];
                } else if (columnIndex == 2) {
                    return counts[rowIndex];
                }

                return null;
            }

        }
        
    }
    
    
    static final class ThreadsMemoryViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean hasData;
        
        private Map<String, Long> eventData;
        
        private String[] names;
        private long[] values;
        
        private TreadsAllocTableModel tableModel;
        private ProfilerTable table;
        private HideableBarRenderer[] renderers;
        
        
        ThreadsMemoryViewSupport(JFRModel model) {
            hasData = model.containsEvent(JFRSnapshotSamplerViewProvider.ThreadAllocationsChecker.class);
            
            initComponents();
        }
        
        
        @Override
        public void init() {
            if (hasData) eventData = new HashMap<>();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (!hasData) return true;
            
            if (JFRSnapshotSamplerViewProvider.EVENT_THREAD_ALLOCATIONS.equals(typeName)) { // NOI18N
                try {
                    JFRThread thread = event.getThread("thread"); // NOI18N
                    if (thread != null) {
                        String threadName = thread.getName(); // NOI18N
                        long allocated = event.getLong("allocated"); // NOI18N
                        Long _allocated = eventData.get(threadName);
                        if (_allocated == null || _allocated < allocated)
                            eventData.put(threadName, allocated);
                    }
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }

        @Override
        public void done() {
            if (hasData) SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    long total = 0;
                    long max = 0;
                    
                    names = new String[eventData.size()];
                    values = new long[eventData.size()];

                    int i = 0;
                    for (Map.Entry<String, Long> entry : eventData.entrySet()) {
                        names[i] = entry.getKey();
                        values[i] = entry.getValue();
                        max = values[i] > max ? values[i] : max;
                        total += values[i++];
                    }
                    
                    renderers[0].setMaxValue(max);
                    table.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
                    renderers[0].setMaxValue(total);
                    
                    tableModel.fireTableDataChanged();

                    eventData.clear();
                    eventData = null;
                }
            });
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerViewSupport.class, "LBL_ThreadAlloc_M"), null, 20, this, null); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (!hasData) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("Per thread allocations", JFRSnapshotSamplerViewProvider.ThreadAllocationsChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                tableModel = new TreadsAllocTableModel();
                table = new ProfilerTable(tableModel, true, true, null);

                table.setMainColumn(0);
                table.setFitWidthColumn(0);

                table.setSortColumn(1);
                table.setDefaultSortOrder(SortOrder.DESCENDING);
                table.setDefaultSortOrder(0, SortOrder.ASCENDING);

                renderers = new HideableBarRenderer[1];
                renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));

                LabelRenderer threadRenderer = new LabelRenderer();
                threadRenderer.setIcon(Icons.getIcon(ProfilerIcons.THREAD));
                threadRenderer.setFont(threadRenderer.getFont().deriveFont(Font.BOLD));

                table.setColumnRenderer(0, threadRenderer);
                table.setColumnRenderer(1, renderers[0]);

                add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
            }
        }
        
        
        private class TreadsAllocTableModel extends AbstractTableModel {
        
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Thread";
                } else if (columnIndex == 1) {
                    return "Allocated";
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
                return names == null ? 0 : names.length;
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return names[rowIndex];
                } else if (columnIndex == 1) {
                    return values[rowIndex];
                }

                return null;
            }

        }
        
    }
    
}
