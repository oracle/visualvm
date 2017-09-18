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

import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.SwingWorker;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.RootNode;
import org.netbeans.modules.profiler.heapwalker.v2.swing.HTMLTextComponent;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerActions;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerFeature;
import org.netbeans.modules.profiler.heapwalker.v2.ui.PluggableTreeTableView;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableViewColumn;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class JavaScriptThreadsView extends HeapWalkerFeature {
    
    private final HeapContext context;
    
    private JComponent component;
    private ProfilerToolbar toolbar;
    private ProfilerToolbar pluginsToolbar;
    
    private HTMLTextComponent htmlView;
    private final PluggableTreeTableView objectsView;
    
    
    public JavaScriptThreadsView(HeapContext context, HeapWalkerActions actions) {
        super("javascript_threads_objects", "Thread", "Thread", JavaScriptSupport.createBadgedIcon(ProfilerIcons.WINDOW_THREADS), 300);
        
        this.context = context;
        Heap heap = context.getFragment().getHeap();
        
        objectsView = new PluggableTreeTableView("javascript_threads", context, actions, TreeTableViewColumn.instances(heap, false)) {
            protected HeapWalkerNode[] computeData(RootNode root, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
                return JavaScriptThreadsObjects.getThreads(root, heap);
            }
            protected void childrenChanged() {
                CCTNode[] children = getRoot().getChildren();
                for (CCTNode child : children) expandNode((HeapWalkerNode)child);
            }
        };
        objectsView.setViewName("Thread");
    }
    

    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel("Results:"));
        toolbar.addSpace(3);
        
        ButtonGroup resultsBG = new ButtonGroup();
        
        JToggleButton rObjects = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS), true) {
            protected void fireItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (component != null) ((CardLayout)component.getLayout()).first(component);
                    if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(true);
                }
            }
        };
        rObjects.setToolTipText("Objects");
        resultsBG.add(rObjects);
        toolbar.add(rObjects);
        JToggleButton rHTML = new JToggleButton(Icons.getIcon(HeapWalkerIcons.PROPERTIES)) {
            protected void fireItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (component != null) ((CardLayout)component.getLayout()).last(component);
                    if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(false);
                }
            }
        };
        rHTML.setToolTipText("HTML");
        resultsBG.add(rHTML);
        toolbar.add(rHTML);

        if (objectsView.hasPlugins()) {
            pluginsToolbar = ProfilerToolbar.create(false);
//            detailsToolbar.addSpace(2);
//            detailsToolbar.addSeparator();
            pluginsToolbar.addSpace(8);

            pluginsToolbar.add(new GrayLabel("Details:"));
            pluginsToolbar.addSpace(2);
            
            pluginsToolbar.add(objectsView.getToolbar());
            
            toolbar.add(pluginsToolbar);
        }
        

        htmlView = new HTMLTextComponent("<br>&nbsp;&nbsp;computing thread...") {
            protected void showURL(URL url) {
//                resultsController.showURL(url);
            }
            protected void firstDisplayed() {
                new SwingWorker<String, String>() {
                    protected String doInBackground() throws Exception {
                        return JavaScriptThreadsHTML.getThreads(context);
                    }
                    protected void done() {
                        try {
                            htmlView.setText(get());
                            if (htmlView.getDocument().getLength() > 0) htmlView.setCaretPosition(0);
                        } catch (InterruptedException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (ExecutionException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }.execute();
            }
        };
        if (htmlView.getDocument().getLength() > 0) htmlView.setCaretPosition(0);
        JScrollPane htmlViewScroll = new JScrollPane(htmlView,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        htmlViewScroll.setBorder(BorderFactory.createEmptyBorder());
        htmlViewScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        htmlViewScroll.getVerticalScrollBar().setUnitIncrement(10);
        htmlViewScroll.getHorizontalScrollBar().setUnitIncrement(10);

        component = new JPanel(new CardLayout());
        component.add(objectsView.getComponent());
        component.add(htmlViewScroll);
    }
    
    
    @ServiceProvider(service=HeapWalkerFeature.Provider.class)
    public static class Provider extends HeapWalkerFeature.Provider {

        public HeapWalkerFeature getFeature(HeapContext context, HeapWalkerActions actions) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context))
                return new JavaScriptThreadsView(context, actions);
            
            return null;
        }

    }
    
}
