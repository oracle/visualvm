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
import java.text.Format;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class NumberPercentRenderer extends MultiRenderer {

    private final ProfilerRenderer valueRenderer;
    private final PercentRenderer percentRenderer;

    private final ProfilerRenderer[] renderers;

    private Dimension percentSize;


    public NumberPercentRenderer() {
        this((Format)null);
    }

    public NumberPercentRenderer(Format customFormat) {
        this(createNumberRenderer(customFormat));
    }

    public NumberPercentRenderer(ProfilerRenderer renderer) {
        valueRenderer = renderer;

        percentRenderer = new PercentRenderer() {
            public void setForeground(Color foreground) {
                if (foreground == null) foreground = Color.BLACK;
                super.setForeground(UIUtils.getDisabledForeground(foreground));
            }
            public Dimension getPreferredSize() {
                if (percentSize == null) percentSize = super.getPreferredSize();
                return percentSize;
            }
        };
        percentRenderer.changeFontSize(-1);
        percentRenderer.setMargin(3, 0, 3, 3);
        percentRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        percentRenderer.setMaxValue(100);
        percentRenderer.setValue(9999, -1);
        int fixedWidth = percentRenderer.getPreferredSize().width;
        percentSize.width = fixedWidth;

        renderers = new ProfilerRenderer[] { valueRenderer, percentRenderer };

        setOpaque(true);
        setHorizontalAlignment(SwingConstants.TRAILING);
    }


    protected ProfilerRenderer[] valueRenderers() {
        return renderers;
    }
    
    
    public void setMaxValue(long maxValue) {
        percentRenderer.setMaxValue(maxValue);
    }
    
    public void setValue(Object value, int row) {
        valueRenderer.setValue(value, row);
        percentRenderer.setValue(value, row);
    }
    
    
    public void setDiffMode(boolean diffMode) {
        percentRenderer.setVisible(!diffMode);
        super.setDiffMode(diffMode);
    }
    
    
    public Dimension getPreferredSize() {
        Dimension dim = valueRenderer.getComponent().getPreferredSize();
        if (percentRenderer.isVisible()) dim.width += percentRenderer.getPreferredSize().width;
        return sharedDimension(dim);
    }
    
    
    private static ProfilerRenderer createNumberRenderer(Format customFormat) {
        NumberRenderer numberRenderer = new NumberRenderer(customFormat);
        numberRenderer.setMargin(3, 3, 3, 3);
        return numberRenderer;
    }
    
    
    public String toString() {
        if (!percentRenderer.isVisible()) return valueRenderer.toString();
        else return valueRenderer.toString() + " " + percentRenderer.toString(); // NOI18N
    }
    
}
