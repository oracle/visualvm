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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.ui.TruffleSummaryView;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleType;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleTypeNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.SummaryView;
import java.util.Collection;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptHeapSummary {
    
    private static final String VIEW_ID = "javascript_summary"; // NOI18N
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class JavaScriptSummaryProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context)) {
                Icon icon = JavaScriptSupport.createBadgedIcon(HeapWalkerIcons.PROPERTIES);
                return new TruffleSummaryView(VIEW_ID, icon, context, actions);
            }

            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 100)
    public static class JavaScriptSummaryOverviewProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context))
                return new JavaScriptSummaryOverview(context);
            
            return null;
        }
        
    }
    
    private static class JavaScriptSummaryOverview extends TruffleSummaryView.OverviewSection {
        
        JavaScriptSummaryOverview(HeapContext context) {
            super(context, 3, 3);
        }
        
        protected void computeEnvironmentData(Object[][] environmentData) {
            super.computeEnvironmentData(environmentData);
            
            environmentData[1][0] = "Node.js:";
            environmentData[2][0] = "Platform:";
            
            JavaScriptHeapFragment fragment = (JavaScriptHeapFragment)getContext().getFragment();
            JavaScriptType processType = fragment.getType("process", null);
            JavaScriptObject process = processType == null || processType.getObjectsCount() == 0 ?
                                              null : processType.getObjectsIterator().next();
            
            if (process == null) {
                environmentData[1][1] = "<not present>";
                environmentData[2][1] = "<unknown>";
            } else {
                Heap heap = fragment.getHeap();
                
                FieldValue releaseFV = process.getFieldValue("release");
                if (releaseFV instanceof ObjectFieldValue) {
                    Instance releaseI = ((ObjectFieldValue)releaseFV).getInstance();
                    if (JavaScriptObject.isJavaScriptObject(releaseI)) {
                        JavaScriptObject releaseO = new JavaScriptObject(releaseI);
                        if ("node".equals(fieldValue(releaseO, "name", heap))) {
                            String versionFV = fieldValue(process, "version", heap);
                            String node = versionFV != null ? "node " + versionFV : "<unknown>";
                            String ltsFV = fieldValue(releaseO, "lts", heap);
                            if (ltsFV != null) node += " (" + ltsFV + ")";
                            environmentData[1][1] = node;
                            
                            String platformFV = fieldValue(process, "platform", heap);
                            String archFV = fieldValue(process, "arch", heap);
                            if (platformFV == null && archFV == null) {
                                environmentData[2][1] = "<unknown>";
                            } else {
                                String platform = platformFV;
                                if (archFV != null) {
                                    if (platform != null) platform += " "; else platform = "";
                                    platform += archFV;
                                }
                                environmentData[2][1] = platform;
                            }

                            return;
                        }                        
                    }
                    
                    environmentData[1][1] = "<unknown>";
                    environmentData[2][1] = "<unknown>";
                    
                    return;
                }
                
                environmentData[1][1] = "<not present>";
                environmentData[2][1] = "<unknown>";
            }
        }
        
        
        private static String fieldValue(JavaScriptObject object, String field, Heap heap) {
            FieldValue value = object == null ? null : object.getFieldValue(field);
            Instance instance = value instanceof ObjectFieldValue ? ((ObjectFieldValue)value).getInstance() : null;
            return instance == null ? null : DetailsSupport.getDetailsString(instance, heap);
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 300)
    public static class JavaScriptSummaryObjectsProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context))
                return new JavaScriptSummaryObjects(context, actions, actionProviders);
            
            return null;
        }
        
    }
    
    private static class JavaScriptSummaryObjects extends TruffleSummaryView.ObjectsSection<JavaScriptObject> {
        
        JavaScriptSummaryObjects(HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            super(JavaScriptObjectsView.class, context, actions, actionProviders);
        }

        
        @Override
        protected boolean isLanguageObject(Instance instance) {
            return JavaScriptObject.isJavaScriptObject(instance);
        }
        
        @Override
        protected JavaScriptObject createObject(Instance instance) {
            return new JavaScriptObject(instance);
        }
        
        @Override
        protected HeapViewerNode createTypeNode(TruffleType type, Heap heap) {
            return new JavaScriptNodes.JavaScriptTypeNode((JavaScriptType)type);
        }

        @Override
        protected ProfilerRenderer typeRenderer(Heap heap) {
            Icon packageIcon = JavaScriptSupport.createBadgedIcon(LanguageIcons.PACKAGE);
            return new TruffleTypeNode.Renderer(packageIcon);
        }
        
        @Override
        protected HeapViewerNode createObjectNode(TruffleObject object, Heap heap) {
            return new JavaScriptNodes.JavaScriptObjectNode((JavaScriptObject)object, object.getType(heap));
        }

        @Override
        protected ProfilerRenderer objectRenderer(Heap heap) {
            Icon instanceIcon = JavaScriptSupport.createBadgedIcon(LanguageIcons.INSTANCE);
            return new TruffleObjectNode.Renderer(heap, instanceIcon);
        }

    }
    
}
