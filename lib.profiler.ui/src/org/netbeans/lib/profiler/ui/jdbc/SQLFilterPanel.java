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
package org.netbeans.lib.profiler.ui.jdbc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.results.jdbc.JdbcCCTProvider;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.InvisibleToolbar;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class SQLFilterPanel extends JPanel {
    
    private static final String APPLY_ACTION_KEY = "apply-action-key"; // NOI18N
    
    private boolean initialized = false;
    
    private JCheckBox regularC;
    private JCheckBox preparedC;
    private JCheckBox callableC;
    private JTextField commandsF;
    private JTextField tablesF;
    private JButton applyB;
    
    private Configuration applied = new Configuration() {
        {
            statements.add(JdbcCCTProvider.SQL_STATEMENT);
            statements.add(JdbcCCTProvider.SQL_PREPARED_STATEMENT);
            statements.add(JdbcCCTProvider.SQL_CALLABLE_STATEMENT);
        }
    };
    
    SQLFilterPanel() {
        super(new BorderLayout());
        
        setBorder(new CompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("controlShadow")), // NOI18N
                                     BorderFactory.createMatteBorder(6, 3, 6, 3, UIUtils.getProfilerResultsBackground())));
        
        JToolBar toolbar = new InvisibleToolbar();
        toolbar.setOpaque(true);
        toolbar.setBackground(UIUtils.getProfilerResultsBackground());
        if (UIUtils.isWindowsModernLookAndFeel())
            toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
        else if (!UIUtils.isNimbusLookAndFeel() && !UIUtils.isAquaLookAndFeel())
            toolbar.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        
        KeyStroke applyKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        Action applyAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { if (applyB.isEnabled()) applyB.doClick(); }
                });
            }
        };
        
        toolbar.add(Box.createHorizontalStrut(3));
        toolbar.add(new JLabel("Statements:"));
        
        toolbar.add(Box.createHorizontalStrut(3));
        regularC = new JCheckBox("Regular", true) {
            protected void fireItemStateChanged(ItemEvent event) { changed(); }
        };
        regularC.setOpaque(false);
        regularC.getActionMap().put(APPLY_ACTION_KEY, applyAction);
        regularC.getInputMap().put(applyKey, APPLY_ACTION_KEY);
        toolbar.add(regularC);
        preparedC = new JCheckBox("Prepared", true) {
            protected void fireItemStateChanged(ItemEvent event) { changed(); }
        };
        preparedC.setOpaque(false);
        preparedC.getActionMap().put(APPLY_ACTION_KEY, applyAction);
        preparedC.getInputMap().put(applyKey, APPLY_ACTION_KEY);
        toolbar.add(preparedC);
        callableC = new JCheckBox("Callable", true) {
            protected void fireItemStateChanged(ItemEvent event) { changed(); }
        };
        callableC.setOpaque(false);
        callableC.getActionMap().put(APPLY_ACTION_KEY, applyAction);
        callableC.getInputMap().put(applyKey, APPLY_ACTION_KEY);
        toolbar.add(callableC);
        toolbar.add(Box.createHorizontalStrut(14));
        
        toolbar.add(Box.createHorizontalStrut(3));
        toolbar.add(new JLabel("Commands:"));
        toolbar.add(Box.createHorizontalStrut(3));
        
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        };
        
        commandsF = new JTextField(10);
        commandsF.getDocument().addDocumentListener(dl);
        commandsF.getActionMap().put(APPLY_ACTION_KEY, applyAction);
        commandsF.getInputMap().put(applyKey, APPLY_ACTION_KEY);
        toolbar.add(commandsF);
        toolbar.add(Box.createHorizontalStrut(14));
        
        toolbar.add(Box.createHorizontalStrut(3));
        toolbar.add(new JLabel("Tables:"));
        toolbar.add(Box.createHorizontalStrut(3));
        
        tablesF = new JTextField(10);
        tablesF.getDocument().addDocumentListener(dl);
        tablesF.getActionMap().put(APPLY_ACTION_KEY, applyAction);
        tablesF.getInputMap().put(applyKey, APPLY_ACTION_KEY);
        toolbar.add(tablesF);
        toolbar.add(Box.createHorizontalStrut(10));
        
        toolbar.addSeparator();
        
        toolbar.add(Box.createHorizontalStrut(10));
        applyB = new JButton("Apply") {
            protected void fireActionPerformed(ActionEvent e) { apply(); }
        };
        applyB.setOpaque(false);
        JPanel applyP = new JPanel(new BorderLayout()) {
            public Dimension getMaximumSize() { return getMinimumSize(); }
        };
        applyP.add(applyB, BorderLayout.CENTER);
        applyP.setOpaque(false);
        toolbar.add(applyP);
        toolbar.add(Box.createHorizontalStrut(3));
        
        add(toolbar, BorderLayout.CENTER);
        
        initialized = true;
        changed();
    }
    
    private Configuration current() {
        Configuration current = new Configuration();
        
        if (regularC.isSelected()) current.statements.add(JdbcCCTProvider.SQL_STATEMENT);
        if (preparedC.isSelected()) current.statements.add(JdbcCCTProvider.SQL_PREPARED_STATEMENT);
        if (callableC.isSelected()) current.statements.add(JdbcCCTProvider.SQL_CALLABLE_STATEMENT);
        
        String commands = commandsF.getText().trim();
        if (!commands.isEmpty()) current.commands.addAll(Arrays.asList(commands.toLowerCase().split(" +"))); // NOI18N
        
        String tables = tablesF.getText().trim();
        if (!tables.isEmpty()) current.tables.addAll(Arrays.asList(tables.toLowerCase().split(" +"))); // NOI18N
        
        return current;
    }
    
    private void changed() {
        if (initialized) applyB.setEnabled(!applied.equals(current()));
    }
    
    private void apply() {
        applyB.setEnabled(false);
        applied = current();
        applyFilter();
    }
    
    
    abstract void applyFilter();
    
    boolean passes(int statement, String command, String[] tables) {
        if (!applied.statements.contains(statement)) return false;
        
        if (!applied.commands.isEmpty() && !applied.commands.contains(command.toLowerCase())) return false;
        
        if (applied.tables.isEmpty()) return true;
        for (String table : tables) if (applied.tables.contains(table.toLowerCase())) return true;
        
        return false;
    }
    
    
    private static class Configuration {
        
        final Set<Integer> statements = new HashSet();
        final Set<String> commands = new HashSet();
        final Set<String> tables = new HashSet();
        
        public boolean equals(Object o) {
            Configuration c = (Configuration)o;
            return statements.equals(c.statements) &&
                   commands.equals(c.commands) &&
                   tables.equals(c.tables);
        }
        
    }
    
}
