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

package org.netbeans.lib.profiler.ui.cpu;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.ResultsPanel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;


/**
 * Common superclass for all results displays displaying CPU results.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public abstract class CPUResultsPanel extends ResultsPanel implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.Bundle"); // NOI18N
    private static final String GO_TO_SOURCE_ITEM_NAME = messages.getString("CPUResultsPanel_GoToSourceItemName"); // NOI18N
    private static final String BACKTRACES_ITEM_NAME = messages.getString("CPUResultsPanel_BackTracesItemName"); // NOI18N
    private static final String SUBTREE_ITEM_NAME = messages.getString("CPUResultsPanel_SubtreeItemName"); // NOI18N
    private static final String ROOT_METHODS_ITEM_NAME = messages.getString("CPUResultsPanel_RootMethodsItemName"); // NOI18N
                                                                                                                    // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected CPUResUserActionsHandler actionsHandler;
    protected JMenuItem popupAddToRoots;
    protected JMenuItem popupFind;
    protected JMenuItem popupShowReverse;
    protected JMenuItem popupShowSource;
    protected JMenuItem popupShowSubtree;
    protected JPopupMenu callGraphPopupMenu;
    protected JPopupMenu cornerPopup;
    protected TreePath popupPath;
    protected String[] columnNames;
    protected TableCellRenderer[] columnRenderers;
    protected String[] columnToolTips;
    protected int[] columnWidths;
    protected boolean[] columnsVisibility;
    protected int columnCount = 0;
    protected int currentView; // View AKA aggregation level: CPUResultsSnapshot.METHOD_LEVEL, CLASS_LEVEL or PACKAGE_LEVEL
    protected int methodId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CPUResultsPanel(CPUResUserActionsHandler actionsHandler) {
        this.actionsHandler = actionsHandler;
        callGraphPopupMenu = createPopupMenu();

        if (popupFind != null) {
            popupFind.setVisible(false);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Returns a meaningful value only for those subclasses that present data for a single thread */
    public abstract int getCurrentThreadId();

    public int getCurrentView() {
        return currentView;
    }

    public JMenuItem getPopupFindItem() {
        return popupFind;
    }

    // Should be overridden whenever possible
    public boolean getSortingOrder() {
        return false;
    }

    /** Changes the aggregation level for the CPU Results
     *
     * @param view one of CPUResultsSnapshot.METHOD_LEVEL_VIEW, CPUResultsSnapshot.CLASS_LEVEL_VIEW, CPUResultsSnapshot.PACKAGE_LEVEL_VIEW
     *
     * @see CPUResultsSnapshot.METHOD_LEVEL_VIEW
     * @see CPUResultsSnapshot.CLASS_LEVEL_VIEW
     * @see CPUResultsSnapshot.PACKAGE_LEVEL_VIEW
     */
    public void changeView(int view) {
        if (currentView == view) {
            return;
        }

        currentView = view;

        popupShowSource.setEnabled(isShowSourceAvailable());
        popupAddToRoots.setEnabled(isAddToRootsAvailable());

        actionsHandler.viewChanged(view); // notify the actions handler about this
    }

    public abstract void reset();

    // Should be overridden whenever possible
    public int getSortingColumn() {
        return CommonConstants.SORTING_COLUMN_DEFAULT;
    }

    protected boolean isAddToRootsAvailable() {
        return (currentView == CPUResultsSnapshot.METHOD_LEVEL_VIEW);
    }

    protected abstract String[] getMethodClassNameAndSig(int methodId, int currentView);

    protected abstract String getSelectedMethodName();

    protected boolean isShowSourceAvailable() {
        return (currentView != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW);
    }

    // ------------------------------------------------------------------
    // Popup menu behavior
    protected JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        popupShowSource = new JMenuItem();
        popupAddToRoots = new JMenuItem();
        popupFind = new JMenuItem();

        Font boldfont = popup.getFont().deriveFont(Font.BOLD);

        ActionListener menuListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                menuActionPerformed(evt);
            }
        };

        popupShowSource.setFont(boldfont);
        popupShowSource.setText(GO_TO_SOURCE_ITEM_NAME);
        popup.add(popupShowSource);

        boolean separator = false;

        if (supportsSubtreeCallGraph()) {
            if (!separator) {
                popup.addSeparator();
                separator = true;
            }

            popupShowSubtree = new JMenuItem();
            popupShowSubtree.setText(SUBTREE_ITEM_NAME);
            popup.add(popupShowSubtree);
            popupShowSubtree.addActionListener(menuListener);
        }

        if (supportsReverseCallGraph()) {
            if (!separator) {
                popup.addSeparator();
                separator = true;
            }

            popupShowReverse = new JMenuItem();
            popupShowReverse.setText(BACKTRACES_ITEM_NAME);
            popup.add(popupShowReverse);
            popupShowReverse.addActionListener(menuListener);
        }

        popup.add(popupFind);

        popup.addSeparator();

        popupAddToRoots.setText(ROOT_METHODS_ITEM_NAME);
        popup.add(popupAddToRoots);

        popupShowSource.addActionListener(menuListener);
        popupAddToRoots.addActionListener(menuListener);
        popupFind.addActionListener(menuListener);

        return popup;
    }

    protected void performDefaultAction() {
        if (popupPath != null) {
            showSourceForMethod(popupPath);
        } else {
            showSourceForMethod(methodId);
        }
    }

    protected void showSourceForMethod(int methodId) {
        if (currentView != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW) {
            boolean methodLevelView = (currentView == CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            String[] classMethodAndSig = getMethodClassNameAndSig(methodId, currentView);
            actionsHandler.showSourceForMethod(classMethodAndSig[0], methodLevelView ? classMethodAndSig[1] : null,
                                               methodLevelView ? classMethodAndSig[2] : null);
        }
    }

    protected void showSourceForMethod(TreePath popupPath) {
        if (currentView != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW) {
            boolean methodLevelView = (currentView == CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            PrestimeCPUCCTNode node = (PrestimeCPUCCTNode) popupPath.getLastPathComponent();
            String[] classMethodAndSig = getMethodClassNameAndSig(node.getMethodId(), currentView);
            actionsHandler.showSourceForMethod(classMethodAndSig[0], methodLevelView ? classMethodAndSig[1] : null,
                                               methodLevelView ? classMethodAndSig[2] : null);
        }
    }

    protected abstract boolean supportsReverseCallGraph();

    protected abstract boolean supportsSubtreeCallGraph();

    protected void showReverseCallGraph(int threadId, int methodId, int currentView, int sortingColumn, boolean sortingOrder) {
        // do nothing, has to be overridden by classes that do support showing reverse call graphs and return
        // true from supportsReverseCallGraph
    }

    protected void showSubtreeCallGraph(final CCTNode node, int currentView, int sortingColumn, boolean sortingOrder) {
        // do nothing, has to be overridden by classes that do support showing subtree call graphs and return
        // true from supportsSubtreeCallGraph
    }

    void menuActionPerformed(ActionEvent evt) {
        Object src = evt.getSource();

        if (src == popupShowSource) {
            performDefaultAction();
        } else if (src == popupShowReverse) {
            int threadId = 0;

            if (popupPath != null) {
                PrestimeCPUCCTNode selectedNode = (PrestimeCPUCCTNode) popupPath.getLastPathComponent();

                if (selectedNode.getParent() == null) {
                    return; // Nothing to do for root node
                }

                if (selectedNode.isSelfTimeNode()) {
                    selectedNode = (PrestimeCPUCCTNode) selectedNode.getParent();
                }

                if (selectedNode == null) {
                    return; // Nothing to do for root node
                }

                if (selectedNode.getMethodId() == 0) {
                    if (selectedNode.getNChildren() > 0) {
                        methodId = ((PrestimeCPUCCTNode) selectedNode.getChild(0)).getMethodId();
                    }
                } else {
                    methodId = selectedNode.getMethodId();
                }

                threadId = selectedNode.getThreadId();
            } else {
                // methodId is already set
                threadId = getCurrentThreadId(); // It's a flat profile window or something and we request a path for its single thread
            }

            showReverseCallGraph(threadId, methodId, currentView, getSortingColumn(), getSortingOrder());
        } else if (src == popupShowSubtree) {
            if (popupPath != null) {
                if (popupPath.getParentPath() == null) {
                    return; // Nothing to do for root node
                }

                PrestimeCPUCCTNode selectedNode = (PrestimeCPUCCTNode) popupPath.getLastPathComponent();

                if (selectedNode.isSelfTimeNode()) {
                    selectedNode = (PrestimeCPUCCTNode) selectedNode.getParent();
                }

                if (selectedNode == null) {
                    return; // Nothing to do for root node
                }

                showSubtreeCallGraph(selectedNode, currentView, getSortingColumn(), getSortingOrder());
            }
        } else if (src == popupAddToRoots) {
            if (popupPath != null) {
                PrestimeCPUCCTNode selectedNode = (PrestimeCPUCCTNode) popupPath.getLastPathComponent();

                if (selectedNode.getMethodId() == 0) {
                    if (selectedNode.getParent() instanceof PrestimeCPUCCTNode) {
                        methodId = ((PrestimeCPUCCTNode) selectedNode.getParent()).getMethodId();
                    }
                } else {
                    methodId = selectedNode.getMethodId();
                }
            } // else methodId is already set

            String[] methodClassNameAndSig = getMethodClassNameAndSig(methodId, currentView);
            actionsHandler.addMethodToRoots(methodClassNameAndSig[0], methodClassNameAndSig[1], methodClassNameAndSig[2]);
        } else if (src == popupFind) {
            actionsHandler.find(this, getSelectedMethodName());
        }
    }
}
