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


/** Custom Table cell renderer that paints a bar based on numerical value within min/max bounds.
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class CustomBarCellRenderer extends EnhancedTableCellRenderer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final Color BAR_FOREGROUND_COLOR = new Color(195, 41, 41);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected double relValue; // relative part of max - min, <0, 1>
    protected long max;
    protected long min;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CustomBarCellRenderer(long min, long max) {
        setMinimum(min);
        setMaximum(max);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setMaximum(long n) {
        max = n;
    }

    public void setMinimum(long n) {
        min = n;
    }

    public void setRelValue(double n) {
        relValue = n;
    }

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return null;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Insets insets = getInsets();
        g.setColor(BAR_FOREGROUND_COLOR);
        g.fillRect(insets.left, insets.top, (int) Math.round(relValue * (getWidth() - insets.right - insets.left)),
                   getHeight() - insets.bottom - insets.top);
    }

    /**
     * Called each time this renderer is to be used to render a specific value on specified row/column.
     * Subclasses need to implement this method to render the value.
     *
     * @param table  the table in which the rendering occurs
     * @param value  the value to be rendered
     * @param row    the row at which the value is located
     * @param column the column at which the value is located
     */
    protected void setValue(JTable table, Object value, int row, int column) {
        if (value instanceof Long) {
            //multiplying by 10 to allow displaying graphs for values < 1
            // - same done for maxi and min values of progress bar, should be ok
            setRelValue(calculateViewValue(((Long) value).longValue()));
        } else if (value instanceof Number) {
            //multiplying by 10 to allow displaying graphs for values < 1
            // - same done for maxi and min values of progress bar, should be ok
            setRelValue(calculateViewValue(((Number) value).doubleValue()));
        } else if (value instanceof String) {
            //multiplying by 10 to allow displaying graphs for values < 1
            // - same done for maxi and min values of progress bar, should be ok
            setRelValue(calculateViewValue(Double.parseDouble((String) value)));
        } else {
            setRelValue(min);
        }
    }

    protected double calculateViewValue(long n) {
        return (double) (n - min) / (double) (max - min);
    }

    protected double calculateViewValue(double n) {
        return (double) (n - min) / (double) (max - min);
    }
}
