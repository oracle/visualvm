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

package org.graalvm.visualvm.lib.ui.memory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import org.graalvm.visualvm.lib.jfluid.results.ResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.AllocMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;
import org.graalvm.visualvm.lib.ui.UIUtils;


/**
 * This class implements presentation frames for Object Allocation Profiling.
 *
 * @author Ian Formanek
 */
public class SnapshotAllocResultsPanel extends AllocResultsPanel implements ActionListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle"); // NOI18N
    private static final String GO_SOURCE_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_GoSourcePopupItemName"); // NOI18N
    private static final String SHOW_STACK_TRACES_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_ShowStackTracesPopupItemName"); // NOI18N
                                                                                                                                          // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AllocMemoryResultsSnapshot snapshot;
    private JMenuItem popupShowSource;
    private JMenuItem popupShowStacks;
    private JPopupMenu memoryResPopupMenu;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotAllocResultsPanel(AllocMemoryResultsSnapshot snapshot, MemoryResUserActionsHandler actionsHandler) {
        super(actionsHandler);
        this.snapshot = snapshot;

        fetchResultsFromSnapshot();

        //prepareResults();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ResultsSnapshot getSnapshot() {
        return snapshot;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == popupShowStacks) {
            actionsHandler.showStacksForClass(selectedClassId, getSortingColumn(), getSortingOrder());
        } else if (e.getSource() == popupShowSource && popupShowSource != null) {
            showSourceForClass(selectedClassId);
        }
    }

    protected String getClassName(int classId) {
        return snapshot.getClassName(classId);
    }

    protected String[] getClassNames() {
        return snapshot.getClassNames();
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
            }

            if (snapshot.containsStacks()) {
                if (GoToSource.isAvailable()) memoryResPopupMenu.addSeparator();
                popupShowStacks = new JMenuItem();
                popupShowStacks.setText(SHOW_STACK_TRACES_POPUP_ITEM_NAME);
                memoryResPopupMenu.add(popupShowStacks);
                popupShowStacks.addActionListener(this);
            }
        }

        return memoryResPopupMenu;
    }

    private void fetchResultsFromSnapshot() {
        totalAllocObjectsSize = UIUtils.copyArray(snapshot.getObjectsSizePerClass());
        nTotalAllocObjects = UIUtils.copyArray(snapshot.getObjectsCounts());

        // In some situations nInstrClasses can be already updated, but nTotalAllocObjects.length and/ort totalAllocObjectsSize - not yet.
        // Take measures to avoid ArrayIndexOutOfBoundsException.
        nTrackedItems = snapshot.getNProfiledClasses();

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

        initDataUponResultsFetch();
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
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(0);
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
            result.append("<td align=\"right\">").append(totalAllocObjectsSize[i]).append("</td>");  // NOI18N
            result.append("<td align=\"right\">").append(nTotalAllocObjects[i]).append("</td></tr>");  // NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TABLE></BODY></HTML>"));  // NOI18N
    }

    private void exportXML(ExportDataDumper eDD, String viewName) {
         // Header
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\">"+newline); // NOI18N
        result.append(" <TableData NumRows=\"").append(nTrackedItems).append("\" NumColumns=\"4\">").append(newline).append("<TableHeader>");  // NOI18N
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
