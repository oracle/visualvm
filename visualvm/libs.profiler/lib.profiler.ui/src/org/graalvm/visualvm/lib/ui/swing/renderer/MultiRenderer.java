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

package org.graalvm.visualvm.lib.ui.swing.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class MultiRenderer extends BaseRenderer implements RelativeRenderer {

    private Dimension preferredSize;


    protected int renderersGap() { return 0; }

    protected abstract ProfilerRenderer[] valueRenderers();


    public void setDiffMode(boolean diffMode) {
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;

        for (ProfilerRenderer renderer : valueRenderers)
            if (renderer instanceof RelativeRenderer)
                ((RelativeRenderer)renderer).setDiffMode(diffMode);
    }

    public boolean isDiffMode() {
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return false;

        for (ProfilerRenderer renderer : valueRenderers)
            if (renderer instanceof RelativeRenderer)
                return ((RelativeRenderer)renderer).isDiffMode();

        return false;
    }


    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);

        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;

        for (ProfilerRenderer renderer : valueRenderers)
            renderer.getComponent().setOpaque(isOpaque);
    }

    public void setForeground(Color foreground) {
        super.setForeground(foreground);

        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;

        for (ProfilerRenderer renderer : valueRenderers)
            renderer.getComponent().setForeground(foreground);
    }
    
    public void setBackground(Color background) {
        super.setBackground(background);
        
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;
        
        for (ProfilerRenderer renderer : valueRenderers)
            renderer.getComponent().setBackground(background);
    }
    
    public Dimension getPreferredSize() {
        if (preferredSize == null) preferredSize = new Dimension();
        else preferredSize.setSize(0, 0);
        
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers != null) {
            int visible = 0;
            for (ProfilerRenderer renderer : valueRenderers) {
                JComponent component = renderer.getComponent();
                if (component.isVisible()) {
                    Dimension rendererSize = component.getPreferredSize();
                    preferredSize.width += rendererSize.width;
                    preferredSize.height = Math.max(preferredSize.height, rendererSize.height);
                    visible++;
                }
            }
            preferredSize.width += renderersGap() * (visible - 1);
        }
        
        return sharedDimension(preferredSize);
    }
    
    
    public void paint(Graphics g) {
        super.paint(g);
        
        int alignment = getHorizontalAlignment();
        int renderersGap = renderersGap();
        
        if (alignment == SwingConstants.LEADING || alignment == SwingConstants.LEFT) {
            
            int xx = location.x;
            
            for (ProfilerRenderer renderer : valueRenderers()) {
                JComponent component = renderer.getComponent();
                if (component.isVisible()) {
                    int componentWidth = component.getPreferredSize().width;
                    component.setSize(componentWidth, size.height);
                    renderer.move(xx, location.y);
                    component.paint(g);
                    xx += componentWidth + renderersGap;
                }
            }
            
        } else {
            
            int xx = location.x + size.width;
            
            ProfilerRenderer[] valueRenderers = valueRenderers();
            for (int i = valueRenderers.length - 1; i >= 0; i--) {
                ProfilerRenderer renderer = valueRenderers[i];
                JComponent component = renderer.getComponent();
                if (component.isVisible()) {
                    int componentWidth = component.getPreferredSize().width;
                    component.setSize(componentWidth, size.height);
                    xx -= componentWidth;
                    renderer.move(xx, location.y);
                    component.paint(g);
                    xx -= renderersGap;
                }
            }
            
        }
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ProfilerRenderer[] renderers = valueRenderers();
        if (renderers != null)
            for (ProfilerRenderer renderer : renderers)
                if (renderer.getComponent().isVisible())
                    sb.append(renderer.toString());
        return sb.toString();
    }
    
}
