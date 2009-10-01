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

package org.netbeans.modules.profiler.ui;

import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 *
 * @author Tomas Hurka
 * @author  Jiri Sedlacek
 */
public final class QuickFilterPanel extends JPanel implements HelpCtx.Provider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private final class FilterValueTextFieldDocumentListener implements DocumentListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void changedUpdate(final DocumentEvent e) {
            checkFilterValue();
        }

        public void insertUpdate(final DocumentEvent e) {
            checkFilterValue();
        }

        public void removeUpdate(final DocumentEvent e) {
            checkFilterValue();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String FILTER_TYPE_LABEL_TEXT = NbBundle.getMessage(QuickFilterPanel.class,
                                                                             "QuickFilterPanel_FilterTypeLabelText"); //NOI18N
    private static final String FILTER_TYPE_EXCLUSIVE_RADIO_TEXT = NbBundle.getMessage(QuickFilterPanel.class,
                                                                                       "QuickFilterPanel_FilterTypeExclusiveRadioText"); //NOI18N
    private static final String FILTER_TYPE_INCLUSIVE_RADIO_TEXT = NbBundle.getMessage(QuickFilterPanel.class,
                                                                                       "QuickFilterPanel_FilterTypeInclusiveRadioText"); //NOI18N
    private static final String FILTER_VALUE_LABEL_TEXT = NbBundle.getMessage(QuickFilterPanel.class,
                                                                              "QuickFilterPanel_FilterValueLabelText"); //NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class, "QuickFilterPanel_OkButtonText"); //NOI18N
    private static final String CANCEL_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                         "QuickFilterPanel_CancelButtonText"); //NOI18N
    private static final String EMPTY_FILTER_MSG = NbBundle.getMessage(GlobalFiltersPanel.class, "QuickFilterPanel_EmptyFilterMsg"); //NOI18N
    private static final String INVALID_FILTER_MSG = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                         "QuickFilterPanel_InvalidFilterMsg"); //NOI18N
    private static final String HINT_MSG = NbBundle.getMessage(GlobalFiltersPanel.class, "QuickFilterPanel_HintMsg"); //NOI18N
    private static final String FILTER_TYPE_EXCLUSIVE_RADIO_ACCESS_DESCR = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                                               "QuickFilterPanel_FilterTypeExclusiveRadioAccessDescr"); //NOI18N
    private static final String FILTER_TYPE_INCLUSIVE_RADIO_ACCESS_DESCR = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                                               "QuickFilterPanel_FilterTypeInclusiveRadioAccessDescr"); //NOI18N
    private static final String FILTER_VALUE_TEXT_FIELD_ACCESS_NAME = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                                          "QuickFilterPanel_FilterValueTextFieldAccessName"); //NOI18N
                                                                                                                                              // -----
    private static final String HELP_CTX_KEY = "QuickFilterPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static QuickFilterPanel defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ButtonGroup filterTypeButtonGroup;
    private HTMLTextArea hintArea;
    private JButton CancelButton;
    private JButton OKButton;
    private JLabel filterTypeLabel;
    private JLabel filterValueHintLabel;
    private JLabel filterValueLabel;
    private JRadioButton filterTypeExclusiveRadio;
    private JRadioButton filterTypeInclusiveRadio;
    private JTextArea filterValueTextArea;
    private SimpleFilter quickFilter;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of QuickFilterPanel */
    private QuickFilterPanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static QuickFilterPanel getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new QuickFilterPanel();
        }

        return defaultInstance;
    }

    public JButton getCancelButton() {
        return CancelButton;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public JButton getOKButton() {
        return OKButton;
    }

    public void applyChanges() {
        if (filterTypeExclusiveRadio.isSelected()) {
            quickFilter.setFilterType(SimpleFilter.SIMPLE_FILTER_EXCLUSIVE);
        } else {
            quickFilter.setFilterType(SimpleFilter.SIMPLE_FILTER_INCLUSIVE);
        }

        quickFilter.setFilterValue(getFilterValueInternal());
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public void init() {
        init(FilterUtils.QUICK_FILTER);
    }

    public void init(SimpleFilter quickFilter) {
        this.quickFilter = quickFilter;

        if ((filterTypeExclusiveRadio != null) && (filterTypeInclusiveRadio != null)) {
            if (quickFilter.getFilterType() == SimpleFilter.SIMPLE_FILTER_EXCLUSIVE) {
                filterTypeExclusiveRadio.setSelected(true);
            } else {
                filterTypeInclusiveRadio.setSelected(true);
            }
        }

        if (filterTypeLabel != null) {
            String[] filterParts = FilterUtils.getSeparateFilters(quickFilter.getFilterValue());
            java.util.List<String> filterPartsList = new ArrayList(filterParts.length);

            for (String filterPart : filterParts) {
                filterPartsList.add(filterPart);
            }

            Collections.sort(filterPartsList);

            StringBuffer val = new StringBuffer(filterParts.length);
            Iterator<String> it = filterPartsList.iterator();

            while (it.hasNext()) {
                val.append(it.next());

                if (it.hasNext()) {
                    val.append("\n"); // NOI18N
                }
            }

            filterValueTextArea.setText(val.toString());
            filterValueTextArea.setCaretPosition(0);
            filterValueTextArea.requestFocus();
        }

        checkFilterValue();
    }

    private boolean isFilterValid() {
        String[] filterParts = getFilterValues();

        for (int i = 0; i < filterParts.length; i++) {
            if (!FilterUtils.isValidProfilerFilter(filterParts[i])) {
                return false;
            }
        }

        return true;
    }

    // Converts JTextArea text delimited by \n to FilterUtils text delimited by ,
    private String getFilterValueInternal() {
        StringBuffer convertedValue = new StringBuffer();

        String[] filterValues = getFilterValues();

        for (int i = 0; i < filterValues.length; i++) {
            String filterValue = filterValues[i].trim();

            if ((i != (filterValues.length - 1)) && !filterValue.endsWith(",")) {
                filterValue = filterValue + ", "; // NOI18N
            }

            convertedValue.append(filterValue);
        }

        return convertedValue.toString();
    }

    private String[] getFilterValues() {
        return filterValueTextArea.getText().split("\\n"); // NOI18N
    }

    private void checkFilterValue() {
        if (filterValueTextArea == null) {
            return;
        }

        getFilterValues();

        String filterValue = filterValueTextArea.getText().trim();

        if (filterValue.length() == 0) {
            filterValueHintLabel.setText(EMPTY_FILTER_MSG);
            OKButton.setEnabled(false);
            filterValueTextArea.setForeground(Color.red);
            filterValueTextArea.setSelectedTextColor(Color.red);
        } else if (!isFilterValid()) {
            filterValueHintLabel.setText(INVALID_FILTER_MSG);
            OKButton.setEnabled(false);
            filterValueTextArea.setForeground(Color.red);
            filterValueTextArea.setSelectedTextColor(Color.red);
        } else {
            filterValueHintLabel.setText(" "); // NOI18N
            OKButton.setEnabled(true);
            filterValueTextArea.setForeground(UIManager.getColor("Label.foreground")); // NOI18N
            filterValueTextArea.setSelectedTextColor(UIManager.getColor("Label.foreground")); // NOI18N
        }
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        // buttons to export
        OKButton = new JButton(OK_BUTTON_TEXT);
        CancelButton = new JButton(CANCEL_BUTTON_TEXT);

        filterTypeLabel = new JLabel();
        filterValueLabel = new JLabel();
        filterValueHintLabel = new JLabel();
        filterTypeButtonGroup = new ButtonGroup();
        filterTypeExclusiveRadio = new JRadioButton();
        filterTypeInclusiveRadio = new JRadioButton();
        filterValueTextArea = new JTextArea();
        hintArea = new HTMLTextArea() {
                public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                    return new Dimension(1, super.getPreferredSize().height);
                }
            };

        setLayout(new GridBagLayout());

        // filterTypeLabel
        filterTypeLabel.setText(FILTER_TYPE_LABEL_TEXT);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(15, 10, 5, 15);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(filterTypeLabel, gridBagConstraints);

        // filterTypeExclusiveRadio
        org.openide.awt.Mnemonics.setLocalizedText(filterTypeExclusiveRadio, FILTER_TYPE_EXCLUSIVE_RADIO_TEXT);
        filterTypeExclusiveRadio.getAccessibleContext().setAccessibleDescription(FILTER_TYPE_EXCLUSIVE_RADIO_ACCESS_DESCR);
        filterTypeButtonGroup.add(filterTypeExclusiveRadio);

        // filterTypeInclusiveRadio
        org.openide.awt.Mnemonics.setLocalizedText(filterTypeInclusiveRadio, FILTER_TYPE_INCLUSIVE_RADIO_TEXT);
        filterTypeInclusiveRadio.getAccessibleContext().setAccessibleDescription(FILTER_TYPE_INCLUSIVE_RADIO_ACCESS_DESCR);
        filterTypeInclusiveRadio.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        filterTypeButtonGroup.add(filterTypeInclusiveRadio);

        // filterRadiosPanel
        final JPanel filterRadiosPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        filterRadiosPanel.add(filterTypeExclusiveRadio);
        filterRadiosPanel.add(filterTypeInclusiveRadio);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 15);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(filterRadiosPanel, gridBagConstraints);

        // filterValueLabel
        org.openide.awt.Mnemonics.setLocalizedText(filterValueLabel, FILTER_VALUE_LABEL_TEXT);
        filterValueLabel.setLabelFor(filterValueTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(filterValueLabel, gridBagConstraints);

        // filterValueTextArea
        filterValueTextArea.getAccessibleContext().setAccessibleName(FILTER_VALUE_TEXT_FIELD_ACCESS_NAME);
        filterValueTextArea.setSelectionColor(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        filterValueTextArea.setSelectedTextColor(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        filterValueTextArea.getDocument().addDocumentListener(new FilterValueTextFieldDocumentListener());

        JTextArea temp = new JTextArea();
        temp.setColumns(45);
        temp.setRows(6);

        JScrollPane filterValueScrollPane = new JScrollPane(filterValueTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        filterValueScrollPane.setPreferredSize(new Dimension(temp.getPreferredSize().width, temp.getPreferredSize().height));
        temp = null;

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 10);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(filterValueScrollPane, gridBagConstraints);

        // filterValueHintLabel
        filterValueHintLabel.setText(" "); // NOI18N
        filterValueHintLabel.setForeground(new Color(89, 79, 191)); // the same as nb wizard error message
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(7, 5, 0, 10);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(filterValueHintLabel, gridBagConstraints);

        // panel filling bottom space
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(new JPanel(), gridBagConstraints);

        Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
        Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                                                    panelBackground.getBlue() - 10);

        // hintArea
        hintArea.setText(HINT_MSG); // NOI18N
        hintArea.setEnabled(false);
        hintArea.setDisabledTextColor(Color.darkGray);
        hintArea.setBackground(hintBackground);
        hintArea.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(5, 7, 0, 7);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(hintArea, gridBagConstraints);

        checkFilterValue();
    }

    /**
     * @param args the command line arguments
     */

    /*  public static void main (String[] args) {
       try {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("plaf.metal.MetalLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //NOI18N
       } catch (Exception e){};
       JFrame frame = new JFrame("FilterSetsPanel Viewer"); //NOI18N
       frame.getContentPane().add(new QuickFilterPanel());
       frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
       frame.pack();
       frame.show();
       }
     */
}
