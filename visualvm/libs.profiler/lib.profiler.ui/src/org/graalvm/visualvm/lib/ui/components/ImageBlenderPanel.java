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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class ImageBlenderPanel extends ImagePanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Color background;
    private Image image1;
    private Image image2;
    private float blendAlpha;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ImageBlenderPanel(Image image1, Image image2) {
        this(image1, image2, UIManager.getColor("Panel.background"), 0); // NOI18N
    }

    public ImageBlenderPanel(Image image1, Image image2, Color background, float blendAlpha) {
        super(createBlendedImage(image1, image2, background, blendAlpha));
        this.background = background;
        this.blendAlpha = blendAlpha;
        this.image1 = image1;
        this.image2 = image2;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setBlendAlpha(float blendAlpha) {
        setImage(createBlendedImage(image1, image2, background, blendAlpha));
        this.blendAlpha = blendAlpha;
    }

    private static Image createBlendedImage(Image image1, Image image2, Color background, float blendAlpha) {
        Image i1 = loadImage(image1);
        Image i2 = loadImage(image2);

        int blendedImageWidth = Math.max(i1.getWidth(null), i2.getWidth(null));
        int blendedImageHeight = Math.max(i1.getHeight(null), i2.getHeight(null));

        BufferedImage blendedImage = new BufferedImage(blendedImageWidth, blendedImageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D blendedImageGraphics = (Graphics2D) blendedImage.getGraphics();

        blendedImageGraphics.setColor(background);
        blendedImageGraphics.fillRect(0, 0, blendedImageWidth, blendedImageHeight);
        blendedImageGraphics.drawImage(i1, 0, 0, null);
        blendedImageGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, blendAlpha));
        blendedImageGraphics.drawImage(i2, 0, 0, null);

        return blendedImage;
    }
}
