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

import java.awt.LayoutManager;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.DimensionBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.IconBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BaseBuilders.InsetsBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.BorderBuilders.BorderBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ComponentBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class JComponentBuilders {
    
    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder getBuilder(Instance instance, Heap heap) {
        if (DetailsUtils.isSubclassOf(instance, JLabel.class.getName())) {
            return new JLabelBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JPanel.class.getName())) {
            return new JPanelBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JComboBox.class.getName())) {
            return new JComboBoxBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JToolBar.class.getName())) {
            return new JToolBarBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, Box.Filler.class.getName())) {
            return new BoxFillerBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, Box.class.getName())) {
            return new BoxBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JScrollBar.class.getName())) {
            return new JScrollBarBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JToolBar.Separator.class.getName())) {
            return new JToolBarSeparatorBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JPopupMenu.Separator.class.getName())) {
            return new JPopupMenuSeparatorBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JSeparator.class.getName())) {
            return new JSeparatorBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JProgressBar.class.getName())) {
            return new JProgressBarBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JSlider.class.getName())) {
            return new JSliderBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JList.class.getName())) {
            return new JListBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JTree.class.getName())) {
            return new JTreeBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JTable.class.getName())) {
            return new JTableBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JTableHeader.class.getName())) {
            return new JTableHeaderBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JViewport.class.getName())) {
            return new JViewportBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JScrollPane.class.getName())) {
            return new JScrollPaneBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JSpinner.class.getName())) {
            return new JSpinnerBuilder(instance, heap);
        } else if (DetailsUtils.isSubclassOf(instance, JPopupMenu.class.getName())) {
            return new JPopupMenuBuilder(instance, heap);
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
        
        JLabelBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
            
            text = Utils.getFieldString(instance, "text");
            
            defaultIcon = IconBuilder.fromField(instance, "defaultIcon", heap);
            
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
            super(instance, heap, false);
            
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
    
    private static class JToolBarBuilder extends JComponentBuilder<JToolBar> {
        
        private final boolean paintBorder;
        private final InsetsBuilder margin;
        private final boolean floatable;
        private final int orientation;
        
        JToolBarBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            
            margin = InsetsBuilder.fromField(instance, "margin", heap);
            
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
        
        BoxBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected Box createInstanceImpl() {
            return new Box(BoxLayout.X_AXIS) {
                public void layout() {}
                public void setLayout(LayoutManager l) {}
            };
        }
        
    }
    
    private static class BoxFillerBuilder extends JComponentBuilder<Box.Filler> {
        
        BoxFillerBuilder(Instance instance, Heap heap) {
            super(instance, heap);
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
        
        DefaultBoundedRangeModelBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            value = DetailsUtils.getIntFieldValue(instance, "value", 0);
            extent = DetailsUtils.getIntFieldValue(instance, "extent", 0);
            min = DetailsUtils.getIntFieldValue(instance, "min", 0);
            max = DetailsUtils.getIntFieldValue(instance, "max", 100);
        }
        
        static DefaultBoundedRangeModelBuilder fromField(Instance instance, String field, Heap heap) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, DefaultBoundedRangeModel.class.getName())) return null;
            return new DefaultBoundedRangeModelBuilder((Instance)model, heap);
        }
        
        protected DefaultBoundedRangeModel createInstanceImpl() {
            return new DefaultBoundedRangeModel(value, extent, min, max);
        }
        
    }
    
    private static class JScrollBarBuilder extends JComponentBuilder<JScrollBar> {
        
        private final DefaultBoundedRangeModelBuilder model;
        private final int orientation;
        
        JScrollBarBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
            
            model = DefaultBoundedRangeModelBuilder.fromField(instance, "model", heap);
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JScrollBar.VERTICAL);
        }
        
        static JScrollBarBuilder fromField(Instance instance, String field, Heap heap) {
            Object insets = instance.getValueOfField(field);
            if (!(insets instanceof Instance)) return null;
            return new JScrollBarBuilder((Instance)insets, heap);
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
        
        JSeparatorBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
            
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JSeparator.HORIZONTAL);
        }
        
        protected JSeparator createInstanceImpl() {
            return new JSeparator(orientation);
        }
        
    }
    
    private static class JToolBarSeparatorBuilder extends JSeparatorBuilder {
        
        private final DimensionBuilder separatorSize;
        
        JToolBarSeparatorBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
            separatorSize = DimensionBuilder.fromField(instance, "separatorSize", heap);
        }
        
        protected JSeparator createInstanceImpl() {
            return separatorSize == null ? new JToolBar.Separator() :
                    new JToolBar.Separator(separatorSize.createInstance());
        }
        
    }
    
    private static class JPopupMenuSeparatorBuilder extends JSeparatorBuilder {
        
        JPopupMenuSeparatorBuilder(Instance instance, Heap heap) {
            super(instance, heap);
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
        
        JProgressBarBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
            
            orientation = DetailsUtils.getIntFieldValue(instance, "orientation", JProgressBar.HORIZONTAL);
            paintBorder = DetailsUtils.getBooleanFieldValue(instance, "paintBorder", true);
            model = DefaultBoundedRangeModelBuilder.fromField(instance, "model", heap);
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
        
        JSliderBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
            
            paintTicks = DetailsUtils.getBooleanFieldValue(instance, "paintTicks", false);
            paintTrack = DetailsUtils.getBooleanFieldValue(instance, "paintTrack", true);
            paintLabels = DetailsUtils.getBooleanFieldValue(instance, "paintLabels", false);
            isInverted = DetailsUtils.getBooleanFieldValue(instance, "isInverted", false);
            sliderModel = DefaultBoundedRangeModelBuilder.fromField(instance, "sliderModel", heap);
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
    
    private static class JListBuilder extends JComponentBuilder<JList> {
        
        JListBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
        }
        
        protected JList createInstanceImpl() {
            return new JList();
        }
        
    }
    
    private static class JTreeBuilder extends JComponentBuilder<JTree> {
        
        private final boolean editable;
        
        JTreeBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
            
            editable = DetailsUtils.getBooleanFieldValue(instance, "editable", false);
        }
        
        protected void setupInstance(JTree instance) {
            super.setupInstance(instance);
            
            instance.setEditable(editable);
        }
        
        protected JTree createInstanceImpl() {
            return new JTree(new Object[0]);
        }
        
    }
    
    private static class JTableBuilder extends JComponentBuilder<JTable> {
        
        JTableBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
        }
        
        protected JTable createInstanceImpl() {
            return new JTable();
        }
        
    }
    
    private static class JTableHeaderBuilder extends JComponentBuilder<JTableHeader> {
        
        JTableHeaderBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
        }
        
        protected JTableHeader createInstanceImpl() {
            return new JTableHeader();
        }
        
    }
    
    private static class JViewportBuilder extends JComponentBuilder<JViewport> {
        
        JViewportBuilder(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected JViewport createInstanceImpl() {
            return new JViewport();
        }
        
    }
    
    private static class JScrollPaneBuilder extends JComponentBuilder<JScrollPane> {
        
        private final BorderBuilder viewportBorder;
        
        JScrollPaneBuilder(Instance instance, Heap heap) {
            super(instance, heap, true);
            
            viewportBorder = BorderBuilders.fromField(instance, "viewportBorder", false, heap);

        }
        
        protected void setupInstance(JScrollPane instance) {
            super.setupInstance(instance);
            
            if (viewportBorder != null) {
                Border b = viewportBorder.createInstance();
                if (b != null) instance.setViewportBorder(b);
            }
        }
        
        protected JScrollPane createInstanceImpl() {
            return new JScrollPane();
        }
        
    }
    
    private static class JSpinnerBuilder extends JComponentBuilder<JSpinner> {
        
        JSpinnerBuilder(Instance instance, Heap heap) {
            super(instance, heap, false);
        }
        
        protected JSpinner createInstanceImpl() {
            return new JSpinner();
        }
        
    }
    
    private static class JPopupMenuBuilder extends JComponentBuilder<JPopupMenu> {
        
        private final String label;
        private final boolean paintBorder;
        
        JPopupMenuBuilder(Instance instance, Heap heap) {
            super(instance, heap);
            
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
