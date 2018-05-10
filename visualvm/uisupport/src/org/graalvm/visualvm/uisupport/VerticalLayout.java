/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.uisupport;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.Box;

/**
 *
 * @author Jiri Sedlacek
 */
public final class VerticalLayout implements LayoutManager {

    private final boolean proportionalWidth;
    private final int vGap;


    public VerticalLayout(boolean proportionalWidth) {
        this(proportionalWidth, 0);
    }

    public VerticalLayout(boolean proportionalWidth, int vGap) {
        this.proportionalWidth = proportionalWidth;
        this.vGap = vGap;
    }


    public void layoutContainer(final Container parent) {
        final Insets insets = parent.getInsets();
        final int posX = insets.left;
        int posY = insets.top;
        final int width = parent.getWidth() - insets.left - insets.right;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                Dimension pref = comp.getPreferredSize();
                if (proportionalWidth) {
                    int w = Math.min(pref.width, width);
                    int o = (width - w) / 2;
                    comp.setBounds(posX + o, posY, w, pref.height);
                } else {
                    comp.setBounds(posX, posY, width, pref.height);
                }
                pref.height += vGap;
                posY += pref.height;
            }
        }
    }

    public Dimension minimumLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxWidth = 0;
        int visibleCount = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible() && !(comp instanceof Box.Filler)) {
                final Dimension size = comp.getPreferredSize();
                maxWidth = Math.max(maxWidth, size.width);
                d.height += size.height;
                visibleCount++;
            }
        }

        d.height += (visibleCount - 1) * vGap;
        d.width += maxWidth;

        return d;
    }

    public Dimension preferredLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxWidth = 0;
        int visibleCount = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                final Dimension size = comp.getPreferredSize();
                maxWidth = Math.max(maxWidth, size.width);
                d.height += size.height;
                visibleCount++;
            }
        }

        d.height += (visibleCount - 1) * vGap;
        d.width += maxWidth;

        return d;
    }


    public void addLayoutComponent(final String name, final Component comp) {}

    public void removeLayoutComponent(final Component comp) {}

}
