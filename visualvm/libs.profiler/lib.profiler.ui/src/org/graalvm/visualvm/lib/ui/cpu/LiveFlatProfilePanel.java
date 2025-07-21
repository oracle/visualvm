/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.ui.cpu;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import org.graalvm.visualvm.lib.ui.LiveResultsPanel;
import org.graalvm.visualvm.lib.ui.cpu.statistics.StatisticalModule;


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
    private Collection<StatisticalModule> statModules = null;
    private CPUSelectionHandler handler = new CPUSelectionHandler() {
        public void methodSelected(int threadId, int methodId, int view) {
            for (StatisticalModule statModule : statModules) {
                statModule.setSelectedMethodId(methodId);
            }
        }
    };

    private JPanel noResultsPanel = null;
    private JPanel resultsTable = null;
    private LiveFlatProfileCollectorPanel fpCollectorPanel = null;
    private TargetAppRunner runner;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    //  public LiveFlatProfilePanel(TargetAppRunner runner, CPUResUserActionsHandler actionsHandler, DrillDownContext context, List additionalStats) {
    public LiveFlatProfilePanel(TargetAppRunner runner, CPUResUserActionsHandler actionsHandler, Collection<StatisticalModule> additionalStats, boolean sampling) {
        this.actionsHandler = actionsHandler;
        this.runner = runner;

        statModules = additionalStats;

        //    drilldownContext = context;
        //    drilldownContext.update(); // get the latest context state
        initComponents();

        setupFlatCollector(sampling);
    }
    
    public LiveFlatProfilePanel(TargetAppRunner runner, CPUResUserActionsHandler actionsHandler, boolean sampling) {
        this(runner, actionsHandler, Collections.emptyList(), sampling);
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
     * to give the panel chance to do some cleanup before asynchrounous
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
