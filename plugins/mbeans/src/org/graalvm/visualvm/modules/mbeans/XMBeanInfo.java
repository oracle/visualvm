/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.mbeans;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.*;
import javax.management.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import static org.graalvm.visualvm.modules.mbeans.Utilities.*;

@SuppressWarnings("serial")
class XMBeanInfo extends JPanel {
    
    private static final Color lightSalmon = new Color(255, 160, 122);
    private static final Color lightYellow = new Color(255, 255, 128);
    
    private final int NAME_COLUMN = 0;
    private final int VALUE_COLUMN = 1;
    
    private final String[] columnNames = {
        Resources.getText("LBL_Name"), // NOI18N
        Resources.getText("LBL_Value") // NOI18N
    };
    
    private JTable infoTable = new JTable();
    
    private static class ReadOnlyDefaultTableModel extends DefaultTableModel {
        @Override
        public void setValueAt(Object value, int row, int col) {
        }
    }
    
    private static class TableRowDivider {
        
        public String tableRowDividerText;
        public Color tableRowDividerColor;
        
        public TableRowDivider(
                String tableRowDividerText, Color tableRowDividerColor) {
            this.tableRowDividerText = tableRowDividerText;
            this.tableRowDividerColor = tableRowDividerColor;
        }
        
        @Override
        public String toString() {
            return tableRowDividerText;
        }
    }
    
    private static MBeanInfoTableCellRenderer renderer =
            new MBeanInfoTableCellRenderer();
    
