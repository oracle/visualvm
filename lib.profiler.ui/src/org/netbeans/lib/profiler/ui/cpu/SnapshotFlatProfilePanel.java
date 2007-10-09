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

package org.netbeans.lib.profiler.ui.cpu;

import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import java.awt.image.BufferedImage;


/**
 * A display containing a flat profile (always appears together with CCT)
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class SnapshotFlatProfilePanel extends FlatProfilePanel implements ScreenshotProvider {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CPUResultsSnapshot snapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotFlatProfilePanel(CPUResUserActionsHandler actionsHandler) {
        this(actionsHandler, null);
    }

    public SnapshotFlatProfilePanel(CPUResUserActionsHandler actionsHandler, CPUSelectionHandler selectionHandler) {
        super(actionsHandler, selectionHandler);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if (resTable == null) {
            return null;
        }

        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(jScrollPane);
        } else {
            return UIUtils.createScreenshot(resTable);
        }
    }

    /**
     * This method is supposed to be used for displaying data obtained in a snapshot. threadId is either an actual
     * threadId >= 0, or -1 to mean "display data for all threads". When data is initialized in this way, all operations
     * such as switching views (aggregation level), obtaining reverse call graph, going to method's source, are allowed.
     */
    public void setDataToDisplay(CPUResultsSnapshot snapshot, int threadId, int view) {
        this.snapshot = snapshot;
        this.currentView = view;
        this.threadId = threadId;
        this.flatProfileContainer = null;
        collectingTwoTimeStamps = snapshot.isCollectingTwoTimeStamps();
    }

    public String getDefaultViewName() {
        return "cpu-hotspots";
    }

    public CPUResultsSnapshot getSnapshot() {
        return snapshot;
    }

    public boolean fitsVisibleArea() {
        return !jScrollPane.getVerticalScrollBar().isEnabled();
    }

    protected String[] getMethodClassNameAndSig(int methodId, int currentView) {
        return snapshot.getMethodClassNameAndSig(methodId, currentView);
    }

    protected void obtainResults() {
        if (snapshot != null) {
            flatProfileContainer = snapshot.getFlatProfile(threadId, currentView);
        }

        // If a now-inapplicable setting remained from previous run, reset it
        if ((snapshot == null) || (!collectingTwoTimeStamps && (sortBy == FlatProfileContainer.SORT_BY_SECONDARY_TIME))) {
            sortBy = FlatProfileContainer.SORT_BY_TIME;
        }

        // Reinit bar max value here - operations necessary for correct bar representation of results
        flatProfileContainer.filterOriginalData(FilterComponent.getFilterStrings(filterString), filterType, valueFilterValue);
        flatProfileContainer.sortBy(sortBy, sortOrder); // This will actually create the below-used percent()
                                                        // thing for proper timer

        setResultsAvailable(true);
    }

    protected void showReverseCallGraph(int threadId, int methodId, int currentView, int sortingColumn, boolean sortingOrder) {
        actionsHandler.showReverseCallGraph(snapshot, threadId, methodId, currentView, sortingColumn, sortingOrder);
    }

    protected boolean supportsReverseCallGraph() {
        return true;
    }

    protected boolean supportsSubtreeCallGraph() {
        return false;
    }

    protected void updateResults() {
        if (threadId < -1) {
            return; // -1 is reserved for all threads merged flat profile; non-negative numbers are actual thread ids
        }

        int currentColumnCount = collectingTwoTimeStamps ? 5 : 4;

        if (columnCount != currentColumnCount) {
            initColumnsData();
        } else {
            if (resTable != null) {
                saveColumnsData();
            }
        }

        flatProfileContainer.sortBy(sortBy, sortOrder);

        jScrollPane.setViewportView(resTable);
        jScrollPane.getViewport().setBackground(resTable.getBackground());
    }
}
