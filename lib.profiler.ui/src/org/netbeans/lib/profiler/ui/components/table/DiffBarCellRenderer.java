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
 * @author Jiri Sedlacek
 */
public class DiffBarCellRenderer extends CustomBarCellRenderer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final Color BAR_FOREGROUND2_COLOR = new Color(41, 195, 41);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DiffBarCellRenderer(long min, long max) {
        super(min, max);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        Insets insets = getInsets();
        int clientWidth = getWidth() - insets.right - insets.left;
        int horizCenter = insets.left + (clientWidth / 2);
        int barExtent = (int) Math.ceil((Math.abs(relValue) * ((double) clientWidth)) / 2d);

        if (relValue > 0) {
            g.setColor(BAR_FOREGROUND_COLOR);
            g.fillRect(horizCenter, insets.top, barExtent, getHeight() - insets.bottom - insets.top);
        } else if (relValue < 0) {
            g.setColor(BAR_FOREGROUND2_COLOR);
            g.fillRect(horizCenter - barExtent, insets.top, barExtent, getHeight() - insets.bottom - insets.top);
        }
    }

    protected double calculateViewValue(long n) {
        long absMax = Math.max(Math.abs(min), max);

        return (double) (n) / (double) (absMax);
    }

    protected double calculateViewValue(double n) {
        long absMax = Math.max(Math.abs(min), max);

        return (double) (n) / (double) (absMax);
    }
}
