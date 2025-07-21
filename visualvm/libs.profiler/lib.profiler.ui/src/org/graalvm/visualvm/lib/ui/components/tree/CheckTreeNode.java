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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


/**
 *
 * @author Jiri Sedlacek
 */
public class CheckTreeNode extends DefaultMutableTreeNode {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int STATE_CHECKED = 1;
    public static final int STATE_UNCHECKED = 2;
    public static final int STATE_PARTIALLY_CHECKED = 4;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int checkState = STATE_UNCHECKED;
    private Icon icon;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------

    // Creates new CheckTreeNode, no user object, initially unchecked
    public CheckTreeNode() {
        this(null);
    }

    // Creates new CheckTreeNode with user object, initially unchecked
    public CheckTreeNode(Object userObject) {
        this(userObject, null);
    }

    /**
     * Creates new CheckTreeNode with given icon, initially unchecked
     */
    public CheckTreeNode(Object userObject, final Icon icon) {
        super(userObject);
        checkState = STATE_UNCHECKED;
        this.icon = icon;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // Returns CheckTreeNode's check state - STATE_CHECKED, STATE_UNCHECKED or STATE_PARTIALLY_CHECKED
    public int getCheckState() {
        return checkState;
    }

    // Sets the CheckTreeNode to be fully checked or fully unchecked
    // Returns Collection of leaf nodes changed by this operation
    public Collection setChecked(boolean checked) {
        if (checked) {
            return setFullyChecked();
        } else {
            return setUnchecked();
        }
    }

    // Check if CheckTreeNode is fully checked
    public boolean isFullyChecked() {
        return getCheckState() == STATE_CHECKED;
    }

    /**
     * The icon property getter
     */
    public Icon getIcon() {
        return icon;
    }

    // Check if CheckTreeNode is partially checked
    public boolean isPartiallyChecked() {
        return getCheckState() == STATE_PARTIALLY_CHECKED;
    }

    // Toggles CheckTreeNode's check state to next state
    // Returns Collection of leaf nodes changed by this operation
    public Collection toggleState() {
        if (getCheckState() == STATE_CHECKED) {
            return setUnchecked();
        } else {
            return setFullyChecked();
        }
    }

    protected Collection setPartiallyChecked() {
        Set changedNodes = new HashSet();
        changedNodes.add(this);

        // Check if change is needed
        if (checkState == STATE_PARTIALLY_CHECKED) {
            return changedNodes;
        }

        // Update checkState of this node
        checkState = STATE_PARTIALLY_CHECKED;

        // Update checkState of parent
        TreeNode parent = getParent();

        if ((parent != null) && parent instanceof CheckTreeNode) {
            changedNodes.addAll(((CheckTreeNode) parent).setPartiallyChecked());
        }

        // Return Collection of leaf nodes changed by this operation
        return changedNodes;
    }

    // ---------------------------------------------------------------------------

    // --- Private implementation ------------------------------------------------
    private Collection setFullyChecked() {
        Set changedNodes = new HashSet();
        changedNodes.add(this);

        // Check if change is needed
        if (checkState == STATE_CHECKED) {
            return changedNodes;
        }

        // Update checkState of this node
        checkState = STATE_CHECKED;

        // Update checkState of all children if any
        if (!isLeaf()) {
            for (int i = 0; i < getChildCount(); i++) {
                TreeNode node = getChildAt(i);

                if (node instanceof CheckTreeNode) {
                    changedNodes.addAll(((CheckTreeNode) node).setFullyChecked());
                }
            }
        }

        // Update checkState of parent
        TreeNode parent = getParent();

        if ((parent != null) && parent instanceof CheckTreeNode) {
            if (areSiblingsFullyChecked()) {
                changedNodes.addAll(((CheckTreeNode) parent).setFullyChecked());
            } else {
                changedNodes.addAll(((CheckTreeNode) parent).setPartiallyChecked());
            }
        }

        // Return Collection of leaf nodes changed by this operation
        return changedNodes;
    }

    private Collection setUnchecked() {
        Set changedNodes = new HashSet();
        changedNodes.add(this);

        // Check if change is needed
        if (checkState == STATE_UNCHECKED) {
            return changedNodes;
        }

        // Update checkState of this node
        checkState = STATE_UNCHECKED;

        // Update checkState of all children if any
        if (!isLeaf()) {
            for (int i = 0; i < getChildCount(); i++) {
                TreeNode node = getChildAt(i);

                if (node instanceof CheckTreeNode) {
                    changedNodes.addAll(((CheckTreeNode) node).setUnchecked());
                }
            }
        }

        // Update checkState of parent
        TreeNode parent = getParent();

        if (parent instanceof CheckTreeNode) {
            if (areSiblingsUnchecked()) {
                changedNodes.addAll(((CheckTreeNode) parent).setUnchecked());
            } else {
                changedNodes.addAll(((CheckTreeNode) parent).setPartiallyChecked());
            }
        }

        // Return Collection of leaf nodes changed by this operation
        return changedNodes;
    }

    private boolean areSiblingsFullyChecked() {
        TreeNode parent = getParent();

        for (int i = 0; i < parent.getChildCount(); i++) {
            TreeNode node = parent.getChildAt(i);

            if (node == this) {
                continue;
            }

            if (!(node instanceof CheckTreeNode) || (((CheckTreeNode) node).getCheckState() != STATE_CHECKED)) {
                return false;
            }
        }

        return true;
    }

    private boolean areSiblingsUnchecked() {
        TreeNode parent = getParent();

        for (int i = 0; i < parent.getChildCount(); i++) {
            TreeNode node = parent.getChildAt(i);

            if (node == this) {
                continue;
            }

            if (!(node instanceof CheckTreeNode) || (((CheckTreeNode) node).getCheckState() != STATE_UNCHECKED)) {
                return false;
            }
        }

        return true;
    }

    // ---------------------------------------------------------------------------  
}