    private static class MBeanInfoTableCellRenderer
            extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (value instanceof TableRowDivider) {
                JLabel label = new JLabel(
                        "<html><b>" + value.toString() + "</b></html>"); // NOI18N
                label.setBackground(ensureContrast(
                        ((TableRowDivider) value).tableRowDividerColor,
                        label.getForeground()));
                label.setOpaque(true);
                return label;
            }
            return comp;
        }
    }
    
    private static TableCellEditor editor =
            new MBeanInfoTableCellEditor(new JTextField());
    
    private static class MBeanInfoTableCellEditor
            extends Utils.ReadOnlyTableCellEditor {
        public MBeanInfoTableCellEditor(JTextField tf) {
            super(tf);
        }
        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {
            Component comp = super.getTableCellEditorComponent(
                    table, value, isSelected, row, column);
            if (value instanceof TableRowDivider) {
                JLabel label = new JLabel(
                        "<html><b>" + value.toString() + "</b></html>"); // NOI18N
                label.setBackground(ensureContrast(
                        ((TableRowDivider) value).tableRowDividerColor,
                        label.getForeground()));
                label.setOpaque(true);
                return label;
            }
            return comp;
        }
    }
    
    public XMBeanInfo() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(Resources.getText("LBL_MBeanInfo"))); // NOI18N
        infoTable.setModel(new ReadOnlyDefaultTableModel());
        infoTable.setRowSelectionAllowed(false);
        infoTable.setColumnSelectionAllowed(false);
        infoTable.getTableHeader().setReorderingAllowed(false);
        ((DefaultTableModel) infoTable.getModel()).setColumnIdentifiers(columnNames);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(140);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setMaxWidth(140);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(renderer);
        infoTable.getColumnModel().getColumn(VALUE_COLUMN).setCellRenderer(renderer);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setCellEditor(editor);
        infoTable.getColumnModel().getColumn(VALUE_COLUMN).setCellEditor(editor);
        infoTable.addKeyListener(new Utils.CopyKeyAdapter());
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        JScrollPane infoTableScrollPane = new JScrollPane(infoTable);
        add(infoTableScrollPane);
    }
    
    // Call on EDT
    public void emptyInfoTable() {
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
    }
    
    // Call on EDT
    private void addDescriptor(Descriptor desc, String text) {
        if (desc != null && desc.getFieldNames().length > 0) {
            DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
            Object rowData[] = new Object[2];
            rowData[0] = new TableRowDivider(
                    text + " " + Resources.getText("LBL_Descriptor") + ":", lightYellow); // NOI18N
            rowData[1] = new TableRowDivider("", lightYellow); // NOI18N
            tableModel.addRow(rowData);
            for (String fieldName : desc.getFieldNames()) {
                rowData[0] = fieldName;
                Object fieldValue = desc.getFieldValue(fieldName);
                if (fieldValue instanceof boolean[]) {
                    rowData[1] = Arrays.toString((boolean[]) fieldValue);
                } else if (fieldValue instanceof byte[]) {
                    rowData[1] = Arrays.toString((byte[]) fieldValue);
                } else if (fieldValue instanceof char[]) {
                    rowData[1] = Arrays.toString((char[]) fieldValue);
                } else if (fieldValue instanceof double[]) {
                    rowData[1] = Arrays.toString((double[]) fieldValue);
                } else if (fieldValue instanceof float[]) {
                    rowData[1] = Arrays.toString((float[]) fieldValue);
                } else if (fieldValue instanceof int[]) {
                    rowData[1] = Arrays.toString((int[]) fieldValue);
                } else if (fieldValue instanceof long[]) {
                    rowData[1] = Arrays.toString((long[]) fieldValue);
                } else if (fieldValue instanceof short[]) {
                    rowData[1] = Arrays.toString((short[]) fieldValue);
                } else if (fieldValue instanceof Object[]) {
                    rowData[1] = Arrays.toString((Object[]) fieldValue);
                } else {
                    rowData[1] = fieldValue;
                }
                tableModel.addRow(rowData);
            }
            tableModel.newDataAvailable(new TableModelEvent(tableModel));
        }
    }
    
    // Call on EDT
    private void addMBeanInfo(XMBean mbean, MBeanInfo mbeanInfo) {
        String border = Resources.getText("LBL_MBeanInfo"); // NOI18N
        String text = Resources.getText("LBL_Info"); // NOI18N
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(border, lightSalmon);
        rowData[1] = new TableRowDivider("", lightSalmon); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = new TableRowDivider(text + ":", lightYellow); // NOI18N
        rowData[1] = new TableRowDivider("", lightYellow); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_ObjectName"); // NOI18N
        rowData[1] = mbean.getObjectName();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_ClassName"); // NOI18N
        rowData[1] = mbeanInfo.getClassName();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Description"); // NOI18N
        rowData[1] = mbeanInfo.getDescription();
        tableModel.addRow(rowData);
        addDescriptor(mbeanInfo.getDescriptor(), text);
        // MBeanConstructorInfo
        //
        int i = 0;
        for (MBeanConstructorInfo mbci : mbeanInfo.getConstructors()) {
            addMBeanConstructorInfo(mbci,
                    Resources.getText("LBL_Constructor") + "-" + i); // NOI18N
            // MBeanParameterInfo
            //
            int j = 0;
            for (MBeanParameterInfo mbpi : mbci.getSignature()) {
                addMBeanParameterInfo(mbpi,
                        Resources.getText("LBL_Parameter") + "-" + i + "-" + j); // NOI18N
                j++;
            }
            i++;
        }
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }
    
    // Call on EDT
    private void addMBeanAttributeInfo(MBeanAttributeInfo mbai) {
        String border = Resources.getText("LBL_MBeanAttributeInfo"); // NOI18N
        String text = Resources.getText("LBL_Attribute"); // NOI18N
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(border, lightSalmon);
        rowData[1] = new TableRowDivider("", lightSalmon); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = new TableRowDivider(text + ":", lightYellow); // NOI18N
        rowData[1] = new TableRowDivider("", lightYellow); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Name"); // NOI18N
        rowData[1] = mbai.getName();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Description"); // NOI18N
        rowData[1] = mbai.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Readable"); // NOI18N
        rowData[1] = mbai.isReadable();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Writable"); // NOI18N
        rowData[1] = mbai.isWritable();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Is"); // NOI18N
        rowData[1] = mbai.isIs();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Type"); // NOI18N
        rowData[1] = mbai.getType();
        tableModel.addRow(rowData);
        addDescriptor(mbai.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }
    
    // Call on EDT
    private void addMBeanOperationInfo(MBeanOperationInfo mboi) {
        String border = Resources.getText("LBL_MBeanOperationInfo"); // NOI18N
        String text = Resources.getText("LBL_Operation"); // NOI18N
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(border, lightSalmon);
        rowData[1] = new TableRowDivider("", lightSalmon); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = new TableRowDivider(text + ":", lightYellow); // NOI18N
        rowData[1] = new TableRowDivider("", lightYellow); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Name"); // NOI18N
        rowData[1] = mboi.getName();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Description"); // NOI18N
        rowData[1] = mboi.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Impact"); // NOI18N
        switch (mboi.getImpact()) {
            case MBeanOperationInfo.INFO:
                rowData[1] = Resources.getText("LBL_INFO"); // NOI18N
                break;
            case MBeanOperationInfo.ACTION:
                rowData[1] = Resources.getText("LBL_ACTION"); // NOI18N
                break;
            case MBeanOperationInfo.ACTION_INFO:
                rowData[1] = Resources.getText("LBL_ACTION_INFO"); // NOI18N
                break;
            case MBeanOperationInfo.UNKNOWN:
                rowData[1] = Resources.getText("LBL_UNKNOWN"); // NOI18N
                break;
        }
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_ReturnType"); // NOI18N
        rowData[1] = mboi.getReturnType();
        tableModel.addRow(rowData);
        addDescriptor(mboi.getDescriptor(), text);
        // MBeanParameterInfo
        //
        int i = 0;
        for (MBeanParameterInfo mbpi : mboi.getSignature()) {
            addMBeanParameterInfo(mbpi,
                    Resources.getText("LBL_Parameter") + "-" + i++); // NOI18N
        }
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }
    
    // Call on EDT
    private void addMBeanNotificationInfo(MBeanNotificationInfo mbni) {
        String border = Resources.getText("LBL_MBeanNotificationInfo") + ":"; // NOI18N
        String text = Resources.getText("LBL_Notification"); // NOI18N
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(border, lightSalmon);
        rowData[1] = new TableRowDivider("", lightSalmon); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = new TableRowDivider(text + ":", lightYellow); // NOI18N
        rowData[1] = new TableRowDivider("", lightYellow); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Name"); // NOI18N
        rowData[1] = mbni.getName();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Description"); // NOI18N
        rowData[1] = mbni.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_NotifTypes"); // NOI18N
        rowData[1] = Arrays.toString(mbni.getNotifTypes());
        tableModel.addRow(rowData);
        addDescriptor(mbni.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }
    
    // Call on EDT
    private void addMBeanConstructorInfo(MBeanConstructorInfo mbci, String text) {
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text + ":", lightYellow); // NOI18N
        rowData[1] = new TableRowDivider("", lightYellow); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Name"); // NOI18N
        rowData[1] = mbci.getName();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Description"); // NOI18N
        rowData[1] = mbci.getDescription();
        tableModel.addRow(rowData);
        addDescriptor(mbci.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }
    
    // Call on EDT
    private void addMBeanParameterInfo(MBeanParameterInfo mbpi, String text) {
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text + ":", lightYellow); // NOI18N
        rowData[1] = new TableRowDivider("", lightYellow); // NOI18N
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Name"); // NOI18N
        rowData[1] = mbpi.getName();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Description"); // NOI18N
        rowData[1] = mbpi.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Resources.getText("LBL_Type"); // NOI18N
        rowData[1] = mbpi.getType();
        tableModel.addRow(rowData);
        addDescriptor(mbpi.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }

    // Call on EDT
    public void loadMBeanInfo(XMBean mbean, MBeanInfo mbeanInfo) {
        // MBeanInfo
        //
        addMBeanInfo(mbean, mbeanInfo);
        // MBeanAttributeInfo
        //
        MBeanAttributeInfo[] ai = mbeanInfo.getAttributes();
        if (ai != null && ai.length > 0) {
            for (MBeanAttributeInfo mbai : ai) {
                addMBeanAttributeInfo(mbai);
            }
        }
        // MBeanOperationInfo
        //
        MBeanOperationInfo[] oi = mbeanInfo.getOperations();
        if (oi != null && oi.length > 0) {
            for (MBeanOperationInfo mboi : oi) {
                addMBeanOperationInfo(mboi);
            }
        }
        // MBeanNotificationInfo
        //
        MBeanNotificationInfo[] ni = mbeanInfo.getNotifications();
        if (ni != null && ni.length > 0) {
            for (MBeanNotificationInfo mbni : ni) {
                addMBeanNotificationInfo(mbni);
            }
        }
    }
}
