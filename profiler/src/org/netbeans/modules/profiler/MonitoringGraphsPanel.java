/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
import java.awt.Color;
import org.netbeans.lib.profiler.ui.graphs.MemoryGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.SurvivingGenerationsGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.ThreadsGraphPanel;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JPanel;


/** A panel containing three shall graphs for memory, threads and gc,
 * used in the Profiler control panel.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Vladislav Nemec
 * @author Jiri Sedlacek
 */
public final class MonitoringGraphsPanel extends javax.swing.JPanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final MemoryGraphPanel heapGraph;
    private final SurvivingGenerationsGraphPanel generationsGraph;
    private final ThreadsGraphPanel threadsGraph;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates new MonitoringGraphsPanel */
    public MonitoringGraphsPanel() {
        heapGraph = MemoryGraphPanel.createSmallPanel(NetBeansProfiler.getDefaultNB().
                    getVMTelemetryModels(), new AbstractAction() {
                        public void actionPerformed(final ActionEvent e) {
                            TelemetryWindow.getDefault().showHeap();
                        }
                    });
        threadsGraph = ThreadsGraphPanel.createSmallPanel(NetBeansProfiler.getDefaultNB().
                    getVMTelemetryModels(), new AbstractAction() {
                        public void actionPerformed(final ActionEvent e) {
                            TelemetryWindow.getDefault().showThreads();
                        }
                    });
        generationsGraph = SurvivingGenerationsGraphPanel.createSmallPanel(NetBeansProfiler.getDefaultNB().
                    getVMTelemetryModels(), new AbstractAction() {
                        public void actionPerformed(final ActionEvent e) {
                            TelemetryWindow.getDefault().showGC();
                        }
                    });

//        heapGraph.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
//        threadsGraph.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 10));
//        generationsGraph.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 10));

        final JPanel graphsPanel = new JPanel();
        graphsPanel.setLayout(new java.awt.GridLayout(1, 3));
//        graphsPanel.setOpaque(true);
//        graphsPanel.setBackground(GraphPanel.CHART_BACKGROUND_COLOR);
        graphsPanel.add(heapGraph);
        graphsPanel.add(generationsGraph);
        graphsPanel.add(threadsGraph);

//        final JPanel legendPanel = new JPanel();
//        legendPanel.setLayout(new java.awt.GridLayout(1, 3));
//        legendPanel.setOpaque(false);
//
//        if (heapGraph.getSmallLegendPanel() != null) {
//            final JPanel heapGraphLegend = new JPanel();
//            heapGraphLegend.setOpaque(true);
//            heapGraphLegend.setBackground(Color.WHITE);
//            heapGraphLegend.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
//            heapGraphLegend.add(heapGraph.getSmallLegendPanel());
//            legendPanel.add(heapGraphLegend);
//        }
//
//        if (generationsGraph.getSmallLegendPanel() != null) {
//            final JPanel generationsGraphLegend = new JPanel();
//            generationsGraphLegend.setOpaque(true);
//            generationsGraphLegend.setBackground(Color.WHITE);
//            generationsGraphLegend.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
//            generationsGraphLegend.add(generationsGraph.getSmallLegendPanel());
//            legendPanel.add(generationsGraphLegend);
//        }
//
//        if (threadsGraph.getSmallLegendPanel() != null) {
//            final JPanel threadsGraphLegend = new JPanel();
//            threadsGraphLegend.setOpaque(true);
//            threadsGraphLegend.setBackground(Color.WHITE);
//            threadsGraphLegend.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
//            threadsGraphLegend.add(threadsGraph.getSmallLegendPanel());
//            legendPanel.add(threadsGraphLegend);
//        }

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);

        add(graphsPanel, BorderLayout.CENTER);
//        add(legendPanel, BorderLayout.SOUTH);
    }
}
