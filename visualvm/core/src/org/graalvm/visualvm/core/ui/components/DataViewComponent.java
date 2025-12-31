/*
 * Copyright (c) 2007, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui.components;

import org.graalvm.visualvm.uisupport.UISupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * UI component displaying single subtab for opened DataSource.
 *
 * @author Jiri Sedlacek
 */
public final class DataViewComponent extends JPanel {

    /**
     * Top left details area of the view.
     */
    public static final int TOP_LEFT = 1;
    /**
     * Top right details area of the view.
     */
    public static final int TOP_RIGHT = 2;
    /**
     * Bottom left details area of the view.
     */
    public static final int BOTTOM_LEFT = 3;
    /**
     * Bottom right details area of the view.
     */
    public static final int BOTTOM_RIGHT = 4;

    private boolean isMasterViewResizable;

    private JPanel masterPanel;
    private JPanel detailsPanel;

    private JExtendedSplitPane detailsTopHorizontalSplitter;
    private JExtendedSplitPane detailsBottomHorizontalSplitter;
    private JExtendedSplitPane detailsVerticalSplitter;

    private DisplayArea masterArea;
    private DisplayArea detailsTopLeftArea;
    private DisplayArea detailsTopRightArea;
    private DisplayArea detailsBottomLeftArea;
    private DisplayArea detailsBottomRightArea;

    private static final Color BACKGROUND_COLOR;
    private static final Color HIGHLIGHT_BACKGROUND;


    static {
        BACKGROUND_COLOR = UISupport.getDefaultBackground();

        int darkerR = BACKGROUND_COLOR.getRed() - 20;
        if (darkerR < 0) darkerR += 40;
        int darkerG = BACKGROUND_COLOR.getGreen() - 20;
        if (darkerG < 0) darkerG += 40;
        int darkerB = BACKGROUND_COLOR.getBlue() - 20;
        if (darkerB < 0) darkerB += 40;

        HIGHLIGHT_BACKGROUND = new Color(darkerR, darkerG, darkerB);
    }
    
    
    /**
     * Creates new instance of DataViewComponent.
     * 
     * @param masterView MasterView for the component.
     * @param masterAreaConfiguration MasterAreaConfiguration for the component.
     */
    public DataViewComponent(MasterView masterView, MasterViewConfiguration masterAreaConfiguration) {
        initComponents();
        createMasterView(masterView);
        configureMasterView(masterAreaConfiguration);
    }
    
    
    private void configureMasterView(MasterViewConfiguration masterViewConfiguration) {
        setMasterViewResizable(masterViewConfiguration.isMasterViewResizable());
    }
    
    /**
     * Configures properties of the details view.
     * 
     * @param detailsViewConfiguration configuration for the details view.
     */
    public void configureDetailsView(DetailsViewConfiguration detailsViewConfiguration) {
        double topHorizontalDividerResizeWeight = detailsViewConfiguration.getTopHorizontalDividerResizeWeight();
        if (topHorizontalDividerResizeWeight != -1) detailsTopHorizontalSplitter.setResizeWeight(topHorizontalDividerResizeWeight);
        double topHorizontalDividerLocation = detailsViewConfiguration.getTopHorizontalDividerLocation();
        if (topHorizontalDividerLocation != -1) detailsTopHorizontalSplitter.setDividerLocation(topHorizontalDividerLocation);
        
        double bottomHorizontalDividerResizeWeight = detailsViewConfiguration.getBottomHorizontalDividerResizeWeight();
        if (bottomHorizontalDividerResizeWeight != -1) detailsBottomHorizontalSplitter.setResizeWeight(bottomHorizontalDividerResizeWeight);
        double bottomHorizontalDividerLocation = detailsViewConfiguration.getBottomHorizontalDividerLocation();
        if (bottomHorizontalDividerLocation != -1) detailsBottomHorizontalSplitter.setDividerLocation(bottomHorizontalDividerLocation);
        
        double verticalDividerResizeWeight = detailsViewConfiguration.getVerticalDividerResizeWeight();
        if (verticalDividerResizeWeight != -1) detailsVerticalSplitter.setResizeWeight(verticalDividerResizeWeight);
        double verticalDividerLocation = detailsViewConfiguration.getVerticalDividerLocation();
        if (verticalDividerLocation != -1) detailsVerticalSplitter.setDividerLocation(verticalDividerLocation);
    }
    
