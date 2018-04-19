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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleStackFrameNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import java.util.Collection;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import java.net.URL;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.ClassNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.java.LocalObjectNode;
import com.sun.tools.visualvm.heapviewer.java.ThreadNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.LocalDynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.utils.HeapUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SortOrder;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleThreadsProvider_CannotResolveClassMsg=Cannot resolve class",
    "TruffleThreadsProvider_CannotResolveInstanceMsg=Cannot resolve instance"
})
public class TruffleThreadsProvider<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> {
    
    private final L language;
    
    
    public TruffleThreadsProvider(L language) {
        this.language = language;
    }
    
    
    public HeapViewerNode[] getThreadsObjects(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        List<HeapViewerNode> threadNodes = new ArrayList();
        
//        JavaClass javaClassClass = heap.getJavaClassByName(Class.class.getName());
        
        TruffleStackTraces tst = new TruffleStackTraces(heap);
        Collection<TruffleStackTraces.StackTrace> threads = tst.getStackTraces();
        
        if (threads != null) {
            for (TruffleStackTraces.StackTrace st : threads) {
                Instance threadInstance = st.getThread();
                String threadName = DetailsSupport.getDetailsString(threadInstance, heap);
                if (threadName == null /*|| !threadName.startsWith(RUBY_THREAD_NAME_PREFIX)*/) continue;
                
                final List<HeapViewerNode> stackFrameNodes = new ArrayList();
                ThreadNode threadNode = new ThreadNode(threadName, threadInstance) {
                    protected HeapViewerNode[] computeChildren(RootNode root) {
                        return stackFrameNodes.toArray(HeapViewerNode.NO_NODES);
                    }
                };
                threadNodes.add(threadNode);

                for (TruffleStackTraces.Frame f : st.getFrames()) {
                    Set<HeapViewerNode> localObjects = new HashSet();
                    for (FieldValue fv :  f.getFieldValues()) {
    //                    String val;
    //                    if (fv instanceof ObjectFieldValue) {
    //                        Instance i = ((ObjectFieldValue)fv).getInstance();
    //                        val = i.getJavaClass().getName()+"#"+i.getInstanceNumber();
    //                    } else {
    //                        val = fv.getValue();
    //                    }
                        if (!(fv instanceof ObjectFieldValue)) continue;

                        Instance instance = ((ObjectFieldValue)fv).getInstance();
                        if (instance == null) continue;
                        
                        if (language.isLanguageObject(instance)) {
                            O object = language.createObject(instance);
                            localObjects.add((HeapViewerNode)language.createLocalObjectNode(object, object.getType(heap)));
                        } else if (DynamicObject.isDynamicObject(instance)) {
                            DynamicObject dobj = new DynamicObject(instance);
                            localObjects.add(new LocalDynamicObjectNode(dobj, dobj.getType(heap)));
                        } else {
                            localObjects.add(new LocalObjectNode(instance));
                        }
                    }
    //                List<FieldValue> fields = f.getFieldValues();
                    String stackFrameName = f.getName();
                    if (stackFrameName == null) stackFrameName = "<unknown>";
                    stackFrameNodes.add(new TruffleStackFrameNode(stackFrameName, localObjects.toArray(HeapViewerNode.NO_NODES)));
    //                sb.append("    at "+f.getName()+"()");
    //                sb.append("<br>");  // NOI18N
    //
    ////                if (!fields.isEmpty()) sb.append("        Locals:");
    ////                sb.append("<br>");  // NOI18N
    //

                }
            }
        } else {
            threadNodes.add(new TextNode("Thread not available"));
        }
        
        return threadNodes.toArray(HeapViewerNode.NO_NODES);
    }
    
    public HeapViewerNode getNodeForURL(URL url, HeapContext context) {
        String urls = url.toString();
                
        if (HeapUtils.isInstance(urls)) {
            Heap heap = context.getFragment().getHeap();
            Instance instance = HeapUtils.instanceFromHtml(urls, heap);
            
            if (language.isLanguageObject(instance)) {
                O object = language.createObject(instance);
                return (HeapViewerNode)language.createObjectNode(object, object.getType(heap));
            } else if (DynamicObject.isDynamicObject(instance)) {
                DynamicObject dobj = new DynamicObject(instance);
                return new LocalDynamicObjectNode(dobj, dobj.getType(heap));
            } else if (instance != null) {
                return new InstanceNode(instance);
            } else {
                ProfilerDialogs.displayError(Bundle.TruffleThreadsProvider_CannotResolveInstanceMsg());
            }
        } else if (HeapUtils.isClass(urls)) {
            JavaClass javaClass = HeapUtils.classFromHtml(urls, context.getFragment().getHeap());
            if (javaClass != null) return new ClassNode(javaClass);
            else ProfilerDialogs.displayError(Bundle.TruffleThreadsProvider_CannotResolveClassMsg());
        }

        return null;
    }
    
