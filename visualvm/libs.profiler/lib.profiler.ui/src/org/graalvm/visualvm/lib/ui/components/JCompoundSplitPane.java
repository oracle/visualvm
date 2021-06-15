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
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 *
 * @author Jiri Sedlacek
 */
public class JCompoundSplitPane extends JExtendedSplitPane {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class DividerMouseListener extends MouseAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private double firstResizeWeight = 0;
        private double secondResizeWeight = 1;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void mouseEntered(MouseEvent e) {
            configureComponents();
        }

        public void mousePressed(MouseEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JSplitPane firstSplit = (JSplitPane) getFirstComponent();
                        JSplitPane secondSplit = (JSplitPane) getSecondComponent();
                        firstResizeWeight = firstSplit.getResizeWeight();
                        secondResizeWeight = secondSplit.getResizeWeight();
                        firstSplit.setResizeWeight(0);
                        secondSplit.setResizeWeight(1);
                    }
                });
        }

        public void mouseReleased(MouseEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ((JSplitPane) getFirstComponent()).setResizeWeight(firstResizeWeight);
                        ((JSplitPane) getSecondComponent()).setResizeWeight(secondResizeWeight);
                    }
                });
        }

        private void configureComponents() {
            configureFirstComponent();
            configureSecondComponent();
        }

        private void configureFirstComponent() {
            JSplitPane firstSplit = (JSplitPane) getFirstComponent();
            int newWidth;
            int newHeight;

            newWidth = firstSplit.getMinimumSize().width;
            newHeight = 0;

            if (getFirstComponent(firstSplit).isVisible() && getSecondComponent(firstSplit).isVisible()) {
                newHeight = getFirstComponent(firstSplit).getSize().height
                            + getSecondComponent(firstSplit).getMinimumSize().height + firstSplit.getDividerSize();
            } else if (getFirstComponent(firstSplit).isVisible()) {
                newHeight = getFirstComponent(firstSplit).getMinimumSize().height;
            } else {
                newHeight = getSecondComponent(firstSplit).getMinimumSize().height;
            }

            firstSplit.setMinimumSize(new Dimension(newWidth, newHeight));
        }

        private void configureSecondComponent() {
            JSplitPane secondSplit = (JSplitPane) getSecondComponent();
            int newWidth = secondSplit.getMinimumSize().width;
            int newHeight = 0;

            if (getFirstComponent(secondSplit).isVisible() && getSecondComponent(secondSplit).isVisible()) {
                newHeight = getSecondComponent(secondSplit).getSize().height
                            + (getFirstComponent(secondSplit).isVisible()
                               ? (getFirstComponent(secondSplit).getMinimumSize().height + secondSplit.getDividerSize()) : 0);
            } else if (getFirstComponent(secondSplit).isVisible()) {
                newHeight = getFirstComponent(secondSplit).getMinimumSize().height;
            } else {
                newHeight = getSecondComponent(secondSplit).getMinimumSize().height;
            }

            secondSplit.setMinimumSize(new Dimension(newWidth, newHeight));
        }
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JCompoundSplitPane() {
        super();
        tweakUI();
    }

    public JCompoundSplitPane(int newOrientation) {
        super(newOrientation);
        tweakUI();
    }

    public JCompoundSplitPane(int newOrientation, boolean newContinuousLayout) {
        super(newOrientation, newContinuousLayout);
        tweakUI();
    }

    public JCompoundSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent,
                              Component newRightComponent) {
        super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
        tweakUI();
    }

    public JCompoundSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        super(newOrientation, newLeftComponent, newRightComponent);
        tweakUI();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    private Component getFirstComponent() {
        return getFirstComponent(this);
    }

    private Component getFirstComponent(JSplitPane splitPane) {
        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return splitPane.getLeftComponent();
        } else {
            return splitPane.getTopComponent();
        }
    }

    private Component getSecondComponent() {
        return getSecondComponent(this);
    }

    private Component getSecondComponent(JSplitPane splitPane) {
        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return splitPane.getRightComponent();
        } else {
            return splitPane.getBottomComponent();
        }
    }

    private void tweakUI() {
        if (!(getUI() instanceof BasicSplitPaneUI)) {
            return;
        }

        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) getUI()).getDivider();

        if (divider != null) {
            divider.addMouseListener(new DividerMouseListener());
        }
    }
}
