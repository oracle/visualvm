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

package com.sun.tools.visualvm.core.ui.components;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.ImageCapabilities;
import java.awt.Insets;
import java.awt.image.VolatileImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.BevelBorder;


/**
 *
 * @author Jaroslav Bachorik
 */
public class LevelIndicator extends JComponent {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Insets NULL_INSETS = new Insets(0, 0, 0, 0);
    private static final Dimension PREFERRED_SIZE = new Dimension(40, 20);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Color maximumColor = Color.RED;
    private Color minimumColor = Color.GREEN;
    private Color peakColor = null;
    private Dimension canvasDimension = null;
    private Insets canvasInsets = NULL_INSETS;
    private boolean autoRepaint = true;
    private boolean followPeak;
    private int peakMarkSize = 8; // peak mark size in pixels
    private long max = 0;
    private long min = 0;
    private long peak = Long.MIN_VALUE;
    private long val = min;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LevelIndicator() {
        setPreferredSize(PREFERRED_SIZE);
        setMinimumSize(PREFERRED_SIZE);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAutoRepaint(boolean autoRepaint) {
        this.autoRepaint = autoRepaint;
    }

    public boolean isAutoRepaint() {
        return autoRepaint;
    }

    public void setFollowPeak(boolean markPeaks) {
        this.followPeak = markPeaks;
    }

    public boolean isFollowPeak() {
        return followPeak;
    }

    public void setMaximum(long max) {
        this.max = max;

        if (autoRepaint) {
            repaint();
        }
    }

    public long getMaximum() {
        return max;
    }

    public void setMaximumColor(Color maximumColor) {
        this.maximumColor = maximumColor;
    }

    public Color getMaximumColor() {
        return maximumColor;
    }

    public void setMinimum(long min) {
        this.min = min;

        if (autoRepaint) {
            repaint();
        }
    }

    public long getMinimum() {
        return min;
    }

    public void setMinimumColor(Color minimumColor) {
        this.minimumColor = minimumColor;
    }

    public Color getMinimumColor() {
        return minimumColor;
    }

    public void setPeak() {
        peak = val;

        if (autoRepaint) {
            repaint();
        }
    }

    public void setPeak(long value) {
        if (peak <= max) {
            peak = value;

            if (autoRepaint) {
                repaint();
            }
        }
    }

    public long getPeak() {
        return peak;
    }

    public void setPeakMarkSize(int peakMarkSize) {
        this.peakMarkSize = peakMarkSize;

        if (autoRepaint) {
            repaint();
        }
    }

    public int getPeakMarkSize() {
        return peakMarkSize;
    }

    public void setValue(long value) {
        val = Math.max(Math.min(value, max), 0);

        if (followPeak && (val > peak)) {
            peak = this.val;
        }

        if (autoRepaint) {
            repaint();
        }
    }

    public long getValue() {
        return val;
    }

    @Override
    public void doLayout() {
        super.doLayout();

        canvasInsets = getInsets();

        canvasDimension = new Dimension(getBounds().width - (canvasInsets.left + canvasInsets.right),
                                        getBounds().height - (canvasInsets.top + canvasInsets.bottom));
    }

    @Override
    public void paintComponent(Graphics g) {
        if (canvasDimension == null) {
            return;
        }

        if ((canvasDimension.getHeight() < 0) || (canvasDimension.getWidth() < 0)) {
            return; // no rendering if dimensions are negative
        }

        try {
            VolatileImage img = createVolatileImage(getBounds().width - (canvasInsets.left + canvasInsets.right),
                                                    getBounds().height - (canvasInsets.top + canvasInsets.bottom),
                                                    new ImageCapabilities(true));
            Graphics2D gr = img.createGraphics();
            renderLevel(gr, img);
            renderPeak(gr);

            gr.dispose();
            g.drawImage(img, canvasInsets.left, canvasInsets.top, this);

            //
            //            if (getBorder() != null) {
            //                getBorder().paintBorder(this, g, 0, 0, getBounds().width, getBounds().height);
            //            }
        } catch (AWTException e) {
        }
    }

    public void unsetPeak() {
        peak = Integer.MIN_VALUE;
        repaint();
    }

    private Color getColorAt(VolatileImage img, int x, int y) {
        if ((x <= 0) || (y < 0) || (x > img.getWidth()) || (y > img.getHeight())) {
            return null;
        }

        int RGB = img.getSnapshot().getRGB(x - 1, 0);
        int red = (RGB & 0x00ff0000) >> 16;
        int green = (RGB & 0x0000ff00) >> 8;
        int blue = RGB & 0x000000ff;

        // and the Java Color is ...
        return new Color(red, green, blue);
    }

    private int getPosition(long value) {
        float ratio = (float) value / (float) (max - min);

        return Math.round((float) (ratio * canvasDimension.getWidth()));
    }

    private void renderLevel(Graphics2D gr, VolatileImage img) {
        gr.setPaint(new GradientPaint(0, 0, minimumColor, canvasDimension.width, 0, maximumColor));
        gr.fillRect(0, 0, canvasDimension.width, canvasDimension.height);

        if (peak > Integer.MIN_VALUE) {
            peakColor = getColorAt(img, getPosition(peak), 0);
        }

        int position = getPosition(val);

        gr.setPaint(getBackground());
        gr.fillRect(position, 0, canvasDimension.width - position + 1, canvasDimension.height);
    }

    private void renderPeak(Graphics2D gr) {
        if (peakColor == null) {
            return;
        }

        int position = getPosition(peak);

        int decrement = 0;
        int left = 0;
        int right = 0;

        do {
            left = Math.round(position - ((peakMarkSize - decrement) / 2f));
            right = Math.round(left + ((peakMarkSize - decrement) / 2f));

            if (left < 0) {
                right += Math.abs(left);
                left = 0;
            }

            if (right > canvasDimension.getWidth()) {
                left -= (right - canvasDimension.getWidth());
                right = (int) canvasDimension.getWidth();
            }

            decrement++;
        } while (((left < 0) || (right > canvasDimension.getWidth())) && (left != right));

        gr.setPaint(peakColor);
        gr.fillRect(left, 0, right - left + 1, canvasDimension.height);
    }
}
