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

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.selector.ui.ProgressDisplayer;
import org.netbeans.modules.profiler.selector.ui.RootSelectorTree;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.DialogDescriptor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import org.netbeans.modules.profiler.selector.spi.SelectionTreeBuilder.Type;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jaroslav Bachorik
 */
final public class ProjectSelectRootMethodsPanel extends JPanel {
    private static ProjectSelectRootMethodsPanel instance = null;

    // -----
    // I18N String constants
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(ProjectSelectRootMethodsPanel.class,
            "SelectRootMethodsPanel_OkButtonText"); // NOI18N
    private static final String REMOVE_SELECTED_ITEM_TEXT = NbBundle.getMessage(ProjectSelectRootMethodsPanel.class,
            "SelectRootMethodsPanel_RemoveSelectedItemText"); // NOI18N
    private static final String REMOVE_ALL_ITEM_TEXT = NbBundle.getMessage(ProjectSelectRootMethodsPanel.class,
            "SelectRootMethodsPanel_RemoveAllItemText"); // NOI18N
    private static final String SELECT_ALL_ITEM_TEXT = NbBundle.getMessage(ProjectSelectRootMethodsPanel.class,
            "SelectRootMethodsPanel_SelectAllItemText"); // NOI18N
    // -----
    protected static final Dimension PREFERRED_TOPTREE_DIMENSION = new Dimension(500, 250);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private HTMLTextArea hintArea;
    private JButton okButton;
    private JCheckBox advancedShowAllProjectsCheckBox;
    private JComboBox treeBuilderList;
    private Project currentProject;
    private RequestProcessor rp = new RequestProcessor("SRM-UI Processor", 1); // NOI18N
    private RootSelectorTree advancedLogicalPackageTree;
    private volatile boolean changingBuilderList = false;
    private boolean globalMode;

    private static final String HELP_CTX_KEY = "ProjectSelectRootMethodsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public static synchronized ProjectSelectRootMethodsPanel getDefault() {
        if (instance == null) {
            instance = new ProjectSelectRootMethodsPanel();
        }

        return instance;
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    /** Creates a new instance of ProjectSelectRootMethodsPanel */
    private ProjectSelectRootMethodsPanel() {
        initComponents(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public static boolean canBeShown() {
        return RootSelectorTree.canBeShown();
    }

    /**
     * This method handles selecting root methods for a single profiling session
     * The method also displays the UI for selecting root methods
     * @param project The project to select root methods for or <b>null</b> for global mode
     * @param currentSelection The current root method selection (valid for the profiling session)
     * @return Returns the array of newly selected root methods or <b>null</b> to signal that no root methods were selected
     */
    public ClientUtils.SourceCodeSelection[] getRootMethods(final Project project,
            final ClientUtils.SourceCodeSelection[] currentSelection) {
        this.currentProject = project;

        advancedLogicalPackageTree.reset();

        setGlobalMode(project == null);

        PropertyChangeListener pcl = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                refreshBuilderList();
            }
        };

        try {
            advancedLogicalPackageTree.addPropertyChangeListener(RootSelectorTree.SELECTION_TREE_VIEW_LIST_PROPERTY, pcl);

            updateSelector(new Runnable() {

                public void run() {
                    advancedLogicalPackageTree.setContext(getContext());
                    advancedLogicalPackageTree.setSelection(currentSelection);
                }
            });

            if (advancedLogicalPackageTree.getBuilderTypes().isEmpty()) {
                NotifyDescriptor nd = new NotifyDescriptor.Message("SelectRootMethodsPanel_NoSelectionProviders"); // NOI18N
                DialogDisplayer.getDefault().notify(nd);
                return null;
            }

            final DialogDescriptor dd = new DialogDescriptor(this,
                    NbBundle.getMessage(this.getClass(), "SelectRootMethodsPanel_Title"), // NOI18N
                    true, new Object[]{okButton, DialogDescriptor.CANCEL_OPTION},
                    okButton, DialogDescriptor.BOTTOM_ALIGN, null, null);

            Object[] additionalOptions = getAdditionalOptions();

            if ((additionalOptions != null) && (additionalOptions.length > 0)) {
                dd.setAdditionalOptions(additionalOptions);
            }

            final Dialog d = ProfilerDialogs.createDialog(dd);
            d.pack(); // To properly layout HTML hint area
            d.setVisible(true);

            //    ClientUtils.SourceCodeSelection[] rootMethods = this.currentSelectionSet.toArray(new ClientUtils.SourceCodeSelection[this.currentSelectionSet.size()]);
            this.currentProject = null;

            if (dd.getValue().equals(okButton)) {
                ClientUtils.SourceCodeSelection[] selection = advancedLogicalPackageTree.getSelection();
                return selection;
            }
            return null;
        } finally {
            advancedLogicalPackageTree.removePropertyChangeListener(RootSelectorTree.SELECTION_TREE_VIEW_LIST_PROPERTY, pcl);
        }

    //    this.currentSelectionSet.clear();
    //    return rootMethods;
    }

