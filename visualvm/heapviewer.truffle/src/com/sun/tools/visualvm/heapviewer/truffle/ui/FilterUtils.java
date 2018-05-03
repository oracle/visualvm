/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.truffle.ui;

import com.sun.tools.visualvm.heapviewer.truffle.swing.EditableHistoryCombo;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
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
import org.netbeans.lib.profiler.filters.GenericFilter;
import org.netbeans.lib.profiler.filters.TextFilter;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.CloseButton;
import org.netbeans.lib.profiler.ui.results.ColoredFilter;
import org.netbeans.lib.profiler.ui.results.PackageColorer;
import org.netbeans.lib.profiler.ui.swing.InvisibleToolbar;
import org.netbeans.lib.profiler.ui.swing.PopupButton;
import org.netbeans.modules.profiler.api.ActionsSupport;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleType;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableView;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleFilterUtils_ActionFilter=Filter",
    "TruffleFilterUtils_TypeFilter=Type Filter:",
    "TruffleFilterUtils_FilterResults=Filter results ({0})",
    "TruffleFilterUtils_MatchCase=Match case",
    "TruffleFilterUtils_Close=Close Filter sidebar ({0})",
    "TruffleFilterUtils_FilterContains=Contains",
    "TruffleFilterUtils_FilterNotContains=Does Not Contain",
    "TruffleFilterUtils_FilterType=Filter type: {0}",
    "TruffleFilterUtils_InsertFilter=Insert Defined Filter"
})
final class FilterUtils {
    
    private static final String FILTER_ACTION_KEY = "filter-action-key"; // NOI18N
    
    private static final String FILTER_CHANGED = "filter-changed"; // NOI18N
    
    
//    public static boolean filterContains(ProfilerTable table, String filter) {
//        return filterContains(table, filter, false, null);
//    }
//    
//    public static boolean filterContains(ProfilerTable table, String filter, boolean matchCase, RowFilter excludes) {
//        return filter(table, new TextFilter(filter, TextFilter.TYPE_INCLUSIVE, matchCase), excludes);
//    }
//    
//    public static boolean filterNotContains(ProfilerTable table, String filter, boolean matchCase, RowFilter excludes) {
//        return filter(table, new TextFilter(filter, TextFilter.TYPE_EXCLUSIVE, matchCase), excludes);
//    }
//    
//    public static boolean filterRegExp(ProfilerTable table, String filter, RowFilter excludes) {
//        return filter(table, new TextFilter(filter, TextFilter.TYPE_REGEXP, false), excludes);
//    }
    
    public static boolean filter(TreeTableView view, final GenericFilter textFilter, final RowFilter excludesFilter) {
        if (textFilter.isAll()) {
            view.setViewFilter(null);
            return false;
        }
        
        view.setViewFilter(new HeapViewerNodeFilter() {
            public boolean passes(HeapViewerNode node, Heap heap) {
//                if (!(node instanceof ClassNode)) return true;
//
//                JavaClass javaClass = ((ClassNode)node).getJavaClass();

                String typeName = HeapViewerNode.getValue(node, TruffleType.TYPE_NAME, heap);
                return typeName == null ? true : textFilter.passes(typeName);
//                if (javaClass == null) return true;
//                if (javaClass.getInstancesCount() == 0) return false;

//                String className = javaClass.getName();
//                if (textFilter.getType() != TextFilter.TYPE_REGEXP) return textFilter.passes(className);
//                else {
//                    for (String value : textFilter.getValues())
//                        if (isInstanceOf(javaClass, value)) return true;
//                    return false;
//                }
            }


//            private boolean isInstanceOf(JavaClass javaClass, String className) {
//                if (javaClass != null) {
////                    JavaClass superCls = javaClass.getSuperClass();
//                    JavaClass cls = javaClass;
//                    for (; cls != null; cls = cls.getSuperClass())
//                        if (cls.getName().equals(className)) return true;
//                }
//                return false;
//            }
        });
        return true;
    }
    
//    public static void filterSubclasses(String className, JComponent filterPanel) {
//        Object filterString = filterPanel.getClientProperty("FILTER_STRING"); // NOI18N
//        if (filterString instanceof JTextComponent) {
//            ((JTextComponent)filterString).setText(className);
//        } else {
//            return;
//        }
//
//        Object filterType = filterPanel.getClientProperty("FILTER_TYPE"); // NOI18N
//        if (filterType instanceof FilterType) {
//            ((FilterType)filterType).filterImpl(TextFilter.TYPE_REGEXP, Icons.getIcon(LanguageIcons.CLASS), FILTER_REGEXP);
//        } else {
//            return;
//        }
//
//        Object filterAction = filterPanel.getClientProperty("FILTER_ACTION"); // NOI18N
//        if (filterAction instanceof AbstractAction) {
//            ((AbstractAction)filterAction).actionPerformed(null);
//        } else {
//            return;
//        }
//    }
    
    public static JComponent createFilterPanel(final TreeTableView view) {
        return createFilterPanel(view, null);
    }
    
