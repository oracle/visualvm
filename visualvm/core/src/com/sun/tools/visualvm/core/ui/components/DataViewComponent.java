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
 *
 * @author Jiri Sedlacek
 */
public final class DataViewComponent extends JPanel {

    public static final int TOP_LEFT = 1;
    public static final int TOP_RIGHT = 2;
    public static final int BOTTOM_LEFT = 3;
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
    
    
    public DataViewComponent(MasterView masterView, MasterViewConfiguration masterAreaConfiguration) {
        initComponents();
        createMasterView(masterView);
        configureMasterView(masterAreaConfiguration);
    }
    
    
    private void configureMasterView(MasterViewConfiguration masterViewConfiguration) {
        setMasterViewResizable(masterViewConfiguration.isMasterViewResizable());
    }
    
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
    
    public void showDetailsArea(int location) {
        DisplayArea displayArea = getDisplayArea(location);
        if (displayArea != null) displayArea.setVisible(true);
    }
    
    public void hideDetailsArea(int location) {
        DisplayArea displayArea = getDisplayArea(location);
        if (displayArea != null) displayArea.setVisible(false);
    }
    
    public boolean isDetailsAreaShown(int location) {
        DisplayArea displayArea = getDisplayArea(location);
        return displayArea != null ? displayArea.isVisible() : false;
    }
    
    // Configures area (closable etc.)
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
    
    // Adds a tab to details area
    public void addDetailsView(DetailsView detailsView, int location) {
        DisplayArea displayArea = getDisplayArea(location);
        if (displayArea != null) {
            if ("".equals(displayArea.getCaption())) displayArea.setCaption(detailsView.getTab().getName());
            displayArea.addTab(detailsView.getTab());
            revalidate();
            repaint();
        }
    }
    
    // Removes a tab from details area
    public void removeDetailsView(DetailsView detailsView) {
        DisplayArea displayArea = getDisplayArea(detailsView.getTab());
        if (displayArea != null) displayArea.removeTab(detailsView.getTab());
    }
    
    public boolean containsDetailsView(DetailsView detailsView) {
        return getDisplayArea(detailsView.getTab()) != null;
    }
    
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
                    divider.setBackground(Color.WHITE);
                    divider.setBorder(null);

                    divider.addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) {
                            divider.setBackground(new Color(235, 235, 235));
                            divider.repaint();
                        }
                        public void mouseExited(MouseEvent e) {
                            divider.setBackground(Color.WHITE);
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
                divider.setBackground(Color.WHITE);
                divider.setBorder(null);
                
                divider.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        divider.setBackground(new Color(235, 235, 235));
                        divider.repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        divider.setBackground(Color.WHITE);
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
                divider.setBackground(Color.WHITE);
                divider.setBorder(null);
                
                divider.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        divider.setBackground(new Color(235, 235, 235));
                        divider.repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        divider.setBackground(Color.WHITE);
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
                divider.setBackground(Color.WHITE);
                divider.setBorder(null);
                
                divider.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        divider.setBackground(new Color(235, 235, 235));
                        divider.repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        divider.setBackground(Color.WHITE);
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
        setBackground(Color.WHITE);
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
    
    public static class MasterView {
        
        private String name;
        private String description;
        private JComponent view;
        
        public MasterView(String name, String description, JComponent view) {
            this.name = name;
            this.description = description;
            this.view = view;
        }
        
        private String getName() { return name; }
        private String getDescription() { return description; }
        private JComponent getView() { return view; }
        
    }
    
    public static class DetailsView {
        
        private DisplayArea.Tab tab;
        
        public DetailsView(String name, String description, int preferredPosition, JComponent view, JComponent[] options) {
            tab = new DisplayArea.Tab(name, description, preferredPosition, view, options);
        }
        
        private DisplayArea.Tab getTab() { return tab; }
        
    }
    
    public static class MasterViewConfiguration {
        
        private boolean isMasterViewResizable;
        
        public MasterViewConfiguration(boolean isMasterAreaResizable) {
            this.isMasterViewResizable = isMasterAreaResizable;
        }
        
        private boolean isMasterViewResizable() { return isMasterViewResizable; }
        
    }
    
    public static class DetailsViewConfiguration {
        
        private double topHorizontalDividerLocation;
        private double topHorizontalDividerResizeWeight;
        private double bottomHorizontalDividerLocation;
        private double bottomHorizontalDividerResizeWeight;
        private double verticalDividerLocation;
        private double verticalDividerResizeWeight;
        
        public DetailsViewConfiguration(double topHorizontalDividerLocation, double topHorizontalDividerResizeWeight, double bottomHorizontalDividerLocation,
                                        double bottomHorizontalDividerResizeWeight, double verticalDividerLocation, double verticalDividerResizeWeight) {
            this.topHorizontalDividerLocation = topHorizontalDividerLocation;
            this.topHorizontalDividerResizeWeight = topHorizontalDividerResizeWeight;
            this.bottomHorizontalDividerLocation = bottomHorizontalDividerLocation;
            this.bottomHorizontalDividerResizeWeight = bottomHorizontalDividerResizeWeight;
            this.verticalDividerLocation = verticalDividerLocation;
            this.verticalDividerResizeWeight = verticalDividerResizeWeight;
        }
        
        public double getTopHorizontalDividerLocation() { return topHorizontalDividerLocation; };
        public double getTopHorizontalDividerResizeWeight() { return topHorizontalDividerResizeWeight; };
        public double getBottomHorizontalDividerLocation() { return bottomHorizontalDividerLocation; };
        public double getBottomHorizontalDividerResizeWeight() { return bottomHorizontalDividerResizeWeight; };
        public double getVerticalDividerLocation() { return verticalDividerLocation; };
        public double getVerticalDividerResizeWeight() { return verticalDividerResizeWeight; };
        
    }
    
    public static class DetailsAreaConfiguration {
        
        private String name;
        private boolean closable;
        
        public DetailsAreaConfiguration(String name, boolean closable) {
            this.name = name;
            this.closable = closable;
        }
        
        private String getName() { return name; }
        private boolean isClosable() { return closable; }
        
    }

}
