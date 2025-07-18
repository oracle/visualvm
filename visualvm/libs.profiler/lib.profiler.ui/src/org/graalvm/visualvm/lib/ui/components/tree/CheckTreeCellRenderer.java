/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.ui.components.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
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
        // --- Workaround for #205932 - not sure why, but works fine...
        Font f = UIManager.getFont("Label.font"); // NOI18N
        if (f != null) treeRenderer.setFont(f.deriveFont(f.getStyle()));
        // ---
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

        if (treeNode != null) {
            checkBox.setVisible(!persistentRenderer);
            setupCellRendererIcon(treeRenderer, treeNode.getIcon());
        } else {
            checkBox.setVisible(false);
            setupCellRendererIcon(treeRenderer, null);
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
