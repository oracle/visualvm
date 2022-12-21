/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.graalvm.visualvm.uisupport.UISupport;


/**
 *
 * @author Jiri Sedlacek
 */
public class Splitter extends JSplitPane {
    
    private CustomizedSplitPaneUI splitterUI;

    private HierarchyListener leftComponentListener;
    private HierarchyListener rightComponentListener;
    
    private double dividerLocation;
    private int customDividerSize;
    private double requestedDividerLocation = -1;
    
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

    
    public Splitter(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        this(newOrientation, false, newLeftComponent, newRightComponent);
    }

    public Splitter(int newOrientation, boolean newContinuousLayout, Component newLeftComponent,
                              Component newRightComponent) {
        super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);

        updateVisibility();

        if (!newLeftComponent.isVisible())
            computeDividerLocationWhenInitiallyHidden(newLeftComponent);

        if (!newRightComponent.isVisible())
            computeDividerLocationWhenInitiallyHidden(newRightComponent);
        
        setResizeWeight(0.5d);
        setDividerLocation(0.5d);
    }


    public void setDividerSize(int newSize) {
        super.setDividerSize(newSize);
        customDividerSize = newSize;
    }

    public void setDividerLocation(double requestedDividerLocation) {
        Component divider = getDivider();
        if (isVisible() && divider.isVisible()) { // SplitPane fully visible
            super.setDividerLocation(requestedDividerLocation);
            dividerLocation = requestedDividerLocation;
        } else if (isVisible()) { // Divider not visible, will be updated in updateVisibility()
            dividerLocation = requestedDividerLocation;
        } else if (!isVisible()) { // SplitPane not visible, dividerLocation will be set on first reasonable getSize()
            this.requestedDividerLocation = requestedDividerLocation;
        }
    }


    public void setLeftComponent(Component newLeftComponent) {
        if (leftComponent != null) {
            leftComponent.removeHierarchyListener(leftComponentListener);
            leftComponentListener = null;
        }

        super.setLeftComponent(newLeftComponent);

        if (getLeftComponent() != null) {
            leftComponentListener = new VisibilityListener(newLeftComponent);
            newLeftComponent.addHierarchyListener(leftComponentListener);
        }

        updateVisibility();
    }

    public void setRightComponent(Component newRightComponent) {
        if (rightComponent != null) {
            rightComponent.removeHierarchyListener(rightComponentListener);
            rightComponentListener = null;
        }

        super.setRightComponent(newRightComponent);

        if (getRightComponent() != null) {
            rightComponentListener = new VisibilityListener(newRightComponent);
            newRightComponent.addHierarchyListener(rightComponentListener);
        }

        updateVisibility();
    }
    

    public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
        if (width > 0 && height > 0 && requestedDividerLocation != -1) {
            super.setDividerLocation(requestedDividerLocation);
            dividerLocation = requestedDividerLocation;
            // SplitPaneUI.paint() needs to be invoked here to set the
            // BasicSplitPaneUI.painted flag to enable resizing the divider
            // even if the component hasn't been shown yet.
            ((BasicSplitPaneUI)getUI()).paint(getGraphics(), this);
            requestedDividerLocation = -1;
        }
    }

    
    private Component getDivider() {
        if (ui == null) return null;
        return ((BasicSplitPaneUI)ui).getDivider();
    }

    private void computeDividerLocationWhenHidden(Component hiddenComponent) {
        if (leftComponent.isVisible() || rightComponent.isVisible()) {
            boolean horiz = getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
            double size  = horiz ? getSize().getWidth() :
                                   getSize().getHeight();
            double csize = horiz ? hiddenComponent.getSize().getWidth() :
                                   hiddenComponent.getSize().getHeight();
            computeDividerLocation(hiddenComponent, size, csize);
        }
    }

    private void computeDividerLocationWhenInitiallyHidden(Component hiddenComponent) {
        if (leftComponent.isVisible() || rightComponent.isVisible()) {
            boolean horiz = getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
            double size  = horiz ? getPreferredSize().getWidth() :
                                   getPreferredSize().getHeight();
            double csize = horiz ? hiddenComponent.getPreferredSize().getWidth() :
                                   hiddenComponent.getPreferredSize().getHeight();
            computeDividerLocation(hiddenComponent, size, csize);
        }
    }

    private void computeDividerLocation(Component hiddenComponent, double size, double csize) {
        if (hiddenComponent == leftComponent) {
            dividerLocation = csize / (size - customDividerSize);
        } else {
            dividerLocation = (size - customDividerSize - csize) / (size - customDividerSize);
        }
    }

    private void updateVisibility() {
        Component divider = getDivider(); // null UI, not yet set
        if (divider == null) return;

        if (leftComponent == null || rightComponent == null) return;

        boolean leftVisible = leftComponent.isVisible();
        boolean rightVisible = rightComponent.isVisible();

        if (leftVisible && rightVisible) {
            if (!divider.isVisible()) {
                Splitter.super.setDividerSize(customDividerSize);
                divider.setVisible(true);
                setDividerLocation(dividerLocation);
            }
            if (!isVisible()) setVisible(true);
        } else if (!leftVisible && !rightVisible) {
            if (isVisible()) setVisible(false);
        } else {
            if (divider.isVisible()) {
                Splitter.super.setDividerSize(0);
                divider.setVisible(false);
                setDividerLocation(0);
            }
            if (!isVisible()) setVisible(true);
        }

        if (getParent() != null) getParent().doLayout();
    }
    
    
    public void updateUI() {
        if (getUI() != customUI()) setUI(customUI());

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
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    setResizeWeight(0.5d);
                    setDividerLocation(0.5d);
                }
            }
        });
    }
    
    private CustomizedSplitPaneUI customUI() {
        if (splitterUI == null) splitterUI = new CustomizedSplitPaneUI();
        return splitterUI;
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


    private class VisibilityListener implements HierarchyListener {

        private boolean wasVisible;
        private final Component c;

        VisibilityListener(Component c) {
            this.c = c;
            wasVisible = c.isVisible();
        }

        public void hierarchyChanged(HierarchyEvent e) {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 ||
                (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                
                boolean visible = c.isVisible();
                if (wasVisible == visible) return;

                wasVisible = visible;

                if (visible) componentShown();
                else componentHidden(c);
            }
        }

        private void componentHidden(Component c) {
            computeDividerLocationWhenHidden(c);

            // Make sure the component is visible when shown
            if ((dividerLocation <= 0) || (dividerLocation >= 1))
                dividerLocation = 0.5;

            updateVisibility();
        }

        private void componentShown() {
            updateVisibility();
        }

    }

}