    protected void initComponents(final Container container) {
        GridBagConstraints gridBagConstraints;

        okButton = new JButton(OK_BUTTON_TEXT);

        ProgressDisplayer pd = new ProgressDisplayer() {

            ProfilerProgressDisplayer pd = null;

            public synchronized void showProgress(String message) {
                pd = ProfilerProgressDisplayer.showProgress(message);
            }

            public synchronized void showProgress(String message, ProgressController controller) {
                pd = ProfilerProgressDisplayer.showProgress(message, controller);
            }

            public synchronized void showProgress(String caption, String message, ProgressController controller) {
                pd = ProfilerProgressDisplayer.showProgress(caption, message, controller);
            }

            public synchronized boolean isOpened() {
                return pd != null;
            }

            public synchronized void close() {
                if (pd != null) {
                    pd.close();
                    pd = null;
                }
            }
        };

        advancedLogicalPackageTree = new RootSelectorTree(pd, RootSelectorTree.DEFAULT_FILTER);
//        advancedLogicalPackageTree.setNodeFilter(getNodeFilter());

        advancedShowAllProjectsCheckBox = new JCheckBox();
        advancedShowAllProjectsCheckBox.setVisible(true);

        treeBuilderList = new JComboBox();
        treeBuilderList.addItemListener(new ItemListener() {

            public void itemStateChanged(final ItemEvent e) {
                if (changingBuilderList) {
                    return;
                }

                if (e.getStateChange() == ItemEvent.SELECTED) {
                    rp.post(new Runnable() {

                        public void run() {
                            advancedLogicalPackageTree.setBuilderType((Type) e.getItem());
                        }
                    });
                }
            }
        });

        container.setLayout(new GridBagLayout());

        hintArea = new HTMLTextArea() {

            public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                return new Dimension(1, super.getPreferredSize().height);
            }
        };

        advancedLogicalPackageTree.setRowHeight(UIUtils.getDefaultRowHeight() + 2);

        JScrollPane advancedLogicalPackageTreeScrollPane = new JScrollPane(advancedLogicalPackageTree);
        advancedLogicalPackageTreeScrollPane.setPreferredSize(PREFERRED_TOPTREE_DIMENSION);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 10, 0, 10);
        container.add(advancedLogicalPackageTreeScrollPane, gridBagConstraints);

        advancedShowAllProjectsCheckBox.setText(NbBundle.getMessage(this.getClass(), "SelectRootMethodsPanel_ShowAllProjectsLabel")); // NOI18N
        advancedShowAllProjectsCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                rp.post(new Runnable() {

                    public void run() {
                        refreshBuilderList();
                        updateSelectorProjects();
                    }
                });
            }
        });

        JPanel comboPanel = new JPanel(new FlowLayout());
        comboPanel.add(new JLabel(NbBundle.getMessage(this.getClass(), "SelectRootMethodsPanel_SelectViewLabel"))); // NOI18N
        comboPanel.add(treeBuilderList);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 10, 5, 10);
        container.add(comboPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new Insets(10, 10, 5, 10);
        container.add(advancedShowAllProjectsCheckBox, gridBagConstraints);

        // hintArea
        String hintString = getHintString();

        if ((hintString != null) && (hintString.length() > 0)) {
            Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
            Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                    panelBackground.getBlue() - 10);
            hintArea.setText(hintString);
            hintArea.setEnabled(false);
            hintArea.setDisabledTextColor(Color.darkGray);
            hintArea.setBackground(hintBackground);
            hintArea.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.insets = new Insets(5, 10, 5, 10);
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            container.add(hintArea, gridBagConstraints);
        }
    }

    private Object[] getAdditionalOptions() {
        return null;
    }

    private void setGlobalMode(boolean value) {
        advancedShowAllProjectsCheckBox.setSelected(value);
        advancedShowAllProjectsCheckBox.setEnabled(!value);
        globalMode = value;
    }

    private String getHintString() {
        return null;
    }

    private void refreshBuilderList() {
        List<Type> builderTypes = advancedLogicalPackageTree.getBuilderTypes();
        if (builderTypes == null || builderTypes.isEmpty()) return;
        
        try {
            changingBuilderList = true;

            treeBuilderList.setModel(new DefaultComboBoxModel(builderTypes.toArray(new Type[builderTypes.size()])));

            treeBuilderList.setSelectedIndex(0);
            advancedLogicalPackageTree.setBuilderType((Type)treeBuilderList.getItemAt(0));
        } finally {
            changingBuilderList = false;
        }
    }

    private Project[] relevantProjects() {
        return advancedShowAllProjectsCheckBox.isSelected() ? OpenProjects.getDefault().getOpenProjects()
                : new Project[]{currentProject};
    }

    private void updateSelector(Runnable updater) {
        ProgressHandle ph = IDEUtils.indeterminateProgress(NbBundle.getMessage(this.getClass(),
                "SelectRootMethodsPanel_ParsingProjectStructureMessage"),
                500); // NOI18N

        try {
            treeBuilderList.setEnabled(false);
            advancedLogicalPackageTree.setEnabled(false);
            advancedShowAllProjectsCheckBox.setEnabled(false);
            okButton.setEnabled(false);
            updater.run();
        } finally {
            ph.finish();
            okButton.setEnabled(true);
            advancedShowAllProjectsCheckBox.setEnabled(!globalMode);
            advancedLogicalPackageTree.setEnabled(true);
            treeBuilderList.setEnabled(true);
        }
    }

    private void updateSelectorProjects() {
        updateSelector(new Runnable() {

            public void run() {
                advancedLogicalPackageTree.setContext(getContext());
            }
        });
    }

    private Lookup getContext() {
        return Lookups.fixed((Object[]) relevantProjects());
    }
}
