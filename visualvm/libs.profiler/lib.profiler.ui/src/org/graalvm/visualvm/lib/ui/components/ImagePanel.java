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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.PixelGrabber;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


/**
 *
 * @author Jiri Sedlacek
 */
public class ImagePanel extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static MediaTracker mTracker = new MediaTracker(new JPanel());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Image image;
    private int imageAlign; // Use SwingConstants.TOP, BOTTOM (LEFT & RIGHT not implemented)

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ImagePanel(Image image) {
        this(image, SwingConstants.TOP);
    }

    public ImagePanel(Image image, int imageAlign) {
        setImage(image);
        setImageAlign(imageAlign);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setImage(Image image) {
        this.image = loadImage(image);

        if (this.image == null) {
            throw new RuntimeException("Failed to load image"); // NOI18N
        }

        setPreferredBackground();
        setPreferredSize(new Dimension(this.image.getWidth(null), this.image.getHeight(null)));

        refresh();
    }

    public void setImageAlign(int imageAlign) {
        this.imageAlign = imageAlign;

        setPreferredBackground();

        refresh();
    }

    protected static Image loadImage(Image image) {
        mTracker.addImage(image, 0);

        try {
            mTracker.waitForID(0);
        } catch (InterruptedException e) {
            return null;
        }

        mTracker.removeImage(image, 0);

        return image;
    }

    protected void setPreferredBackground() {
        int[] pixels = new int[1];

        PixelGrabber pg = null;

        switch (imageAlign) {
            case (SwingConstants.TOP):
                pg = new PixelGrabber(image, 0, image.getHeight(null) - 1, 1, 1, pixels, 0, 1);

                break;
            case (SwingConstants.BOTTOM):
                pg = new PixelGrabber(image, 0, 0, 1, 1, pixels, 0, 1);

                break;
            default:
                pg = new PixelGrabber(image, 0, image.getHeight(null) - 1, 1, 1, pixels, 0, 1);
        }

        try {
            if ((pg != null) && pg.grabPixels()) {
                setBackground(new Color(pixels[0]));
            }
        } catch (InterruptedException e) {
        }
    }

    protected void paintComponent(Graphics graphics) {
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, getWidth(), getHeight());

        switch (imageAlign) {
            case (SwingConstants.TOP):
                graphics.drawImage(image, (getWidth() - image.getWidth(null)) / 2, 0, this);

                break;
            case (SwingConstants.BOTTOM):
                graphics.drawImage(image, (getWidth() - image.getWidth(null)) / 2, getHeight() - image.getHeight(null), this);

                break;
            default:
                graphics.drawImage(image, (getWidth() - image.getWidth(null)) / 2, 0, this);
        }
    }

    private void refresh() {
        if (isShowing()) {
            invalidate();
            repaint();
        }
    }
}
