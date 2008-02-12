/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.tools.visualvm.core.ui.components;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class DisplayArea extends JComponent {

    private boolean ignoresContentsHeight = true;

    public DisplayArea() {
        initComponents();
        setClosable(true);
        setVisible(false); // No tabs added yet
    }
    
    public void setCaption(String caption) {
        this.caption = caption;
        if (presenter != null) presenter.setCaption(caption);
    }
    
    public String getCaption() {
        return caption;
    }
    
    public void setClosable(boolean closable) {
        optionsContainer.setClosable(closable);
        updatePresenter();
    }

    public boolean isClosable() {
        return optionsContainer.isClosable();
    }
    
    public void setIgnoresContentsHeight(boolean ignoresContentsHeight) {
        this.ignoresContentsHeight = ignoresContentsHeight;
    }
    
    public boolean ignoresContentsHeight() {
        return ignoresContentsHeight;
    }
    
    public Presenter getPresenter() {
        if (presenter == null) {
            presenter = createPresenter();
            updatePresenter();
        }
        return presenter;
    }

    public void addTab(Tab tab) {
        DisplayAreaSupport.TabButton tabButton = tabsContainer.addTab(tab);
        if (tabButton != null) {
            optionsContainer.addOptions(tab);
            updateTabbed();
            if (tabsContainer.getTabsCount() == 1) setSelectedTab(tab);
            final Tab tabF = tab;
            tabButton.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) setSelectedTab(tabF); }
            });
            tabButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { setSelectedTab(tabF); }
            });
        }
        
        if (tabsContainer.getTabsCount() > 0) setVisible(true);
        updatePresenter();
    }
    
    public void insertTab(Tab tab, int position) {
        // adding the tab to the last available position
        if (position >= tabsContainer.getTabsCount()) {
            addTab(tab);
            return;
        } 
        
        DisplayAreaSupport.TabButton tabButton = tabsContainer.insertTab(tab, position);
        if (tabButton != null) {
            optionsContainer.addOptions(tab);
            updateTabbed();
            if (position == 0) setSelectedTab(tab);
            final Tab tabF = tab;
            tabButton.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) setSelectedTab(tabF); }
            });
            tabButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { setSelectedTab(tabF); }
            });
        }
        
        if (tabsContainer.getTabsCount() > 0) setVisible(true);
        updatePresenter();
    }
    
    public void removeTab(Tab tab) {
        Tab toSelect = null;
        boolean wasSelected = getSelectedTab() == tab;        
        if (wasSelected) {
            toSelect = tabsContainer.getPreviousTab(tab);
            if (toSelect == null) toSelect = tabsContainer.getNextTab(tab);
        }
        
        if (tabsContainer.removeTab(tab)) {
            optionsContainer.removeOptions(tab);
            
            if (wasSelected) {
                if (toSelect != null) setSelectedTab(toSelect);
                else viewContainer.setSelectedView(null);
            }
            
            updateTabbed();
        }
        
        if (tabsContainer.getTabsCount() == 0) setVisible(false);
        updatePresenter();
    }
    
    public boolean containsTab(Tab tab) {
        return tabsContainer.containsTab(tab);
    }
    
    public void setSelectedTab(Tab tab) {
        if (tabsContainer.getSelectedTab() == tab) return;
        
        tabsContainer.setSelectedTab(tab);
        optionsContainer.setSelectedOptions(tab);
        viewContainer.setSelectedView(tab.getView());
    }
    
    public Tab getSelectedTab() {
        return tabsContainer.getSelectedTab();
    }
    
    
    public Dimension getPreferredSize() {
        if (ignoresContentsHeight()) return new Dimension(0, tabsContainer.getPreferredSize().height);
        else return super.getPreferredSize();
    }
    
    public Dimension getMinimumSize() {
        if (ignoresContentsHeight()) return getPreferredSize();
        else return super.getMinimumSize();
    }
    
    
    private void updatePresenter() {
        if (presenter == null) return;
        presenter.setVisible(tabsContainer.getTabsCount() > 0 && isClosable());
    }
    
    private void updateTabbed() {
        boolean tabbed = tabsContainer.getTabsCount() > 1;
        middleSpacer.updateTabbed(tabbed);
        optionsContainer.updateTabbed(tabbed);
    }
    
    private Presenter createPresenter() {
        final Presenter presenter = new Presenter();
        presenter.setCaption(caption);
        presenter.setOpaque(false);
        final boolean[] internalChange = new boolean[1];
        internalChange[0] = false;
        
        presenter.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               internalChange[0] = true;
               setVisible(presenter.isSelected());
               internalChange[0] = false;
           } 
        });
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    boolean isShowing = isShowing();
                    presenter.setToolTipText(isShowing ? "Hide " + caption : "Show " + caption);
                    if (!internalChange[0]) presenter.setSelected(isShowing);
                }
            }
        });
        
        return presenter;
    }

    private void initComponents() {
        viewContainer = new ViewArea();
        
        tabsContainer = new TabsContainer();
        middleSpacer = new MiddleSpacer();
        optionsContainer = new OptionsContainer();
        
        JPanel captionArea = new JPanel();
        captionArea.setLayout(new BorderLayout());
        captionArea.setOpaque(false);
        
        captionArea.add(tabsContainer, BorderLayout.WEST);
        captionArea.add(middleSpacer, BorderLayout.CENTER);
        captionArea.add(optionsContainer, BorderLayout.EAST);
        
        setLayout(new BorderLayout());
        add(captionArea, BorderLayout.NORTH);
        add(viewContainer, BorderLayout.CENTER);
    }
    
    private String caption = "";
    private Presenter presenter;
    
    private ViewArea viewContainer;
    
    private TabsContainer tabsContainer;
    private MiddleSpacer middleSpacer;
    private OptionsContainer optionsContainer;
    
    
    public static class Presenter extends JCheckBox {
        
        public void setCaption(String caption) { setText(caption); }
        public String getCaption() { return getText(); }
        
        public void setDescription(String description) { setToolTipText(description); }
        public String getDescription() { return getToolTipText(); }
        
    }
    
    
    public static class Tab {
        
        private String name;
        private String description;
        private JComponent view;
        private JComponent[] options;
        
        public Tab(String name) { this(name, null, null, null); }
        public Tab(String name, JComponent view) { this(name, null, view, null); }
        public Tab(String name, String description, JComponent view, JComponent[] options) {
            setName(name);
            setDescription(description);
            setView(view);
            setOptions(options);
        }
        
        public void setName(String name) { this.name = name; }
        public String getName() { return name; }
        
        public void setDescription(String description) { this.description = description; }
        public String getDescription() { return description; }
        
        public void setView(JComponent view) { this.view = view; }
        public JComponent getView() { return view; }
        
        public void setOptions(JComponent[] options) { this.options = options; }
        public JComponent[] getOptions() { return options; }
        
    }
    

    private static class TabsContainer extends JPanel {
        
        private List<Tab> tabs = new ArrayList();
        private Tab selectedTab;

        private TabsContainer() {
            setLayout(null);            
            setOpaque(true);
            setBackground(DisplayAreaSupport.BACKGROUND_COLOR_NORMAL);
        }
        
        
        private DisplayAreaSupport.TabButton addTab(Tab tab) {
            if (tabs.contains(tab)) return null;
            
            if (getLayout() == null) setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            tabs.add(tab);
            
            DisplayAreaSupport.TabButton tabButton = new DisplayAreaSupport.TabButton(tab.getName(), tab.getDescription());
            DisplayAreaSupport.TabButtonContainer tabButtonContainer = new DisplayAreaSupport.TabButtonContainer(tabButton);
            tabButtonContainer.setAlignmentY(JComponent.TOP_ALIGNMENT);
            add(tabButtonContainer);
            
            updateTabButtons();
            
            return tabButton;
        }
        
        private DisplayAreaSupport.TabButton insertTab(Tab tab, int position) {
            if (tabs.contains(tab)) return null;
            
            if (getLayout() == null) setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            tabs.add(position, tab);
            
            DisplayAreaSupport.TabButton tabButton = new DisplayAreaSupport.TabButton(tab.getName(), tab.getDescription());
            DisplayAreaSupport.TabButtonContainer tabButtonContainer = new DisplayAreaSupport.TabButtonContainer(tabButton);
            tabButtonContainer.setAlignmentY(JComponent.TOP_ALIGNMENT);
            add(tabButtonContainer, position);
            
            updateTabButtons();
            
            return tabButton;
        }

        private boolean removeTab(Tab tab) {
            int index = tabs.indexOf(tab);
            if (index != -1) {
                tabs.remove(index);
                remove(index);
                if (tab == selectedTab) selectedTab = null;
                updateTabButtons();
                
                return true;
            }
            
            return false;
        }
        
        private boolean containsTab(Tab tab) {
            return tabs.contains(tab);
        }
        
        private void setSelectedTab(Tab tab) {
            selectedTab = tab;
            updateTabButtons();
        }
        
        private Tab getSelectedTab() {
            return selectedTab;
        }
        
        private Tab getPreviousTab(Tab tab) {
            int index = tabs.indexOf(tab);
            return index > 0 ? tabs.get(--index) : null;
        }
        
        private Tab getNextTab(Tab tab) {
            int index = tabs.indexOf(tab);
            return index < tabs.size() - 1 ? tabs.get(++index) : null;
        }
        
        private int getTabsCount() {
            return tabs.size();
        }
        
        private void updateTabButtons() {
            int tabIndex = tabs.indexOf(selectedTab);
            Component[] components = getComponents();
            int componentsCount = components.length;
            for (int i = 0; i < componentsCount; i++) ((DisplayAreaSupport.TabButtonContainer)components[i]).updateTabButton(i, tabIndex, componentsCount);
        }
        
    }
    
    private static class MiddleSpacer extends JPanel {
        
        private MiddleSpacer() {
            setLayout(null);
            setOpaque(true);
            setBackground(DisplayAreaSupport.BACKGROUND_COLOR_NORMAL);
        }
        
        private void updateTabbed(boolean tabbed) {
            if (tabbed)
                setBorder(new DisplayAreaSupport.TabbedCaptionBorder(
                    DisplayAreaSupport.BORDER_COLOR_NORMAL, DisplayAreaSupport.COLOR_NONE,
                    DisplayAreaSupport.BORDER_COLOR_HIGHLIGHT, DisplayAreaSupport.COLOR_NONE));
            else
                setBorder(new DisplayAreaSupport.TabbedCaptionBorder(
                    DisplayAreaSupport.BORDER_COLOR_NORMAL, DisplayAreaSupport.COLOR_NONE,
                    DisplayAreaSupport.BACKGROUND_COLOR_NORMAL, DisplayAreaSupport.COLOR_NONE));
        }
        
    }
    
    private static class OptionsContainer extends JPanel {
        
        private JPanel contentsPanel;
        private JButton closeButton;
        private CardLayout layout = new CardLayout(0, 0);
        private Map<Tab, JPanel> tabsMapper = new HashMap<Tab, JPanel>();
        
        private OptionsContainer() {
            initComponents();
        }
        
        private void setClosable(boolean closable) {
            closeButton.setVisible(closable);
        }
        
        private boolean isClosable() {
            return closeButton.isVisible();
        }
        
        private void addOptions(Tab tab) {
            JPanel optionsContainer = new JPanel();
            optionsContainer.setLayout(new BoxLayout(optionsContainer, BoxLayout.X_AXIS));
            optionsContainer.setOpaque(false);
            
            JComponent[] options = tab.getOptions();
            if (options != null) for (JComponent option : options) {
                option.setBorder(BorderFactory.createEmptyBorder(3, 5, 2, DisplayAreaSupport.TABBUTTON_MARGIN_RIGHT));
                option.setAlignmentY(JComponent.CENTER_ALIGNMENT);
                optionsContainer.add(option);
            }
            
            tabsMapper.put(tab, optionsContainer);
            contentsPanel.add(optionsContainer, tab.getName());
        }
       
        private void removeOptions(Tab tab) {
            JPanel optionsContainer = tabsMapper.remove(tab);
            if (optionsContainer != null) contentsPanel.remove(optionsContainer);
        }
        
        private void setSelectedOptions(Tab tab) {
            layout.show(contentsPanel, tab.getName());
        }
        
        private void updateTabbed(boolean tabbed) {
            if (tabbed)
                setBorder(new DisplayAreaSupport.TabbedCaptionBorder(
                    DisplayAreaSupport.BORDER_COLOR_NORMAL, DisplayAreaSupport.COLOR_NONE,
                    DisplayAreaSupport.BORDER_COLOR_HIGHLIGHT, DisplayAreaSupport.BORDER_COLOR_NORMAL));
            else
                setBorder(new DisplayAreaSupport.TabbedCaptionBorder(
                    DisplayAreaSupport.BORDER_COLOR_NORMAL, DisplayAreaSupport.COLOR_NONE,
                    DisplayAreaSupport.COLOR_NONE, DisplayAreaSupport.BORDER_COLOR_NORMAL));
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(DisplayAreaSupport.BACKGROUND_COLOR_NORMAL);
            
            ImageIcon closeIcon = new ImageIcon(DisplayArea.class.getResource("/com/sun/tools/visualvm/core/ui/resources/closePanel.png")); // NOI18N
            closeButton = new DisplayAreaSupport.ImageIconButton(closeIcon);
            closeButton.setToolTipText("Hide");
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { getParent().getParent().setVisible(false); }
            });
            
            contentsPanel = new JPanel(layout);
            contentsPanel.setOpaque(false);
            add(contentsPanel, BorderLayout.WEST);
            add(closeButton, BorderLayout.EAST);
        }
        
    }
    
    private static class ViewArea extends JPanel {
        
//        private JScrollPane viewScroll;
        
        private ViewArea() {
//            viewScroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//            viewScroll.setBorder(BorderFactory.createEmptyBorder());
//            viewScroll.setViewportBorder(BorderFactory.createEmptyBorder());
//            viewScroll.setOpaque(false);
//            viewScroll.getViewport().setOpaque(false);
            
            setLayout(new BorderLayout());
            setOpaque(false);
            
//            add(viewScroll, BorderLayout.CENTER);
        }
        
        private void setSelectedView(JComponent component) {
//            viewScroll.setViewportView(component);
            synchronized (getTreeLock()) {
                removeAll();
                if (component != null) add(component, BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        }
        
    }
    
}
