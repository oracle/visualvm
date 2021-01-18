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

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import javax.swing.plaf.*;
import javax.swing.event.*;
import javax.swing.*;


/**
 * This list implements the drag and drop functionality.
 */
@SuppressWarnings("serial")
class XTextField extends JPanel
    implements DocumentListener,
               ActionListener {

    private XObject selectedObject;
    private Class expectedClass;
    private Object value;
    protected JTextField textField;

    private static boolean allowNullSelection = false;

    protected final static int COMPATIBLE_VALUE = 1;
    protected final static int CURRENT_VALUE = 2;
    protected final static int NULL_VALUE = 3;

    private JButton button;
    private XMBeanOperations operation;

    //used in XTestFieldEditor
    public XTextField() {
        super(new BorderLayout());
        add(textField = new JTextField(),BorderLayout.CENTER);
        textField.addActionListener(this);
        //
    }

    public XTextField(Object value) {
        this(value,value.toString().length());
    }

    public XTextField(Object value, int colWidth) {
        this(value,value.getClass(),colWidth, true, null, null);
    }

    public XTextField(Object value,
                      Class expectedClass,
                      int colWidth,
                      boolean isCallable,
                      JButton button,
                      XMBeanOperations operation) {
        super(new BorderLayout());
        this.expectedClass = expectedClass;
        this.button = button;
        this.operation = operation;
        add(textField = new JTextField(value.toString(),colWidth),
            BorderLayout.CENTER);
        if(isCallable)
            textField.addActionListener(this);

        boolean fieldEditable = expectedClass == null ? false :
            Utils.isEditableType(expectedClass.getName());
        if (fieldEditable && isCallable) {
            textField.setEditable(true);
        }
        else {
            textField.setEditable(false);
        }
    }

    public static void setNullSelectionAllowed(boolean allowNullSelection) {
        XTextField.allowNullSelection = allowNullSelection;
    }

    public static boolean getNullSelectionAllowed() {
        return allowNullSelection;
    }

    protected void init(Object value, Class expectedClass) {
        this.expectedClass = expectedClass;
        this.value = value;
        boolean fieldEditable = expectedClass == null ? false :
            Utils.isEditableType(expectedClass.getName());
        clearObject();
        if (value != null) {
            textField.setText(value.toString());
        }
        else {
            //null String value for the moment
            textField.setText(""); // NOI18N
        }
        textField.setToolTipText(null);
        if (fieldEditable) {
            if (!textField.isEditable()) {
                textField.setEditable(true);
            }

        }
        else {
            if (textField.isEditable()) {
                textField.setEditable(false);
            }
        }
    }





    private synchronized void setObject(XObject object) {
        clearObject();
        selectedObject = object;
        setSelectedColors();
        textField.setText(object.getText());
        textField.getDocument().addDocumentListener(this);
        paintImmediately(getVisibleRect());
    }

    private synchronized void clearObject() {
        textField.getDocument().removeDocumentListener(this);
        selectedObject = null;
        setDefaultColors();
    }

    private synchronized void setSelectedColors() {
        // fore = textField.getForeground();
        // back = textField.getBackground();

        //textField.setForeground(Color.red);
        // textField.setBackground(Color.yellow);
    }

    private synchronized void setDefaultColors() {
        //  if (fore != null) textField.setForeground(fore);
        // if (back != null)  textField.setBackground(back);
    }

    public void setHorizontalAlignment(int h) {
        textField.setHorizontalAlignment(h);
    }

    //can be overwritten
    protected JMenuItem buildJMenuItem(XObject xobject, int valueType) {
        if (valueType == COMPATIBLE_VALUE) {
            return new JMenuItem(xobject.getText());
        }
        else if (valueType == CURRENT_VALUE) {
            return new JMenuItem("> "+xobject.getText()); // NOI18N
        }
        else if (valueType == NULL_VALUE) {
            return new JMenuItem("null"); // NOI18N
        }
        else {
            return null;
        }
    }

    private JPopupMenu buildEditPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        return menu;
    }


    // ACTIONLISTENER IMPLEMENTATION
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JTextField) {
            if(operation != null)
                operation.performInvokeRequest(button);
        }
    }

    /**
     * This method returns either the user inputted String, or an XObject
     * if one was dropped on the input field.
     */
    public Object getValue() {
        if (selectedObject!=null) {
            if (selectedObject == XObject.NULL_OBJECT) {
                //null case
                return null;
            }
            else {
                return selectedObject;
            }
        }
        else {
            return  textField.getText();
        }
    }

    public void changedUpdate(DocumentEvent e) {
        // the user typed something, so remove references
        // to the object that was dropped.
        clearObject();
    }

    public void removeUpdate(DocumentEvent e) {
        // the user typed something, so remove references
        // to the object that was dropped.
        clearObject();
    }

    public void insertUpdate(DocumentEvent e) {
        // the user typed something, so remove references
        // to the object that was dropped.
        clearObject();
    }

}
