/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.heapwalk.ui;

import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.heapwalk.AnalysisController;
import org.netbeans.modules.profiler.heapwalk.memorylint.Rule;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class AnalysisControllerUI extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // -- Displayer for Rule htmlDescription -------------------------------------
    private static class DescriptionDisplayer extends JPanel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private DescriptionDisplayer(URL ruleBase, String htmlDescription) {
            initComponents(ruleBase, htmlDescription);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public static void showDescription(Rule rule, String htmlDescription) {
            Class ruleClass = rule.getClass();
            URL ruleBase = ruleClass.getResource(ruleClass.getSimpleName() + ".class"); // NOI18N
            final DialogDescriptor dd = new DialogDescriptor(new DescriptionDisplayer(ruleBase, htmlDescription),
                                                             rule.getDisplayName(), true,
                                                             new Object[] { DialogDescriptor.OK_OPTION },
                                                             DialogDescriptor.OK_OPTION, DialogDescriptor.BOTTOM_ALIGN, null, null);
            final Dialog d = ProfilerDialogs.createDialog(dd);
            d.pack(); // allows correct resizing of textarea in PreferredInstrFilterPanel
            d.setVisible(true);
        }

        private void initComponents(URL ruleBase, String htmlDescription) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            HTMLTextArea descriptionArea = new HTMLTextArea();
            HTMLDocument hdoc = (HTMLDocument) descriptionArea.getDocument();
            descriptionArea.setText(htmlDescription);
            descriptionArea.setCaretPosition(0);
            hdoc.setBase(ruleBase);

            JScrollPane descriptionAreaScrollPane = new JScrollPane(descriptionArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            descriptionAreaScrollPane.setPreferredSize(new Dimension(375, 220));

            add(descriptionAreaScrollPane, BorderLayout.CENTER);
        }
    }

    // --- Presenter -------------------------------------------------------------
    private static class Presenter extends JToggleButton {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static ImageIcon ICON_INFO = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/heapwalk/ui/resources/memoryLint.png")); // NOI18N

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Presenter() {
            super();
            setText(CONTROLLER_NAME);
            setToolTipText(CONTROLLER_DESCR);
            setIcon(ICON_INFO);
            setMargin(new java.awt.Insets(getMargin().top, getMargin().top, getMargin().bottom, getMargin().top));
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String INFO_STRING = NbBundle.getMessage(AnalysisControllerUI.class, "AnalysisControllerUI_InfoString"); // NOI18N
    private static final String RULES_TO_APPLY_STRING = NbBundle.getMessage(AnalysisControllerUI.class,
                                                                            "AnalysisControllerUI_RulesToApplyString"); // NOI18N
    private static final String PROCESSING_RULES_MSG = NbBundle.getMessage(AnalysisControllerUI.class,
                                                                           "AnalysisControllerUI_ProcessingRulesMsg"); // NOI18N
    private static final String CANCEL_BUTTON_TEXT = NbBundle.getMessage(AnalysisControllerUI.class,
                                                                         "AnalysisControllerUI_CancelButtonText"); // NOI18N
    private static final String PERFORM_BUTTON_TEXT = NbBundle.getMessage(AnalysisControllerUI.class,
                                                                          "AnalysisControllerUI_PerformButtonText"); // NOI18N
    private static final String ANALYSIS_RESULTS_TEXT = NbBundle.getMessage(AnalysisControllerUI.class,
                                                                            "AnalysisControllerUI_AnalysisResultsText"); // NOI18N
    private static final String CONTROLLER_NAME = NbBundle.getMessage(AnalysisControllerUI.class,
                                                                      "AnalysisControllerUI_ControllerName"); // NOI18N
    private static final String CONTROLLER_DESCR = NbBundle.getMessage(AnalysisControllerUI.class,
                                                                       "AnalysisControllerUI_ControllerDescr"); // NOI18N
                                                                                                                // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AbstractButton presenter;
    private AnalysisController analysisController;
    private HTMLTextArea resultsArea;
    private HTMLTextArea settingsArea;
    private JButton performButton;
    private JPanel resultsContainer;
    private JPanel rulesContainer;

    // --- UI definition ---------------------------------------------------------
    private JSplitPane contentsSplit;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public AnalysisControllerUI(AnalysisController analysisController) {
        this.analysisController = analysisController;

        initComponents();
        initRules();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public AbstractButton getPresenter() {
        if (presenter == null) {
            presenter = new Presenter();
        }

        return presenter;
    }
    
    public void displayNewRules() {
        initRules();
    }

    public void setResult(final String result) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    resultsContainer.removeAll();
                    HTMLTextArea resultDisplayer = new HTMLTextArea(result) {
                        protected void showURL(URL url) {
                            analysisController.showURL(url);
                        }
                    };
                    try { resultDisplayer.setCaretPosition(0); } catch (Exception e) {}
                    resultsContainer.add(resultDisplayer, BorderLayout.CENTER);
                    resultsContainer.invalidate();
                    revalidate();
                    repaint();
                }
            });
    }

    private boolean[] getRulesSelection() {
        List<JCheckBox> ruleCheckboxes = new ArrayList();

        for (Component component : rulesContainer.getComponents()) {
            if (component instanceof JCheckBox && ((JCheckBox) component).getActionCommand().equals("RULE_CHECKBOX")) {
                ruleCheckboxes.add((JCheckBox) component); // NOI18N
            }
        }

        boolean[] rulesSelection = new boolean[ruleCheckboxes.size()];

        for (int i = 0; i < rulesSelection.length; i++) {
            rulesSelection[i] = ruleCheckboxes.get(i).isSelected();
        }

        return rulesSelection;
    }

    private void cancelAnalysis() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    resultsContainer.removeAll();
                    resultsContainer.invalidate();
                    revalidate();
                    repaint();
                    performButton.setEnabled(true);
                }
            });
        analysisController.cancelAnalysis();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // Top separator
        JSeparator separator = new JSeparator() {
            public Dimension getMaximumSize() {
                return new Dimension(super.getMaximumSize().width, 1);
            }

            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 1);
            }
        };

        separator.setBackground(getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(separator, constraints);

        // settingsArea
        settingsArea = new HTMLTextArea();
        settingsArea.setText("<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/rules.png'>&nbsp;&nbsp;"
                             + RULES_TO_APPLY_STRING + "</b><br><hr>&nbsp;&nbsp;&nbsp;&nbsp;Searching for rules..."); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 0, 5);
        add(settingsArea, constraints);

        // performButton
        performButton = new JButton(PERFORM_BUTTON_TEXT);
        performButton.setEnabled(false);
        performButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    performAnalysis();
                }
            });

        // rulesContainer
        rulesContainer = new JPanel(new GridBagLayout());
        rulesContainer.setOpaque(true);

        JScrollPane rulesContainerScrollPane = new JScrollPane(rulesContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                int height = Math.min(pref.height, 160);

                return new Dimension(pref.width, height);
            }

            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };

        rulesContainerScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        rulesContainerScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        rulesContainerScrollPane.setBorder(BorderFactory.createEmptyBorder());
        rulesContainerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        rulesContainerScrollPane.setBackground(settingsArea.getBackground());
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 15, 5, 5);
        add(rulesContainerScrollPane, constraints);

        // resultsArea
        resultsArea = new HTMLTextArea();
        resultsArea.setText("<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/properties.png'>&nbsp;&nbsp;"
                            + ANALYSIS_RESULTS_TEXT + "</b><br><hr>"); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        add(resultsArea, constraints);

        // resultsContainer
        resultsContainer = new JPanel(new BorderLayout());
        resultsContainer.setOpaque(true);

        JScrollPane resultsContainerScrollPane = new JScrollPane(resultsContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                int height = Math.min(pref.height, 160);

                return new Dimension(pref.width, height);
            }

            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };

        resultsContainerScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        resultsContainerScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        resultsContainerScrollPane.setBorder(BorderFactory.createEmptyBorder());
        resultsContainerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        resultsContainerScrollPane.setBackground(settingsArea.getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 15, 5, 8);
        add(resultsContainerScrollPane, constraints);

        // UI tweaks
        setBackground(settingsArea.getBackground());
        rulesContainer.setBackground(settingsArea.getBackground());
        resultsContainer.setBackground(settingsArea.getBackground());
    }

    // --- Private implementation ------------------------------------------------
    private void initRules() {
        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    final List<Rule> rules = analysisController.getRules();
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                GridBagConstraints constraints;
                                
                                synchronized(getTreeLock()) {
                                    rulesContainer.removeAll();

                                    for (int i = 0; i < rules.size(); i++) {
                                        final Rule rule = rules.get(i);
                                        final String ruleName = rule.getDisplayName();
                                        final String ruleDescription = rule.getDescription();
                                        final JComponent ruleCustomizer = rule.getCustomizer();
                                        final String htmlDescription = rule.getHTMLDescription();

                                        JCheckBox checkBox = new JCheckBox(ruleName);
                                        checkBox.setActionCommand("RULE_CHECKBOX"); // NOI18N
                                        checkBox.setOpaque(false);
                                        checkBox.setToolTipText(ruleDescription);
                                        constraints = new GridBagConstraints();
                                        constraints.gridx = 0;
                                        constraints.gridy = i;
                                        constraints.gridwidth = ((ruleCustomizer == null) && (htmlDescription == null))
                                                                ? GridBagConstraints.REMAINDER : 1;
                                        constraints.fill = GridBagConstraints.NONE;
                                        constraints.anchor = GridBagConstraints.WEST;
                                        constraints.insets = new Insets(0, 0, 0, 0);
                                        rulesContainer.add(checkBox, constraints);
                                        checkBox.addActionListener(new ActionListener() {
                                                public void actionPerformed(ActionEvent e) {
                                                    updatePerformButton();
                                                }
                                            });

                                        if (htmlDescription != null) {
                                            HTMLLabel description = new HTMLLabel("<a href='#info'>" + INFO_STRING + "</a>") { // NOI18N
                                                protected void showURL(URL url) {
                                                    DescriptionDisplayer.showDescription(rule, htmlDescription);
                                                }
                                            };

                                            description.setOpaque(false);
                                            constraints = new GridBagConstraints();
                                            constraints.gridx = 1;
                                            constraints.gridy = i;
                                            constraints.gridwidth = (ruleCustomizer == null) ? GridBagConstraints.REMAINDER : 1;
                                            constraints.fill = GridBagConstraints.NONE;
                                            constraints.anchor = GridBagConstraints.WEST;
                                            constraints.insets = new Insets(0, 2, 0, 0);
                                            rulesContainer.add(description, constraints);
                                        }

                                        if (ruleCustomizer != null) {
                                            ruleCustomizer.setOpaque(false);
                                            constraints = new GridBagConstraints();
                                            constraints.gridx = 2;
                                            constraints.gridy = i;
                                            constraints.gridwidth = GridBagConstraints.REMAINDER;
                                            constraints.fill = GridBagConstraints.NONE;
                                            constraints.anchor = GridBagConstraints.WEST;
                                            constraints.insets = new Insets(0, 8, 0, 5);
                                            rulesContainer.add(ruleCustomizer, constraints);
                                        }
                                    }

                                    JPanel fillerPanel = new JPanel(new GridBagLayout());
                                    fillerPanel.setOpaque(false);
                                    constraints = new GridBagConstraints();
                                    constraints.gridx = 0;
                                    constraints.gridy = rules.size();
                                    constraints.gridwidth = GridBagConstraints.REMAINDER;
                                    constraints.weightx = 1;
                                    constraints.fill = GridBagConstraints.HORIZONTAL;
                                    constraints.anchor = GridBagConstraints.WEST;
                                    constraints.insets = new Insets(0, 0, 0, 0);
                                    rulesContainer.add(fillerPanel, constraints);

                                    constraints = new GridBagConstraints();
                                    constraints.gridx = 1;
                                    constraints.gridy = 3;
                                    constraints.gridwidth = 1;
                                    constraints.fill = GridBagConstraints.NONE;
                                    constraints.anchor = GridBagConstraints.EAST;
                                    constraints.insets = new Insets(3, 0, 0, 8);
                                    add(performButton, constraints);

                                    settingsArea.setText("<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/rules.png'>&nbsp;&nbsp;"
                                                         + RULES_TO_APPLY_STRING + "</b><br><hr>"); // NOI18N
                                    updatePerformButton();
                                }
                            }
                        });
                }
            });
    }

    private void performAnalysis() {
        performButton.setEnabled(false);

        BoundedRangeModel progressModel = analysisController.performAnalysis(getRulesSelection());
        resultsContainer.removeAll();

        GridBagConstraints constraints;

        JPanel progressContainer = new JPanel(new GridBagLayout());
        progressContainer.setOpaque(false);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 5);
        progressContainer.add(new JLabel(PROCESSING_RULES_MSG + "  "), constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 0, 0, 8);
        progressContainer.add(new JProgressBar(progressModel), constraints);

        JButton cancelAnalysis = new JButton(CANCEL_BUTTON_TEXT);
        cancelAnalysis.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancelAnalysis();
                }
            });
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(0, 0, 0, 8);
        progressContainer.add(cancelAnalysis, constraints);

        resultsContainer.add(progressContainer, BorderLayout.NORTH);
        resultsContainer.invalidate();
        revalidate();
        repaint();
    }

    private void updatePerformButton() {
        if (analysisController.isAnalysisRunning()) {
            performButton.setEnabled(false);
        } else {
            boolean[] rulesSelection = getRulesSelection();

            if (rulesSelection.length > 0) {
                for (boolean checked : rulesSelection) {
                    if (checked) {
                        performButton.setEnabled(true);

                        return;
                    }
                }
            }

            performButton.setEnabled(false);
        }
    }
}
