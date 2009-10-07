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

package org.netbeans.lib.profiler.ui.charts;

import java.util.Iterator;
import java.util.Vector;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractBarChartModel implements BarChartModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Vector listeners; // Data change listeners

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of AbstractBarChartModel */
    public AbstractBarChartModel() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract String getXAxisDesc();

    // --- Abstract BarChartModel ------------------------------------------------
    public abstract String[] getXLabels();

    public abstract String getYAxisDesc();

    public abstract int[] getYValues();

    // ---------------------------------------------------------------------------

    // --- Listeners -------------------------------------------------------------

    /**
     * Adds new ChartModel listener.
     * @param listener ChartModel listener to add
     */
    public synchronized void addChartModelListener(ChartModelListener listener) {
        if (listeners == null) {
            listeners = new Vector();
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes ChartModel listener.
     * @param listener ChartModel listener to remove
     */
    public synchronized void removeChartModelListener(ChartModelListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies all listeners about the data change.
     */
    protected void fireChartDataChanged() {
        if (listeners == null) {
            return;
        }

        Vector toNotify;

        synchronized (this) {
            toNotify = ((Vector) listeners.clone());
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((ChartModelListener) iterator.next()).chartDataChanged();
        }
    }

    // ---------------------------------------------------------------------------
}
