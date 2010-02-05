/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 *
 * @author Jiri Sedlacek
 */
public class VerticalLayout implements LayoutManager {

    public void layoutContainer(final Container parent) {
        final Insets insets = parent.getInsets();
        final int posX = insets.left;
        int posY = insets.top;
        final int width = parent.getWidth() - insets.left - insets.right;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                final int height = comp.getPreferredSize().height;
                comp.setBounds(posX, posY, width, height);
                posY += height;
            }
        }
    }

    public Dimension minimumLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxWidth = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                maxWidth = Math.max(maxWidth, comp.getMinimumSize().width);
                d.height += comp.getPreferredSize().height;
            }
        }

        d.width += maxWidth;

        return d;
    }

    public Dimension preferredLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxWidth = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                final Dimension size = comp.getPreferredSize();
                maxWidth = Math.max(maxWidth, size.width);
                d.height += size.height;
            }
        }

        d.width += maxWidth;

        return d;
    }


    public void addLayoutComponent(final String name, final Component comp) {}

    public void removeLayoutComponent(final Component comp) {}

}
