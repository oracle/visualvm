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

package org.graalvm.visualvm.lib.ui.memory;

import org.graalvm.visualvm.lib.jfluid.results.memory.*;
import javax.swing.tree.*;


/**
 * Implementation of TreeModel for Memory CCT Trees
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public class MemoryCCTTreeModel implements TreeModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private PresoObjAllocCCTNode root;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of MemoryCCTTreeModel */
    public MemoryCCTTreeModel(PresoObjAllocCCTNode root) {
        this.root = root;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object getChild(Object obj, int index) {
        if (obj == null) {
            return null;
        }

        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode) obj;

        return node.getChild(index);
    }

    public int getChildCount(Object obj) {
        if (obj == null) {
            return -1;
        }

        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode) obj;

        return node.getNChildren();
    }

    public int getIndexOfChild(Object parentObj, Object childObj) {
        if ((parentObj == null) || (childObj == null)) {
            return -1;
        }

        PresoObjAllocCCTNode parent = (PresoObjAllocCCTNode) parentObj;
        PresoObjAllocCCTNode child = (PresoObjAllocCCTNode) childObj;

        return parent.getIndexOfChild(child);
    }

    public boolean isLeaf(Object obj) {
        if (obj == null) {
            return true;
        }

        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode) obj;

        return (node.getNChildren() == 0);
    }

    public Object getRoot() {
        return root;
    }

    public void addTreeModelListener(javax.swing.event.TreeModelListener treeModelListener) {
    }

    public void removeTreeModelListener(javax.swing.event.TreeModelListener treeModelListener) {
    }

    // --------------------------------------------------------------  

    // TreeModel interface methods that we don't implement
    public void valueForPathChanged(javax.swing.tree.TreePath treePath, Object obj) {
    }
}
