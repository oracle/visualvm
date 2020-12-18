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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.GCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.heapviewer.java.ClassesContainer;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstancesContainer;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class JavaClassesProvider {
    
    @NbBundle.Messages({
        "Classes_Messages_MoreNodes=<another {0} classes left>",
        "Classes_Messages_SamplesContainer=<sample {0} classes>",
        "Classes_Messages_NodesContainer=<classes {0}-{1}>",
        "Classes_Messages_NoClasses=<no classes>",
        "Classes_Messages_NoClassesFilter=<no classes matching the filter>",
        "Classes_Messages_NoPackages=<no packages>",
        "Classes_Messages_NoPackagesFilter=<no packages matching the filter>"
    })
    static final class Classes_Messages {
        static String getMoreNodesString(String moreNodesCount)  {
            return Bundle.Classes_Messages_MoreNodes(moreNodesCount);
        }
        static String getSamplesContainerString(String objectsCount)  {
            return Bundle.Classes_Messages_SamplesContainer(objectsCount);
        }
        static String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return Bundle.Classes_Messages_NodesContainer(firstNodeIdx, lastNodeIdx);
        }
        static String getNoClassesString(HeapViewerNodeFilter viewFilter) {
            return viewFilter == null ? Bundle.Classes_Messages_NoClasses() : Bundle.Classes_Messages_NoClassesFilter();
        }
        static String getNoPackagesString(HeapViewerNodeFilter viewFilter) {
            return viewFilter == null ? Bundle.Classes_Messages_NoPackages() : Bundle.Classes_Messages_NoPackagesFilter();
        }
    }
    
    public static HeapViewerNode[] getHeapClasses(HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        NodesComputer<JavaClass> computer = new NodesComputer<JavaClass>(heap.getAllClasses().size(), UIThresholds.MAX_TOPLEVEL_CLASSES) {
            protected boolean sorts(DataType dataType) {
                return true;
            }
            protected HeapViewerNode createNode(JavaClass javaClass) {
                return new ClassNode(javaClass);
            }
            protected ProgressIterator<JavaClass> objectsIterator(int index, Progress progress) {
                Iterator<JavaClass> iterator = heap.getAllClasses().listIterator(index);
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Classes_Messages.getMoreNodesString(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Classes_Messages.getSamplesContainerString(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Classes_Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
            }
        };
//        System.err.println(">>> COMPUTED " + java.util.Arrays.toString(computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress)));
        HeapViewerNode[] nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        return nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Classes_Messages.getNoClassesString(viewFilter)) } : nodes;
    }
    

    public static HeapViewerNode[] getHeapPackages(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        List<HeapViewerNode> nodes = new ArrayList();
        Map<String, ClassesContainer.Objects> packages = new HashMap();
        
        Thread worker = Thread.currentThread();
        
        List<JavaClass> classes = heap.getAllClasses();
        for (JavaClass cls : classes) {
            String className = cls.getName();
            int nameIdx = className.lastIndexOf('.');
            if (nameIdx == -1) {
                ClassNode clsn = new ClassNode(cls);
                if (viewFilter == null || viewFilter.passes(clsn, heap)) nodes.add(clsn);
            } else {
                if (viewFilter != null && !viewFilter.passes(new ClassNode(cls), heap)) continue;
                
                String pkgName = className.substring(0, nameIdx);
                ClassesContainer.Objects node = packages.get(pkgName);
                if (node == null) {
                    node = new ClassesContainer.Objects(pkgName);
                    nodes.add(node);
                    packages.put(pkgName, node);
                }
                node.add(cls, heap);
            }
            if (worker.isInterrupted()) throw new InterruptedException();
        }
        
        return nodes.isEmpty() ? new HeapViewerNode[] { new TextNode(Classes_Messages.getNoPackagesString(viewFilter)) } :
                                 nodes.toArray(HeapViewerNode.NO_NODES);
    }
    

    @NbBundle.Messages({
        "GCRoots_Messages_MoreNodes=<another {0} GC roots left>",
        "GCRoots_Messages_SamplesContainer=<sample {0} GC roots>",
        "GCRoots_Messages_NodesContainer=<GC roots {0}-{1}>",
        "GCRoots_Messages_NoGCRoots=<no GC roots>",
        "GCRoots_Messages_NoGCRootsFilter=<no GC roots matching the filter>"
    })
    private static final class GCRoots_Messages {
        private static String getMoreNodesString(String moreNodesCount)  {
            return Bundle.GCRoots_Messages_MoreNodes(moreNodesCount);
        }
        private static String getSamplesContainerString(String objectsCount)  {
            return Bundle.GCRoots_Messages_SamplesContainer(objectsCount);
        }
        private static String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return Bundle.GCRoots_Messages_NodesContainer(firstNodeIdx, lastNodeIdx);
        }
        private static String getNoItemsString(HeapViewerNodeFilter viewFilter) {
            return viewFilter == null ? Bundle.GCRoots_Messages_NoGCRoots() : Bundle.GCRoots_Messages_NoGCRootsFilter();
        }
    }
    
    public static HeapViewerNode[] getHeapGCRoots(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) throws InterruptedException {
        final Collection<GCRoot> gcroots = heap.getGCRoots();
        final List<Instance> gcrootInstances = gcroots.stream()
                .map(GCRoot::getInstance)
                .distinct()
                .collect(Collectors.toList());
        
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(gcrootInstances.size(), UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Instance gcRootInstance) {
                    return new InstanceNode(gcRootInstance);
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                    Iterator<Instance> iterator = gcrootInstances.listIterator(index);
                    return new ProgressIterator(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return GCRoots_Messages.getMoreNodesString(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return GCRoots_Messages.getSamplesContainerString(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return GCRoots_Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                }
            };
            HeapViewerNode[] nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
            return nodes.length == 0 ? new HeapViewerNode[] { new TextNode(GCRoots_Messages.getNoItemsString(viewFilter)) } : nodes;
        } else {
            if (viewFilter != null) {
                Iterator<Instance> gcrootsI = gcrootInstances.iterator();
                while (gcrootsI.hasNext())
                    if (!viewFilter.passes(new InstanceNode(gcrootsI.next()), heap))
                        gcrootsI.remove();
            }
            
            if (aggregation == 3) {
                List<GCTypeNode> tnodes = new ArrayList();
                Map<String, GCTypeNode> types = new HashMap();
                for (Instance instance : gcrootInstances) {
                    Collection<GCRoot> igcroots = (Collection<GCRoot>)heap.getGCRoots(instance);
                    Set<String> typeSet = new HashSet();
                    for (GCRoot gcroot : igcroots) {
                        String tname = gcroot.getKind();
                        if (typeSet.add(tname)) {
                            GCTypeNode tnode = types.get(tname);
                            if (tnode == null) {
                                tnode = new GCTypeNode(tname);
                                tnodes.add(tnode);
                                types.put(tname, tnode);
                            }
                            tnode.add(gcroot.getInstance(), heap);
                        }
                    }
                }
                return tnodes.isEmpty() ? new HeapViewerNode[] { new TextNode(GCRoots_Messages.getNoItemsString(viewFilter)) } :
                                          tnodes.toArray(HeapViewerNode.NO_NODES);
            } else {
                List<InstancesContainer.Objects> cnodes = new ArrayList();
                Map<String, InstancesContainer.Objects> classes = new HashMap();
                for (Instance instance : gcrootInstances) {
                    JavaClass javaClass = instance.getJavaClass();
                    String className = javaClass.getName();
                    InstancesContainer.Objects cnode = classes.get(className);
                    if (cnode == null) {
                        cnode = new InstancesContainer.Objects(className, javaClass) {
                            protected String getMoreNodesString(String moreNodesCount)  {
                                return GCRoots_Messages.getMoreNodesString(moreNodesCount);
                            }
                            protected String getSamplesContainerString(String objectsCount)  {
                                return GCRoots_Messages.getSamplesContainerString(objectsCount);
                            }
                            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                                return GCRoots_Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                            }
                        };
                        classes.put(className, cnode);
                        cnodes.add(cnode);
                    }
                    cnode.add(instance, heap);
                }

                if (aggregation == 1) {
                    return cnodes.isEmpty() ? new HeapViewerNode[] { new TextNode(GCRoots_Messages.getNoItemsString(viewFilter)) } :
                                              cnodes.toArray(HeapViewerNode.NO_NODES);
                }

                List<HeapViewerNode> pnodes = new ArrayList();
                Map<String, ClassesContainer.ContainerNodes> packages = new HashMap();
                for (InstancesContainer.Objects cnode : cnodes) {
                    String className = cnode.getName();
                    int nameIdx = className.lastIndexOf('.'); // NOI18N
                    if (nameIdx == -1) {
                        pnodes.add(cnode);
                    } else {
                        String pkgName = className.substring(0, nameIdx);
                        ClassesContainer.ContainerNodes node = packages.get(pkgName);
                        if (node == null) {
                            node = new ClassesContainer.ContainerNodes(pkgName);
                            pnodes.add(node);
                            packages.put(pkgName, node);
                        }
                        node.add(cnode, heap);
                    }
                }

                return pnodes.isEmpty() ? new HeapViewerNode[] { new TextNode(GCRoots_Messages.getNoItemsString(viewFilter)) } :
                                          pnodes.toArray(HeapViewerNode.NO_NODES);
            }
        }
    }
    
    
    @NbBundle.Messages({
        "Dominators_Messages_MoreNodes=<another {0} dominators left>",
        "Dominators_Messages_SamplesContainer=<sample {0} dominators>",
        "Dominators_Messages_NodesContainer=<dominators {0}-{1}>",
        "Dominators_Messages_NoDominators=<no dominators>",
        "Dominators_Messages_NoDominatorsFilter=<no dominators matching the filter>",
        "Dominators_Messages_NoRetainedSizes=<retained sizes not computed yet>"
    })
    private static final class Dominators_Messages {
        private static String getMoreNodesString(String moreNodesCount)  {
            return Bundle.Dominators_Messages_MoreNodes(moreNodesCount);
        }
        private static String getSamplesContainerString(String objectsCount)  {
            return Bundle.Dominators_Messages_SamplesContainer(objectsCount);
        }
        private static String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return Bundle.Dominators_Messages_NodesContainer(firstNodeIdx, lastNodeIdx);
        }
        private static String getNoItemsString(HeapViewerNodeFilter viewFilter) {
            return viewFilter == null ? Bundle.Dominators_Messages_NoDominators() : Bundle.Dominators_Messages_NoDominatorsFilter();
        }
    }
    
    public static HeapViewerNode[] getHeapDominators(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) throws InterruptedException {
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapViewerNode[] { new TextNode(Bundle.Dominators_Messages_NoRetainedSizes()) };
        
        List<Instance> dominators = new ArrayList(getDominatorRoots(heap));
        
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(dominators.size(), UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Instance instance) {
                    return new InstanceNode(instance);
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                    Iterator<Instance> iterator = dominators.listIterator(index);
                    return new ProgressIterator(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Dominators_Messages.getMoreNodesString(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Dominators_Messages.getSamplesContainerString(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Dominators_Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                }
            };
            HeapViewerNode[] nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
            return nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Dominators_Messages.getNoItemsString(viewFilter)) } : nodes;
        } else {
            if (viewFilter != null) {
                Iterator<Instance> dominatorsI = dominators.iterator();
                while (dominatorsI.hasNext())
                    if (!viewFilter.passes(new InstanceNode(dominatorsI.next()), heap))
                        dominatorsI.remove();
            }
            
            List<InstancesContainer.Objects> cnodes = new ArrayList();
            Map<String, InstancesContainer.Objects> classes = new HashMap();
            for (Instance instance : dominators) {
                JavaClass javaClass = instance.getJavaClass();
                String className = javaClass.getName();
                InstancesContainer.Objects cnode = classes.get(className);
                if (cnode == null) {
                    cnode = new InstancesContainer.Objects(className, javaClass) {
                        protected String getMoreNodesString(String moreNodesCount)  {
                            return Dominators_Messages.getMoreNodesString(moreNodesCount);
                        }
                        protected String getSamplesContainerString(String objectsCount)  {
                            return Dominators_Messages.getSamplesContainerString(objectsCount);
                        }
                        protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                            return Dominators_Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                        }
                    };
                    classes.put(className, cnode);
                    cnodes.add(cnode);
                }
                cnode.add(instance, heap);
            }
            
            if (aggregation == 1) {
                return cnodes.isEmpty() ? new HeapViewerNode[] { new TextNode(Dominators_Messages.getNoItemsString(viewFilter)) } :
                                          cnodes.toArray(HeapViewerNode.NO_NODES);
            }
            
            List<HeapViewerNode> pnodes = new ArrayList();
            Map<String, ClassesContainer.ContainerNodes> packages = new HashMap();
            for (InstancesContainer.Objects cnode : cnodes) {
                String className = cnode.getName();
                int nameIdx = className.lastIndexOf('.'); // NOI18N
                if (nameIdx == -1) {
                    pnodes.add(cnode);
                } else {
                    String pkgName = className.substring(0, nameIdx);
                    ClassesContainer.ContainerNodes node = packages.get(pkgName);
                    if (node == null) {
                        node = new ClassesContainer.ContainerNodes(pkgName);
                        pnodes.add(node);
                        packages.put(pkgName, node);
                    }
                    node.add(cnode, heap);
                }
            }
            
            return pnodes.isEmpty() ? new HeapViewerNode[] { new TextNode(Dominators_Messages.getNoItemsString(viewFilter)) } :
                                          pnodes.toArray(HeapViewerNode.NO_NODES);
        }
    }

    static Set<Instance> getDominatorRoots(Heap heap) {
        int searchScope = 1000;
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(searchScope);
                
        Set<Instance> dominators = new HashSet(searchInstances);
        Set<Instance> removed = new HashSet();

        for (Instance instance : searchInstances) {
            if (dominators.contains(instance)) {
                Instance dom = instance;
                long retainedSize = instance.getRetainedSize();

                while (!instance.isGCRoot()) {
                    instance = instance.getNearestGCRootPointer();
                    if (dominators.contains(instance) && instance.getRetainedSize()>=retainedSize) {
                        dominators.remove(dom);
                        removed.add(dom);
                        dom = instance;
                        retainedSize = instance.getRetainedSize();
                    }
                    if (removed.contains(instance)) {
                        dominators.remove(dom);
                        removed.add(dom);
                        break;
                    }
                }
            }
        }
        return dominators;
    }
    
}
