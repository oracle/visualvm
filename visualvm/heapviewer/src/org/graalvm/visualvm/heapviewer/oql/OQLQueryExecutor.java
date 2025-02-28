/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.oql;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.ClassesContainer;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstancesContainer;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.ProgressNode;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLException;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.ReferenceChain;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "OQLQueryExecutor_MoreNodes=<another {0} objects left>",
    "OQLQueryExecutor_SamplesContainer=<sample {0} objects>",
    "OQLQueryExecutor_NodesContainer=<objects {0}-{1}>",
    "OQLQueryExecutor_NoClasses=<no objects>",
    "OQLQueryExecutor_NoClassesFilter=<no objects matching the filter>",
    "OQLQueryExecutor_NoResults=<no results>",
    "OQLQueryExecutor_TooManyResults=showing first {0} results",
    "OQLQueryExecutor_QueryError=Query error",
    "OQLQueryExecutor_BadQuery=Bad OQL query",
    "OQLQueryExecutor_NothingExecuted=<no script executed yet>",
    "OQLQueryExecutor_QueryRunning=<query running...>",
    "OQLQueryExecutor_ObjectsNotCollected=<objects results not collected>",
    "OQLQueryExecutor_HTMLNotCollected=<HTML results not collected>"
})
class OQLQueryExecutor {
    
    private final OQLEngine engine;
    
    private boolean collectObjects = true;
    private volatile boolean hasObjectsResults;
    private Set<Object> queryObjects;
    
    private boolean collectHTML = true;
    private int htmlResultsLimit;
    private volatile boolean hasHTMLResults;
    private String queryHTML;
    
    private final AtomicBoolean queryRunning;
    private final ExecutorService progressUpdater;
    
    
    OQLQueryExecutor(OQLEngine engine) {
        this.engine = engine;
        
        queryHTML = htmlize(Bundle.OQLQueryExecutor_NothingExecuted()); // NOI18N
        
        queryRunning = new AtomicBoolean(false);
        progressUpdater = Executors.newSingleThreadExecutor();
    }
    
    
    void runQuery(String queryString, boolean collectObjects, boolean collectHTML, int htmlResultsLimit) {
        this.collectObjects = collectObjects;
        
        this.collectHTML = collectHTML;
        this.htmlResultsLimit = htmlResultsLimit;
        
        runQuery(queryString);
    }
    
    void cancelQuery() {
        String errorMessage = null;
        
        try {
            engine.cancelQuery();
        } catch (OQLException e) {
            errorMessage = e.getLocalizedMessage().replace("\n", "<br>").replace("\r", "<br>"); // NOI18N
        }
        
        queryRunning.compareAndSet(true, false);
//        htmlCollecting.compareAndSet(true, false);
        queryFinished(hasObjectsResults, hasHTMLResults, errorMessage);
    }
    
    boolean isQueryRunning() {
        return queryRunning.get();
    }
    
    
    protected void queryStarted(BoundedRangeModel model) {}
    
    protected void queryFinished(boolean hasObjectsResults, boolean hasHTMLResults, String errorMessage) {}
    
    
    HeapViewerNode[] getQueryObjects(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) throws InterruptedException {
        if (!collectObjects) return new HeapViewerNode[] { new TextNode(Bundle.OQLQueryExecutor_ObjectsNotCollected()) };
        else if (queryObjects == null) return new HeapViewerNode[] { new TextNode(Bundle.OQLQueryExecutor_NothingExecuted()) };
        else if (isQueryRunning()) return new HeapViewerNode[] { new ProgressNode(Bundle.OQLQueryExecutor_QueryRunning()) };
        else return getObjects(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, aggregation);
    }
    
