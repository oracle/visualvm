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
