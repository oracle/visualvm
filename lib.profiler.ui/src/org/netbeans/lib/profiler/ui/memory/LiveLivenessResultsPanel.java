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

package org.netbeans.lib.profiler.ui.memory;

import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.instrumentation.InstrumentationException;
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.ui.LiveResultsPanel;
import org.netbeans.lib.profiler.ui.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.results.memory.ClassHistoryDataManager;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.ProfilerDialogs;


/**
 * This class implements presentation frames for Object Liveness Profiling.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class LiveLivenessResultsPanel extends LivenessResultsPanel implements LiveResultsPanel, ActionListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String GO_SOURCE_POPUP_ITEM_NAME = messages.getString("LiveLivenessResultsPanel_GoSourcePopupItemName"); // NOI18N
    private static final String SHOW_STACK_TRACES_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_LiveShowStackTracesPopupItemName"); // NOI18N
    private static final String STOP_CLASS_POPUP_ITEM_NAME = messages.getString("LiveLivenessResultsPanel_StopClassPopupItemName"); // NOI18N
    private static final String STOP_BELOW_LINE_POPUP_ITEM_NAME = messages.getString("LiveLivenessResultsPanel_StopBelowLinePopupItemName"); // NOI18N
    private static final String STOP_CLASS_SPEC_POPUP_ITEM_NAME = messages.getString("LiveLivenessResultsPanel_StopClassSpecPopupItemName"); // NOI18N
    private static final String STOP_BELOW_LINE_SPEC_POPUP_ITEM_NAME = messages.getString("LiveLivenessResultsPanel_StopBelowLineSpecPopupItemName"); // NOI18N
    private static final String LOG_CLASS_HISTORY = messages.getString("LiveResultsPanel_LogClassHistory"); // NOI18N
                                                                                                            // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected TargetAppRunner runner;

    private ClassHistoryActionsHandler historyActionsHandler;
    private ClassHistoryDataManager classHistoryManager;
    private JMenuItem popupRemoveProfForClass;
    private JMenuItem popupRemoveProfForClassesBelow;
    private JMenuItem popupShowSource;
    private JMenuItem popupShowStacks;
    private JMenuItem startHisto;
    private JPopupMenu popup;
    private ProfilingSessionStatus status;
    private boolean updateResultsInProgress = false;
    private boolean updateResultsPending = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LiveLivenessResultsPanel(TargetAppRunner runner,
                                    MemoryResUserActionsHandler actionsHandler,
                                    ClassHistoryActionsHandler historyActionsHandler,
                                    ClassHistoryDataManager classHistoryManager) {
        super(actionsHandler);
        this.runner = runner;
        this.status = runner.getProfilerClient().getStatus();
        this.historyActionsHandler = historyActionsHandler;
        this.classHistoryManager = classHistoryManager;
        initColumnsData();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(jScrollPane);
        } else {
            return UIUtils.createScreenshot(resTable);
        }
    }

    public String getViewName() {
        return "memory-liveness-live"; // NOI18N
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == popupRemoveProfForClass) {
            new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    MemoryCCTProvider olcgb = runner.getProfilerClient().getMemoryCCTProvider();
                    boolean[] newlyUnprofiledClasses = new boolean[sortedClassIds.length];
                    int line = ((Integer) filteredToFullIndexes.get(clickedLine)).intValue();

                    if (!olcgb.classMarkedUnprofiled(sortedClassIds[line])) {
                        olcgb.markClassUnprofiled(sortedClassIds[line]);
                        newlyUnprofiledClasses[sortedClassIds[line]] = true;

                        if (line < nTrackedAllocObjects.length) { // The following arrays may actually be smaller
                            nTrackedAllocObjects[line] = 0;
                            nTrackedLiveObjects[line] = 0;
                            trackedLiveObjectsSize[line] = 0;
                            avgObjectAge[line] = 0;
                            maxSurvGen[line] = 0;
                        }

                        nTotalAllocObjects[line] = 0;

                        deinstrumentMemoryProfiledClasses(newlyUnprofiledClasses);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    prepareResults();
                }
            }.execute();
        } else if (source == popupRemoveProfForClassesBelow) {
            new SwingWorker() {

                @Override
                protected Object doInBackground() throws Exception {
                    int line = clickedLine;
                    MemoryCCTProvider olcgb = runner.getProfilerClient().getMemoryCCTProvider();
                    boolean[] newlyUnprofiledClasses = new boolean[sortedClassIds.length];
                    int nClasses = filteredToFullIndexes.size();

                    for (int i = line + 1; i < nClasses; i++) {
                        int index = ((Integer) filteredToFullIndexes.get(i)).intValue();

                        if (!olcgb.classMarkedUnprofiled(sortedClassIds[index])) {
                            olcgb.markClassUnprofiled(sortedClassIds[index]);
                            newlyUnprofiledClasses[sortedClassIds[index]] = true;
                            nTrackedAllocObjects[index] = 0;
                            nTrackedLiveObjects[index] = 0;
                            trackedLiveObjectsSize[index] = 0;
                            avgObjectAge[index] = 0;
                            maxSurvGen[index] = 0;
                            nTotalAllocObjects[index] = 0;
                        }
                    }
                    deinstrumentMemoryProfiledClasses(newlyUnprofiledClasses);
                    
                    return null;
                }

                @Override
                protected void done() {
                    prepareResults();
                }
            }.execute();
        } else if (source == popupShowSource && popupShowSource != null) {
            showSourceForClass(selectedClassId);
        } else if (source == popupShowStacks) {
            actionsHandler.showStacksForClass(selectedClassId, getSortingColumn(), getSortingOrder());
        } else if (e.getSource() == startHisto) {
            String selectedClassName = StringUtils.userFormClassName(
                                                getClassName(selectedClassId));
            historyActionsHandler.showClassHistory(selectedClassId, selectedClassName);
        }
    }

    public void fetchResultsFromTargetApp() throws ClientUtils.TargetAppOrVMTerminated {
        MemoryCCTProvider olcgb = runner.getProfilerClient().getMemoryCCTProvider();

        if (olcgb == null) {
            throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
        } else {
            MemoryCCTProvider.ObjectNumbersContainer onc = olcgb.getLivenessObjectNumbers();
            nTrackedAllocObjects = onc.nTrackedAllocObjects;
            nTrackedLiveObjects = onc.nTrackedLiveObjects;
            trackedLiveObjectsSize = onc.trackedLiveObjectsSize;
            avgObjectAge = onc.avgObjectAge;
            maxSurvGen = onc.maxSurvGen;
            nInstrClasses = onc.nInstrClasses;

            if (((nTrackedLiveObjects == null) && (nTrackedAllocObjects == null)) || (avgObjectAge == null)
                    || (maxSurvGen == null)) {
                return;
            }

            // This returns the array containing the total number of allocated objects for each class.
            nTotalAllocObjects = runner.getProfilerClient().getAllocatedObjectsCountResults();

            // Below is a bit of "defensive programming". Normally the sizes of arrays here should be same
            // except for nTotalAllocObjects, that is returned from the server, and may be shorter if some
            // instrumented classes have not propagated to the server yet.
            nTrackedItems = Math.min(nTrackedAllocObjects.length, nTrackedLiveObjects.length);
            nTrackedItems = Math.min(nTrackedItems, trackedLiveObjectsSize.length);
            nTrackedItems = Math.min(nTrackedItems, avgObjectAge.length);
            nTrackedItems = Math.min(nTrackedItems, maxSurvGen.length);
            nTrackedItems = Math.min(nTrackedItems, nInstrClasses);
            nTrackedItems = Math.min(nTrackedItems, nTotalAllocObjects.length);

            // Now if some classes are unprofiled, reflect that in nTotalAllocObjects
            //for (int i = 0; i < nTrackedAllocObjects.length; i++) {
            for (int i = 0; i < nTrackedItems; i++) {
                if (nTrackedAllocObjects[i] == -1) {
                    nTotalAllocObjects[i] = 0;
                }
            }

            // Operations necessary for correct bar representation of results
            maxValue = 0;
            nTotalTrackedBytes = 0;
            nTotalTracked = 0;

            //for (int i = 0; i < trackedLiveObjectsSize.length; i++) {
            for (int i = 0; i < nTrackedItems; i++) {
                if (maxValue < trackedLiveObjectsSize[i]) {
                    maxValue = trackedLiveObjectsSize[i];
                }

                nTotalTrackedBytes += trackedLiveObjectsSize[i];
                nTotalTracked += nTrackedLiveObjects[i];
            }

            if (classHistoryManager.isTrackingClass())
                classHistoryManager.processData(nTotalAllocObjects,
                                                nTrackedLiveObjects,
                                                trackedLiveObjectsSize);

//            if (handler != null) {
//                handler.performAction("history update", new Object[] { nTrackedLiveObjects, nTotalAllocObjects }); // NOI18N
//            }

            initDataUponResultsFetch();
        }
    }

    public boolean fitsVisibleArea() {
        return !jScrollPane.getVerticalScrollBar().isEnabled();
    }

    public void handleRemove() {
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
        // Profiling session will finish and context menu is opened, this would block last live results update -> menu will be closed
        if ((popup != null) && popup.isVisible()) {
            updateResultsPending = false; // clear the flag, updateLiveResults() will be called explicitely from outside
            popup.setVisible(false); // close the context menu
        }
    }

    // --- Save current View action support --------------------------------------
    public boolean hasView() {
        return resTable != null;
    }

    public boolean supports(int instrumentationType) {
        return instrumentationType == CommonConstants.INSTR_OBJECT_LIVENESS;
    }

    public void updateLiveResults() {
        if ((popup != null) && popup.isVisible()) {
            updateResultsPending = true;

            return;
        }

        if (updateResultsInProgress == true) {
            return;
        }

        updateResultsInProgress = true;

        String selectedRowString = null;

        if (resTable != null) {
            int selectedRowIndex = resTable.getSelectedRow();

            if (selectedRowIndex >= resTable.getRowCount()) {
                selectedRowIndex = -1;
                resTable.clearSelection();
            }

            if (selectedRowIndex != -1) {
                selectedRowString = resTable.getValueAt(selectedRowIndex, 0).toString();
            }
        }

        try {
            if (runner.getProfilingSessionStatus().targetAppRunning) {
                reset();
                fetchResultsFromTargetApp();
            }

            prepareResults();

            if (selectedRowString != null) {
                resTable.selectRowByContents(selectedRowString, 0, false);
            }

            if ((resTable != null) && resTable.isFocusOwner()) {
                resTable.requestFocusInWindow(); // prevents results table from losing focus
            }
        } catch (ClientUtils.TargetAppOrVMTerminated e) {
            ProfilerDialogs.displayWarning(e.getMessage());
            ProfilerLogger.log(e.getMessage());
        }

        updateResultsInProgress = false;
    }

    protected String getClassName(int classId) {
        return status.getClassNames()[classId];
    }

    protected String[] getClassNames() {
        return status.getClassNames();
    }

    protected int getPercentsTracked() {
        return 100 / runner.getProfilerEngineSettings().getAllocTrackEvery();
    }

    protected JPopupMenu getPopupMenu() {
        if (popup == null) {
            popup = new JPopupMenu();

            popupRemoveProfForClass = new JMenuItem();
            popupRemoveProfForClassesBelow = new JMenuItem();
            popupRemoveProfForClass.setText(STOP_CLASS_POPUP_ITEM_NAME);
            popupRemoveProfForClassesBelow.setText(STOP_BELOW_LINE_POPUP_ITEM_NAME);

            if (GoToSource.isAvailable()) {
                Font boldfont = popup.getFont().deriveFont(Font.BOLD);

                popupShowSource = new JMenuItem();
                popupShowSource.setText(GO_SOURCE_POPUP_ITEM_NAME);
                popupShowSource.setFont(boldfont);

                popup.add(popupShowSource);
                popup.addSeparator();
            }

            if (runner.getProfilerEngineSettings().getAllocStackTraceLimit() != 0) {
                popupShowStacks = new JMenuItem();
                popupShowStacks.setText(SHOW_STACK_TRACES_POPUP_ITEM_NAME);
                popup.add(popupShowStacks);
                popupShowStacks.addActionListener(this);
                popup.addSeparator();
            }

            popup.add(popupRemoveProfForClass);
            popup.add(popupRemoveProfForClassesBelow);

            if (popupShowSource != null) popupShowSource.addActionListener(this);
            popupRemoveProfForClass.addActionListener(this);
            popupRemoveProfForClassesBelow.addActionListener(this);

            popup.addSeparator();
            startHisto = new JMenuItem();
            startHisto.setText(LOG_CLASS_HISTORY);
            popup.add(startHisto);
            startHisto.addActionListener(this);

            popup.addPopupMenuListener(new PopupMenuListener() {
                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (updateResultsPending) {
                                    updateLiveResults();
                                    updateResultsPending = false;
                                }
                            }
                        });
                }
            });
        }
        // Only show these items when target JVM is alive
        boolean jvmAlive = status.targetAppRunning;
        if (popupShowStacks != null) popupShowStacks.setEnabled(jvmAlive);
        popupRemoveProfForClass.setEnabled(jvmAlive);
        popupRemoveProfForClassesBelow.setEnabled(jvmAlive);
        startHisto.setEnabled(jvmAlive);

        return popup;
    }

    /**
     * When the user invokes a popup menu, we need to adjust the name for the "Stop profiling classes below this line"
     * item to include the concrete class name, so that the user knows exactly what they are going to remove.
     */
    protected void adjustFramePopupMenuTextIfNecessary() {
        //String name = StringUtils.userFormClassName(sortedClassNames[clickedLine]);
        String name = sortedClassNames[clickedLine];
        popupRemoveProfForClass.setText(MessageFormat.format(STOP_CLASS_SPEC_POPUP_ITEM_NAME, new Object[] { name }));
        popupRemoveProfForClassesBelow.setText(MessageFormat.format(STOP_BELOW_LINE_SPEC_POPUP_ITEM_NAME, new Object[] { name }));
    }

    protected void performDefaultAction(int classId) {
        showSourceForClass(classId);
    }

    /**
     * Disable memory profiling for classes with ids such that newlyUnprofiledClasses[id] = true
     */
    private void deinstrumentMemoryProfiledClasses(boolean[] newlyUnprofiledClasses) {
        try {
            runner.getProfilerClient().deinstrumentMemoryProfiledClasses(newlyUnprofiledClasses);
        } catch (InstrumentationException ex1) {
            runner.getAppStatusHandler().displayError(ex1.getMessage());
        } catch (ClientUtils.TargetAppOrVMTerminated ex2) {
            runner.getAppStatusHandler().displayWarning(ex2.getMessage());
            ProfilerLogger.log(ex2.getMessage());
        }
    }
}
