/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.ColorBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.DimensionBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.FontBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.PointBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.RectangleBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class AwtDetailsProvider extends DetailsProvider.Basic {

    private static final String FONT_MASK = "java.awt.Font+";                   // NOI18N
    private static final String COLOR_MASK = "java.awt.Color+";                 // NOI18N
    private static final String POINT_MASK = "java.awt.Point+";                 // NOI18N
    private static final String DIMENSION_MASK = "java.awt.Dimension+";         // NOI18N
    private static final String RECTANGLE_MASK = "java.awt.Rectangle+";         // NOI18N
    private static final String INSETS_MASK = "java.awt.Insets+";               // NOI18N
    private static final String TEXTATTRIBUTE_MASK = "java.text.AttributedCharacterIterator$Attribute+"; // NOI18N
    private static final String CURSOR_MASK = "java.awt.Cursor+";               // NOI18N
    
    public AwtDetailsProvider() {
        super(FONT_MASK, COLOR_MASK, POINT_MASK, DIMENSION_MASK,
              RECTANGLE_MASK, INSETS_MASK, TEXTATTRIBUTE_MASK, CURSOR_MASK);
    }
    
    public String getDetailsString(String className, Instance instance) {
        if (FONT_MASK.equals(className)) {                                      // Font+
            String name = Utils.getFontName(instance);
            if (name == null) name = "Default";                                 // NOI18N
            int size = DetailsUtils.getIntFieldValue(instance, "size", 10);     // NOI18N // TODO: should use default font size
            name += ", " + size + "pt";                                         // NOI18N
            int style = DetailsUtils.getIntFieldValue(instance, "style", 0);    // NOI18N
            if ((style & 1) != 0) name += ", bold";                             // NOI18N
            if ((style & 2) != 0) name += ", italic";                           // NOI18N
            return name;
        } else if (COLOR_MASK.equals(className)) {                              // Color+
            Color color = new ColorBuilder(instance).createInstance();
            return color.getRed() + ", " + color.getGreen() +                   // NOI18N
                   ", " + color.getBlue() + ", " + color.getAlpha();            // NOI18N
        } else if (POINT_MASK.equals(className)) {                              // Point+
            Point point = new PointBuilder(instance).createInstance();
            return point.x + ", " + point.y;                                    // NOI18N
        } else if (DIMENSION_MASK.equals(className)) {                          // Dimension+
            Dimension dimension = new DimensionBuilder(instance).createInstance();
            return dimension.width + ", " + dimension.height;                   // NOI18N
        } else if (RECTANGLE_MASK.equals(className)) {                          // Rectangle+
            Rectangle rectangle = new RectangleBuilder(instance).createInstance();
            return rectangle.x + ", " + rectangle.y +                           // NOI18N
                   ", " + rectangle.width + ", " + rectangle.height;            // NOI18N
        } else if (INSETS_MASK.equals(className)) {                             // Insets+
            Insets insets = new InsetsBuilder(instance).createInstance();
            return insets.top + ", " + insets.left +                            // NOI18N
                   ", " + insets.bottom + ", " + insets.right;                  // NOI18N
        } else if (TEXTATTRIBUTE_MASK.equals(className) ||                      // AttributedCharacterIterator$Attribute+
CURSOR_MASK.equals(className)) {                             // Cursor+
            return DetailsUtils.getInstanceFieldString(instance, "name");                                    // NOI18N
        }
        return null;
    }
    
    public View getDetailsView(String className, Instance instance) {
        if (FONT_MASK.equals(className)) {                                      // Font+
            return new FontView(instance);
        } else if (COLOR_MASK.equals(className)) {                              // Color+
            return new ColorView(instance);
        }
        return null;
    }
    
    @NbBundle.Messages({
        "FontView_Preview=ABCabc123"
    })
    private static class FontView extends Utils.View<FontBuilder> {
        
        FontView(Instance instance) {
            super(0, false, true, instance);
        }
        
        protected FontBuilder getBuilder(Instance instance) {
            return new FontBuilder(instance);
        }
        
        protected Component getComponent(FontBuilder builder) {
            JLabel label = new JLabel();
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(builder.createInstance());
            label.setText(Bundle.FontView_Preview());
            return label;
        }
        
    }
    
    private static class ColorView extends Utils.View<ColorBuilder> {
        
        ColorView(Instance instance) {
            super(0, true, true, instance);
        }
        
        protected ColorBuilder getBuilder(Instance instance) {
            return new ColorBuilder(instance);
        }
        
        protected Component getComponent(ColorBuilder builder) {
            final Color color = builder.createInstance();
            JPanel panel = new JPanel(null) {
                public void paint(Graphics g) {
                    g.setColor(color);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            panel.setOpaque(false);
            return panel;
        }
        
    }
    
}
