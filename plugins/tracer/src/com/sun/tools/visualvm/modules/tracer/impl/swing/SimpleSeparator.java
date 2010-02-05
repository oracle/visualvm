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


    public SimpleSeparator() {
        this(HORIZONTAL);
    }

    public SimpleSeparator(int orientation) {
        super(null);
        this.orientation = orientation;
    }


    public Dimension getMinimumSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL) return new Dimension(insets.left + insets.right, insets.top + insets.bottom + 1);
        else return new Dimension(insets.left + insets.right + 1, 0);
    }

    public Dimension getMaximumSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL) return new Dimension(Integer.MAX_VALUE, insets.top + insets.bottom + 1);
        else return new Dimension(1, Integer.MAX_VALUE);
    }

    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL) return new Dimension(insets.left + insets.right + 1, insets.top + insets.bottom + 1);
        else return new Dimension(insets.left + insets.right + 1, insets.top + insets.bottom + 1);
    }


    public void paint(Graphics g) {
        g.setColor(new Color(192, 192, 192));
        Insets insets = getInsets();
        if (orientation == HORIZONTAL) g.drawLine(insets.left, insets.top, getWidth() - insets.right, insets.top);
        else g.drawLine(insets.left, insets.top, insets.left, getHeight() - insets.bottom);
    }

}
