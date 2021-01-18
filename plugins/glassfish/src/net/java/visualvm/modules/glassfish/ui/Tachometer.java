/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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


package net.java.visualvm.modules.glassfish.ui;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.ImageCapabilities;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 *
 * @author Jaroslav Bachorik
 */
public class Tachometer extends JComponent {
    private int val = 0;
    private int max = 100;
    private int min = 0;
    
    private final static Dimension MINIMUM_SIZE = new Dimension(80, 80);
    private final static double MAX_ANGLE = 270d;
    
    @Override
    public void paint(Graphics g) {
        try {
            Border border = getBorder();
            Insets insets = new Insets(0, 0, 0, 0);
            if (border != null) {
                insets = border.getBorderInsets(this);
            }
            
            int diameter = Math.min(getBounds().width - (insets.left + insets.right), getBounds().height - (insets.top + insets.bottom)) - 4;
            VolatileImage img = createVolatileImage(getBounds().width - (insets.left + insets.right), getBounds().height - (insets.top + insets.bottom), new ImageCapabilities(true));
            Graphics2D gr = img.createGraphics();

            gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gr.drawOval(insets.left, insets.top, diameter, diameter);
            TextLayout textTl = new TextLayout("fERRARI", getFont(), gr.getFontRenderContext());
            AffineTransform at = new AffineTransform();
            at.translate(0, (float)getBounds().getHeight() - (insets.top + insets.bottom) - (float)textTl.getBounds().getHeight());
            at.scale(2d, 2d);
//            at.shear(1.3d, 0.8d);
            Shape textShape  = textTl.getOutline(at);
            gr.fill(textShape);
//            GlyphVector gv = getFont().createGlyphVector(gr.getFontRenderContext(), "fERRARI");
//            Rectangle bounds = gv.getPixelBounds(gr.getFontRenderContext(), 0, 0);
//            double scale = (double)(getBounds().width - (insets.left + insets.right)) / (double)bounds.width;
//            for(int i=0;i<gv.getNumGlyphs();i++) {
//                gv.setGlyphTransform(i, AffineTransform.getScaleInstance(scale, 1d));
//                gv.
//            }
//            gr.drawGlyphVector(gv, insets.left + 2, getBounds().height - (insets.top + insets.bottom) - 2 - bounds.height);
            int lineLength = diameter / 2 - 10;
            gr.drawLine(diameter / 2, diameter / 2, diameter / 2 - getXOnArc(lineLength, getAngle()), diameter / 2 - getYOnArc(lineLength, getAngle()));
            gr.dispose();
            
            g.drawImage(img, insets.left, insets.top, this);
            if (border != null) {
                border.paintBorder(this, g, 0, 0, getBounds().width, getBounds().height);
            }
        } catch (AWTException e) {}
    }

    @Override
    public Dimension getMinimumSize() {
        return MINIMUM_SIZE;
    }

    @Override
    public Dimension getPreferredSize() {
        return MINIMUM_SIZE;
    }
    
    public void setMinValue(int minValue) {
        min = minValue;
        repaint();
    }
    
    public void setMaxValue(int maxValue) {
        max = maxValue;
        repaint();
    }
    
    public void setValue(int value) {
        val = value;
        repaint();
    }
    
    private static int getYOnArc(int diameter, int angle) {
        return (int)(Math.sin(getRadians(angle)) * (double)diameter);
    }
    
    private static int getXOnArc(int diameter, int angle) {
        return (int)(Math.cos(getRadians(angle)) * (double)diameter);
    }
    
    private static double getRadians(int angle) {
        return 0.0175d * angle - Math.PI / 4;
    }
    
    private int getAngle() {
        double ratio = (double)val / (double)(max - min);
        int angle = (int)(ratio * MAX_ANGLE);
        return angle;
    }
}
