/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.profiler.v2.features;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.filters.TextFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.jdbc.JdbcCCTProvider;
import org.netbeans.lib.profiler.ui.swing.TextArea;
import org.netbeans.modules.profiler.v2.ui.SettingsPanel;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "SQLFeatureModes_allQueries=All queries",
    "SQLFeatureModes_filteredQueries=Defined queries",
    "SQLFeatureModes_queryContains=Query contains:",
    "SQLFeatureModes_filterHint=case-insensitive query substring(s)"
})
class SQLFeatureModes {
    
    private static abstract class SQLMode extends FeatureMode {
        
        private static final String[] JDBC_MARKER_CLASSES = {
            JdbcCCTProvider.DRIVER_INTERFACE,
            JdbcCCTProvider.CONNECTION_INTERFACE,
            JdbcCCTProvider.STATEMENT_INTERFACE,
            JdbcCCTProvider.PREPARED_STATEMENT_INTERFACE,
            JdbcCCTProvider.CALLABLE_STATEMENT_INTERFACE
        };
        
        void configureSettings(ProfilingSettings settings) {
            settings.setProfilingType(ProfilingSettings.PROFILE_CPU_JDBC);
            settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);

            ClientUtils.SourceCodeSelection[] roots = new ClientUtils.SourceCodeSelection[JDBC_MARKER_CLASSES.length];
            for (int i = 0; i < JDBC_MARKER_CLASSES.length; i++) {
                roots[i] = new ClientUtils.SourceCodeSelection(JDBC_MARKER_CLASSES[i], "*", null); // NOI18N
                roots[i].setMarkerMethod(true);
            }
            settings.addRootMethods(roots);
        }
        
    }
    
    static abstract class AllQueriesMode extends SQLMode {
        
        String getID() {
            return "AllQueriesMode"; // NOI18N
        }

        String getName() {
            return Bundle.SQLFeatureModes_allQueries();
        }
        
        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            settings.setInstrumentationFilter(new TextFilter());
        }
        
        void confirmSettings() {}
        
        boolean pendingChanges() { return false; }

        boolean currentSettingsValid() { return true; }
        
        JComponent getUI() { return null; }
        
    }
    
    static abstract class FilteredQueriesMode extends SQLMode {
        
        private static final String QUERIES_FILTER_FLAG = "QUERIES_FILTER_FLAG"; // NOI18N
        
        private static final int MIN_ROWS = 1;
        private static final int MAX_ROWS = 15;
        private static final int DEFAULT_ROWS = 3;
        private static final int MIN_COLUMNS = 10;
        private static final int MAX_COLUMNS = 100;
        private static final int DEFAULT_COLUMNS = 50;
        
        String getID() {
            return "FilteredQueriesMode"; // NOI18N
        }

        String getName() {
            return Bundle.SQLFeatureModes_filteredQueries();
        }
        
        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            
            String filter = getFlatValues(readFlag(QUERIES_FILTER_FLAG, "").split("\\n")); // NOI18N
            settings.setInstrumentationFilter(new TextFilter(filter, TextFilter.TYPE_INCLUSIVE, false));
        }
        
        void confirmSettings() {
            if (ui != null && filterArea != null) { // filter out notifications from initialization
                assert SwingUtilities.isEventDispatchThread();
                
                String filter = filterArea.showsHint() ? "" : // NOI18N
                                filterArea.getText().trim();
                storeFlag(QUERIES_FILTER_FLAG, filter.isEmpty() ? null : filter);
            }
        }
        
        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                String filter = filterArea.showsHint() ? "" : // NOI18N
                                filterArea.getText().trim();
                if (!filter.equals(readFlag(QUERIES_FILTER_FLAG, ""))) return true; // NOI18N
            }
            return false;
        }

        boolean currentSettingsValid() {
            assert SwingUtilities.isEventDispatchThread();
            
            if (ui != null) {
                if (filterArea.showsHint() || filterArea.getText().trim().isEmpty()) return false;
            } else {
                if (readFlag(QUERIES_FILTER_FLAG, "").isEmpty()) return false; // NOI18N
            }
            
            return true;
        }
        
        private static String getFlatValues(String[] values) {
            StringBuilder convertedValue = new StringBuilder();

            for (int i = 0; i < values.length; i++) {
                String filterValue = values[i].trim();
                if ((i != (values.length - 1)) && !filterValue.endsWith(",")) // NOI18N
                    filterValue = filterValue + ","; // NOI18N
                convertedValue.append(filterValue);
            }

            return convertedValue.toString();
        }
        
        JComponent getUI() {
            if (ui == null) {
                JPanel p = new JPanel(new GridBagLayout());
                p.setOpaque(false);
                
                GridBagConstraints c;
        
                JPanel classesPanel = new SettingsPanel();
                classesPanel.add(new JLabel(Bundle.SQLFeatureModes_queryContains()));
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.fill = GridBagConstraints.NONE;
                c.insets = new Insets(0, 0, 0, 5);
                c.anchor = GridBagConstraints.NORTHWEST;
                p.add(classesPanel, c);

                final JScrollPane[] container = new JScrollPane[1];
                filterArea = new TextArea(readFlag(QUERIES_FILTER_FLAG, "")) { // NOI18N
                    protected void changed() {
                        settingsChanged();
                    }
                    protected boolean changeSize(boolean vertical, boolean direction) {
                        if (vertical) {
                            int rows = readRows();
                            if (direction) rows = Math.min(rows + 1, MAX_ROWS);
                            else rows = Math.max(rows - 1, MIN_ROWS);
                            storeRows(rows);
                        } else {
                            int cols = readColumns();
                            if (direction) cols = Math.min(cols + 3, MAX_COLUMNS);
                            else cols = Math.max(cols - 3, MIN_COLUMNS);
                            storeColumns(cols);
                        }
                        
                        layoutImpl();                        
                        return true;
                    }
                    protected boolean resetSize() {
                        storeRows(DEFAULT_ROWS);
                        storeColumns(DEFAULT_COLUMNS);
                
                        layoutImpl();
                        return true;
                    }
                    private void layoutImpl() {
                        setRows(readRows());
                        setColumns(readColumns());
                        container[0].setPreferredSize(null);
                        container[0].setPreferredSize(container[0].getPreferredSize());
                        container[0].setMinimumSize(container[0].getPreferredSize());
                        JComponent root = SwingUtilities.getRootPane(container[0]);
                        root.doLayout();
                        root.repaint();
                        setColumns(0);
                    }
                    protected void customizePopup(JPopupMenu popup) {
                        popup.addSeparator();
                        popup.add(createResizeMenu());
                    }
                    public Point getToolTipLocation(MouseEvent event) {
                        Component scroll = getParent().getParent();
                        return SwingUtilities.convertPoint(scroll, 0, scroll.getHeight(), this);
                    }
                };
                filterArea.setFont(new Font("Monospaced", Font.PLAIN, filterArea.getFont().getSize())); // NOI18N
                filterArea.setRows(readRows());
                filterArea.setColumns(readColumns());
                container[0] = new JScrollPane(filterArea);
                container[0].setPreferredSize(container[0].getPreferredSize());
                container[0].setMinimumSize(container[0].getPreferredSize());
                filterArea.setColumns(0);
                filterArea.setHint(Bundle.SQLFeatureModes_filterHint());
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 0;
                c.weightx = 1;
                c.weighty = 1;
                c.fill = GridBagConstraints.VERTICAL;
                c.insets = new Insets(0, 0, 0, 5);
                c.anchor = GridBagConstraints.NORTHWEST;
                p.add(container[0], c);
                
                ui = p;
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { settingsChanged(); }
                });
            }
            return ui;
        }
        
        private int readRows() {
            return NbPreferences.forModule(SQLFeatureModes.class).getInt("SQLFeatureModes.rows", DEFAULT_ROWS); // NOI18N
        }
        
        private void storeRows(int rows) {
            NbPreferences.forModule(SQLFeatureModes.class).putInt("SQLFeatureModes.rows", rows); // NOI18N
        }
        
        private int readColumns() {
            return NbPreferences.forModule(SQLFeatureModes.class).getInt("SQLFeatureModes.columns", DEFAULT_COLUMNS); // NOI18N
        }
        
        private void storeColumns(int columns) {
            NbPreferences.forModule(SQLFeatureModes.class).putInt("SQLFeatureModes.columns", columns); // NOI18N
        }
        
        private JComponent ui;
        private TextArea filterArea;
        
    }
    
}
