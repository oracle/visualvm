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

package org.graalvm.visualvm.modules.customtype.icons;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ImageUtils {
    final public static BufferedImage resizeImage(BufferedImage img, int width, int height) {
        Graphics2D gin = img.createGraphics();
        GraphicsConfiguration gc = gin.getDeviceConfiguration();
        gin.dispose();

        BufferedImage dst = gc.createCompatibleImage(width, height, BufferedImage.BITMASK);
        Graphics2D gr = dst.createGraphics();
        gr.setComposite(AlphaComposite.Src);

        AffineTransform at = AffineTransform.getScaleInstance((double)width/img.getWidth(), (double)height/img.getHeight());
        gr.drawRenderedImage(img,at);
        return dst;
    }
}
