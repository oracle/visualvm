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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;


/**
 *
 * @author Jiri Sedlacek
 */
class HorizontalImagePanel extends FixedImagePanel {
    
    private Dimension minSize;
    private Dimension maxSize;
    
    HorizontalImagePanel(Image img) {
        super(img);
        
        minSize = new Dimension(0, imageSize.height);
        maxSize = new Dimension(Integer.MAX_VALUE, imageSize.height);
    }

    @Override
    public Dimension getPreferredSize() {
        Component[] childComponents = getComponents();
        if (childComponents.length == 0) {
            return minSize;
        } else {
            // TODO: should be computed just when child components are added/removed
            int minWidth = 0;
            for (Component component : childComponents) {
                int minChildComponentWidth = component.getMinimumSize().width;
                if (minWidth < minChildComponentWidth) minWidth = minChildComponentWidth;
            }
            return new Dimension(minWidth, imageSize.height);
        }
    }
    
    @Override
    public Dimension getMaximumSize() {
        return maxSize;
    }
    
    
    @Override
    protected void paintComponent(Graphics graphics) {
        int compEnd = getSize().width;
        int drawEnd = 0;
        while (drawEnd < compEnd) {
            graphics.drawImage(image, drawEnd, 0, this);
            drawEnd += imageSize.width;
        }
    }
}
