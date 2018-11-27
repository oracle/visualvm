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

import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.GCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaFrameGCRoot;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.ui.HTMLView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.PluggableTreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaThreadsView_Name=Threads",
    "JavaThreadsView_Description=Threads",
    "JavaThreadsView_ComputingThreads=<br>&nbsp;&nbsp;&lt;computing threads...&gt;",
    "JavaThreadsView_Results=Results:",
    "JavaThreadsView_TooltipObjects=Objects",
    "JavaThreadsView_TooltipHTML=HTML",
    "JavaThreadsView_Details=Details:",
    "JavaThreadsView_SelectAction=Select in Threads",
    "JavaThreadsView_ExpandAction=Expand All Threads",
    "JavaThreadsView_CollapseAction=Collapse All Threads"
})
public class JavaThreadsView extends HeapViewerFeature {
    
    private static final String FEATURE_ID = "java_threads"; // NOI18N
    private static final String VIEW_OBJECTS_ID = FEATURE_ID + "_objects"; // NOI18N
    private static final String VIEW_HTML_ID = FEATURE_ID + "_html"; // NOI18N
    
//    private final HeapContext context;
//    private final HeapViewerActions actions;
    
    private JComponent component;
    private ProfilerToolbar toolbar;
    private ProfilerToolbar pluginsToolbar;
    
    private final HTMLView htmlView;
    private final PluggableTreeTableView objectsView;
    
    private JToggleButton rObjects;
    private JToggleButton rHTML;
    
    
    public JavaThreadsView(HeapContext context, HeapViewerActions actions) {
        super(FEATURE_ID, Bundle.JavaThreadsView_Name(), Bundle.JavaThreadsView_Description(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS), 300);
        
//        this.context = context;
//        this.actions = actions;
        
        Heap heap = context.getFragment().getHeap();
        
        objectsView = new PluggableTreeTableView(VIEW_OBJECTS_ID, context, actions, TreeTableViewColumn.instances(heap, false)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                return JavaThreadsProvider.getThreadsNodes(root, heap);
            }
            protected void childrenChanged() {
                setupDefault();
            }
            @Override
            protected void populatePopup(HeapViewerNode node, JPopupMenu popup) {
                if (popup.getComponentCount() > 0) popup.addSeparator();
                
                popup.add(new AbstractAction(Bundle.JavaThreadsView_ExpandAction()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setupDefault();
                            }
                        });
                    }
                });
                
                popup.add(new AbstractAction(Bundle.JavaThreadsView_CollapseAction()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                collapseChildren((HeapViewerNode)getRoot());
                            }
                        });
                    }
                });
            }
        };
        objectsView.setViewName(Bundle.JavaThreadsView_Name());
        
        htmlView = new HTMLView(VIEW_HTML_ID, context, actions, Bundle.JavaThreadsView_ComputingThreads()) {
            protected String computeData(HeapContext context, String viewID) {
                return JavaThreadsProvider.getThreadsHTML(context);
            }
            protected HeapViewerNode nodeForURL(URL url, HeapContext context) {
                return JavaThreadsProvider.getNode(url, context);
            }
        };
    }
    
    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    @Override
    protected void closed() {
        objectsView.closed();
    }
    
    
    void selectInstance(long instanceID, String viewID) {
        if (rHTML == null) init();
        
        rHTML.setSelected(true);
        htmlView.selectReference(Long.toString(instanceID));
    }
    
    void configureAllThreads() {
        if (component != null) {
            rObjects.setSelected(true);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    objectsView.collapseChildren((HeapViewerNode)objectsView.getRoot());
                    setupDefault();
                }
            });
        }
    }
    
    
    private void setupDefault() {
        CCTNode[] children = objectsView.getRoot().getChildren();
        for (CCTNode child : children) objectsView.expandNode((HeapViewerNode)child);
    }
    
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel(Bundle.JavaThreadsView_Results()));
        toolbar.addSpace(3);
        
        ButtonGroup resultsBG = new ButtonGroup();
        
        rObjects = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS), true) {
            protected void fireItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (component != null) ((CardLayout)component.getLayout()).first(component);
                    if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(true);
                }
            }
        };
        rObjects.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        rObjects.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        rObjects.setToolTipText(Bundle.JavaThreadsView_TooltipObjects());
        resultsBG.add(rObjects);
        toolbar.add(rObjects);
        
        rHTML = new JToggleButton(Icons.getIcon(HeapWalkerIcons.PROPERTIES)) {
            protected void fireItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (component != null) ((CardLayout)component.getLayout()).last(component);
                    if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(false);
                }
            }
        };
        rHTML.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        rHTML.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        rHTML.setToolTipText(Bundle.JavaThreadsView_TooltipHTML());
        resultsBG.add(rHTML);
        toolbar.add(rHTML);
        
        if (objectsView.hasPlugins()) {
            pluginsToolbar = ProfilerToolbar.create(false);
//            detailsToolbar.addSpace(2);
//            detailsToolbar.addSeparator();
            pluginsToolbar.addSpace(8);

            pluginsToolbar.add(new GrayLabel(Bundle.JavaThreadsView_Details()));
            pluginsToolbar.addSpace(2);
            
            pluginsToolbar.add(objectsView.getToolbar());
            
            toolbar.add(pluginsToolbar);
        }

        component = new JPanel(new CardLayout());
        component.add(objectsView.getComponent());
        JComponent html = htmlView.getComponent();
        html.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        component.add(new ScrollableContainer(html));
    }
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (JavaHeapFragment.isJavaHeap(context))
                return new JavaThreadsView(context, actions);
            
            return null;
        }

    }
    
    
    private static class SelectInstanceAction extends HeapViewerNodeAction {
        
        private final long id;
        private final HeapViewerActions actions;
        
        SelectInstanceAction(long id, HeapViewerActions actions) {
            super(Bundle.JavaThreadsView_SelectAction(), 205);
            this.id = id;
            this.actions = actions;
        }

        public void actionPerformed(ActionEvent e) {
            JavaThreadsView threadsView = actions.findFeature(JavaThreadsView.class);
            if (threadsView != null) {
                actions.selectFeature(threadsView);
                threadsView.selectInstance(id, VIEW_HTML_ID);
            }
        }
        
    }
    
    @ServiceProvider(service=HeapViewerNodeAction.Provider.class)
    public static class SelectInstanceActionProvider extends HeapViewerNodeAction.Provider {
        
        public boolean supportsView(HeapContext context, String viewID) {
            return !viewID.startsWith(FEATURE_ID) && JavaHeapFragment.isJavaHeap(context);
        }

        public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            Heap heap = context.getFragment().getHeap();
            
            Instance instance = HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
            if (instance == null) return null;
            
            GCRoot gcRoot = heap.getGCRoot(instance);
            if (gcRoot == null) return null;
            
            String gcRootKind = gcRoot.getKind();
            
            if (GCRoot.JAVA_FRAME.equals(gcRootKind)) {
                JavaFrameGCRoot frameVar = (JavaFrameGCRoot)gcRoot;
                if (frameVar.getFrameNumber() == -1) return null;
            } else if (GCRoot.THREAD_OBJECT.equals(gcRootKind)) {
//                ThreadObjectGCRoot thread = (ThreadObjectGCRoot)gcRoot;
            } else {
                return null;
            }
            
            return new HeapViewerNodeAction[] { new SelectInstanceAction(instance.getInstanceId(), actions) };
        }
        
    }
    
}
