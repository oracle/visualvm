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

package org.netbeans.lib.profiler.ui.cpu;

import org.netbeans.lib.profiler.results.ExportDataDumper;
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

    public SnapshotFlatProfilePanel(CPUResUserActionsHandler actionsHandler, boolean sampling) {
        this(actionsHandler, null, sampling);
    }

    public SnapshotFlatProfilePanel(CPUResUserActionsHandler actionsHandler, CPUSelectionHandler selectionHandler, boolean sampling) {
        super(actionsHandler, selectionHandler, sampling);
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD, boolean combine, String viewName) {
        percentFormat.setMinimumFractionDigits(2);
        percentFormat.setMaximumFractionDigits(2);
        switch (exportedFileType) {
            case 1: exportCSV(",", eDD,combine); break; // normal CSV
            case 2: exportCSV(";", eDD,combine); break; // Excel CSV
            case 3: exportXML(eDD, combine, viewName); break;
            case 4: exportHTML(eDD, combine, viewName); break;
        }
        percentFormat.setMinimumFractionDigits(0);
        percentFormat.setMaximumFractionDigits(1);
    }

    private void exportHTML(ExportDataDumper eDD, boolean combine, String viewName) {
         // Header
        StringBuffer result;
        boolean iCTTS = flatProfileContainer.isCollectingTwoTimeStamps();
        if (!combine) {
            result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE><style type=\"text/css\">pre.method{overflow:auto;width:600;height:30;vertical-align:baseline}pre.parent{overflow:auto;width:400;height:30;vertical-align:baseline}td.method{text-align:left;width:600}td.parent{text-align:left;width:400}td.right{text-align:right;white-space:nowrap}</style></HEAD><BODY><TABLE border=\"1\"><tr>");
        } else {
            result = new StringBuffer("<br><br><TABLE border=\"1\"><tr>"); // NOI18N
        }

         // NOI18N
        for (int i = 0; i < (columnCount); i++) {
            result.append("<th>").append(columnNames[i]).append("</th>");
        }
        result.append("</tr>");
        eDD.dumpData(result);

        for (int i=0; i < flatProfileContainer.getNRows(); i++) {
            result = new StringBuffer("<tr><td class=\"method\"><pre class=\"method\">"+replaceHTMLCharacters(flatProfileContainer.getMethodNameAtRow(i))+"</pre></td>");
            result.append("<td class=\"right\">").append(percentFormat.format(((double)flatProfileContainer.getPercentAtRow(i))/100)).append("</td>");
            result.append("<td class=\"right\">").append((double) flatProfileContainer.getTimeInMcs0AtRow(i)/1000).append(" ms</td>");
            if (iCTTS) {
                result.append("<td class=\"right\">").append((double) flatProfileContainer.getTimeInMcs1AtRow(i)/1000).append(" ms</td>");
            }
            result.append("<td class=\"right\">").append(flatProfileContainer.getNInvocationsAtRow(i)).append("</td></tr>");
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TABLE></BODY></HTML>"));
    }

    private void exportXML(ExportDataDumper eDD, boolean combine, String viewName) {
         // Header
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result;
        boolean iCTTS = flatProfileContainer.isCollectingTwoTimeStamps();
        if (!combine) {
            result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\" type=\"table\">"+newline+" <TableData NumRows=\""+flatProfileContainer.getNRows()+"\" NumColumns=\"4\">"+newline+"  <TableHeader>"); // NOI18N
        } else {
            result = new StringBuffer(newline+"<TableData NumRows=\""+flatProfileContainer.getNRows()+"\" NumColumns=\"4\">"+newline+"  <TableHeader>"); // NOI18N
        }
        for (int i = 0; i < ( columnCount); i++) {
            result.append("   <TableColumn><![CDATA[").append(columnNames[i]).append("]]></TableColumn>").append(newline);
        }
        result.append("  </TableHeader>").append(newline).append("  <TableBody>").append(newline);
        eDD.dumpData(result);

        for (int i=0; i < flatProfileContainer.getNRows(); i++) {
            result = new StringBuffer("   <TableRow>"+newline+"    <TableColumn><![CDATA["+flatProfileContainer.getMethodNameAtRow(i)+"]]></TableColumn>"+newline);
            result.append("    <TableColumn><![CDATA[").append(percentFormat.format(((double)flatProfileContainer.getPercentAtRow(i))/100)).append("]]></TableColumn>").append(newline);
            result.append("    <TableColumn><![CDATA[").append(((double) flatProfileContainer.getTimeInMcs0AtRow(i))/1000).append(" ms]]></TableColumn>").append(newline);
            if (iCTTS) {
                result.append("    <TableColumn><![CDATA[").append(((double) flatProfileContainer.getTimeInMcs1AtRow(i))/1000).append(" ms]]></TableColumn>").append(newline);
            }
            result.append("    <TableColumn><![CDATA[").append(flatProfileContainer.getNInvocationsAtRow(i)).append("]]></TableColumn>").append(newline).append("  </TableRow>").append(newline);
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer("  </TableBody>"+" </TableData>"+newline+"</ExportedView>"));
    }

    private void exportCSV(String separator, ExportDataDumper eDD, boolean combine) {
        // Header
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        boolean iCTTS = flatProfileContainer.isCollectingTwoTimeStamps();
        if (combine) {
            result.append(quote).append(quote).append(separator).append(quote).append(quote).append(separator).append(quote).append(quote).append(separator).append(quote).append(quote).append(newLine);
        }
        for (int i = 0; i < (columnCount); i++) {
            result.append(quote).append(columnNames[i]).append(quote).append(separator);
        }
        result.deleteCharAt(result.length()-1);
        result.append(newLine);
        eDD.dumpData(result);

        // Data
        for (int i=0; i < flatProfileContainer.getNRows(); i++) {
            result = new StringBuffer();
            result.append(quote).append(flatProfileContainer.getMethodNameAtRow(i)).append(quote).append(separator);
            result.append(quote).append(flatProfileContainer.getPercentAtRow(i)).append(quote).append(separator);
            result.append(quote).append((double)flatProfileContainer.getTimeInMcs0AtRow(i)/1000).append(" ms").append(quote).append(separator);
            if (iCTTS) {
                result.append(quote).append((double)flatProfileContainer.getTimeInMcs1AtRow(i)/1000).append(" ms").append(quote).append(separator);
            }
            result.append(quote).append(flatProfileContainer.getNInvocationsAtRow(i)).append(quote).append(newLine);
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
              case '<': sb.append("&lt;"); break;
              case '>': sb.append("&gt;"); break;
              case '&': sb.append("&amp;"); break;
              case '"': sb.append("&quot;"); break;
              default: sb.append(c); break;
          }
        }
        return sb.toString();
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
        
        initDataUponResultsFetch();

        // If a now-inapplicable setting remained from previous run, reset it
        if ((snapshot == null) || (!collectingTwoTimeStamps && (sortBy == FlatProfileContainer.SORT_BY_SECONDARY_TIME))) {
            sortBy = FlatProfileContainer.SORT_BY_TIME;
        }

        // Reinit bar max value here - operations necessary for correct bar representation of results
        flatProfileContainer.filterOriginalData(FilterComponent.getFilterValues(filterString), filterType, valueFilterValue);
        flatProfileContainer.sortBy(sortBy, sortOrder); // This will actually create the below-used percent()
                                                        // thing for proper timer

        setResultsAvailable(true);
    }
    
    protected void initDataUponResultsFetch() {}

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
