/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.tracer.impl.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SimpleSeparator extends JPanel implements SwingConstants {

    private final int orientation;
    private final Dimension preferredSize = new Dimension(1, 1);


    public SimpleSeparator() {
        this(HORIZONTAL);
    }

    public SimpleSeparator(int orientation) {
        super(null);
        this.orientation = orientation;
    }


    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize.width = preferredSize.width;
        this.preferredSize.height = preferredSize.height;
    }


    public Dimension getMinimumSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            return new Dimension(insets.left + insets.right,
                                 insets.top + insets.bottom + 1);
        else
            return new Dimension(insets.left + insets.right + 1,
                                 insets.top + insets.bottom);
    }

    public Dimension getMaximumSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            return new Dimension(Integer.MAX_VALUE,
                                 insets.top + insets.bottom + 1);
        else
            return new Dimension(insets.left + insets.right + 1,
                                 Integer.MAX_VALUE);
    }

    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            return new Dimension(Math.max(insets.left + insets.right, preferredSize.width),
                                 insets.top + insets.bottom + 1);
        else
            return new Dimension(insets.left + insets.right + 1,
                                 Math.max(insets.top + insets.bottom, preferredSize.height));
    }


    public void paint(Graphics g) {
        g.setColor(new Color(192, 192, 192));
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            g.drawLine(insets.left, insets.top, getWidth() - insets.right, insets.top);
        else
            g.drawLine(insets.left, insets.top, insets.left, getHeight() - insets.bottom);
    }

}
