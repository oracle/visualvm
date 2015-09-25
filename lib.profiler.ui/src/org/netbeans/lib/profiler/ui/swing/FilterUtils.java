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
package org.netbeans.lib.profiler.ui.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.CloseButton;
import org.netbeans.modules.profiler.api.ActionsSupport;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.spi.ActionsSupportProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public final class FilterUtils {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.swing.Bundle"); // NOI18N
    public static final String ACTION_FILTER = messages.getString("FilterUtils_ActionFilter"); // NOI18N
    private static final String SIDEBAR_CAPTION = messages.getString("FilterUtils_SidebarCaption"); // NOI18N
    private static final String BTN_FILTER_TOOLTIP = messages.getString("FilterUtils_BtnFilterTooltip"); // NOI18N
    private static final String BTN_MATCH_CASE_TOOLTIP = messages.getString("FilterUtils_BtnMatchCaseTooltip"); // NOI18N
    private static final String BTN_CLOSE_TOOLTIP = messages.getString("FilterUtils_BtnCloseTooltip"); // NOI18N
    private static final String MSG_INVALID_REGEXP = messages.getString("FilterUtils_MsgInvalidRegexp"); // NOI18N
    private static final String FILTER_CONTAINS = messages.getString("FilterUtils_FilterContains"); // NOI18N
    private static final String FILTER_NOT_CONTAINS = messages.getString("FilterUtils_FilterNotContains"); // NOI18N
    private static final String FILTER_REGEXP = messages.getString("FilterUtils_FilterRegexp"); // NOI18N
    private static final String FILTER_TYPE = messages.getString("FilterUtils_FilterType"); // NOI18N
    // -----
    
    public static final String FILTER_ACTION_KEY = "filter-action-key"; // NOI18N
    
    
    public static boolean filterContains(ProfilerTable table, String text) {
        return filterContains(table, text, false, null);
    }
    
    public static boolean filterContains(ProfilerTable table, String text, boolean matchCase, RowFilter excludes) {
        return filterImpl(table, text, matchCase, excludes, true);
    }
    
    public static boolean filterNotContains(ProfilerTable table, String text, boolean matchCase, RowFilter excludes) {
        return filterImpl(table, text, matchCase, excludes, false);
    }
    
    private static boolean filterImpl(ProfilerTable table, String text, final boolean matchCase,
                                      final RowFilter excludesFilter, final boolean mode) {
        final int mainColumn = table.getMainColumn();
        
        if (text != null && !matchCase) text = text.toLowerCase();
        final String[] texts = text == null || text.isEmpty() ? new String[0] : text.split(" +"); // NOI18N
        Filter filter = new Filter() {
            public boolean include(RowFilter.Entry entry) {
                if (texts.length == 0) return true;
                if (excludesFilter != null && excludesFilter.include(entry)) return true;
                for (String f : texts) {
                    String value = entry.getValue(mainColumn).toString();
                    if (!matchCase) value = value.toLowerCase();
                    if (value.contains(f)) return mode;
                }
                return !mode;
            }
        };
        
        if (texts.length > 0) {
            table.addRowFilter(filter);
            return table.getRowCount() > 0;
        } else {
            table.removeRowFilter(filter);
            return false;
        }
    }
    
    public static boolean filterRegExp(ProfilerTable table, String text, final boolean matchCase, final RowFilter excludesFilter) {
        Pattern p = null;
        boolean f = text != null && !text.isEmpty();
        if (f) try {
            p = Pattern.compile(text);
        } catch (PatternSyntaxException e) {
            ProfilerDialogs.displayError(MSG_INVALID_REGEXP);
            f = false;
        }
        final boolean _f = f;
        final Pattern _p = p;
        final int mainColumn = table.getMainColumn();
        Filter filter = new Filter() {
            public boolean include(RowFilter.Entry entry) {
                if (!_f) return true;
                if (excludesFilter != null && excludesFilter.include(entry)) return true;
                return _p.matcher(entry.getValue(mainColumn).toString()).matches();
            }
        };
        
        if (f) {
            table.addRowFilter(filter);
            return table.getRowCount() > 0;
        } else {
            table.removeRowFilter(filter);
            return false;
        }
    }
    
    public static JComponent createFilterPanel(final ProfilerTable table) {
        return createFilterPanel(table, null);
    }
    
    public static JComponent createFilterPanel(final ProfilerTable table, final RowFilter excludesFilter) {
        JToolBar toolbar = new InvisibleToolbar();
        if (UIUtils.isWindowsModernLookAndFeel())
            toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
        else if (!UIUtils.isNimbusLookAndFeel() && !UIUtils.isAquaLookAndFeel())
            toolbar.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(new JLabel(SIDEBAR_CAPTION));
        toolbar.add(Box.createHorizontalStrut(3));
        
        final EditableHistoryCombo combo = new EditableHistoryCombo();        
        final JTextComponent textC = combo.getTextComponent();
        
        JPanel comboContainer = new JPanel(new BorderLayout());
        comboContainer.add(combo, BorderLayout.CENTER);
        comboContainer.setMinimumSize(combo.getMinimumSize());
        comboContainer.setPreferredSize(combo.getPreferredSize());
        comboContainer.setMaximumSize(combo.getMaximumSize());
        
        toolbar.add(comboContainer);
        
        toolbar.add(Box.createHorizontalStrut(5));
        
        KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        KeyStroke filterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        
        final JButton[] filterButton = new JButton[1];
        final String[] activeFilter = new String[1];
        final int[] activeFilterType = new int[1];
        final boolean[] activeMatchCase = new boolean[1];
        
        JButton filter = new JButton(ACTION_FILTER, Icons.getIcon(GeneralIcons.FILTER)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String filterString = getFilterString(combo);
                        if (doFilter(table, filterString, activeFilterType[0], activeMatchCase[0], excludesFilter))
                            combo.addItem(filterString);
                        activeFilter[0] = filterString;
                        updateFilterButton(filterButton[0], activeFilter[0], filterString);
                    }
                });
            }
        };
        String filterAccelerator = ActionsSupport.keyAcceleratorString(filterKey);
        filter.setToolTipText(MessageFormat.format(BTN_FILTER_TOOLTIP, filterAccelerator));
        filterButton[0] = filter;
        toolbar.add(filter);
        
        updateFilterButton(filter, activeFilter[0], getFilterString(combo));
        
        toolbar.add(Box.createHorizontalStrut(2));
        
        toolbar.addSeparator();
        
        toolbar.add(Box.createHorizontalStrut(1));
        
        final JToggleButton matchCase = new JToggleButton(Icons.getIcon(GeneralIcons.MATCH_CASE)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                if (isEnabled()) SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        activeMatchCase[0] = isSelected();
                        String filterString = getFilterString(combo);
                        if (doFilter(table, filterString, activeFilterType[0], activeMatchCase[0], excludesFilter))
                            combo.addItem(filterString);
                    }
                });
            }
        };
        matchCase.setToolTipText(BTN_MATCH_CASE_TOOLTIP);
        
        PopupButton filterType = new PopupButton(Icons.getIcon(GeneralIcons.FILTER_CONTAINS)) {
            protected void populatePopup(JPopupMenu popup) {
                popup.add(new JMenuItem(FILTER_CONTAINS, Icons.getIcon(GeneralIcons.FILTER_CONTAINS)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        filterImpl(0, getIcon(), getText());
                    }
                });
                popup.add(new JMenuItem(FILTER_NOT_CONTAINS, Icons.getIcon(GeneralIcons.FILTER_NOT_CONTAINS)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        filterImpl(1, getIcon(), getText());
                    }
                });
                popup.add(new JMenuItem(FILTER_REGEXP, Icons.getIcon(GeneralIcons.FILTER_REG_EXP)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        filterImpl(2, getIcon(), getText());
                    }
                });
            }
            private void filterImpl(final int type, final Icon icon, final String name) {
                if (type == 2) {
                    matchCase.setEnabled(false);
                    matchCase.setSelected(false);
                } else {
                    if (!matchCase.isEnabled()) {
                        matchCase.setSelected(activeMatchCase[0]);
                        matchCase.setEnabled(true);
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setIcon(icon);
                        setToolTipText(MessageFormat.format(FILTER_TYPE, name));
                        activeFilterType[0] = type;
                        String filterString = getFilterString(combo);
                        if (doFilter(table, filterString, activeFilterType[0], activeMatchCase[0], excludesFilter))
                            combo.addItem(filterString);
                    }
                });
            }
        };
        filterType.setToolTipText(MessageFormat.format(FILTER_TYPE, FILTER_CONTAINS));
        toolbar.add(filterType);
        
        toolbar.add(matchCase);
        
        toolbar.add(Box.createHorizontalStrut(2));
        
        combo.setOnTextChangeHandler(new Runnable() {
            public void run() {
                updateFilterButton(filterButton[0], activeFilter[0], getFilterString(combo));
            }
        });
        
        final JPanel panel = new JPanel(new BorderLayout()) {
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                if (!visible) table.requestFocusInWindow();
            }
            public boolean requestFocusInWindow() {
                if (textC != null) textC.selectAll();
                return combo.requestFocusInWindow();
            }
        };
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("controlShadow"))); // NOI18N
        panel.add(toolbar, BorderLayout.CENTER);
        
        final Runnable hider = new Runnable() {
            public void run() {
                activeFilter[0] = null;
                updateFilterButton(filterButton[0], activeFilter[0], getFilterString(combo));
                filterContains(table, activeFilter[0], true, excludesFilter);
                panel.setVisible(false);
            }
        };
        JButton closeButton = CloseButton.create(hider);
        String escAccelerator = ActionsSupport.keyAcceleratorString(escKey);
        closeButton.setToolTipText(MessageFormat.format(BTN_CLOSE_TOOLTIP, escAccelerator));
        panel.add(closeButton, BorderLayout.EAST);
        
        String HIDE = "hide-action"; // NOI18N
        InputMap map = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Action hiderAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { hider.run(); }
        };
        panel.getActionMap().put(HIDE, hiderAction);
        map.put(escKey, HIDE);
        
        if (textC != null) {
            map = textC.getInputMap();
            Action nextAction = new AbstractAction() {
                public void actionPerformed(final ActionEvent e) {
                    if (combo.isPopupVisible()) combo.hidePopup();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (filterButton[0].isEnabled()) filterButton[0].doClick(); }
                    });
                }
            };
            textC.getActionMap().put(FILTER_ACTION_KEY, nextAction);
            map.put(filterKey, FILTER_ACTION_KEY);
        }
        
        return panel;
    }
    
    private static String getFilterString(EditableHistoryCombo combo) {
        String filter = combo.getText();
        return filter == null ? null : filter.trim();
    }
    
    private static boolean doFilter(ProfilerTable table, String text, int filterType, boolean matchCase, RowFilter excludesFilter) {
        switch (filterType) {
            case 0: return filterContains(table, text, matchCase, excludesFilter);
            case 1: return filterNotContains(table, text, matchCase, excludesFilter);
            case 2: return filterRegExp(table, text, matchCase, excludesFilter);
            default: return false;
        }
    }
    
    private static void updateFilterButton(JButton button, String activeFilter, String currentFilter) {
        String active = activeFilter == null ? "" : activeFilter; // NOI18N
        String current = currentFilter == null ? "" : currentFilter; // NOI18N
        button.setEnabled(!current.equals(active));
    }
    
    private static abstract class Filter extends RowFilter {

        public boolean equals(Object o) {
            return o instanceof Filter;
        }
        
        public int hashCode() {
            return Integer.MAX_VALUE;
        }
    
    }
    
    
    // Do not create instances of this class
    private FilterUtils() {}
    
    
    // Default keybinding Ctrl+G for Filter action
    private static interface Support { @ServiceProvider(service=ActionsSupportProvider.class, position=100)
        public static final class FilterActionProvider extends ActionsSupportProvider {
            public KeyStroke registerAction(String actionKey, Action action, ActionMap actionMap, InputMap inputMap) {
                if (!FILTER_ACTION_KEY.equals(actionKey)) return null;
                
                KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK);
                actionMap.put(actionKey, action);
                inputMap.put(ks, actionKey);

                return ks;
            }
        }
    }
    
}
