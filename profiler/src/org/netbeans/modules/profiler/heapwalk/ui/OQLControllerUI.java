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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.heapwalk.OQLController;
import org.netbeans.modules.profiler.heapwalk.oql.ui.OQLEditor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 * @author Jaroslav Bachorik
 */
public class OQLControllerUI extends JPanel implements PropertyChangeListener {
    // --- Presenter -------------------------------------------------------------

    private static class Presenter extends JToggleButton {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static ImageIcon ICON_INFO = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/heapwalk/ui/resources/oql.png")); // NOI18N

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
    private static final String CANCEL_BUTTON_TEXT = NbBundle.getMessage(OQLControllerUI.class,
            "AnalysisControllerUI_CancelButtonText"); // NOI18N
    private static final String PERFORM_BUTTON_TEXT = "Run Query";
    private static final String ANALYSIS_RESULTS_TEXT = NbBundle.getMessage(OQLControllerUI.class,
            "AnalysisControllerUI_AnalysisResultsText"); // NOI18N
    private static final String CONTROLLER_NAME = NbBundle.getMessage(OQLControllerUI.class,
            "OQLControllerUI_ControllerName"); // NOI18N
    private static final String CONTROLLER_DESCR = NbBundle.getMessage(OQLControllerUI.class,
            "OQLControllerUI_ControllerDescr"); // NOI18N
    // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private AbstractButton presenter;
    private OQLController oqlController;
    private HTMLTextArea resultsArea;
    private OQLEditor queryContainer;
    private JButton performButton;
    private JPanel resultsContainer;

    // --- UI definition ---------------------------------------------------------
    private JSplitPane contentsSplit;

    public OQLControllerUI(OQLController controller) {
        this.oqlController = controller;

        initComponents();
        updatePerformButton();
    }

    // --- Public interface ------------------------------------------------------
    public AbstractButton getPresenter() {
        if (presenter == null) {
            presenter = new Presenter();
        }

        return presenter;
    }

    public void setResult(final String result) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                resultsContainer.removeAll();
                HTMLTextArea resultDisplayer = new HTMLTextArea(result) {

                    protected void showURL(URL url) {
                        oqlController.showURL(url);
                    }
                };
                try {
                    resultDisplayer.setCaretPosition(0);
                } catch (Exception e) {
                }
                resultsContainer.add(resultDisplayer, BorderLayout.CENTER);
                resultsContainer.invalidate();
                updatePerformButton();
                revalidate();
                repaint();
            }
        });
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
        oqlController.cancelAnalysis();
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
        queryContainer = new OQLEditor(oqlController.getEngine());

        queryContainer.addPropertyChangeListener(OQLEditor.VALIDITY_PROPERTY, this);
//        new JPanel(new BorderLayout());
//        HTMLTextArea queryHeaderArea = new HTMLTextArea();
//
//        queryHeaderArea.setText("<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/rules.png'>&nbsp;&nbsp;"
//                             + "OQL Query:" + "</b><br><hr>"); // NOI18N
//
//        queryContainer.add(queryHeaderArea, BorderLayout.NORTH);
//
//        queryEditor = new JEditorPane("text/x-oql", "");
//
//        queryEditor.setBackground(queryHeaderArea.getBackground());
//        queryEditor.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY));
//        queryEditor.getDocument().addDocumentListener(new DocumentListener() {
//
//            public void insertUpdate(DocumentEvent e) {
//                updatePerformButton();
//            }
//
//            public void removeUpdate(DocumentEvent e) {
//                updatePerformButton();
//            }
//
//            public void changedUpdate(DocumentEvent e) {
//                //
//            }
//        });
//
//        JComponent jc = Utilities.getEditorUI(queryEditor).getExtComponent();
//
//        queryContainer.add(jc, BorderLayout.CENTER); //NOI18N
//
//        queryContainer.setBackground(queryHeaderArea.getBackground());

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 0, 5);
        add(queryContainer, constraints);

        // performButton
        performButton = new JButton(PERFORM_BUTTON_TEXT);
        performButton.setMnemonic('R');
        performButton.setEnabled(false);
        performButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                performAnalysis();
            }
        });

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(3, 0, 0, 8);
        add(performButton, constraints);

        // queryContainer
//        queryContainer = new JPanel(new GridBagLayout());
//        queryContainer.setOpaque(true);

        JScrollPane queryContainerScrollPane = new JScrollPane(queryContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
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

        queryContainerScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        queryContainerScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        queryContainerScrollPane.setBorder(BorderFactory.createEmptyBorder());
        queryContainerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        queryContainerScrollPane.setBackground(queryContainer.getBackground());
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 15, 5, 5);
        add(queryContainerScrollPane, constraints);

        // resultsArea
        resultsArea = new HTMLTextArea();
        resultsArea.setText("<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/properties.png'>&nbsp;&nbsp;" + ANALYSIS_RESULTS_TEXT + "</b><br><hr>"); // NOI18N
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
        resultsContainerScrollPane.setBackground(queryContainer.getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
//        constraints.weightx = 1;
//        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 15, 5, 8);
        add(resultsContainerScrollPane, constraints);

        // UI tweaks
        setBackground(queryContainer.getBackground());
        queryContainer.setBackground(queryContainer.getBackground());
        resultsContainer.setBackground(queryContainer.getBackground());
    }

    private String getQuery() {
        return queryContainer.getScript();
    }

    private void performAnalysis() {
        performButton.setEnabled(false);

        BoundedRangeModel progressModel = oqlController.executeQuery(getQuery());
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
        progressContainer.add(new JLabel("Executing query"), constraints);

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
        if (oqlController.isAnalysisRunning()) {
            performButton.setEnabled(false);
        } else {
            performButton.setEnabled(queryContainer.isValidScript());
//            performButton.setEnabled(queryEditor.getText().length() > 0);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(OQLEditor.VALIDITY_PROPERTY)) {
            updatePerformButton();
        }
    }
}
