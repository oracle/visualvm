/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.selector.api.SelectionTreeBuilderFactory;
import org.netbeans.modules.profiler.selector.spi.SelectionTreeBuilder;
import org.netbeans.modules.profiler.selector.spi.SelectionTreeBuilder.Type;
import org.netbeans.modules.profiler.selector.ui.RootSelectorTree;
import org.netbeans.modules.profiler.selector.ui.ProgressDisplayer;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jaroslav Bachorik
 */
final public class ClassSelectRootMethodsPanel extends JPanel {

    final private static class Singleton {

        final private static ClassSelectRootMethodsPanel INSTANCE = new ClassSelectRootMethodsPanel();
    }

    final public static ClassSelectRootMethodsPanel getDefault() {
        return Singleton.INSTANCE;
    }
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(ClassSelectRootMethodsPanel.class,
            "SelectRootMethodsPanel_OkButtonText"); // NOI18N
    private static final String HELP_CTX_KEY = "ClassSelectRootMethodsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static final Dimension PREFERRED_TOPTREE_DIMENSION = new Dimension(500, 250);
    private JButton okButton;
    private RootSelectorTree advancedLogicalPackageTree;

    private ClassSelectRootMethodsPanel() {
        init(this);
    }

    private void init(final Container container) {
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

        container.setLayout(new GridBagLayout());

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
    }

    public ClientUtils.SourceCodeSelection[] getRootMethods(final FileObject javaFile,
            final ClientUtils.SourceCodeSelection[] currentSelection) {
        advancedLogicalPackageTree.reset();

        updateSelector(new Runnable() {

            public void run() {
                List<SelectionTreeBuilder> builders = SelectionTreeBuilderFactory.buildersFor(javaFile);
                advancedLogicalPackageTree.setContext(Lookups.fixed((Object[])builders.toArray(new SelectionTreeBuilder[builders.size()])));
                advancedLogicalPackageTree.setSelection(currentSelection);

                List<Type> builderTypes = advancedLogicalPackageTree.getBuilderTypes();
                if (builderTypes.size() > 0) {
                    advancedLogicalPackageTree.setBuilderType(builderTypes.get(0));
                }
            }
        });

        final DialogDescriptor dd = new DialogDescriptor(this,
                NbBundle.getMessage(this.getClass(), "SelectRootMethodsPanel_Title"), // NOI18N
                true, new Object[]{okButton, DialogDescriptor.CANCEL_OPTION},
                okButton, DialogDescriptor.BOTTOM_ALIGN, null, null);

//            Object[] additionalOptions = getAdditionalOptions();
//
//            if ((additionalOptions != null) && (additionalOptions.length > 0)) {
//                dd.setAdditionalOptions(additionalOptions);
//            }

        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.pack(); // To properly layout HTML hint area
        d.setVisible(true);

        if (dd.getValue().equals(okButton)) {
            ClientUtils.SourceCodeSelection[] selection = advancedLogicalPackageTree.getSelection();
            return selection;
        }
        return null;
    }

    private void updateSelector(Runnable updater) {
        final ProgressHandle ph = ProgressHandleFactory.createHandle(NbBundle.getMessage(this.getClass(),
                "SelectRootMethodsPanel_ParsingProjectStructureMessage")); // NOI18N
        CommonUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                ph.setInitialDelay(500);
                ph.start();
            }
        });

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
