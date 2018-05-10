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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;
import javax.swing.*;

@SuppressWarnings("serial")
class OperationEntry extends JPanel {
    private final static Logger LOGGER = Logger.getLogger(OperationEntry.class.getName());
    
    private MBeanOperationInfo operation;
    private JComboBox sigs;
    private Dimension preferredSize;
    private XTextField inputs[];

    public OperationEntry (MBeanOperationInfo operation,
                           boolean isCallable,
                           JButton button,
                           XMBeanOperations xoperations) {
        super(new BorderLayout());
        this.operation = operation;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setPanel(isCallable, button, xoperations);
    }

    /**
     * This method chops off the throws exceptions, removes "java.lang".
     */
    private String preProcessSignature(String signature) {
        int index;
        if ((index=signature.indexOf(" throws"))>0) { // NOI18N
            signature = signature.substring(0,index);
        }
        while ((index = signature.indexOf("java.lang."))>0) { // NOI18N
            signature = signature.substring(0,index)+
                signature.substring(index+10,signature.length());
        }
        return signature;
    }

    private void setPanel(boolean isCallable,
                          JButton button,
                          XMBeanOperations xoperations) {
        try {
            MBeanParameterInfo params[] = operation.getSignature();
            add(new JLabel("(",JLabel.CENTER)); // NOI18N
            inputs = new XTextField[params.length];
            for (int i = 0; i < params.length; i++) {
                if(params[i].getName() != null) {
                    JLabel name =
                        new JLabel(params[i].getName(), JLabel.CENTER);
                    name.setToolTipText(params[i].getDescription());
                    add(name);
                }

                String defaultTextValue =
                    Utils.getDefaultValue(params[i].getType());
                int fieldWidth = defaultTextValue.length();
                if (fieldWidth > 15) fieldWidth = 15;
                else
                    if (fieldWidth < 10) fieldWidth = 10;

                Class<?> clazz;
                try {
                    clazz = Utils.getClass(params[i].getType());
                } catch (ClassNotFoundException e) {
                    clazz = null;
                }

                add(inputs[i] =
                        new XTextField(Utils.getReadableClassName(defaultTextValue),
                        clazz,
                        fieldWidth,
                        isCallable,
                        button,
                        xoperations));
                inputs[i].setHorizontalAlignment(SwingConstants.CENTER);

                if (i < params.length-1)
                    add(new JLabel(",",JLabel.CENTER)); // NOI18N
            }
            add(new JLabel(")",JLabel.CENTER)); // NOI18N
            validate();
            doLayout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting Operation panel: ", e); // NOI18N
        }
    }

    public String[] getSignature() {
        MBeanParameterInfo params[] = operation.getSignature();
        String result[] = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = params[i].getType();
        }
        return result;
    }

    public Object[] getParameters() throws Exception {
        MBeanParameterInfo params[] = operation.getSignature();
        String signature[] = new String[params.length];
        for (int i = 0; i < params.length; i++)
        signature[i] = params[i].getType();
        return Utils.getParameters(inputs,signature);
    }

    public String getReturnType() {
        return operation.getReturnType();
    }
}
