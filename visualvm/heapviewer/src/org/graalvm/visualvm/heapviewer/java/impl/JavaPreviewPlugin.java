/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaPreviewPlugin_Name=Preview",
    "JavaPreviewPlugin_Description=Preview",
    "JavaPreviewPlugin_NoDetails=<no details>"
})
class JavaPreviewPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    
    private InstanceScrollPane component;
    

    JavaPreviewPlugin(HeapContext context) {
        super(Bundle.JavaPreviewPlugin_Name(), Bundle.JavaPreviewPlugin_Description(), Icons.getIcon(HeapWalkerIcons.PROPERTIES));
        heap = context.getFragment().getHeap();
    }

    
    @Override
    protected void closed() {
        // TODO: should cancel the preview
    }
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        component.showInstance(node == null ? null : HeapViewerNode.getValue(node, DataType.INSTANCE, heap));
    }
    
    
    protected JComponent createComponent() {
        if (component == null) component = new InstanceScrollPane();
        return component;
    }
    
    
    private static class InstanceScrollPane extends JScrollPane {
        
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
                       DetailsSupport.getDetailsView(selectedInstance);
            if (instanceView == null) {
                JLabel noDetails = new JLabel(Bundle.JavaPreviewPlugin_NoDetails(), JLabel.CENTER);
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
            if (!viewID.startsWith("diff") && JavaHeapFragment.isJavaHeap(context)) return new JavaPreviewPlugin(context); // NOI18N
            return null;
        }
        
    }
    
}
