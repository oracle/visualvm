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

import org.graalvm.visualvm.lib.ui.components.HTMLLabel;
import java.awt.*;
import java.net.URL;
import javax.swing.*;


/** Enhanced Table cell rendered that paints text labels using provided text alignment
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public class HTMLLabelTableCellRenderer extends EnhancedTableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected HTMLLabel label;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a default table cell renderer with LEADING horizontal alignment showing border when focused. */
    public HTMLLabelTableCellRenderer() {
        this(JLabel.LEADING);
    }

    public HTMLLabelTableCellRenderer(int horizontalAlignment) {
        this(horizontalAlignment, false);
    }

    public HTMLLabelTableCellRenderer(int horizontalAlignment, final boolean persistent) {
        setHorizontalAlignment(horizontalAlignment);
        label = new HTMLLabel() {
                protected void showURL(URL url) {
                    HTMLLabelTableCellRenderer.this.handleLink(url);
                }

                public void setCursor(Cursor cursor) {
                    if (persistent) {
                        super.setCursor(cursor);
                    } else {
                        HTMLLabelTableCellRenderer.this.handleCursor(cursor);
                    }
                }
            };

        setLayout(new BorderLayout());
        add(label,
            ((horizontalAlignment == JLabel.LEADING) || (horizontalAlignment == JLabel.LEFT)) ? BorderLayout.WEST
                                                                                              : BorderLayout.EAST);

        //    setBorder (BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return new HTMLLabelTableCellRenderer(getHorizontalAlignment(), true).getTableCellRendererComponent(table, value,
                                                                                                            isSelected, hasFocus,
                                                                                                            row, column);
    }

    protected void setRowBackground(Color c) {
        super.setRowBackground(c);
        label.setBackground(c);
    }

    protected void setValue(JTable table, Object value, int row, int column) {
        if (table != null) {
            setFont(table.getFont());
        }

        label.setText((value == null) ? "" : value.toString()); //NOI18N
    }

    protected void handleCursor(Cursor cursor) {
        // override to react to setCursor
    }

    protected void handleLink(URL url) {
        // override to react to URL clicks
    }
}
