/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class FilterComponent implements CommonConstants {
    
    public static FilterComponent create(boolean createDefaultFilters,
                                         boolean toLowerCase) {
        return new Impl(createDefaultFilters, toLowerCase);
    }
    
    
    public abstract JComponent getComponent();
    
    
    public abstract void setFilterValue(String filterValue);
    
    public abstract String getFilterValue();
    
    public abstract void addFilterType(String filterName, int filterType);
    
    public abstract void setFilterType(int filterType);
    
    public abstract int getFilterType();
    
    public abstract void setFilter(String filterValue, int filterType);
    
    public abstract void setHint(String hint);
    
    public abstract String getHint();
    
    public abstract void addChangeListener(ChangeListener listener);
    
    public abstract void removeChangeListener(ChangeListener listener);

    
    public static String[] getFilterValues(String filterValue) {
        return filterValue == null ? null :
                filterValue.trim().split(" +"); // NOI18N
    }
    
    
    protected FilterComponent() {}
    
    
    
    private static class Impl extends FilterComponent {
        
        // -----
        // I18N String constants
        private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.components.Bundle"); // NOI18N
        private static final String STRING_FILTER_CONTAINS = messages.getString("FilterComponent_FilterContains"); // NOI18N
        private static final String STRING_FILTER_NOT_CONTAINS = messages.getString("FilterComponent_FilterNotContains"); // NOI18N
        private static final String STRING_FILTER_REGEXP = messages.getString("FilterComponent_FilterRegexp"); // NOI18N
        private static final String FILTER_TYPE_TOOLTIP = messages.getString("FilterComponent_FilterTypeToolTip"); // NOI18N
        private static final String FILTER_TOOLTIP = messages.getString("FilterComponent_FilterValueToolTip"); // NOI18N
        private static final String SUBMIT_TOOLTIP = messages.getString("FilterComponent_SetFilterButtonToolTip"); // NOI18N
        private static final String CLEAR_TOOLTIP = messages.getString("FilterComponent_ClearFilterButtonToolTip"); // NOI18N
        private static final String ACCESS_NAME = messages.getString("FilterComponent_AccessName"); // NOI18N
        private static final String ACCESS_DESCR = messages.getString("FilterComponent_AccessDescr"); // NOI18N
        private static final String FILTER_HINT = messages.getString("FilterComponent_FilterHint"); // NOI18N
        // -----

        private static final String FILTER_EMPTY = ""; // NOI18N
        
        private final Set<ChangeListener> listeners = new HashSet();
        private final List<String> filterTypeNames = new ArrayList();
        private final List<Integer> filterTypes = new ArrayList();

        private final boolean toLowerCase;
        private String filterValue = FILTER_EMPTY;
        private int filterType = FILTER_NONE;
        private String filterHint;

        private final JComponent component;
        private final FilterCombo filterCombo;
        
        private boolean suppressEvents = false;


        Impl(boolean createDefaultFilters, boolean lowerCase) {
            toLowerCase = lowerCase;
            
            filterCombo = new FilterCombo();
            filterCombo.setToolTipText(FILTER_TOOLTIP);
        
            component = new JPanel(new BorderLayout()) {
                public void setBounds(int x, int y, int w, int h) {
                    if (UIUtils.isAquaLookAndFeel()) w += 2;
                    super.setBounds(x, y, w, h);
                }
            };
            component.setOpaque(false);
            
            if (UIUtils.isWindowsLookAndFeel() || UIUtils.isMetalLookAndFeel()) {
                Insets i = filterCombo.getInsets();
                component.setBorder(BorderFactory.createEmptyBorder(0, -i.left, 0, -i.right));
            } else if (UIUtils.isGTKLookAndFeel()) {
                component.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
            }
            component.add(filterCombo, BorderLayout.CENTER);
            
            if (createDefaultFilters) {
                addFilterType(STRING_FILTER_CONTAINS, FILTER_CONTAINS);
                addFilterType(STRING_FILTER_NOT_CONTAINS, FILTER_NOT_CONTAINS);
                addFilterType(STRING_FILTER_REGEXP, FILTER_REGEXP);
            }
        }
        
        
        public JComponent getComponent() {
            return component;
        };
        
        
        public void setFilterValue(String value) {
            value = value == null ? value : value.trim();
            if (filterValue.equals(value)) return;
            filterValue = value;
            filterCombo.setText(value);
        }
    
        public String getFilterValue() {
            return toLowerCase && isCaseInsensitiveFilter() ?
                    filterValue.toLowerCase() : filterValue;
        }
        
        private boolean isCaseInsensitiveFilter() {
            return filterType == FILTER_CONTAINS ||
                   filterType == FILTER_NOT_CONTAINS;
        }
    
        public void addFilterType(String name, int type) {
            filterTypeNames.add(name);
            filterTypes.add(type);
            if (filterType == FILTER_NONE) setFilterType(type);
        }
    
        public void setFilterType(int type) {
            if (type != FILTER_NONE) filterTypeChanged(type);
        }
    
        public int getFilterType() {
            return getFilterValue().isEmpty() ? FILTER_NONE : filterType;
        }
    
        public void setFilter(String value, int type) {
            if (filterValue.equals(value) && filterType == type) return;
            suppressEvents = true;
            try {
                setFilterType(type);
                setFilterValue(value);
            } finally {
                suppressEvents = false;
            }
            
            fireChange();
        }

        public void setHint(String hint) {
            if (filterHint != null && filterHint.equals(hint)) return;
            filterHint = hint;
            setHintImpl();
        }
        
        private void setHintImpl() {
            String type = filterTypeNames.get(filterTypes.indexOf(filterType));
            filterCombo.setHint(MessageFormat.format(FILTER_HINT, filterHint, type));
        }
        
        public String getHint() {
            return filterHint;
        }
        
        public void addChangeListener(ChangeListener listener) {
            listeners.add(listener);
        }
    
        public void removeChangeListener(ChangeListener listener) {
            listeners.remove(listener);
        }

        
        private void filterValueChanged(String newFilter) {
            if (filterValue.equals(newFilter)) return;
            filterValue = newFilter;
            fireChange();
        }

        private void filterTypeChanged(int newType) {
            if (filterType == newType) return;
            filterType = newType;
            setHintImpl();
            fireChange();
        }
        
        private void fireChange() {
            if (suppressEvents) return;
            ChangeEvent event = new ChangeEvent(this);
            for (ChangeListener listener : listeners)
                listener.stateChanged(event);
        }

        private void submitFilter() {
            String filter = filterCombo.getText();
            filterCombo.addItem(filter);
            filterCombo.setSelectedItem(filter);
        }

        private void clearFilter() {
            filterCombo.setSelectedItem(FILTER_EMPTY);
        }

        private void showPopup() {
            JPopupMenu menu = new JPopupMenu();
            for (int i = 0; i < filterTypeNames.size(); i++) {
                final int type = filterTypes.get(i);
                JMenuItem m = new JRadioButtonMenuItem(filterTypeNames.get(i)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        setFilterType(type);
                    }
                };
                m.setSelected(type == filterType);
                menu.add(m);
            }

            menu.show(filterCombo, 0, filterCombo.getHeight() - filterCombo.getInsets().bottom);
        }


        private final class FilterCombo extends JComboBox {

            public FilterCombo() {
                super(new DefaultComboBoxModel());
                setEditor(new FilterComboEditor(getEditor()));
                setEditable(true); // must be invoked after setEditor
            }

            public void setHint(String hint) {
                getEditorImpl().setHint(hint);
            }

            public String getText() {
                return getEditorImpl().getItem().toString().trim();
            }
            
            public void setText(String text) {
                getEditorImpl().setItem(text);
            }

            public void setSelectedItem(final Object newObject) {
                super.setSelectedItem(newObject);

                final FilterComboEditor editorImpl = getEditorImpl();
                editorImpl.setItem(newObject);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (!isPopupVisible()) {
                            editorImpl.requestFocusInWindow();
                            filterValueChanged(newObject.toString());
                            editorImpl.textChanged();
                        }
                    }
                });
            }

            public Object getSelectedItem() {
                Object selected = super.getSelectedItem();
                return selected == null ? FILTER_EMPTY : selected;
            }

            public void addItem(Object anObject) {
                String item = anObject == null ? null : anObject.toString();
                if (item == null || item.isEmpty()) return;

                DefaultComboBoxModel model = getModelImpl();
                Object selected = model.getSelectedItem();
                int index = model.getIndexOf(item);
                if (index > 0) model.removeElement(item);
                if (index != 0) model.insertElementAt(item, 0);
                model.setSelectedItem(selected);
            }

            public DefaultComboBoxModel getModelImpl() {
                return (DefaultComboBoxModel)super.getModel();
            }

            public FilterComboEditor getEditorImpl() {
                return (FilterComboEditor)super.getEditor();
            }

        }

        private final class FilterComboEditor extends JTextField implements ComboBoxEditor {

            private final IconButton filterButton;
            private final IconButton confirmButton;
            private final IconButton clearButton;
            private final JPanel buttonsPanel;
            private final JLabel hintLabel;

            public FilterComboEditor(ComboBoxEditor impl) {

                // Filter button ---------------------------------------------------
                filterButton = new IconButton(Icons.getIcon(GeneralIcons.FILTER)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        showPopup();
                    }
                    public String getToolTipText() {
                        int filterIndex = filterTypes.indexOf(filterType);
                        return filterIndex != -1 ? MessageFormat.format(FILTER_TYPE_TOOLTIP,
                                filterTypeNames.get(filterIndex)) : null;
                    }
                };
                filterButton.setRolloverIcon(Icons.getIcon(GeneralIcons.FILTER_HIGHL));
                filterButton.setToolTipText(""); // NOI18N // Enable tooltip

                // Confirm button --------------------------------------------------
                confirmButton = new IconButton(Icons.getIcon(GeneralIcons.SET_FILTER)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        submitFilter();
                    }
                };
                confirmButton.setRolloverIcon(Icons.getIcon(GeneralIcons.SET_FILTER_HIGHL));
                confirmButton.setToolTipText(SUBMIT_TOOLTIP);

                // Clear button ----------------------------------------------------
                clearButton = new IconButton(Icons.getIcon(GeneralIcons.CLEAR_FILTER)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        clearFilter();
                    }
                };
                clearButton.setRolloverIcon(Icons.getIcon(GeneralIcons.CLEAR_FILTER_HIGHL));
                clearButton.setToolTipText(CLEAR_TOOLTIP);

                // Confirm & Clear container ---------------------------------------
                buttonsPanel = new JPanel(new BorderLayout(0, 0));
                buttonsPanel.setOpaque(false);
                buttonsPanel.add(confirmButton, BorderLayout.WEST);
                buttonsPanel.add(clearButton, BorderLayout.EAST);
                
                getAccessibleContext().setAccessibleName(ACCESS_NAME);
                getAccessibleContext().setAccessibleDescription(ACCESS_DESCR);

                setLayout(null);
                add(filterButton);
                add(buttonsPanel);

                tweakAppearance((JComponent)impl.getEditorComponent());

                hintLabel = new JLabel() {
                    protected void paintBorder(Graphics g) {}
                };
                hintLabel.setOpaque(false);
                hintLabel.setEnabled(false);
                hintLabel.setBorder(getBorder());

                getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) { textChanged(); }
                    @Override
                    public void removeUpdate(DocumentEvent e) { textChanged(); }
                    @Override
                    public void changedUpdate(DocumentEvent e) { textChanged(); }
                });

                addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) { repaint(); }
                    @Override
                    public void focusLost(FocusEvent e) { repaint(); }
                });

                textChanged();

            }

            private void tweakAppearance(JComponent impl) {
                // Mark as ComboBox editor
                setName("ComboBox.textField"); // Nimbus

                // Button margins
                if (UIUtils.isNimbusLookAndFeel() || UIUtils.isAquaLookAndFeel()) {
                    filterButton.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
                } else if (UIUtils.isWindowsLookAndFeel()) {
                    filterButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 1));
                } else { // Metal, GTK
                    filterButton.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 2));
                }
                confirmButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 4));
                clearButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 4));

                // Set border and margin
                int leftMargin = filterButton.getPreferredSize().width + 2;
                if (UIUtils.isGTKLookAndFeel()) leftMargin -= 4;
                else if (UIUtils.isAquaLookAndFeel()) leftMargin -= 8;
                int rightMargin = buttonsPanel.getPreferredSize().width + 2;
                if (UIUtils.isAquaLookAndFeel()) rightMargin += 10;
                else if (UIUtils.isWindowsLookAndFeel()) rightMargin -= 2;
                Border margin = BorderFactory.createEmptyBorder(0, leftMargin, 0, rightMargin);
                if (UIUtils.isNimbusLookAndFeel()) {
                    setBorder(margin);
                } else {
                    setBorder(impl.getBorder());
                    UIUtils.addBorder(this, margin);
                }

                // Set size
                if (UIUtils.isMetalLookAndFeel()) {
                    setPreferredSize(impl.getPreferredSize());
                }
            }

            public void paint(Graphics g) {
                super.paint(g);
                if (!isFocusOwner() && getText().isEmpty())
                    hintLabel.paint(g);
            }

            public void setBounds(int x, int y, int w, int  h) {
                int woffset = UIUtils.isAquaLookAndFeel() ? 14 : 0;
                super.setBounds(x, y, w + woffset, h);
                hintLabel.setBounds(x, y, w + woffset, h);
            }

            public void doLayout() {
                Dimension d = filterButton.getPreferredSize();
                filterButton.setBounds(0, 0, d.width, getHeight());

                int woffset = UIUtils.isAquaLookAndFeel() ? 14 : 0;
                int xoffset = UIUtils.isAquaLookAndFeel() ? 5 + woffset : 0;
                d = buttonsPanel.getPreferredSize();
                buttonsPanel.setBounds(getWidth() - d.width  - xoffset, 0, d.width, getHeight());
            }

            public void processKeyEvent(KeyEvent e) {
                super.processKeyEvent(e);
                if (e.getID() != KeyEvent.KEY_PRESSED) return;
                
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_ENTER) submitFilter();
                else if (keyCode == KeyEvent.VK_ESCAPE) clearFilter();
                else if ((keyCode == KeyEvent.VK_CONTEXT_MENU) ||
                        ((keyCode == KeyEvent.VK_F10) &&
                        (e.getModifiers() == InputEvent.SHIFT_MASK))) showPopup();
            }

            public boolean isOptimizedDrawingEnabled() {
                return false;
            }

            public void setHint(String hint) {
                hintLabel.setText(hint);
                repaint();
            }

            @Override
            public Component getEditorComponent() {
                return this;
            }

            @Override
            public void setItem(Object anObject) {
                setText(anObject == null ? FILTER_EMPTY : anObject.toString());
            }

            @Override
            public Object getItem() {
                return getText();
            }

            @Override
            public void selectAll() {
                super.selectAll();
                requestFocus();
            }

            @Override
            public void addActionListener(ActionListener l) {
                super.addActionListener(l);
            }

            @Override
            public void removeActionListener(ActionListener l) {
                super.removeActionListener(l);
            }

            public void textChanged() {
                String text = getText();
                confirmButton.setVisible(!text.equals(filterValue));
                clearButton.setVisible(!text.isEmpty());
            }

        }

        private static class IconButton extends JButton {

            public IconButton(Icon icon) {
                super(icon);
                setOpaque(false);
                setRolloverEnabled(true);
                setMargin(new Insets(0, 0, 0, 0));
                setBorderPainted(false);
                setContentAreaFilled(false);
                setRequestFocusEnabled(false);
                setDefaultCapable(false);
                setCursor(Cursor.getDefaultCursor());
            }

        }

    }

}