    public static JComponent createFilterPanel(final TreeTableView view, final RowFilter excludesFilter) {
        return createFilterPanel(view, excludesFilter, null);
    }
    
    public static JComponent createFilterPanel(final TreeTableView view, final RowFilter excludesFilter, Component[] options) {
        JToolBar toolbar = new InvisibleToolbar();
        if (UIUtils.isWindowsModernLookAndFeel())
            toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
        else if (!UIUtils.isNimbusLookAndFeel() && !UIUtils.isAquaLookAndFeel())
            toolbar.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(new JLabel(Bundle.TruffleFilterUtils_TypeFilter()));
        toolbar.add(Box.createHorizontalStrut(3));
        
        final EditableHistoryCombo combo = new EditableHistoryCombo();        
        final JTextComponent textC = combo.getTextComponent();
        
        JPanel comboContainer = new JPanel(new BorderLayout());
        comboContainer.add(combo, BorderLayout.CENTER);
        comboContainer.setMinimumSize(combo.getMinimumSize());
        comboContainer.setPreferredSize(combo.getPreferredSize());
        comboContainer.setMaximumSize(combo.getMaximumSize());
        
        toolbar.add(comboContainer);
        
        if (PackageColorer.hasRegisteredColors()) {
            toolbar.add(new PopupButton() {
                {
                    setToolTipText(Bundle.TruffleFilterUtils_InsertFilter());
                }
//                protected void displayPopup() {
//                    JPopupMenu menu = new JPopupMenu();
//                    populatePopup(menu);
//                    if (menu.getComponentCount() > 0) {
//                        Dimension size = menu.getPreferredSize();
//                        size.width = Math.max(size.width, getWidth());
//                        menu.setPreferredSize(size);
//                        menu.show(this, 0, -size.height);
//                    }
//                }
                protected void populatePopup(JPopupMenu popup) {
                    for (final ColoredFilter color : PackageColorer.getRegisteredColors()) {
                        if (color.getValue().trim().isEmpty()) continue;
                        Icon icon = color.getColor() == null ? null : color.getIcon(12, 12);
                        popup.add(new JMenuItem(color.getName(), icon) {
                            protected void fireActionPerformed(ActionEvent event) {
                                String current = getFilterString(combo);
                                if (current == null) current = ""; // NOI18N
                                if (!current.isEmpty()) current += " "; // NOI18N
                                current += color.getValue();
                                textC.setText(current);
                                combo.requestFocusInWindow();
                            }
                        });
                    }
                }
            });
        }
        
        toolbar.add(Box.createHorizontalStrut(5));
        
        KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        KeyStroke filterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        
        final TextFilter activeFilter = new TextFilter() {
//            protected void handleInvalidFilter(String invalidValue, RuntimeException e) {
//                ProfilerDialogs.displayError(MessageFormat.format(MSG_INVALID_REGEXP, invalidValue));
//            }
        };
        final TextFilter currentFilter = new TextFilter();
        
        final JButton filter = new JButton(Bundle.TruffleFilterUtils_ActionFilter(), Icons.getIcon(GeneralIcons.FILTER)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                final JButton _this = this;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        activeFilter.copyFrom(currentFilter);
                        if (filter(view, activeFilter, excludesFilter))
                            combo.addItem(activeFilter.getValue());
                        putClientProperty(FILTER_CHANGED, null);
                        updateFilterButton(_this, currentFilter, activeFilter);
                    }
                });
            }
        };
        String filterAccelerator = ActionsSupport.keyAcceleratorString(filterKey);
        filter.setToolTipText(Bundle.TruffleFilterUtils_FilterResults(filterAccelerator));
        
        Action filterAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (filter.isEnabled()) {
                            filter.doClick();
                            combo.requestFocusInWindow();
                        }
                    }
                });
            }
        };
        installAction(filter, filterAction, filterKey, FILTER_ACTION_KEY);
        toolbar.add(filter);
        
        updateFilterButton(filter, currentFilter, activeFilter);
        
        toolbar.add(Box.createHorizontalStrut(2));
        
        toolbar.addSeparator();
        
        toolbar.add(Box.createHorizontalStrut(1));
        
        final JToggleButton matchCase = new JToggleButton(Icons.getIcon(GeneralIcons.MATCH_CASE)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                if (isEnabled()) SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        currentFilter.setCaseSensitive(isSelected());
                        updateFilterButton(filter, currentFilter, activeFilter);
                    }
                });
            }
        };
        matchCase.setToolTipText(Bundle.TruffleFilterUtils_MatchCase());
        installAction(matchCase, filterAction, filterKey, FILTER_ACTION_KEY);
        
        FilterType filterType = new FilterType(Icons.getIcon(GeneralIcons.FILTER_CONTAINS)) {
            protected void populatePopup(JPopupMenu popup) {
                popup.add(new JMenuItem(Bundle.TruffleFilterUtils_FilterContains(), Icons.getIcon(GeneralIcons.FILTER_CONTAINS)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        filterImpl(TextFilter.TYPE_INCLUSIVE, getIcon(), getText());
                    }
                });
                popup.add(new JMenuItem(Bundle.TruffleFilterUtils_FilterNotContains(), Icons.getIcon(GeneralIcons.FILTER_NOT_CONTAINS)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        filterImpl(TextFilter.TYPE_EXCLUSIVE, getIcon(), getText());
                    }
                });
