/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.java;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import com.sun.tools.visualvm.heapviewer.model.ContainerNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;

/**
 *
 * @author Jiri Sedlacek
 */
public final class InstancesContainer {
    
    public static class Objects extends ContainerNode<Instance> {
        
        private final JavaClass javaClass;
    
        public Objects(String name, JavaClass javaClass) {
            this(name, javaClass, UIThresholds.MAX_CONTAINER_INSTANCES);
        }

        public Objects(String name, JavaClass javaClass, int maxItems) {
            super(name, maxItems);
            this.javaClass = javaClass;
        }

        protected int getCount(Instance item, Heap heap) {
            return 1;
        }

        protected long getOwnSize(Instance item, Heap heap) {
            return item.getSize();
        }

        protected long getRetainedSize(Instance item, Heap heap) {
            return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
                   item.getRetainedSize() : DataType.RETAINED_SIZE.getNotAvailableValue();
        }

        protected InstanceNode createNode(Instance instance) {
            return new InstanceNode(instance);
        }

        protected String getMoreNodesString(String moreNodesCount)  {
            return InstancesContainer.getMoreNodesString(moreNodesCount);
        }
        
        protected String getSamplesContainerString(String objectsCount)  {
            return InstancesContainer.getSamplesContainerString(objectsCount);
        }
        
        protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return InstancesContainer.getNodesContainerString(firstNodeIdx, lastNodeIdx);
        }
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.CLASS) return javaClass;

            return super.getValue(type, heap);
        }

    }
    
    
    public static class Nodes extends ContainerNode.Nodes<InstanceNode> {
        
        private final JavaClass javaClass;
        
        public Nodes(String name, JavaClass javaClass) {
            this(name, javaClass, UIThresholds.MAX_CONTAINER_INSTANCES);
        }

        public Nodes(String name, JavaClass javaClass, int maxItems) {
            super(name, maxItems);
            this.javaClass = javaClass;
        }
        
        protected String getMoreNodesString(String moreNodesCount)  {
            return InstancesContainer.getMoreNodesString(moreNodesCount);
        }
        
        protected String getSamplesContainerString(String objectsCount)  {
            return InstancesContainer.getSamplesContainerString(objectsCount);
        }
        
        protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return InstancesContainer.getNodesContainerString(firstNodeIdx, lastNodeIdx);
        }
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.CLASS) return javaClass;

            return super.getValue(type, heap);
        }
        
    }
    
    
    private InstancesContainer() {}
    
    
    private static String getMoreNodesString(String moreNodesCount)  {
        return "<another " + moreNodesCount + " instances left>";
    }
    
    private static String getSamplesContainerString(String objectsCount)  {
        return "<sample " + objectsCount + " instances>";
    }
    
    private static String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
        return "<instances " + firstNodeIdx + "-" + lastNodeIdx + ">";
    }
    
}
