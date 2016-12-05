/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.v2.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.ui.swing.FilteringToolbar;
import org.netbeans.lib.profiler.ui.swing.ProfilerPopup;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProjectsSelector_selectProjects=Select projects:",
    "ProjectsSelector_filterProjects=Filter projects",
    "ProjectsSelector_columnSelected=Selected",
    "ProjectsSelector_columnProject=Project",
    "ProjectsSelector_columnSelectedToolTip=Selected for profiling",
    "ProjectsSelector_columnProjectToolTip=Project name"
})
public abstract class ProjectsSelector {
    
    private final Collection<Lookup.Provider> selected;
    
    public ProjectsSelector(Collection<Lookup.Provider> selected) {
        this.selected = new HashSet(selected);
    }
    
    
    public void show(Component invoker) {
        UI ui = new UI(selected);
        ui.show(invoker);
    }
    
    
    protected abstract void selectionChanged(Collection<Lookup.Provider> selected);
    
    
    private class UI {
        
        private JPanel panel;
        
        UI(Collection<Lookup.Provider> selected) {
            populatePopup();
        }
        
        void show(Component invoker) {
//            ProfilerPopupFactory.getPopup(invoker, panel, -5, invoker.getHeight() - 1).show();
            ProfilerPopup.create(invoker, panel, -5, invoker.getHeight() - 1).show();
        }
        
        private void populatePopup() {
            JPanel content = new JPanel(new BorderLayout());
            content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            
            JLabel hint = new JLabel(Bundle.ProjectsSelector_selectProjects(), JLabel.LEADING);
            hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            content.add(hint, BorderLayout.NORTH);

            final SelectedProjectsModel projectsModel = new SelectedProjectsModel();
            final ProfilerTable projectsTable = new ProfilerTable(projectsModel, true, false, null);
            projectsTable.setColumnToolTips(new String[] {
                Bundle.ProjectsSelector_columnSelectedToolTip(),
                Bundle.ProjectsSelector_columnProjectToolTip() });
            projectsTable.setMainColumn(1);
            projectsTable.setFitWidthColumn(1);
            projectsTable.setDefaultSortOrder(1, SortOrder.ASCENDING);
            projectsTable.setSortColumn(1);
            projectsTable.setFixedColumnSelection(0); // #268298 - make sure SPACE always hits the Boolean column
            projectsTable.setColumnRenderer(0, new CheckBoxRenderer());
            LabelRenderer projectRenderer = new ProjectRenderer();
            projectsTable.setColumnRenderer(1, projectRenderer);
            int w = new JLabel(projectsTable.getColumnName(0)).getPreferredSize().width;
            projectsTable.setDefaultColumnWidth(0, w + 15);
            int h = projectsTable.getRowHeight() * 8;
            h += projectsTable.getTableHeader().getPreferredSize().height;
            projectRenderer.setText("A longest expected project name A longest expected project name"); // NOI18N
            Dimension prefSize = new Dimension(w + projectRenderer.getPreferredSize().width, h);
            projectsTable.setPreferredScrollableViewportSize(prefSize);
            ProfilerTableContainer tableContainer = new ProfilerTableContainer(projectsTable, true, null);
            content.add(tableContainer, BorderLayout.CENTER);

            JToolBar controls = new FilteringToolbar(Bundle.ProjectsSelector_filterProjects()) {
                protected void filterChanged() {
                    if (isAll()) projectsTable.setRowFilter(null);
                    else projectsTable.setRowFilter(new RowFilter() {
                        public boolean include(RowFilter.Entry entry) {
                            return passes(entry.getStringValue(1));
                        }
                    });
                }
            };

            content.add(controls, BorderLayout.SOUTH);

            panel = content;
        }
        
        private class SelectedProjectsModel extends AbstractTableModel {
            
            Lookup.Provider[] projects = ProjectUtilities.getOpenedProjects(); 
            
            SelectedProjectsModel() {
                ProjectUtilities.getOpenedProjects();
            }
        
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return Bundle.ProjectsSelector_columnSelected();
                } else if (columnIndex == 1) {
                    return Bundle.ProjectsSelector_columnProject();
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
                return projects.length;
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return selected.contains(projects[rowIndex]);
                } else if (columnIndex == 1) {
                    return projects[rowIndex];
                }
                return null;
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (Boolean.TRUE.equals(aValue)) {
                    if (selected.add(projects[rowIndex])) selectionChanged(selected);
                } else if (selected.size() > 1) {
                    if (selected.remove(projects[rowIndex])) selectionChanged(selected);
                }
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 0;
            }

        }
        
    }
    
    private static class ProjectRenderer extends LabelRenderer {
        
        private final Font font;
        private final Lookup.Provider main;
        

        public ProjectRenderer() {
            font = getFont();
            main = ProjectUtilities.getMainProject();
        }

        public void setValue(Object value, int row) {
            if (value == null) {
                setText(""); // NOI18N
                setIcon(null);
            } else {
                Lookup.Provider project = (Lookup.Provider)value;
                setText(ProjectUtilities.getDisplayName(project));
                Icon icon = ProjectUtilities.getIcon(project);
                setIcon(isEnabled() ? icon : disabledIcon(icon));
                setFont(Objects.equals(main, value) ? font.deriveFont(Font.BOLD) : font);
            }
        }
        
        private static Icon disabledIcon(Icon icon) {
            return new ImageIcon(GrayFilter.createDisabledImage(((ImageIcon)icon).getImage()));
        }
        
    }
    
}
