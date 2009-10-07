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

import java.util.Collection;
import java.util.LinkedList;
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
        setChecked(false);
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
        if (checked == true) {
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
        LinkedList changedNodes = new LinkedList();
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
        LinkedList changedNodes = new LinkedList();
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
        LinkedList changedNodes = new LinkedList();
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

        if ((parent != null) && parent instanceof CheckTreeNode) {
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
