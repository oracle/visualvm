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
import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.IconBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ComponentBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.JPopupMenuImpl;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class ButtonBuilders {
    
    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder getBuilder(Instance instance, Heap heap) {
        if (DetailsUtils.isSubclassOf(instance, JButton.class.getName())) {
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
        } else if (DetailsUtils.isSubclassOf(instance, JMenuBar.class.getName())) {
            return new JMenuBarBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JMenuItem.class.getName())) {
            return new JMenuItemBuilder(instance, heap);
        }
        return null;
    }
    
    
    private static final class DefaultButtonModelBuilder extends InstanceBuilder<DefaultButtonModel> {
        
        private final int stateMask;
        
        DefaultButtonModelBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            stateMask = DetailsUtils.getIntFieldValue(instance, "stateMask", 0);
        }
        
        static DefaultButtonModelBuilder fromField(Instance instance, String field, Heap heap) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, DefaultButtonModel.class.getName())) return null;
            return new DefaultButtonModelBuilder((Instance)model, heap);
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
            this(instance, heap, false);
        }
        
        AbstractButtonBuilder(Instance instance, Heap heap, boolean trackChildren) {
            super(instance, heap, false);
            
            model = DefaultButtonModelBuilder.fromField(instance, "model", heap);
            
            text = Utils.getFieldString(instance, "text");
            
            margin = InsetsBuilder.fromField(instance, "margin", heap);
            
            defaultIcon = IconBuilder.fromField(instance, "defaultIcon", heap);
            
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
            super(instance, heap, true);
        }
        
        protected JMenuItem createInstanceImpl() {
            return new JMenuItem();
        }
        
        protected Component createPresenterImpl(JMenuItem instance) {
            JPopupMenu popupMenu = new JPopupMenuImpl();
            popupMenu.add(instance);
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
    
    private static class JMenuBarBuilder extends JComponentBuilder<JMenuBar> {
        
        private final boolean paintBorder;
        private final InsetsBuilder margin;
        
        JMenuBarBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            margin = InsetsBuilder.fromField(instance, "margin", heap);
        }
        
        protected void setupInstance(JMenuBar instance) {
            super.setupInstance(instance);
            
            instance.setBorderPainted(paintBorder);
            if (margin != null) instance.setMargin(margin.createInstance());
        }
        
        protected JMenuBar createInstanceImpl() {
            return new JMenuBar();
        }
        
    }
    
}
