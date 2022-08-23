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

package org.graalvm.visualvm.lib.ui.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class PopupButton extends SmallButton {

    private static final Icon DROPDOWN_ICON = Icons.getIcon(GeneralIcons.POPUP_ARROW);
    private static final int DROPDOWN_ICON_WIDTH = DROPDOWN_ICON.getIconWidth();
    private static final int DROPDOWN_ICON_HEIGHT = DROPDOWN_ICON.getIconHeight();

    private int iconOffset;
    private int popupAlign = SwingConstants.LEADING;


    {
        if (UIUtils.isMetalLookAndFeel()) iconOffset = 6;
        else if (UIUtils.isNimbusLookAndFeel()) iconOffset = 8;
        else iconOffset = 7;

        setHorizontalAlignment(LEADING);
    }


    public PopupButton() { super(); }

    public PopupButton(Icon icon) { super(icon); }

    public PopupButton(String text) { super(text); }

    public PopupButton(Action a) { super(a); }

    public PopupButton(String text, Icon icon) { super(text, icon); }


    public void setPopupAlign(int align) {
        popupAlign = align;
    }

    public int getPopupAlign() {
        return popupAlign;
    }


    protected void fireActionPerformed(ActionEvent e) {
        super.fireActionPerformed(e);
        displayPopup();
    }

    protected void displayPopup() {
        JPopupMenu menu = new JPopupMenu();
        populatePopup(menu);
        if (menu.getComponentCount() > 0) {
            Dimension size = menu.getPreferredSize();
            size.width = Math.max(size.width, getWidth());
            menu.setPreferredSize(size);
            
            int align = getPopupAlign();
            
            int x;
            switch (align) {
                case SwingConstants.EAST:
                case SwingConstants.NORTH_EAST:
                case SwingConstants.SOUTH_EAST:
                    x = getWidth() - size.width;
                    break;
                default:
                    x = 0;
                    break;
            }
            
            int y;
            switch (align) {
                case SwingConstants.NORTH:
                case SwingConstants.NORTH_EAST:
                case SwingConstants.NORTH_WEST:
                    y = -size.height;
                    break;
                default:
                    y = getHeight();
                    break;
            }
            
            menu.show(this, x, y);
        }
    }
    
    protected void populatePopup(JPopupMenu popup) {
        // Implementation here
    }
    
    
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width += DROPDOWN_ICON_WIDTH + (isEmpty() ? 3 : 5);
        return size;
    }
    
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    public void addNotify() {
        super.addNotify();
        if (UIUtils.isWindowsLookAndFeel() && getParent() instanceof JToolBar) {
            if (getIcon() == NO_ICON) setIconTextGap(2);
            iconOffset = 5;
        }
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isEmpty()) {
            DROPDOWN_ICON.paintIcon(this, g, (getWidth() - DROPDOWN_ICON_WIDTH) / 2,
                                             (getHeight() - DROPDOWN_ICON_HEIGHT) / 2);
        } else {
            DROPDOWN_ICON.paintIcon(this, g, getWidth() - DROPDOWN_ICON_WIDTH - iconOffset,
                                            (getHeight() - DROPDOWN_ICON_HEIGHT) / 2);
        }
    }
    
    
    private boolean isEmpty() {
        if (getIcon() != NO_ICON) return false;
        String text = getText();
        return text == null || text.isEmpty();
    }
    
}
