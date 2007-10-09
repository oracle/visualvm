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

package org.netbeans.modules.profiler.ui.stats;

import org.netbeans.lib.profiler.results.cpu.TimingAdjusterOld;
import org.netbeans.lib.profiler.results.cpu.cct.CPUCCTVisitorAdapter;
import org.netbeans.lib.profiler.results.cpu.cct.CompositeCPUCCTWalker;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.CategoryCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.marking.Mark;
import org.netbeans.lib.profiler.ui.cpu.statistics.StatisticalModule;
import org.netbeans.lib.profiler.ui.cpu.statistics.TimingData;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ForwardCategoryDistributionPanel extends StatisticalModule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class MarkTime {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        public static final Comparator COMPARATOR = new Comparator() {
            public int compare(Object o1, Object o2) {
                if ((o1 == null) || (o2 == null)) {
                    return 0;
                }

                if (!(o1 instanceof MarkTime && o2 instanceof MarkTime)) {
                    return 0;
                }

                if (((MarkTime) o1).time < ((MarkTime) o2).time) {
                    return 1;
                } else if (((MarkTime) o1).time > ((MarkTime) o2).time) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };


        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public Mark mark;
        public long time;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public MarkTime(final Mark mark, final long time) {
            this.mark = mark;
            this.time = time;
        }
    }

    private class Model extends CPUCCTVisitorAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Map<Mark, Long> markMap = new HashMap<Mark, Long>();
        private Mark usedMark;
        private Stack<Mark> markStack = new Stack<Mark>();
        private int inCalls;
        private int lastCalls;
        private int outCalls;
        private long time0;
        private long time1;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Map<Mark, Long> getDistribution() {
            return new HashMap<Mark, Long>(markMap);
        }

        public void afterWalk() {
            //      markStack.pop();
            //      updateTimeForMark(Mark.DEFAULT);
            refreshData();
        }

        public void beforeWalk() {
            markStack.clear();
            markMap.clear();

            usedMark = Mark.DEFAULT;
        }

        public void visit(CategoryCPUCCTNode node) {
            if (time0 > 0L) {
                // fill the timing data structures
                Long markTime = markMap.get(usedMark);

                if (markTime == null) {
                    markTime = Long.valueOf(0L);
                }

                long cleansedTime = (long) TimingAdjusterOld.getDefault().adjustTime(time0, inCalls - lastCalls, outCalls, false);

                if (cleansedTime > 0L) {
                    markMap.put(usedMark, markTime + cleansedTime);
                }
            }

            // clean up the timing helpers
            outCalls = 0;
            inCalls = 0;
            lastCalls = 0;
            time0 = 0;
            time1 = 0;

            markStack.push(usedMark);
            usedMark = node.getMark();
        }

        public void visit(MethodCPUCCTNode node) {
            if (node.getMethodId() != getSelectedMethodId()) {
                return;
            }

            time0 += node.getNetTime0();
            time1 += node.getNetTime1();
            inCalls += node.getNCalls();
            outCalls += node.getNCalls();
            lastCalls = node.getNCalls();
        }

        public void visitPost(CategoryCPUCCTNode node) {
            if (time0 > 0L) {
                // fill the timing data structures
                Long markTime = markMap.get(usedMark);

                if (markTime == null) {
                    markTime = Long.valueOf(0L);
                }

                long cleansedTime = (long) TimingAdjusterOld.getDefault().adjustTime(time0, inCalls, outCalls - lastCalls, false);

                if (cleansedTime > 0L) {
                    markMap.put(usedMark, markTime + cleansedTime);
                }
            }

            // clean up the timing helpers
            outCalls = 0;
            inCalls = 0;
            lastCalls = 0;
            time0 = 0;
            time1 = 0;

            usedMark = markStack.pop();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NO_METHOD_LABEL_TEXT = NbBundle.getMessage(ForwardCategoryDistributionPanel.class,
                                                                           "ForwardCategoryDistributionPanel_NoMethodLabelText"); // NOI18N
    private static final String NO_DATA_LABEL_TEXT = NbBundle.getMessage(ForwardCategoryDistributionPanel.class,
                                                                         "ForwardCategoryDistributionPanel_NoDataLabelText"); // NOI18N
    private static final String METHOD_CATEGORIES_STRING = NbBundle.getMessage(ForwardCategoryDistributionPanel.class,
                                                                               "ForwardCategoryDistributionPanel_MethodCategoriesString"); // NOI18N
    private static final String DESCR_STRING = NbBundle.getMessage(ForwardCategoryDistributionPanel.class,
                                                                   "ForwardCategoryDistributionPanel_DescrString"); // NOI18N
                                                                                                                    // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CompositeCPUCCTWalker walker;
    private JLabel noData = new JLabel(NO_DATA_LABEL_TEXT);
    private JLabel noMethods = new JLabel(NO_METHOD_LABEL_TEXT);
    private Model model;
    private RuntimeCPUCCTNode lastAppNode;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of ForwardCategoryDistributionPanel */
    public ForwardCategoryDistributionPanel() {
        initComponents();
        model = new Model();
        walker = new CompositeCPUCCTWalker();
        walker.add(0, model);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setSelectedMethodId(int methodId) {
        int lastSelectedId = getSelectedMethodId();
        super.setSelectedMethodId(methodId);

        if (lastSelectedId != methodId) {
            lastSelectedId = methodId;
            refresh(lastAppNode);
        }
    }

    public void refresh(RuntimeCPUCCTNode appNode) {
        if (appNode != null) {
            if (walker != null) {
                appNode.accept(walker);
            }

            lastAppNode = appNode;
        }
    }

    private void initComponents() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        noMethods.setOpaque(false);
        noMethods.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0), noMethods.getBorder()));
        noData.setOpaque(false);
        noData.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0), noData.getBorder()));

        add(noMethods);
        setName(METHOD_CATEGORIES_STRING);
        setToolTipText(DESCR_STRING);

        //    setPreferredSize(new Dimension(60, 10));
    }

    private void refreshData() {
        if (model == null) {
            return;
        }

        Runnable uiUpdater = null;

        if (getSelectedMethodId() == -1) {
            uiUpdater = new Runnable() {
                    public void run() {
                        removeAll();
                        add(noMethods);
                        revalidate();
                        repaint();
                    }
                };
        } else {
            Map<Mark, Long> catTimes = model.getDistribution();

            if ((catTimes == null) || catTimes.isEmpty()) {
                uiUpdater = new Runnable() {
                        public void run() {
                            removeAll();
                            add(noData);
                            revalidate();
                            repaint();
                        }
                    };
            } else {
                long parentTime = 0;
                long shownTime = 0;

                final List<MarkTime> shownCats = new ArrayList<MarkTime>();

                for (Map.Entry<Mark, Long> entry : catTimes.entrySet()) {
                    if (catTimes.keySet().contains(entry.getKey())) {
                        long time = entry.getValue();
                        shownTime += time;
                        shownCats.add(new MarkTime(entry.getKey(), time));
                    }
                }

                final long fullTime = (parentTime > shownTime) ? parentTime : shownTime;

                //        long restTime = fullTime - shownTime;
                //        if (restTime > 0) {
                //          shownCats.add(new MarkTime(dd.getCurrentMark(), restTime));
                //        }
                Collections.sort(shownCats, MarkTime.COMPARATOR);

                uiUpdater = new Runnable() {
                        public void run() {
                            removeAll();

                            for (MarkTime cat : shownCats) {
                                float ratio = (float) cat.time / (float) fullTime;
                                float percent = 100f * ratio;

                                JPanel panel = new JPanel(new BorderLayout());
                                panel.setOpaque(false);

                                JLabel data = new JLabel(cat.mark.description + " (" + StringUtils.floatPerCentToString(percent)
                                                         + "%)"); // NOI18N
                                data.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0),
                                                                                  data.getBorder()));
                                data.setOpaque(false);
                                panel.add(data, BorderLayout.WEST);

                                JProgressBar prgbar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
                                prgbar.setOpaque(false);
                                prgbar.setPreferredSize(new Dimension(120, data.getPreferredSize().height + 2));
                                prgbar.setMaximumSize(prgbar.getPreferredSize());
                                prgbar.setMinimumSize(prgbar.getPreferredSize());
                                prgbar.setForeground(new Color(Color.HSBtoRGB(100f, ratio, 0.7f)));
                                prgbar.setString(""); // NOI18N
                                prgbar.setStringPainted(true);
                                prgbar.setValue((int) percent);

                                JPanel prgbarContainer = new JPanel(new FlowLayout(0, 2, FlowLayout.LEADING));
                                prgbarContainer.setOpaque(false);
                                prgbarContainer.add(prgbar);
                                panel.add(prgbarContainer, BorderLayout.EAST);
                                add(panel);
                            }

                            revalidate();
                            repaint();
                        }
                    };
            }
        }

        if (EventQueue.isDispatchThread()) {
            uiUpdater.run();
        } else {
            EventQueue.invokeLater(uiUpdater);
        }
    }
}
