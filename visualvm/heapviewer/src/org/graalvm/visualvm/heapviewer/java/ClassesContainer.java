/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java;

import org.graalvm.visualvm.heapviewer.model.ContainerNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ClassesContainer_MoreNodes=<another {0} classes left>",
    "ClassesContainer_SamplesContainer=<sample {0} classes>",
    "ClassesContainer_NodesContainer=<classes {0}-{1}>"
})
public final class ClassesContainer {
    
    private ClassesContainer() {}
    
    
    private static String getMoreNodesString(String moreNodesCount)  {
        return Bundle.ClassesContainer_MoreNodes(moreNodesCount);
    }
    
    private static String getSamplesContainerString(String objectsCount)  {
        return Bundle.ClassesContainer_SamplesContainer(objectsCount);
    }
    
    private static String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
        return Bundle.ClassesContainer_NodesContainer(firstNodeIdx, lastNodeIdx);
    }
    
    
    public static class Objects extends ContainerNode<JavaClass> {
    
        public Objects(String name) {
            this(name, UIThresholds.MAX_CONTAINER_CLASSES);
        }

        public Objects(String name, int maxItems) {
            super(name, maxItems);
        }

        protected int getCount(JavaClass item, Heap heap) {
            return item.getInstancesCount();
        }

        protected long getOwnSize(JavaClass item, Heap heap) {
            return item.getAllInstancesSize();
        }

        protected long getRetainedSize(JavaClass item, Heap heap) {
            return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
                   item.getRetainedSizeByClass(): DataType.RETAINED_SIZE.getNotAvailableValue();
        }

        protected ClassNode createNode(JavaClass javaClass) {
            return new ClassNode(javaClass);
        }
        
        protected String getMoreNodesString(String moreNodesCount)  {
            return ClassesContainer.getMoreNodesString(moreNodesCount);
        }
        
        protected String getSamplesContainerString(String objectsCount)  {
            return ClassesContainer.getSamplesContainerString(objectsCount);
        }
        
        protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return ClassesContainer.getNodesContainerString(firstNodeIdx, lastNodeIdx);
        }

    }
    
    
    public static class Nodes extends ContainerNode.Nodes<ClassNode> {
        
        public Nodes(String name) {
            this(name, UIThresholds.MAX_CONTAINER_CLASSES);
        }

        public Nodes(String name, int maxItems) {
            super(name, maxItems);
        }
        
        
        protected String getMoreNodesString(String moreNodesCount)  {
            return ClassesContainer.getMoreNodesString(moreNodesCount);
        }
        
        protected String getSamplesContainerString(String objectsCount)  {
            return ClassesContainer.getSamplesContainerString(objectsCount);
        }
        
        protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return ClassesContainer.getNodesContainerString(firstNodeIdx, lastNodeIdx);
        }
        
    }
    
    
    public static class ContainerNodes extends ContainerNode.Nodes<InstancesContainer.Objects> {
        
        public ContainerNodes(String name) {
            this(name, Integer.MAX_VALUE);
        }

        public ContainerNodes(String name, int maxItems) {
            super(name, maxItems);
        }
        
    }
    
}
