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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.text.JTextComponent;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.PlaceholderIcon;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.PlaceholderPanel;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class Builders {
    
    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder getComponentBuilder(Instance instance, Heap heap) {
        if (DetailsUtils.isSubclassOf(instance, JLabel.class.getName())) {
            return new JLabelBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JButton.class.getName())) {
            return new JButtonBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JCheckBox.class.getName())) {
            return new JCheckBoxBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JRadioButton.class.getName())) {
            return new JRadioButtonBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JToggleButton.class.getName())) {
            return new JToggleButtonBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JCheckBoxMenuItem.class.getName())) {
            return new JCheckBoxMenuItemBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JRadioButtonMenuItem.class.getName())) {
            return new JRadioButtonMenuItemBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JMenu.class.getName())) {
            return new JMenuBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JMenuItem.class.getName())) {
            return new JMenuItemBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JPanel.class.getName())) {
            return new JPanelBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JComboBox.class.getName())) {
            return new JComboBoxBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JTextField.class.getName())) {
            return new JTextFieldBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JTextArea.class.getName())) {
            return new JTextAreaBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JEditorPane.class.getName())) {
            return new JEditorPaneBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JToolBar.class.getName())) {
            return new JToolBarBuilder(instance, heap);
        }
        
        
        // Always at the end - support for unrecognized components
         else if (DetailsUtils.isSubclassOf(instance, JComponent.class.getName())) {
            return new JComponentBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, Container.class.getName())) {
            return new ContainerBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, Component.class.getName())) {
            return new ComponentBuilder(instance, heap);
        }
        return null;
    }
    
    
    static abstract class InstanceBuilder<T> {

        InstanceBuilder(Instance instance, Heap heap) {}

        protected void setupInstance(T instance) {}

        protected T createInstanceImpl() { return null; }
        
        final T createInstance() {
            T instance = createInstanceImpl();
            if (instance != null) setupInstance(instance);
            return instance;
        }

    }
    
    static final class PointBuilder extends InstanceBuilder<Point> {
        
        private final int x;
        private final int y;
        
        PointBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            x = DetailsUtils.getIntFieldValue(instance, "x", 0);
            y = DetailsUtils.getIntFieldValue(instance, "y", 0);
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
        
        protected Insets createInstanceImpl() {
            return new Insets(top, left, bottom, right);
        }
        
    }
    
    static final class FontBuilder extends InstanceBuilder<Font> {
        
        private final String name;
        private final int style;
        private final int size;
        
        FontBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            name = Utils.getFontName(instance, heap);
            style = DetailsUtils.getIntFieldValue(instance, "style", 0);
            size = DetailsUtils.getIntFieldValue(instance, "size", 10);
        }
        
        protected Font createInstanceImpl() {
            return new Font(name, style, size);
        }
        
    }
    
    static final class ColorBuilder extends InstanceBuilder<Color> {
        
        private final int value;
        
        ColorBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            value = DetailsUtils.getIntFieldValue(instance, "value", 0);
        }
        
        protected Color createInstanceImpl() {
            return new Color(value);
        }
        
    }
    
    static final class IconBuilder extends InstanceBuilder<Icon> {
        
        private final int width;
        private final int height;
        
        IconBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            width = DetailsUtils.getIntFieldValue(instance, "width", 0);
            height = DetailsUtils.getIntFieldValue(instance, "height", 0);
        }
        
        protected Icon createInstanceImpl() {
            return new PlaceholderIcon(width, height);
        }
        
    }
    
    static final class ChildrenBuilder extends InstanceBuilder<Component[]> {
        
        private final List<InstanceBuilder<Component>> component;
        
        ChildrenBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            component = new ArrayList();
            
            if (instance instanceof ObjectArrayInstance) {                      // Component[] (JDK 5-)
                List<Instance> components = ((ObjectArrayInstance)instance).getValues();
                for (Instance c : components) {
                    if (c != null) {
                        ComponentBuilder builder = getComponentBuilder(c, heap);
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
                                ComponentBuilder builder = getComponentBuilder(c, heap);
                                if (builder != null) component.add(builder);
                            }
                        }
                    }
                }
            }
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
        
        ComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            className = instance.getJavaClass().getName();
            
            bounds = new RectangleBuilder(instance, heap);
            
            Object _foreground = instance.getValueOfField("foreground");
            foreground = _foreground instanceof Instance ? 
                    new ColorBuilder((Instance)_foreground, heap) : null;
            Object _background = instance.getValueOfField("background");
            background = _background instanceof Instance ? 
                    new ColorBuilder((Instance)_background, heap) : null;
            
            Object _font = instance.getValueOfField("font");
            font = _font instanceof Instance ? 
                    new FontBuilder((Instance)_font, heap) : null;
            
            visible = DetailsUtils.getBooleanFieldValue(instance, "visible", true);
            enabled = DetailsUtils.getBooleanFieldValue(instance, "enabled", true);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            instance.setBounds(bounds.createInstance());
            
            if (foreground != null) instance.setForeground(foreground.createInstance());
            if (background != null) instance.setBackground(background.createInstance());
            
            if (font != null) instance.setFont(font.createInstance());
            
            instance.setVisible(visible);
            instance.setEnabled(enabled);
        }
        
        protected T createInstanceImpl() {
            return (T)new PlaceholderPanel(className);
        }
        
        protected Component createPresenterImpl(T instance) { return instance; }
        
        final Component createPresenter() {
            T instance = createInstance();
            return instance != null ? createPresenterImpl(instance) : null;
        }
        
    }
    
    private static class ContainerBuilder<T extends Container> extends ComponentBuilder<T> {
        
        private final ChildrenBuilder component;
        
        ContainerBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            Object _component = instance.getValueOfField("component");
            component = _component instanceof Instance ? 
                    new ChildrenBuilder((Instance)_component, heap) : null;
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            if (component != null) {
                Component[] components = component.createInstance();
                for (Component c : components) instance.add(c);
            }
        }
        
        protected T createInstanceImpl() {
            return (T)new PlaceholderPanel(className);
        }
        
    }
    
    private static class JComponentBuilder<T extends JComponent> extends ContainerBuilder<T> {
        
        private final boolean isAlignmentXSet;
        private final float alignmentX;
        private final boolean isAlignmentYSet;
        private final float alignmentY;
        private final int flags;
        
        JComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            isAlignmentXSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentXSet", false);
            alignmentX = DetailsUtils.getFloatFieldValue(instance, "alignmentX", 0);
            isAlignmentYSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentYSet", false);
            alignmentY = DetailsUtils.getFloatFieldValue(instance, "alignmentY", 0);
            
            flags = DetailsUtils.getIntFieldValue(instance, "flags", 0);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            if (isAlignmentXSet) instance.setAlignmentX(alignmentX);
            if (isAlignmentYSet) instance.setAlignmentY(alignmentY);
            
            int opaque_mask = (1 << 3);
            boolean opaque = (flags & opaque_mask) == opaque_mask;
            instance.setOpaque(opaque);
        }
        
        protected T createInstanceImpl() {
            return (T)new PlaceholderPanel(className);
        }
        
    }
    
    private static final class JLabelBuilder extends JComponentBuilder<JLabel> {
        
        private final String text;
        private final IconBuilder defaultIcon;
        private final int verticalAlignment;
        private final int horizontalAlignment;
        private final int verticalTextPosition;
        private final int horizontalTextPosition;
        private final int iconTextGap;
        
        JLabelBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            text = DetailsUtils.getInstanceFieldString(instance, "text", heap);
            
            Object _defaultIcon = instance.getValueOfField("defaultIcon");
            defaultIcon = _defaultIcon instanceof Instance &&
                DetailsUtils.isSubclassOf((Instance)_defaultIcon, "javax.swing.ImageIcon") ?
                    new IconBuilder((Instance)_defaultIcon, heap) : null;
            
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
    
    private static final class DefaultButtonModelBuilder extends InstanceBuilder<DefaultButtonModel> {
        
        private final int stateMask;
        
        DefaultButtonModelBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            stateMask = DetailsUtils.getIntFieldValue(instance, "stateMask", 0);
        }
        
        protected void setupInstance(DefaultButtonModel instance) {
            super.setupInstance(instance);
            
            instance.setArmed((stateMask & DefaultButtonModel.ARMED) != 0);
            instance.setSelected((stateMask & DefaultButtonModel.SELECTED) != 0);
            instance.setEnabled((stateMask & DefaultButtonModel.ENABLED) != 0);
            instance.setPressed((stateMask & DefaultButtonModel.PRESSED) != 0);
            instance.setRollover((stateMask & DefaultButtonModel.ROLLOVER) != 0);
        }
        
        protected DefaultButtonModel createInstanceImpl() {
            return new DefaultButtonModel();
        }
        
    }
    
    private static abstract class AbstractButtonBuilder<T extends AbstractButton> extends JComponentBuilder<T> {
        
        private final DefaultButtonModelBuilder model;
        private final String text;
        private final InsetsBuilder margin;
        private final IconBuilder defaultIcon;
        private final int verticalAlignment;
        private final boolean borderPaintedSet;
        private final boolean paintBorder;
        private final boolean contentAreaFilledSet;
        private final boolean contentAreaFilled;
        private final int horizontalAlignment;
        private final int verticalTextPosition;
        private final int horizontalTextPosition;
        private final int iconTextGap;
        
        AbstractButtonBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            Object _model = instance.getValueOfField("model");
            if (_model instanceof Instance) {
                Instance __model = (Instance)_model;
                model = DetailsUtils.isSubclassOf(__model, DefaultButtonModel.class.getName()) ?
                        new DefaultButtonModelBuilder(__model, heap) : null;
            } else {
                model = null;
            }
            
            text = DetailsUtils.getInstanceFieldString(instance, "text", heap);
            
            Object _margin = instance.getValueOfField("margin");
            margin = _margin instanceof Instance ?
                    new InsetsBuilder((Instance)_margin, heap) : null;
            
            Object _defaultIcon = instance.getValueOfField("defaultIcon");
            defaultIcon = _defaultIcon instanceof Instance &&
                DetailsUtils.isSubclassOf((Instance)_defaultIcon, "javax.swing.ImageIcon") ?
                    new IconBuilder((Instance)_defaultIcon, heap) : null;
            
            borderPaintedSet = DetailsUtils.getBooleanFieldValue(instance, "borderPaintedSet", false);
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            contentAreaFilledSet = DetailsUtils.getBooleanFieldValue(instance, "contentAreaFilledSet", false);
            contentAreaFilled = DetailsUtils.getBooleanFieldValue(instance, "contentAreaFilled", true);
            
            verticalAlignment = DetailsUtils.getIntFieldValue(instance, "verticalAlignment", JLabel.CENTER);
            horizontalAlignment = DetailsUtils.getIntFieldValue(instance, "horizontalAlignment", JLabel.LEADING);
            verticalTextPosition = DetailsUtils.getIntFieldValue(instance, "verticalTextPosition", JLabel.CENTER);
            horizontalTextPosition = DetailsUtils.getIntFieldValue(instance, "horizontalTextPosition", JLabel.TRAILING);
            iconTextGap = DetailsUtils.getIntFieldValue(instance, "iconTextGap", 4);
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            if (model != null) instance.setModel(model.createInstance());
            
            instance.setText(text);
            
            if (margin != null) instance.setMargin(margin.createInstance());
            
            if (defaultIcon != null) instance.setIcon(defaultIcon.createInstance());
            
            if (borderPaintedSet) instance.setBorderPainted(paintBorder);
            if (contentAreaFilledSet) instance.setContentAreaFilled(contentAreaFilled);
            
            instance.setVerticalAlignment(verticalAlignment);
            instance.setHorizontalAlignment(horizontalAlignment);
            instance.setVerticalTextPosition(verticalTextPosition);
            instance.setHorizontalTextPosition(horizontalTextPosition);
            instance.setIconTextGap(iconTextGap);
        }
        
    }
    
    private static final class JButtonBuilder extends AbstractButtonBuilder<JButton> {
        
        JButtonBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JButton createInstanceImpl() {
            return new JButton();
        }
        
    }
    
    private static class JToggleButtonBuilder extends AbstractButtonBuilder<JToggleButton> {
        
        JToggleButtonBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JToggleButton createInstanceImpl() {
            return new JToggleButton();
        }
        
    }
    
    private static class JCheckBoxBuilder extends JToggleButtonBuilder {
        
        private final boolean flat;
        
        JCheckBoxBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            flat = DetailsUtils.getBooleanFieldValue(instance, "flat", false);
        }
        
        protected JToggleButton createInstanceImpl() {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setBorderPaintedFlat(flat);
            return checkBox;
        }
        
    }
    
    private static class JRadioButtonBuilder extends JToggleButtonBuilder {
        
        JRadioButtonBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JToggleButton createInstanceImpl() {
            return new JRadioButton();
        }
        
    }
    
    private static class JMenuItemBuilder extends AbstractButtonBuilder<JMenuItem> {
        
        JMenuItemBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JMenuItem();
        }
        
        protected Component createPresenterImpl(JMenuItem instance) {
            JPopupMenu popupMenu = new JPopupMenu() {
                public void setVisible(boolean visible) {}
                public boolean isVisible() { return true; }
            };
            popupMenu.setInvoker(new JMenu());
            popupMenu.add(instance);
            instance.setOpaque(false);
            return popupMenu;
        }
        
    }
    
    private static class JCheckBoxMenuItemBuilder extends JMenuItemBuilder {
        
        JCheckBoxMenuItemBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JCheckBoxMenuItem();
        }
        
    }
    
    private static class JRadioButtonMenuItemBuilder extends JMenuItemBuilder {
        
        JRadioButtonMenuItemBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JRadioButtonMenuItem();
        }
        
    }
    
    private static class JMenuBuilder extends JMenuItemBuilder {
        
        JMenuBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JMenu();
        }
        
        protected Component createPresenterImpl(JMenuItem instance) {
            JMenuBar menuBar = new JMenuBar();
            menuBar.add(instance);
            return menuBar;
        }
        
    }
    
    private static class JPanelBuilder extends JComponentBuilder<JPanel> {
        
        JPanelBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JPanel createInstanceImpl() {
            return new JPanel(null);
        }
        
    }
    
    private static class JComboBoxBuilder extends JComponentBuilder<JComboBox> {
        
        private final boolean isEditable;
        
        JComboBoxBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            isEditable = DetailsUtils.getBooleanFieldValue(instance, "isEditable", false);
        }
        
        protected void setupInstance(JComboBox instance) {
            super.setupInstance(instance);
            
            instance.setEditable(isEditable);
        }
        
        protected JComboBox createInstanceImpl() {
            return new JComboBox();
        }
        
    }
    
    private static abstract class JTextComponentBuilder<T extends JTextComponent> extends JComponentBuilder<T> {
        
        private final boolean isEditable;
        private final InsetsBuilder margin;
        
        JTextComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            isEditable = DetailsUtils.getBooleanFieldValue(instance, "isEditable", false);
            
            Object _margin = instance.getValueOfField("margin");
            margin = _margin instanceof Instance ?
                    new InsetsBuilder((Instance)_margin, heap) : null;
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            instance.setEditable(isEditable);
            
            if (margin != null) instance.setMargin(margin.createInstance());
        }
        
    }
    
    private static class JTextFieldBuilder extends JTextComponentBuilder<JTextField> {
        
        JTextFieldBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JTextField createInstanceImpl() {
            return new JTextField();
        }
        
    }
    
    private static class JTextAreaBuilder extends JTextComponentBuilder<JTextArea> {
        
        JTextAreaBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JTextArea createInstanceImpl() {
            return new JTextArea();
        }
        
    }
    
    private static class JEditorPaneBuilder extends JTextComponentBuilder<JEditorPane> {
        
        JEditorPaneBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JEditorPane createInstanceImpl() {
            return new JEditorPane();
        }
        
    }
    
    private static class JToolBarBuilder extends JComponentBuilder<JToolBar> {
        
        private final boolean paintBorder;
        private final InsetsBuilder margin;
        private final boolean floatable;
        private final int orientation;
        
        JToolBarBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            
            Object _margin = instance.getValueOfField("margin");
            margin = _margin instanceof Instance ?
                    new InsetsBuilder((Instance)_margin, heap) : null;
            
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
    
}
