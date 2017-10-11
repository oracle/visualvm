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
package com.sun.tools.visualvm.truffle.heapwalker.javascript;

import com.sun.tools.visualvm.truffle.heapwalker.AbstractObjectsProvider;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectsContainer;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.TextNode;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleLanguageHeapFragment;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.netbeans.modules.profiler.heapwalker.v2.utils.NodesComputer;

/**
 *
 * @author Jiri Sedlacek
 */
public class JavaScriptObjectsProvider extends AbstractObjectsProvider {
    
    static final String JS_LANG_ID = "com.oracle.truffle.js.runtime.builtins.JSClass";
    private static DynamicObjectsContainer PLACEHOLDER = new DynamicObjectsContainer("", 0);

    public static HeapWalkerNode[] getAllObjects(HeapWalkerNode parent, HeapContext context, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {
//        long start = System.currentTimeMillis();

        final TruffleLanguageHeapFragment fragment = (TruffleLanguageHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
//        if (aggregation == 0) {
            NodesComputer<DynamicObject> computer = new NodesComputer<DynamicObject>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(DynamicObject dobject) {
                    return new JavaScriptNodes.JavaScriptDynamicObjectNode(dobject, heap);
                }
                protected Iterator<DynamicObject> objectsIterator(int index) {
                    Iterator<DynamicObject> dobjects = fragment.getDynamicObjectsIterator(JS_LANG_ID);
                    for (int i = 0; i < index; i++) dobjects.next();
                    return dobjects;
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return "<another " + moreNodesCount + " objects left>";
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return "<sample " + objectsCount + " objects>";
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return "<objects " + firstNodeIdx + "-" + lastNodeIdx + ">";
                }
            };

            return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
//        }
        
//        List<HeapWalkerNode> nodes = new ArrayList();
//        Map<String, DynamicObjectsContainer> types = new HashMap();
//
//        Map<Instance, String> shapes = new HashMap();
//        DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
//        SortOrder sortOrder = sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
//        SortedNodesBuffer objects = new SortedNodesBuffer(100, dataType, sortOrder, heap, parent) {
//            protected String getMoreItemsString(String formattedNodesLeft) {
//                return "<another " + formattedNodesLeft + " objects left>";
//            }
//        };
//        Iterator<DynamicObject> instancesI = fragment.getDynamicObjectsIterator();
//        while (instancesI.hasNext()) {
//            DynamicObject dobject = instancesI.next();
//            Instance shape = dobject.getShape();
//            String type = shapes.get(shape);
//            if (type == null) {
//                type = DetailsSupport.getDetailsString(shape, heap);
//                shapes.put(shape, type);
//            }
//
//            DynamicObjectsContainer typeNode = types.get(type);
//            if (typeNode == null) {
//                String langid = dobject.getLanguageId().getName();
//                if (JS_LANG_ID.equals(langid)) {
//                    if (aggregation == 0) {
//                        typeNode = PLACEHOLDER;
//                    } else {
//                        typeNode = new JavaScriptNodes.JavaScriptDynamicObjectsContainer(type, 100);
//                        nodes.add(typeNode);
//                    }
//                    types.put(type, typeNode);
//                }
//            }
//            if (typeNode != null) {
//                if (aggregation == 0) {
//                     objects.add(new JavaScriptNodes.JavaScriptDynamicObjectNode(dobject, heap));
//                } else {
//                    typeNode.add(dobject, heap);
//                }
//            }
//        }
//        
////        System.err.println(">>> ALL JS objects X computed in " + (System.currentTimeMillis() - start));
//        
//        if (aggregation == 0) return objects.getNodes();
//        else return nodes.toArray(HeapWalkerNode.NO_NODES);
    }
    
    
    public static HeapWalkerNode[] getDominators(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapWalkerNode[] { new TextNode("<Retained sizes not computed yet>") };
        
        List<HeapWalkerNode> nodes = new ArrayList();
        
        int maxSearchInstances = 10000;
        
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(maxSearchInstances);
        Iterator<Instance> searchInstancesIt = searchInstances.iterator();
        Map<String, DynamicObjectsContainer> types = new HashMap();
        Set<Instance> dominators;
        
        while (searchInstancesIt.hasNext()) {
            if (!DynamicObject.isDynamicObject(searchInstancesIt.next())) {
                searchInstancesIt.remove();
            }
        }
        dominators = getDominatorRoots(searchInstances);

        for (Instance dominator : dominators) {
            DynamicObject dobject = new DynamicObject(dominator);
            String type = dobject.getType(heap);

            DynamicObjectsContainer typeNode = types.get(type);
            if (typeNode == null) {
                String langid = dobject.getLanguageId().getName();
                if (JS_LANG_ID.equals(langid)) {
                    if (aggregation == 0) {
                        typeNode = PLACEHOLDER;
                    } else {
                        typeNode = new JavaScriptNodes.JavaScriptDynamicObjectsContainer(type, Integer.MAX_VALUE);
                        nodes.add(typeNode);
                    }
                    types.put(type, typeNode);
                }
            }
            if (typeNode != null) {
                if (aggregation == 0) {
                    nodes.add(new JavaScriptNodes.JavaScriptDynamicObjectNode(dobject, heap));
                } else {
                    typeNode.add(dobject, heap);
                }
            }
        }
        
        if (nodes.isEmpty()) nodes.add(new TextNode("<No dominators found>"));
        return nodes.toArray(HeapWalkerNode.NO_NODES);
    }
    
    public static HeapWalkerNode[] getGCRoots(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {
        List<HeapWalkerNode> nodes = new ArrayList();
        
        Iterator<Instance> gcroots = heap.getGCRoots().iterator();
        
        Map<String, DynamicObjectsContainer> types = new HashMap();

        while (gcroots.hasNext()) {
            Instance gcroot = ((GCRoot)gcroots.next()).getInstance();
            if (!DynamicObject.isDynamicObject(gcroot)) continue;

            DynamicObject dobject = new DynamicObject(gcroot);
            String type = dobject.getType(heap);

            DynamicObjectsContainer typeNode = types.get(type);
            if (typeNode == null) {
                String langid = dobject.getLanguageId().getName();
                if (JS_LANG_ID.equals(langid)) {
                    if (aggregation == 0) {
                        typeNode = PLACEHOLDER;
                    } else {
                        typeNode = new JavaScriptNodes.JavaScriptDynamicObjectsContainer(type, Integer.MAX_VALUE);
                        nodes.add(typeNode);
                    }
                    types.put(type, typeNode);
                }
            }
            if (typeNode != null) {
                if (aggregation == 0) {
                    nodes.add(new JavaScriptNodes.JavaScriptDynamicObjectNode(dobject, heap));                
                } else {
                    typeNode.add(dobject, heap);
                }
            }
        }
        
        if (nodes.isEmpty()) nodes.add(new TextNode("<No GC roots found>"));
        return nodes.toArray(HeapWalkerNode.NO_NODES);
    }
}
