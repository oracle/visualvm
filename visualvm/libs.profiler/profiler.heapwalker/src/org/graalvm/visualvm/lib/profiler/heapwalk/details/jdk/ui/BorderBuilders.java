/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.ColorBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.FontBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.IconBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class BorderBuilders {
    
    static BorderBuilder fromField(Instance instance, String field, boolean uiresource) {
        Object _border = instance.getValueOfField(field);
        if (!(_border instanceof Instance)) return null;

        Instance border = (Instance)_border;

        // Make sure subclasses are listed before base class if using isSubclassOf
        if (DetailsUtils.isSubclassOf(border, BevelBorder.class.getName())) {
            return new BevelBorderBuilder(border);
        } else if (DetailsUtils.isSubclassOf(border, MatteBorder.class.getName())) { // Must be before EmptyBorder (extends EmptyBorder)
            return new EmptyBorderBuilder(border);
        } else if (DetailsUtils.isSubclassOf(border, EmptyBorder.class.getName())) {
            return new MatteBorderBuilder(border);
        } else if (DetailsUtils.isSubclassOf(border, EtchedBorder.class.getName())) {
            return new EtchedBorderBuilder(border);
        } else if (DetailsUtils.isSubclassOf(border, LineBorder.class.getName())) {
            return new LineBorderBuilder(border);
        } else if (DetailsUtils.isSubclassOf(border, TitledBorder.class.getName())) {
            return new TitledBorderBuilder(border);
        } else if (DetailsUtils.isSubclassOf(border, CompoundBorder.class.getName())) {
            return new CompoundBorderBuilder(border);
        }

        return null;
    }
    
    static abstract class BorderBuilder extends InstanceBuilder<Border> {
        private final boolean isUIResource;
        BorderBuilder(Instance instance) {
            super(instance);
            this.isUIResource = instance.getJavaClass().getName().
                    startsWith("javax.swing.plaf.BorderUIResource$");
        }
        boolean isUIResource() {
            return isUIResource;
        }
    }
    
    private static class BevelBorderBuilder extends BorderBuilder {
        
        private final int bevelType;
        private final ColorBuilder highlightOuter;
        private final ColorBuilder highlightInner;
        private final ColorBuilder shadowInner;
        private final ColorBuilder shadowOuter;
        
        BevelBorderBuilder(Instance instance) {
            super(instance);
            
            bevelType = DetailsUtils.getIntFieldValue(instance, "bevelType", BevelBorder.LOWERED);
            highlightOuter = ColorBuilder.fromField(instance, "highlightOuter");
            highlightInner = ColorBuilder.fromField(instance, "highlightInner");
            shadowInner = ColorBuilder.fromField(instance, "shadowInner");
            shadowOuter = ColorBuilder.fromField(instance, "shadowOuter");
        }
        
        protected Border createInstanceImpl() {
            if (highlightOuter == null && shadowInner == null) {
                if (highlightInner == null && shadowOuter == null) {
                    return BorderFactory.createBevelBorder(bevelType);
                } else {
                    return BorderFactory.createBevelBorder(bevelType,
                            highlightInner.createInstance(), shadowOuter.createInstance());
                }
            } else {
                return BorderFactory.createBevelBorder(bevelType,
                        highlightOuter.createInstance(), highlightInner.createInstance(),
                        shadowOuter.createInstance(), shadowInner.createInstance());
            }
        }
        
    }
    
    private static class EmptyBorderBuilder extends BorderBuilder {
        
        private final InsetsBuilder insets;
        
        EmptyBorderBuilder(Instance instance) {
            super(instance);
            
            insets = new InsetsBuilder(instance);
        }
        
        protected Border createInstanceImpl() {
            Insets i = insets.createInstance();
            if (i.top == 0 && i.left == 0 && i.bottom == 0 && i.right == 0) {
                return BorderFactory.createEmptyBorder();
            } else {
                return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right);
            }
        }
        
    }
    
    private static class MatteBorderBuilder extends BorderBuilder {
        
        private final InsetsBuilder insets;
        private final ColorBuilder color;
        private final IconBuilder tileIcon;
        
        MatteBorderBuilder(Instance instance) {
            super(instance);
            
            insets = new InsetsBuilder(instance);
            color = ColorBuilder.fromField(instance, "color");
            tileIcon = IconBuilder.fromField(instance, "tileIcon");
        }
        
        protected Border createInstanceImpl() {
            Insets i = insets.createInstance();
            if (color == null) {
                return BorderFactory.createMatteBorder(i.top, i.left, i.bottom,
                        i.right, tileIcon == null ? null : tileIcon.createInstance());
            } else {
                return BorderFactory.createMatteBorder(i.top, i.left, i.bottom,
                        i.right, color.createInstance());
            }
        }
        
    }
    
    private static class EtchedBorderBuilder extends BorderBuilder {
        
        private final int etchType;
        private final ColorBuilder highlight;
        private final ColorBuilder shadow;
        
        EtchedBorderBuilder(Instance instance) {
            super(instance);
            
            etchType = DetailsUtils.getIntFieldValue(instance, "etchType", EtchedBorder.LOWERED);
            highlight = ColorBuilder.fromField(instance, "highlight");
            shadow = ColorBuilder.fromField(instance, "shadow");
        }
        
        protected Border createInstanceImpl() {
            if (highlight == null && shadow == null) {
                return BorderFactory.createEtchedBorder(etchType);
            } else {
                return BorderFactory.createEtchedBorder(etchType,
                        highlight == null ? null : highlight.createInstance(),
                        shadow == null ? null : shadow.createInstance());
            }
        }
        
    }
    
    private static class LineBorderBuilder extends BorderBuilder {
        
        private final int thickness;
        private final ColorBuilder lineColor;
        private final boolean roundedCorners;
        
        LineBorderBuilder(Instance instance) {
            super(instance);
            
            thickness = DetailsUtils.getIntFieldValue(instance, "thickness", 1);
            lineColor = ColorBuilder.fromField(instance, "lineColor");
            roundedCorners = DetailsUtils.getBooleanFieldValue(instance, "roundedCorners", false);
        }
        
        protected Border createInstanceImpl() {
            Color c = lineColor == null ? null : lineColor.createInstance();
            if (c == null) c = Color.BLACK;
            if (roundedCorners) {
                return new LineBorder(c, thickness, roundedCorners);
            } else if (thickness == 1) {
                return BorderFactory.createLineBorder(c);
            } else {
                return BorderFactory.createLineBorder(c, thickness);
            }
        }
        
    }
    
    private static class TitledBorderBuilder extends BorderBuilder {
        
        private final String title;
        private final BorderBuilder border;
        private final int titlePosition;
        private final int titleJustification;
        private final FontBuilder titleFont;
        private final ColorBuilder titleColor;
        
        TitledBorderBuilder(Instance instance) {
            super(instance);
            
            title = Utils.getFieldString(instance, "title");
            border = fromField(instance, "border", false);
            titlePosition = DetailsUtils.getIntFieldValue(instance, "titlePosition", TitledBorder.DEFAULT_POSITION);
            titleJustification = DetailsUtils.getIntFieldValue(instance, "titleJustification", TitledBorder.LEADING);
            titleFont = FontBuilder.fromField(instance, "titleFont");
            titleColor = ColorBuilder.fromField(instance, "titleColor");
        }
        
        protected Border createInstanceImpl() {
            Font font = titleFont == null || titleFont.isUIResource() ?
                        null : titleFont.createInstance();
            Color color = titleColor == null || titleColor.isUIResource() ?
                        null : titleColor.createInstance();
            
            return new TitledBorder(border == null ? null : border.createInstance(),
                    title, titleJustification, titlePosition, font, color);
        }
        
    }
    
    private static class CompoundBorderBuilder extends BorderBuilder {
        
        private final BorderBuilder outsideBorder;
        private final BorderBuilder insideBorder;
        
        CompoundBorderBuilder(Instance instance) {
            super(instance);
            
            outsideBorder = fromField(instance, "outsideBorder", true);
            insideBorder = fromField(instance, "insideBorder", true);
        }
        
        protected Border createInstanceImpl() {
            Border outside = outsideBorder == null || outsideBorder.isUIResource() ?
                             null : outsideBorder.createInstance();
            Border inside = insideBorder == null || insideBorder.isUIResource() ?
                             null : insideBorder.createInstance();
            if (outside == null && inside == null) {
                return BorderFactory.createEmptyBorder();
            } else if (outside == null || inside == null) {
                if (outside == null) return inside;
                else return outside;
            } else {
                return BorderFactory.createCompoundBorder(outside, inside);
            }
        }
        
    }
    
}
