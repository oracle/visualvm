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
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.ComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.ComponentBuilders.JComponentBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.ui.Utils.InstanceBuilder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class DataViewBuilders {

    // Make sure subclasses are listed before base class if using isSubclassOf
    static ComponentBuilder getBuilder(Instance instance) {
        if (DetailsUtils.isSubclassOf(instance, JComboBox.class.getName())) {
            return new JComboBoxBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JList.class.getName())) {
            return new JListBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JTree.class.getName())) {
            return new JTreeBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JTable.class.getName())) {
            return new JTableBuilder(instance);
        } else if (DetailsUtils.isSubclassOf(instance, JTableHeader.class.getName())) {
            return new JTableHeaderBuilder(instance);
        }
        return null;
    }
    
    
    private static class JComboBoxBuilder extends JComponentBuilder<JComboBox> {
        
        private final boolean isEditable;
        private final String selectedObject;
        
        JComboBoxBuilder(Instance instance) {
            super(instance, false);
            
            isEditable = DetailsUtils.getBooleanFieldValue(instance, "isEditable", false);
            
            String _selectedObject = null;
            Object _dataModel = instance.getValueOfField("dataModel");
            if (_dataModel instanceof Instance) {
                Instance dataModel = (Instance)_dataModel;
                if (DetailsUtils.isSubclassOf(dataModel, DefaultComboBoxModel.class.getName())) {
                    Object _selected = dataModel.getValueOfField("selectedObject");
                    if (_selected instanceof Instance) {
                        Instance selected = (Instance)_selected;
                        _selectedObject = DetailsUtils.getInstanceString(selected);
                        if (_selectedObject == null)
                            _selectedObject = BrowserUtils.getSimpleType(selected.getJavaClass().getName()) +
                                             " #" + selected.getInstanceNumber();
                    }
                }
            }
            selectedObject = _selectedObject;
        }
        
        protected void setupInstance(JComboBox instance) {
            super.setupInstance(instance);
            
            instance.setEditable(isEditable);
            if (selectedObject != null) instance.addItem(selectedObject);
        }
        
        protected JComboBox createInstanceImpl() {
            return new JComboBox();
        }
        
    }
    
    private static class DefaultListModelBuilder extends InstanceBuilder<DefaultListModel> {
        
        private final List<String> model;
        
        DefaultListModelBuilder(Instance instance) {
            super(instance);
            
            model = new ArrayList();
            
            Object _delegate = instance.getValueOfField("delegate");
            if (_delegate instanceof Instance) {
                Instance delegate = (Instance)_delegate;
                Object _elementData = delegate.getValueOfField("elementData");
                if (_elementData instanceof ObjectArrayInstance) {
                    int size = DetailsUtils.getIntFieldValue(delegate, "elementCount", 0);
                    if (size > 0) { // TODO: should read up to 'size' elements
                        ObjectArrayInstance elementData = (ObjectArrayInstance)_elementData;
                        for (Instance item : elementData.getValues()) {
                            if (item != null) {
                                String ytem = DetailsUtils.getInstanceString(item);
                                if (ytem == null)
                                    ytem = BrowserUtils.getSimpleType(item.getJavaClass().getName()) +
                                           " #" + item.getInstanceNumber();
                                model.add(ytem);
                            }
                        }
                    }
                }
            }
        }
        
        static DefaultListModelBuilder fromField(Instance instance, String field) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, DefaultListModel.class.getName())) return null;
            return new DefaultListModelBuilder((Instance)model);
        }
        
        protected void setupInstance(DefaultListModel instance) {
            super.setupInstance(instance);
            
            for (String item : model) instance.addElement(item);
        }
        
        protected DefaultListModel createInstanceImpl() {
            return new DefaultListModel();
        }
        
    }
    
    private static class JListBuilder extends JComponentBuilder<JList> {
        
        private final DefaultListModelBuilder dataModel;
        
        JListBuilder(Instance instance) {
            super(instance, false);
            
            dataModel = DefaultListModelBuilder.fromField(instance, "dataModel");
        }
        
        protected void setupInstance(JList instance) {
            super.setupInstance(instance);
            
            ListModel model = dataModel == null ? null : dataModel.createInstance();
            if (model != null) instance.setModel(model);
        }
        
        protected JList createInstanceImpl() {
            return new JList();
        }
        
    }
    
    private static class JTreeBuilder extends JComponentBuilder<JTree> {
        
        private final boolean editable;
        
        JTreeBuilder(Instance instance) {
            super(instance, false);
            
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
    
    private static class TableColumnBuilder extends InstanceBuilder<TableColumn> {
        private final int modelIndex;
        private final int width;
        private final String headerValue;
        
        TableColumnBuilder(Instance instance) {
            super(instance);
            
            modelIndex = DetailsUtils.getIntFieldValue(instance, "modelIndex", 0);
            width = DetailsUtils.getIntFieldValue(instance, "width", 75);
            
            String _headerValue = Utils.getFieldString(instance, "headerValue");
            headerValue = _headerValue != null ? _headerValue : "Col " + (modelIndex + 1);
        }
        
        protected void setupInstance(TableColumn instance) {
            super.setupInstance(instance);
            
            instance.setHeaderValue(headerValue);
        }
        
        protected TableColumn createInstanceImpl() {
            return new TableColumn(modelIndex, width, null, null);
        }
    }
    
    private static class TableColumnModelBuilder extends InstanceBuilder<DefaultTableColumnModel> {
        
        private final List<TableColumnBuilder> tableColumns;
        private final int columnMargin;
        
        TableColumnModelBuilder(Instance instance) {
            super(instance);
            
            tableColumns = new ArrayList();
            columnMargin = DetailsUtils.getIntFieldValue(instance, "columnMargin", 1);
            
            Object _columns = instance.getValueOfField("tableColumns");
            if (_columns instanceof Instance) {
                Instance columns = (Instance)_columns;
                Object _elementData = columns.getValueOfField("elementData");
                if (_elementData instanceof ObjectArrayInstance) {
                    int size = DetailsUtils.getIntFieldValue(columns, "elementCount", 0);
                    if (size > 0) { // TODO: should read up to 'size' elements
                        ObjectArrayInstance elementData = (ObjectArrayInstance)_elementData;
                        for (Instance column : elementData.getValues()) {
                            if (column != null)
                                tableColumns.add(new TableColumnBuilder(column));
                        }
                    }
                }
            }
        }
        
        static TableColumnModelBuilder fromField(Instance instance, String field) {
            Object model = instance.getValueOfField(field);
            if (!(model instanceof Instance)) return null;
            if (!DetailsUtils.isSubclassOf((Instance)model, DefaultTableColumnModel.class.getName())) return null;
            return new TableColumnModelBuilder((Instance)model);
        }
        
        protected void setupInstance(DefaultTableColumnModel instance) {
            super.setupInstance(instance);
            
            for (TableColumnBuilder builder : tableColumns)
                instance.addColumn(builder.createInstance());
            instance.setColumnMargin(columnMargin); 
        }
        
        protected DefaultTableColumnModel createInstanceImpl() {
            return new DefaultTableColumnModel();
        }
        
    }
    
    private static class JTableBuilder extends JComponentBuilder<JTable> {
        
        private final TableColumnModelBuilder columnModel;
        
        JTableBuilder(Instance instance) {
            super(instance, false);
            
            columnModel = TableColumnModelBuilder.fromField(instance, "columnModel");
        }
        
        protected JTable createInstanceImpl() {
            return new JTable();
        }
        
        protected Component createPresenterImpl(JTable instance) {
            TableColumnModel _columnModel = columnModel == null ? null : columnModel.createInstance();
            if (_columnModel == null || _columnModel.getColumnCount() == 0) {
                TableColumn column = new TableColumn(0, instance.getWidth());
                column.setHeaderValue("Table");
                _columnModel = new DefaultTableColumnModel();
                _columnModel.addColumn(column);
            }
            instance.setColumnModel(_columnModel);
            instance.setPreferredScrollableViewportSize(instance.getSize());
            
            return new JScrollPane(instance);
        }
        
    }
    
    private static class JTableHeaderBuilder extends JComponentBuilder<JTableHeader> {
        
        private final TableColumnModelBuilder columnModel;
        
        JTableHeaderBuilder(Instance instance) {
            super(instance, false);
            
            columnModel = TableColumnModelBuilder.fromField(instance, "columnModel");
        }
        
        protected void setupInstance(JTableHeader instance) {
            super.setupInstance(instance);
            
            TableColumnModel _columnModel = columnModel == null ? null : columnModel.createInstance();
            if (_columnModel == null || _columnModel.getColumnCount() == 0) {
                TableColumn column = new TableColumn(0, instance.getWidth());
                column.setHeaderValue("Table");
                _columnModel = new DefaultTableColumnModel();
                _columnModel.addColumn(column);
            }
            instance.setColumnModel(_columnModel);
        }
        
        protected JTableHeader createInstanceImpl() {
            return new JTableHeader();
        }
        
    }
    
}
