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

package org.netbeans.modules.profiler.ui.stats.drilldown;

import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.charts.PieChart;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.cpu.StatisticsPanel;
import org.netbeans.lib.profiler.ui.cpu.statistics.StatisticalModule;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.netbeans.modules.profiler.categories.Category;


/**
 *
 * @author Jaroslav Bachorik
 */
public class DrillDownPanel extends StatisticalModule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DrillDownListener listener = new DrillDownListener() {
        public void dataChanged() {
        }

        public void drillDownPathChanged(List list) {
            updateCrumbNav();
        }
    };

    private HTMLTextArea crumbNav;
    private DrillDown ddModel;
    private PieChart pieChart;
    private ProjectPieChartModel pieModel = null;
    private StatisticsPanel panel = null;
    private String lastNavigableCategoryId = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of DrillDownPanel */
    public DrillDownPanel(DrillDown model) {
        ddModel = model;
        ddModel.addListener(listener);
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void addSnippet(StatisticalModule component) {
        panel.addSnippet(component);
    }

    public void pause() {
        //    ddModel.pause();
    }

    public void refresh(RuntimeCPUCCTNode appNode) {
        // TODO
    }

    public void removeSnippet(StatisticalModule component) {
        panel.removeSnippet(component);
    }

    public void resume() {
        //    ddModel.resume();
    }

    private synchronized HTMLTextArea getCrumbNav() {
        if (crumbNav == null) {
            crumbNav = new HTMLTextArea();
            crumbNav.setOpaque(false);
            crumbNav.addHyperlinkListener(new HyperlinkListener() {
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        String query = e.getURL().getQuery();

                        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                            return;
                        }

                        int index = e.getURL().getQuery().lastIndexOf('='); // NOI18N

                        if (index <= -1) {
                            return;
                        }

                        ddModel.drillup(query.substring(index + 1));
                    }
                });
            updateCrumbNav();
        }

        return crumbNav;
    }

    private synchronized PieChart getPieChart() {
        if (pieChart == null) {
            pieChart = new PieChart();
            pieModel = new ProjectPieChartModel(ddModel);
            pieChart.setModel(pieModel);
        }

        return pieChart;
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());
        panel = new StatisticsPanel(getCrumbNav(), getPieChart(),
                                    new Runnable() {
                public void run() {
                    navigateOneLevelBack();
                }
            });
        panel.addListener(new StatisticsPanel.Listener() {
                public void itemClicked(int itemIndex) {
                    pieModel.drilldown(itemIndex);
                }
            });
        add(panel, BorderLayout.CENTER);
    }

    private void navigateOneLevelBack() {
        if (lastNavigableCategoryId != null) {
            ddModel.drillup(lastNavigableCategoryId);
        }
    }

    private synchronized void updateCrumbNav() {
        StringBuilder sb = new StringBuilder();
        lastNavigableCategoryId = null;

        int counter = ddModel.getDrillDownPath().size() - 1;
        for (Category category : ddModel.getDrillDownPath()) {
            if (sb.length() > 0) {
                sb.append("/");
            }
            if (counter-- > 0) {
                sb.append("<a href=\"http://localhost/category?id=").append(category.getId()).append("\">").append(category.getLabel())
                  .append("</a>"); // NOI18N
                lastNavigableCategoryId = category.getId();
            } else {
                sb.append(category.getLabel());
            }

            crumbNav.setText(sb.toString());
        }
    }
}
