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


/** Enhanced Table cell rendered that paints text labels using provided text alignment
 *
 * @author Ian Formanek
 */
public class LabelTableCellRenderer extends EnhancedTableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected JLabel label;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a default table cell renderer with LEADING horizontal alignment showing border when focused. */
    public LabelTableCellRenderer() {
        this(JLabel.LEADING);
    }

    public LabelTableCellRenderer(int horizontalAlignment) {
        setHorizontalAlignment(horizontalAlignment);
        label = new JLabel("", horizontalAlignment); //NOI18N

        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return new LabelTableCellRenderer(label.getHorizontalAlignment()).getTableCellRendererComponent(table, value, isSelected,
                                                                                                        hasFocus, row, column);
    }

    protected void setRowForeground(Color c) {
        super.setRowForeground(c);
        label.setForeground(c);
    }

    protected void setValue(JTable table, Object value, int row, int column) {
        if (table != null) {
            setFont(table.getFont());
        }

        label.setText((value == null) ? "" : value.toString()); //NOI18N
    }
}
