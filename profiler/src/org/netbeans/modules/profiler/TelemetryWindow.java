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

import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.charts.ChartActionListener;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChart;
import org.netbeans.lib.profiler.ui.graphs.GraphPanel;
import org.netbeans.lib.profiler.ui.graphs.MemoryGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.SurvivingGenerationsGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.ThreadsGraphPanel;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;


/** An IDE TopComponent to display profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public final class TelemetryWindow extends TopComponent {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static final class GraphTab extends JPanel implements ActionListener, ChartActionListener,
                                                                  SaveViewAction.ViewProvider {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final ImageIcon zoomInIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/lib/profiler/ui/resources/zoomIn.png")); //NOI18N
        private static final ImageIcon zoomOutIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/lib/profiler/ui/resources/zoomOut.png")); //NOI18N
        private static final ImageIcon zoomIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/lib/profiler/ui/resources/zoom.png")); //NOI18N
        private static final ImageIcon scaleToFitIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/lib/profiler/ui/resources/scaleToFit.png")); //NOI18N

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final GraphPanel panel;
        private final JButton scaleToFitButton;
        private final JButton zoomInButton;
        private final JButton zoomOutButton;
        private final JScrollBar scrollBar;
        private boolean lastTrackingEnd;
        private double lastScale;
        private long lastOffset;

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

            final boolean scaleToFit = panel.getChart().isFitToWindow();

            zoomInButton = new JButton(zoomInIcon);
            zoomOutButton = new JButton(zoomOutIcon);
            scaleToFitButton = new JButton(scaleToFit ? zoomIcon : scaleToFitIcon);

            scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);

            zoomInButton.setEnabled(!scaleToFit);
            zoomOutButton.setEnabled(!scaleToFit);
            scrollBar.setEnabled(!scaleToFit);

            zoomInButton.setToolTipText(ZOOM_IN_TOOLTIP);
            zoomOutButton.setToolTipText(ZOOM_OUT_TOOLTIP);
            scaleToFitButton.setToolTipText(scaleToFit ? FIXED_SCALE_TOOLTIP : SCALE_TO_FIT_TOOLTIP);

            if (!panel.getChart().containsValidData()) {
                scaleToFitButton.setEnabled(false);
                zoomInButton.setEnabled(false);
                zoomOutButton.setEnabled(false);
            }

            toolBar.setFloatable(false);
            toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE); //NOI18N

            toolBar.add(new SaveViewAction(this));
            toolBar.addSeparator();
            toolBar.add(zoomInButton);
            toolBar.add(zoomOutButton);
            toolBar.add(scaleToFitButton);

            final JPanel graphPanel = new JPanel();
            graphPanel.setLayout(new BorderLayout());
            graphPanel.setBorder(new CompoundBorder(new EmptyBorder(new Insets(0, 5, 0, 5)), new BevelBorder(BevelBorder.LOWERED)));
            graphPanel.add(panel, BorderLayout.CENTER);
            graphPanel.add(scrollBar, BorderLayout.SOUTH);

            final JPanel legendContainer = new JPanel();
            legendContainer.setLayout(new FlowLayout(FlowLayout.TRAILING));

            if (panel.getBigLegendPanel() != null) {
                legendContainer.add(panel.getBigLegendPanel());
            }

            add(toolBar, BorderLayout.NORTH);
            add(graphPanel, BorderLayout.CENTER);
            add(legendContainer, BorderLayout.SOUTH);

            zoomInButton.addActionListener(this);
            zoomOutButton.addActionListener(this);
            scaleToFitButton.addActionListener(this);

            panel.getChart().associateJScrollBar(scrollBar);
            panel.getChart().addChartActionListener(this);
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

        // --- ActionListener -------------------------------------------------------
        public void actionPerformed(final ActionEvent e) {
            final SynchronousXYChart xyChart = panel.getChart();

            if (e.getSource() == scaleToFitButton) {
                if (xyChart.isFitToWindow()) {
                    if (lastTrackingEnd) {
                        xyChart.setTrackingEnd(lastScale);
                    } else {
                        xyChart.setScaleAndOffsetX(lastScale, lastOffset);
                    }
                } else {
                    lastScale = xyChart.getScale();
                    lastOffset = xyChart.getViewOffsetX();
                    lastTrackingEnd = xyChart.isTrackingEnd();
                    xyChart.setFitToWindow();
                }

                //updateButtons();
            } else if (e.getSource() == zoomInButton) {
                xyChart.setScale(xyChart.getScale() * 2);
            } else if (e.getSource() == zoomOutButton) {
                xyChart.setScale(xyChart.getScale() / 2);
            }
        }

        public void chartDataChanged() {
            updateZoomButtons();
        }

        public void chartFitToWindowChanged() {
            if (panel.getChart().isFitToWindow()) {
                scaleToFitButton.setIcon(zoomIcon);
                scaleToFitButton.setToolTipText(FIXED_SCALE_TOOLTIP);
            } else {
                scaleToFitButton.setIcon(scaleToFitIcon);
                scaleToFitButton.setToolTipText(SCALE_TO_FIT_TOOLTIP);
            }

            updateZoomButtons();
        }

        public void chartPanned() {
        }

        public void chartTrackingEndChanged() {
        }

        public void chartZoomed() {
            updateZoomButtons();
        }

        public boolean fitsVisibleArea() {
            return true;
        }

        // --- Save Current View action support ------------------------------------
        public boolean hasView() {
            return true;
        }

        // --- ChartActionListener -------------------------------------------------
        private void updateZoomButtons() {
            if (!panel.getChart().containsValidData()) {
                scaleToFitButton.setEnabled(false);
                zoomInButton.setEnabled(false);
                zoomOutButton.setEnabled(false);
            } else {
                scaleToFitButton.setEnabled(true);

                if (panel.getChart().isFitToWindow()) {
                    zoomInButton.setEnabled(false);
                    zoomOutButton.setEnabled(false);
                } else {
                    if (panel.getChart().isMaximumZoom()) {
                        zoomInButton.setEnabled(false);
                    } else {
                        zoomInButton.setEnabled(true);
                    }

                    if (panel.getChart().isMinimumZoom()) {
                        zoomOutButton.setEnabled(false);
                    } else {
                        zoomOutButton.setEnabled(true);
                    }
                }
            }
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
    private static final String ZOOM_IN_TOOLTIP = NbBundle.getMessage(TelemetryWindow.class, "TelemetryWindow_ZoomInTooltip"); // NOI18N
    private static final String ZOOM_OUT_TOOLTIP = NbBundle.getMessage(TelemetryWindow.class, "TelemetryWindow_ZoomOutTooltip"); // NOI18N
    private static final String FIXED_SCALE_TOOLTIP = NbBundle.getMessage(TelemetryWindow.class,
                                                                          "TelemetryWindow_FixedScaleTooltip"); // NOI18N
    private static final String SCALE_TO_FIT_TOOLTIP = NbBundle.getMessage(TelemetryWindow.class,
                                                                           "TelemetryWindow_ScaleToFitTooltip"); // NOI18N
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

        heapGraph = new MemoryGraphPanel(true, Color.WHITE, ((NetBeansProfiler) Profiler.getDefault()).getMemoryXYChartModel(),
                                         null);
        generationsGraph = new SurvivingGenerationsGraphPanel(true, Color.WHITE,
                                                              ((NetBeansProfiler) Profiler.getDefault())
                                                                                                                                                                                                                                                                                                                                               .getSurvivingGenerationsXYChartModel(),
                                                              null);
        threadsStatsGraph = new ThreadsGraphPanel(true, Color.WHITE,
                                                  ((NetBeansProfiler) Profiler.getDefault()).getThreadsXYChartModel(), null);

        heapGraph.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 20, Color.WHITE));
        generationsGraph.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 20, Color.WHITE));
        threadsStatsGraph.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 20, Color.WHITE));

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
        getActionMap().put("PreviousViewAction",
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    moveToPreviousSubTab();
                }
            }); // NOI18N
        getActionMap().put("NextViewAction",
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    moveToNextSubTab();
                }
            }); // NOI18N

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
