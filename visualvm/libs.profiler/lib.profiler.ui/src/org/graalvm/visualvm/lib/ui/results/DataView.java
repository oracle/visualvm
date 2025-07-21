/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.results;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class DataView extends JPanel {

    protected DataView() {
        super(new BorderLayout());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() { SearchUtils.enableSearchActions(getResultsComponent()); }
        });
    }


    // --- View UI -------------------------------------------------------------

    protected abstract ProfilerTable getResultsComponent();

    public final JMenuItem createCopyMenuItem() {
        return getResultsComponent().createCopyMenuItem();
    }

    public final JMenuItem[] createCustomMenuItems(JComponent invoker, Object value, ClientUtils.SourceCodeSelection userValue) {
        Collection<? extends PopupCustomizer> customizers = Lookup.getDefault().lookupAll(PopupCustomizer.class);
        if (customizers.isEmpty()) return null;
        
        List<JMenuItem> menuItems = new ArrayList<>(customizers.size());
        for (PopupCustomizer customizer : customizers) {
            JMenuItem[] items = customizer.getMenuItems(invoker, this, value, userValue);
            if (items != null) Collections.addAll(menuItems, items);
        }
        
        return menuItems.isEmpty() ? null : menuItems.toArray(new JMenuItem[0]);
    }
    
    public void notifyOnFocus(final Runnable handler) {
        getResultsComponent().addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { handler.run(); }
        });
    }
    
    
    // --- Default action ------------------------------------------------------
    
    protected void installDefaultAction() {
        getResultsComponent().setDefaultAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int row = getResultsComponent().getSelectedRow();
                ClientUtils.SourceCodeSelection userValue = getUserValueForRow(row);
                if (userValue != null) performDefaultAction(userValue);
            }
        });
    }
    
    protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {}
    
    protected ClientUtils.SourceCodeSelection getUserValueForRow(int row) { return null; }
    
    
    // --- Filter & Find support -----------------------------------------------
    
    private JComponent bottomPanel;
    private JComponent filterPanel;
    private JComponent searchPanel;
    
//    public boolean filterActive() {
//        return filterPanel == null ? false : filterPanel.isVisible();
//    }
    
    protected RowFilter getExcludesFilter() {
        return null;
    }
    
    protected Component[] getFilterOptions() {
        return null;
    }
    
    protected void enableFilter() {
        if (filterPanel != null) {
            Object a = filterPanel.getClientProperty("SET_FILTER_CHANGED"); // NOI18N
            if (a instanceof Action) ((Action)a).actionPerformed(null);
        }
    }
    
    public void activateFilter() {
        JComponent panel = getBottomPanel();
        
        if (filterPanel == null) {
            filterPanel = FilterUtils.createFilterPanel(getResultsComponent(), getExcludesFilter(), getFilterOptions());
            panel.add(filterPanel);
            Container parent = panel.getParent();
            parent.invalidate();
            parent.revalidate();
            parent.repaint();
        }
        
        panel.setVisible(true);
        
        filterPanel.setVisible(true);
        filterPanel.requestFocusInWindow();
    }
    
    
//    public boolean searchActive() {
//        return searchPanel == null ? false : searchPanel.isVisible();
//    }
    
    protected SearchUtils.TreeHelper getSearchHelper() {
        return null;
    }
    
    protected Component[] getSearchOptions() {
        return null;
    }
    
    public void activateSearch() {
        JComponent panel = getBottomPanel();
        
        if (searchPanel == null) {
            SearchUtils.TreeHelper searchHelper = getSearchHelper();
            if (searchHelper == null) searchPanel = SearchUtils.createSearchPanel(getResultsComponent(), getSearchOptions());
            else searchPanel = SearchUtils.createSearchPanel((ProfilerTreeTable)getResultsComponent(), searchHelper, getSearchOptions());
            panel.add(searchPanel);
            Container parent = panel.getParent();
            parent.invalidate();
            parent.revalidate();
            parent.repaint();
        }
        
        panel.setVisible(true);
        
        searchPanel.setVisible(true);
        searchPanel.requestFocusInWindow();
    }
    
    
    protected boolean hasBottomFilterFindMargin() {
        return false;
    }
    
    protected void addFilterFindPanel(JComponent comp) {
        add(comp, BorderLayout.SOUTH);
    }
    
    
    private JComponent getBottomPanel() {
        if (bottomPanel == null) {
            bottomPanel = new JPanel(new FilterFindLayout());
            bottomPanel.setOpaque(true);
            bottomPanel.setBackground(UIManager.getColor("controlShadow")); // NOI18N
            addFilterFindPanel(bottomPanel);
        }
        return bottomPanel;
    }
    
    
    private final class FilterFindLayout implements LayoutManager {

        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            JComponent filter = filterPanel;
            if (filter != null && !filter.isVisible()) filter = null;
            
            JComponent search = searchPanel;
            if (search != null && !search.isVisible()) search = null;
            
            Dimension dim = new Dimension();
            
            if (filter != null && search != null) {
                Dimension dim1 = filter.getPreferredSize();
                Dimension dim2 = search.getPreferredSize();
                dim.width = dim1.width + dim2.width + 1;
                dim.height = Math.max(dim1.height, dim2.height);
            } else if (filter != null) {
                dim = filter.getPreferredSize();
            } else if (search != null) {
                dim = search.getPreferredSize();
            }
            
            if ((filter != null || search != null) && hasBottomFilterFindMargin())
                dim.height += 1;
            
            return dim;
        }

        public Dimension minimumLayoutSize(Container parent) {
            JComponent filter = filterPanel;
            if (filter != null && !filter.isVisible()) filter = null;
            
            JComponent search = searchPanel;
            if (search != null && !search.isVisible()) search = null;
            
            Dimension dim = new Dimension();
            
            if (filter != null && search != null) {
                Dimension dim1 = filter.getMinimumSize();
                Dimension dim2 = search.getMinimumSize();
                dim.width = dim1.width + dim2.width + 1;
                dim.height = Math.max(dim1.height, dim2.height);
            } else if (filter != null) {
                dim = filter.getMinimumSize();
            } else if (search != null) {
                dim = search.getMinimumSize();
            }
            
            if ((filter != null || search != null) && hasBottomFilterFindMargin())
                dim.height += 1;
            
            return dim;
        }

        public void layoutContainer(Container parent) {
            JComponent filter = filterPanel;
            if (filter != null && !filter.isVisible()) filter = null;
            
            JComponent search = searchPanel;
            if (search != null && !search.isVisible()) search = null;
            
            int bottomOffset = hasBottomFilterFindMargin() ? 1 : 0;
            
            if (filter != null && search != null) {
                Dimension size = parent.getSize();
                int w = (size.width - 1) / 2;
                filter.setBounds(0, 0, w, size.height - bottomOffset);
                search.setBounds(w + 1, 0, size.width - w - 1, size.height - bottomOffset);
            } else if (filter != null) {
                Dimension size = parent.getSize();
                filter.setBounds(0, 0, size.width, size.height - bottomOffset);
            } else if (search != null) {
                Dimension size = parent.getSize();
                search.setBounds(0, 0, size.width, size.height - bottomOffset);
            }
        }
        
    }
    
    
    public static abstract class PopupCustomizer {
        
        public abstract JMenuItem[] getMenuItems(JComponent invoker, DataView source, Object value, ClientUtils.SourceCodeSelection userValue);
        
    }
    
}
