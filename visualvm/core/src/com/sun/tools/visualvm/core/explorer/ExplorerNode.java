/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.explorer;

import java.util.Set;
import javax.swing.Icon;
import javax.swing.tree.MutableTreeNode;

/**
 * A common interface to be implemented by all Explorer nodes.
 *
 * @author Jiri Sedlacek
 */
public interface ExplorerNode<T> extends MutableTreeNode, Comparable {

    /**
     * ExplorerNode will be appended as a last node to its parent.
     */
    public static final int POSITION_AT_THE_END = Integer.MAX_VALUE;


    /**
     * Returns the string to appear in the explorer.
     * 
     * @return string to appear in the explorer.
     */
    public String getName();

    /**
     * Returns the icon to appear in the explorer.
     * 
     * @return icon to appear in the explorer.
     */
    public Icon getIcon();

    /**
     * Adds new node to this node.
     * 
     * @param newChild node to be added to this node.
     */
    public void addNode(ExplorerNode newChild);

    /**
     * Adds new nodes to this node.
     * 
     * @param newChildren nodes to be added to this node.
     */
    public void addNodes(Set<ExplorerNode> newChildren);

    /**
     * Returns a general-purpose object stored in context of the node.
     * 
     * @return general-purpose object stored in context of the node.
     */
    public T getUserObject();

    /**
     * Returns preferred position of the node within other parent's nodes.
     * 
     * @return preferred position of the node within other parent's nodes.
     */
    public int getPreferredPosition();

}
