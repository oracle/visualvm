/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

import org.netbeans.lib.profiler.ui.components.*;
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
