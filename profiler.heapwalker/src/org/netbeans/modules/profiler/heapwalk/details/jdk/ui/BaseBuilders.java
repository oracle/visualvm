/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.profiler.heapwalk.details.jdk.ui;

import org.netbeans.modules.profiler.heapwalk.details.jdk.image.ImageDetailProvider;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.image.ImageBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.PlaceholderIcon;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class BaseBuilders {
    
    static final class PointBuilder extends InstanceBuilder<Point> {
        
        private final int x;
        private final int y;
        
        PointBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            x = DetailsUtils.getIntFieldValue(instance, "x", 0);
            y = DetailsUtils.getIntFieldValue(instance, "y", 0);
        }
        
        static PointBuilder fromField(Instance instance, String field, Heap heap) {
            Object point = instance.getValueOfField(field);
            if (!(point instanceof Instance)) return null;
            return new PointBuilder((Instance)point, heap);
        }
        
        protected Point createInstanceImpl() {
            return new Point(x, y);
        }
        
    }
    
    static final class DimensionBuilder extends InstanceBuilder<Dimension> {
        
        private final int width;
        private final int height;
        
        DimensionBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            width = DetailsUtils.getIntFieldValue(instance, "width", 0);
            height = DetailsUtils.getIntFieldValue(instance, "height", 0);
        }
        
        static DimensionBuilder fromField(Instance instance, String field, Heap heap) {
            Object dimension = instance.getValueOfField(field);
            if (!(dimension instanceof Instance)) return null;
            return new DimensionBuilder((Instance)dimension, heap);
        }
        
        protected Dimension createInstanceImpl() {
            return new Dimension(width, height);
        }
        
    }
    
    static final class RectangleBuilder extends InstanceBuilder<Rectangle> {
        
        private final PointBuilder point;
        private final DimensionBuilder dimension;
        
        RectangleBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            point = new PointBuilder(instance, heap);
            dimension = new DimensionBuilder(instance, heap);
        }
        
        static RectangleBuilder fromField(Instance instance, String field, Heap heap) {
            Object rectangle = instance.getValueOfField(field);
            if (!(rectangle instanceof Instance)) return null;
            return new RectangleBuilder((Instance)rectangle, heap);
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
        
        InsetsBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            top = DetailsUtils.getIntFieldValue(instance, "top", 0);
            left = DetailsUtils.getIntFieldValue(instance, "left", 0);
            bottom = DetailsUtils.getIntFieldValue(instance, "bottom", 0);
            right = DetailsUtils.getIntFieldValue(instance, "right", 0);
        }
        
        static InsetsBuilder fromField(Instance instance, String field, Heap heap) {
            Object insets = instance.getValueOfField(field);
            if (!(insets instanceof Instance)) return null;
            return new InsetsBuilder((Instance)insets, heap);
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
        
        FontBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            name = Utils.getFontName(instance, heap);
            style = DetailsUtils.getIntFieldValue(instance, "style", 0);
            size = DetailsUtils.getIntFieldValue(instance, "size", 10);
            isUIResource = DetailsUtils.isSubclassOf(instance, "javax.swing.plaf.FontUIResource");
        }
        
        boolean isUIResource() {
            return isUIResource;
        }
        
        static FontBuilder fromField(Instance instance, String field, Heap heap) {
            Object font = instance.getValueOfField(field);
            if (!(font instanceof Instance)) return null;
            return new FontBuilder((Instance)font, heap);
        }
        
        protected Font createInstanceImpl() {
            return new Font(name, style, size);
        }
        
    }
    
    static final class ColorBuilder extends InstanceBuilder<Color> {
        
        private final int value;
        private final boolean isUIResource;
        
        ColorBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            value = DetailsUtils.getIntFieldValue(instance, "value", 0);
            isUIResource = DetailsUtils.isSubclassOf(instance, "javax.swing.plaf.ColorUIResource") ||
                           DetailsUtils.isSubclassOf(instance, "javax.swing.plaf.nimbus.DerivedColor$UIResource");
        }
        
        boolean isUIResource() {
            return isUIResource;
        }
        
        static ColorBuilder fromField(Instance instance, String field, Heap heap) {
            Object color = instance.getValueOfField(field);
            if (!(color instanceof Instance)) return null;            
            return new ColorBuilder((Instance)color, heap);
        }
        
        protected Color createInstanceImpl() {
            return new Color(value);
        }
        
    }
    
    static final class IconBuilder extends InstanceBuilder<Icon> {
        
        private final int width;
        private final int height;
        private final Image image;
        
        IconBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            width = DetailsUtils.getIntFieldValue(instance, "width", 0);
            height = DetailsUtils.getIntFieldValue(instance, "height", 0);
            image = ImageBuilder.buildImage(instance, heap);
        }
        
        static IconBuilder fromField(Instance instance, String field, Heap heap) {
            Object icon = instance.getValueOfField(field);
            if (!(icon instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)icon, ImageIcon.class.getName())) return null;
            return new IconBuilder((Instance)icon, heap);
        }
        
        protected Icon createInstanceImpl() {
            if(image == null) {
                    return new PlaceholderIcon(width, height);
            }
            return new ImageIcon(image);
        }
        
    }
    
}
