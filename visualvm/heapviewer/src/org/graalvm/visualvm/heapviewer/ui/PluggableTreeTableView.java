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

package org.graalvm.visualvm.heapviewer.ui;

import org.graalvm.visualvm.heapviewer.swing.MultiSplitContainer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.CloseButton;
import org.graalvm.visualvm.lib.ui.components.JExtendedSplitPane;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import java.awt.Container;
import javax.swing.JButton;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public class PluggableTreeTableView extends TreeTableView {
    
    private static final Color SEPARATOR_COLOR = UIManager.getColor("Separator.foreground"); // NOI18N
    
    private final List<HeapViewPlugin> plugins;
//    private Collection<? extends HeapViewPlugin.Provider> pluginProviders;
    
    private ProfilerToolbar toolbar;
    private JComponent pluginsComponent;
    
    
    public PluggableTreeTableView(String viewID, HeapContext context, HeapViewerActions actions, TreeTableViewColumn... columns) {
        this(viewID, context, actions, true, true, columns);
    }
    
    public PluggableTreeTableView(String viewID, HeapContext context, HeapViewerActions actions, boolean useBreadCrumbs, boolean pluggableColumns, TreeTableViewColumn... columns) {
        super(viewID, context, actions, useBreadCrumbs, pluggableColumns, columns);
        
        plugins = new ArrayList();
        Collection<? extends HeapViewPlugin.Provider> pluginProviders = Lookup.getDefault().lookupAll(HeapViewPlugin.Provider.class);
        for (HeapViewPlugin.Provider provider : pluginProviders) {
            HeapViewPlugin plugin = provider.createPlugin(context, actions, viewID);
            if (plugin != null) plugins.add(plugin);
        }
    }
    
    
    public boolean hasPlugins() {
        return !plugins.isEmpty();
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        super.nodeSelected(node, adjusting);
        for (HeapViewPlugin plugin : plugins) plugin.doNodeSelected(node, adjusting);
    }
    
    protected JComponent createComponent() {
        final JComponent comp = super.createComponent();
        
        if (toolbar == null) init();
        
        JExtendedSplitPane contentSplit = new JExtendedSplitPane(JExtendedSplitPane.VERTICAL_SPLIT, true, comp, pluginsComponent) {
            public boolean requestFocusInWindow() {
                return comp.requestFocusInWindow();
            }
        };
        BasicSplitPaneDivider contentDivider = ((BasicSplitPaneUI)contentSplit.getUI()).getDivider();
        contentDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARATOR_COLOR));
        contentDivider.setDividerSize(5);
        contentSplit.setBorder(BorderFactory.createEmptyBorder());
        contentSplit.setResizeWeight(0.7d);
        
        return contentSplit;
    }
    
    
    public void closed() {
        if (pluginsComponent != null && pluginsComponent.isVisible()) {
            for (Component comp : pluginsComponent.getComponents())
                if (comp.isVisible()) comp.setVisible(false); // PluginContainer.setVisible(false) calls plugin.closed()
        }
        super.closed();
    }
    
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        pluginsComponent = new MultiSplitContainer();
        pluginsComponent.setPreferredSize(new Dimension(300, 300));
        
        int pcount = plugins.size();
        for (int i = 0; i < pcount; i++) {
            HeapViewPlugin plugin = plugins.get(i);
            final PluginContainer[] container = new PluginContainer[1];
            PluginPresenter presenter = new PluginPresenter(plugin) {
                @Override
                Container getPluginContainer() { return container[0]; }
            };
            presenter.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            if (i == 0) presenter.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
            else if (i == pcount - 1) presenter.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
            else presenter.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
            container[0] = new PluginContainer(plugin, presenter);
            toolbar.add(presenter);
            pluginsComponent.add(container[0]);
        }
        
        checkVisibility(pluginsComponent);
    }
    
    private static void checkVisibility(JComponent comp) {
        if (comp == null) return;

        comp.invalidate();
        comp.revalidate();
        comp.doLayout();
        comp.repaint();

        for (Component c : comp.getComponents())
            if (c.isVisible()) {
                comp.setVisible(true);

                return;
            }

        comp.setVisible(false);
    }
    
    
    private static abstract class PluginPresenter extends JToggleButton {
        
        PluginPresenter(HeapViewPlugin plugin) {
            super(plugin.getName(), plugin.getIcon());
            setToolTipText(plugin.getDescription());
            putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N
        }
        
        abstract Container getPluginContainer();
        
        protected void fireItemStateChanged(ItemEvent e) {
            Container container = getPluginContainer();
            container.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            checkVisibility((JComponent)container.getParent());
        }
        
    }
    
    
    private static class PluginContainer extends JPanel {
        
        private final HeapViewPlugin plugin;
        
        PluginContainer(HeapViewPlugin plugin, final PluginPresenter pluginPresenter) {
            super(new BorderLayout());
            
            this.plugin = plugin;
            
            JButton closeButton = CloseButton.create(new Runnable() {
                public void run() { pluginPresenter.setSelected(false); }
            });
            closeButton.setFocusable(true);
            
            JPanel detailHeader = new JPanel(null);
            detailHeader.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, SEPARATOR_COLOR));
            detailHeader.setLayout(new BoxLayout(detailHeader, BoxLayout.LINE_AXIS));
            detailHeader.setBackground(UIUtils.getDarker(UIUtils.getProfilerResultsBackground()));
            JLabel captionL = new JLabel(plugin.getName(), plugin.getIcon(), JLabel.LEADING);
            captionL.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            detailHeader.add(captionL);            
            detailHeader.add(Box.createGlue());
            detailHeader.add(closeButton);
            
            add(detailHeader, BorderLayout.NORTH);
            setVisible(pluginPresenter.isSelected());
        }
        
        public void setVisible(boolean visible) {
            if (visible && getComponentCount() < 2)
                add(plugin.getComponent(), BorderLayout.CENTER);
            super.setVisible(visible);
            
            if (!visible) plugin.closed();
        }
        
    }
    
}
