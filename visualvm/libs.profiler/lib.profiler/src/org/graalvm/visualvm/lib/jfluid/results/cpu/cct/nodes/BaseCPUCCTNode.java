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

package org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes;

import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class BaseCPUCCTNode implements RuntimeCPUCCTNode {

    private static final RuntimeCCTNode[] EMPTY_CHILDREN = new RuntimeCCTNode[0];

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /** Children nodes in the RuntimeCPUCCTNode tree. This field can have three different values depending on the
     * number of children:
     *   null if there are no children
     *   instance of RuntimeCPUCCTNode if there is exactly one child
     *   instance of RuntimeCPUCCTNode[] if there are multiple children
     * This is purely a memory consumption optimization, which typically saves about 50% of memory, since a lot of
     * RuntimeCPUCCTNode trees are a sequence of single-child nodes, and in such case we remove the need to
     * create a one-item array.
     */
    private Object children;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BaseCPUCCTNode */
    public BaseCPUCCTNode() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public RuntimeCCTNode[] getChildren() {
        if (children == null) {
            return EMPTY_CHILDREN;
        } else if (children instanceof RuntimeCPUCCTNode) {
            return new RuntimeCPUCCTNode[]{(RuntimeCPUCCTNode)children};
        }
        return (RuntimeCPUCCTNode[])children;
    }

    public void attachNodeAsChild(RuntimeCPUCCTNode node) {
        if (children == null) {
            children = node;
        } else if (children instanceof RuntimeCPUCCTNode) {
            children = new RuntimeCPUCCTNode[]{(RuntimeCPUCCTNode)children,node};
        } else {
            RuntimeCPUCCTNode[] ch = (RuntimeCPUCCTNode[]) children;
            RuntimeCPUCCTNode[] newChildren = new RuntimeCPUCCTNode[ch.length+1];
            System.arraycopy(ch, 0, newChildren, 0, ch.length);
            newChildren[newChildren.length-1] = node;
            children = newChildren;
        }
    }
}
