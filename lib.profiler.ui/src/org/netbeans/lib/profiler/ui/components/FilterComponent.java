/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;


/**
 *
 * @author Jiri Sedlacek
 */
public class FilterComponent extends JPanel {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public interface FilterListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void filterChanged();
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //--- Private classes -----
    private class ArrowSignIcon implements Icon {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final ImageIcon popupArrowIcon = new ImageIcon(getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/popupArrow.png")); // NOI18N
        private Icon icon;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ArrowSignIcon(Icon icon) {
            this.icon = icon;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getIconHeight() {
            return icon.getIconHeight();
        }

        public int getIconWidth() {
            return icon.getIconWidth();
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            icon.paintIcon(c, g, x, y);
            popupArrowIcon.paintIcon(c, g, (x + icon.getIconWidth()) - popupArrowIcon.getIconWidth(),
                                     (y + icon.getIconHeight()) - popupArrowIcon.getIconHeight());

            /*g.setColor(Color.DARK_GRAY);
               g.drawLine(x + icon.getIconWidth() - 8, y + icon.getIconHeight() - 5, x + icon.getIconWidth() - 2, y + icon.getIconHeight() - 5);
               g.drawLine(x + icon.getIconWidth() - 7, y + icon.getIconHeight() - 4, x + icon.getIconWidth() - 3, y + icon.getIconHeight() - 4);
               g.drawLine(x + icon.getIconWidth() - 6, y + icon.getIconHeight() - 3, x + icon.getIconWidth() - 4, y + icon.getIconHeight() - 3);
               g.drawLine(x + icon.getIconWidth() - 5, y + icon.getIconHeight() - 2, x + icon.getIconWidth() - 5, y + icon.getIconHeight() - 2);*/
        }
    }

    private static class ButtonBorderIcon implements Icon {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Color BUTTON_BORDER_DARK = new Color(115, 115, 115);
        private Color BUTTON_BORDER_LIGHT = new Color(204, 204, 204);
        private Icon icon;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ButtonBorderIcon(Icon icon) {
            this.icon = icon;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getIconHeight() {
            return icon.getIconHeight();
        }

        public int getIconWidth() {
            return icon.getIconWidth();
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            icon.paintIcon(c, g, x, y);
            g.setColor(BUTTON_BORDER_DARK);
            g.drawLine((x + icon.getIconWidth()) - 1, y, (x + icon.getIconWidth()) - 1, (y + icon.getIconHeight()) - 1);
            g.drawLine(x, (y + icon.getIconHeight()) - 1, (x + icon.getIconWidth()) - 1, (y + icon.getIconHeight()) - 1);
            g.setColor(BUTTON_BORDER_LIGHT);
            g.drawLine(x, y, (x + icon.getIconWidth()) - 1, y);
            g.drawLine(x, y, x, (y + icon.getIconHeight()) - 1);
        }
    }

    private class FilterStringComboActionListener implements ActionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(ActionEvent e) {
            if (internalChange) {
                return;
            }

            String string = (String) filterStringCombo.getSelectedItem();

            if ((string == null) || (string.equals(""))) {
                return; // NOI18N
                        //if (string == filterStringCombo.comboPopupSeparatorString) return;
            }

            if ((filterStringCombo.comboPopupKeyFlag) || (!filterStringCombo.comboPopupActionFlag)) {
                return;
            }

            setFilterString(string);
            updateSetClearButtons();
        }
    }

    private class FilterStringComboPopupMenuListener implements PopupMenuListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void popupMenuCanceled(PopupMenuEvent e) {
            filterStringCombo.comboPopupActionFlag = false;

            //filterStringCombo.comboPopupSeparatorSelectedFlag = false;
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            filterStringCombo.comboPopupActionFlag = false;
            //filterStringCombo.comboPopupSeparatorSelectedFlag = false;
            filterStringCombo.clearSelection();
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            filterStringCombo.comboPopupActionFlag = true;

            //filterStringCombo.comboPopupSeparatorSelectedFlag = false;
        }
    }

    private class FilterStringComboRenderer extends DefaultListCellRenderer {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JSeparator separator;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public FilterStringComboRenderer() {
            super();
            separator = new JSeparator();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            /*if ((value == filterStringCombo.comboPopupSeparatorString) && (isSelected)) {
               filterStringCombo.comboPopupSeparatorSelectedFlag = true;
               } else {
                 filterStringCombo.comboPopupSeparatorSelectedFlag = false;
               }
            
               if (value == filterStringCombo.comboPopupSeparatorString) return separator;*/
            JComponent renderer = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            renderer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0),
                                                                  renderer.getBorder()));