    String getQueryHTML() {
        return queryHTML;
    }
    
    
    private static class Messages {
        static String getMoreNodesString(String moreNodesCount)  {
            return Bundle.OQLQueryExecutor_MoreNodes(moreNodesCount);
        }
        static String getSamplesContainerString(String objectsCount)  {
            return Bundle.OQLQueryExecutor_SamplesContainer(objectsCount);
        }
        static String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
            return Bundle.OQLQueryExecutor_NodesContainer(firstNodeIdx, lastNodeIdx);
        }
        static String getNoObjectsString(HeapViewerNodeFilter viewFilter) {
            return viewFilter == null ? Bundle.OQLQueryExecutor_NoClasses() :
                                        Bundle.OQLQueryExecutor_NoClassesFilter();
        }
    }
    
    private HeapViewerNode[] getObjects(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) throws InterruptedException {
        if (aggregation == 0) {
            NodesComputer<Object> computer = new NodesComputer<Object>(queryObjects.size(), UIThresholds.MAX_OQL_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(Object object) {
                    return object instanceof Instance ? new InstanceNode((Instance)object) :
                                                        new ClassNode((JavaClass)object);
                }
                protected ProgressIterator<Object> objectsIterator(int index, Progress progress) {
                    Iterator<Object> iterator = queryObjects.iterator();
                    return new ProgressIterator<>(iterator, index, true, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Messages.getMoreNodesString(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Messages.getSamplesContainerString(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                }
            };
            HeapViewerNode[] nodes = computer.computeNodes(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
            return nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Messages.getNoObjectsString(viewFilter)) } : nodes;
        } else {
            try {
                progress.setupUnknownSteps();
                
                List<InstancesContainer.Objects> cnodes = new ArrayList<>();
                Map<String, InstancesContainer.Objects> classes = new HashMap<>();
                for (Object object : queryObjects) {
                    progress.step();

                    Instance instance = object instanceof Instance ? (Instance)object : null;
                    JavaClass javaClass = instance != null ? instance.getJavaClass() : (JavaClass)object;
                    if (viewFilter != null && !viewFilter.passes(new ClassNode(javaClass), heap)) continue;

                    String className = javaClass.getName();
                    InstancesContainer.Objects cnode = classes.get(className);
                    if (cnode == null) {
                        cnode = new InstancesContainer.Objects(className, javaClass) {
                            protected String getMoreNodesString(String moreNodesCount)  {
                                return Messages.getMoreNodesString(moreNodesCount);
                            }
                            protected String getSamplesContainerString(String objectsCount)  {
                                return Messages.getSamplesContainerString(objectsCount);
                            }
                            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                                return Messages.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                            }
                        };
                        classes.put(className, cnode);
                        cnodes.add(cnode);
                    }
                    if (instance != null) cnode.add(instance, heap);
                }

                if (aggregation == 1) {
                    return cnodes.isEmpty() ? new HeapViewerNode[] { new TextNode(Messages.getNoObjectsString(viewFilter)) } :
                                                  cnodes.toArray(HeapViewerNode.NO_NODES);
                } else {
                    List<HeapViewerNode> pnodes = new ArrayList<>();
                    Map<String, ClassesContainer.ContainerNodes> packages = new HashMap<>();
                    for (InstancesContainer.Objects cnode : cnodes) {
                        // progress.step(); // NOTE: aggregating classes by package should be fast
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

                    return pnodes.isEmpty() ? new HeapViewerNode[] { new TextNode(Messages.getNoObjectsString(viewFilter)) } :
                                                  pnodes.toArray(HeapViewerNode.NO_NODES);
                }
            } finally {
                progress.finish();
            }
        }
    }
    
    
    private void runQuery(String queryString) {
        new RequestProcessor("OQL Query Processor").post(new Runnable() { // NOI18N
            public void run() {
                if (queryObjects == null) queryObjects = new HashSet<>();
                queryObjects.clear();
                hasObjectsResults = false;
                
                queryHTML = !collectHTML ? (htmlize(Bundle.OQLQueryExecutor_ObjectsNotCollected())) :
                                           (htmlize(Bundle.OQLQueryExecutor_QueryRunning()));
                hasHTMLResults = false;
                
                BoundedRangeModel progressModel = new DefaultBoundedRangeModel(0, 10, 0, 100);
                
                final AtomicInteger counter = new AtomicInteger(htmlResultsLimit);
                progressModel.setMaximum(100);

                final StringBuilder sb = new StringBuilder();
                final boolean[] oddRow = new boolean[1];
                Color oddRowBackground = UIUtils.getDarker(
                                UIUtils.getProfilerResultsBackground());
                final String oddRowBackgroundString =
                        "rgb(" + oddRowBackground.getRed() + "," + //NOI18N
                                 oddRowBackground.getGreen() + "," + //NOI18N
                                 oddRowBackground.getBlue() + ")"; //NOI18N

                sb.append("<table border='0' width='100%'>"); // NOI18N

                try {
                    queryRunning.compareAndSet(false, true);
                    queryStarted(progressModel);
                    progressUpdater.submit(new ProgressUpdater(progressModel));
                    engine.executeQuery(queryString, new OQLEngine.ObjectVisitor() {
                        public boolean visit(Object o) {
//                                    System.err.println(">>> Visiting object " + o);
                            boolean collectingHtml = collectHTML && counter.decrementAndGet() >= 0;

                            if (collectingHtml) {
                                sb.append(oddRow[0] ?
                                    "<tr><td style='background-color: " + // NOI18N
                                    oddRowBackgroundString + ";'>" : "<tr><td>"); // NOI18N
                                oddRow[0] = !oddRow[0];
                            }
                            
                            dump(o, collectingHtml ? sb : null);
                            
                            if (collectingHtml) sb.append("</td></tr>"); // NOI18N
                            
//                            boolean htmlFull = counter.decrementAndGet() == 0;
//                            if (htmlFull) htmlCollecting.compareAndSet(true, false);
                            
                            return engine.isCancelled();
//                            return counter.decrementAndGet() == 0 || (!queryRunning.get() && !engine.isCancelled()); // process all hits while the analysis is running
                            
                        }
                    });
                    
                    int count = counter.get();

                    if (count < 0) { // Some results are missing
                        sb.append("<tr><td>");  // NOI18N
                        sb.append("&lt;" + Bundle.OQLQueryExecutor_TooManyResults(htmlResultsLimit) + "&gt;");      // NOI18N
                        sb.append("</td></tr>");   // NOI18N
                    }

                    sb.append("</table>"); // NOI18N

                    queryHTML = count != htmlResultsLimit ? sb.toString() :
                                htmlize(Bundle.OQLQueryExecutor_NoResults()); // Query returned no results
                    
                    queryRunning.compareAndSet(true, false);
//                    htmlCollecting.compareAndSet(true, false);
                    queryFinished(hasObjectsResults, hasHTMLResults, null);
                } catch (OQLException oQLException) {
                    Logger.getLogger(OQLQueryExecutor.class.getName()).log(Level.INFO, "Error executing OQL", oQLException);   // NOI18N
                    StringBuilder errorMessage = new StringBuilder();
                    errorMessage.append("<h2>").append(Bundle.OQLQueryExecutor_QueryError()).append("</h2>"); // NOI18N
                    errorMessage.append(Bundle.OQLQueryExecutor_BadQuery()); // NOI18N
                    errorMessage.append("<hr>"); // NOI18N
                    errorMessage.append(oQLException.getLocalizedMessage().replace("\n", "<br>").replace("\r", "<br>")); // NOI18N

                    queryRunning.compareAndSet(true, false);
//                    htmlCollecting.compareAndSet(true, false);
                    queryFinished(hasObjectsResults, hasHTMLResults, errorMessage.toString());
                }
            }

        });
    }
    
    private void dump(Object o, StringBuilder sb) {
        if (o == null) {
            return;
        }        
        if (o instanceof Instance) {
            Instance i = (Instance)o;
            queryObjects.add(i);
            hasObjectsResults = true;
            if (sb != null) {
                sb.append(HeapUtils.instanceToHtml(i, true, null));
                hasHTMLResults = true;
            }
        } else if (o instanceof JavaClass) {
            JavaClass c = (JavaClass)o;
            queryObjects.add(c);
            hasObjectsResults = true;
            if (sb != null) {
                sb.append(HeapUtils.classToHtml(c));
                hasHTMLResults = true;
            }
        } else if (o instanceof ReferenceChain) {
            ReferenceChain rc = (ReferenceChain) o;
            boolean first = true;
            while (rc != null) {
                if (!first) {
                    if (sb != null) sb.append("-&gt;"); // NOI18N
                } else {
                    first = false;
                }
                o = rc.getObj();
                if (o instanceof Instance) {
                    Instance i = (Instance)o;
                    queryObjects.add(i);
                    hasObjectsResults = true;
                    if (sb != null) {
                        sb.append(HeapUtils.instanceToHtml(i, true, null));
                        hasHTMLResults = true;
                    }
                } else if (o instanceof JavaClass) {
                    JavaClass c = (JavaClass)o;
                    queryObjects.add(c);
                    hasObjectsResults = true;
                    if (sb != null) {
                        sb.append(HeapUtils.classToHtml(c));
                        hasHTMLResults = true;
                    }
                }
                rc = rc.getNext();
            }
        } else if (o instanceof Map) {
            Set<Map.Entry> entries = ((Map)o).entrySet();
            if (sb != null) {
                sb.append("<span><b>{</b><br/>"); // NOI18N
                hasHTMLResults = true;
            }
            boolean first = true;
            for(Map.Entry entry : entries) {
                if (!first) {
                    if (sb != null) sb.append(",<br/>"); // NOI18N
                } else {
                    first = false;
                }
                if (sb != null) {
                    sb.append(entry.getKey().toString().replace("<", "&lt;").replace(">", "&gt;")); // NOI18N
                    sb.append(" = "); // NOI18N
                }
                dump(unwrap(entry.getValue()), sb);
            }
            if (sb != null) sb.append("<br/><b>}</b></span>"); // NOI18N
        } else if (o instanceof Object[]) {
            if (sb != null) {
                sb.append("<span><b>[</b>&nbsp;"); // NOI18N
                hasHTMLResults = true;
            }
            boolean first = true;
            for (Object obj1 : (Object[]) o) {
                if (!first) {
                    if (sb != null) sb.append(", "); // NOI18N
                } else {
                    first = false;
                }
                dump(unwrap(obj1), sb);
            }
            if (sb != null) sb.append("&nbsp;<b>]</b></span>"); // NOI18N
        } else {
            String s = o.toString();
            if (sb != null) {
                sb.append(s);
                hasHTMLResults = true;
            }
            if (collectObjects) extractObjects(s);
        }
    }
    
    private static final String INSTANCE_LINK_PREFIX = "<a href='file://instance/"; // NOI18N
    private static final String STRING_INSTANCE_LINK_NAME_PREFIX = "' name='"; // NOI18N
    private static final String INSTANCE_LINK_SUFFIX = "</a>"; // NOI18N
    private static final String CLASS_LINK_PREFIX = "<a href='file://class/"; // NOI18N
    private static final String STRING_CLASS_LINK_NAME_PREFIX = "' name='"; // NOI18N
    private static final String CLASS_LINK_SUFFIX = "</a>"; // NOI18N
    private void extractObjects(String s) {
        int instanceIdx = s.indexOf(INSTANCE_LINK_PREFIX);
        int classIdx = instanceIdx != -1 ? -1 : s.indexOf(CLASS_LINK_PREFIX);
        
        while (instanceIdx != -1 || classIdx != -1) {
            if (instanceIdx != -1) {
                s = s.substring(instanceIdx + INSTANCE_LINK_PREFIX.length());
                String instanceNumber = s.substring(0, s.indexOf(STRING_INSTANCE_LINK_NAME_PREFIX));
                s = s.substring(s.indexOf(INSTANCE_LINK_SUFFIX) + INSTANCE_LINK_SUFFIX.length());

                long instanceID = Long.parseLong(instanceNumber);
                Instance i = engine.getHeap().getInstanceByID(instanceID);
                if (i != null) { queryObjects.add(i); hasObjectsResults = true; }
            } else {
                s = s.substring(classIdx + CLASS_LINK_PREFIX.length());
                String classNumber = s.substring(0, s.indexOf(STRING_CLASS_LINK_NAME_PREFIX));
                s = s.substring(s.indexOf(CLASS_LINK_SUFFIX) + CLASS_LINK_SUFFIX.length());

                long classID = Long.parseLong(classNumber);
                JavaClass c = engine.getHeap().getJavaClassByID(classID);
                if (c != null) { queryObjects.add(c); hasObjectsResults = true; }
            }

            instanceIdx = s.indexOf(INSTANCE_LINK_PREFIX);
            classIdx = instanceIdx != -1 ? -1 : s.indexOf(CLASS_LINK_PREFIX);
        }
    }
    
    private Object unwrap(Object obj1) {
        Object obj2 = engine.unwrapJavaObject(obj1, true);
        return obj2 != null ? obj2 : obj1;
    }
    
    
    private static String htmlize(String s) {
        return "<p>&nbsp;&nbsp" + s.replace("<", "&lt;").replace(">", "&gt;") + "</p>"; // NOI18N
    }
    
    
    private class ProgressUpdater implements Runnable {

        private final BoundedRangeModel progressModel;

        ProgressUpdater(BoundedRangeModel model) {
            progressModel = model;
        }

        public void run() {
            while (queryRunning.get()) {
                final int newVal;
                int val = progressModel.getValue() + 10;
                
                if (val > progressModel.getMaximum()) {
                    val = progressModel.getMinimum();
                }
                newVal = val;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressModel.setValue(newVal);
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
}
