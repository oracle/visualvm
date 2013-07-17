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

package org.netbeans.lib.profiler.ui.components.table;

import org.netbeans.lib.profiler.ui.UIUtils;
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