            //if ((!isSelected) && (index >= nOwnComboItems)) renderer.setForeground(Color.DARK_GRAY.brighter());
            return renderer;
        }
    }

    private class FilterTextFieldDocumentListener implements DocumentListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void changedUpdate(DocumentEvent e) {
            if (filterStringCombo.comboPopupActionFlag) {
                return;
            }

            checkRegExp();
            updateSetClearButtons();
        }

        public void insertUpdate(DocumentEvent e) {
            if (filterStringCombo.comboPopupActionFlag) {
                return;
            }

            checkRegExp();
            updateSetClearButtons();
        }

        public void removeUpdate(DocumentEvent e) {
            if (filterStringCombo.comboPopupActionFlag) {
                return;
            }

            checkRegExp();
            updateSetClearButtons();
        }
    }

    private class FilterTextFieldFocusListener implements FocusListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void focusGained(FocusEvent e) {
            doFocusGained();
        }

        public void focusLost(FocusEvent e) {
            doFocusLost();
        }
    }

    private class FilterTextFieldKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(KeyEvent e) {
            filterStringCombo.comboPopupKeyFlag = true;

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                performSetFilterButtonAction();
                filterStringCombo.clearSelection();

                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                filterStringCombo.setText(filterString);
                updateSetClearButtons();

                return;
            }

            if (e.getModifiers() == InputEvent.SHIFT_MASK) {
                if ((e.getKeyCode() == KeyEvent.VK_UP) || (e.getKeyCode() == KeyEvent.VK_DOWN)) {
                    filterTypePopup.show(FilterComponent.this, 0 + 1, FilterComponent.this.getHeight() - 2);

                    return;
                }
            }
        }

        public void keyReleased(KeyEvent e) {
            filterStringCombo.comboPopupKeyFlag = false;
        }
    }

    private class FilterTypeButtonMouseListener extends MouseAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void mousePressed(MouseEvent e) {
            filterTypePopup.show(FilterComponent.this, 0 + 1, FilterComponent.this.getHeight() - 2);
        }
    }

    private class JFilterStringCombo extends JComboBox {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public JTextComponent comboEditor;

        //public final String comboPopupSeparatorString = EMPTY_STRING;

        //private int lastSelectedComboItem;

        //public boolean comboPopupSeparatorSelectedFlag = false;
        public boolean comboPopupActionFlag = false;
        public boolean comboPopupKeyFlag = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public JFilterStringCombo() {
            super();
            comboEditor = (JTextComponent) getEditor().getEditorComponent();
            setRenderer(new FilterStringComboRenderer());
            addPopupMenuListener(new FilterStringComboPopupMenuListener());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setText(String text) {
            comboEditor.setText(text);
        }

        public String getText() {
            return comboEditor.getText();
        }

        public void clearSelection() {
            comboEditor.setSelectionStart(comboEditor.getCaretPosition());
            comboEditor.setSelectionEnd(comboEditor.getCaretPosition());
        }

        /*protected void fireItemStateChanged(ItemEvent e) {
           if ((!comboPopupSeparatorSelectedFlag) && (e.getStateChange() == ItemEvent.SELECTED)) {
             if (e.getItem() == comboPopupSeparatorString) {
               int selectedComboItem = filterStringCombo.getSelectedIndex();
               filterStringCombo.setSelectedIndex(selectedComboItem + (lastSelectedComboItem < selectedComboItem ? 1 : -1));
             }
             lastSelectedComboItem = filterStringCombo.getSelectedIndex();
           }
           super.fireItemStateChanged(e);
           }*/
    }

    private class PopupItemsListener implements ActionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(ActionEvent e) {
            setFilterTypePopupItemActive((JMenuItem) e.getSource(), false);
            filterStringCombo.requestFocus();
        }
    }

    private class SetClearButtonsActionListener implements ActionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == setFilterButton) {
                performSetFilterButtonAction();
                filterStringCombo.requestFocus();

                return;
            }

            if (e.getSource() == clearFilterButton) {
                performClearFilterButtonAction();
                filterStringCombo.requestFocus();

                return;
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.components.Bundle"); // NOI18N
    private static final String DEFAULT_TEXTFIELD_STRING = messages.getString("FilterComponent_DefaultTextFieldString"); // NOI18N
    private static final String INVALID_REGEXP_STRING = messages.getString("FilterComponent_InvalidRegExpString"); // NOI18N
    private static final String FILTER_TYPE_TOOLTIP = messages.getString("FilterComponent_FilterTypeToolTip"); // NOI18N
    private static final String FILTER_VALUE_TOOLTIP = messages.getString("FilterComponent_FilterValueToolTip"); // NOI18N
    private static final String SET_FILTER_BUTTON_TOOLTIP = messages.getString("FilterComponent_SetFilterButtonToolTip"); // NOI18N
    private static final String CLEAR_FILTER_BUTTON_TOOLTIP = messages.getString("FilterComponent_ClearFilterButtonToolTip"); // NOI18N
    private static final String FILTER_STRING_COMBO_ACCESS_NAME = messages.getString("FilterComponent_FilterStringComboAccessName"); // NOI18N
    private static final String FILTER_STRING_COMBO_ACCESS_DESCR = messages.getString("FilterComponent_FilterStringComboAccessDescr"); // NOI18N
    private static final String ACCESS_NAME = messages.getString("FilterComponent_AccessName"); // NOI18N
    private static final String ACCESS_DESCR = messages.getString("FilterComponent_AccessDescr"); // NOI18N
                                                                                                  // -----
    private static final Color DEFAULT_TEXTFIELD_FOREGROUND = Color.GRAY;
    private static final String EMPTY_STRING = ""; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final ImageIcon setFilterIcon = new ImageIcon(getClass()
                                                              .getResource("/org/netbeans/lib/profiler/ui/resources/setFilter.png")); // NOI18N
    private final Icon setFilterRolloverIcon = new ButtonBorderIcon(setFilterIcon);
    private final ImageIcon clearFilterIcon = new ImageIcon(getClass()
                                                                .getResource("/org/netbeans/lib/profiler/ui/resources/clearFilter.png")); // NOI18N
    private final Icon clearFilterRolloverIcon = new ButtonBorderIcon(clearFilterIcon);
    private Color textFieldForeground;
    private JButton clearFilterButton;
    private JButton filterTypeButton;
    private JButton setFilterButton;
    private JFilterStringCombo filterStringCombo;
    private JLabel incorrectRegExpLabel;
    private JMenuItem activeFilterItem;
    private JPanel setClearFilterButtonsPanel;
    private JPanel textFieldRegExpWarningPanel;
    private JPopupMenu filterTypePopup;

    //private Vector tmpBufferComboItems;
    private PopupItemsListener popupItemsListener;
    private String filterString = EMPTY_STRING;
    private String textFieldEmptyText = DEFAULT_TEXTFIELD_STRING;
    private Vector filterNames;

    //private Vector filterStringsBuffers; // Originally used as separate buffers for each filter type
    private Vector filterStringsBuffer; // One buffer for all used filter strings
    private Vector filterTypes;
    private Vector listeners;
    private Vector rolloverIcons;
    private Vector standardIcons;
    private boolean internalChange = false;
    private boolean textFieldEmptyFlag = true;
    private boolean validRegExpFlag = true;
    private int defaultFilterType = CommonConstants.FILTER_CONTAINS;
    private int filterType = CommonConstants.FILTER_NONE;
    private int lastFilterType;
    private int nOwnComboItems = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of FilterComponent */
    public FilterComponent() {
        super();

        listeners = new Vector();

        popupItemsListener = new PopupItemsListener();

        standardIcons = new Vector();
        rolloverIcons = new Vector();
        filterNames = new Vector();
        filterTypes = new Vector();
        //filterStringsBuffers = new Vector();
        filterStringsBuffer = new Vector();

        //tmpBufferComboItems = new Vector();
        lastFilterType = -1;

        initComponents();
        checkRegExp();
        updateSetClearButtons();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getDefaultFilterType() {
        return defaultFilterType;
    }

    public void setEmptyFilterText(String text) {
        if (text.length() == 0) {
            return;
        }

        String oldTextFieldEmptyText = textFieldEmptyText;
        textFieldEmptyText = text;

        if (filterStringCombo.getText().equals(oldTextFieldEmptyText)) {
            filterStringCombo.setText(textFieldEmptyText);
        }
    }

    //--- Public methods -----
    public void setFilterString(String string) {
        setFilterString(string, true);
    }

    public String getFilterString() {
        return filterString;
    }

    public String[] getFilterStrings() {
        return getFilterStrings(filterString);
    }

    public static String[] getFilterStrings(String string) {
        if (string == null) {
            return null;
        }

        return string.trim().split(" +"); // NOI18N
    }

    public void setDefaultFilterType(int type) {
        defaultFilterType = type;
    }

    public void setFilterType(int type) {
        setFilterType(type, false);
    }

    public int getFilterType() {
        return filterType;
    }

    public void setFilterValues(String string, int type) {
        setFilterString(string, false);
        setFilterType(type, false);
        fireFilterChanged();
        updateSetClearButtons();
    }

    //public JMenuItem addFilterItem(ImageIcon standardIcon, ImageIcon rolloverIcon, String filterName, int filterType) {
    public JMenuItem addFilterItem(ImageIcon icon, String filterName, int filterType) {
        Icon standardIcon = icon;
        Icon rolloverIcon = new ButtonBorderIcon(icon);

        standardIcons.add(standardIcon);
        rolloverIcons.add(rolloverIcon);
        filterNames.add(filterName);
        filterTypes.add(new Integer(filterType));

        //filterStringsBuffers.add(new Vector());
        JMenuItem menuItem = new JMenuItem();
        menuItem.setText(filterName);
        menuItem.setIcon(standardIcon);
        menuItem.setBackground(UIUtils.getProfilerResultsBackground());
        menuItem.addActionListener(popupItemsListener);

        filterTypePopup.add(menuItem);
        filterTypePopup.pack();

        setFilterTypePopupItemActive(menuItem, false);

        return menuItem;
    }

    //--- FilterListener interface -----
    public void addFilterListener(FilterListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public JSeparator addSeparatorItem() {
        standardIcons.add(null);
        rolloverIcons.add(null);
        filterNames.add(null);
        filterTypes.add(null);

        //filterStringsBuffers.add(null);
        JPopupMenu.Separator separator = new JPopupMenu.Separator();

        separator.setForeground(Color.BLACK);
        separator.setBackground(Color.WHITE);

        filterTypePopup.add(separator);
        filterTypePopup.pack();

        return separator;
    }

    public void removeFilterListener(FilterListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    private JMenuItem getFilterMenuItemByIndex(int index) {
        if (index < filterTypePopup.getComponentCount()) {
            return (JMenuItem) filterTypePopup.getComponent(index);
        } else {
            return null;
        }
    }

    private void setFilterString(String string, boolean fireChange) {
        if (string == null) {
            return;
        }

        string = string.trim();

        if (!filterString.equals(string)) {
            filterString = string;
            filterStringCombo.setText(filterString);
            internalChange = true;
            addComboBoxItem(filterString);
            internalChange = false;

            if (fireChange) {
                boolean textFieldEmptyFlagBkp = textFieldEmptyFlag;
                textFieldEmptyFlag = false;
                fireFilterChanged();
                checkRegExp();
                updateSetClearButtons();
                textFieldEmptyFlag = textFieldEmptyFlagBkp;
            }

            if (filterString.length() > 0) {
                doFocusGained();
            } else {
                doFocusLost();
            }
        }
    }

    private void setFilterType(int type, boolean fireChange) {
        if (filterType != type) {
            for (int i = 0; i < filterTypes.size(); i++) {
                Integer int_index = (Integer) filterTypes.get(i);

                if ((int_index != null) && (int_index.intValue() == type)) {
                    setFilterTypePopupItemActive(i, fireChange);

                    return;
                }
            }
        }
    }

    private void setFilterTypePopupItemActive(JMenuItem menuItem, boolean fireChange) {
        int index = filterTypePopup.getComponentIndex(menuItem);

        if (index != -1) {
            setFilterTypePopupItemActive(index, fireChange);
        }
    }

    private void setFilterTypePopupItemActive(int index, boolean fireChange) {
        int newFilterType = ((Integer) (filterTypes.get(index))).intValue();

        if (newFilterType != filterType) {
            activeFilterItem = getFilterMenuItemByIndex(index);

            filterType = newFilterType;

            Icon standardIcon = new ArrowSignIcon((ImageIcon) standardIcons.get(index));
            Icon rolloverIcon = new ButtonBorderIcon(standardIcon);
            String filterName = (String) filterNames.get(index);

            filterTypeButton.setIcon(standardIcon);
            filterTypeButton.setRolloverIcon(rolloverIcon);
            filterTypeButton.setToolTipText(MessageFormat.format(FILTER_TYPE_TOOLTIP, new Object[] { filterName }));

            checkRegExp();

            if (fireChange) {
                fireFilterChanged();
            }

            updateSetClearButtons();
            updateComboItems();
        }
    }

    private int getIndexByCurrentFilterType() {
        return getIndexByFilterType(filterType);
    }

    private int getIndexByFilterType(int type) {
        for (int i = 0; i < filterTypes.size(); i++) {
            Integer int_index = (Integer) filterTypes.get(i);

            if ((int_index != null) && (int_index.intValue() == type)) {
                return i;
            }
        }

        return -1;
    }

    private void addComboBoxItem(String string) {
        if ((string == null) || (string.length() == 0)) {
            return;
        }

        //Vector filterStringsBuffer = (Vector)filterStringsBuffers.get(getIndexByCurrentFilterType());
        if (filterStringsBuffer.contains(string)) {
            filterStringsBuffer.remove(string);
        }

        filterStringsBuffer.add(string);
        updateComboItems();
    }

    private void checkRegExp() {
        if ((filterType != CommonConstants.FILTER_REGEXP) || textFieldEmptyFlag) {
            validRegExpFlag = true;
        } else {
            try {
                String[] filters = getFilterStrings(filterStringCombo.getText());

                for (String filter : filters) {
                    EMPTY_STRING.matches(filter);
                }

                validRegExpFlag = true;
            } catch (java.util.regex.PatternSyntaxException e) {
                validRegExpFlag = false;
            }
        }
    }

    private void doFocusGained() {
        textFieldEmptyFlag = false;
        filterStringCombo.comboEditor.setForeground(textFieldForeground);

        if (filterStringCombo.getText().equals(textFieldEmptyText)) {
            filterStringCombo.setText(""); // NOI18N
        }
    }

    private void doFocusLost() {
        if ((filterStringCombo.getText().length() == 0) && (filterString.length() == 0)) {
            textFieldEmptyFlag = true;
            filterStringCombo.setText(textFieldEmptyText);
            filterStringCombo.comboEditor.setForeground(DEFAULT_TEXTFIELD_FOREGROUND);
        }
    }

    private void fireFilterChanged() {
        if (validRegExpFlag) {
            for (int i = 0; i < listeners.size(); i++) {
                ((FilterListener) (listeners.elementAt(i))).filterChanged();
            }
        }

        lastFilterType = filterType;
    }

    //--- Private implementation -----
    private void initComponents() {
        Border componentBorder;
        Color textFieldBackground;

        ActionListener setClearButtonsActionListner = new SetClearButtonsActionListener();

        if (UIUtils.isMetalLookAndFeel()) {
            // force smaller combobox
            filterStringCombo = new JFilterStringCombo() {
                    public Dimension getMaximumSize() {
                        return new Dimension(super.getMaximumSize().width, comboEditor.getPreferredSize().height);
                    }
                    ;
                    public Dimension getPreferredSize() {
                        return new Dimension(super.getPreferredSize().width, comboEditor.getPreferredSize().height);
                    }
                    ;
                    public Dimension getMinimumSize() {
                        return new Dimension(super.getMinimumSize().width, comboEditor.getPreferredSize().height);
                    }
                    ;
                };
        } else {
            filterStringCombo = new JFilterStringCombo();
        }

        filterStringCombo.setEditable(true);
        componentBorder = UIUtils.isGTKLookAndFeel() || UIUtils.isNimbusLookAndFeel() ? BorderFactory.createBevelBorder(BevelBorder.LOWERED) :
            filterStringCombo.getBorder();
        filterStringCombo.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 0));
        filterStringCombo.setMaximumRowCount(7);
        filterStringCombo.addActionListener(new FilterStringComboActionListener());
        filterStringCombo.getAccessibleContext().setAccessibleName(FILTER_STRING_COMBO_ACCESS_NAME);
        filterStringCombo.getAccessibleContext().setAccessibleDescription(FILTER_STRING_COMBO_ACCESS_DESCR);
        filterStringCombo.setToolTipText(FILTER_VALUE_TOOLTIP);

        JTextComponent filterStringComboEditor = filterStringCombo.comboEditor;

        textFieldBackground = filterStringComboEditor.getBackground();
        textFieldForeground = filterStringComboEditor.getForeground();

        filterStringCombo.setBackground(textFieldBackground);

        filterStringComboEditor.setForeground(DEFAULT_TEXTFIELD_FOREGROUND);
        filterStringComboEditor.setText(textFieldEmptyText);
        filterStringComboEditor.getDocument().addDocumentListener(new FilterTextFieldDocumentListener());
        filterStringComboEditor.addKeyListener(new FilterTextFieldKeyListener());
        filterStringComboEditor.addFocusListener(new FilterTextFieldFocusListener());

        filterTypeButton = new JButton(""); // NOI18N
        filterTypeButton.setFocusable(false);
        filterTypeButton.setBorder(BorderFactory.createEmptyBorder());
        filterTypeButton.setBackground(textFieldBackground);
        filterTypeButton.setContentAreaFilled(false);
        filterTypeButton.addMouseListener(new FilterTypeButtonMouseListener());

        incorrectRegExpLabel = new JLabel(INVALID_REGEXP_STRING);
        incorrectRegExpLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        incorrectRegExpLabel.setBackground(textFieldBackground);
        incorrectRegExpLabel.setForeground(Color.RED);

        textFieldRegExpWarningPanel = new JPanel(new BorderLayout(0, 0));
        textFieldRegExpWarningPanel.setBackground(textFieldBackground);
        textFieldRegExpWarningPanel.add(filterStringCombo, BorderLayout.CENTER);
        textFieldRegExpWarningPanel.add(incorrectRegExpLabel, BorderLayout.EAST);

        setFilterButton = new JButton(""); // NOI18N
        setFilterButton.setIcon(setFilterIcon);
        setFilterButton.setRolloverIcon(setFilterRolloverIcon);
        setFilterButton.setFocusable(false);
        setFilterButton.setBorder(BorderFactory.createEmptyBorder());
        setFilterButton.setBackground(textFieldBackground);
        setFilterButton.setContentAreaFilled(false);
        setFilterButton.addActionListener(setClearButtonsActionListner);
        setFilterButton.setToolTipText(SET_FILTER_BUTTON_TOOLTIP);

        clearFilterButton = new JButton(""); // NOI18N
        clearFilterButton.setIcon(clearFilterIcon);
        clearFilterButton.setRolloverIcon(clearFilterRolloverIcon);
        clearFilterButton.setFocusable(false);
        clearFilterButton.setBorder(BorderFactory.createEmptyBorder());
        clearFilterButton.setBackground(textFieldBackground);
        clearFilterButton.setContentAreaFilled(false);
        clearFilterButton.addActionListener(setClearButtonsActionListner);
        clearFilterButton.setToolTipText(CLEAR_FILTER_BUTTON_TOOLTIP);

        setClearFilterButtonsPanel = new JPanel(new BorderLayout(0, 0));
        setClearFilterButtonsPanel.setBackground(textFieldBackground);
        setClearFilterButtonsPanel.add(setFilterButton, BorderLayout.WEST);
        setClearFilterButtonsPanel.add(clearFilterButton, BorderLayout.EAST);

        filterTypePopup = new JPopupMenu() {
                public void setVisible(boolean visible) {
                    super.setVisible(visible);

                    if (visible) {
                        MenuElement[] me;

                        if (activeFilterItem != null) {
                            me = new MenuElement[] { this, activeFilterItem };
                        } else {
                            me = new MenuElement[] { this };
                        }

                        MenuSelectionManager.defaultManager().setSelectedPath(me);
                    }
                }
            };
        filterTypePopup.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        filterTypePopup.setBackground(UIUtils.getProfilerResultsBackground());

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(componentBorder, BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        setBackground(textFieldBackground);

        add(filterTypeButton, BorderLayout.WEST);
        add(textFieldRegExpWarningPanel, BorderLayout.CENTER);
        add(setClearFilterButtonsPanel, BorderLayout.EAST);

        getAccessibleContext().setAccessibleName(ACCESS_NAME);
        getAccessibleContext().setAccessibleDescription(ACCESS_DESCR);
    }

    private void performClearFilterButtonAction() {
        filterString = EMPTY_STRING;
        filterStringCombo.setText(filterString);
        setFilterType(defaultFilterType, false);
        fireFilterChanged();
        updateSetClearButtons();
    }

    private void performSetFilterButtonAction() {
        filterStringCombo.setText(filterStringCombo.getText().trim());

        String newFilterString = filterStringCombo.getText();

        if ((!newFilterString.equals(filterString)) || (lastFilterType != filterType)) {
            filterString = newFilterString;
            addComboBoxItem(filterString);
            fireFilterChanged();
            updateSetClearButtons();
        }
    }

    /*private void addFilterStringsBufferComboItems(Vector filterStringsBuffer) {
       for (int i = filterStringsBuffer.size() - 1; i >= 0; i--) {
         if (!tmpBufferComboItems.contains(filterStringsBuffer.get(i))) {
           Object string = filterStringsBuffer.get(i);
           filterStringCombo.addItem(string);
           tmpBufferComboItems.add(string);
         }
       }
       }*/
    private void updateComboItems() {
        String currentFilterString = filterStringCombo.getText();
        filterStringCombo.removeAllItems();

        for (int i = filterStringsBuffer.size() - 1; i >= 0; i--) {
            filterStringCombo.addItem(filterStringsBuffer.get(i));
        }

        /*int index = getIndexByCurrentFilterType();
           Vector ownFilterStringsBuffer;
           Vector filterStringsBuffer;
        
           ownFilterStringsBuffer = (Vector)filterStringsBuffers.get(index);
           nOwnComboItems = ownFilterStringsBuffer.size();
           if (nOwnComboItems > 0) {
             addFilterStringsBufferComboItems(ownFilterStringsBuffer);
           }
        
           for (int i = 0; i < filterStringsBuffers.size(); i++) {
             filterStringsBuffer = (Vector)filterStringsBuffers.get(i);
             if ((filterStringsBuffer != null) && (i != index)) {
               addFilterStringsBufferComboItems(filterStringsBuffer);
             }
           }
        
           if ((nOwnComboItems > 0) && (filterStringCombo.getItemCount() > nOwnComboItems)) filterStringCombo.insertItemAt(filterStringCombo.comboPopupSeparatorString, nOwnComboItems);*/
        filterStringCombo.setText(currentFilterString);

        //tmpBufferComboItems.clear();
    }

    private void updateSetClearButtons() {
        if (validRegExpFlag) {
            if (incorrectRegExpLabel.isVisible()) {
                incorrectRegExpLabel.setVisible(false);
            }
        } else {
            if (!incorrectRegExpLabel.isVisible()) {
                incorrectRegExpLabel.setVisible(true);
            }

            if (clearFilterButton.isVisible()) {
                clearFilterButton.setVisible(false);
            }

            if (setFilterButton.isVisible()) {
                setFilterButton.setVisible(false);
            }

            updateSetClearFilterButtonsPanelBorder();

            return;
        }

        if (textFieldEmptyFlag) {
            if (clearFilterButton.isVisible()) {
                clearFilterButton.setVisible(false);
            }

            if (setFilterButton.isVisible()) {
                setFilterButton.setVisible(false);
            }

            updateSetClearFilterButtonsPanelBorder();

            return;
        }

        // clearFilterButton
        if (filterString.length() == 0) {
            if (clearFilterButton.isVisible()) {
                clearFilterButton.setVisible(false);
            }
        } else {
            if (!clearFilterButton.isVisible()) {
                clearFilterButton.setVisible(true);
            }
        }

        // setFilterButton
        if (((filterStringCombo.getText().equals(filterString)) && (lastFilterType == filterType))
                || (filterStringCombo.getText().length() == 0)) {
            if (setFilterButton.isVisible()) {
                setFilterButton.setVisible(false);
            }
        } else {
            if (!setFilterButton.isVisible()) {
                if (clearFilterButton.isVisible()) {
                    ((BorderLayout) (setClearFilterButtonsPanel.getLayout())).setHgap(1);
                } else {
                    ((BorderLayout) (setClearFilterButtonsPanel.getLayout())).setHgap(0);
                }

                setFilterButton.setVisible(true);
            }
        }

        updateSetClearFilterButtonsPanelBorder();
    }

    private void updateSetClearFilterButtonsPanelBorder() {
        if ((clearFilterButton.isVisible()) || (setFilterButton.isVisible())) {
            setClearFilterButtonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        } else {
            setClearFilterButtonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }
    }
}
