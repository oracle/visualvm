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

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import javax.swing.JPanel;


/**
 *
 * @author Jiri Sedlacek
 */
class FixedImagePanel extends JPanel {
    
    protected Image image;
    protected Dimension imageSize;


    FixedImagePanel(Image img) {
        try {
            image = loadImage(img, new MediaTracker(this));
            imageSize = new Dimension(image.getWidth(null), image.getHeight(null));
            setOpaque(false);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to load image: " + e.getMessage()); // NOI18N
        }
    }

    
    public Dimension getPreferredSize() {
        return imageSize;
    }
    
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    
    protected void paintComponent(Graphics graphics) {
        graphics.drawImage(image, 0, 0, this);
    }

    
    private static Image loadImage(Image image, MediaTracker mTracker) throws InterruptedException {
        mTracker.addImage(image, 0);
        mTracker.waitForID(0);
        mTracker.removeImage(image, 0);
        return image;
    }
}
