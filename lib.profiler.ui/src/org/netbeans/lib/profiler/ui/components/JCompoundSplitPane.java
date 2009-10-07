/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.ui.components;

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
