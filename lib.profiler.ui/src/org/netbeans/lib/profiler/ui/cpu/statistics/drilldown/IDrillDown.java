/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.cpu.statistics.drilldown;

import org.netbeans.lib.profiler.results.cpu.marking.Mark;
import java.util.List;


/**
 * This interface marks a generic drill-down functionality provider
 * @author Jaroslav Bachorik
 */
public interface IDrillDown {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Provides info whether the given mark is the current one
     */
    public boolean isCurrent(final Mark mark);

    /**
     * Indicates that the drilldown is parked in a special "self" category
     */
    public boolean isInSelf();

    /**
     * Returns the information whether the drilldown manager is validly setup
     */
    public boolean isValid();

    /**
     *Returns the current active category
     * @return Returns the current active category
     */
    Mark getCurrentMark();

    /**
     * Returns the accumulated time for the current category
     * @return Returns the accumulated time for the current category
     */
    long getCurrentTime(boolean net);

    /**
     * Returns the representation of the drill-down filter
     * @return Returns the drill-down path
     */
    List getDrillDownPath();

    /**
     * Time getter for category
     * @param category The category to retrieve the time for
     * @return Returns the accumulated time per category
     */
    long getMarkTime(Mark category, boolean net);

    /**
     * Returns the current list of subcategories
     * @return Returns the list of the current category's subcategories
     */
    List getSubmarks();

    /**
     * Returns the top level category
     */
    Mark getTopMark();

    /**
     * Returns the accumulated time for the top category
     * @return Returns the accumulated time for the top category
     */
    long getTopTime(boolean net);

    void addListener(DrillDownListener listener);

    /**
     * Indicates whehter the given mark can be used for drilldown
     */
    boolean canDrilldown(Mark mark);

    /**
     * Performs drill-down using the specified category
     * @param category The category to use for drill down
     */
    void drilldown(Mark mark);

    /**
     * Drills-up one level
     */
    void drillup();

    /**
     * Drills-up to the specified category
     */
    void drillup(Mark mark);

    void removeListener(DrillDownListener listener);

    void reset();
}
