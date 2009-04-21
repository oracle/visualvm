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

package org.netbeans.modules.profiler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.graphs.GraphPanel;
import org.netbeans.lib.profiler.ui.graphs.MemoryGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.SurvivingGenerationsGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.ThreadsGraphPanel;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;


/** An IDE TopComponent to display profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public final class TelemetryWindow extends TopComponent {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static final class GraphTab extends JPanel implements /*ActionListener, ChartActionListener,*/
                                                                  SaveViewAction.ViewProvider, ExportAction.ExportProvider {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final String timestamp = "Timestamp";
        private static final String heapSize = "Heap_Size_in_Bytes";
        private static final String usedHeap = "Used_Heap_in_Bytes";
        private static final String survivingGenerations = "Surviving_Generations";
        private static final String timeInGC = "Relative_Time_Spent_in_GC";
        private static final String  threadsCount = "Number_of_Threads";
        private static final String loadedClasses = "Loaded_Classes_Count";

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final GraphPanel panel;
        private final ExportAction exportActionButton;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public GraphTab(final GraphPanel panel) {
            this.panel = panel;

            setLayout(new BorderLayout());

            final JToolBar toolBar = new JToolBar() {
                public Component add(Component comp) {
                    if (comp instanceof JButton) {
                        UIUtils.fixButtonUI((JButton) comp);
                    }

                    return super.add(comp);
                }
            };

            exportActionButton = new ExportAction(this, null);

            toolBar.setFloatable(false);
            toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE); //NOI18N
            toolBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));

            toolBar.add(exportActionButton);
            toolBar.add(new SaveViewAction(this));
            toolBar.addSeparator();
            for (Action action : panel.getActions()) toolBar.add(action);

            add(toolBar, BorderLayout.NORTH);
            add(panel, BorderLayout.CENTER);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

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
            String col1Name = timestamp;
            String col2Name = new String();
            String col3Name = new String();
            System.arraycopy(data.timeStamps, 0, col1, 0, nItems);
            if (panel instanceof MemoryGraphPanel) {
                System.arraycopy(data.totalMemory, 0, col2, 0, nItems);
                System.arraycopy(data.usedMemory, 0, col3, 0, nItems);
                col2Name=heapSize;
                col3Name=usedHeap;
            } else if (panel instanceof SurvivingGenerationsGraphPanel) {
                System.arraycopy(data.nSurvivingGenerations, 0, col2, 0, nItems);
                System.arraycopy(data.relativeGCTimeInPerMil, 0, col3, 0, nItems);
                col2Name=survivingGenerations;
                col3Name=timeInGC;
            } else if (panel instanceof ThreadsGraphPanel) {
                System.arraycopy(data.nUserThreads, 0, col2, 0, nItems);
                System.arraycopy(data.loadedClassesCount, 0, col3, 0, nItems);
                col2Name=threadsCount;
                col3Name=loadedClasses;
            }
            //header
            eDD.dumpData(new StringBuffer(quote+col1Name+quote+separator+quote+col2Name+quote+separator+quote+col3Name+quote+newLine));
            Date d = new Date();            
            // Data
            for (int i=0; i < (nItems); i++) {
                d.setTime(col1[i]);
                eDD.dumpData(new StringBuffer(quote+d.toString()+quote+separator+quote+col2[i]+quote+separator+quote+col3[i]+quote+newLine));
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
            
            String col1Name = NbBundle.getMessage(TelemetryWindow.class,timestamp);
            String col2Name = new String();
            String col3Name = new String();
            String viewName = new String();
            System.arraycopy(data.timeStamps, 0, col1, 0, nItems);
            if (panel instanceof MemoryGraphPanel) {
                System.arraycopy(data.totalMemory, 0, col2, 0, nItems);
                System.arraycopy(data.usedMemory, 0, col3, 0, nItems);
                viewName=MEMORY_HEAP_TAB_NAME;
                col2Name=NbBundle.getMessage(TelemetryWindow.class,heapSize);
                col3Name=NbBundle.getMessage(TelemetryWindow.class,usedHeap);
            } else if (panel instanceof SurvivingGenerationsGraphPanel) {
                System.arraycopy(data.nSurvivingGenerations, 0, col2, 0, nItems);
                System.arraycopy(data.relativeGCTimeInPerMil, 0, col3, 0, nItems);
                viewName=MEMORY_GC_TAB_NAME;
                col2Name=NbBundle.getMessage(TelemetryWindow.class,survivingGenerations);
                col3Name=NbBundle.getMessage(TelemetryWindow.class,timeInGC);
            } else if (panel instanceof ThreadsGraphPanel) {
                System.arraycopy(data.nUserThreads, 0, col2, 0, nItems);
                System.arraycopy(data.loadedClassesCount, 0, col3, 0, nItems);
                viewName=THREADS_STATISTICS_TAB_NAME;
                col2Name=NbBundle.getMessage(TelemetryWindow.class,threadsCount);
                col3Name=NbBundle.getMessage(TelemetryWindow.class,loadedClasses);
            }
            //header
            StringBuffer result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE></HEAD><BODY><table border=\"1\"><tr>"); // NOI18N
            result.append("<th>"+col1Name+"</th><th>"+col2Name+"</th><th>"+col3Name+"</th></tr>"); //NOI18N
            eDD.dumpData(result);
            Date d = new Date();
            // Data
            for (int i=0; i < (nItems); i++) {
                d.setTime(col1[i]);
                eDD.dumpData(new StringBuffer("<tr><td>"+d.toString()+"</td><td align=right>"+col2[i]+"</td><td align=right>"+col3[i]+"</td></tr>")); //NOI18N
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

            String col1Name = NbBundle.getMessage(TelemetryWindow.class,timestamp);
            String col2Name = new String();
            String col3Name = new String();
            String viewName = new String();
            System.arraycopy(data.timeStamps, 0, col1, 0, nItems);
            if (panel instanceof MemoryGraphPanel) {
                System.arraycopy(data.totalMemory, 0, col2, 0, nItems);
                System.arraycopy(data.usedMemory, 0, col3, 0, nItems);
                viewName=MEMORY_HEAP_TAB_NAME;
                col2Name=heapSize;
                col3Name=usedHeap;
            } else if (panel instanceof SurvivingGenerationsGraphPanel) {
                System.arraycopy(data.nSurvivingGenerations, 0, col2, 0, nItems);
                System.arraycopy(data.relativeGCTimeInPerMil, 0, col3, 0, nItems);
                viewName=MEMORY_GC_TAB_NAME;
                col2Name=survivingGenerations;
                col3Name=timeInGC;
            } else if (panel instanceof ThreadsGraphPanel) {
                System.arraycopy(data.nUserThreads, 0, col2, 0, nItems);
                System.arraycopy(data.loadedClassesCount, 0, col3, 0, nItems);
                viewName=THREADS_STATISTICS_TAB_NAME;
                col2Name=threadsCount;
                col3Name=loadedClasses;
            }
            //header
            String newline = System.getProperty("line.separator"); // NOI18N
            StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\">"+newline); // NOI18N
            result.append("<TableData NumRows=\""+nItems+"\" NumColumns=\"3\">"+newline+" <TableHeader>");  // NOI18N
            result.append(" <TableColumn>"+col1Name+" </TableColumn>"+newline+" <TableColumn>"+col2Name+" </TableColumn>"+newline+" <TableColumn>"+col3Name+" </TableColumn>"+newline);  // NOI18N
            result.append(" </TableHeader>"+newline+" <TableBody>"+newline); //NOI18N
            eDD.dumpData(result);
            Date d = new Date();
            // Data
            for (int i=0; i < (nItems); i++) {
                d.setTime(col1[i]);
                result = new StringBuffer("  <TableRow>"+newline+"   <TableCell>"+d.toString()+"</TableCell>"+newline);  // NOI18N
                result.append("   <TableCell>"+col2[i]+"</TableCell>"+newline);  // NOI18N
                result.append("   <TableCell>"+col3[i]+"</TableCell>"+newline+"  </TableRow>"+newline);  // NOI18N
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

    // -----
    // I18N String constants
    private static final String THREADS_STATISTICS_TAB_NAME = NbBundle.getMessage(TelemetryWindow.class,
                                                                                  "TelemetryWindow_ThreadsStatisticsTabName"); // NOI18N
    private static final String MEMORY_HEAP_TAB_NAME = NbBundle.getMessage(TelemetryWindow.class,
                                                                           "TelemetryWindow_MemoryHeapTabName"); // NOI18N
    private static final String MEMORY_GC_TAB_NAME = NbBundle.getMessage(TelemetryWindow.class, "TelemetryWindow_MemoryGCTabName"); // NOI18N
    private static final String THREADS_STATISTICS_TAB_DESCR = NbBundle.getMessage(TelemetryWindow.class,
                                                                                   "TelemetryWindow_ThreadsStatisticsTabDescr"); // NOI18N
    private static final String MEMORY_HEAP_TAB_DESCR = NbBundle.getMessage(TelemetryWindow.class,
                                                                            "TelemetryWindow_MemoryHeapTabDescr"); // NOI18N
    private static final String MEMORY_GC_TAB_DESCR = NbBundle.getMessage(TelemetryWindow.class,
                                                                          "TelemetryWindow_MemoryGCTabDescr"); // NOI18N
    private static final String TELEMETRY_ACCESS_DESCR = NbBundle.getMessage(TelemetryWindow.class,
                                                                             "TelemetryWindow_TelemetryAccessDescr"); // NOI18N
                                                                                                                      // -----
    private static final String HELP_CTX_KEY = "TelemetryWindow.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static TelemetryWindow defaultInstance;
    private static final Image windowIcon = ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/telemetryWindow.png"); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final GraphTab generationsPanel;
    private final GraphTab heapPanel;
    private final GraphTab threadsStatsPanel;
    private final JTabbedPane tabs;
    private final MemoryGraphPanel heapGraph;
    private final SurvivingGenerationsGraphPanel generationsGraph;
    private final ThreadsGraphPanel threadsStatsGraph;
    private Component lastFocusOwner;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Initializes the window */
    public TelemetryWindow() {
        setName(NbBundle.getMessage(TelemetryWindow.class, "LAB_TelemetryWindowName")); // NOI18N
        setIcon(windowIcon);
        getAccessibleContext().setAccessibleDescription(TELEMETRY_ACCESS_DESCR);
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();

        tabs.setTabPlacement(JTabbedPane.BOTTOM);
        add(tabs, BorderLayout.CENTER);

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

        tabs.addTab(MEMORY_HEAP_TAB_NAME, null, heapPanel, MEMORY_HEAP_TAB_DESCR);
        tabs.addTab(MEMORY_GC_TAB_NAME, null, generationsPanel, MEMORY_GC_TAB_DESCR);
        tabs.addTab(THREADS_STATISTICS_TAB_NAME, null, threadsStatsPanel, THREADS_STATISTICS_TAB_DESCR);

        // Fix for Issue 115062 (CTRL-PageUp/PageDown should move between snapshot tabs)
        tabs.getActionMap().getParent().remove("navigatePageUp"); // NOI18N
        tabs.getActionMap().getParent().remove("navigatePageDown"); // NOI18N

        // support for traversing subtabs using Ctrl-Alt-PgDn/PgUp
        getActionMap().put("PreviousViewAction", new AbstractAction() { // NOI18N
                public void actionPerformed(ActionEvent e) {
                    moveToPreviousSubTab();
                }
            });
        getActionMap().put("NextViewAction", new AbstractAction() { // NOI18N
                public void actionPerformed(ActionEvent e) {
                    moveToNextSubTab();
                }
            });

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
            IDEUtils.runInEventDispatchThread(new Runnable() {
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

    public void componentActivated() {
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        } else if (threadsStatsGraph != null) {
            threadsStatsGraph.requestFocus();
        }
    }

    public void componentDeactivated() {
        lastFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    public void showGC() {
        tabs.setSelectedComponent(generationsPanel);
        open();
        requestActive();
    }

    public void showHeap() {
        tabs.setSelectedComponent(heapPanel);
        open();
        requestActive();
    }

    public void showThreads() {
        tabs.setSelectedComponent(threadsStatsPanel);
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

    private void moveToNextSubTab() {
        tabs.setSelectedIndex(UIUtils.getNextSubTabIndex(tabs, tabs.getSelectedIndex()));
    }

    private void moveToPreviousSubTab() {
        tabs.setSelectedIndex(UIUtils.getPreviousSubTabIndex(tabs, tabs.getSelectedIndex()));
    }
}
