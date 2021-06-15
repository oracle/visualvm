/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.ui.UIUtils;
import java.awt.*;
import javax.swing.*;


/** Enhanced Table cell rendered that paints text labels using provided text alignment
 *
 * @author Ian Formanek
 */
public class MethodNameTableCellRenderer extends EnhancedTableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JLabel label1;
    private JLabel label2;
    private JLabel label3;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a default table cell renderer with LEADING horizontal alignment showing border when focused. */
    public MethodNameTableCellRenderer() {
        label1 = new JLabel(""); //NOI18N
        label2 = new JLabel(""); //NOI18N
        label3 = new JLabel(""); //NOI18N

        label2.setFont(label1.getFont().deriveFont(Font.BOLD));

        setLayout(new BorderLayout());

        JPanel in = new JPanel();
        in.setOpaque(false);
        in.setLayout(new BorderLayout());
        add(label1, BorderLayout.WEST);
        add(in, BorderLayout.CENTER);
        in.add(label2, BorderLayout.WEST);
        in.add(label3, BorderLayout.CENTER);

        setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return new MethodNameTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    protected void setRowForeground(Color c) {
        super.setRowForeground(c);
        label1.setForeground(c);
        label2.setForeground(c);
        label3.setForeground(UIUtils.getDisabledForeground(c));
    }
    
    protected void setValue(JTable table, Object value, int row, int column) {
        if (table != null) {
            setFont(table.getFont());
        }

        if (value != null) {
            String str = value.toString();
            int bracketIndex = str.indexOf('('); //NOI18N
            String text3 = ""; //NOI18N

            if (bracketIndex != -1) {
                text3 = " " + str.substring(bracketIndex); //NOI18N
                str = str.substring(0, bracketIndex);
            }

            int dotIndex = str.lastIndexOf('.'); //NOI18N
            label1.setText(str.substring(0, dotIndex + 1));
            label2.setText(str.substring(dotIndex + 1));
            label3.setText(text3);
        } else {
            label1.setText(""); //NOI18N
            label2.setText(""); //NOI18N
            label3.setText(""); //NOI18N
        }
    }
}
