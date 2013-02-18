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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
        protected final Map<String, Object> map = new HashMap();

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
            
            if (instance instanceof ObjectArrayInstance) {                      // Component[] (JDK 5)
                List<Instance> components = ((ObjectArrayInstance)instance).getValues();
                for (Instance c : components) {
                    // TODO: read ncomponents, skip trailing null items
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
        
        ComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            className = instance.getJavaClass().getName();
            
            bounds = new RectangleBuilder(instance, heap);
            
            Object foreground = instance.getValueOfField("foreground");
            if (foreground instanceof Instance)
                map.put("foreground", new ColorBuilder((Instance)foreground, heap));
            Object background = instance.getValueOfField("background");
            if (background instanceof Instance)
                map.put("background", new ColorBuilder((Instance)background, heap));
            
            Object font = instance.getValueOfField("font");
            if (font instanceof Instance)
                map.put("font", new FontBuilder((Instance)font, heap));
            
            map.put("visible", DetailsUtils.getBooleanFieldValue(instance, "visible", true));
            map.put("enabled", DetailsUtils.getBooleanFieldValue(instance, "enabled", true));
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            instance.setBounds(bounds.createInstance());
            
            ColorBuilder foreground = (ColorBuilder)map.get("foreground");
            if (foreground != null) instance.setForeground(foreground.createInstance());
            ColorBuilder background = (ColorBuilder)map.get("background");
            if (background != null) instance.setBackground(background.createInstance());
            
            FontBuilder font = (FontBuilder)map.get("font");
            if (font != null) instance.setFont(font.createInstance());
            
            instance.setVisible((Boolean)map.get("visible"));
            instance.setEnabled((Boolean)map.get("enabled"));
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
        
        ContainerBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            Object component = instance.getValueOfField("component");
            if (component instanceof Instance)
                map.put("component", new ChildrenBuilder((Instance)component, heap));
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            ChildrenBuilder component = (ChildrenBuilder)map.get("component");
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
        
        JComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            boolean isAlignmentXSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentXSet", false);
            if (isAlignmentXSet) map.put("alignmentX", DetailsUtils.getFloatFieldValue(instance, "alignmentX", 0));
            boolean isAlignmentYSet = DetailsUtils.getBooleanFieldValue(instance, "isAlignmentYSet", false);
            if (isAlignmentYSet) map.put("alignmentY", DetailsUtils.getFloatFieldValue(instance, "alignmentY", 0));
            
            map.put("flags", DetailsUtils.getIntFieldValue(instance, "flags", 0));
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            Float alignmentX = (Float)map.get("alignmentX");
            if (alignmentX != null) instance.setAlignmentX(alignmentX);
            Float alignmentY = (Float)map.get("alignmentY");
            if (alignmentY != null) instance.setAlignmentY(alignmentY);
            
            int opaque_mask = (1 << 3);
            boolean opaque = ((Integer)map.get("flags") & opaque_mask) == opaque_mask;
            instance.setOpaque(opaque);
        }
        
        protected T createInstanceImpl() {
            return (T)new PlaceholderPanel(className);
        }
        
    }
    
    private static final class JLabelBuilder extends JComponentBuilder<JLabel> {
        
        JLabelBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            map.put("text", DetailsUtils.getInstanceFieldString(instance, "text", heap));
            
            Object defaultIcon = instance.getValueOfField("defaultIcon");
//            if (defaultIcon instanceof Instance)
            if (defaultIcon instanceof Instance &&
                DetailsUtils.isSubclassOf((Instance)defaultIcon, "javax.swing.ImageIcon"))
                map.put("defaultIcon", new IconBuilder((Instance)defaultIcon, heap));
            
            map.put("verticalAlignment", DetailsUtils.getIntFieldValue(instance, "verticalAlignment", JLabel.CENTER));
            map.put("horizontalAlignment", DetailsUtils.getIntFieldValue(instance, "horizontalAlignment", JLabel.LEADING));
            map.put("verticalTextPosition", DetailsUtils.getIntFieldValue(instance, "verticalTextPosition", JLabel.CENTER));
            map.put("horizontalTextPosition", DetailsUtils.getIntFieldValue(instance, "horizontalTextPosition", JLabel.TRAILING));
            map.put("iconTextGap", DetailsUtils.getIntFieldValue(instance, "iconTextGap", 4));
        }
        
        protected void setupInstance(JLabel instance) {
            super.setupInstance(instance);
            
            instance.setText((String)map.get("text"));
            
            IconBuilder defaultIcon = (IconBuilder)map.get("defaultIcon");
            if (defaultIcon != null) instance.setIcon(defaultIcon.createInstance());
            
            instance.setVerticalAlignment((Integer)map.get("verticalAlignment"));
            instance.setHorizontalAlignment((Integer)map.get("horizontalAlignment"));
            instance.setVerticalTextPosition((Integer)map.get("verticalTextPosition"));
            instance.setHorizontalTextPosition((Integer)map.get("horizontalTextPosition"));
            instance.setIconTextGap((Integer)map.get("iconTextGap"));
        }
        
        protected JLabel createInstanceImpl() {
            return new JLabel();
        }
        
    }
    
    private static final class DefaultButtonModelBuilder extends InstanceBuilder<DefaultButtonModel> {
        
        DefaultButtonModelBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            map.put("stateMask", DetailsUtils.getIntFieldValue(instance, "stateMask", 0));
        }
        
        protected void setupInstance(DefaultButtonModel instance) {
            super.setupInstance(instance);
            
            int stateMask = (Integer)map.get("stateMask");
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
        
        AbstractButtonBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            Object _model = instance.getValueOfField("model");
            if (_model instanceof Instance) {
                Instance model = (Instance)_model;
                if (DetailsUtils.isSubclassOf(model, DefaultButtonModel.class.getName()))
                    map.put("model", new DefaultButtonModelBuilder(model, heap));
            }
            
            map.put("text", DetailsUtils.getInstanceFieldString(instance, "text", heap));
            
            Object margin = instance.getValueOfField("margin");
            if (margin instanceof Instance)
                map.put("margin", new InsetsBuilder((Instance)margin, heap));
            
            Object defaultIcon = instance.getValueOfField("defaultIcon");
//            if (defaultIcon instanceof Instance)
            if (defaultIcon instanceof Instance &&
                DetailsUtils.isSubclassOf((Instance)defaultIcon, "javax.swing.ImageIcon"))
                map.put("defaultIcon", new IconBuilder((Instance)defaultIcon, heap));
            
            boolean borderPaintedSet = DetailsUtils.getBooleanFieldValue(instance, "borderPaintedSet", false);
            if (borderPaintedSet) map.put("paintBorder", DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true));
            boolean contentAreaFilledSet = DetailsUtils.getBooleanFieldValue(instance, "contentAreaFilledSet", false);
            if (contentAreaFilledSet) map.put("contentAreaFilled", DetailsUtils.getBooleanFieldValue(instance, "contentAreaFilled", true));
            
            map.put("verticalAlignment", DetailsUtils.getIntFieldValue(instance, "verticalAlignment", AbstractButton.CENTER));
            map.put("horizontalAlignment", DetailsUtils.getIntFieldValue(instance, "horizontalAlignment", AbstractButton.CENTER));
            map.put("verticalTextPosition", DetailsUtils.getIntFieldValue(instance, "verticalTextPosition", AbstractButton.CENTER));
            map.put("horizontalTextPosition", DetailsUtils.getIntFieldValue(instance, "horizontalTextPosition", AbstractButton.TRAILING));
            map.put("iconTextGap", DetailsUtils.getIntFieldValue(instance, "iconTextGap", 4));
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            DefaultButtonModelBuilder model = (DefaultButtonModelBuilder)map.get("model");
            if (model != null) instance.setModel(model.createInstance());
            
            instance.setText((String)map.get("text"));
            
            InsetsBuilder margin = (InsetsBuilder)map.get("margin");
            if (margin != null) instance.setMargin(margin.createInstance());
            
            IconBuilder defaultIcon = (IconBuilder)map.get("defaultIcon");
            if (defaultIcon != null) instance.setIcon(defaultIcon.createInstance());
            
            Boolean paintBorder = (Boolean)map.get("paintBorder");
            if (paintBorder != null) instance.setBorderPainted(paintBorder);
            Boolean contentAreaFilled = (Boolean)map.get("contentAreaFilled");
            if (contentAreaFilled != null) instance.setContentAreaFilled(contentAreaFilled);
            
            instance.setVerticalAlignment((Integer)map.get("verticalAlignment"));
            instance.setHorizontalAlignment((Integer)map.get("horizontalAlignment"));
            instance.setVerticalTextPosition((Integer)map.get("verticalTextPosition"));
            instance.setHorizontalTextPosition((Integer)map.get("horizontalTextPosition"));
            instance.setIconTextGap((Integer)map.get("iconTextGap"));
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
        
        JCheckBoxBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            map.put("flat", DetailsUtils.getBooleanFieldValue(instance, "flat", false));
        }
        
        protected JToggleButton createInstanceImpl() {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setBorderPaintedFlat((Boolean)map.get("flat"));
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
        
        JComboBoxBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            map.put("isEditable", DetailsUtils.getBooleanFieldValue(instance, "isEditable", false));
        }
        
        protected void setupInstance(JComboBox instance) {
            super.setupInstance(instance);
            
            instance.setEditable((Boolean)map.get("isEditable"));
        }
        
        protected JComboBox createInstanceImpl() {
            return new JComboBox();
        }
        
    }
    
    private static abstract class JTextComponentBuilder<T extends JTextComponent> extends JComponentBuilder<T> {
        
        JTextComponentBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            map.put("isEditable", DetailsUtils.getBooleanFieldValue(instance, "isEditable", false));
            
            Object margin = instance.getValueOfField("margin");
            if (margin instanceof Instance)
                map.put("margin", new InsetsBuilder((Instance)margin, heap));
        }
        
        protected void setupInstance(T instance) {
            super.setupInstance(instance);
            
            instance.setEditable((Boolean)map.get("isEditable"));
            
            InsetsBuilder margin = (InsetsBuilder)map.get("margin");
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
        
        JToolBarBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            map.put("paintBorder", DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true));
            
            Object margin = instance.getValueOfField("margin");
            if (margin instanceof Instance)
                map.put("margin", new InsetsBuilder((Instance)margin, heap));
            
            map.put("floatable", DetailsUtils.getBooleanFieldValue(instance, "floatable", true));
            map.put("orientation", DetailsUtils.getIntFieldValue(instance, "orientation", JToolBar.HORIZONTAL));
        }
        
        protected void setupInstance(JToolBar instance) {
            super.setupInstance(instance);
            
            instance.setBorderPainted((Boolean)map.get("paintBorder"));
            
            InsetsBuilder margin = (InsetsBuilder)map.get("margin");
            if (margin != null) instance.setMargin(margin.createInstance());
            
            instance.setFloatable((Boolean)map.get("floatable"));
            instance.setOrientation((Integer)map.get("orientation"));
        }
        
        protected JToolBar createInstanceImpl() {
            return new JToolBar();
        }
        
    }
    
}
