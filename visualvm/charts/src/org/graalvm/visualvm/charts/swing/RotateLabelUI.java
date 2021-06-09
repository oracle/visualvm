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

package org.graalvm.visualvm.charts.swing;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.text.View;

/**
 * LabelUI for displaying rotated labels.
 * Based on BasicLabelUI; modified paint(), layout() and get*Size().
 *
 * @author Jiri Sedlacek
 */
public class RotateLabelUI extends BasicLabelUI {

    private static final double ROTATE_0   = 0;
    private static final double ROTATE_90  = Math.PI / 2;
    private static final double ROTATE_180 = Math.PI;
    private static final double ROTATE_270 = -Math.PI / 2;


    public static final RotateLabelUI R0 = rotate0();
    public static final RotateLabelUI R90 = rotate90();
    public static final RotateLabelUI R180 = rotate180();
    public static final RotateLabelUI R270 = rotate270();


    private final double rotation;
    private final boolean vertical;


    private static RotateLabelUI rotate0()   { return new RotateLabelUI(ROTATE_0); }
    private static RotateLabelUI rotate90()  { return new RotateLabelUI(ROTATE_90); }
    private static RotateLabelUI rotate180() { return new RotateLabelUI(ROTATE_180); }
    private static RotateLabelUI rotate270() { return new RotateLabelUI(ROTATE_270); }

    
    private RotateLabelUI(double rotation) {
        this.rotation = rotation;
        this.vertical = rotation == ROTATE_90 || rotation == ROTATE_270;
    }


    private static Rectangle paintIconR = new Rectangle();
    private static Rectangle paintTextR = new Rectangle();
    private static Rectangle paintViewR = new Rectangle();
    private static Insets    paintViewInsets = new Insets(0, 0, 0, 0);


    public void paint(Graphics g, JComponent c) {
        JLabel label = (JLabel)c;
        String text = label.getText();
        Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();

        if (icon == null && text == null) return;

        Graphics2D g2 = (Graphics2D) g;
    	AffineTransform transform = null;

        if (rotation != ROTATE_0) {
            transform = g2.getTransform();
            g2.rotate(rotation);
            
            if (rotation == ROTATE_90) {
                g2.translate(0, -c.getWidth());
            } else if (rotation == ROTATE_180) {
                g2.translate(-c.getWidth(), -c.getHeight());
            } else if (rotation == ROTATE_270) {
                g2.translate(-c.getHeight(), 0);
            }
        }

        FontMetrics fm = g.getFontMetrics();
        String clippedText = layout(label, fm, c.getWidth(), c.getHeight());

        if (icon != null) icon.paintIcon(c, g, paintIconR.x, paintIconR.y);

        if (text != null) {
	    View v = (View)c.getClientProperty(BasicHTML.propertyKey);
	    if (v != null) {
		v.paint(g, paintTextR);
	    } else {
		int textX = paintTextR.x;
		int textY = paintTextR.y + fm.getAscent();

		if (label.isEnabled()) {
		    paintEnabledText(label, g, clippedText, textX, textY);
		} else {
		    paintDisabledText(label, g, clippedText, textX, textY);
		}
	    }
        }

        if (transform != null) g2.setTransform(transform);
    }


    private String layout(JLabel label, FontMetrics fm, int width, int height) {
        Insets insets = label.getInsets(paintViewInsets);
        String text = label.getText();
        Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();
        paintViewR.x = insets.left;
        paintViewR.y = insets.top;

        if (vertical) {
            paintViewR.height = width - (insets.left + insets.right);
            paintViewR.width = height - (insets.top + insets.bottom);
        } else {
            paintViewR.width = width - (insets.left + insets.right);
            paintViewR.height = height - (insets.top + insets.bottom);
        }

        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;
        return layoutCL(label, fm, text, icon, paintViewR, paintIconR, paintTextR);
    }


    public Dimension getPreferredSize(JComponent c) {
        return getDimension(super.getPreferredSize(c));
    }

    public Dimension getMinimumSize(JComponent c) {
        return getDimension(super.getMinimumSize(c));
    }

    public Dimension getMaximumSize(JComponent c) {
    	return getDimension(super.getMaximumSize(c));
    }

    
    private Dimension getDimension(Dimension dimension) {
        if (!vertical) return dimension;
        int width = dimension.width;
        dimension.width = dimension.height;
        dimension.height = width;
        return dimension;
    }

}

