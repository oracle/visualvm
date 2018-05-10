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
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.ui.LiveResultsPanel;
import org.netbeans.lib.profiler.ui.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.memory.ClassHistoryDataManager;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.ProfilerDialogs;


/**
 * This class implements presentation frames for Object Allocation Profiling.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class LiveAllocResultsPanel extends AllocResultsPanel implements LiveResultsPanel, ActionListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String GO_SOURCE_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_GoSourcePopupItemName"); // NOI18N
    private static final String SHOW_STACK_TRACES_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_LiveShowStackTracesPopupItemName"); // NOI18N
    private static final String LOG_CLASS_HISTORY = messages.getString("LiveResultsPanel_LogClassHistory"); // NOI18N
                                                                                                            // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected TargetAppRunner runner;

    private ClassHistoryActionsHandler historyActionsHandler;
    private ClassHistoryDataManager classHistoryManager;
    private JMenuItem popupShowSource;
    private JMenuItem popupShowStacks;
    private JMenuItem startHisto;
    private JPopupMenu memoryResPopupMenu;
    private ProfilingSessionStatus status;
    private boolean updateResultsInProgress = false;
    private boolean updateResultsPending = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LiveAllocResultsPanel(TargetAppRunner runner,
                                 MemoryResUserActionsHandler actionsHandler,
                                 ClassHistoryActionsHandler historyActionsHandler,
                                 ClassHistoryDataManager classHistoryManager) {
        super(actionsHandler);
        this.status = runner.getProfilerClient().getStatus();
        this.runner = runner;
        this.historyActionsHandler = historyActionsHandler;
        this.classHistoryManager = classHistoryManager;
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
        return "memory-allocations-live"; // NOI18N
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == popupShowStacks) {
            actionsHandler.showStacksForClass(selectedClassId, getSortingColumn(), getSortingOrder());
        } else if (e.getSource() == popupShowSource && popupShowSource != null) {
            showSourceForClass(selectedClassId);
        } else if (e.getSource() == startHisto) {
            String selectedClassName = StringUtils.userFormClassName(
                                                getClassName(selectedClassId));
            historyActionsHandler.showClassHistory(selectedClassId, selectedClassName);
                
        }
    }

    public void fetchResultsFromTargetApp() throws ClientUtils.TargetAppOrVMTerminated {
        MemoryCCTProvider oacgb = runner.getProfilerClient().getMemoryCCTProvider();

        if (oacgb == null) {
            throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
        } else {
            totalAllocObjectsSize = oacgb.getAllocObjectNumbers();
            nTotalAllocObjects = runner.getProfilerClient().getAllocatedObjectsCountResults();

            // In some situations nInstrClasses can be already updated, but nTotalAllocObjects.length and/ort totalAllocObjectsSize - not yet.
            // Take measures to avoid ArrayIndexOutOfBoundsException.
            nTrackedItems = status.getNInstrClasses();

            if (nTrackedItems > nTotalAllocObjects.length) {
                nTrackedItems = nTotalAllocObjects.length;
            }

            if (nTrackedItems > totalAllocObjectsSize.length) {
                nTrackedItems = totalAllocObjectsSize.length;
            }

            // Operations necessary for correct bar representation of results
            maxValue = 0;
            nTotalBytes = 0;
            nTotalClasses = 0;

            for (int i = 0; i < nTrackedItems; i++) {
                if (maxValue < totalAllocObjectsSize[i]) {
                    maxValue = totalAllocObjectsSize[i];
                }

                nTotalBytes += totalAllocObjectsSize[i];
                nTotalClasses += nTotalAllocObjects[i];
            }

            if (classHistoryManager.isTrackingClass())
                classHistoryManager.processData(nTotalAllocObjects,
                                                totalAllocObjectsSize);

//            if (handler != null) {
//                handler.performAction("history update", new Object[] { nTotalAllocObjects, totalAllocObjectsSize }); // NOI18N
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
     * to give the panel chance to do some cleanup before asynchronous
     * call to updateLiveResults() will happen.
     *
     * Currently it closes the context menu if open, which would otherwise
     * block updating the results.
     */
    public void handleShutdown() {
        // Profiling session will finish and context menu is opened, this would block last live results update -> menu will be closed
        if ((memoryResPopupMenu != null) && memoryResPopupMenu.isVisible()) {
            updateResultsPending = false; // clear the flag, updateLiveResults() will be called explicitely from outside
            memoryResPopupMenu.setVisible(false); // close the context menu
        }
    }

    // --- Save current View action support --------------------------------------
    public boolean hasView() {
        return resTable != null;
    }

    public boolean supports(int instrumentataionType) {
        return instrumentataionType == CommonConstants.INSTR_OBJECT_ALLOCATIONS;
    }

    public void updateLiveResults() {
        if ((memoryResPopupMenu != null) && memoryResPopupMenu.isVisible()) {
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

    protected JPopupMenu getPopupMenu() {
        if (memoryResPopupMenu == null) {
            memoryResPopupMenu = new JPopupMenu();

            if (GoToSource.isAvailable()) {
                Font boldfont = memoryResPopupMenu.getFont().deriveFont(Font.BOLD);

                popupShowSource = new JMenuItem();
                popupShowSource.setFont(boldfont);
                popupShowSource.setText(GO_SOURCE_POPUP_ITEM_NAME);
                memoryResPopupMenu.add(popupShowSource);

                popupShowSource.addActionListener(this);
                memoryResPopupMenu.addSeparator();
            }

            if (runner.getProfilerEngineSettings().getAllocStackTraceLimit() != 0) {
                popupShowStacks = new JMenuItem();
                popupShowStacks.setText(SHOW_STACK_TRACES_POPUP_ITEM_NAME);
                memoryResPopupMenu.add(popupShowStacks);
                popupShowStacks.addActionListener(this);
                memoryResPopupMenu.addSeparator();
            }

            startHisto = new JMenuItem();
            startHisto.setText(LOG_CLASS_HISTORY);
            memoryResPopupMenu.add(startHisto);
            startHisto.addActionListener(this);

            memoryResPopupMenu.addPopupMenuListener(new PopupMenuListener() {
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
        startHisto.setEnabled(jvmAlive);

        return memoryResPopupMenu;
    }

    public void exportData(int typeOfFile, ExportDataDumper eDD, String viewName) {
        percentFormat.setMinimumFractionDigits(2);
        percentFormat.setMaximumFractionDigits(2);
        switch (typeOfFile) {
            case 1: exportCSV(",", eDD); break; // normal CSV   // NOI18N
            case 2: exportCSV(";", eDD); break; // Excel CSV  // NOI18N
            case 3: exportXML(eDD, viewName); break;
            case 4: exportHTML(eDD, viewName); break;
        }
        percentFormat.setMinimumFractionDigits(0);
        percentFormat.setMaximumFractionDigits(1);
    }

    private void exportHTML(ExportDataDumper eDD, String viewName) {
         // Header
        StringBuffer result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE></HEAD><BODY><TABLE border=\"1\"><tr>"); // NOI18N
        for (int i = 0; i < (columnNames.length); i++) {
            result.append("<th>").append(columnNames[i]).append("</th>");  // NOI18N
        }
        result.append("</tr>");  // NOI18N
        eDD.dumpData(result);

        for (int i=0; i < nTrackedItems; i++) {

            result = new StringBuffer("<tr><td>"+replaceHTMLCharacters(sortedClassNames[i])+"</td>");  // NOI18N
            result.append("<td align=\"right\">").append(percentFormat.format(((double) totalAllocObjectsSize[i])/nTotalBytes)).append("</td>");  // NOI18N
            result.append("<td align=\"right\">").append(totalAllocObjectsSize[i]).append(" B</td>");  // NOI18N
            result.append("<td align=\"right\">").append(nTotalAllocObjects[i]).append("</td></tr>");  // NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TABLE></BODY></HTML>"));  // NOI18N
    }

    private void exportXML(ExportDataDumper eDD, String viewName) {
         // Header
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\">"+newline); // NOI18N
        result.append("<TableData NumRows=\"").append(nTrackedItems).append("\" NumColumns=\"4\">").append(newline).append("<TableHeader>");  // NOI18N
        for (int i = 0; i < (columnNames.length); i++) {
            result.append("  <TableColumn><![CDATA[").append(columnNames[i]).append("]]></TableColumn>").append(newline);  // NOI18N
        }
        result.append("</TableHeader>");  // NOI18N
        eDD.dumpData(result);

        // Data
        for (int i=0; i < nTrackedItems; i++) {
            result = new StringBuffer("  <TableRow>"+newline+"   <TableColumn><![CDATA["+sortedClassNames[i]+"]]></TableColumn>"+newline);  // NOI18N
            result.append("   <TableColumn><![CDATA[").append(percentFormat.format(((double) totalAllocObjectsSize[i])/nTotalBytes)).append("]]></TableColumn>").append(newline);  // NOI18N
            result.append("   <TableColumn><![CDATA[").append(totalAllocObjectsSize[i]).append("]]></TableColumn>").append(newline);  // NOI18N
            result.append("   <TableColumn><![CDATA[").append(nTotalAllocObjects[i]).append("]]></TableColumn>").append(newline).append("  </TableRow>").append(newline);  // NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TableData>"+newline+"</ExportedView>"));  // NOI18N
    }

    private void exportCSV(String separator, ExportDataDumper eDD) {
        // Header
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N

        for (int i = 0; i < (columnNames.length); i++) {
            result.append(quote).append(columnNames[i]).append(quote).append(separator);
        }
        result.deleteCharAt(result.length()-1);
        result.append(newLine);
        eDD.dumpData(result);

        // Data
        for (int i=0; i < nTrackedItems; i++) {
            result = new StringBuffer();
            result.append(quote).append(sortedClassNames[i]).append(quote).append(separator);
            result.append(quote).append(percentFormat.format(((double) totalAllocObjectsSize[i])/nTotalBytes)).append(quote).append(separator);
            result.append(quote).append(totalAllocObjectsSize[i]).append(quote).append(separator);
            result.append(quote).append(nTotalAllocObjects[i]).append(quote).append(newLine);
            eDD.dumpData(result);
        }
        eDD.close();
    }

    private String replaceHTMLCharacters(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          char c = s.charAt(i);
          switch (c) {
              case '<': sb.append("&lt;"); break;  // NOI18N
              case '>': sb.append("&gt;"); break;  // NOI18N
              case '&': sb.append("&amp;"); break;  // NOI18N
              case '"': sb.append("&quot;"); break;  // NOI18N
              default: sb.append(c); break;
          }
        }
        return sb.toString();
    }
}
