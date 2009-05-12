/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package org.netbeans.lib.profiler.charts.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.border.AbstractBorder;

/**
 *
 * @author Jiri Sedlacek
 */
public class RoundBorder extends AbstractBorder {

    private final Color lineColor;
    private final Color fillColor;
    private final Stroke borderStroke;

    private final int arcRadius;
    private final int borderExtent;

    private final int borderStrokeWidth;
    private final int halfBorderStrokeWidth;
    private final int inset;

    private final boolean forceSpeed;


    public RoundBorder(float lineWidth, Color lineColor, Color fillColor,
                       int arcRadius, int borderExtent) {
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);
        this.arcRadius = arcRadius;
        this.borderExtent = borderExtent;

        borderStroke = new BasicStroke(lineWidth);

        borderStrokeWidth = (int)lineWidth;
        halfBorderStrokeWidth = borderStrokeWidth / 2;
        inset = borderStrokeWidth + borderExtent;

        forceSpeed = Utils.forceSpeed();
    }


    public Insets getBorderInsets(Component c)       {
        return new Insets(inset, inset, inset, inset);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = inset;
        return insets;
    }


    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D)g;

        g2.setPaint(fillColor);
        // NOTE: fillRoundRect seems to have poor performance on Linux
//            g2.fillRoundRect(x + halfBorderStrokeWidth, y + halfBorderStrokeWidth,
//                             width - borderStrokeWidth, height - borderStrokeWidth,
//                             arcRadius * 2, arcRadius * 2);

        int arcRadius2 = arcRadius * 2;
        int arcRadius2p1 = arcRadius2 + 1;

        g2.fillArc(x, y, arcRadius2, arcRadius2, 90, 90);
        g2.fillArc(x + width - arcRadius2p1, y, arcRadius2, arcRadius2, 0, 90);
        g2.fillArc(x, y + height - arcRadius2p1, arcRadius2, arcRadius2, 180, 90);
        g2.fillArc(x + width - arcRadius2p1, y + height - arcRadius2p1, arcRadius2, arcRadius2, 270, 90);

        g2.fillRect(x + arcRadius, y, width - arcRadius2p1, height);
        g2.fillRect(x, y + arcRadius, arcRadius, height - arcRadius2p1);
        g2.fillRect(x + width - arcRadius - 1, y + arcRadius, arcRadius, height - arcRadius2p1);

        Object aa = null;
        Object sc = null;
        if (!forceSpeed) {
            aa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            sc = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }

        g2.setStroke(borderStroke);
        g2.setPaint(lineColor);
        g2.drawRoundRect(x + halfBorderStrokeWidth, y + halfBorderStrokeWidth,
                         width - borderStrokeWidth, height - borderStrokeWidth,
                         arcRadius * 2, arcRadius * 2);

        if (!forceSpeed) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, sc);
        }
    }

}