    public String getThreadsHTML(HeapContext context) {
//        long start = System.currentTimeMillis();
        
        StringBuilder sb = new StringBuilder();
        
        Heap heap = context.getFragment().getHeap();
        JavaClass javaClassClass = heap.getJavaClassByName(Class.class.getName());
        
        TruffleStackTraces tst = new TruffleStackTraces(heap);
        Collection<TruffleStackTraces.StackTrace> threads = tst.getStackTraces();
        
        sb.append("<pre>"); // NOI18N

        if (threads != null) {
            for (TruffleStackTraces.StackTrace st : threads) {
                sb.append("<b>&nbsp;&nbsp;Thread " + DetailsSupport.getDetailsString(st.getThread(), heap) + "</b>");        
                sb.append("<br>");  // NOI18N

                List<TruffleStackTraces.Frame> frames = st.getFrames();
                for (TruffleStackTraces.Frame f : frames) {
                    List<FieldValue> fields = f.getFieldValues();
                    String fname = f.getName();
                    if (fname == null) fname = "<unknown>";
                    sb.append("    at "+HeapUtils.htmlize(fname));
                    sb.append("<br>");  // NOI18N

    //                if (!fields.isEmpty()) sb.append("        Locals:");
    //                sb.append("<br>");  // NOI18N

                    for (FieldValue fv : fields) {
    //                    String val;
    //                    if (fv instanceof ObjectFieldValue) {
    //                        Instance i = ((ObjectFieldValue)fv).getInstance();
    //                        val = i.getJavaClass().getName()+"#"+i.getInstanceNumber();
    //                    } else {
    //                        val = fv.getValue();
    //                    }
                        if (!(fv instanceof ObjectFieldValue)) continue;
                        Instance instance = ((ObjectFieldValue)fv).getInstance();
                        if (instance == null) continue;
                        sb.append("       <span style=\"color: #666666\">local object:</span> " + printInstance(instance, heap, javaClassClass));
                        sb.append("<br>");  // NOI18N
                    }
                }
            }
        } else {
            sb.append("<b>&nbsp;&nbsp;Thread not available");
        }
        
        sb.append("<br>");  // NOI18N
        sb.append("</pre>"); // NOI18N
        
//        System.err.println(">>> JAVASCRIPT Threads computed in " + (System.currentTimeMillis() - start));
        
        return sb.toString();
    }
    
    private String printInstance(Instance instance, Heap heap, JavaClass javaClassClass) {
        if (language.isLanguageObject(instance)) {
            O object = language.createObject(instance);
            TruffleObjectNode<O> node = language.createObjectNode(object, object.getType(heap));
            String instanceString = HeapUtils.instanceToHtml(instance, false, heap, javaClassClass);
            String type = node.getTypeName();
            instanceString = instanceString.replace(">" + instance.getJavaClass().getName() + "#", ">" + HeapUtils.htmlize(type) + "#");
            String logValue = node.getLogicalValue(heap);
            if (logValue != null) instanceString += " <span style=\"color: #666666\">: " + HeapUtils.htmlize(logValue) + "</span>";
            return instanceString;
        } else if (DynamicObject.isDynamicObject(instance)) {
            DynamicObject dobj = new DynamicObject(instance);
            TruffleObjectNode<O> node = new DynamicObjectNode(dobj, dobj.getType(heap));
            String instanceString = HeapUtils.instanceToHtml(instance, false, heap, javaClassClass);
            String type = node.getTypeName();
            instanceString = instanceString.replace(">" + instance.getJavaClass().getName() + "#", ">" + HeapUtils.htmlize(type) + "#");
            String logValue = node.getLogicalValue(heap);
            if (logValue != null) instanceString += " <span style=\"color: #666666\">: " + HeapUtils.htmlize(logValue) + "</span>";
            return instanceString;
        } else {
            return HeapUtils.instanceToHtml(instance, true, heap, javaClassClass);
        }
    }
    
}
