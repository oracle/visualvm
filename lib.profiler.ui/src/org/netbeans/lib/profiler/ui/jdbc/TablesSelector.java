/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.ui.jdbc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.ui.swing.FilteringToolbar;
import org.netbeans.lib.profiler.ui.swing.ProfilerPopupFactory;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.SmallButton;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class TablesSelector {
    
    private final String[] tables;
    private final Collection<String> selected;
    
    
    public TablesSelector(Collection<String> tables, Collection<String> selected) {
        this.tables = tables.toArray(new String[0]);
        this.selected = new HashSet(selected);
    }
    
    
    public void show(Component invoker) {
        UI ui = new UI();
        ui.show(invoker);
    }
    
    
    protected abstract void selectionChanged(Collection<String> selected);
    
    
    private class UI {
        
        private JPanel panel;
        private SmallButton selectAll;
        private SmallButton unselectAll;
        
        UI() {
            populatePopup();
        }
        
        void show(Component invoker) {
            Dimension panelSize = panel.getPreferredSize();
            ProfilerPopupFactory.getPopup(invoker, panel, invoker.getWidth() - panelSize.width - 6, -panelSize.height - 6).show();
        }
        
        private void populatePopup() {
            JPanel content = new JPanel(new BorderLayout());
            content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            
            JLabel hint = new JLabel("Select tables:", JLabel.LEADING);
            hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            content.add(hint, BorderLayout.NORTH);

            final SelectedTablesModel tablesModel = new SelectedTablesModel();
            final ProfilerTable tablesTable = new ProfilerTable(tablesModel, true, false, null);
            tablesTable.setColumnToolTips(new String[] {
                "Selected for displaying",
                "Table name" });
            tablesTable.setMainColumn(1);
            tablesTable.setFitWidthColumn(1);
            tablesTable.setDefaultSortOrder(1, SortOrder.ASCENDING);
            tablesTable.setSortColumn(1);
            tablesTable.setColumnRenderer(0, new CheckBoxRenderer());
            LabelRenderer projectRenderer = new LabelRenderer();
            tablesTable.setColumnRenderer(1, projectRenderer);
            int w = new JLabel(tablesTable.getColumnName(0)).getPreferredSize().width;
            tablesTable.setDefaultColumnWidth(0, w + 15);
            int h = tablesTable.getRowHeight() * 8;
            h += tablesTable.getTableHeader().getPreferredSize().height;
            projectRenderer.setText("A LONGEST EXPECTED TABLE NAME A LONGEST EXPECTED TABLE NAME"); // NOI18N
            Dimension prefSize = new Dimension(w + projectRenderer.getPreferredSize().width, h);
            tablesTable.setPreferredScrollableViewportSize(prefSize);
            ProfilerTableContainer tableContainer = new ProfilerTableContainer(tablesTable, true, null);
            content.add(tableContainer, BorderLayout.CENTER);

            JToolBar controls = new FilteringToolbar("Filter tables") {
                protected void filterChanged(final String filter) {
                    if (filter == null) tablesTable.setRowFilter(null);
                    else tablesTable.setRowFilter(new RowFilter() {
                        public boolean include(RowFilter.Entry entry) {
                            return entry.getStringValue(1).toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH));
                        }
                    });
                }
            };
            
            controls.add(Box.createHorizontalStrut(2));
            controls.addSeparator();
            controls.add(Box.createHorizontalStrut(3));
            
            selectAll = new SmallButton(" Select All ") {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            selected.clear();
                            tablesModel.fireTableDataChanged();
                            doSelectionChanged(selected);
                        }
                    });
                }
            };
            controls.add(selectAll);
            unselectAll = new SmallButton(" Unselect All ") {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            selected.clear();
                            selected.addAll(Arrays.asList(tables));
                            tablesModel.fireTableDataChanged();
                            doSelectionChanged(selected);
                        }
                    });
                }
            };
            controls.add(unselectAll);

            content.add(controls, BorderLayout.SOUTH);

            panel = content;
            
            updateSelectionButtons();
        }
        
        private void updateSelectionButtons() {
            selectAll.setEnabled(selected.size() > 0);
            unselectAll.setEnabled(selected.size() < tables.length);
        }
        
        private void doSelectionChanged(Collection<String> selected) {
            updateSelectionButtons();
            selectionChanged(selected);
        }
        
        private class SelectedTablesModel extends AbstractTableModel {
            
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Selected";
                } else if (columnIndex == 1) {
                    return "Tables";
                }
                return null;
            }

            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                } else if (columnIndex == 1) {
                    return Lookup.Provider.class;
                }
                return null;
            }

            public int getRowCount() {
                return tables.length;
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return !selected.contains(tables[rowIndex]);
                } else if (columnIndex == 1) {
                    return tables[rowIndex];
                }
                return null;
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (Boolean.TRUE.equals(aValue)) {
                    if (selected.remove(tables[rowIndex])) doSelectionChanged(selected);
                } else {
                    if (selected.add(tables[rowIndex])) doSelectionChanged(selected);
                }
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 0;
            }

        }
        
    }
    
}
