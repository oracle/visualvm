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

package org.netbeans.lib.profiler.ui;

import java.awt.*;


/** Various UI Constants used in the JFluid UI
 *
 * @author Ian Formanek
 */
public interface UIConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    /** Color used to draw vertical gridlines in JTables */
    public static final Color TABLE_VERTICAL_GRID_COLOR = new Color(214, 223, 247); //Color.LIGHT_GRAY;

    /** if true, results tables display the horizontal grid lines */
    public static final boolean SHOW_TABLE_HORIZONTAL_GRID = false;

    /** if true, results tables display the vertical grid lines */
    public static final boolean SHOW_TABLE_VERTICAL_GRID = true;

    /** Color used for painting selected cell background in JTables */
    public static final Color TABLE_SELECTION_BACKGROUND_COLOR = new Color(193, 210, 238); //(253, 249, 237)

    /** Color used for painting selected cell foreground in JTables */
    public static final Color TABLE_SELECTION_FOREGROUND_COLOR = Color.BLACK;
    public static final int TABLE_ROW_MARGIN = 0;
}
