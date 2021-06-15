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

import java.awt.*;


/**
 *
 * @author Jiri Sedlacek
 */
public class VerticalLayout implements LayoutManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int hgap;
    private int vgap;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public VerticalLayout() {
        this(10, 10);
    }

    public VerticalLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getHGap() {
        return hgap;
    }

    public int getVGap() {
        return vgap;
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void layoutContainer(Container container) {
        synchronized (container.getTreeLock()) {
            Insets insets = container.getInsets();

            int x = insets.left + hgap;
            int y = insets.top + vgap;
            int w = container.getSize().width - insets.left - insets.right - (2 * hgap);
            int h;

            for (int i = 0; i < container.getComponentCount(); i++) {
                Component component = container.getComponent(i);

                if (component.isVisible()) {
                    h = component.getPreferredSize().height;
                    component.setBounds(x, y, w, h);
                    y += (h + vgap);
                }
            }
        }
    }

    public Dimension minimumLayoutSize(Container container) {
        return preferredLayoutSize(container);
    }

    public Dimension preferredLayoutSize(Container container) {
        synchronized (container.getTreeLock()) {
            Insets insets = container.getInsets();

            int width = insets.left + insets.right + (2 * hgap);
            int height = insets.top + insets.bottom + vgap;

            for (int i = 0; i < container.getComponentCount(); i++) {
                Component component = container.getComponent(i);

                if (component.isVisible()) {
                    Dimension preferredDim = component.getPreferredSize();
                    width = Math.max(width, preferredDim.width + insets.left + insets.right + (2 * hgap));
                    height += (preferredDim.height + vgap);
                }
            }

            return new Dimension(width, height);
        }
    }

    public void removeLayoutComponent(Component comp) {
    }

    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ", vgap=" + vgap + "]"; // NOI18N
    }
}
