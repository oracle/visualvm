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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.modules.profiler.ppoints.TimedGlobalProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ui.ValidityAwarePanel;
import org.openide.util.NbBundle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class TimeCustomizer extends ValidityAwarePanel implements ActionListener, ChangeListener, DocumentListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String UNITS_MINUTES = NbBundle.getMessage(TimeCustomizer.class, "TimeCustomizer_UnitsMinutes"); // NOI18N
    private static final String UNITS_HOURS = NbBundle.getMessage(TimeCustomizer.class, "TimeCustomizer_UnitsHours"); // NOI18N
    private static final String TAKE_AT_LABEL_TEXT = NbBundle.getMessage(TimeCustomizer.class, "TimeCustomizer_TakeAtLabelText"); // NOI18N
    private static final String NOW_BUTTON_TEXT = NbBundle.getMessage(TimeCustomizer.class, "TimeCustomizer_NowButtonText"); // NOI18N
    private static final String TODAY_BUTTON_TEXT = NbBundle.getMessage(TimeCustomizer.class, "TimeCustomizer_TodayButtonText"); // NOI18N
    private static final String TAKE_ONCE_RADIO_TEXT = NbBundle.getMessage(TimeCustomizer.class,
                                                                           "TimeCustomizer_TakeOnceRadioText"); // NOI18N
    private static final String TAKE_EVERY_RADIO_TEXT = NbBundle.getMessage(TimeCustomizer.class,
                                                                            "TimeCustomizer_TakeEveryRadioText"); // NOI18N
    private static final String TIME_FIELD_ACCESS_NAME = NbBundle.getMessage(TimeCustomizer.class,
                                                                             "TimeCustomizer_TimeFieldAccessName"); // NOI18N
    private static final String DATE_FIELD_ACCESS_NAME = NbBundle.getMessage(TimeCustomizer.class,
                                                                             "TimeCustomizer_DateFieldAccessName"); // NOI18N
    private static final String FREQUENCY_COMBO_ACCESS_NAME = NbBundle.getMessage(TimeCustomizer.class,
                                                                                  "TimeCustomizer_FrequencyComboAccessName"); // NOI18N
                                                                                                                              // -----
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();

    // --- Implementation --------------------------------------------------------
    private static int defaultTextComponentHeight = -1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Color TEXT_FOREGROUND;
    private JButton timeTakeAtButton;
    private JButton timeTakeAtDateButton;
    private JComboBox timeFrequencyCombo;
    private JLabel timeTakeAtLabel;
    private JPanel firstLineCaptionSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private JPanel secondLineCaptionSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private JRadioButton timeFrequencyRadio;
    private JRadioButton timeOnceRadio;
    private JSpinner timeFrequencySpinner;
    private JTextField timeTakeAtDateField;
    private JTextField timeTakeAtField;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TimeCustomizer() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getPreferredCaptionAreaWidth() {
        // Not used for this customizer
        return -1;
    }

    public void setTimeCondition(TimedGlobalProfilingPoint.TimeCondition condition) {
        timeTakeAtField.setText(TIME_FORMAT.format(new Date(condition.getStartTime())));
        timeTakeAtDateField.setText(DATE_FORMAT.format(new Date(condition.getStartTime())));
        timeOnceRadio.setSelected(!condition.getRepeats());
        timeFrequencyRadio.setSelected(condition.getRepeats());
        timeFrequencySpinner.setValue(condition.getPeriodTime());

        switch (condition.getPeriodUnits()) {
            case TimedGlobalProfilingPoint.TimeCondition.UNITS_MINUTES:
                timeFrequencyCombo.setSelectedItem(UNITS_MINUTES);

                break;
            case TimedGlobalProfilingPoint.TimeCondition.UNITS_HOURS:
                timeFrequencyCombo.setSelectedItem(UNITS_HOURS);

                break;
            default:
                break;
        }
    }

    public TimedGlobalProfilingPoint.TimeCondition getTimeCondition() {
        TimedGlobalProfilingPoint.TimeCondition condition = new TimedGlobalProfilingPoint.TimeCondition();

        Date time = null;

        try {
            time = TIME_FORMAT.parse(timeTakeAtField.getText());
        } catch (ParseException ex) {
        }

        Date date = null;

        try {
            date = DATE_FORMAT.parse(timeTakeAtDateField.getText());
        } catch (ParseException ex) {
        }

        if ((time == null) || (date == null)) {
            return null;
        }

        condition.setStartTime(time.getTime() + date.getTime() + Calendar.getInstance().get(Calendar.ZONE_OFFSET));
        condition.setRepeats(timeFrequencyRadio.isSelected());
        condition.setPeriodTime(((Integer) timeFrequencySpinner.getValue()).intValue());

        if (timeFrequencyCombo.getSelectedItem() == UNITS_MINUTES) {
            condition.setPeriodUnits(TimedGlobalProfilingPoint.TimeCondition.UNITS_MINUTES);
        } else if (timeFrequencyCombo.getSelectedItem() == UNITS_HOURS) {
            condition.setPeriodUnits(TimedGlobalProfilingPoint.TimeCondition.UNITS_HOURS);
        }

        return condition;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == timeTakeAtButton) {
            timeTakeAtField.setText(TIME_FORMAT.format(new Date(System.currentTimeMillis())));
        } else if (e.getSource() == timeTakeAtDateButton) {
            timeTakeAtDateField.setText(DATE_FORMAT.format(new Date(System.currentTimeMillis())));
        }
    }

    public void changedUpdate(DocumentEvent e) {
        updateValidity();
    }

    public void insertUpdate(DocumentEvent e) {
        updateValidity();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("plaf.metal.MetalLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //NOI18N
        } catch (Exception e) {
        }

        ;

        TimeCustomizer main = new TimeCustomizer();

        //    main.addValidityListener(new ValidityListener() {
        //      public void validityChanged(boolean isValid) { System.err.println(">>> Validity changed to " + isValid); }
        //    });
        JFrame frame = new JFrame("Customize Profiling Point"); //NOI18N
        frame.getContentPane().add(main);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void normalizeCaptionAreaWidth() {
        // Not used for this customizer
    }

    public void removeUpdate(DocumentEvent e) {
        updateValidity();
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == timeFrequencyRadio) {
            timeFrequencySpinner.setEnabled(timeFrequencyRadio.isSelected());
            timeFrequencyCombo.setEnabled(timeFrequencyRadio.isSelected());
        }
    }

    private boolean checkDate() {
        try {
            Date date = DATE_FORMAT.parse(timeTakeAtDateField.getText());
            timeTakeAtDateField.setForeground(TEXT_FOREGROUND);

            return true;
        } catch (ParseException ex) {
            timeTakeAtDateField.setForeground(Color.RED);

            return false;
        }
    }

    private boolean checkTime() {
        try {
            Date time = TIME_FORMAT.parse(timeTakeAtField.getText());
            timeTakeAtField.setForeground(TEXT_FOREGROUND);

            return true;
        } catch (ParseException ex) {
            timeTakeAtField.setForeground(Color.RED);

            return false;
        }
    }

    //  private int getDefaultTextComponentHeight() {
    //    if (defaultTextComponentHeight == -1) defaultTextComponentHeight = new JComboBox().getPreferredSize().height;
    //    return defaultTextComponentHeight;
    //  }
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        JPanel timeSettingsContainer = new JPanel(new GridBagLayout());

        // timeTakeAtLabel
        timeTakeAtLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(timeTakeAtLabel, TAKE_AT_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        timeSettingsContainer.add(timeTakeAtLabel, constraints);

        // timeTakeAtField
        timeTakeAtField = new JTextField() {
                public Dimension getPreferredSize() {
                    return new Dimension(100, super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        timeTakeAtField.getAccessibleContext().setAccessibleName(TIME_FIELD_ACCESS_NAME);
        timeTakeAtLabel.setLabelFor(timeTakeAtField);
        timeTakeAtField.getDocument().addDocumentListener(this);
        timeTakeAtField.setHorizontalAlignment(JTextField.TRAILING);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        timeSettingsContainer.add(timeTakeAtField, constraints);

        // timeTakeAtButton
        timeTakeAtButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(timeTakeAtButton, NOW_BUTTON_TEXT);
        timeTakeAtButton.addActionListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 8);
        timeSettingsContainer.add(timeTakeAtButton, constraints);

        // timeTakeAtDateField
        timeTakeAtDateField = new JTextField() {
                public Dimension getPreferredSize() {
                    return new Dimension(100, super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        timeTakeAtDateField.getAccessibleContext().setAccessibleName(DATE_FIELD_ACCESS_NAME);
        timeTakeAtDateField.getDocument().addDocumentListener(this);
        timeTakeAtDateField.setHorizontalAlignment(JTextField.TRAILING);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        timeSettingsContainer.add(timeTakeAtDateField, constraints);

        // timeTakeAtDateButton
        timeTakeAtDateButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(timeTakeAtDateButton, TODAY_BUTTON_TEXT);
        timeTakeAtDateButton.addActionListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        timeSettingsContainer.add(timeTakeAtDateButton, constraints);

        JPanel triggerFillerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 5;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        timeSettingsContainer.add(triggerFillerPanel, constraints);

        // timeSettingsContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 5, 0);
        add(timeSettingsContainer, constraints);

        // --- next row ----------------------------------------------------------
        ButtonGroup triggerRadiosGroup = new ButtonGroup();
        JPanel triggerRadiosContainer = new JPanel(new GridBagLayout());

        // timeOnceRadio
        timeOnceRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(timeOnceRadio, TAKE_ONCE_RADIO_TEXT);
        triggerRadiosGroup.add(timeOnceRadio);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerRadiosContainer.add(timeOnceRadio, constraints);

        // timeFrequencyRadio
        timeFrequencyRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(timeFrequencyRadio, TAKE_EVERY_RADIO_TEXT);
        triggerRadiosGroup.add(timeFrequencyRadio);
        timeFrequencyRadio.setSelected(true);
        timeFrequencyRadio.addChangeListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerRadiosContainer.add(timeFrequencyRadio, constraints);

        // timeFrequencySpinner
        timeFrequencySpinner = new JExtendedSpinner(new SpinnerNumberModel(1, 1, 9999, 1)) {
                public Dimension getPreferredSize() {
                    return new Dimension(Math.max(super.getPreferredSize().width, 55),
                                         org.netbeans.modules.profiler.ui.stp.Utils.getDefaultSpinnerHeight());
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerRadiosContainer.add(timeFrequencySpinner, constraints);

        // timeFrequencyCombo
        timeFrequencyCombo = new JComboBox(new Object[] { UNITS_MINUTES, UNITS_HOURS }) {
                public Dimension getPreferredSize() {
                    return new Dimension(Math.min(super.getPreferredSize().width, 200), super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        timeFrequencyCombo.getAccessibleContext().setAccessibleName(FREQUENCY_COMBO_ACCESS_NAME);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        triggerRadiosContainer.add(timeFrequencyCombo, constraints);

        JPanel takeRadiosSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        triggerRadiosContainer.add(takeRadiosSpacer, constraints);

        // takeRadiosContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(triggerRadiosContainer, constraints);

        // --- next row ----------------------------------------------------------
        JPanel fillerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(fillerPanel, constraints);

        // --- UI tweaks -------------------------------------------------------
        TEXT_FOREGROUND = timeTakeAtDateField.getForeground();
    }

    private void updateValidity() {
        boolean isTimeValid = checkTime();
        boolean isDateValid = checkDate();
        boolean isValid = isTimeValid && isDateValid;

        if (isValid != TimeCustomizer.this.areSettingsValid()) {
            fireValidityChanged(isValid);
        }
    }
}
