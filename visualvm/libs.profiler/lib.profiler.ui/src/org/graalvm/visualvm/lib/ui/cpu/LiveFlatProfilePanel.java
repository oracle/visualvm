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

package org.graalvm.visualvm.lib.ui.cpu;

import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.ui.LiveResultsPanel;
import org.graalvm.visualvm.lib.ui.cpu.statistics.StatisticalModule;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;


/**
 * A display containing a flat profile (always appears together with CCT)
 *
 * @author Ian Formanek
 */

//public class LiveFlatProfilePanel extends FlatProfilePanel implements LiveResultsPanel {
public class LiveFlatProfilePanel extends JPanel implements LiveResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.cpu.Bundle"); // NOI18N
    private static final String NO_RESULTS_STRING = messages.getString("LiveFlatProfilePanel_NoResultsString"); // NOI18N
                                                                                                                // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private CPUResUserActionsHandler actionsHandler = null;
    private Collection statModules = null;
    private CPUSelectionHandler handler = new CPUSelectionHandler() {
        public void methodSelected(int threadId, int methodId, int view) {
            for (Iterator it = statModules.iterator(); it.hasNext();) {
                ((StatisticalModule) it.next()).setSelectedMethodId(methodId);
            }
        }
    };

    private JPanel noResultsPanel = null;
    private JPanel resultsTable = null;
    private LiveFlatProfileCollectorPanel fpCollectorPanel = null;
    private TargetAppRunner runner;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    //  public LiveFlatProfilePanel(TargetAppRunner runner, CPUResUserActionsHandler actionsHandler, DrillDownContext context, List additionalStats) {
    public LiveFlatProfilePanel(TargetAppRunner runner, CPUResUserActionsHandler actionsHandler, Collection additionalStats, boolean sampling) {
        this.actionsHandler = actionsHandler;
        this.runner = runner;

        statModules = additionalStats;

        //    drilldownContext = context;
        //    drilldownContext.update(); // get the latest context state
        initComponents();

        setupFlatCollector(sampling);
    }
    
    public LiveFlatProfilePanel(TargetAppRunner runner, CPUResUserActionsHandler actionsHandler, boolean sampling) {
        this(runner, actionsHandler, Collections.EMPTY_LIST, sampling);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAdditionalStats(Collection<StatisticalModule> stats) {
        statModules = stats;
    }
    
    public int getSortingColumn() {
        return fpCollectorPanel.getSortingColumn();
    }

    public boolean getSortingOrder() {
        return fpCollectorPanel.getSortingOrder();
    }

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        return fpCollectorPanel.getViewImage(onlyVisibleArea);
    }

    public String getViewName() {
        return fpCollectorPanel.getViewName();
    }

    public boolean fitsVisibleArea() {
        return fpCollectorPanel.fitsVisibleArea();
    }

    public void handleRemove() {
        fpCollectorPanel.handleRemove();
    }

    /**
     * Called when auto refresh is on and profiling session will finish
     * to give the panel chance to do some cleanup before asynchronous
     * call to updateLiveResults() will happen.
     *
     * Currently it closes the context menu if open, which would otherwise
     * block updating the results.
     */
    public void handleShutdown() {
        handleRemove();
        fpCollectorPanel.handleShutdown();
    }

    public boolean hasValidDrillDown() {
        return false;
    }

    // --- Save current View action support --------------------------------------
    public boolean hasView() {
        return fpCollectorPanel.hasView();
    }

    public void reset() {
        fpCollectorPanel.reset();
    }

    public boolean supports(int instrumentationType) {
        return fpCollectorPanel.supports(instrumentationType);
    }

    public void updateLiveResults() {
        fpCollectorPanel.updateLiveResults();
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD, String viewName) {
        fpCollectorPanel.exportData(exportedFileType, eDD, viewName);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);

        noResultsPanel = new JPanel();
        noResultsPanel.setLayout(new BorderLayout());
        noResultsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        final JLabel noResultsLabel = new JLabel(NO_RESULTS_STRING);

        noResultsLabel.setFont(noResultsLabel.getFont().deriveFont(14));
        noResultsLabel.setEnabled(false);
        noResultsPanel.add(noResultsLabel, BorderLayout.NORTH);

        resultsTable = new JPanel(new CardLayout());
        resultsTable.add(noResultsLabel, "NORESULTS"); // NOI18N

        add(resultsTable, BorderLayout.CENTER);
    }

    private void setupFlatCollector(boolean sampling) {
        fpCollectorPanel = new LiveFlatProfileCollectorPanel(runner, actionsHandler, handler, sampling);
        resultsTable.add(fpCollectorPanel, "RESULTS"); // NOI18N
                                                       //    ((CardLayout)resultsTable.getLayout()).show(resultsTable, "NORESULTS");

        ((CardLayout) resultsTable.getLayout()).show(resultsTable, "RESULTS"); // NOI18N
    }
}
