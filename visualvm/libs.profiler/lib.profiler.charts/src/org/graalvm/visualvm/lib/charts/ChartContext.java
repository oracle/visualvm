/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.charts;

import org.netbeans.lib.profiler.charts.swing.LongRect;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ChartContext {

    // --- Chart orientation ---------------------------------------------------

    public boolean isRightBased();

    public boolean isBottomBased();


    // --- Fixed scale ---------------------------------------------------------

    public boolean fitsWidth();

    public boolean fitsHeight();


    // --- Chart bounds --------------------------------------------------------

    public long getDataOffsetX();

    public long getDataOffsetY();

    public long getDataWidth();

    public long getDataHeight();

    public long getViewWidth();

    public long getViewHeight();


    // --- Viewport bounds -----------------------------------------------------

    public long getViewportOffsetX();

    public long getViewportOffsetY();

    public int getViewportWidth();

    public int getViewportHeight();


    // --- Data to View --------------------------------------------------------

    public double getViewX(double dataX);

    public double getReversedViewX(double dataX);

    public double getViewY(double dataY);

    public double getReversedViewY(double dataY);

    public double getViewWidth(double dataWidth);

    public double getViewHeight(double dataHeight);

    public LongRect getViewRect(LongRect dataRect);

//    public LongRect getReversedViewRect(LongRect dataRect);


    // --- View to Data --------------------------------------------------------

    public double getDataX(double viewX);

    public double getReversedDataX(double viewX);

    public double getDataY(double viewY);

    public double getReversedDataY(double viewY);

    public double getDataWidth(double viewWidth);

    public double getDataHeight(double viewHeight);

//    public LongRect getDataRect(LongRect viewRect);
//
//    public LongRect getReversedDataRect(LongRect viewRect);

}
