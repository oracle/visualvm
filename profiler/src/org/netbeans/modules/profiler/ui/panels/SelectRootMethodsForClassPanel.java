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

package org.netbeans.modules.profiler.ui.panels;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import org.netbeans.modules.profiler.selector.ui.ProgressDisplayer;
import org.netbeans.modules.profiler.selector.ui.RootSelectorNode;
import org.netbeans.modules.profiler.selector.ui.RootSelectorTree;
import org.netbeans.modules.profiler.utilities.trees.TreeDecimator;



/**
 *
 * @author Jaroslav Bachorik
 */
public class SelectRootMethodsForClassPanel extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static final Dimension PREFERRED_TOPTREE_DIMENSION = new Dimension(500, 250);
    private static SelectRootMethodsForClassPanel instance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    //  private JComboBox treeBuilderList;
    private HTMLTextArea hintArea;
    private JButton okButton;
    private Project currentProject;
    private RootSelectorTree advancedLogicalPackageTree;
    private String assignedClassName;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of SelectRootMethodsForClassPanel */
    public SelectRootMethodsForClassPanel() {
        initComponents(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized SelectRootMethodsForClassPanel getDefault() {
        if (instance == null) {
            instance = new SelectRootMethodsForClassPanel();
        }

        return instance;
    }

    public ClientUtils.SourceCodeSelection[] getRootMethods(final Project project, final String className,
                                                            final ClientUtils.SourceCodeSelection[] currentSelection) {
        this.assignedClassName = demaskInnerClass(className);
        this.currentProject = project;

        updateSelector(new Runnable() {
                public void run() {
                    advancedLogicalPackageTree.setup(new Project[] { currentProject }, currentSelection);
                }
            });

        final DialogDescriptor dd = new DialogDescriptor(this,
                                                         NbBundle.getMessage(this.getClass(), "SelectRootMethodsPanel_Title"), // NOI18N
                                                         true, new Object[] { okButton, DialogDescriptor.CANCEL_OPTION },
                                                         okButton, DialogDescriptor.BOTTOM_ALIGN, null, null);

        Object[] additionalOptions = getAdditionalOptions();

        if ((additionalOptions != null) && (additionalOptions.length > 0)) {
            dd.setAdditionalOptions(additionalOptions);
        }

        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack(); // To properly layout HTML hint area
        d.setVisible(true);

        this.currentProject = null;

        return advancedLogicalPackageTree.getSelection();
    }

    protected void initComponents(final Container container) {
        GridBagConstraints gridBagConstraints;

        okButton = new JButton("OK");

        advancedLogicalPackageTree = new RootSelectorTree(new ProgressDisplayer() {
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
        });
        
        advancedLogicalPackageTree.setNodeFilter(new TreeDecimator.NodeFilter<RootSelectorNode>() {
            public boolean match(RootSelectorNode node) {
                return node.getSignature().toFlattened().equals(assignedClassName);
            }

            public boolean maymatch(RootSelectorNode node) {
                return assignedClassName.startsWith(node.getSignature().toFlattened());
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

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new Insets(10, 10, 5, 10);

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

    private String getHintString() {
        return null;
    }

    private String demaskInnerClass(String className) {
        return className;
//        return className.replace('$', '.'); // NOI18N
    }

//    private SelectorNode[] findElligibleNodes(SelectorNode currentRoot) {
//        ClientUtils.SourceCodeSelection signature = currentRoot.getSignature();
//
//        if ((signature != null) && signature.getClassName().equals(assignedClassName)) {
//            return new SelectorNode[] { currentRoot };
//        }
//
//        Set<SelectorNode> foundNodes = new HashSet<SelectorNode>();
//
//        if ((signature == null) || assignedClassName.startsWith(signature.toFlattened())) {
//            Enumeration chldrn = currentRoot.children();
//
//            while (chldrn.hasMoreElements()) {
//                SelectorNode[] nodes = findElligibleNodes((SelectorNode) chldrn.nextElement());
//                foundNodes.addAll(Arrays.asList(nodes));
//            }
//        }
//
//        return foundNodes.toArray(new SelectorNode[foundNodes.size()]);
//    }
//
//    private void refreshDefaultBuilder() {
//        Collection<?extends SelectionTreeBuilder> allBuilders = Lookup.getDefault().lookupAll(SelectionTreeBuilder.class);
//
//        for (SelectionTreeBuilder builder : allBuilders) {
//            if (builder.supports(currentProject)) {
//                if (builder.isDefault()) {
//                    defaultBuilder = builder;
//
//                    break;
//                }
//            }
//        }
//    }

    private void updateSelector(Runnable updater) {
        ProgressHandle ph = IDEUtils.indeterminateProgress(NbBundle.getMessage(this.getClass(),
                                                                               "SelectRootMethodsPanel_ParsingProjectStructureMessage"),
                                                           500); // NOI18N

        try {
            advancedLogicalPackageTree.setEnabled(false);
            okButton.setEnabled(false);
            updater.run();
        } finally {
            ph.finish();
            okButton.setEnabled(true);
            advancedLogicalPackageTree.setEnabled(true);
        }
    }
}
