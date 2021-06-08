/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.tracer.impl.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.Border;

/**
 *
 * @author Jiri Sedlacek
 */
public final class EnhancedLabelRenderer extends LabelRenderer {

    private static final EnhancedInsets EMPTY_INSETS = new EnhancedInsets();

    private EnhancedInsets marginInsets;
    private EnhancedInsets borderInsets;
    private Border border;
    private Color background;


    public void setMargin(Insets marginInsets) {
        if (marginInsets == null) this.marginInsets = EMPTY_INSETS;
        else this.marginInsets = new EnhancedInsets(marginInsets);
    }

    // Overridden for performance reasons.
    public void setBorder(Border border) {
        this.border = border;
        if (border == null) borderInsets = EMPTY_INSETS;
        else borderInsets = new EnhancedInsets(border.getBorderInsets(this));
    }

    // Overridden for performance reasons.
    public Border getBorder() {
        return border;
    }

    // Overridden for performance reasons.
    public void setBackground(Color background) {
        this.background = background;
    }

    // Overridden for performance reasons.
    public Color getBackground() {
        return background;
    }

    private EnhancedInsets getMarginInsets() {
        if (marginInsets == null) marginInsets = EMPTY_INSETS;
        return marginInsets;
    }

    private EnhancedInsets getBorderInsets() {
        if (borderInsets == null) borderInsets = EMPTY_INSETS;
        return borderInsets;
    }


    protected void prePaint(Graphics g, int x, int y) {
        if (background != null) {
            g.setColor(background);
            EnhancedInsets margin = getMarginInsets();
            Dimension size = getPreferredSize();
            g.fillRect(x - margin.left,
                       y - margin.top,
                       size.width + margin.width(),
                       size.height + margin.height());
        }
    }

    protected void postPaint(Graphics g, int x, int y) {
        if (border != null) {
            EnhancedInsets bi = getBorderInsets();
            EnhancedInsets margin = getMarginInsets();
            Dimension size = getPreferredSize();
            border.paintBorder(this, g,
                               x - margin.left - bi.left,
                               y - margin.top - bi.top,
                               size.width + margin.width() + bi.width(),
                               size.height + margin.height() + bi.height());
        }
    }


    private static class EnhancedInsets extends Insets {

        public EnhancedInsets() {
            this(0, 0, 0, 0);
        }
        
        public EnhancedInsets(Insets insets) {
            this(insets.top, insets.left, insets.bottom, insets.right);
        }

        public EnhancedInsets(int top, int left, int bottom, int right) {
            super(top, left, bottom, right);
        }


        public int width() {
            return left + right;
        }

        public int height() {
            return top + bottom;
        }

    }

}
