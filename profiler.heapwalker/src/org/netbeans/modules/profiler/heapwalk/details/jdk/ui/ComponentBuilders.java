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

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.border.Border;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.ColorBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.FontBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.RectangleBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BorderBuilders.BorderBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.PlaceholderPanel;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class ComponentBuilders {
    
    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder getBuilder(Instance instance, Heap heap) {
        
        if (Thread.interrupted()) return null;
        
        ComponentBuilder jcomponent = JComponentBuilders.getBuilder(instance, heap);
        if (jcomponent != null) return jcomponent;
        
        ComponentBuilder button = ButtonBuilders.getBuilder(instance, heap);
        if (button != null) return button;
        
        ComponentBuilder textComponent = TextComponentBuilders.getBuilder(instance, heap);
        if (textComponent != null) return textComponent;
        
        ComponentBuilder pane = PaneBuilders.getBuilder(instance, heap);
        if (pane != null) return pane;
        
        ComponentBuilder dataView = DataViewBuilders.getBuilder(instance, heap);
        if (dataView != null) return dataView;
        
        ComponentBuilder window = WindowBuilders.getBuilder(instance, heap);
        if (window != null) return window;
        
        
        // Always at the end - support for unrecognized components
        if (DetailsUtils.isSubclassOf(instance, JComponent.class.getName())) {
            return new JComponentBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, Container.class.getName())) {
            return new ContainerBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, Component.class.getName())) {
            return new ComponentBuilder(instance, heap);
        }
        return null;
    }
    
    
    private static final class ChildrenBuilder extends InstanceBuilder<Component[]> {
        
        private final List<InstanceBuilder<Component>> component;
        
        ChildrenBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            component = new ArrayList();
            
            if (instance instanceof ObjectArrayInstance) {                      // Component[] (JDK 5-)
                List<Instance> components = ((ObjectArrayInstance)instance).getValues();
                for (Instance c : components) {
                    if (c != null) {
                        ComponentBuilder builder = getBuilder(c, heap);
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
                                ComponentBuilder builder = getBuilder(c, heap);
                                if (builder != null) component.add(builder);
                            }
                        }
                    }
                }
            }
        }
        
        static ChildrenBuilder fromField(Instance instance, String field, Heap heap) {
            Object children = instance.getValueOfField(field);
            if (!(children instanceof Instance)) return null;
            return new ChildrenBuilder((Instance)children, heap);
        }
        
        protected Component[] createInstanceImpl() {
            Component[] components = new Component[component.size()];
            for (int i = 0; i < components.length; i++)
                components[i] = component.get(i).createInstance();
            return components;
        }
        
    }
    
    static class ComponentBuilder<T extends Component> extends InstanceBuilder<T> {
        
        protected final String className;
        
        private final RectangleBuilder bounds;
        private final ColorBuilder foreground;
        private final ColorBuilder background;
        private final FontBuilder font;
        private final boolean visible;
        private final boolean enabled;
        
        private boolean isPlaceholder = false;
        
        ComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            className = instance.getJavaClass().getName()+"#"+instance.getInstanceNumber();
            
            bounds = new RectangleBuilder(instance, heap);
            
            foreground = ColorBuilder.fromField(instance, "foreground", heap);
            background = ColorBuilder.fromField(instance, "background", heap);
            
            font = FontBuilder.fromField(instance, "font", heap);
            
            visible = DetailsUtils.getBooleanFieldValue(instance, "visible", true);
            enabled = DetailsUtils.getBooleanFieldValue(instance, "enabled", true);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            instance.setBounds(bounds.createInstance());
            
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
        
        ContainerBuilder(Instance instance, Heap heap) {
            this(instance, heap, true);
        }
        
        protected ContainerBuilder(Instance instance, Heap heap, boolean trackChildren) {
            super(instance, heap);
            
            this.trackChildren = trackChildren;
            component = isVisible() && trackChildren ?
                    ChildrenBuilder.fromField(instance, "component", heap) : null;
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
        
        JComponentBuilder(Instance instance, Heap heap) {
            this(instance, heap, true);
        }
        
        protected JComponentBuilder(Instance instance, Heap heap, boolean trackChildren) {
            super(instance, heap, trackChildren);
            
            isAlignmentXSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentXSet", false);
            alignmentX = DetailsUtils.getFloatFieldValue(instance, "alignmentX", 0);
            isAlignmentYSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentYSet", false);
            alignmentY = DetailsUtils.getFloatFieldValue(instance, "alignmentY", 0);
            
            border = BorderBuilders.fromField(instance, "border", false, heap);
            
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
