/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.java.impl;

import com.sun.tools.visualvm.heapviewer.java.ClassNode;
import com.sun.tools.visualvm.heapviewer.java.ClassesContainer;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaDiffClassesProvider {
    
    static HeapViewerNode[] getDiffHeapClasses(HeapViewerNode parent, final Heap heap1, List<ClassNode> diffClasses, boolean retained, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        NodesComputer<ClassNode> computer = new NodesComputer<ClassNode>(diffClasses.size(), UIThresholds.MAX_TOPLEVEL_CLASSES) {
            protected boolean sorts(DataType dataType) {
                return true;
            }
            protected HeapViewerNode createNode(ClassNode javaClass) {
                return javaClass;
            }
            protected ProgressIterator<ClassNode> objectsIterator(int index, Progress progress) {
                Iterator<ClassNode> iterator = diffClasses.listIterator(index);
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return JavaClassesProvider.Classes_Messages.getMoreNodesString(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return JavaClassesProvider.Classes_Messages.getSamplesContainerString(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return JavaClassesProvider.Classes_Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
            }
        };
        
        HeapViewerNode[] nodes = computer.computeNodes(parent, heap1, viewID, viewFilter, dataTypes, sortOrders, progress);
        return nodes.length == 0 ? new HeapViewerNode[] { new TextNode(JavaClassesProvider.Classes_Messages.getNoClassesString(viewFilter)) } : nodes;
    }
    
    static HeapViewerNode[] getDiffHeapPackages(HeapViewerNode parent, Heap heap1, List<ClassNode> diffClasses, boolean retained, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        List<HeapViewerNode> nodes = new ArrayList();
        Map<String, DiffPackageNode> packages = new HashMap();
        
        for (ClassNode cls : diffClasses) {
            String className = cls.getName();
            int nameIdx = className.lastIndexOf('.'); // NOI18N
            if (nameIdx == -1) {
                if (viewFilter == null || viewFilter.passes(cls, heap1)) nodes.add(cls);
            } else {
                if (viewFilter != null && !viewFilter.passes(cls, heap1)) continue;
                
                String pkgName = className.substring(0, nameIdx);
                DiffPackageNode node = packages.get(pkgName);
                if (node == null) {
                    node = new DiffPackageNode(pkgName, retained);
                    nodes.add(node);
                    packages.put(pkgName, node);
                }
                node.add(cls, heap1);
            }
        }
        
        return nodes.isEmpty() ? new HeapViewerNode[] { new TextNode(JavaClassesProvider.Classes_Messages.getNoPackagesString(viewFilter)) } :
                                 nodes.toArray(HeapViewerNode.NO_NODES);
    }
    
    
    static List<ClassNode> createDiffClasses(Heap h1, Heap h2, final boolean retained) {
        if (retained) {
            if (!DataType.RETAINED_SIZE.valuesAvailable(h1))
                DataType.RETAINED_SIZE.computeValuesImmediately(h1);
            
            if (!DataType.RETAINED_SIZE.valuesAvailable(h2))
                DataType.RETAINED_SIZE.computeValuesImmediately(h2);
        }
        
        Map<String, DiffClassNode> classes = new HashMap();
        
        List<JavaClass> classes1 = h1.getAllClasses();
        for (JavaClass jc1 : classes1) {
            String id1 = jc1.getName();
            DiffClassNode djc1 = classes.get(id1);
            if (djc1 == null) {
                djc1 = DiffClassNode.own(jc1, retained);
                classes.put(id1, djc1);
            } else {
                djc1.mergeOwn(jc1);
            }
        }
        
        List<JavaClass> classes2 = h2.getAllClasses();
        for (JavaClass jc2 : classes2) {
            String id2 = jc2.getName();
            DiffClassNode djc2 = classes.get(id2);
            if (djc2 == null) {
                djc2 = DiffClassNode.external(new ExternalJavaClass(jc2, retained), retained);
                classes.put(id2, djc2);
            } else {
                djc2.mergeExternal(jc2);
            }
        }
        
        return new ArrayList(classes.values());
    }
    
    private static class DiffClassNode extends ClassNode {
        
        private final boolean trackRetained;
        
        private int instancesCount;
        private long ownSize;
        private long retainedSize;
        
        
        static DiffClassNode own(JavaClass ownClass, boolean trackRetained) {
            DiffClassNode dClass = new DiffClassNode(ownClass, trackRetained);
            dClass.mergeOwn(ownClass);
            return dClass;
        }
        
        static DiffClassNode external(JavaClass externalClass, boolean trackRetained) {
            DiffClassNode dClass = new DiffClassNode(externalClass, trackRetained);
            dClass.mergeExternal(externalClass);
            return dClass;
        }
        
        
        private DiffClassNode(JavaClass jClass, boolean trackRetained) {
            super(jClass);
            
            this.trackRetained = trackRetained;
            
            setChildren(NO_NODES);
        }
        
        
        void mergeOwn(JavaClass ownClass) {
            instancesCount += ownClass.getInstancesCount();
            ownSize += ownClass.getAllInstancesSize();
            if (trackRetained) retainedSize += ownClass.getRetainedSizeByClass();
        }
        
        void mergeExternal(JavaClass externalClass) {
            instancesCount -= externalClass.getInstancesCount();
            ownSize -= externalClass.getAllInstancesSize();
            if (trackRetained) retainedSize -= externalClass.getRetainedSizeByClass();
        }
        
        
        public int getInstancesCount() {
            return instancesCount;
        }

        public long getOwnSize() {
            return ownSize;
        }

        public long getRetainedSize(Heap heap) {
            return trackRetained ? retainedSize : DataType.RETAINED_SIZE.getNotAvailableValue();
        }
        
        
        public boolean isLeaf() {
            return true;
        }
        
        public ClassNode createCopy() {
            return null;
        }
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.NAME) return getName();
            if (type == DataType.COUNT) return getInstancesCount();
            if (type == DataType.OWN_SIZE) return getOwnSize();
            if (type == DataType.RETAINED_SIZE) return getRetainedSize(heap);

            if (type == DataType.CLASS) return getJavaClass();

            return type.getNotAvailableValue();
        }
        
    }
    
    private static class DiffPackageNode extends ClassesContainer.Nodes {
        
        private final boolean trackRetained;
        
        DiffPackageNode (String name, boolean trackRetained) {
            super(name);
            
            this.trackRetained = trackRetained;
            
            count = 0;
            ownSize = 0;
            if (trackRetained) retainedSize = 0;
        }
        
        public void add(ClassNode item, Heap heap) {
            items.add(item);

            count += getCount(item, heap);
            ownSize += getOwnSize(item, heap);
            if (trackRetained) retainedSize += getRetainedSize(item, heap);
        }
        
    }
    
    private static class ExternalJavaClass implements JavaClass {
        
        private final long allInstancesSize;
        private final boolean isArray;
        private final int instanceSize;
        private final int instancesCount;
        private final long retainedSizeByClass;
        private final long javaClassId;
        private final String name;
        
        ExternalJavaClass(JavaClass javaClass, boolean retained) {
            allInstancesSize = javaClass.getAllInstancesSize();
            isArray = javaClass.isArray();
            instanceSize = javaClass.getInstanceSize();
            instancesCount = javaClass.getInstancesCount();
            retainedSizeByClass = retained ? javaClass.getRetainedSizeByClass() : DataType.RETAINED_SIZE.getNotAvailableValue();
            javaClassId = javaClass.getJavaClassId();
            name = javaClass.getName();
        }

        @Override
        public Object getValueOfStaticField(String string) {
            return null;
        }

        @Override
        public long getAllInstancesSize() {
            return allInstancesSize;
        }

        @Override
        public boolean isArray() {
            return isArray;
        }

        @Override
        public Instance getClassLoader() {
            return null;
        }

        @Override
        public List getFields() {
            return null;
        }

        @Override
        public int getInstanceSize() {
            return instanceSize;
        }

        @Override
        public List getInstances() {
            return null;
        }

        @Override
        public Iterator getInstancesIterator() {
            return null;
        }

        @Override
        public int getInstancesCount() {
            return instancesCount;
        }

        @Override
        public long getRetainedSizeByClass() {
            return retainedSizeByClass;
        }

        @Override
        public long getJavaClassId() {
            return javaClassId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List getStaticFieldValues() {
            return null;
        }

        @Override
        public Collection getSubClasses() {
            return null;
        }

        @Override
        public JavaClass getSuperClass() {
            // NOTE: breaks Hierarchy details, better provide synthetic <unknown class>
            return null;
        }
        
    }
    
}
