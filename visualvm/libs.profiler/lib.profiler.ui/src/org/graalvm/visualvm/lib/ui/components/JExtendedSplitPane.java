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

package org.graalvm.visualvm.lib.ui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 *
 * @author Jiri Sedlacek
 */
public class JExtendedSplitPane extends JSplitPane {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SplitPaneActionListener implements ActionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(ActionEvent e) {
            switch (e.getID()) {
                case JTitledPanel.STATE_CLOSED:

                    //System.err.println(">>> STATE_CLOSED");
                    break;
                case JTitledPanel.STATE_RESTORED:
                    setDividerLocation(getLastDividerLocation());

                    break;
                case JTitledPanel.STATE_MAXIMIZED:

                    //System.err.println(">>> STATE_MAXIMIZED");
                    break;
                case JTitledPanel.STATE_MINIMIZED:

                    if (e.getSource() == getFirstComponent()) {
                        setDividerLocation(getFirstComponent().getPreferredSize().height);
                    } else {
                        setDividerLocation(getSize().height - dividerSize - getSecondComponent().getPreferredSize().height);
                    }

                    break;
            }
        }
    }

    private class SplitPaneComponentListener extends ComponentAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void componentHidden(ComponentEvent e) {
            computeDividerLocationWhenHidden(e.getComponent());

            if ((dividerLocation == 0) || (dividerLocation == 1)) {
                dividerLocation = 0.5;
            }

            updateVisibility();
        }

        public void componentShown(ComponentEvent e) {
            updateVisibility();
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ActionListener splitPaneActionListener = new SplitPaneActionListener();
    private ComponentListener splitPaneComponentListener = new SplitPaneComponentListener();
    private double dividerLocation;
    private int dividerSize;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JExtendedSplitPane() {
        super();
    }

    public JExtendedSplitPane(int newOrientation) {
        super(newOrientation);
    }

    public JExtendedSplitPane(int newOrientation, boolean newContinuousLayout) {
        super(newOrientation, newContinuousLayout);
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

    public JExtendedSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        super(newOrientation, newLeftComponent, newRightComponent);
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
        dividerSize = newSize;
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
                    dividerLocation = hiddenComponent.getSize().width / (getSize().getWidth() - dividerSize);
                } else {
                    dividerLocation = (getSize().getWidth() - dividerSize - hiddenComponent.getSize().width) / (getSize()
                                                                                                                    .getWidth()
                                                                                                               - dividerSize);
                }
            } else {
                if (hiddenComponent == getFirstComponent()) {
                    dividerLocation = hiddenComponent.getSize().height / (getSize().getHeight() - dividerSize);
                } else {
                    dividerLocation = (getSize().getHeight() - dividerSize - hiddenComponent.getSize().height) / (getSize()
                                                                                                                      .getHeight()
                                                                                                                 - dividerSize);
                }
            }
            dividerLocation = Math.max(0, dividerLocation);
            dividerLocation = Math.min(1, dividerLocation);
        }
    }

    private void computeDividerLocationWhenInitiallyHidden(Component hiddenComponent) {
        if (getTopComponent().isVisible() || getBottomComponent().isVisible()) {
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                if (hiddenComponent == getFirstComponent()) {
                    dividerLocation = hiddenComponent.getPreferredSize().width / (getPreferredSize().getWidth() - dividerSize);
                } else {
                    dividerLocation = (getPreferredSize().getWidth() - dividerSize - hiddenComponent.getPreferredSize().width) / (getPreferredSize()
                                                                                                                                      .getWidth()
                                                                                                                                 - dividerSize);
                }
            } else {
                if (hiddenComponent == getFirstComponent()) {
                    dividerLocation = hiddenComponent.getPreferredSize().height / (getPreferredSize().getHeight() - dividerSize);
                } else {
                    dividerLocation = (getPreferredSize().getHeight() - dividerSize - hiddenComponent.getPreferredSize().height) / (getPreferredSize()
                                                                                                                                        .getHeight()
                                                                                                                                   - dividerSize);
                }
            }
            dividerLocation = Math.max(0, dividerLocation);
            dividerLocation = Math.min(1, dividerLocation);
        }
    }

    private void registerListeners(Component component) {
        if (splitPaneComponentListener != null) {
            component.addComponentListener(splitPaneComponentListener);
        }

        if (splitPaneActionListener != null) {
            if (component instanceof JTitledPanel) {
                ((JTitledPanel) component).addActionListener(splitPaneActionListener);
            }

            //else if (component instanceof JExtendedSplitPane) ((JTitledPanel)component).addActionListener(splitPaneActionListener);
        }
    }

    private void unregisterListeners(Component component) {
        if (splitPaneComponentListener != null) {
            component.removeComponentListener(splitPaneComponentListener);
        }

        if (splitPaneActionListener != null) {
            if (component instanceof JTitledPanel) {
                ((JTitledPanel) component).removeActionListener(splitPaneActionListener);
            }

            //else if (component instanceof JExtendedSplitPane) ((JTitledPanel)component).removeActionListener(splitPaneActionListener);
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
                super.setDividerSize(dividerSize);
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
                super.setDividerSize(0);
                divider.setVisible(false);
                setDividerLocation(0);
            }

            if (!isVisible()) {
                setVisible(true);
            }
        }
    }
}
