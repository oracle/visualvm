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

package org.graalvm.visualvm.lib.ui.components.table;

import java.awt.*;
import javax.swing.*;


/**
 *
 * @author  Jiri Sedlacek
 */
public class BooleanTableCellRenderer extends EnhancedTableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JCheckBox checkBox;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BooleanTableCellRenderer */
    public BooleanTableCellRenderer() {
        super();
        super.setLayout(new BorderLayout(0, 0));

        checkBox = new JCheckBox();
        checkBox.setHorizontalAlignment(JCheckBox.CENTER);
        checkBox.setOpaque(false);

        add(checkBox, BorderLayout.CENTER);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return null;
    }

    protected void setState(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if ((supportsFocusBorder) && (hasFocus) && (isSelected)) {
            checkBox.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
        } else {
            checkBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }
    }

    protected void setValue(javax.swing.JTable table, Object value, int row, int column) {
        checkBox.setSelected(((value != null) && ((Boolean) value).booleanValue()));
    }
}