    /**
     * Shows details area on provided location.
     * 
     * @param location location to be shown.
     */
    public void showDetailsArea(int location) {
        DisplayArea displayArea = getDisplayArea(location);
        if (displayArea != null) displayArea.setVisible(true);
    }
    
    /**
     * Hides details area on provided location.
     * 
     * @param location location to be hidden.
     */
    public void hideDetailsArea(int location) {
        DisplayArea displayArea = getDisplayArea(location);
        if (displayArea != null) displayArea.setVisible(false);
    }
    
    /**
     * Returns true if the details area on provided location is currently shown, false otherwise.
     * @param location location to be checked.
     * @return true if the details area on provided location is currently shown, false otherwise.
     */
    public boolean isDetailsAreaShown(int location) {
        DisplayArea displayArea = getDisplayArea(location);
        return displayArea != null ? displayArea.isVisible() : false;
    }
    
    /**
     * Configures properties of the details area.
     * 
     * @param detailsAreaConfiguration configuration for the details area.
     * @param location location of the details area to be configured.
     */
    public void configureDetailsArea(DetailsAreaConfiguration detailsAreaConfiguration, int location) {
        DisplayArea displayArea = getDisplayArea(location);
        if (displayArea != null) {
            displayArea.setCaption(detailsAreaConfiguration.getName());
            displayArea.setClosable(detailsAreaConfiguration.isClosable());
        }
    }
    
    private void createMasterView(MasterView masterView) {
        masterPanel.setVisible(true);
        JComponent[] options = new JComponent[] { detailsTopLeftArea.getPresenter(), detailsTopRightArea.getPresenter(), detailsBottomLeftArea.getPresenter(), detailsBottomRightArea.getPresenter() };
        masterArea.addTab(new DisplayArea.Tab(masterView.getName(), masterView.getDescription(), 0, masterView.getView(), options));
    }
    
    /**
     * Adds new details view.
     * 
     * @param detailsView DetailsView to be added.
     * @param location location where the DetailsView will be added.
     */
    public void addDetailsView(DetailsView detailsView, int location) {
        DisplayArea displayArea = getDisplayArea(location);
        if (displayArea != null) {
            if ("".equals(displayArea.getCaption())) displayArea.setCaption(detailsView.getTab().getName());
            displayArea.addTab(detailsView.getTab());
            revalidate();
            repaint();
        }
    }
    
    /**
     * Removes details view.
     * 
     * @param detailsView DetailsView to be removed.
     */
    public void removeDetailsView(DetailsView detailsView) {
        DisplayArea displayArea = getDisplayArea(detailsView.getTab());
        if (displayArea != null) displayArea.removeTab(detailsView.getTab());
    }
    
    /**
     * Returns true if DataViewComponent contains the DetailsView, false otherwise.
     * 
     * @param detailsView DetailsView to check.
     * @return true if DataViewComponent contains the DetailsView, false otherwise.
     */
    public boolean containsDetailsView(DetailsView detailsView) {
        return getDisplayArea(detailsView.getTab()) != null;
    }
    
