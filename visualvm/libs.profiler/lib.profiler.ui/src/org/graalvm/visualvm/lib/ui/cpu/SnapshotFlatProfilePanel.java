/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainer;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.FilterComponent;
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

    public SnapshotFlatProfilePanel(CPUResUserActionsHandler actionsHandler, Boolean sampling) {
        this(actionsHandler, null, sampling);
    }

    public SnapshotFlatProfilePanel(CPUResUserActionsHandler actionsHandler, CPUSelectionHandler selectionHandler, Boolean sampling) {
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
            result.append("<th>").append(columnNames[i]).append(columnNames[i].equals("Total Time")?" [&micro;s]":"").append("</th>");
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
            result.append("<td class=\"right\">").append((double) flatProfileContainer.getTotalTimeInMcs0AtRow(i)/1000).append(" ms</td>");
            if (iCTTS) {
                result.append("<td class=\"right\">").append((double) flatProfileContainer.getTotalTimeInMcs1AtRow(i)/1000).append(" ms</td>");
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
            result.append("    <TableColumn><![CDATA[").append(((double) flatProfileContainer.getTotalTimeInMcs0AtRow(i))/1000).append(" ms]]></TableColumn>").append(newline);
            if (iCTTS) {
                result.append("    <TableColumn><![CDATA[").append(((double) flatProfileContainer.getTotalTimeInMcs1AtRow(i))/1000).append(" ms]]></TableColumn>").append(newline);
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
            result.append(quote).append((double)flatProfileContainer.getTotalTimeInMcs0AtRow(i)/1000).append(" ms").append(quote).append(separator);
            if (iCTTS) {
                result.append(quote).append((double)flatProfileContainer.getTotalTimeInMcs1AtRow(i)/1000).append(" ms").append(quote).append(separator);
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
     * threadId &gt;= 0, or -1 to mean "display data for all threads". When data is initialized in this way, all operations
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
