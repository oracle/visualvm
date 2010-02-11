/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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
public final class HorizontalLayout implements LayoutManager {

    private final boolean proportionalHeight;
    private final int hGap;


    public HorizontalLayout(boolean proportionalHeight) {
        this(proportionalHeight, 0);
    }

    public HorizontalLayout(boolean proportionalHeight, int hGap) {
        this.proportionalHeight = proportionalHeight;
        this.hGap = hGap;
    }


    public void layoutContainer(final Container parent) {
        final Insets insets = parent.getInsets();
        int posX = insets.left;
        final int posY = insets.top;
        final int height = parent.getHeight() - insets.top - insets.bottom;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                Dimension pref = comp.getPreferredSize();
                if (proportionalHeight) {
                    int h = Math.min(pref.height, height);
                    int o = (height - h) / 2;
                    comp.setBounds(posX, posY + o, pref.width, h);
                } else {
                    comp.setBounds(posX, posY, pref.width, height);
                }
                pref.width += hGap;
                posX += pref.width;
            }
        }
    }

    public Dimension minimumLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxHeight = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                maxHeight = Math.max(maxHeight, comp.getMinimumSize().height);
                d.width += comp.getPreferredSize().width;
            }
        }

        d.height += maxHeight;

        return d;
    }

    public Dimension preferredLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxHeight = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                final Dimension size = comp.getPreferredSize();
                maxHeight = Math.max(maxHeight, size.height);
                d.width += size.width;
            }
        }

        d.height += maxHeight;

        return d;
    }


    public void addLayoutComponent(final String name, final Component comp) {}

    public void removeLayoutComponent(final Component comp) {}

}
