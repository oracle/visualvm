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

import java.awt.*;
import javax.swing.*;
import org.graalvm.visualvm.lib.ui.UIUtils;


/** Enhanced Table cell rendered that paints text labels using provided text alignment
 *
 * @author Ian Formanek
 */
public class LabelBracketTableCellRenderer extends EnhancedTableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JLabel label1;
    private JLabel label2;
    private int digitsWidth = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a default table cell renderer with LEADING horizontal alignment showing border when focused. */
    public LabelBracketTableCellRenderer() {
        this(JLabel.LEADING);
    }

    public LabelBracketTableCellRenderer(int horizontalAlignment) {
        this(horizontalAlignment, "(99.9%)"); //NOI18N
    }

    public LabelBracketTableCellRenderer(int horizontalAlignment, final String widestBracketText) {
        setHorizontalAlignment(horizontalAlignment);
        label1 = new JLabel("", horizontalAlignment); //NOI18N
        label2 = new JLabel("", horizontalAlignment) { //NOI18N
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();

                    if (digitsWidth == -1) {
                        digitsWidth = getFontMetrics(getFont()).stringWidth(widestBracketText);
                    }

                    if (d.width < digitsWidth) {
                        return new Dimension(digitsWidth, d.height);
                    } else {
                        return d;
                    }
                }
            };

        Font f = label2.getFont();
        label2.setFont(f.deriveFont((float)f.getSize() - 1));

        setLayout(new BorderLayout());

        if (horizontalAlignment == JLabel.LEADING) {
            add(label1, BorderLayout.WEST);
            add(label2, BorderLayout.CENTER);
        } else {
            add(label1, BorderLayout.CENTER);
            add(label2, BorderLayout.EAST);
        }

        setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return new LabelBracketTableCellRenderer(label1.getHorizontalAlignment()).getTableCellRendererComponent(table, value,
                                                                                                                isSelected,
                                                                                                                hasFocus, row,
                                                                                                                column);
    }

    protected void setRowForeground(Color c) {
        super.setRowForeground(c);
        label1.setForeground(c);
        label2.setForeground(UIUtils.getDisabledForeground(c));
    }

    protected void setValue(JTable table, Object value, int row, int column) {
        if (table != null) {
            setFont(table.getFont());
        }

        if (value != null) {
            String str = value.toString();
            int bracketIdx = str.lastIndexOf('('); // NOI18N

            if (bracketIdx != -1) {
                label1.setText(str.substring(0, bracketIdx));
                label2.setText(str.substring(bracketIdx));
            } else {
                label1.setText(str);
                label2.setText(""); // NOI18N
            }
        } else {
            label1.setText(""); // NOI18N
            label2.setText(""); // NOI18N
        }
    }
}
