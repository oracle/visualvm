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

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.utils.formatting.DefaultMethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import org.netbeans.modules.profiler.ui.ManualMethodSelect;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.HelpCtx;


/**
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
public final class RootMethodsPanel extends JPanel implements ActionListener, ListSelectionListener, HelpCtx.Provider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ROOT_METHODS_LABEL_TEXT = NbBundle.getMessage(RootMethodsPanel.class,
                                                                              "RootMethodsPanel_RootMethodsLabelText"); //NOI18N
    private static final String ADD_FROM_PROJECTBUTTON_TEXT = NbBundle.getMessage(RootMethodsPanel.class,
                                                                                  "RootMethodsPanel_AddButtonText"); //NOI18N
    private static final String ADD_MANUAL_BUTTON_TEXT = NbBundle.getMessage(RootMethodsPanel.class,
                                                                             "RootMethodsPanel_AddManualButtonText"); //NOI18N
    private static final String REMOVE_BUTTON_TEXT = NbBundle.getMessage(RootMethodsPanel.class,
                                                                         "RootMethodsPanel_RemoveButtonText"); //NOI18N
    private static final String MESSAGE_AREA_TEXT = NbBundle.getMessage(RootMethodsPanel.class, "RootMethodsPanel_MessageAreaText"); //NOI18N
    private static final String SPECIFY_ROOT_METHODS_DIALOG_CAPTION = NbBundle.getMessage(RootMethodsPanel.class,
                                                                                          "RootMethodsPanel_SpecifyRootMethodsDialogCaption"); //NOI18N
    private static final String ROOTS_LIST_ACCESS_NAME = NbBundle.getMessage(RootMethodsPanel.class,
                                                                             "RootMethodsPanel_RootsListAccessName"); //NOI18N
    private static final String ADD_FROM_PROJECT_BUTTON_ACCESS_DESCR = NbBundle.getMessage(RootMethodsPanel.class,
                                                                                           "RootMethodsPanel_AddFromProjectButtonAccessDescr"); //NOI18N
    private static final String ADD_MANUALLY_BUTTON_ACCESS_DESCR = NbBundle.getMessage(RootMethodsPanel.class,
                                                                                       "RootMethodsPanel_AddManuallyButtonAccessDescr"); //NOI18N
    private static final String REMOVE_BUTTON_ACCESS_DESCR = NbBundle.getMessage(RootMethodsPanel.class,
                                                                                 "RootMethodsPanel_RemoveButtonAccessDescr"); //NOI18N
    private static final String INCORRECT_MANUAL_ROOT_MSG = NbBundle.getMessage(RootMethodsPanel.class,
                                                                                 "RootMethodsPanel_IncorrectManualRootMsg"); //NOI18N
                                                                                                                              // -----
    
    private static final String HELP_CTX_KEY = "RootMethodsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    
    private static RootMethodsPanel defaultInstance;
    private static MethodNameFormatterFactory formatterFactory = MethodNameFormatterFactory.getDefault(new DefaultMethodNameFormatter(DefaultMethodNameFormatter.VERBOSITY_FULLCLASSMETHOD));

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList selectedRoots = new ArrayList();
    private DefaultListModel rootsListModel;
    private HTMLTextArea hintArea;
    private JButton addFromProjectButton;
    private JButton addManualButton;
    private JButton removeButton;
    private JList rootsList;
    private Project project;
    private boolean globalAttach = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates new form RootMethodsPanel
     */
    private RootMethodsPanel() {
        super();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public static ClientUtils.SourceCodeSelection[] getSelectedRootMethods(ClientUtils.SourceCodeSelection[] roots,
                                                                           Project project) {
        final RootMethodsPanel rm = getDefault();
        rm.project = project;
        rm.globalAttach = rm.project == null;
        rm.addFromProjectButton.setEnabled(!rm.globalAttach || (OpenProjects.getDefault().getOpenProjects().length > 0));
        rm.refreshList(roots);

        return performDisplay(rm);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == addFromProjectButton) {
            RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        final ClientUtils.SourceCodeSelection[] sel = ProjectSelectRootMethodsPanel.getDefault()
                                                                                                   .getRootMethods(project,
                                                                                                                   (ClientUtils.SourceCodeSelection[]) selectedRoots
                                                                                                                                                   .toArray(new ClientUtils.SourceCodeSelection[] {
                                                                                                                                                                
                                                                                                                                                            }));

                        if (sel != null) {
                            addNewRootMethods(sel, true);
                        }
                    }
                });
        } else if (e.getSource() == addManualButton) {
            final ClientUtils.SourceCodeSelection scs = ManualMethodSelect.selectMethod();
            
            if (scs != null) {
                String newItem = null;
                try {
                    newItem = formatterFactory.getFormatter().formatMethodName(scs).toFormatted();
                    if (!selectedRoots.contains(scs)) {
                        selectedRoots.add(scs);

                        rootsListModel.addElement(newItem);
                        rootsList.setSelectedValue(newItem, true);
                    }
                } catch (Exception ex) {
                    NetBeansProfiler.getDefaultNB().displayError(INCORRECT_MANUAL_ROOT_MSG);
                }
            }
        } else if (e.getSource() == removeButton) {
            final int[] selectedSessionIndices = rootsList.getSelectedIndices();

            for (int i = selectedSessionIndices.length - 1; i >= 0; i--) {
                rootsListModel.remove(selectedSessionIndices[i]);
                selectedRoots.remove(selectedSessionIndices[i]);
            }

            int toSelect = -1;

            if (selectedSessionIndices.length > 0) {
                toSelect = selectedSessionIndices[0];
            }

            if (toSelect >= rootsListModel.size()) {
                toSelect = rootsListModel.size() - 1;
            }

            rootsList.setSelectedIndex(toSelect);
            updateButtons();
        }
    }

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the change.
     */
    public void valueChanged(final ListSelectionEvent e) {
        updateButtons();
    }

    private static RootMethodsPanel getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new RootMethodsPanel();
        }

        return defaultInstance;
    }

    // ---------------------------------------------------------------------------
    private static ClientUtils.SourceCodeSelection[] performDisplay(final RootMethodsPanel rm) {
        final DialogDescriptor dd = new DialogDescriptor(rm, SPECIFY_ROOT_METHODS_DIALOG_CAPTION);
        final Dialog d = ProfilerDialogs.createDialog(dd);

        if (rm.addFromProjectButton.isEnabled()) {
            rm.addFromProjectButton.grabFocus();
        }

        d.setVisible(true);

        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
            ClientUtils.SourceCodeSelection[] ret = new ClientUtils.SourceCodeSelection[rm.selectedRoots.size()];
            rm.selectedRoots.toArray(ret);

            return ret;
        }

        return null;
    }

    private void addNewRootMethods(ClientUtils.SourceCodeSelection[] sel, boolean clean) {
        if (clean) {
            selectedRoots.clear();
            rootsListModel.clear();
        }

        MethodNameFormatter formatter = formatterFactory.getFormatter();

        for (int i = 0; i < sel.length; i++) {
            final ClientUtils.SourceCodeSelection scs = sel[i];

            if (!selectedRoots.contains(scs)) {
                selectedRoots.add(scs);
                rootsListModel.addElement(formatter.formatMethodName(scs).toFormatted());

                //          new MethodNameFormatter(
                //          scs.getClassName(), scs.getMethodName(), scs.getMethodSignature()
                //          ).getFullFormattedClassAndMethod()
                //          );
            }
        }
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;
        setLayout(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(12, 12, 12, 12);

        JLabel label = new JLabel();
        JScrollPane rootsScrollPane = new JScrollPane();
        rootsList = new JList();

        JPanel buttonPanel = new JPanel();
        addFromProjectButton = new JButton();
        addManualButton = new JButton();
        removeButton = new JButton();
        hintArea = new HTMLTextArea() {
                public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                    return new Dimension(1, super.getPreferredSize().height);
                }
            };

        org.openide.awt.Mnemonics.setLocalizedText(label, ROOT_METHODS_LABEL_TEXT);
        label.setLabelFor(rootsList);
        label.setIconTextGap(10);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(12, 12, 5, 12);
        add(label, gridBagConstraints);

        rootsList.getAccessibleContext().setAccessibleName(ROOTS_LIST_ACCESS_NAME);

        rootsScrollPane.setViewportView(rootsList);
        rootsScrollPane.setPreferredSize(new Dimension(330, rootsScrollPane.getPreferredSize().height));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 24, 0, 12);
        add(rootsScrollPane, gridBagConstraints);

        buttonPanel.setLayout(new GridLayout(4, 1, 0, 5));

        buttonPanel.setOpaque(false);

        org.openide.awt.Mnemonics.setLocalizedText(addFromProjectButton, ADD_FROM_PROJECTBUTTON_TEXT);
        addFromProjectButton.getAccessibleContext().setAccessibleDescription(ADD_FROM_PROJECT_BUTTON_ACCESS_DESCR);
        buttonPanel.add(addFromProjectButton);

        org.openide.awt.Mnemonics.setLocalizedText(addManualButton, ADD_MANUAL_BUTTON_TEXT);
        addManualButton.getAccessibleContext().setAccessibleDescription(ADD_MANUALLY_BUTTON_ACCESS_DESCR);
        buttonPanel.add(addManualButton);

        org.openide.awt.Mnemonics.setLocalizedText(removeButton, REMOVE_BUTTON_TEXT);
        removeButton.getAccessibleContext().setAccessibleDescription(REMOVE_BUTTON_ACCESS_DESCR);
        buttonPanel.add(removeButton);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.insets = new Insets(0, 0, 0, 12);
        add(buttonPanel, gridBagConstraints);

        Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
        Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                                                    panelBackground.getBlue() - 10);

        // hintArea
        hintArea.setText(MESSAGE_AREA_TEXT); // NOI18N
        hintArea.setEnabled(false);
        hintArea.setDisabledTextColor(Color.darkGray);
        hintArea.setBackground(hintBackground);
        hintArea.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new Insets(12, 12, 0, 12);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        add(hintArea, gridBagConstraints);

        addFromProjectButton.addActionListener(this);
        addManualButton.addActionListener(this);
        removeButton.addActionListener(this);
        rootsList.addListSelectionListener(this);
    }

    private void refreshList(final ClientUtils.SourceCodeSelection[] roots) {
        selectedRoots.clear();

        for (int i = 0; i < roots.length; i++) {
            ClientUtils.SourceCodeSelection root = roots[i];

            if (root != null) {
                selectedRoots.add(root);
            }
        }

        rootsListModel = new DefaultListModel();
        updateList();
        rootsList.setModel(rootsListModel);

        updateButtons();
    }

    private void updateButtons() {
        removeButton.setEnabled(rootsList.getSelectedIndices().length > 0);
    }

    private void updateList() {
        rootsListModel.removeAllElements();

        MethodNameFormatter formatter = formatterFactory.getFormatter();

        for (Iterator it = selectedRoots.iterator(); it.hasNext();) {
            final ClientUtils.SourceCodeSelection scs = (ClientUtils.SourceCodeSelection) it.next();
            rootsListModel.addElement(formatter.formatMethodName(scs).toFormatted());

            //        new MethodNameFormatter(
            //        scs.getClassName(), scs.getMethodName(), scs.getMethodSignature()
            //        ).getFullFormattedClassAndMethod()
            //        );
        }
    }
}
