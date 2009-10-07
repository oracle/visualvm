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

package org.netbeans.lib.profiler.ui.components.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;


/**
 *
 * @author Jiri Sedlacek
 */
public class CheckTreeCellRenderer extends JPanel implements TreeCellRendererPersistent {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Dimension checkBoxDimension = new JCheckBox().getPreferredSize();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JCheckBox checkBox = new JCheckBox();
    private ButtonModel checkBoxModel = checkBox.getModel();
    private Component treeRendererComponent;
    private DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer();
    private boolean persistentRenderer = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CheckTreeCellRenderer() {
        setLayout(new BorderLayout());
        setOpaque(false);
        checkBox.setOpaque(false);
        add(checkBox, BorderLayout.WEST);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static Dimension getCheckBoxDimension() {
        return checkBoxDimension;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
                                                  int row, boolean hasFocus) {
        // Get CheckTreeNode from current Node
        CheckTreeNode treeNode = (value instanceof CheckTreeNode) ? (CheckTreeNode) value : null;

        // Update UI
        if (treeRendererComponent != null) {
            remove(treeRendererComponent);
        }

        if ((treeNode != null) && treeRenderer instanceof DefaultTreeCellRenderer) {
            checkBox.setVisible(!persistentRenderer);
            setupCellRendererIcon((DefaultTreeCellRenderer) treeRenderer, treeNode.getIcon());
        } else {
            checkBox.setVisible(false);
            setupCellRendererIcon((DefaultTreeCellRenderer) treeRenderer, null);
        }

        treeRendererComponent = treeRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        add(treeRendererComponent, BorderLayout.CENTER);

        // Return if no path or not a CheckTreeNode
        if (treeNode == null) {
            return this;
        }

        // If tree model supports checking (uses CheckTreeNodes), setup CheckBox
        if (treeNode.isFullyChecked()) {
            setupCheckBox(Boolean.TRUE);
        } else {
            setupCheckBox(treeNode.isPartiallyChecked() ? null : Boolean.FALSE);
        }

        return this;
    }

    public Component getTreeCellRendererComponentPersistent(JTree tree, Object value, boolean selected, boolean expanded,
                                                            boolean leaf, int row, boolean hasFocus) {
        CheckTreeCellRenderer ctcr = new CheckTreeCellRenderer();
        ctcr.persistentRenderer = true;

        return ctcr.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }

    private void setupCellRendererIcon(DefaultTreeCellRenderer renderer, Icon icon) {
        renderer.setLeafIcon(icon);
        renderer.setOpenIcon(icon);
        renderer.setClosedIcon(icon);
    }

    private void setupCheckBox(Boolean state) {
        if (state == Boolean.TRUE) {
            // Fully checked
            checkBoxModel.setArmed(false);
            checkBoxModel.setPressed(false);
            checkBoxModel.setSelected(true);
        } else if (state == Boolean.FALSE) {
            // Fully unchecked
            checkBoxModel.setArmed(false);
            checkBoxModel.setPressed(false);
            checkBoxModel.setSelected(false);
        } else {
            // Partially checked
            checkBoxModel.setArmed(true);
            checkBoxModel.setPressed(true);
            checkBoxModel.setSelected(true);
        }
    }
}
