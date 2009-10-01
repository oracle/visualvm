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

package org.netbeans.modules.profiler.ui.panels;

import org.netbeans.lib.profiler.ui.components.HTMLTextArea;

//import org.netbeans.api.progress.ProgressHandle;
//import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


/**
 *
 * @author Jiri Sedlacek
 */
public class PreferredInstrFilterPanel extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public abstract static class FilterResolver {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String[] filterValues;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public synchronized String[] getFilterValues() {
            if (filterValues == null) {
                filterValues = computeFilterValues();
            }

            return filterValues;
        }

        protected abstract String[] computeFilterValues();
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String TO_QUICKFILTER_STRING = NbBundle.getMessage(PreferredInstrFilterPanel.class,
                                                                            "PreferredInstrFilterPanel_ToQuickFilterString"); //NOI18N
    private static final String TO_QUICKFILTER_ACCESS_DESCR = NbBundle.getMessage(PreferredInstrFilterPanel.class,
                                                                                  "PreferredInstrFilterPanel_ToQuickFilterAccessDescr"); //NOI18N
    private static final String CLOSE_BUTTON_STRING = NbBundle.getMessage(PreferredInstrFilterPanel.class,
                                                                          "PreferredInstrFilterPanel_CloseButtonString"); //NOI18N
    private static final String PREFERRED_PACKAGES_CAPTION = NbBundle.getMessage(PreferredInstrFilterPanel.class,
                                                                                 "PreferredInstrFilterPanel_PreferredPackagesCaption"); //NOI18N
    private static final String COMPUTING_FILTER_STRING = NbBundle.getMessage(PreferredInstrFilterPanel.class,
                                                                              "PreferredInstrFilterPanel_ComputingFilterString"); //NOI18N
                                                                                                                                  // -----
    private static PreferredInstrFilterPanel defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public JButton CLOSE_BUTTON;
    public JButton OPEN_IN_QUICKFILTER_BUTTON;
    private HTMLTextArea packagesArea;
    private JLabel label;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of PreferredInstrFilterPanel */
    private PreferredInstrFilterPanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static PreferredInstrFilterPanel getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new PreferredInstrFilterPanel();
        }

        return defaultInstance;
    }

    public void init(final FilterResolver filterComputer) {
        // Update busy UI from AWT
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    OPEN_IN_QUICKFILTER_BUTTON.setEnabled(false);
                    packagesArea.setText(COMPUTING_FILTER_STRING);

                    // Compute filters in worker thread
                    RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                //            ProgressHandle pHandle = null;
                                //            try {
                                //              pHandle = ProgressHandleFactory.createHandle(COMPUTING_FILTER_STRING);
                                //              pHandle.setInitialDelay(0);
                                //              pHandle.start();

                                // Compute filter value
                                String[] packageNames = filterComputer.getFilterValues();

                                // Sort filter
                                List<String> packageNamesList = new ArrayList(packageNames.length);

                                for (String filterPart : packageNames) {
                                    packageNamesList.add(filterPart);
                                }

                                Collections.sort(packageNamesList);

                                // Format: each package to separate line
                                final StringBuffer packageNamesBuffer = new StringBuffer(packageNames.length);
                                Iterator<String> it = packageNamesList.iterator();

                                while (it.hasNext()) {
                                    packageNamesBuffer.append(it.next());

                                    if (it.hasNext()) {
                                        packageNamesBuffer.append("<br>"); // NOI18N
                                    }
                                }

                                // Update finished UI from AWT
                                SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            packagesArea.setText(packageNamesBuffer.toString());
                                            packagesArea.setCaretPosition(0);
                                            OPEN_IN_QUICKFILTER_BUTTON.setEnabled(true);
                                        }
                                    });

                                //            } finally {
                                //              if (pHandle != null) pHandle.finish();
                                //            }
                            }
                        });
                }
            });
    }

    private void initComponents() {
        OPEN_IN_QUICKFILTER_BUTTON = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(OPEN_IN_QUICKFILTER_BUTTON, TO_QUICKFILTER_STRING);
        OPEN_IN_QUICKFILTER_BUTTON.getAccessibleContext().setAccessibleDescription(TO_QUICKFILTER_ACCESS_DESCR);
        CLOSE_BUTTON = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(CLOSE_BUTTON, CLOSE_BUTTON_STRING);

        label = new JLabel();
        packagesArea = new HTMLTextArea();

        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());

        label.setLabelFor(packagesArea);
        org.openide.awt.Mnemonics.setLocalizedText(label, PREFERRED_PACKAGES_CAPTION);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(15, 10, 5, 10);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(label, gridBagConstraints);

        JScrollPane packagesAreaScrollPane = new JScrollPane();
        packagesAreaScrollPane.setPreferredSize(new Dimension(350, 200));
        packagesArea.setText(""); //NOI18N
        packagesAreaScrollPane.setViewportView(packagesArea);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(packagesAreaScrollPane, gridBagConstraints);
    }
}
