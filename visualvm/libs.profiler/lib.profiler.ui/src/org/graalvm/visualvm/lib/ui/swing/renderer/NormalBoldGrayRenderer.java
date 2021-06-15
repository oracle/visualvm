/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Font;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class NormalBoldGrayRenderer extends MultiRenderer {

    private final LabelRenderer normalRenderer;
    private final LabelRenderer boldRenderer;
    private final LabelRenderer grayRenderer;

    private final ProfilerRenderer[] renderers;

    private Color customForeground;
    private Color replaceableForeground = UIUtils.getDefaultTableForeground();


    public NormalBoldGrayRenderer() {
        normalRenderer = new LabelRenderer(true) {
            public void setForeground(Color foreground) {
                if (customForeground != null && Objects.equals(foreground, replaceableForeground)) foreground = customForeground;
                super.setForeground(foreground);
            }
        };
        normalRenderer.setMargin(3, 3, 3, 0);

        boldRenderer = new LabelRenderer(true) {
            public void setForeground(Color foreground) {
                if (customForeground != null && Objects.equals(foreground, replaceableForeground)) foreground = customForeground;
                super.setForeground(foreground);
            }
        };
        boldRenderer.setMargin(3, 0, 3, 0);
        Font font = boldRenderer.getFont();
        boldRenderer.setFont(font.deriveFont(Font.BOLD));

        grayRenderer = new LabelRenderer(true) {
            public void setForeground(Color foreground) {
                if (Objects.equals(foreground, replaceableForeground)) {
                    if (customForeground != null && supportsCustomGrayForeground()) super.setForeground(customForeground);
                    else super.setForeground(UIUtils.getDisabledForeground(foreground == null ? Color.BLACK : foreground));
                } else {
                    super.setForeground(foreground);
                }
            }
        };
        grayRenderer.setMargin(3, 0, 3, 3);
        
        renderers = new ProfilerRenderer[] { normalRenderer, boldRenderer, grayRenderer };
        
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.LEADING);
    }
    
    
    protected void setCustomForeground(Color foreground) {
        customForeground = foreground;
    }
    
    public void setReplaceableForeground(Color foreground) {
        replaceableForeground = foreground;
    }
    
    protected boolean supportsCustomGrayForeground() {
        return true;
    }

    
    protected ProfilerRenderer[] valueRenderers() {
        return renderers;
    }
    
    
    protected void setNormalValue(String value) {
        normalRenderer.setText(value);
    }
    
    public final String getNormalValue() {
        return normalRenderer.getText();
    }
    
    protected void setBoldValue(String value) {
        boldRenderer.setText(value);
    }
    
    public final String getBoldValue() {
        return boldRenderer.getText();
    }
    
    protected void setGrayValue(String value) {
        grayRenderer.setText(value);
    }
    
    public final String getGrayValue() {
        return grayRenderer.getText();
    }
    
    // Invoke after values are set!
    protected void setIcon(Icon icon) {
        String text = normalRenderer.getText();
        if (text == null || text.isEmpty()) {
            normalRenderer.setIcon(null);
            text = boldRenderer.getText();
            if (text == null || text.isEmpty()) {
                boldRenderer.setIcon(null);
                grayRenderer.setIcon(icon);
            } else {
                boldRenderer.setIcon(icon);
                grayRenderer.setIcon(null);
            }
        } else {
            normalRenderer.setIcon(icon);
            boldRenderer.setIcon(null);
            grayRenderer.setIcon(null);
        }
    }
    
    public Icon getIcon() {
        Icon icon = normalRenderer.getIcon();
        if (icon == null) icon = boldRenderer.getIcon();
        if (icon == null) icon = grayRenderer.getIcon();
        return icon;
    }
    
    // Invoke after values are set!
    protected void setIconTextGap(int gap) {
        String text = normalRenderer.getText();
        if (text == null || text.isEmpty()) {
//            normalRenderer.setIcon(null);
            text = boldRenderer.getText();
            if (text == null || text.isEmpty()) {
//                boldRenderer.setIcon(null);
                grayRenderer.setIconTextGap(gap);
            } else {
                boldRenderer.setIconTextGap(gap);
//                grayRenderer.setIcon(null);
            }
        } else {
            normalRenderer.setIconTextGap(gap);
//            boldRenderer.setIcon(null);
//            grayRenderer.setIcon(null);
        }
    }
        
}
