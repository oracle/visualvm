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

package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.*;
import javax.swing.*;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.table.EnhancedTableCellRenderer;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNode;


/**
 *
 * @author Jiri Sedlacek
 */
final class FieldTableCellRenderer extends EnhancedTableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final JLabel label1;
    private final JLabel label2;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public FieldTableCellRenderer() {
        setHorizontalAlignment(JLabel.TRAILING);
        
        label1 = new JLabel(""); //NOI18N
        label2 = new JLabel(""); //NOI18N
        
        setLayout(null);
        add(label1);
        add(label2);

        setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }
    
    public Dimension getPreferredSize() {
        Dimension d1 = label1.getPreferredSize();
        Dimension d2 = label2.getPreferredSize();
        d1.width += d2.width;
        d1.height = Math.max(d1.height, d2.height);
        Insets i = getInsets();
        d1.width += i.left + i.right;
        d1.height += i.top + i.bottom;
        return d1;
    }
    
    public Dimension getMaximumSize() {
	return getPreferredSize();
    }

    public Dimension getMinimumSize() {
	return getPreferredSize();
    }
    
    public void doLayout() {
        Insets i = getInsets();
        int w = getWidth() - i.left - i.right;
        int h = getHeight() - i.top - i.bottom;
        Dimension d1 = label1.getPreferredSize();
        Dimension d2 = label2.getPreferredSize();
        if (d1.width >= w) { // d1 partially displayed, d2 not displayed
            label1.setBounds(i.left, i.top, w, h);
            label2.setBounds(0, 0, 0, 0);
        } else if (d1.width + d2.width <= w) { // d1 and d2 fully displayed
            label1.setBounds(getWidth() - i.right - d1.width - d2.width, i.top, d1.width, h);
            label2.setBounds(getWidth() - i.right - d2.width, i.top, d2.width, h);
        } else { // d1 fully displayed, d2 partially displayed
            label1.setBounds(i.left, i.top, d1.width, h);
            label2.setBounds(i.left + d1.width, i.top, w - d1.width, h);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return new FieldTableCellRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    protected void setRowForeground(Color c) {
        super.setRowForeground(c);
        label1.setForeground(c);
        label2.setForeground(UIUtils.getDisabledForeground(c));
    }

    protected void setValue(JTable table, Object value, int row, int column) {
        if (value instanceof HeapWalkerNode) {
            HeapWalkerNode node = (HeapWalkerNode)value;
            label1.setText(node.getValue());
            String details = node.getDetails();
            label2.setText(details == null || details.isEmpty() ? "" : "  " + details); //NOI18N
        } else {
            label1.setText(""); //NOI18N
            label2.setText(""); //NOI18N
        }
    }
}
