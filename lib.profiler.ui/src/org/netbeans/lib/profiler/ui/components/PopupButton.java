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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.DefaultButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;


/**
 *
 * @author Jiri Sedlacek
 */
public class PopupButton extends JButton {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class PopupButtonListener extends MouseMotionAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean overPopupArea;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isOverPopupArea() {
            return overPopupArea;
        }

        public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
        }

        public void mouseMoved(MouseEvent e) {
            boolean newOverPopupArea = getPopupBounds().contains(e.getPoint());

            if (overPopupArea != newOverPopupArea) {
                overPopupArea = newOverPopupArea;
                repaint();
            }
        }
    }

    private class PopupButtonModel extends DefaultButtonModel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean popupAreaPressed;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setPopupAreaPressed(boolean b) {
            popupAreaPressed = b;
        }

        public boolean isPopupAreaPressed() {
            return popupAreaPressed;
        }

        public void setPressed(boolean b) {
            if (b == false) {
                boolean popupAction = isPopupAreaPressed() && isArmed() && listener.isOverPopupArea();
                setPopupAreaPressed(b);
                super.setPressed(b);

                if (popupAction) {
                    showPopupMenu();
                }
            } else {
                if (listener.isOverPopupArea()) {
                    setPopupAreaPressed(b);
                } else {
                    super.setPressed(b);
                }
            }
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Dimension preferredSize;
    private Dimension referenceSize;
    private JPopupMenu popupMenu;
    private JRadioButtonMenuItem selectedItem;
    private PopupButtonListener listener;
    private PopupButtonModel model;
    private ImageIcon[] icons;
    private String[] labels;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public PopupButton(String[] labels, ImageIcon[] icons) {
        super(labels[0], icons[0]);

        this.labels = labels;
        this.icons = icons;

        if (labels.length > 1) {
            model = new PopupButtonModel();
            listener = new PopupButtonListener();
            addMouseMotionListener(listener);
            setModel(model);
            addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (e.isShiftDown() && ((e.getKeyCode() == KeyEvent.VK_UP) || (e.getKeyCode() == KeyEvent.VK_DOWN))) {
                            showPopupMenu();
                        }
                    }
                });
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Dimension getMinimumSize() {
        Dimension size = computePreferredSize();

        if (size != null) {
            return size;
        } else {
            return super.getMinimumSize();
        }
    }

    public Dimension getPreferredSize() {
        Dimension size = computePreferredSize();

        if (size != null) {
            return size;
        } else {
            return super.getPreferredSize();
        }
    }

    public void paint(Graphics g) {
        super.paint(g);

        if ((labels.length > 1) && model.isRollover() && !(model.isPressed())) {
            Rectangle popupBounds = getPopupBounds();
            g.setColor(Color.WHITE);
            g.fillRect(popupBounds.x + 1, popupBounds.y + 1, popupBounds.width - 2, popupBounds.height - 2);
            g.setColor(((model.isPressed() && model.isArmed()) || model.isSelected()) ? Color.GRAY : Color.LIGHT_GRAY);
            g.drawLine(popupBounds.x, popupBounds.y + 1, popupBounds.x, (popupBounds.y + popupBounds.height) - 2);
            drawPopupArrow(g, popupBounds.x + 4, (popupBounds.y + (popupBounds.height / 2)) - 1);
        }
    }

    public void showPopupMenu() {
        JPopupMenu popup = getPopupMenu();
        popup.show(this, 0, getSize().height - 1);
    }

    private Rectangle getButtonBounds() {
        return new Rectangle(0, 0, getSize().width, getSize().height);
    }

    private Rectangle getPopupBounds() {
        Rectangle buttonBounds = getButtonBounds();

        return new Rectangle((buttonBounds.x + buttonBounds.width) - 15, buttonBounds.y, 15, buttonBounds.height);
    }

    private JPopupMenu getPopupMenu() {
        if (popupMenu == null) {
            ActionListener menuItemListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) e.getSource();

                    if (selectedItem != menuItem) {
                        selectedItem.setSelected(false);
                    }

                    menuItem.setSelected(true);
                    selectedItem = menuItem;
                    setText(menuItem.getText());

                    if (icons.length > 1) {
                        setIcon(icons[getPopupMenu().getComponentIndex(menuItem)]);
                    }

                    firePopupAction();
                }
            };

            JPopupMenu popup = new JPopupMenu();

            for (int i = 0; i < labels.length; i++) {
                JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(labels[i], i == 0);

                if (i == 0) {
                    selectedItem = menuItem;
                }

                menuItem.addActionListener(menuItemListener);
                popup.add(menuItem);
            }

            popupMenu = popup;
        }

        return popupMenu;
    }

    private Dimension computePreferredSize() {
        if ((labels.length > 1) && (referenceSize != super.getPreferredSize())) {
            JButton referenceButton = new JButton(labels[0], icons[0]);
            referenceButton.setFont(getFont());
            referenceButton.setBorder(getBorder());
            referenceButton.setMargin(getMargin());
            referenceButton.setContentAreaFilled(isContentAreaFilled());
            referenceButton.setVerticalTextPosition(getVerticalTextPosition());
            referenceButton.setHorizontalTextPosition(getHorizontalTextPosition());

            preferredSize = new Dimension(referenceButton.getPreferredSize());

            if (labels.length > 1) {
                for (int i = 1; i < labels.length; i++) {
                    referenceButton.setText(labels[i]);

                    if (icons.length > 1) {
                        referenceButton.setIcon(icons[i]);
                    }

                    preferredSize.width = Math.max(preferredSize.width, referenceButton.getPreferredSize().width);
                    preferredSize.height = Math.max(preferredSize.height, referenceButton.getPreferredSize().height);
                }
            }

            referenceSize = super.getPreferredSize();
        }

        return preferredSize;
    }

    private void drawPopupArrow(Graphics g, int x, int y) {
        g.setColor(listener.isOverPopupArea() ? Color.DARK_GRAY : Color.GRAY);

        if (model.isPopupAreaPressed()) {
            x++;
            y++;
        }

        ;
        g.drawLine(x + 0, y + 0, x + 6, y + 0);
        g.drawLine(x + 1, y + 1, x + 5, y + 1);
        g.drawLine(x + 2, y + 2, x + 4, y + 2);
        g.drawLine(x + 3, y + 3, x + 3, y + 3);
    }

    private void firePopupAction() {
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();

        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent) currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent) currentEvent).getModifiers();
        }

        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(),
                                            EventQueue.getMostRecentEventTime(), modifiers));
    }
}
