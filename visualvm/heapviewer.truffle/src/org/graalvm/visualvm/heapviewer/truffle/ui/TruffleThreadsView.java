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
package org.graalvm.visualvm.heapviewer.truffle.ui;

import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguage;
import org.graalvm.visualvm.heapviewer.truffle.TruffleThreadsProvider;
import org.graalvm.visualvm.heapviewer.ui.HTMLView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.PluggableTreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import javax.swing.Icon;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleThreadsView_Name=Threads",
    "TruffleThreadsView_Description=Threads",
    "TruffleThreadsView_ComputingThreads=computing threads...",
    "TruffleThreadsView_Results=Results:",
    "TruffleThreadsView_ViewObjects=Objects",
    "TruffleThreadsView_ViewHtml=HTML",
    "TruffleThreadsView_Details=Details:"
})
public class TruffleThreadsView extends HeapViewerFeature {
    
    private static final String FEATURE_ID = "threads"; // NOI18N
    
    private static final String OBJECTS_ID = "_objects"; // NOI18N
    private static final String HTML_ID = "_html"; // NOI18N
    
    private JComponent component;
    private ProfilerToolbar toolbar;
    private ProfilerToolbar pluginsToolbar;
    
    private final HTMLView htmlView;
    private final PluggableTreeTableView objectsView;
    
    
    public TruffleThreadsView(TruffleLanguage language, HeapContext context, HeapViewerActions actions) {
        super(idFromLanguage(language), Bundle.TruffleThreadsView_Name(), Bundle.TruffleThreadsView_Description(), iconFromLanguage(language), 300);
        
        Heap heap = context.getFragment().getHeap();
        
        final TruffleThreadsProvider threadsProvider = new TruffleThreadsProvider(language);
        
        objectsView = new PluggableTreeTableView(getID() + OBJECTS_ID, context, actions, TreeTableViewColumn.instances(heap, false)) {
            @Override
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                return threadsProvider.getThreadsObjects(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
            }
            protected void childrenChanged() {
                CCTNode[] children = getRoot().getChildren();
                for (CCTNode child : children) expandNode((HeapViewerNode)child);
            }
        };
        objectsView.setViewName(getName());
        
        htmlView = new HTMLView(getID() + HTML_ID, context, actions, "<br>&nbsp;&nbsp;" + Bundle.TruffleThreadsView_ComputingThreads()) { // NOI18N
            protected String computeData(HeapContext context, String viewID) {
                return threadsProvider.getThreadsHTML(context);
            }
            protected HeapViewerNode nodeForURL(URL url, HeapContext context) {
                return threadsProvider.getNodeForURL(url, context);
            }
        };
    }
    
    
    static String idFromLanguage(TruffleLanguage language) {
        return language.getID() + "_" + FEATURE_ID; // NOI18N
    }
    
    static Icon iconFromLanguage(TruffleLanguage language) {
        return language.createLanguageIcon(Icons.getIcon(ProfilerIcons.WINDOW_THREADS));
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
        
        toolbar.add(new GrayLabel(Bundle.TruffleThreadsView_Results()));
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
        rObjects.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        rObjects.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        rObjects.setToolTipText(Bundle.TruffleThreadsView_ViewObjects());
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
        rHTML.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        rHTML.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        rHTML.setToolTipText(Bundle.TruffleThreadsView_ViewHtml());
        resultsBG.add(rHTML);
        toolbar.add(rHTML);

        if (objectsView.hasPlugins()) {
            pluginsToolbar = ProfilerToolbar.create(false);
//            detailsToolbar.addSpace(2);
//            detailsToolbar.addSeparator();
            pluginsToolbar.addSpace(8);

            pluginsToolbar.add(new GrayLabel(Bundle.TruffleThreadsView_Details()));
            pluginsToolbar.addSpace(2);
            
            pluginsToolbar.add(objectsView.getToolbar());
            
            toolbar.add(pluginsToolbar);
        }
        
        JScrollPane htmlViewScroll = new JScrollPane(htmlView.getComponent(),
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
    
}
