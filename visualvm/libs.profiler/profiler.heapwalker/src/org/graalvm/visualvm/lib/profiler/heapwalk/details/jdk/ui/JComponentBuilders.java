/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.LayoutManager;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.DimensionBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.IconBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class JComponentBuilders {

    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder<? extends JComponent> getBuilder(Instance instance) {
        if (DetailsUtils.isSubclassOf(instance, JLabel.class.getName())) {
            return new JLabelBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JPanel.class.getName())) {
            return new JPanelBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JToolBar.class.getName())) {
            return new JToolBarBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, Box.Filler.class.getName())) {
            return new BoxFillerBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, Box.class.getName())) {
            return new BoxBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JScrollBar.class.getName())) {
            return new JScrollBarBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JToolBar.Separator.class.getName())) {
            return new JToolBarSeparatorBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JPopupMenu.Separator.class.getName())) {
            return new JPopupMenuSeparatorBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JSeparator.class.getName())) {
            return new JSeparatorBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JProgressBar.class.getName())) {
            return new JProgressBarBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JSlider.class.getName())) {
            return new JSliderBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JSpinner.class.getName())) {
            return new JSpinnerBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JPopupMenu.class.getName())) {
            return new JPopupMenuBuilder(instance);
        }
        return null;
    }
    
    
    private static final class JLabelBuilder extends JComponentBuilder<JLabel> {
        
        private final String text;
        private final IconBuilder defaultIcon;
        private final int verticalAlignment;
        private final int horizontalAlignment;
        private final int verticalTextPosition;
        private final int horizontalTextPosition;
        private final int iconTextGap;
        
        JLabelBuilder(Instance instance) {
            super(instance, false);
            
            text = Utils.getFieldString(instance, "text");
            
            defaultIcon = IconBuilder.fromField(instance, "defaultIcon");
            
            verticalAlignment = DetailsUtils.getIntFieldValue(instance, "verticalAlignment", JLabel.CENTER);
            horizontalAlignment = DetailsUtils.getIntFieldValue(instance, "horizontalAlignment", JLabel.LEADING);
            verticalTextPosition = DetailsUtils.getIntFieldValue(instance, "verticalTextPosition", JLabel.CENTER);
            horizontalTextPosition = DetailsUtils.getIntFieldValue(instance, "horizontalTextPosition", JLabel.TRAILING);
            iconTextGap = DetailsUtils.getIntFieldValue(instance, "iconTextGap", 4);
        }
        
        protected void setupInstance(JLabel instance) {
            super.setupInstance(instance);
            
            instance.setText(text);
            
            if (defaultIcon != null) instance.setIcon(defaultIcon.createInstance());
            
            instance.setVerticalAlignment(verticalAlignment);
            instance.setHorizontalAlignment(horizontalAlignment);
            instance.setVerticalTextPosition(verticalTextPosition);
            instance.setHorizontalTextPosition(horizontalTextPosition);
            instance.setIconTextGap(iconTextGap);
        }
        
        protected JLabel createInstanceImpl() {
            return new JLabel();
        }
        
    }
    
    private static class JPanelBuilder extends JComponentBuilder<JPanel> {
        
        JPanelBuilder(Instance instance) {
            super(instance);
        }
        
        protected JPanel createInstanceImpl() {
            return new JPanel(null);
        }
        
    }
    
    private static class JToolBarBuilder extends JComponentBuilder<JToolBar> {
        
        private final boolean paintBorder;
        private final InsetsBuilder margin;
        private final boolean floatable;
        private final int orientation;
        
        JToolBarBuilder(Instance instance) {
            super(instance);
            
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            
            margin = InsetsBuilder.fromField(instance, "margin");
            
            floatable = DetailsUtils.getBooleanFieldValue(instance, "floatable", true);
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JToolBar.HORIZONTAL);
        }
        
        protected void setupInstance(JToolBar instance) {
            super.setupInstance(instance);
            
            instance.setBorderPainted(paintBorder);
            
            if (margin != null) instance.setMargin(margin.createInstance());
            
            instance.setFloatable(floatable);
            instance.setOrientation(orientation);
        }
        
        protected JToolBar createInstanceImpl() {
            return new JToolBar();
        }
        
    }
    
    private static class BoxBuilder extends JComponentBuilder<Box> {
        
        BoxBuilder(Instance instance) {
            super(instance);
        }
        
        protected Box createInstanceImpl() {
            return new Box(BoxLayout.X_AXIS) {
                public void layout() {}
                public void setLayout(LayoutManager l) {}
            };
        }
        
    }
    
    private static class BoxFillerBuilder extends JComponentBuilder<Box.Filler> {
        
        BoxFillerBuilder(Instance instance) {
            super(instance);
        }
        
        protected Box.Filler createInstanceImpl() {
            return new Box.Filler(null, null, null);
        }
        
    }
    
    private static class DefaultBoundedRangeModelBuilder extends InstanceBuilder<DefaultBoundedRangeModel> {
        
        private final int value;
        private final int extent;
        private final int min;
        private final int max;
        
        DefaultBoundedRangeModelBuilder(Instance instance) {
            super(instance);
            
            value = DetailsUtils.getIntFieldValue(instance, "value", 0);
            extent = DetailsUtils.getIntFieldValue(instance, "extent", 0);
            min = DetailsUtils.getIntFieldValue(instance, "min", 0);
            max = DetailsUtils.getIntFieldValue(instance, "max", 100);
        }
        
        static DefaultBoundedRangeModelBuilder fromField(Instance instance, String field) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, DefaultBoundedRangeModel.class.getName())) return null;
            return new DefaultBoundedRangeModelBuilder((Instance)model);
        }
        
        protected DefaultBoundedRangeModel createInstanceImpl() {
            return new DefaultBoundedRangeModel(value, extent, min, max);
        }
        
    }
    
    private static class JScrollBarBuilder extends JComponentBuilder<JScrollBar> {
        
        private final DefaultBoundedRangeModelBuilder model;
        private final int orientation;
        
        JScrollBarBuilder(Instance instance) {
            super(instance, false);
            
            model = DefaultBoundedRangeModelBuilder.fromField(instance, "model");
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JScrollBar.VERTICAL);
        }
        
        static JScrollBarBuilder fromField(Instance instance, String field) {
            Object insets = instance.getValueOfField(field);
            if (!(insets instanceof Instance)) return null;
            return new JScrollBarBuilder((Instance)insets);
        }
        
        protected void setupInstance(JScrollBar instance) {
            super.setupInstance(instance);
            
            if (model != null) instance.setModel(model.createInstance());
        }
        
        protected JScrollBar createInstanceImpl() {
            return new JScrollBar(orientation);
        }
        
    }
    
    private static class JSeparatorBuilder extends JComponentBuilder<JSeparator> {
        
        private final int orientation;
        
        JSeparatorBuilder(Instance instance) {
            super(instance, false);
            
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JSeparator.HORIZONTAL);
        }
        
        protected JSeparator createInstanceImpl() {
            return new JSeparator(orientation);
        }
        
    }
    
    private static class JToolBarSeparatorBuilder extends JSeparatorBuilder {
        
        private final DimensionBuilder separatorSize;
        
        JToolBarSeparatorBuilder(Instance instance) {
            super(instance);
            
            separatorSize = DimensionBuilder.fromField(instance, "separatorSize");
        }
        
        protected JSeparator createInstanceImpl() {
            return separatorSize == null ? new JToolBar.Separator() :
                    new JToolBar.Separator(separatorSize.createInstance());
        }
        
    }
    
    private static class JPopupMenuSeparatorBuilder extends JSeparatorBuilder {
        
        JPopupMenuSeparatorBuilder(Instance instance) {
            super(instance);
        }
        
        protected JSeparator createInstanceImpl() {
            return new JPopupMenu.Separator();
        }
        
    }
    
    private static class JProgressBarBuilder extends JComponentBuilder<JProgressBar> {
        
        private final int orientation;
        private final boolean paintBorder;
        private final DefaultBoundedRangeModelBuilder model;
        private final String progressString;
        private final boolean paintString;
        private final boolean indeterminate;
        
        JProgressBarBuilder(Instance instance) {
            super(instance, false);
            
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JProgressBar.HORIZONTAL);
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            model = DefaultBoundedRangeModelBuilder.fromField(instance, "model");
            progressString = Utils.getFieldString(instance, "progressString");
            paintString = DetailsUtils.getBooleanFieldValue(instance, "paintString", false);
            indeterminate = DetailsUtils.getBooleanFieldValue(instance, "indeterminate", false);
        }
        
        protected void setupInstance(JProgressBar instance) {
            super.setupInstance(instance);
            
            instance.setBorderPainted(paintBorder);
            if (model != null) instance.setModel(model.createInstance());
            if (progressString != null) instance.setString(progressString);
            instance.setStringPainted(paintString);
            instance.setIndeterminate(indeterminate);
        }
        
        protected JProgressBar createInstanceImpl() {
            return new JProgressBar(orientation);
        }
        
    }
    
    private static class JSliderBuilder extends JComponentBuilder<JSlider> {
        
        private final boolean paintTicks;
        private final boolean paintTrack;
        private final boolean paintLabels;
        private final boolean isInverted;
        private final DefaultBoundedRangeModelBuilder sliderModel;
        private final int majorTickSpacing;
        private final int minorTickSpacing;
        private final boolean snapToTicks;
        private final int orientation;
        
        JSliderBuilder(Instance instance) {
            super(instance, false);
            
            paintTicks = DetailsUtils.getBooleanFieldValue(instance, "paintTicks", false);
            paintTrack = DetailsUtils.getBooleanFieldValue(instance, "paintTrack", true);
            paintLabels = DetailsUtils.getBooleanFieldValue(instance, "paintLabels", false);
            isInverted = DetailsUtils.getBooleanFieldValue(instance, "isInverted", false);
            sliderModel = DefaultBoundedRangeModelBuilder.fromField(instance, "sliderModel");
            majorTickSpacing = DetailsUtils.getIntFieldValue(instance, "majorTickSpacing", 0);
            minorTickSpacing = DetailsUtils.getIntFieldValue(instance, "minorTickSpacing", 0);
            snapToTicks = DetailsUtils.getBooleanFieldValue(instance, "snapToTicks", false);
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JProgressBar.HORIZONTAL);
        }
        
        protected void setupInstance(JSlider instance) {
            super.setupInstance(instance);
            
            instance.setPaintTicks(paintTicks);
            instance.setPaintTrack(paintTrack);
            instance.setPaintLabels(paintLabels);
            instance.setInverted(isInverted);
            if (sliderModel != null) instance.setModel(sliderModel.createInstance());
            instance.setMajorTickSpacing(majorTickSpacing);
            instance.setMinorTickSpacing(minorTickSpacing);
            instance.setSnapToTicks(snapToTicks);
        }
        
        protected JSlider createInstanceImpl() {
            return new JSlider(orientation);
        }
        
    }
    
    private static Number getNumber(Instance number) {
        if (!DetailsUtils.isSubclassOf(number, Number.class.getName())) return null;
        String _number = DetailsUtils.getInstanceString(number);
        if (_number == null || _number.isEmpty()) return null;
        
        try {
            if (DetailsUtils.isSubclassOf(number, Integer.class.getName()))
                return Integer.valueOf(_number);
            else if (DetailsUtils.isSubclassOf(number, Byte.class.getName()))
                return Byte.valueOf(_number);
            else if (DetailsUtils.isSubclassOf(number, Short.class.getName()))
                return Short.valueOf(_number);
            else if (DetailsUtils.isSubclassOf(number, Long.class.getName()))
                return Long.valueOf(_number);
            else if (DetailsUtils.isSubclassOf(number, Float.class.getName()))
                return Float.valueOf(_number);
            else if (DetailsUtils.isSubclassOf(number, Double.class.getName()))
                return Double.valueOf(_number);
        } catch (NumberFormatException e) {}
        
        return null;
    }
    
    private static class JSpinnerBuilder extends JComponentBuilder<JSpinner> {
        
        private Number value;
        
        JSpinnerBuilder(Instance instance) {
            super(instance, false);
            
            Number val = null;
            Object model = instance.getValueOfField("model");
            if (model instanceof Instance) {
                Object _value = ((Instance)model).getValueOfField("value");
                if (_value instanceof Instance) val = getNumber((Instance)_value);
            }
            value = val;
        }
        
        protected void setupInstance(JSpinner instance) {
            super.setupInstance(instance);
            
            if (value != null) instance.setValue(value);
        }
        
        protected JSpinner createInstanceImpl() {
            return new JSpinner();
        }
        
    }
    
    private static class JPopupMenuBuilder extends JComponentBuilder<JPopupMenu> {
        
        private final String label;
        private final boolean paintBorder;
        
        JPopupMenuBuilder(Instance instance) {
            super(instance);
            
            label = Utils.getFieldString(instance, "label");
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
        }
        
        protected void setupInstance(JPopupMenu instance) {
            super.setupInstance(instance);
            
            if (label != null) instance.setLabel(label);
            instance.setBorderPainted(paintBorder);
        }
        
        protected JPopupMenu createInstanceImpl() {
            return new Utils.JPopupMenuImpl();
        }
        
    }
    
}
