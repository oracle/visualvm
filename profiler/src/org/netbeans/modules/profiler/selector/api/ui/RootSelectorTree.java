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

package org.netbeans.modules.profiler.selector.api.ui;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.SwingWorker;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JCheckTree;
import org.netbeans.lib.profiler.ui.components.JCheckTree.CheckTreeListener;
import org.netbeans.lib.profiler.ui.components.tree.CheckTreeNode;
import org.netbeans.modules.profiler.selector.api.SelectionTreeBuilder;
import org.netbeans.modules.profiler.selector.api.SelectorNode;
import org.netbeans.modules.profiler.selector.api.nodes.ContainerNode;
import org.netbeans.modules.profiler.ui.panels.ProgressDisplayer;
import org.openide.util.NbBundle;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class RootSelectorTree extends JCheckTree {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class CancellableController implements ProgressDisplayer.ProgressController {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private volatile boolean cancelled = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean cancel() {
            cancelled = true;

            return true;
        }
    }

    private final class TreeKickStart extends SwingWorker {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private ProgressDisplayer progress = null;
        private ProgressHandle ph;
        private TreeNode rootNode;
        private Project[] projects;
        private ClientUtils.SourceCodeSelection[] selection;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TreeKickStart(Project[] projects, ClientUtils.SourceCodeSelection[] selection) {
            super(false);
            this.projects = projects;
            this.selection = selection;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected void doInBackground() {
            setModel(new DefaultTreeModel(new DefaultMutableTreeNode(LOADING_STRING)));
            setRootVisible(true);
            setShowsRootHandles(false);
            relevantProjects = projects;
            rootNode = customizeRoot(getTreeRoot());
            setRootVisible(false);
            setShowsRootHandles(true);
            setModel(new DefaultTreeModel(rootNode));
            applySelection(selection);
            treeDidChange();
        }

        protected void done() {
            if (progress != null) {
                progress.close();
            }

            rootNode = null;
            projects = null;
            selection = null;
        }

        protected void nonResponding() {
            progress = ProgressDisplayer.showProgress(ACCESSING_STRING);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String EMPTY_STRING = NbBundle.getMessage(RootSelectorTree.class, "RootSelectorTree_EmptyString"); // NOI18N
    private static final String LOADING_STRING = NbBundle.getMessage(RootSelectorTree.class, "RootSelectorTree_LoadingString"); // NOI18N
    private static final String ACCESSING_STRING = NbBundle.getMessage(RootSelectorTree.class, "RootSelectorTree_AccessingString"); // NOI18N
    private static final String ROOT_STRING = NbBundle.getMessage(RootSelectorTree.class, "RootSelectorTree_RootString"); // NOI18N
    private static final String NO_PROJECT_STRING = NbBundle.getMessage(RootSelectorTree.class, "RootSelectorTree_NoProjectString"); // NOI18N
                                                                                                                                     // -----
    private static final TreeModel DEFAULTMODEL = new DefaultTreeModel(new DefaultMutableTreeNode(EMPTY_STRING));

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Set<ClientUtils.SourceCodeSelection> currentSelectionSet = new HashSet<ClientUtils.SourceCodeSelection>();
    private Project[] relevantProjects;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RootSelectorTree() {
        init();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setProjects(Project[] projects) {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        new TreeKickStart(projects, currentSelectionSet.toArray(new ClientUtils.SourceCodeSelection[currentSelectionSet.size()]))
        .execute();
    }

    public void setSelection(final ClientUtils.SourceCodeSelection[] selection) {
        new SwingWorker(false) {
                protected void doInBackground() {
                    applySelection(selection);
                }

                protected void done() {
                    treeDidChange();
                }
            }.execute();
    }

    public ClientUtils.SourceCodeSelection[] getSelection() {
        return currentSelectionSet.toArray(new ClientUtils.SourceCodeSelection[currentSelectionSet.size()]);
    }

    /**
     * Resets the selector tree
     * Clears the list of selected root methods + sets the default tree model
     * Should be called right before trying to show the selector tree
     */
    public void reset() {
        setModel(DEFAULTMODEL);
        currentSelectionSet.clear();
        relevantProjects = new Project[0];
    }

    public void setup(Project[] projects, final ClientUtils.SourceCodeSelection[] selection) {
        if (this.currentSelectionSet != null) {
            this.currentSelectionSet.clear();
        }

        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        new TreeKickStart(projects, selection).execute();
    }

    protected abstract SelectionTreeBuilder getBuilder();

    /**
     * Override this method to customize showing inherited methods
     */
    protected boolean isShowingInheritedMethods() {
        return false;
    }

    /**
     * Override this method in order to eg. process just a subtree
     */
    protected TreeNode customizeRoot(DefaultMutableTreeNode root) {
        return root;
    }

    private static void addRequiredPackages(Collection<ClientUtils.SourceCodeSelection> selection,
                                            Collection<ClientUtils.SourceCodeSelection> toAdd) {
        for (ClientUtils.SourceCodeSelection signature : selection) {
            boolean appendSignature = true;

            for (ClientUtils.SourceCodeSelection addSignature : toAdd) {
                appendSignature = appendSignature && !signature.contains(addSignature);
            }

            if (appendSignature) {
                toAdd.add(signature);
            }
        }
    }

    private static void calculateInflatedSelection(SelectorNode node, SelectorNode root,
                                                   Collection<ClientUtils.SourceCodeSelection> selection,
                                                   Collection<ClientUtils.SourceCodeSelection> toRemove) {
        if ((node == null) || (root == null) || (selection == null) || (toRemove == null)) {
            return; // don't process an invalid data
        }

        if (root.isFullyChecked() || root.isPartiallyChecked()) {
            if ((root.getSignature() != null) && !toRemove.contains(root.getSignature())) {
                selection.add(root.getSignature());
            }

            if ((root.getSignature() == null) || (node.getSignature() == null)
                    || root.getSignature().contains(node.getSignature())) {
                int childrenCount = root.getChildCount();

                for (int i = 0; i < childrenCount; i++) {
                    SelectorNode childNode = (SelectorNode) root.getChildAt(i);
                    calculateInflatedSelection(node, childNode, selection, toRemove);
                }
            }
        }
    }

    private static void checkNodeChildren(final DefaultMutableTreeNode myNode, boolean recurse) {
        checkNodeChildren(myNode, recurse, null);
    }

    private static void checkNodeChildren(final DefaultMutableTreeNode myNode, boolean recurse, CancellableController controller) {
        if ((controller != null) && controller.isCancelled()) {
            return;
        }

        Enumeration children = myNode.children();

        if (myNode instanceof CheckTreeNode) {
            if (((CheckTreeNode) myNode).isFullyChecked()) {
                while (children.hasMoreElements()) {
                    TreeNode child = (TreeNode) children.nextElement();

                    if (child instanceof CheckTreeNode) {
                        ((CheckTreeNode) child).setChecked(true);

                        if (recurse) {
                            checkNodeChildren((CheckTreeNode) child, recurse, controller);
                        }
                    }
                }
            }
        }
    }

    private boolean isSingleSelection() {
        return (relevantProjects != null) && (relevantProjects.length < 2);
    }

    private DefaultMutableTreeNode getTreeRoot() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ROOT_STRING);

        if (relevantProjects != null) {
            for (Project project : relevantProjects) {
                if (getBuilder().supports(project)) {
                    for (SelectorNode node : getBuilder().buildSelectionTree(project, isSingleSelection())) {
                        if (node.isValid()) {
                            root.add(node);
                        }
                    }
                }
            }
        } else {
            root.add(new DefaultMutableTreeNode(NO_PROJECT_STRING));
        }

        return root;
    }

    private void applySelection(ClientUtils.SourceCodeSelection[] selections) {
        TreeNode root = (TreeNode) this.getModel().getRoot();
        Enumeration childrenEnum = root.children();

        while (childrenEnum.hasMoreElements()) {
            Object child = childrenEnum.nextElement();

            if (child instanceof SelectorNode) {
                for (ClientUtils.SourceCodeSelection selection : selections) {
                    applySelection((SelectorNode) child, selection);
                }
            }
        }

        currentSelectionSet.addAll(Arrays.asList(selections));
    }

    private void applySelection(SelectorNode node, ClientUtils.SourceCodeSelection selection) {
        ClientUtils.SourceCodeSelection signature = node.getSignature();

        if (signature != null) {
            if (signature.equals(selection)) {
                node.setChecked(true);

                return;
            }

            if (!signature.contains(selection)) {
                return;
            }
        }

        Enumeration childrenEnum = node.children();

        while (childrenEnum.hasMoreElements()) {
            Object child = childrenEnum.nextElement();

            if (child instanceof SelectorNode) {
                applySelection((SelectorNode) child, selection);
            }
        }
    }

    private void init() {
        UIUtils.makeTreeAutoExpandable(this, true);
        this.addCheckTreeListener(new CheckTreeListener() {
                public void checkTreeChanged(Collection<CheckTreeNode> nodes) {
                }

                public void checkNodeToggled(final TreePath treePath, boolean before) {
                    if (!before) { // only after the node check-mark has been changed

                        SelectorNode selectedNode = (SelectorNode) treePath.getLastPathComponent();
                        Collection<ClientUtils.SourceCodeSelection> signatures = selectedNode.getRootMethods(true);

                        if (selectedNode.isFullyChecked()) { // new root method selected

                            Collection<ClientUtils.SourceCodeSelection> toRemove = new ArrayList<ClientUtils.SourceCodeSelection>();

                            // replace with this root method as much as possible from the previously selected root methods (eg. wildcard replacing single root methods within a package etc.)
                            // basically remove all root methods of the selected node's subtree
                            for (ClientUtils.SourceCodeSelection signature : signatures) {
                                for (ClientUtils.SourceCodeSelection rootMethod : currentSelectionSet) {
                                    if (signature.contains(rootMethod)) {
                                        toRemove.add(rootMethod);
                                    }
                                }
                            }

                            removeSelection(toRemove.toArray(new ClientUtils.SourceCodeSelection[toRemove.size()]));
                            applySelection(signatures.toArray(new ClientUtils.SourceCodeSelection[signatures.size()]));
                        } else {
                            // removing a previously selected root method
                            ContainerNode parent = selectedNode.getParent();

                            Collection<ClientUtils.SourceCodeSelection> toAdd = new ArrayList<ClientUtils.SourceCodeSelection>();

                            if (parent != null) {
                                Enumeration siblings = parent.children();

                                // might be changing full-check to partial-check for the selected node parent; in that case replace the parent's wildcarded root method with its children root methods
                                while (siblings.hasMoreElements()) {
                                    SelectorNode siblingNode = (SelectorNode) siblings.nextElement();

                                    if ((siblingNode != selectedNode) && siblingNode.isFullyChecked()) {
                                        toAdd.addAll(siblingNode.getRootMethods(true));
                                    }
                                }
                            }

                            Collection<ClientUtils.SourceCodeSelection> toRemove = new ArrayList<ClientUtils.SourceCodeSelection>();

                            for (ClientUtils.SourceCodeSelection signature : signatures) {
                                for (ClientUtils.SourceCodeSelection rootMethod : currentSelectionSet) {
                                    if (rootMethod.contains(signature) || signature.contains(rootMethod)) {
                                        toRemove.add(rootMethod);
                                    }
                                }
                            }

                            toRemove.addAll(signatures);

                            TreeNode root = (TreeNode) getModel().getRoot();
                            Collection<ClientUtils.SourceCodeSelection> selection = new ArrayList<ClientUtils.SourceCodeSelection>();
                            int firstLevelCnt = root.getChildCount();

                            for (int i = 0; i < firstLevelCnt; i++) {
                                calculateInflatedSelection(parent, (SelectorNode) root.getChildAt(i), selection, toRemove);
                            }

                            addRequiredPackages(selection, toAdd);

                            removeSelection(toRemove.toArray(new ClientUtils.SourceCodeSelection[toRemove.size()]));

                            applySelection(toAdd.toArray(new ClientUtils.SourceCodeSelection[toAdd.size()]));
                        }
                    }
                }
            });
        this.addTreeWillExpandListener(new TreeWillExpandListener() {
                private volatile boolean openingSubtree = false;

                public void treeWillCollapse(TreeExpansionEvent event)
                                      throws ExpandVetoException {
                }

                public void treeWillExpand(final TreeExpansionEvent event)
                                    throws ExpandVetoException {
                    TreeNode node = (TreeNode) event.getPath().getLastPathComponent();

                    if (!(node instanceof DefaultMutableTreeNode)) {
                        return;
                    }

                    final DefaultMutableTreeNode myNode = (DefaultMutableTreeNode) node;

                    if (myNode.getChildCount() == -1) {
                        if (openingSubtree) {
                            throw new ExpandVetoException(event);
                        }

                        openingSubtree = true;

                        new SwingWorker() {
                                private volatile ProgressDisplayer progress = null;

                                protected void doInBackground() {
                                    checkNodeChildren(myNode, false);
                                }

                                protected void nonResponding() {
                                    progress = ProgressDisplayer.showProgress(NbBundle.getMessage(this.getClass(),
                                                                                                  "NodeLoadingMessage")); // NOI18N
                                }

                                protected void done() {
                                    if (progress != null) {
                                        progress.close();
                                    }

                                    expandPath(event.getPath());
                                    doLayout();
                                    openingSubtree = false;
                                }
                            }.execute();
                        throw new ExpandVetoException(event);
                    } else {
                        checkNodeChildren(myNode, false);
                    }
                }
            });

        this.setRootVisible(false);
        this.setShowsRootHandles(true);
        this.setModel(DEFAULTMODEL);
    }

    private void removeSelection(ClientUtils.SourceCodeSelection[] selections) {
        TreeNode root = (TreeNode) this.getModel().getRoot();
        Enumeration childrenEnum = root.children();

        while (childrenEnum.hasMoreElements()) {
            Object child = childrenEnum.nextElement();

            if (child instanceof SelectorNode) {
                for (ClientUtils.SourceCodeSelection selection : selections) {
                    removeSelection((SelectorNode) child, selection);
                }
            }
        }

        currentSelectionSet.removeAll(Arrays.asList(selections));
    }

    private void removeSelection(SelectorNode node, ClientUtils.SourceCodeSelection selection) {
        ClientUtils.SourceCodeSelection signature = node.getSignature();

        if (signature != null) {
            if (signature.equals(selection)) {
                node.setChecked(false);

                return;
            }

            if (!signature.contains(selection)) {
                return;
            }
        }

        Enumeration childrenEnum = node.children();

        while (childrenEnum.hasMoreElements()) {
            Object child = childrenEnum.nextElement();

            if (child instanceof SelectorNode) {
                removeSelection((SelectorNode) child, selection);
            }
        }
    }
}
