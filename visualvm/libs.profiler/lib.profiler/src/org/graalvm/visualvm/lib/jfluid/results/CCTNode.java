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

package org.graalvm.visualvm.lib.jfluid.results;

import java.util.Collections;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;


/**
 * This interface must be implemented by every CCT node.
 *
 * @author Jiri Sedlacek
 */
public abstract class CCTNode implements TreeNode {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract CCTNode getChild(int index);

    public abstract CCTNode[] getChildren();

    public abstract int getIndexOfChild(Object child);

    public abstract int getNChildren();

    public abstract CCTNode getParent();

    //public boolean hasChildren();


    // --- Filtering support ---

    private boolean filtered;

    public CCTNode createFilteredNode() { return null; }

    protected void setFilteredNode() { filtered = true; }

    public boolean isFiltered() { return filtered; }

    public void merge(CCTNode node) {}

    // ---


    //--- TreeNode adapter ---
    public Enumeration<CCTNode> children() {
        final CCTNode[] _children = getChildren();
        final int _childrenCount = _children == null ? 0 : _children.length;

        if (_childrenCount == 0) return Collections.emptyEnumeration();

        return new Enumeration<CCTNode>() {
            private int index = 0;
            public boolean hasMoreElements() { return index < _childrenCount; }
            public CCTNode nextElement()     { return _children[index++]; }
        };
    }

    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public int getIndex(TreeNode node) {
        return getIndexOfChild(node);
    }

    public int getChildCount() {
        return getNChildren();
    }
    
    public TreeNode getChildAt(int index) {
        return getChild(index);
    }
    //---
    
    
    public static interface FixedPosition {}
    
    public static interface AlwaysFirst extends FixedPosition {}
    
    public static interface AlwaysLast extends FixedPosition {}
    
    public static interface DoNotSortChildren {}
    
}
