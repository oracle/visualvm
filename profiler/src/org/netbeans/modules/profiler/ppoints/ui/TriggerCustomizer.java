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
import org.netbeans.modules.profiler.ppoints.TriggeredGlobalProfilingPoint;
import org.openide.util.NbBundle;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class TriggerCustomizer extends ValidityAwarePanel implements ActionListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String HEAP_USG_REL_KEY = NbBundle.getMessage(TriggerCustomizer.class, "TriggerCustomizer_HeapUsgRelKey"); // NOI18N
    private static final String HEAP_USG_REL_UNIT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                        "TriggerCustomizer_HeapUsgRelUnit"); // NOI18N
    private static final String HEAP_SIZE_ABS_KEY = NbBundle.getMessage(TriggerCustomizer.class,
                                                                        "TriggerCustomizer_HeapSizeAbsKey"); // NOI18N
    private static final String HEAP_SIZE_ABS_UNIT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                         "TriggerCustomizer_HeapSizeAbsUnit"); // NOI18N
    private static final String SURVGEN_COUNT_KEY = NbBundle.getMessage(TriggerCustomizer.class,
                                                                        "TriggerCustomizer_SurvgenCountKey"); // NOI18N
    private static final String SURVGEN_COUNT_UNIT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                         "TriggerCustomizer_SurvgenCountUnit"); // NOI18N
    private static final String LDCLASS_COUNT_KEY = NbBundle.getMessage(TriggerCustomizer.class,
                                                                        "TriggerCustomizer_LdClassCountKey"); // NOI18N
    private static final String LDCLASS_COUNT_UNIT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                         "TriggerCustomizer_LdClassCountUnit"); // NOI18N
    private static final String TAKE_WHEN_LABEL_TEXT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                           "TriggerCustomizer_TakeWhenLabelText"); // NOI18N
    private static final String EXCEEDS_LABEL_TEXT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                         "TriggerCustomizer_ExceedsLabelText"); // NOI18N
    private static final String TAKE_ONCE_RADIO_TEXT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                           "TriggerCustomizer_TakeOnceRadioText"); // NOI18N
    private static final String TAKE_ALWAYS_RADIO_TEXT = NbBundle.getMessage(TriggerCustomizer.class,
                                                                             "TriggerCustomizer_TakeAlwaysRadioText"); // NOI18N
                                                                                                                       // -----
    private static int defaultTextComponentHeight = -1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final SpinnerModel percentsModel = new SpinnerNumberModel(1, 1, 99, 1);

    // --- Implementation --------------------------------------------------------
    private final SpinnerModel unitsModel = new SpinnerNumberModel(1, 1, 9999, 1);
    private JComboBox triggerWhenCombo;
    private JLabel triggerExceedsLabel;
    private JLabel triggerGenerationsLabel;
    private JLabel triggerWhenLabel;
    private JRadioButton triggerAlwaysRadio;
    private JRadioButton triggerOnceRadio;
    private JSpinner triggerValueSpinner;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TriggerCustomizer() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getPreferredCaptionAreaWidth() {
        // Not used for this customizer
        return -1;
    }

    public void setTriggerCondition(TriggeredGlobalProfilingPoint.TriggerCondition condition) {
        switch (condition.getMetric()) {
            case TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPUSG:
                triggerWhenCombo.setSelectedItem(HEAP_USG_REL_KEY);
                triggerValueSpinner.setValue((int) condition.getValue());

                break;
            case TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPSIZ:
                triggerWhenCombo.setSelectedItem(HEAP_SIZE_ABS_KEY);
                triggerValueSpinner.setValue((int) (condition.getValue() / (1024 * 1024)));

                break;
            case TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_SURVGEN:
                triggerWhenCombo.setSelectedItem(SURVGEN_COUNT_KEY);
                triggerValueSpinner.setValue((int) condition.getValue());

                break;
            case TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_LDCLASS:
                triggerWhenCombo.setSelectedItem(LDCLASS_COUNT_KEY);
                triggerValueSpinner.setValue((int) condition.getValue());

                break;
            default:
                break;
        }

        triggerOnceRadio.setSelected(condition.isOnetime());
        triggerAlwaysRadio.setSelected(!condition.isOnetime());
    }

    public TriggeredGlobalProfilingPoint.TriggerCondition getTriggerCondition() {
        TriggeredGlobalProfilingPoint.TriggerCondition condition = new TriggeredGlobalProfilingPoint.TriggerCondition();

        Object key = triggerWhenCombo.getSelectedItem();

        if (key == HEAP_USG_REL_KEY) {
            condition.setMetric(TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPUSG);
            condition.setValue(((Integer) triggerValueSpinner.getValue()).intValue());
        } else if (key == HEAP_SIZE_ABS_KEY) {
            condition.setMetric(TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPSIZ);
            condition.setValue(((Integer) triggerValueSpinner.getValue()).intValue() * (1024 * 1024));
        } else if (key == SURVGEN_COUNT_KEY) {
            condition.setMetric(TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_SURVGEN);
            condition.setValue(((Integer) triggerValueSpinner.getValue()).intValue());
        } else if (key == LDCLASS_COUNT_KEY) {
            condition.setMetric(TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_LDCLASS);
            condition.setValue(((Integer) triggerValueSpinner.getValue()).intValue());
        }

        condition.setOnetime(triggerOnceRadio.isSelected());

        return condition;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == triggerWhenCombo) {
            Object key = triggerWhenCombo.getSelectedItem();

            if (key == HEAP_USG_REL_KEY) {
                triggerGenerationsLabel.setText(HEAP_USG_REL_UNIT);
                triggerValueSpinner.setModel(percentsModel);
            } else if (key == HEAP_SIZE_ABS_KEY) {
                triggerGenerationsLabel.setText(HEAP_SIZE_ABS_UNIT);
                triggerValueSpinner.setModel(unitsModel);
            } else if (key == SURVGEN_COUNT_KEY) {
                triggerGenerationsLabel.setText(SURVGEN_COUNT_UNIT);
                triggerValueSpinner.setModel(unitsModel);
            } else if (key == LDCLASS_COUNT_KEY) {
                triggerGenerationsLabel.setText(LDCLASS_COUNT_UNIT);
                triggerValueSpinner.setModel(unitsModel);
            }
        }
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

        TriggerCustomizer main = new TriggerCustomizer();

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

    //  private int getDefaultTextComponentHeight() {
    //    if (defaultTextComponentHeight == -1) defaultTextComponentHeight = new JComboBox().getPreferredSize().height;
    //    return defaultTextComponentHeight;
    //  }
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        JPanel triggerSettingsContainer = new JPanel(new GridBagLayout());

        // triggerWhenLabel
        triggerWhenLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(triggerWhenLabel, TAKE_WHEN_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerSettingsContainer.add(triggerWhenLabel, constraints);

        // triggerWhenCombo
        triggerWhenCombo = new JComboBox(new Object[] { HEAP_USG_REL_KEY, HEAP_SIZE_ABS_KEY, SURVGEN_COUNT_KEY, LDCLASS_COUNT_KEY }) {
                public Dimension getPreferredSize() {
                    return new Dimension(Math.min(super.getPreferredSize().width, 200), super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        triggerWhenLabel.setLabelFor(triggerWhenCombo);
        triggerWhenCombo.addActionListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerSettingsContainer.add(triggerWhenCombo, constraints);

        // triggerExceedsLabel
        triggerExceedsLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(triggerExceedsLabel, EXCEEDS_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerSettingsContainer.add(triggerExceedsLabel, constraints);

        // lineNumberSpinner
        triggerValueSpinner = new JExtendedSpinner(percentsModel) {
                public Dimension getPreferredSize() {
                    return new Dimension(Math.max(super.getPreferredSize().width, 55),
                                         org.netbeans.modules.profiler.ui.stp.Utils.getDefaultSpinnerHeight());
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        triggerExceedsLabel.setLabelFor(triggerValueSpinner);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerSettingsContainer.add(triggerValueSpinner, constraints);

        // triggerGenerationsLabel
        triggerGenerationsLabel = new JLabel(HEAP_USG_REL_UNIT);
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerSettingsContainer.add(triggerGenerationsLabel, constraints);

        JPanel triggerFillerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 5;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        triggerSettingsContainer.add(triggerFillerPanel, constraints);

        // triggerSettingsContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 5, 0);
        add(triggerSettingsContainer, constraints);

        // --- next row ----------------------------------------------------------
        ButtonGroup triggerRadiosGroup = new ButtonGroup();
        JPanel triggerRadiosContainer = new JPanel(new GridBagLayout());

        // triggerOnceRadio
        triggerOnceRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(triggerOnceRadio, TAKE_ONCE_RADIO_TEXT);
        triggerRadiosGroup.add(triggerOnceRadio);
        triggerOnceRadio.setSelected(true);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        triggerRadiosContainer.add(triggerOnceRadio, constraints);

        // triggerAlwaysRadio
        triggerAlwaysRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(triggerAlwaysRadio, TAKE_ALWAYS_RADIO_TEXT);
        triggerRadiosGroup.add(triggerAlwaysRadio);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        triggerRadiosContainer.add(triggerAlwaysRadio, constraints);

        JPanel takeRadiosSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
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
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(triggerRadiosContainer, constraints);

        // --- next row ----------------------------------------------------------
        JPanel fillerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(fillerPanel, constraints);
    }

    private void updateValidity() {
        boolean isValid = true;

        if (isValid != TriggerCustomizer.this.areSettingsValid()) {
            fireValidityChanged(isValid);
        }
    }
}
