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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptPreviewPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    
     private InstanceScrollPane component;
    

    public JavaScriptPreviewPlugin(HeapContext context) {
        super("Preview", "Preview", Icons.getIcon(HeapWalkerIcons.PROPERTIES));
        heap = context.getFragment().getHeap();
    }

    protected JComponent createComponent() {
        if (component == null) init();
        return component;
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        if (!(node instanceof JavaScriptNodes.JavaScriptObjectNode)) { component.showInstance(null); return; }
        
        JavaScriptNodes.JavaScriptObjectNode dnode = (JavaScriptNodes.JavaScriptObjectNode)node;
        if ("Function".equals(dnode.getTypeName()) || "JSFunction".equals(dnode.getTypeName())) {
            JavaScriptObject jsobj = dnode.getTruffleObject();
            FieldValue dataField = jsobj.getFieldValue("functionData (hidden)");
            Instance data = dataField instanceof ObjectFieldValue ? ((ObjectFieldValue)dataField).getInstance() : null;
            if (data == null) { component.showInstance(null); return; }

            Object rootNode = ((Instance)data).getValueOfField("lazyInit");
            if (!(rootNode instanceof Instance)) {
                Object callTarget = data.getValueOfField("callTarget");
                if (!(callTarget instanceof Instance)) { component.showInstance(null); return; }

                rootNode = ((Instance)callTarget).getValueOfField("rootNode");
                if (!(rootNode instanceof Instance)) { component.showInstance(null); return; }
            }

            Instance sourceSection = null;
            List<FieldValue> rootNodeFields = ((Instance)rootNode).getFieldValues();
            for (FieldValue field : rootNodeFields) {
                if ("sourceSection".equals(field.getField().getName()) && field instanceof ObjectFieldValue) {
                    Instance instance = ((ObjectFieldValue)field).getInstance();
                    if (instance != null) {
                        sourceSection = instance;
                        break;
                    }
                }
            }

            if (!(sourceSection instanceof Instance)) {
                Object nnode = ((Instance)rootNode).getValueOfField("node");
                if (!(nnode instanceof Instance)) { component.showInstance(null); return; }

                Object ssourceSection = ((Instance)nnode).getValueOfField("source");
                if (!(ssourceSection instanceof Instance)) { component.showInstance(null); return; }

                sourceSection = (Instance)ssourceSection;
            }

            component.showInstance((Instance)sourceSection);
        } else {
            component.showInstance(null);
        }
    }
    
    
    private void init() {
        component = new InstanceScrollPane();
    }
    
    
    private class InstanceScrollPane extends JScrollPane {
        
        private Instance selectedInstance = null;
        private boolean instancePending = false;
        
        
        InstanceScrollPane() {
            setBorder(BorderFactory.createEmptyBorder());
            setViewportBorder(BorderFactory.createEmptyBorder());
//            setViewportBorder(BorderFactory.createLineBorder(
//                    UIManager.getLookAndFeel().getID().equals("Metal") ? // NOI18N
//                    UIManager.getColor("Button.darkShadow") : // NOI18N
//                    UIManager.getColor("Button.shadow"))); // NOI18N
            
            addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        if (instancePending && isShowing()) showInstanceImpl();
                    }
                }
            });
            
            showInstanceImpl();
        }
        
        
        void showInstance(Instance instance) {
            if (selectedInstance == instance) return;
            selectedInstance = instance;
            if (isShowing()) showInstanceImpl();
            else instancePending = true;
        }
        
        private void showInstanceImpl() {
            JComponent instanceView = selectedInstance == null ? null :
                       DetailsSupport.getDetailsView(selectedInstance, heap);
            if (instanceView == null) {
                JLabel noDetails = new JLabel("<no details>", JLabel.CENTER);
                noDetails.setEnabled(false);
                
                instanceView = new JPanel(new BorderLayout());
                instanceView.setOpaque(true);
                instanceView.setBackground(UIUtils.getProfilerResultsBackground());
                instanceView.add(noDetails, BorderLayout.CENTER);
            }
            setViewportView(instanceView);
            //doLayout();
            instancePending = false;
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 100)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context)) return new JavaScriptPreviewPlugin(context);
            return null;
        }
        
    }
    
}
