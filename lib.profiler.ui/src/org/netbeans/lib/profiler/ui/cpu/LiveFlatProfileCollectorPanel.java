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

import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.FlatProfileProvider;
import org.netbeans.lib.profiler.ui.LiveResultsPanel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.netbeans.lib.profiler.results.ExportDataDumper;


/**
 *
 * @author Jaroslav Bachorik
 */
public class LiveFlatProfileCollectorPanel extends FlatProfilePanel implements LiveResultsPanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JPopupMenu popup;
    private TargetAppRunner runner = null;
    private boolean firstTime = true;
    private boolean updateResultsInProgress = false;
    private boolean updateResultsPending = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LiveFlatProfileCollectorPanel(TargetAppRunner runner, CPUResUserActionsHandler actionsHandler,
                                         CPUSelectionHandler selectionHandler) {
        super(actionsHandler, selectionHandler);
        //    setCPUSelectionHandler(selectionHandler);
        this.runner = runner;

        addFilterListener(new FilterComponent.FilterListener() {

            public void filterChanged() {
                if (flatProfileContainer != null) {
                    setDataToDisplay(flatProfileContainer);
                }
            }
        });

        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * This method is supposed to be used for displaying data in live update mode. The data is initialized using a
     * "lightweight" flat profile container, not backed by the snapshot, that does not allow any operations listed
     * above, i.e. switching views, obtaining reverse call graph, and going to method's source.
     */
    public void setDataToDisplay(final FlatProfileContainer fpc) {
        threadId = -1;
        flatProfileContainer = fpc;
        collectingTwoTimeStamps = flatProfileContainer.isCollectingTwoTimeStamps();

        flatProfileContainer.filterOriginalData(FilterComponent.getFilterStrings(filterString), filterType, valueFilterValue);

        prepareResults(firstTime);
        firstTime = false;

        setResultsAvailable(hasData());

        //    flatProfileContainer.sortBy(sortBy, sortOrder);   // This will actually create the below-used percent() thing for proper timer
    }

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(jScrollPane);
        }

        return UIUtils.createScreenshot(resTable);
    }

    public String getViewName() {
        return "cpu-live"; // NOI18N
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

    public boolean hasData() {
        return (flatProfileContainer != null) && (flatProfileContainer.getNRows() > 0);
    }

    public boolean hasView() {
        return resTable != null;
    }

    @Override
    public void prepareResults() {
        super.prepareResults();
    }

    public boolean supports(int instrumentationType) {
        return (instrumentationType == CommonConstants.INSTR_RECURSIVE_FULL)
               || (instrumentationType == CommonConstants.INSTR_RECURSIVE_SAMPLED);
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

        FlatProfileProvider flatProvider = runner.getProfilerClient().getFlatProfileProvider();

        if (flatProvider != null) {
            FlatProfileContainer fpc = flatProvider.createFlatProfile();
            int retryCounter = 2;
            boolean doRetry = false;

            do {
                doRetry = false;

                if (fpc != null) {
                    setDataToDisplay(fpc);

                    if (selectedRowString != null) {
                        resTable.selectRowByContents(selectedRowString, 0, false);
                    }
                } else {
                    doRetry = true;
                }

                if (doRetry) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        doRetry = false;
                    }
                }
            } while ((--retryCounter > 0) && doRetry);
        }

        updateResultsInProgress = false;
    }

    protected String[] getMethodClassNameAndSig(int methodId, int currentView) {
        ProfilingSessionStatus status = runner.getProfilingSessionStatus();
        String className = status.getInstrMethodClasses()[methodId];

        if (currentView == CPUResultsSnapshot.METHOD_LEVEL_VIEW) {
            String methodName = (status.getInstrMethodNames() != null) ? status.getInstrMethodNames()[methodId] : null;
            String methodSig = (status.getInstrMethodSignatures() != null) ? status.getInstrMethodSignatures()[methodId] : null;

            return new String[] { className, methodName, methodSig };
        }

        return new String[] { className, null, null };
    }

    @Override
    protected JPopupMenu createPopupMenu() {
        if (popup == null) {
            popup = super.createPopupMenu();
        }

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

        return popup;
    }

    protected void obtainResults() {
        // If a now-inapplicable setting remained from previous run, reset it
        if ((!collectingTwoTimeStamps && (sortBy == FlatProfileContainer.SORT_BY_SECONDARY_TIME))) {
            sortBy = FlatProfileContainer.SORT_BY_TIME;
        }

        // Reinit bar max value here - operations necessary for correct bar representation of results
        flatProfileContainer.filterOriginalData(FilterComponent.getFilterStrings(filterString), filterType, valueFilterValue);
        flatProfileContainer.sortBy(sortBy, sortOrder); // This will actually create the below-used percent() thing for proper timer
    }

    /**
     * Default implementation throwing IllegalStateException, needs to be overriden by classes that do support showReverseCallGraph
     */
    @Override
    protected void showReverseCallGraph(int threadId, int methodId, int currentView, int sortingColumn, boolean sortingOrder) {
        throw new IllegalStateException();
    }

    protected boolean supportsReverseCallGraph() {
        return false;
    }

    protected boolean supportsSubtreeCallGraph() {
        return false;
    }

    void setSelectedRowString(String rowString) {
        if (rowString != null) {
            resTable.selectRowByContents(rowString, 0, false);
        }
    }

    String getSelectedRowString() {
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

        return selectedRowString;
    }

    private void initComponents() {
        this.setPreferredSize(new Dimension(800, 600));
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD, String viewName) {
        percentFormat.setMaximumFractionDigits(2);
        percentFormat.setMinimumFractionDigits(2);
        switch (exportedFileType) {
            case 1: exportCSV(",", eDD); break; //NOI18N
            case 2: exportCSV(";", eDD); break; //NOI18N
            case 3: exportXML(eDD, viewName); break;
            case 4: exportHTML(eDD, viewName); break;
        }
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(0);
    }

    private void exportHTML(ExportDataDumper eDD, String viewName) {
         // Header
        StringBuffer result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE></HEAD><BODY><TABLE border=\"1\"><tr>"); // NOI18N
        for (int i = 0; i < ( columnCount); i++) {
            result.append("<th>"+columnNames[i]+"</th>"); //NOI18N
        }
        result.append("</tr>"); //NOI18N


        eDD.dumpData(result);
        for (int i=0; i < (flatProfileContainer.getNRows()-1); i++) {
            result = new StringBuffer("<tr><td>"+replaceHTMLCharacters(flatProfileContainer.getMethodNameAtRow(i))+"</td>"); //NOI18N
            result.append("<td align=\"right\">"+percentFormat.format(flatProfileContainer.getPercentAtRow(i))+((flatProfileContainer.getTimeInMcs0AtRow(i)%10==0)?((flatProfileContainer.getTimeInMcs0AtRow(i)%100==0)?((flatProfileContainer.getTimeInMcs0AtRow(i)%1000==0)?("    "):("  ")):(" ")):(""))+"</td>"); //NOI18N
            result.append("<td align=right>"+((double) flatProfileContainer.getTimeInMcs0AtRow(i)/1000)+" ms</td>"); //NOI18N
            result.append("<td align=\"right\">"+flatProfileContainer.getNInvocationsAtRow(i)+"</td></tr>"); //NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TABLE></BODY></HTML>")); //NOI18N
    }

    private void exportXML(ExportDataDumper eDD, String viewName) {
         // Header
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\" type=\"table\">"+newline+" <TableData NumRows=\""+flatProfileContainer.getNRows()+"\" NumColumns=\"4\">"+newline+"  <TableHeader>"); // NOI18N
        for (int i = 0; i < ( columnCount); i++) {
            result.append("   <TableColumn><![CDATA["+columnNames[i]+"]]></TableColumn>"+newline); //NOI18N
        }
        result.append("  </TableHeader>"+newline+"  <TableBody>"+newline); //NOI18N
        eDD.dumpData(result);

        for (int i=0; i < (flatProfileContainer.getNRows()); i++) {
            result = new StringBuffer("   <TableRow>"+newline+"    <TableColumn><![CDATA["+flatProfileContainer.getMethodNameAtRow(i)+"]]></TableColumn>"+newline); //NOI18N
            result.append("    <TableColumn><![CDATA["+percentFormat.format(flatProfileContainer.getPercentAtRow(i))+"]]></TableColumn>"+newline); //NOI18N
            result.append("    <TableColumn><![CDATA["+(((double) flatProfileContainer.getTimeInMcs0AtRow(i))/1000)+" ms]]></TableColumn>"+newline); //NOI18N
            result.append("    <TableColumn><![CDATA["+flatProfileContainer.getNInvocationsAtRow(i)+"]]></TableColumn>"+newline+"  </TableRow>"+newline); //NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer("  </TableBody>"+" </TableData>"+newline+"</ExportedView>")); //NOI18N
    }

    private void exportCSV(String separator, ExportDataDumper eDD) {
        // Header
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N

        for (int i = 0; i < (columnCount); i++) {
            result.append(quote+columnNames[i]+quote+separator);
        }
        result.deleteCharAt(result.length()-1);
        result.append(newLine);
        eDD.dumpData(result);

        // Data
        for (int i=0; i < (flatProfileContainer.getNRows()); i++) {
            result = new StringBuffer();
            result.append(quote+flatProfileContainer.getMethodNameAtRow(i)+quote+separator);
            result.append(quote+flatProfileContainer.getPercentAtRow(i)+quote+separator);
            result.append(quote+((double)flatProfileContainer.getTimeInMcs0AtRow(i)/1000)+" ms"+quote+separator);
            result.append(quote+flatProfileContainer.getNInvocationsAtRow(i)+quote+newLine);
            eDD.dumpData(result);
        }
        eDD.close();
    }

    private String replaceHTMLCharacters(String s) {
        StringBuffer sb = new StringBuffer();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          char c = s.charAt(i);
          switch (c) {
              case '<': sb.append("&lt;"); break; //NOI18N
              case '>': sb.append("&gt;"); break; //NOI18N
              case '&': sb.append("&amp;"); break; //NOI18N
              case '"': sb.append("&quot;"); break; //NOI18N
              default: sb.append(c); break;
          }
        }
        return sb.toString();
    }
}