//                popup.add(new JMenuItem(FILTER_REGEXP, Icons.getIcon(LanguageIcons.CLASS)) {
//                    protected void fireActionPerformed(ActionEvent e) {
//                        super.fireActionPerformed(e);
//                        filterImpl(TextFilter.TYPE_REGEXP, getIcon(), getText());
//                    }
//                });
            }
            protected void filterImpl(final int type, final Icon icon, final String name) {
//                if (type == TextFilter.TYPE_REGEXP) {
//                    matchCase.setEnabled(false);
//                    matchCase.setSelected(false);
//                } else {
                    if (!matchCase.isEnabled()) {
                        matchCase.setSelected(currentFilter.isCaseSensitive());
                        matchCase.setEnabled(true);
                    }
//                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setIcon(icon);
                        setToolTipText(Bundle.TruffleFilterUtils_FilterType(name));
                        currentFilter.setType(type);
                        updateFilterButton(filter, currentFilter, activeFilter);
                    }
                });
            }
        };
        filterType.setToolTipText(Bundle.TruffleFilterUtils_FilterType(Bundle.TruffleFilterUtils_FilterContains()));
        installAction(filterType, filterAction, filterKey, FILTER_ACTION_KEY);
        toolbar.add(filterType);
        
        toolbar.add(matchCase);
        
        if (options != null) for (Component option : options) toolbar.add(option);
        
        toolbar.add(Box.createHorizontalStrut(2));
        
        combo.setOnTextChangeHandler(new Runnable() {
            public void run() {
                currentFilter.setValue(getFilterString(combo));
                updateFilterButton(filter, currentFilter, activeFilter);
            }
        });
        
        final JPanel panel = new JPanel(new BorderLayout()) {
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                if (!visible) view.getComponent().requestFocusInWindow();
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
                boolean wasAll = activeFilter.isAll();
                activeFilter.setValue(""); // NOI18N
                updateFilterButton(filter, currentFilter, activeFilter);
                if (!wasAll) filter(view, activeFilter, excludesFilter);
                panel.setVisible(false);
            }
        };
        JButton closeButton = CloseButton.create(hider);
        String escAccelerator = ActionsSupport.keyAcceleratorString(escKey);
        closeButton.setToolTipText(Bundle.TruffleFilterUtils_Close(escAccelerator));
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
            Action _filterAction = new AbstractAction() {
                public void actionPerformed(final ActionEvent e) {
                    if (combo.isPopupVisible()) combo.hidePopup();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (filter.isEnabled()) filter.doClick(); }
                    });
                }
            };
            textC.getActionMap().put(FILTER_ACTION_KEY, _filterAction);
            map.put(filterKey, FILTER_ACTION_KEY);
        }
        
        panel.putClientProperty("SET_FILTER_CHANGED", new AbstractAction() { // NOI18N
            public void actionPerformed(final ActionEvent e) {
                filter.putClientProperty(FILTER_CHANGED, Boolean.TRUE);
                updateFilterButton(filter, currentFilter, activeFilter);
            }
        });
        
        panel.putClientProperty("FILTER_STRING", textC); // NOI18N
        panel.putClientProperty("FILTER_TYPE", filterType); // NOI18N
        panel.putClientProperty("FILTER_ACTION", filterAction); // NOI18N
        
        return panel;
    }
    
    private static abstract class FilterType extends PopupButton {
        FilterType(Icon icon) { super(icon); }
        protected abstract void filterImpl(final int type, final Icon icon, final String name);
    }
    
    private static void installAction(JComponent comp, Action action, KeyStroke keyStroke, String actionKey) {
        comp.getActionMap().put(actionKey, action);
        comp.getInputMap().put(keyStroke, actionKey);
    }
    
    private static String getFilterString(EditableHistoryCombo combo) {
        String filter = combo.getText();
        return filter == null ? null : filter.trim();
    }
    
    private static void updateFilterButton(JButton button, TextFilter currentFilter, TextFilter activeFilter) {
        if (Boolean.TRUE.equals(button.getClientProperty(FILTER_CHANGED))) button.setEnabled(true);
        else button.setEnabled(!currentFilter.equals(activeFilter));
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
    
    
//    // Default keybinding Ctrl+G for Filter action
//    private static interface Support { @ServiceProvider(service=ActionsSupportProvider.class, position=100)
//        public static final class FilterActionProvider extends ActionsSupportProvider {
//            public KeyStroke registerAction(String actionKey, Action action, ActionMap actionMap, InputMap inputMap) {
//                if (!FILTER_ACTION_KEY.equals(actionKey)) return null;
//                
//                KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK);
//                actionMap.put(actionKey, action);
//                inputMap.put(ks, actionKey);
//
//                return ks;
//            }
//        }
//    }
    
}
