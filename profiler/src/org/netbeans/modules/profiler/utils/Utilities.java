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

package org.netbeans.modules.profiler.utils;

import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 * Miscelaneous utilities for the NetBeans integration.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class Utilities {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static final class SelectClassPanel extends JPanel implements PropertyChangeListener, ExplorerManager.Provider {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private BeanTreeView explorerTree;
        private ExplorerManager manager;
        private JButton okButton;
        private boolean allowMultiple;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public SelectClassPanel() {
        }

        private SelectClassPanel(final JButton okButton, final boolean allowMultiple) {
            manager = new ExplorerManager();
            manager.setRootContext(new ProjectsRootNode());
            manager.addPropertyChangeListener(this);

            this.okButton = okButton;
            this.allowMultiple = allowMultiple;
            okButton.setEnabled(false);
            setBorder(new EmptyBorder(12, 12, 12, 12));
            setLayout(new BorderLayout());
            explorerTree = new BeanTreeView();
            explorerTree.getAccessibleContext().setAccessibleName(EXPLORER_TREE_ACCESS_NAME);
            explorerTree.getAccessibleContext().setAccessibleDescription(EXPLORER_TREE_ACCESS_DESCR);
            explorerTree.setDefaultActionAllowed(false);
            explorerTree.setPopupAllowed(false);
            explorerTree.setBorder(UIManager.getBorder("ScrollPane.border")); //NOI18N
            add(explorerTree, java.awt.BorderLayout.CENTER);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * Get the explorer manager.
         *
         * @return the manager
         */
        public ExplorerManager getExplorerManager() {
            return manager;
        }

        public Node[] getSelectedClassElements() {
            return getExplorerManager().getSelectedNodes();
        }

        public void propertyChange(final PropertyChangeEvent evt) {
            final Node[] nodes = getExplorerManager().getSelectedNodes();
            boolean enable = false;

            if (allowMultiple) {
                if (nodes.length > 0) {
                    for (int i = 0; i < nodes.length; i++) {
                        if ((nodes[i].getCookie(DataObject.class) == null) && (SourceUtils.isJavaClass(nodes[i]))) {
                            enable = false;

                            break;
                        }
                    }
                }
            } else {
                if ((nodes.length == 1) && (nodes[0].getCookie(DataObject.class) != null) && (SourceUtils.isJavaClass(nodes[0]))) {
                    enable = true;
                }
            }

            okButton.setEnabled(enable);
        }

        void activate() {
            ExplorerUtils.activateActions(manager, true);
        }

        void deactivate() {
            ExplorerUtils.activateActions(manager, false);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SELECT_CLASS_BUTTON_OK_TEXT = NbBundle.getMessage(Utilities.class,
                                                                                  "Utilities_SelectClass-ButtonOKText"); // NOI18N
    private static final String SELECT_CLASS_DIALOG_CAPTION = NbBundle.getMessage(Utilities.class,
                                                                                  "Utilities_SelectClass-DialogCaption"); // NOI18N
    private static final String EXPLORER_TREE_ACCESS_NAME = NbBundle.getMessage(Utilities.class,
                                                                                "Utilities_ExplorerTreeAccessName"); // NOI18N
    private static final String EXPLORER_TREE_ACCESS_DESCR = NbBundle.getMessage(Utilities.class,
                                                                                 "Utilities_ExplorerTreeAccessDescr"); // NOI18N
                                                                                                                       // -----
    private static SelectClassPanel selectClassPanel;
    private static JButton okButton;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Opens a dialog that allows the user to select a Java class (with or without source) from the filesystems
     *
     * @return ClassElement of the top class in the selected class file
     */
    public static Node[] selectClass(final boolean allowMultiple) {
        okButton = new JButton(SELECT_CLASS_BUTTON_OK_TEXT);
        selectClassPanel = new SelectClassPanel(okButton, allowMultiple);

        final DialogDescriptor dd = new DialogDescriptor(selectClassPanel, SELECT_CLASS_DIALOG_CAPTION, true,
                                                         new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                         DialogDescriptor.BOTTOM_ALIGN, null, null);
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            selectClassPanel.deactivate();

            return selectClassPanel.getSelectedClassElements();
        }

        selectClassPanel.deactivate();
        selectClassPanel = null;

        return null;
    }
}
