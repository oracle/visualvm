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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.border.Border;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.ColorBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.FontBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BaseBuilders.RectangleBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.BorderBuilders.BorderBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.PlaceholderPanel;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class ComponentBuilders {

    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder getBuilder(Instance instance) {

        if (Thread.interrupted()) return null;

        ComponentBuilder jcomponent = JComponentBuilders.getBuilder(instance);
        if (jcomponent != null) return jcomponent;

        ComponentBuilder button = ButtonBuilders.getBuilder(instance);
        if (button != null) return button;

        ComponentBuilder textComponent = TextComponentBuilders.getBuilder(instance);
        if (textComponent != null) return textComponent;

        ComponentBuilder pane = PaneBuilders.getBuilder(instance);
        if (pane != null) return pane;

        ComponentBuilder dataView = DataViewBuilders.getBuilder(instance);
        if (dataView != null) return dataView;
        
        ComponentBuilder window = WindowBuilders.getBuilder(instance);
        if (window != null) return window;
        
        
        // Always at the end - support for unrecognized components
        if (DetailsUtils.isSubclassOf(instance, JComponent.class.getName())) {
            return new JComponentBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, Container.class.getName())) {
            return new ContainerBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, Component.class.getName())) {
            return new ComponentBuilder(instance);
        }
        return null;
    }
    
    
    private static final class ChildrenBuilder extends InstanceBuilder<Component[]> {
        
        private final List<InstanceBuilder<Component>> component;
        
        ChildrenBuilder(Instance instance) {
            super(instance);
            
            component = new ArrayList();
            
            if (instance instanceof ObjectArrayInstance) {                      // Component[] (JDK 5-)
                List<Instance> components = ((ObjectArrayInstance)instance).getValues();
                for (Instance c : components) {
                    if (c != null) {
                        ComponentBuilder builder = getBuilder(c);
                        if (builder != null) component.add(builder);
                    }
                }
            } else {                                                            // ArrayList<Component> (JDK 6+)
                int size = DetailsUtils.getIntFieldValue(instance, "size", 0);
                if (size > 0) {
                    Object elementData = instance.getValueOfField("elementData");
                    if (elementData instanceof ObjectArrayInstance) {
                        List<Instance> components = ((ObjectArrayInstance)elementData).getValues();
                        for (Instance c : components) {
                            if (c != null) {
                                ComponentBuilder builder = getBuilder(c);
                                if (builder != null) component.add(builder);
                            }
                        }
                    }
                }
            }
        }
        
        static ChildrenBuilder fromField(Instance instance, String field) {
            Object children = instance.getValueOfField(field);
            if (!(children instanceof Instance)) return null;
            return new ChildrenBuilder((Instance)children);
        }
        
        protected Component[] createInstanceImpl() {
            Component[] components = new Component[component.size()];
            for (int i = 0; i < components.length; i++)
                components[i] = component.get(i).createInstance();
            return components;
        }
        
    }
    
    static class ComponentBuilder<T extends Component> extends InstanceBuilder<T> {
        
        private final static int MAX_WIDTH = 10000;
        private final static int MAX_HEIGHT = 10000;
        
        protected final String className;
        
        private final RectangleBuilder bounds;
        private final ColorBuilder foreground;
        private final ColorBuilder background;
        private final FontBuilder font;
        private final boolean visible;
        private final boolean enabled;
        
        private boolean isPlaceholder = false;
        
        ComponentBuilder(Instance instance) {
            super(instance);
            
            className = instance.getJavaClass().getName()+"#"+instance.getInstanceNumber();
            
            bounds = new RectangleBuilder(instance);
            
            foreground = ColorBuilder.fromField(instance, "foreground");
            background = ColorBuilder.fromField(instance, "background");
            
            font = FontBuilder.fromField(instance, "font");
            
            visible = DetailsUtils.getBooleanFieldValue(instance, "visible", true);
            enabled = DetailsUtils.getBooleanFieldValue(instance, "enabled", true);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            // #250485 - large components may cause OOME when previewing
            Rectangle rect = bounds.createInstance();
            if (rect != null) {
                rect.width = Math.min(rect.width, MAX_WIDTH);
                rect.height = Math.min(rect.height, MAX_HEIGHT);
                instance.setBounds(rect);
            }
            
//            if (foreground != null) instance.setForeground(foreground.createInstance());
//            if (background != null) instance.setBackground(background.createInstance());
            
            if (foreground != null && (isPlaceholder || !foreground.isUIResource()))
                    instance.setForeground(foreground.createInstance());
            if (background != null && (isPlaceholder || !background.isUIResource()))
                    instance.setBackground(background.createInstance());
            
            if (font != null && (isPlaceholder || !font.isUIResource()))
                instance.setFont(font.createInstance());
            
            instance.setVisible(visible);
            instance.setEnabled(enabled);
        }
        
        protected final boolean isVisible() {
            return visible;
        }
        
        protected final void setPlaceholder() {
            isPlaceholder = true;
        }
        
        protected final boolean isPlaceholder() {
            return isPlaceholder;
        }
        
        protected T createInstanceImpl() {
            setPlaceholder();
            return (T)new PlaceholderPanel(className);
        }
        
        protected Component createPresenterImpl(T instance) { return instance; }
        
        final Component createPresenter() {
            T instance = createInstance();
            return instance != null ? createPresenterImpl(instance) : null;
        }
        
    }
    
    static class ContainerBuilder<T extends Container> extends ComponentBuilder<T> {
        
        private final boolean trackChildren;
        private final ChildrenBuilder component;
        
        ContainerBuilder(Instance instance) {
            this(instance, true);
        }
        
        protected ContainerBuilder(Instance instance, boolean trackChildren) {
            super(instance);
            
            this.trackChildren = trackChildren;
            component = isVisible() && trackChildren ?
                    ChildrenBuilder.fromField(instance, "component") : null;
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            if (trackChildren) {
                instance.setLayout(null);
                instance.removeAll();
                if (component != null) {
                    Component[] components = component.createInstance();
                    for (Component c : components) instance.add(c);
                }
            }
        }
        
        protected T createInstanceImpl() {
            setPlaceholder();
            return (T)new PlaceholderPanel(className);
        }
        
    }
    
    static class JComponentBuilder<T extends JComponent> extends ContainerBuilder<T> {
        
        private final boolean isAlignmentXSet;
        private final float alignmentX;
        private final boolean isAlignmentYSet;
        private final float alignmentY;
        private final BorderBuilder border;
        private final int flags;
        
        JComponentBuilder(Instance instance) {
            this(instance, true);
        }
        
        protected JComponentBuilder(Instance instance, boolean trackChildren) {
            super(instance, trackChildren);
            
            isAlignmentXSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentXSet", false);
            alignmentX = DetailsUtils.getFloatFieldValue(instance, "alignmentX", 0);
            isAlignmentYSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentYSet", false);
            alignmentY = DetailsUtils.getFloatFieldValue(instance, "alignmentY", 0);
            
            border = BorderBuilders.fromField(instance, "border", false);
            
            flags = DetailsUtils.getIntFieldValue(instance, "flags", 0);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            instance.putClientProperty("className", className);
            
            if (isAlignmentXSet) instance.setAlignmentX(alignmentX);
            if (isAlignmentYSet) instance.setAlignmentY(alignmentY);
            
            if (border != null && (isPlaceholder() || !border.isUIResource())) {
                Border b = border.createInstance();
                if (b != null) instance.setBorder(b);
            }
            
            int opaque_mask = (1 << 3);
            boolean opaque = (flags & opaque_mask) == opaque_mask;
            instance.setOpaque(opaque);
        }
        
        protected T createInstanceImpl() {
            setPlaceholder();
            return (T)new PlaceholderPanel(className);
        }
        
    }
    
}
