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
package org.graalvm.visualvm.lib.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.CloseButton;
import org.graalvm.visualvm.lib.profiler.api.ActionsSupport;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.spi.ActionsSupportProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SearchUtils {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.swing.Bundle"); // NOI18N
    public static final String ACTION_FIND = messages.getString("SearchUtils_ActionFind"); // NOI18N
    private static final String MSG_NODATA = messages.getString("SearchUtils_MsgNoData"); // NOI18N
    private static final String MSG_NOTFOUND = messages.getString("SearchUtils_MsgNotFound"); // NOI18N
    private static final String SIDEBAR_CAPTION = messages.getString("SearchUtils_SidebarCaption"); // NOI18N
    private static final String BTN_PREVIOUS = messages.getString("SearchUtils_BtnPrevious"); // NOI18N
    private static final String BTN_PREVIOUS_TOOLTIP = messages.getString("SearchUtils_BtnPreviousTooltip"); // NOI18N
    private static final String BTN_NEXT = messages.getString("SearchUtils_BtnNext"); // NOI18N
    private static final String BTN_NEXT_TOOLTIP = messages.getString("SearchUtils_BtnNextTooltip"); // NOI18N
    private static final String BTN_MATCH_CASE_TOOLTIP = messages.getString("SearchUtils_BtnMatchCaseTooltip"); // NOI18N
    private static final String BTN_CLOSE_TOOLTIP = messages.getString("SearchUtils_BtnCloseTooltip"); // NOI18N
    // -----
    
    public static final String FIND_ACTION_KEY = "find-action-key"; // NOI18N
    public static final String FIND_NEXT_ACTION_KEY = "find-next-action-key"; // NOI18N
    public static final String FIND_PREV_ACTION_KEY = "find-prev-action-key"; // NOI18N
    public static final String FIND_SEL_ACTION_KEY = "find-sel-action-key"; // NOI18N
    
    private static final String LAST_FIND_TEXT = "last-find-text"; // NOI18N
    private static final String LAST_FIND_MATCH_CASE = "last-find-match-case"; // NOI18N
    private static final String FIND_TREE_HELPER = "find-tree-helper"; // NOI18N
    
    
    public static boolean findString(ProfilerTable table, String text) {
        return findString(table, text, true, true);
    }
    
    public static boolean findString(ProfilerTable table, String text, boolean matchCase, boolean next) {
        int rowCount = table.getRowCount();
        
        ProfilerTreeTable treeTable = null;
        
        if (rowCount == 0) {
            ProfilerDialogs.displayWarning(MSG_NODATA, ACTION_FIND, null);
            return false;
        } else if (rowCount == 1) {
            if (!(table instanceof ProfilerTreeTable)) return false;
            
            treeTable = (ProfilerTreeTable)table;
            TreeNode node = treeTable.getValueForRow(0);
            if (node == null || node.isLeaf()) return false;
        }
        
        if (treeTable != null || table instanceof ProfilerTreeTable) {
            if (treeTable == null) treeTable = (ProfilerTreeTable)table;
            return findString(treeTable, text, matchCase, next, null);
        } else {
            table.putClientProperty(LAST_FIND_TEXT, text);
            table.putClientProperty(LAST_FIND_MATCH_CASE, matchCase);
            
            if (!matchCase) text = text.toLowerCase();
            
            int mainColumn = table.convertColumnIndexToView(table.getMainColumn());
        
            int selectedRow = table.getSelectedRow();
            boolean fromSelection = selectedRow != -1;
        
            if (!fromSelection) selectedRow = next ? 0 : rowCount - 1;
            else selectedRow = next ? table.getNextRow(selectedRow) :
                                      table.getPreviousRow(selectedRow);
        
            int searchSteps = fromSelection ? rowCount - 1 : rowCount;
            for (int i = 0; i < searchSteps; i++) {
                String value = table.getStringValue(selectedRow, mainColumn);
                if (!matchCase) value = value.toLowerCase();
                if (value.contains(text)) {
                    table.selectRow(selectedRow, true);
                    return true;
                }
                selectedRow = next ? table.getNextRow(selectedRow) :
                                     table.getPreviousRow(selectedRow);
            }
            
            ProfilerDialogs.displayInfo(MSG_NOTFOUND, ACTION_FIND, null);
            return false;
        }
    }
    
    public static boolean findString(ProfilerTreeTable treeTable, String text, boolean matchCase, boolean next, TreeHelper helper) {
        treeTable.putClientProperty(LAST_FIND_TEXT, text);
        treeTable.putClientProperty(LAST_FIND_MATCH_CASE, matchCase);
        
        if (!matchCase) text = text.toLowerCase();
        
        int mainColumn = treeTable.convertColumnIndexToView(treeTable.getMainColumn());
        
        TreePath selectedPath = treeTable.getSelectionPath();
        if (selectedPath == null) selectedPath = treeTable.getRootPath();
        boolean firstPath = true;
        TreePath startPath = null;
        
        int nodeType = helper == null ? TreeHelper.NODE_SEARCH_DOWN : helper.getNodeType(selectedPath);
        
        do {
            selectedPath = next ? treeTable.getNextPath(selectedPath, TreeHelper.isDown(nodeType)) :
                                  treeTable.getPreviousPath(selectedPath, TreeHelper.isDown(nodeType));
            TreeNode node = (TreeNode)selectedPath.getLastPathComponent();
            
            if (helper != null) nodeType = helper.getNodeType(node);
            
            if (TreeHelper.isSearch(nodeType)) {
                String nodeValue = treeTable.getStringValue(node, mainColumn);
                if (!matchCase) nodeValue = nodeValue.toLowerCase();
                if (nodeValue.contains(text)) {
                    treeTable.selectPath(selectedPath, true);
                    return true;
                }
            }
            
            if (startPath == null) startPath = selectedPath;
            else if (firstPath) firstPath = false;
        } while (firstPath || !selectedPath.equals(startPath));
        
        ProfilerDialogs.displayInfo(MSG_NOTFOUND, ACTION_FIND, null);
        return false;
    }
    
    public static void enableSearchActions(final ProfilerTable table) {
        ActionMap actionMap = table.getActionMap();
        InputMap inputMap = table.getInputMap();
        
        Action nextAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Object text = table.getClientProperty(LAST_FIND_TEXT);
                        Object matchCase = table.getClientProperty(LAST_FIND_MATCH_CASE);
                        if (text != null && matchCase != null) {
                            TreeHelper helper = (TreeHelper)table.getClientProperty(FIND_TREE_HELPER);
                            if (helper == null) findString(table, text.toString(), Boolean.TRUE == matchCase, true);
                            else findString((ProfilerTreeTable)table, text.toString(), Boolean.TRUE == matchCase, true, helper);
                        }
                    }
                });
            }
        };
        ActionsSupport.registerAction(FIND_NEXT_ACTION_KEY, nextAction, actionMap, inputMap);
        
        Action prevAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Object text = table.getClientProperty(LAST_FIND_TEXT);
                        Object matchCase = table.getClientProperty(LAST_FIND_MATCH_CASE);
                        if (text != null && matchCase != null) {
                            TreeHelper helper = (TreeHelper)table.getClientProperty(FIND_TREE_HELPER);
                            if (helper == null) findString(table, text.toString(), Boolean.TRUE == matchCase, false);
                            else findString((ProfilerTreeTable)table, text.toString(), Boolean.TRUE == matchCase, false, helper);
                        }
                            
                    }
                });
            }
        };
        ActionsSupport.registerAction(FIND_PREV_ACTION_KEY, prevAction, actionMap, inputMap);
        
        Action selAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        int selectedRow = table.getSelectedRow();
                        if (selectedRow == -1) return;
                        int mainColumn = table.convertColumnIndexToView(table.getMainColumn());
                        TreeHelper helper = (TreeHelper)table.getClientProperty(FIND_TREE_HELPER);
                        if (helper == null) findString(table, table.getStringValue(selectedRow, mainColumn), true, true);
                        else findString((ProfilerTreeTable)table, table.getStringValue(selectedRow, mainColumn), true, true, helper);
                    }
                });
            }
        };
        ActionsSupport.registerAction(FIND_SEL_ACTION_KEY, selAction, actionMap, inputMap);
    }
    
    
    public static JComponent createSearchPanel(final ProfilerTable table) {
        return createSearchPanel(table, null);
    }
    
    public static JComponent createSearchPanel(final ProfilerTable table, Component[] options) {
        return createSearchPanelImpl(table, null, options);
    }
    
    public static JComponent createSearchPanel(final ProfilerTreeTable table, final TreeHelper helper, Component[] options) {
        return createSearchPanelImpl(table, helper, options);
    }
    
    private static JComponent createSearchPanelImpl(final ProfilerTable table, final TreeHelper helper, Component[] options) {
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
        KeyStroke prevKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK);
        KeyStroke nextKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        
        final JToggleButton matchCase = new JToggleButton(Icons.getIcon(GeneralIcons.MATCH_CASE));
        matchCase.setToolTipText(BTN_MATCH_CASE_TOOLTIP);
        // NOTE: added below
        
        final JButton prev = new JButton(BTN_PREVIOUS, Icons.getIcon(GeneralIcons.FIND_PREVIOUS)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String search = getSearchString(combo);
                        if (search == null || search.isEmpty()) return;
                        if (helper == null) {
                            if (findString(table, search, matchCase.isSelected(), false)) combo.addItem(search);
                        } else {
                            if (findString((ProfilerTreeTable)table, search, matchCase.isSelected(), false, helper))
                                combo.addItem(search);
                        }
                    }
                });
            }
        };
        prev.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        prev.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        String prevAccelerator = ActionsSupport.keyAcceleratorString(prevKey);
        prev.setToolTipText(MessageFormat.format(BTN_PREVIOUS_TOOLTIP, prevAccelerator));
        prev.setEnabled(false);
        toolbar.add(prev);
        
        if (!UIUtils.isAquaLookAndFeel()) toolbar.add(Box.createHorizontalStrut(2));
        
        final JButton next = new JButton(BTN_NEXT, Icons.getIcon(GeneralIcons.FIND_NEXT)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String search = getSearchString(combo);
                        if (search == null || search.isEmpty()) return;
                        if (helper == null) {
                            if (findString(table, search, matchCase.isSelected(), true)) combo.addItem(search);
                        } else {
                            if (findString((ProfilerTreeTable)table, search, matchCase.isSelected(), true, helper))
                                combo.addItem(search);
                        }
                    }
                });
            }
        };
        next.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        next.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        String nextAccelerator = ActionsSupport.keyAcceleratorString(nextKey);
        next.setToolTipText(MessageFormat.format(BTN_NEXT_TOOLTIP, nextAccelerator));
        next.setEnabled(false);
        toolbar.add(next);
        
        toolbar.add(Box.createHorizontalStrut(2));
        
        toolbar.addSeparator();
        
        toolbar.add(Box.createHorizontalStrut(1));
        
        toolbar.add(matchCase);
        
        if (options != null) for (Component option : options) toolbar.add(option);
        
        toolbar.add(Box.createHorizontalStrut(2));
        
        combo.setOnTextChangeHandler(new Runnable() {
            public void run() {
                boolean enable = !combo.getText().trim().isEmpty();
                prev.setEnabled(enable);
                next.setEnabled(enable);
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
        
        final Runnable hider = new Runnable() { public void run() { panel.setVisible(false); } };
        JButton closeButton = CloseButton.create(hider);
        String escAccelerator = ActionsSupport.keyAcceleratorString(escKey);
        closeButton.setToolTipText(MessageFormat.format(BTN_CLOSE_TOOLTIP, escAccelerator));
        panel.add(closeButton, BorderLayout.EAST);
        
        String HIDE = "hide-action"; // NOI18N
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Action hiderAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { hider.run(); }
        };
        panel.getActionMap().put(HIDE, hiderAction);
        inputMap.put(escKey, HIDE);
        
        if (textC != null) {
            inputMap = textC.getInputMap();
            ActionMap actionMap = textC.getActionMap();
            
            String NEXT = "search-next-action"; // NOI18N
            Action nextAction = new AbstractAction() {
                public void actionPerformed(final ActionEvent e) {
                    if (combo.isPopupVisible()) combo.hidePopup();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (next.isEnabled()) next.doClick(); }
                    });
                }
            };
            actionMap.put(NEXT, nextAction);
            inputMap.put(nextKey, NEXT);
            
            KeyStroke nextKey2 = ActionsSupport.registerAction(FIND_NEXT_ACTION_KEY, nextAction, actionMap, inputMap);
            String nextAccelerator2 = ActionsSupport.keyAcceleratorString(nextKey2);
            if (nextAccelerator2 != null) next.setToolTipText(MessageFormat.format(BTN_NEXT_TOOLTIP,
                                                         nextAccelerator + ", " + nextAccelerator2)); // NOI18N

            String PREV = "search-prev-action"; // NOI18N
            Action prevAction = new AbstractAction() {
                public void actionPerformed(final ActionEvent e) {
                    if (combo.isPopupVisible()) combo.hidePopup();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (next.isEnabled()) prev.doClick(); }
                    });
                }
            };
            actionMap.put(PREV, prevAction);
            inputMap.put(prevKey, PREV);
            
            KeyStroke prevKey2 = ActionsSupport.registerAction(FIND_PREV_ACTION_KEY, prevAction, actionMap, inputMap);
            String prevAccelerator2 = ActionsSupport.keyAcceleratorString(prevKey2);
            if (prevAccelerator2 != null) prev.setToolTipText(MessageFormat.format(BTN_PREVIOUS_TOOLTIP,
                                                         prevAccelerator + ", " + prevAccelerator2)); // NOI18N
        }
        
        if (helper != null) table.putClientProperty(FIND_TREE_HELPER, helper);
        
        return panel;
    }
    
    private static String getSearchString(EditableHistoryCombo combo) {
        String search = combo.getText();
        return search == null ? null : search.trim();
    }
    
    
    // Do not create instances of this class
    private SearchUtils() {}
    
    
    public static abstract class TreeHelper {
        
        public static final int NODE_SEARCH_DOWN =  10; // Node to be searched, search its children
        public static final int NODE_SEARCH_NEXT =  11; // Node to be searched, do not search its children
        public static final int NODE_SKIP_DOWN   = 100; // Node not to be searched, search its children
        public static final int NODE_SKIP_NEXT   = 101; // Node not to be searched, do not search its children
        
        public abstract int getNodeType(TreeNode node);
        
        int getNodeType(TreePath path) { return getNodeType((TreeNode)path.getLastPathComponent()); }
        
        static boolean isSearch(int type) { return type < 100; }
        
        static boolean isDown(int type) { return (type & 1) == 0; }
        
    }
    
    
    // Default keybinding Ctrl+F and F3 variants for Find action
    private static interface Support { @ServiceProvider(service=ActionsSupportProvider.class, position=100)
        public static final class SearchActionProvider extends ActionsSupportProvider {
            public KeyStroke registerAction(String actionKey, Action action, ActionMap actionMap, InputMap inputMap) {
                KeyStroke ks = null;
                
                if (FIND_ACTION_KEY.equals(actionKey)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                } else if (FIND_NEXT_ACTION_KEY.equals(actionKey)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
                } else if (FIND_PREV_ACTION_KEY.equals(actionKey)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK);
                } else if (FIND_SEL_ACTION_KEY.equals(actionKey)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.CTRL_MASK);
                }
                
                if (ks != null) {
                    actionMap.put(actionKey, action);
                    inputMap.put(ks, actionKey);
                }

                return ks;
            }
        }
    }
    
}
