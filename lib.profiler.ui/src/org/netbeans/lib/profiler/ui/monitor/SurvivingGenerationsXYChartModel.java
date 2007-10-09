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

package org.netbeans.lib.profiler.ui.monitor;

import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.ui.charts.AsyncMarksProvider;
import java.awt.Color;
import java.util.ResourceBundle;


/**
 *
 * @author Jiri Sedlacek
 */
public class SurvivingGenerationsXYChartModel extends VMTelemetryXYChartModel implements AsyncMarksProvider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.monitor.Bundle"); // NOI18N
    private static final String SURVGEN_STRING = messages.getString("SurvivingGenerationsXYChartModel_SurvGenString"); // NOI18N
    private static final String TIME_REL_STRING = messages.getString("SurvivingGenerationsXYChartModel_TimeRelString"); // NOI18N
                                                                                                                        // -----
    private static final Color GC_MARKS_COLOR = new Color(250, 230, 230);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SurvivingGenerationsXYChartModel(VMTelemetryDataManager vmTelemetryDataManager) {
        super(vmTelemetryDataManager);
        setupModel(new String[] { SURVGEN_STRING, TIME_REL_STRING },
                   new Color[] { new Color(255, 127, 127), new Color(127, 63, 191) });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Color getMarkColor() {
        return GC_MARKS_COLOR;
    }

    public long getMarkEnd(int index) {
        return vmTelemetryDataManager.gcFinishs[index];
    }

    public long getMarkStart(int index) {
        return vmTelemetryDataManager.gcStarts[index];
    }

    public int getMarksCount() {
        return vmTelemetryDataManager.getGCItemCount();
    }

    // TODO: will be moved to chart axis definition
    public long getMaxDisplayYValue(int seriesIndex) {
        switch (seriesIndex) {
            case 0:
                return getMaxYValue(0);
            case 1:
                return 1000;
        }

        return 0;
    }

    protected long[] getYValues(int seriesIndex) {
        switch (seriesIndex) {
            case 0:
                return vmTelemetryDataManager.nSurvivingGenerations;
            case 1:
                return vmTelemetryDataManager.relativeGCTimeInPerMil;
        }

        return null;
    }
}
