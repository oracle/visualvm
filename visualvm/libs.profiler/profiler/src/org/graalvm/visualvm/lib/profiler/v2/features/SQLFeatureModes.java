/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.v2.features;

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
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.filters.TextFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcCCTProvider;
import org.graalvm.visualvm.lib.ui.swing.TextArea;
import org.graalvm.visualvm.lib.profiler.v2.ui.SettingsPanel;
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
                filterArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, filterArea.getFont().getSize()));
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
