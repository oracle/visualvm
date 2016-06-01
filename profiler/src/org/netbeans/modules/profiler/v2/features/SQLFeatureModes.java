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

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.filters.TextFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.jdbc.JdbcCCTProvider;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.v2.ui.SettingsPanel;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "SQLFeatureModes_allQueries=All queries",
    "SQLFeatureModes_filteredQueries=Defined queries",
    "SQLFeatureModes_queryContains=Query contains:"
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
        
        String getID() {
            return "FilteredQueriesMode"; // NOI18N
        }

        String getName() {
            return Bundle.SQLFeatureModes_filteredQueries();
        }
        
        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            
            String filter = readFlag(QUERIES_FILTER_FLAG, ""); // NOI18N
            settings.setInstrumentationFilter(new TextFilter(filter, TextFilter.TYPE_INCLUSIVE, false));
        }
        
        void confirmSettings() {
            if (ui != null && filterField != null) { // filter out notifications from initialization
                assert SwingUtilities.isEventDispatchThread();
                
                String filter = filterField.getText().trim();
                storeFlag(QUERIES_FILTER_FLAG, filter.isEmpty() ? null : filter);
            }
        }
        
        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                String filter = filterField.getText().trim();
                if (!filter.equals(readFlag(QUERIES_FILTER_FLAG, ""))) return true; // NOI18N
            }
            return false;
        }

        boolean currentSettingsValid() {
            assert SwingUtilities.isEventDispatchThread();
            
            if (ui != null) {
                if (filterField.getText().trim().isEmpty()) return false;
            } else {
                if (readFlag(QUERIES_FILTER_FLAG, "").isEmpty()) return false; // NOI18N
            }
            
            return true;
        }
        
        JComponent getUI() {
            if (ui == null) {
                ui = new SettingsPanel();
                
                JLabel filterHint = new JLabel(Bundle.SQLFeatureModes_queryContains());
                ui.add(filterHint);
                
                ui.add(Box.createHorizontalStrut(5));
                
                filterField = new JTextField(50) {
                    public Dimension getMaximumSize() {
                        Dimension dim = super.getMaximumSize();
                        dim.height = super.getPreferredSize().height;
                        if (UIUtils.isMetalLookAndFeel()) dim.height += 4;
                        return dim;
                    }
                };
                filterField.setText(readFlag(QUERIES_FILTER_FLAG, "")); // NOI18N
                filterField.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { settingsChanged(); }
                    public void removeUpdate(DocumentEvent e) { settingsChanged(); }
                    public void changedUpdate(DocumentEvent e) { settingsChanged(); }
                });
                ui.add(filterField);
            }
            return ui;
        }
        
        private JComponent ui;
        private JTextField filterField;
        
    }
    
}
