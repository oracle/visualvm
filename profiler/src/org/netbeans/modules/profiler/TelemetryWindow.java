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

package org.netbeans.modules.profiler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.graphs.GraphPanel;
import org.netbeans.lib.profiler.ui.graphs.MemoryGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.SurvivingGenerationsGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.ThreadsGraphPanel;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.Action;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.ui.ResultsView;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;


/** An IDE TopComponent to display profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TelemetryWindow_ThreadsStatisticsTabName=Threads / Loaded Classes",
    "TelemetryWindow_MemoryHeapTabName=Memory (Heap)",
    "TelemetryWindow_MemoryGCTabName=Memory (GC)",
    "TelemetryWindow_ThreadsStatisticsTabDescr=Graph displaying number of application threads",
    "TelemetryWindow_MemoryHeapTabDescr=Graph displaying heap size and usage",
    "TelemetryWindow_MemoryGCTabDescr=Graph displaying surviving generations and time spent in GC",
    "TelemetryWindow_TelemetryAccessDescr=Profiler telemetry graphs",
    "LAB_TelemetryWindowName=VM Telemetry",
    "Timestamp=Timestamp",
    "Heap_Size_in_Bytes=Heap Size (Bytes)",
    "Used_Heap_in_Bytes=Used Heap (Bytes)",
    "Surviving_Generations=Surviving Generations",
    "Relative_Time_Spent_in_GC=Relative Time Spent in GC (%)",
    "Number_of_Threads=Threads",
    "Loaded_Classes_Count=Loaded Classes"
})
public final class TelemetryWindow extends ProfilerTopComponent {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static final class GraphTab implements SaveViewAction.ViewProvider, ExportAction.ExportProvider {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final GraphPanel panel;
        private final ProfilerToolbar toolBar;
        private final ExportAction exportActionButton;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public GraphTab(final GraphPanel panel) {
            this.panel = panel;

            toolBar = ProfilerToolbar.create(true);
            exportActionButton = new ExportAction(this, null);
            toolBar.add(exportActionButton);
            toolBar.add(new SaveViewAction(this));
            toolBar.addSeparator();
            for (Action action : panel.getActions()) toolBar.add(action);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getToolbar() {
            return toolBar.getComponent();
        }
        
        public Component getComponent() {
            return panel;
        }
        
        public BufferedImage getViewImage(boolean onlyVisibleArea) {
            return UIUtils.createScreenshot(panel);
        }

        public String getViewName() {
            if (panel instanceof MemoryGraphPanel) {
                return "memory-heap"; // NOI18N
            } else if (panel instanceof SurvivingGenerationsGraphPanel) {
                return "memory-gc"; // NOI18N
            } else if (panel instanceof ThreadsGraphPanel) {
                return "threads-statistics"; // NOI18N
            }

            return null;
        }

        public boolean fitsVisibleArea() {
            return true;
        }

        // --- Save Current View action support ------------------------------------
        public boolean hasView() {
            return true;
        }

        private void exportCSVData(String separator, ExportDataDumper eDD) {
            VMTelemetryDataManager data = Profiler.getDefault().getVMTelemetryManager();
            String newLine = "\r\n"; // NOI18N
            String quote = "\""; // NOI18N
            // Initialize data
            int nItems = data.getItemCount();
            long[] col1 = new long[nItems];
            long[] col2 = new long[nItems];
            long[] col3 = new long[nItems];
            // TODO Issue #160475
            String col1Name = Bundle.Timestamp();
            String col2Name = ""; // NOI18N
            String col3Name = ""; // NOI18N
            System.arraycopy(data.timeStamps, 0, col1, 0, nItems);
            if (panel instanceof MemoryGraphPanel) {
                System.arraycopy(data.totalMemory, 0, col2, 0, nItems);
                System.arraycopy(data.usedMemory, 0, col3, 0, nItems);
                col2Name=Bundle.Heap_Size_in_Bytes();
                col3Name=Bundle.Used_Heap_in_Bytes();
            } else if (panel instanceof SurvivingGenerationsGraphPanel) {
                System.arraycopy(data.nSurvivingGenerations, 0, col2, 0, nItems);
                System.arraycopy(data.relativeGCTimeInPerMil, 0, col3, 0, nItems);
                col2Name=Bundle.Surviving_Generations();
                col3Name=Bundle.Relative_Time_Spent_in_GC();
            } else if (panel instanceof ThreadsGraphPanel) {
                System.arraycopy(data.nUserThreads, 0, col2, 0, nItems);
                System.arraycopy(data.loadedClassesCount, 0, col3, 0, nItems);
                col2Name=Bundle.Number_of_Threads();
                col3Name=Bundle.Loaded_Classes_Count();
            }
            //header
            eDD.dumpData(new StringBuffer(quote+col1Name+quote+separator+quote+col2Name+quote+separator+quote+col3Name+quote+newLine));
            Date d = new Date();            
            // Data
            for (int i=0; i < (nItems); i++) {
                d.setTime(col1[i]);
                eDD.dumpData(new StringBuffer(quote+DateFormat.getDateTimeInstance().format(d)+quote+separator+quote+col2[i]+quote+separator+quote+col3[i]+quote+newLine));
            }
            eDD.close();
        }

        private void exportHTMLData(ExportDataDumper eDD) {
            VMTelemetryDataManager data = Profiler.getDefault().getVMTelemetryManager();
            // Initialize data
            int nItems = data.getItemCount();
            long[] col1 = new long[nItems];
            long[] col2 = new long[nItems];
            long[] col3 = new long[nItems];
            
            String col1Name = Bundle.Timestamp();
            String col2Name = new String();
            String col3Name = new String();
            String viewName = new String();
            System.arraycopy(data.timeStamps, 0, col1, 0, nItems);
            if (panel instanceof MemoryGraphPanel) {
                System.arraycopy(data.totalMemory, 0, col2, 0, nItems);
                System.arraycopy(data.usedMemory, 0, col3, 0, nItems);
                viewName=Bundle.TelemetryWindow_MemoryHeapTabName();
                col2Name=Bundle.Heap_Size_in_Bytes();
                col3Name=Bundle.Used_Heap_in_Bytes();
            } else if (panel instanceof SurvivingGenerationsGraphPanel) {
                System.arraycopy(data.nSurvivingGenerations, 0, col2, 0, nItems);
                System.arraycopy(data.relativeGCTimeInPerMil, 0, col3, 0, nItems);
                viewName=Bundle.TelemetryWindow_MemoryGCTabName();
                col2Name=Bundle.Surviving_Generations();
                col3Name=Bundle.Relative_Time_Spent_in_GC();
            } else if (panel instanceof ThreadsGraphPanel) {
                System.arraycopy(data.nUserThreads, 0, col2, 0, nItems);
                System.arraycopy(data.loadedClassesCount, 0, col3, 0, nItems);
                viewName=Bundle.TelemetryWindow_ThreadsStatisticsTabName();
                col2Name=Bundle.Number_of_Threads();
                col3Name=Bundle.Loaded_Classes_Count();
            }
            //header
            StringBuffer result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE></HEAD><BODY><table border=\"1\"><tr>"); // NOI18N
            result.append("<th>").append(col1Name).append("</th><th>").append(col2Name).append("</th><th>").append(col3Name).append("</th></tr>"); //NOI18N
            eDD.dumpData(result);
            Date d = new Date();
            // Data
            for (int i=0; i < (nItems); i++) {
                d.setTime(col1[i]);
                eDD.dumpData(new StringBuffer("<tr><td>"+DateFormat.getDateTimeInstance().format(d)+"</td><td align=right>"+col2[i]+"</td><td align=right>"+col3[i]+"</td></tr>")); //NOI18N
            }
            eDD.dumpDataAndClose(new StringBuffer("</table></BODY></HTML>"));
        }

        private void exportXMLData(ExportDataDumper eDD) {
            VMTelemetryDataManager data = Profiler.getDefault().getVMTelemetryManager();
            // Initialize data
            int nItems = data.getItemCount();
            long[] col1 = new long[nItems];
            long[] col2 = new long[nItems];
            long[] col3 = new long[nItems];

            String col1Name = Bundle.Timestamp();
            String col2Name = new String();
            String col3Name = new String();
            String viewName = new String();
            System.arraycopy(data.timeStamps, 0, col1, 0, nItems);
            if (panel instanceof MemoryGraphPanel) {
                System.arraycopy(data.totalMemory, 0, col2, 0, nItems);
                System.arraycopy(data.usedMemory, 0, col3, 0, nItems);
                viewName=Bundle.TelemetryWindow_MemoryHeapTabName();
                col2Name=Bundle.Heap_Size_in_Bytes();
                col3Name=Bundle.Used_Heap_in_Bytes();
            } else if (panel instanceof SurvivingGenerationsGraphPanel) {
                System.arraycopy(data.nSurvivingGenerations, 0, col2, 0, nItems);
                System.arraycopy(data.relativeGCTimeInPerMil, 0, col3, 0, nItems);
                viewName=Bundle.TelemetryWindow_MemoryGCTabName();
                col2Name=Bundle.Surviving_Generations();
                col3Name=Bundle.Relative_Time_Spent_in_GC();
            } else if (panel instanceof ThreadsGraphPanel) {
                System.arraycopy(data.nUserThreads, 0, col2, 0, nItems);
                System.arraycopy(data.loadedClassesCount, 0, col3, 0, nItems);
                viewName=Bundle.TelemetryWindow_ThreadsStatisticsTabName();
                col2Name=Bundle.Number_of_Threads();
                col3Name=Bundle.Loaded_Classes_Count();
            }
            //header
            String newline = System.getProperty("line.separator"); // NOI18N
            StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\">"+newline); // NOI18N
            result.append("<TableData NumRows=\"").append(nItems).append("\" NumColumns=\"3\">").append(newline).append(" <TableHeader>");  // NOI18N
            result.append(" <TableColumn>").append(col1Name).append(" </TableColumn>").append(newline).append(" <TableColumn>").append(col2Name).append(" </TableColumn>").append(newline).append(" <TableColumn>").append(col3Name).append(" </TableColumn>").append(newline);  // NOI18N
            result.append(" </TableHeader>").append(newline).append(" <TableBody>").append(newline); //NOI18N
            eDD.dumpData(result);
            Date d = new Date();
            // Data
            for (int i=0; i < (nItems); i++) {
                d.setTime(col1[i]);
                result = new StringBuffer("  <TableRow>"+newline+"   <TableCell>"+DateFormat.getDateTimeInstance().format(d)+"</TableCell>"+newline);  // NOI18N
                result.append("   <TableCell>").append(col2[i]).append("</TableCell>").append(newline);  // NOI18N
                result.append("   <TableCell>").append(col3[i]).append("</TableCell>").append(newline).append("  </TableRow>").append(newline);  // NOI18N
                eDD.dumpData(result);
            }
            eDD.dumpDataAndClose(new StringBuffer(" </TableBody>"+newline+"</TableData>"+newline+"</ExportedView>"));  // NOI18N
        }

        public void exportData(int exportedFileType, ExportDataDumper eDD) {
            if ( (panel instanceof MemoryGraphPanel)||(panel instanceof SurvivingGenerationsGraphPanel)||(panel instanceof ThreadsGraphPanel)) {
                switch (exportedFileType) {
                    case 1: exportCSVData(",", eDD); //NOI18N                            
                            break;
                    case 2: exportCSVData(";", eDD); //NOI18N                            
                            break;
                    case 3: exportXMLData(eDD); //NOI18N
                            break;
                    case 4: exportHTMLData(eDD);
                            break;
                }
            }
        }

        public boolean hasExportableView() {
            return Profiler.getDefault().getVMTelemetryManager().getItemCount() > 0;
        }

        public boolean hasLoadedSnapshot() {
            return false;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "TelemetryWindow.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static TelemetryWindow defaultInstance;
    private static final Image windowIcon = Icons.getImage(ProfilerIcons.WINDOW_TELEMETRY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final GraphTab generationsPanel;
    private final GraphTab heapPanel;
    private final GraphTab threadsStatsPanel;
    private final ResultsView telemetryView;
    private final MemoryGraphPanel heapGraph;
    private final SurvivingGenerationsGraphPanel generationsGraph;
    private final ThreadsGraphPanel threadsStatsGraph;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Initializes the window */
    public TelemetryWindow() {
        setName(Bundle.LAB_TelemetryWindowName()); 
        setIcon(windowIcon);
        getAccessibleContext().setAccessibleDescription(Bundle.TelemetryWindow_TelemetryAccessDescr());
        setLayout(new BorderLayout());
        
        telemetryView = new ResultsView();
        add(telemetryView, BorderLayout.CENTER);

        heapGraph = MemoryGraphPanel.createBigPanel(NetBeansProfiler.getDefaultNB().
                                                    getVMTelemetryModels());
        generationsGraph = SurvivingGenerationsGraphPanel.createBigPanel(
                                                    NetBeansProfiler.getDefaultNB().
                                                    getVMTelemetryModels());
        threadsStatsGraph = ThreadsGraphPanel.createBigPanel(NetBeansProfiler.getDefaultNB().
                                                    getVMTelemetryModels());

        heapPanel = new GraphTab(heapGraph);
        generationsPanel = new GraphTab(generationsGraph);
        threadsStatsPanel = new GraphTab(threadsStatsGraph);

        telemetryView.addView(Bundle.TelemetryWindow_MemoryHeapTabName(), null,
                Bundle.TelemetryWindow_MemoryHeapTabDescr(), heapPanel.getComponent(), heapPanel.getToolbar());
        telemetryView.addView(Bundle.TelemetryWindow_MemoryGCTabName(), null,
                Bundle.TelemetryWindow_MemoryGCTabDescr(), generationsPanel.getComponent(), generationsPanel.getToolbar());
        telemetryView.addView(Bundle.TelemetryWindow_ThreadsStatisticsTabName(), null,
                Bundle.TelemetryWindow_ThreadsStatisticsTabDescr(), threadsStatsPanel.getComponent(), threadsStatsPanel.getToolbar());

        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static TelemetryWindow getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new TelemetryWindow();
        }

        return defaultInstance;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public static void closeIfOpened() {
        if (defaultInstance != null) {
            CommonUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        if (defaultInstance.isOpened()) {
                            defaultInstance.close();
                        }
                    }
                });
        }
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }
    
    protected Component defaultFocusOwner() {
        return threadsStatsGraph;
    }

    public void showGC() {
        telemetryView.selectView(generationsPanel.getComponent());
        open();
        requestActive();
    }

    public void showHeap() {
        telemetryView.selectView(heapPanel.getComponent());
        open();
        requestActive();
    }

    public void showThreads() {
        telemetryView.selectView(threadsStatsPanel.getComponent());
        open();
        requestActive();
    }

    /**
     * Subclasses are encouraged to override this method to provide preferred value
     * for unique TopComponent Id returned by getID. Returned value is used as starting
     * value for creating unique TopComponent ID.
     * Value should be preferably unique, but need not be.
     */
    protected String preferredID() {
        return this.getClass().getName();
    }
}
