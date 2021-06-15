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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.image.ImageBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.PlaceholderIcon;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class BaseBuilders {

    static final class PointBuilder extends InstanceBuilder<Point> {

        private final int x;
        private final int y;

        PointBuilder(Instance instance) {
            super(instance);
            x = DetailsUtils.getIntFieldValue(instance, "x", 0);
            y = DetailsUtils.getIntFieldValue(instance, "y", 0);
        }

        static PointBuilder fromField(Instance instance, String field) {
            Object point = instance.getValueOfField(field);
            if (!(point instanceof Instance)) return null;
            return new PointBuilder((Instance)point);
        }

        protected Point createInstanceImpl() {
            return new Point(x, y);
        }

    }

    static final class DimensionBuilder extends InstanceBuilder<Dimension> {

        private final int width;
        private final int height;

        DimensionBuilder(Instance instance) {
            super(instance);
            width = DetailsUtils.getIntFieldValue(instance, "width", 0);
            height = DetailsUtils.getIntFieldValue(instance, "height", 0);
        }
        
        static DimensionBuilder fromField(Instance instance, String field) {
            Object dimension = instance.getValueOfField(field);
            if (!(dimension instanceof Instance)) return null;
            return new DimensionBuilder((Instance)dimension);
        }
        
        protected Dimension createInstanceImpl() {
            return new Dimension(width, height);
        }
        
    }
    
    static final class RectangleBuilder extends InstanceBuilder<Rectangle> {
        
        private final PointBuilder point;
        private final DimensionBuilder dimension;
        
        RectangleBuilder(Instance instance) {
            super(instance);
            point = new PointBuilder(instance);
            dimension = new DimensionBuilder(instance);
        }
        
        static RectangleBuilder fromField(Instance instance, String field) {
            Object rectangle = instance.getValueOfField(field);
            if (!(rectangle instanceof Instance)) return null;
            return new RectangleBuilder((Instance)rectangle);
        }
        
        protected Rectangle createInstanceImpl() {
            return new Rectangle(point.createInstance(), dimension.createInstance());
        }
        
    }
    
    static final class InsetsBuilder extends InstanceBuilder<Insets> {
        
        private final int top;
        private final int left;
        private final int bottom;
        private final int right;
        
        InsetsBuilder(Instance instance) {
            super(instance);
            top = DetailsUtils.getIntFieldValue(instance, "top", 0);
            left = DetailsUtils.getIntFieldValue(instance, "left", 0);
            bottom = DetailsUtils.getIntFieldValue(instance, "bottom", 0);
            right = DetailsUtils.getIntFieldValue(instance, "right", 0);
        }
        
        static InsetsBuilder fromField(Instance instance, String field) {
            Object insets = instance.getValueOfField(field);
            if (!(insets instanceof Instance)) return null;
            return new InsetsBuilder((Instance)insets);
        }
        
        protected Insets createInstanceImpl() {
            return new Insets(top, left, bottom, right);
        }
        
    }
    
    static final class FontBuilder extends InstanceBuilder<Font> {
        
        private final String name;
        private final int style;
        private final int size;
        private final boolean isUIResource;
        
        FontBuilder(Instance instance) {
            super(instance);
            name = Utils.getFontName(instance);
            style = DetailsUtils.getIntFieldValue(instance, "style", 0);
            size = DetailsUtils.getIntFieldValue(instance, "size", 10);
            isUIResource = DetailsUtils.isSubclassOf(instance, "javax.swing.plaf.FontUIResource");
        }
        
        boolean isUIResource() {
            return isUIResource;
        }
        
        static FontBuilder fromField(Instance instance, String field) {
            Object font = instance.getValueOfField(field);
            if (!(font instanceof Instance)) return null;
            return new FontBuilder((Instance)font);
        }
        
        protected Font createInstanceImpl() {
            return new Font(name, style, size);
        }
        
    }
    
    static final class ColorBuilder extends InstanceBuilder<Color> {
        
        private final int value;
        private final boolean isUIResource;
        
        ColorBuilder(Instance instance) {
            super(instance);
            value = DetailsUtils.getIntFieldValue(instance, "value", 0);
            isUIResource = DetailsUtils.isSubclassOf(instance, "javax.swing.plaf.ColorUIResource") ||
                           DetailsUtils.isSubclassOf(instance, "javax.swing.plaf.nimbus.DerivedColor$UIResource");
        }
        
        boolean isUIResource() {
            return isUIResource;
        }
        
        static ColorBuilder fromField(Instance instance, String field) {
            Object color = instance.getValueOfField(field);
            if (!(color instanceof Instance)) return null;            
            return new ColorBuilder((Instance)color);
        }
        
        protected Color createInstanceImpl() {
            return new Color(value);
        }
        
    }
    
    static final class IconBuilder extends InstanceBuilder<Icon> {
        
        private final int width;
        private final int height;
        private final Image image;
        
        IconBuilder(Instance instance) {
            super(instance);
            width = DetailsUtils.getIntFieldValue(instance, "width", 0);
            height = DetailsUtils.getIntFieldValue(instance, "height", 0);
            image = ImageBuilder.buildImage(instance);
        }
        
        static IconBuilder fromField(Instance instance, String field) {
            Object icon = instance.getValueOfField(field);
            if (!(icon instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)icon, ImageIcon.class.getName())) return null;
            return new IconBuilder((Instance)icon);
        }
        
        protected Icon createInstanceImpl() {
            if(image == null) {
                    return new PlaceholderIcon(width, height);
            }
            return new ImageIcon(image);
        }
        
    }
    
}
