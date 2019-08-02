/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.console.r;

import java.awt.AWTException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.RenderingHints;
import java.util.Objects;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
class RPlotPanel extends JPanel {
    
    private Image offscreenImage;
    
    private Boolean renderingQuality;
    
    
    RPlotPanel() {
        setOpaque(true);
        setBackground(UIUtils.getProfilerResultsBackground());
    }
    
    
    final void setRenderingQuality(Boolean renderingQuality) {
        boolean change = !Objects.equals(this.renderingQuality, renderingQuality);
        this.renderingQuality = renderingQuality;
        if (change) repaint();
    }
    
    final Boolean getRenderingQuality() {
        return renderingQuality;
    }
    
    
    Image createPlotImage() {
        int w = getWidth();
        int h = getHeight();
        
        if (w <= 0 || h <= 0) {
            offscreenImage = null;
        } else {
            try {
                offscreenImage = createVolatileImage(getWidth(), getHeight(), new ImageCapabilities(true));
            } catch (AWTException e1) {
                try {
                    offscreenImage = createVolatileImage(getWidth(), getHeight(), new ImageCapabilities(false));
                } catch (AWTException e2) {
                    offscreenImage = createImage(getWidth(), getHeight());
                }
            }
        }
        
        return offscreenImage;
    }
    
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        int w = getWidth();
        int h = getHeight();
        
        if (renderingQuality != null && g instanceof Graphics2D) {
            Object interpolation = renderingQuality ? RenderingHints.VALUE_INTERPOLATION_BICUBIC :
                                                      RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        }
        
        Image img = offscreenImage; // not synchronized, createPlotImage() called from worker thread
        if (img != null && w > 0 && h > 0) g.drawImage(img, 0, 0, w, h, null);
    }
    
}
