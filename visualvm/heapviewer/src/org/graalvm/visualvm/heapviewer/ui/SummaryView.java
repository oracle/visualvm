/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.TableModel;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.graalvm.visualvm.uisupport.VerticalLayout;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public class SummaryView {
    
    private final String viewID;
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    
    private final Collection<HeapViewerNodeAction.Provider> actionProviders;
    
    private List<HeapView> content;
    
    private JComponent component;
    private ProfilerToolbar toolbar;
    
    
    public SummaryView(String viewID, HeapContext context, HeapViewerActions actions) {
        this.viewID = viewID;
        this.context = context;
        this.actions = actions;
        
        actionProviders = new ArrayList<>();
        for (HeapViewerNodeAction.Provider provider : Lookup.getDefault().lookupAll(HeapViewerNodeAction.Provider.class))
            if (provider.supportsView(context, viewID)) actionProviders.add(provider);
        
        content = new ArrayList<>();
        Collection<? extends ContentProvider> providers = Lookup.getDefault().lookupAll(ContentProvider.class);
        for (ContentProvider provider : providers) {
            HeapView summary = provider.createSummary(this.viewID, this.context, this.actions, this.actionProviders);
            if (summary != null) content.add(summary);
        }
    }
    
    
    public JComponent getComponent() {
        if (component == null) initUI();
        return component;
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }
    
    
    protected HeapView createDefaultSummary() { return null; }
    
    protected void uiCreated(List<HeapView> views) {}
    
    
    private void initUI() {
        toolbar = ProfilerToolbar.create(false);
        
        
        JPanel p = new JPanel(new VerticalLayout(false, 6));
        p.setOpaque(true);
        p.setBackground(UIUtils.getProfilerResultsBackground());
        
        HeapView defaultContent = createDefaultSummary();
        if (defaultContent != null) content.add(0, defaultContent);
        
        for (HeapView view : content) {
            p.add(new ContentContainer(view));
            ProfilerToolbar viewToolbar = view.getToolbar();
            if (viewToolbar != null) toolbar.add(viewToolbar);
        }
        
        uiCreated(new ArrayList<>(content));
        
        component = new ScrollableContainer(p);
        
        content.clear();
        content = null;
    }
    
    
    public static class SimpleTable extends ProfilerTable {
        
        private static final int COLUMN_MARGIN = 5;
        
        public SimpleTable(TableModel tm, int fillerColumn) {
            super(tm, false, false, null);
            setShowHorizontalLines(false);
            setShowVerticalLines(false);
            setIntercellSpacing(new Dimension());
            setRowSelectionAllowed(false);
            setColumnSelectionAllowed(false);
            setFitWidthColumn(fillerColumn);
        }
        
        public void setColumnRenderer(int column, ProfilerRenderer renderer, boolean fixedWidth) {
            super.setColumnRenderer(column, renderer);
            
            if (fixedWidth) {
                int w = 0;
                int rc = getRowCount();
                for (int row = 0; row < rc; row++) {
                    renderer.setValue(getValueAt(row, column), column);
                    w = Math.max(w, renderer.getComponent().getPreferredSize().width);
                }

                setDefaultColumnWidth(column, w + COLUMN_MARGIN);
            }
        }
        
    }
    
    
    private static class ContentContainer extends JPanel {
        
        ContentContainer(HeapView view) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
            
            /*JLabel captionL = new JLabel(view.getName(), JLabel.LEADING);
//            captionL.setToolTipText(view.getDescription());
            captionL.setOpaque(false);
            captionL.setBorder(BorderFactory.createEmptyBorder(TABBUTTON_MARGIN_TOP, TABBUTTON_MARGIN_LEFT, TABBUTTON_MARGIN_BOTTOM, TABBUTTON_MARGIN_RIGHT));

            caption = new JPanel(new BorderLayout());
            caption.setOpaque(true);
            caption.setBackground(BACKGROUND_COLOR_NORMAL);
            caption.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, BORDER_COLOR_NORMAL));
            caption.add(captionL, BorderLayout.CENTER);
            
            add(caption, BorderLayout.NORTH);*/
            
            add(view.getComponent(), BorderLayout.CENTER);
        }
        
    }
    
    
    public static abstract class ContentProvider {
        
        public abstract HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders);
        
    }
    
}
