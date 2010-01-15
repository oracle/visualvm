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

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 *
 * @author Jiri Sedlacek
 */
class JExtendedSplitPane extends JSplitPane {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SplitPaneComponentListener extends ComponentAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void componentHidden(ComponentEvent e) {
            computeDividerLocationWhenHidden(e.getComponent());

            if ((dividerLocation <= 0) || (dividerLocation >= 1)) {
                dividerLocation = 0.5;
            }

            updateVisibility();
        }

        public void componentShown(ComponentEvent e) {
            updateVisibility();
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    transient private ComponentListener splitPaneComponentListener = new SplitPaneComponentListener();
    
    private double dividerLocation;
    private int customDividerSize;
    private double requestedDividerLocation = -1;


    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    public JExtendedSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        this(newOrientation, false, newLeftComponent, newRightComponent);
    }

    public JExtendedSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent,
                              Component newRightComponent) {
        super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
        registerListeners(newLeftComponent);
        registerListeners(newRightComponent);
        updateVisibility();

        if (!newLeftComponent.isVisible()) {
            computeDividerLocationWhenInitiallyHidden(newLeftComponent);
        }

        if (!newRightComponent.isVisible()) {
            computeDividerLocationWhenInitiallyHidden(newRightComponent);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setBottomComponent(Component comp) {
        setRightComponent(comp);
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

    public void setLeftComponent(Component comp) { // Actually setTopComponent is implemented as setLeftComponent

        if (getLeftComponent() != null) {
            unregisterListeners(getLeftComponent());
        }

        super.setLeftComponent(comp);

        if (getLeftComponent() != null) {
            registerListeners(getLeftComponent());
        }

        updateVisibility();
    }

    public void setRightComponent(Component comp) { // Actually setBottomComponent is implemented as setRightComponent

        if (getRightComponent() != null) {
            unregisterListeners(getRightComponent());
        }

        super.setRightComponent(comp);

        if (getRightComponent() != null) {
            registerListeners(getRightComponent());
        }

        updateVisibility();
    }

    public void setTopComponent(Component comp) {
        setLeftComponent(comp);
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
        if (getUI() == null) {
            return null;
        }

        return ((BasicSplitPaneUI) getUI()).getDivider();
    }

    private Component getFirstComponent() {
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return getLeftComponent();
        } else {
            return getTopComponent();
        }
    }

    private Component getSecondComponent() {
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return getRightComponent();
        } else {
            return getBottomComponent();
        }
    }

    private void computeDividerLocationWhenHidden(Component hiddenComponent) {
        if (getTopComponent().isVisible() || getBottomComponent().isVisible()) {
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                if (hiddenComponent == getFirstComponent()) {
                    dividerLocation = hiddenComponent.getSize().width / (getSize().getWidth() - customDividerSize);
                } else {
                    dividerLocation = (getSize().getWidth() - customDividerSize - hiddenComponent.getSize().width) / (getSize()
                                                                                                                    .getWidth()
                                                                                                               - customDividerSize);
                }
            } else {
                if (hiddenComponent == getFirstComponent()) {
                    dividerLocation = hiddenComponent.getSize().height / (getSize().getHeight() - customDividerSize);
                } else {
                    dividerLocation = (getSize().getHeight() - customDividerSize - hiddenComponent.getSize().height) / (getSize()
                                                                                                                      .getHeight()
                                                                                                                 - customDividerSize);
                }
            }
        }
    }

    private void computeDividerLocationWhenInitiallyHidden(Component hiddenComponent) {
        if (getTopComponent().isVisible() || getBottomComponent().isVisible()) {
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                if (hiddenComponent == getFirstComponent()) {
                    dividerLocation = hiddenComponent.getPreferredSize().width / (getPreferredSize().getWidth() - customDividerSize);
                } else {
                    dividerLocation = (getPreferredSize().getWidth() - customDividerSize - hiddenComponent.getPreferredSize().width) / (getPreferredSize()
                                                                                                                                      .getWidth()
                                                                                                                                 - customDividerSize);
                }
            } else {
                if (hiddenComponent == getFirstComponent()) {
                    dividerLocation = hiddenComponent.getPreferredSize().height / (getPreferredSize().getHeight() - customDividerSize);
                } else {
                    dividerLocation = (getPreferredSize().getHeight() - customDividerSize - hiddenComponent.getPreferredSize().height) / (getPreferredSize()
                                                                                                                                        .getHeight()
                                                                                                                                   - customDividerSize);
                }
            }
        }
    }

    private void registerListeners(Component component) {
        if (splitPaneComponentListener != null) {
            component.addComponentListener(splitPaneComponentListener);
        }
    }

    private void unregisterListeners(Component component) {
        if (splitPaneComponentListener != null) {
            component.removeComponentListener(splitPaneComponentListener);
        }
    }

    private void updateVisibility() {
        Component firstComponent = getFirstComponent();
        Component secondComponent = getSecondComponent();
        Component divider = getDivider();

        if ((firstComponent == null) || (secondComponent == null) || (divider == null)) {
            return;
        }

        if (firstComponent.isVisible() && secondComponent.isVisible()) {
            if (!divider.isVisible()) {
                JExtendedSplitPane.super.setDividerSize(customDividerSize);
                divider.setVisible(true);
                setDividerLocation(dividerLocation);
            }

            if (!isVisible()) {
                setVisible(true);
            }
        } else if (!firstComponent.isVisible() && !secondComponent.isVisible()) {
            if (isVisible()) {
                setVisible(false);
            }
        } else {
            if (divider.isVisible()) {
                JExtendedSplitPane.super.setDividerSize(0);
                divider.setVisible(false);
                setDividerLocation(0);
            }

            if (!isVisible()) {
                setVisible(true);
            }
        }

        if (getParent() != null) getParent().doLayout();
    }
}