    /**
     * Selects the DetailsView.
     * 
     * @param detailsView DetailsView to be selected.
     */
    public void selectDetailsView(DetailsView detailsView) {
        DisplayArea displayArea = getDisplayArea(detailsView.getTab());
        if (displayArea != null) displayArea.setSelectedTab(detailsView.getTab());
    }
    
    
    private void setMasterViewResizable(boolean isMasterViewResizable) {            
        this.isMasterViewResizable = isMasterViewResizable;
        masterArea.setIgnoresContentsHeight(isMasterViewResizable);
        JComponent contents = null;

        if (isMasterViewResizable) {
            final CustomizedSplitPaneUI mainVerticalSplitterUI = new CustomizedSplitPaneUI();
            JExtendedSplitPane mainVerticalSplitter = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT, masterPanel, detailsPanel){
                public void updateUI() {
                    if (getUI() != mainVerticalSplitterUI)
                        setUI(mainVerticalSplitterUI);

                    setBorder(null);
                    setOpaque(false);
                    setDividerSize(6);
                    setContinuousLayout(true);

                    final BasicSplitPaneDivider divider = ((BasicSplitPaneUI) getUI()).getDivider();
                    divider.setBackground(BACKGROUND_COLOR);
                    divider.setBorder(null);

                    divider.addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) {
                            divider.setBackground(HIGHLIGHT_BACKGROUND);
                            divider.repaint();
                        }
                        public void mouseExited(MouseEvent e) {
                            divider.setBackground(BACKGROUND_COLOR);
                            divider.repaint();
                        }
                    });
                }
            };
            mainVerticalSplitter.setDividerLocation(0.5d);

            contents = mainVerticalSplitter;
        } else {
            JPanel containerPanel = new JPanel(new BorderLayout());
            containerPanel.setOpaque(false);
            containerPanel.add(masterPanel, BorderLayout.NORTH);
            containerPanel.add(detailsPanel, BorderLayout.CENTER);

            contents = containerPanel;
        }

        synchronized (getTreeLock()) {
            removeAll();
            add(contents, BorderLayout.CENTER);
        }
        
        revalidate();
        repaint();
    }

    private boolean isMasterAreaResizable() {
        return isMasterViewResizable;
    }
    
    
    private DisplayArea getDisplayArea(int location) {
        switch (location) {
            case TOP_LEFT:      return detailsTopLeftArea;
            case TOP_RIGHT:     return detailsTopRightArea;
            case BOTTOM_LEFT:   return detailsBottomLeftArea;
            case BOTTOM_RIGHT:  return detailsBottomRightArea;
            default:            return null;
        }
    }
    
    private DisplayArea getDisplayArea(DisplayArea.Tab tab) {
        if (detailsTopLeftArea.containsTab(tab)) return detailsTopLeftArea;
        if (detailsTopRightArea.containsTab(tab)) return detailsTopRightArea;
        if (detailsBottomLeftArea.containsTab(tab)) return detailsBottomLeftArea;
        if (detailsBottomRightArea.containsTab(tab)) return detailsBottomRightArea;
        
        return null;
    }
    

    private void initComponents() {

        // Top details area

        detailsTopLeftArea = new DisplayArea();
        detailsTopRightArea = new DisplayArea();

        final JPanel detailsTopPanel = new JPanel(new BorderLayout());
        detailsTopPanel.setOpaque(false);
        
        final CustomizedSplitPaneUI detailsTopHorizontalSplitterUI = new CustomizedSplitPaneUI();
        detailsTopHorizontalSplitter = new JExtendedSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailsTopLeftArea, detailsTopRightArea) {
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                detailsTopPanel.setVisible(visible);
                revalidate();
                repaint();
            }
            public void updateUI() {
                if (getUI() != detailsTopHorizontalSplitterUI)
                    setUI(detailsTopHorizontalSplitterUI);
                
                setBorder(null);
                setOpaque(false);
                setDividerSize(6);
                setContinuousLayout(true);

                final BasicSplitPaneDivider divider = ((BasicSplitPaneUI) getUI()).getDivider();
                divider.setBackground(BACKGROUND_COLOR);
                divider.setBorder(null);
                
                divider.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        divider.setBackground(HIGHLIGHT_BACKGROUND);
                        divider.repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        divider.setBackground(BACKGROUND_COLOR);
                        divider.repaint();
                    }
                });
            }
        };
        detailsTopHorizontalSplitter.setResizeWeight(0.5d);
        detailsTopHorizontalSplitter.setDividerLocation(0.5d);
        detailsTopPanel.add(detailsTopHorizontalSplitter, BorderLayout.CENTER);
        

        // Bottom details area

        detailsBottomLeftArea = new DisplayArea();
        detailsBottomRightArea = new DisplayArea();

        final JPanel detailsBottomPanel = new JPanel(new BorderLayout());            
        detailsBottomPanel.setOpaque(false);
        
        final CustomizedSplitPaneUI detailsBottomHorizontalSplitterUI = new CustomizedSplitPaneUI();
        detailsBottomHorizontalSplitter = new JExtendedSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailsBottomLeftArea, detailsBottomRightArea) {
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                detailsBottomPanel.setVisible(visible);
                revalidate();
                repaint();
            }
            public void updateUI() {
                if (getUI() != detailsBottomHorizontalSplitterUI)
                    setUI(detailsBottomHorizontalSplitterUI);
                
                setBorder(null);
                setOpaque(false);
                setDividerSize(6);
                setContinuousLayout(true);

                final BasicSplitPaneDivider divider = ((BasicSplitPaneUI) getUI()).getDivider();
                divider.setBackground(BACKGROUND_COLOR);
                divider.setBorder(null);
                
                divider.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        divider.setBackground(HIGHLIGHT_BACKGROUND);
                        divider.repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        divider.setBackground(BACKGROUND_COLOR);
                        divider.repaint();
                    }
                });
            }
        };
        detailsBottomHorizontalSplitter.setResizeWeight(0.5d);
        detailsBottomHorizontalSplitter.setDividerLocation(0.5d);
        detailsBottomPanel.add(detailsBottomHorizontalSplitter, BorderLayout.CENTER);
        

        // Details area

        detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setOpaque(false);
        detailsPanel.setVisible(false);

        final CustomizedSplitPaneUI detailsVerticalSplitterUI = new CustomizedSplitPaneUI();
        detailsVerticalSplitter = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT, detailsTopPanel, detailsBottomPanel) {
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                detailsPanel.setVisible(visible);
                revalidate();
                repaint();
            }
            public void updateUI() {
                if (getUI() != detailsVerticalSplitterUI)
                    setUI(detailsVerticalSplitterUI);
                
                setBorder(null);
                setOpaque(false);
                setDividerSize(6);
                setContinuousLayout(true);

                final BasicSplitPaneDivider divider = ((BasicSplitPaneUI) getUI()).getDivider();
                divider.setBackground(BACKGROUND_COLOR);
                divider.setBorder(null);
                
                divider.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        divider.setBackground(HIGHLIGHT_BACKGROUND);
                        divider.repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        divider.setBackground(BACKGROUND_COLOR);
                        divider.repaint();
                    }
                });
            }
        };
        detailsVerticalSplitter.setResizeWeight(0.5d);
        detailsVerticalSplitter.setDividerLocation(0.5d);
        detailsPanel.add(detailsVerticalSplitter, BorderLayout.CENTER);


        // Master area

        masterArea = new DisplayArea();
        masterArea.setClosable(false);

        masterPanel = new JPanel(new BorderLayout());
        masterPanel.setOpaque(false);
        masterPanel.setVisible(false);
        masterPanel.add(masterArea, BorderLayout.CENTER);
        
        
        // DataView
        
        setOpaque(true);
        setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout());
    }
    
    private static class CustomizedSplitPaneUI extends BasicSplitPaneUI {
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                public void paint(Graphics g) {
                    Dimension size = getSize();
                    g.setColor(getBackground());
                    g.fillRect(0, 0, size.width, size.height);
                }
            };
        }
    }
    
    /**
     * Master view of DataViewComponent. This is the upper part of the component showing the master contents and/or
     * controls for details views. Master view is always shown for each DataViewComponent.
     */
    public static class MasterView {
        
        private String name;
        private String description;
        private JComponent view;
        
        /**
         * Creates new instance of MasterView.
         * 
         * @param name name of the view.
         * @param description description of the view.
         * @param view UI component to be shown.
         */
        public MasterView(String name, String description, JComponent view) {
            this.name = name;
            this.description = description;
            this.view = view;
        }
        
        private String getName() { return name; }
        private String getDescription() { return description; }
        private JComponent getView() { return view; }
        
    }
    
    /**
     * Details view of DataViewComponent. This is an optional view of the DataViewComponent which can be hidden or
     * not used at all.
     */
    public static class DetailsView {
        
        private DisplayArea.Tab tab;
        
        /**
         * Creates new instance of DetailsView.
         * 
         * @param name name of the view.
         * @param description description of the view.
         * @param preferredPosition preferred position of the view among other views.
         * @param view UI component to be shown.
         * @param options components to be shown in view's caption.
         */
        public DetailsView(String name, String description, int preferredPosition, JComponent view, JComponent[] options) {
            tab = new DisplayArea.Tab(name, description, preferredPosition, view, options);
        }
        
        private DisplayArea.Tab getTab() { return tab; }
        
    }
    
    /**
     * Configuration of the MasterView.
     */
    public static class MasterViewConfiguration {
        
        private boolean isMasterViewResizable;
        
        /**
         * Creates new instance of MasterViewConfiguration.
         * 
         * @param isMasterAreaResizable controls if master area can be dynamically resized in the UI (effective only if at least one details view is displayed).
         */
        public MasterViewConfiguration(boolean isMasterAreaResizable) {
            this.isMasterViewResizable = isMasterAreaResizable;
        }
        
        private boolean isMasterViewResizable() { return isMasterViewResizable; }
        
    }
    
    /**
     * Configuration of the DetailsView.
     */
    public static class DetailsViewConfiguration {
        
        private double topHorizontalDividerLocation;
        private double topHorizontalDividerResizeWeight;
        private double bottomHorizontalDividerLocation;
        private double bottomHorizontalDividerResizeWeight;
        private double verticalDividerLocation;
        private double verticalDividerResizeWeight;
        
        /**
         * Creates new instance of DetailsViewConfiguration.
         * 
         * @param topHorizontalDividerLocation preferred relative location of top horizontal divider.
         * @param topHorizontalDividerResizeWeight preferred resize weight of top horizontal divider.
         * @param bottomHorizontalDividerLocation preferred relative location of bottom horizontal divider.
         * @param bottomHorizontalDividerResizeWeight preferred resize weight of bottom horizontal divider.
         * @param verticalDividerLocation preferred location of vertical divider.
         * @param verticalDividerResizeWeight preferred resize weight of vertical divider.
         */
        public DetailsViewConfiguration(double topHorizontalDividerLocation, double topHorizontalDividerResizeWeight, double bottomHorizontalDividerLocation,
                                        double bottomHorizontalDividerResizeWeight, double verticalDividerLocation, double verticalDividerResizeWeight) {
            this.topHorizontalDividerLocation = topHorizontalDividerLocation;
            this.topHorizontalDividerResizeWeight = topHorizontalDividerResizeWeight;
            this.bottomHorizontalDividerLocation = bottomHorizontalDividerLocation;
            this.bottomHorizontalDividerResizeWeight = bottomHorizontalDividerResizeWeight;
            this.verticalDividerLocation = verticalDividerLocation;
            this.verticalDividerResizeWeight = verticalDividerResizeWeight;
        }
        
        /**
         * Returns preferred relative location of top horizontal divider.
         * @return preferred relative location of top horizontal divider.
         */
        public double getTopHorizontalDividerLocation() { return topHorizontalDividerLocation; }
        /**
         * Returns preferred resize weight of top horizontal divider.
         * @return preferred resize weight of top horizontal divider.
         */
        public double getTopHorizontalDividerResizeWeight() { return topHorizontalDividerResizeWeight; }
        /**
         * Returns preferred relative location of bottom horizontal divider.
         * @return preferred relative location of bottom horizontal divider.
         */
        public double getBottomHorizontalDividerLocation() { return bottomHorizontalDividerLocation; }
        /**
         * Returns preferred resize weight of bottom horizontal divider.
         * @return preferred resize weight of bottom horizontal divider.
         */
        public double getBottomHorizontalDividerResizeWeight() { return bottomHorizontalDividerResizeWeight; }
        /**
         * Returns preferred relative location of vertical divider.
         * @return preferred relative location of vertical divider.
         */
        public double getVerticalDividerLocation() { return verticalDividerLocation; }
        /**
         * Returns preferred resize weight of vertical divider.
         * @return preferred resize weight of vertical divider.
         */
        public double getVerticalDividerResizeWeight() { return verticalDividerResizeWeight; }
        
    }
    
    /**
     * Configuration for details area.
     */
    public static class DetailsAreaConfiguration {
        
        private String name;
        private boolean closable;
        
        /**
         * Creates new instance of DetailsAreaConfiguration.
         * 
         * @param name name of the details area.
         * @param closable controls if the details area is closable or always shown.
         */
        public DetailsAreaConfiguration(String name, boolean closable) {
            this.name = name;
            this.closable = closable;
        }
        
        private String getName() { return name; }
        private boolean isClosable() { return closable; }
        
    }

}
