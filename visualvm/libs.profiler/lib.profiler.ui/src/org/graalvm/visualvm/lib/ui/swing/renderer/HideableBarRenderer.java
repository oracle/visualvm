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

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public class HideableBarRenderer extends MultiRenderer {

    private static final int BAR_MARGIN = 20;
    private static final int MIN_BAR_WIDTH = 20;
    private static final int MAX_BAR_WIDTH = 100;
    private static final int OPT_BAR_WIDTH = 50;


    public static enum BarDiffMode {
        MODE_BAR_DIFF,      // divided bar with green/red sections
        MODE_BAR_NORMAL,    // single bar as if diff was not in effect
        MODE_BAR_NONE       // no bar (hidden) in diff mode
    }


    private BarDiffMode barDiffMode = BarDiffMode.MODE_BAR_DIFF;

    private int maxRendererWidth;

    private final BarRenderer barRenderer;
    private final ProfilerRenderer mainRenderer;
    private final NumberPercentRenderer numberPercentRenderer;

    private final ProfilerRenderer[] valueRenderers;


    public HideableBarRenderer(ProfilerRenderer renderer) {
        this(renderer, renderer.getComponent().getPreferredSize().width);
    }

    public HideableBarRenderer(ProfilerRenderer renderer, int maxWidth) {
        maxRendererWidth = maxWidth;

        barRenderer = new BarRenderer();
        mainRenderer = renderer;
        numberPercentRenderer = renderer instanceof NumberPercentRenderer ?
                                (NumberPercentRenderer)renderer : null;

        valueRenderers = new ProfilerRenderer[] { barRenderer, mainRenderer };

        setOpaque(true);
        setHorizontalAlignment(SwingConstants.TRAILING);
    }


    public void setBarDiffMode(BarDiffMode barDiffMode) {
        this.barDiffMode = barDiffMode;
    }
    
    public BarDiffMode getBarDiffMode() {
        return barDiffMode;
    }
    
    
    public void setDiffMode(boolean diffMode) {
        super.setDiffMode(diffMode);
        
        if (!diffMode || BarDiffMode.MODE_BAR_NORMAL.equals(barDiffMode)) {
            barRenderer.setVisible(true);
            barRenderer.setDiffMode(false);
        } else {
            if (BarDiffMode.MODE_BAR_NONE.equals(barDiffMode)) barRenderer.setVisible(false);
            else if (BarDiffMode.MODE_BAR_DIFF.equals(barDiffMode)) barRenderer.setDiffMode(true);
        }
    }
    
    
    public void setMaxValue(long maxValue) {
        int oldDigits = Long.toString(barRenderer.getMaxValue()).length();
        int newDigits = Long.toString(maxValue).length();
        
        barRenderer.setMaxValue(maxValue);
        if (numberPercentRenderer != null) numberPercentRenderer.setMaxValue(maxValue);
        
        if (oldDigits < newDigits) {
            // Number of the same pow10 created using only digit '9'
//            int ref = (int)Math.pow(10, Math.ceil(Math.log10(maxValue + 1))) - 1;
            mainRenderer.setValue((long)Math.pow(10, newDigits) - 1, -1);
            int mainWidth = mainRenderer.getComponent().getPreferredSize().width;
            maxRendererWidth = Math.max(maxRendererWidth, mainWidth);
        }
    }
    
    public void setValue(Object value, int row) {
        barRenderer.setValue(value, row);
        mainRenderer.setValue(value, row);
    }
    

    protected ProfilerRenderer[] valueRenderers() {
        return valueRenderers;
    }
    
    protected int renderersGap() {
        return BAR_MARGIN;
    }
    
    
    public Dimension getPreferredSize() {
        return mainRenderer.getComponent().getPreferredSize();
    }
    
    public int getOptimalWidth() {
        return maxRendererWidth + renderersGap() + OPT_BAR_WIDTH;
    }
    
    public int getMaxNoBarWidth() {
        return maxRendererWidth + renderersGap() + MIN_BAR_WIDTH - 1;
    }
    
    public int getNoBarWidth() {
        return maxRendererWidth;
    }
    
    public void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(location.x, location.y, size.width, size.height);
        
        JComponent component = mainRenderer.getComponent();
        int componentWidth = component.getPreferredSize().width;
        int componentX = size.width - componentWidth;
        
        mainRenderer.move(location.x + componentX, location.y);
        component.setSize(componentWidth, size.height);
        component.paint(g);
        
        if (barRenderer.isVisible()) {
            int freeWidth = size.width - maxRendererWidth - renderersGap();
            if (freeWidth >= MIN_BAR_WIDTH) {
                barRenderer.setSize(Math.min(freeWidth, MAX_BAR_WIDTH), size.height);
                barRenderer.move(location.x, location.y);
                barRenderer.paint(g);
            }
        }
    }
    
    public String toString() {
        return mainRenderer.toString();
    }
    
}
