/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package org.netbeans.lib.profiler.ui.memory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.*;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.GoToSource;


/**
 * This class implements presentation frames for Object Allocation Profiling.
 *
 * @author Ian Formanek
 * @author Tomas Hurka
 */
public class SnapshotSampledResultsPanel extends SampledResultsPanel implements ActionListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String GO_SOURCE_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_GoSourcePopupItemName"); // NOI18N
    private static final String SHOW_STACK_TRACES_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_ShowStackTracesPopupItemName"); // NOI18N
                                                                                                                                          // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private SampledMemoryResultsSnapshot snapshot;
    private JMenuItem popupShowSource;
    private JMenuItem popupShowStacks;
    private JPopupMenu memoryResPopupMenu;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotSampledResultsPanel(SampledMemoryResultsSnapshot snapshot, MemoryResUserActionsHandler actionsHandler) {
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
        totalLiveObjectsSize = UIUtils.copyArray(snapshot.getObjectsSizePerClass());
        nTotalLiveObjects = UIUtils.copyArray(snapshot.getObjectsCounts());

        // In some situations nInstrClasses can be already updated, but nTotalAllocObjects.length and/ort totalAllocObjectsSize - not yet.
        // Take measures to avoid ArrayIndexOutOfBoundsException.
        nTrackedItems = snapshot.getNProfiledClasses();

        if (nTrackedItems > nTotalLiveObjects.length) {
            nTrackedItems = nTotalLiveObjects.length;
        }

        if (nTrackedItems > totalLiveObjectsSize.length) {
            nTrackedItems = totalLiveObjectsSize.length;
        }

        // Operations necessary for correct bar representation of results
        maxValue = 0;
        nTotalLiveBytes = 0;
        nTotalClasses = 0;

        for (int i = 0; i < nTrackedItems; i++) {
            if (maxValue < totalLiveObjectsSize[i]) {
                maxValue = totalLiveObjectsSize[i];
            }

            nTotalLiveBytes += totalLiveObjectsSize[i];
            nTotalClasses += nTotalLiveObjects[i];
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
            result.append("<td align=\"right\">").append(percentFormat.format(((double) totalLiveObjectsSize[i])/nTotalLiveBytes)).append("</td>");  // NOI18N
            result.append("<td align=\"right\">").append(totalLiveObjectsSize[i]).append("</td>");  // NOI18N
            result.append("<td align=\"right\">").append(nTotalLiveObjects[i]).append("</td></tr>");  // NOI18N
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
            result.append("   <TableColumn><![CDATA[").append(percentFormat.format(((double) totalLiveObjectsSize[i])/nTotalLiveBytes)).append("]]></TableColumn>").append(newline);  // NOI18N
            result.append("   <TableColumn><![CDATA[").append(totalLiveObjectsSize[i]).append("]]></TableColumn>").append(newline);  // NOI18N
            result.append("   <TableColumn><![CDATA[").append(nTotalLiveObjects[i]).append("]]></TableColumn>").append(newline).append("  </TableRow>").append(newline);  // NOI18N
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
            result.append(quote).append(percentFormat.format(((double) totalLiveObjectsSize[i])/nTotalLiveBytes)).append(quote).append(separator);
            result.append(quote).append(totalLiveObjectsSize[i]).append(quote).append(separator);
            result.append(quote).append(nTotalLiveObjects[i]).append(quote).append(newLine);
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

