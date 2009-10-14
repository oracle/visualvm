/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.netbeans.modules.profiler.heapwalk.model;

import java.util.List;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.Value;

/**
 *
 * @author Jiri Sedlacek
 */
public class InstancesContainerNode extends AbstractHeapWalkerNode {

    private final String name;
    private List<Value> childrenValues;
    private final List<Instance> instances;


    public InstancesContainerNode(String name, HeapWalkerNode parent, List<Value> childrenValues,
                                  List<Instance> instances) {
        super(parent);
        this.name = name;
        this.childrenValues = childrenValues;
        this.instances = instances;
    }


    public List<Instance> getInstances() {
        return instances;
    }


    protected String computeName() {
        return name;
    }

    protected String computeType() {
        return "-"; // NOI18N
    }

    protected String computeValue() {
        return "-"; // NOI18N
    }

    protected String computeSize() {
        return "-"; // NOI18N
    }

    protected String computeRetainedSize() {
        return "-"; // NOI18N
    }

    protected Icon computeIcon() {
        return BrowserUtils.ICON_INSTANCE;
    }

    protected HeapWalkerNode[] computeChildren() {
        HeapWalkerNode[] nodes = new HeapWalkerNode[childrenValues.size()];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = HeapWalkerNodeFactory.createReferenceNode(childrenValues.get(i), this);
        childrenValues = null;
        return nodes;
    }

}
