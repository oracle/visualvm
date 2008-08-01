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

import org.netbeans.lib.profiler.ContextAware;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.results.cpu.cct.CPUCCTVisitorAdapter;
import org.netbeans.lib.profiler.results.cpu.cct.CompositeCPUCCTWalker;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MarkedCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.cpu.statistics.StatisticalModule;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MergedCategoryTrackerPanel extends StatisticalModule implements ContextAware {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class MergedCategoryTrackerModel extends CPUCCTVisitorAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final Set<String> EMPTY_SET = new HashSet<String>();
        private Collection<String> categories = new ArrayList<String>();
        private ReadWriteLock lock = new ReentrantReadWriteLock();
        private boolean inCalculation = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public MergedCategoryTrackerModel() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Set<String> getPaths(int methodId) {
            lock.readLock().lock();

            try {
                return new HashSet<String>(categories);
            } finally {
                lock.readLock().unlock();
            }
        }

        public void afterWalk() {
            lock.writeLock().unlock();
            refreshData();
        }

        public void beforeWalk() {
            lock.writeLock().lock();
            categories.clear();
        }

        public void visit(MethodCPUCCTNode node) {
            if (node.getMethodId() == getSelectedMethodId()) {
                inCalculation = true;
            }
        }

        public void visitPost(MarkedCPUCCTNode node) {
            if (inCalculation) {
                categories.add("***"); //node.getMark().getDescription());
            }
        }

        public void visitPost(MethodCPUCCTNode node) {
            if (node.getMethodId() == getSelectedMethodId()) {
                inCalculation = false;
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NO_METHOD_LABEL_TEXT = NbBundle.getMessage(MergedCategoryTrackerPanel.class,
                                                                           "MergedCategoryTrackerPanel_NoMethodLabelText"); // NOI18N
    private static final String NO_DATA_LABEL_TEXT = NbBundle.getMessage(MergedCategoryTrackerPanel.class,
                                                                         "MergedCategoryTrackerPanel_NoDataLabelText"); // NOI18N
    private static final String METHOD_CATEGORIES_STRING = NbBundle.getMessage(MergedCategoryTrackerPanel.class,
                                                                               "MergedCategoryTrackerPanel_MethodCategoriesString"); // NOI18N
    private static final String DESCR_STRING = NbBundle.getMessage(MergedCategoryTrackerPanel.class,
                                                                   "MergedCategoryTrackerPanel_DescrString"); // NOI18N
                                                                                                              // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CompositeCPUCCTWalker treeWalker;
    private JLabel noData = new JLabel(NO_DATA_LABEL_TEXT);
    private JLabel noMethods = new JLabel(NO_METHOD_LABEL_TEXT);
    private Map<Integer, List<MarkedCPUCCTNode>> pathMap = new HashMap<Integer, List<MarkedCPUCCTNode>>();
    private MergedCategoryTrackerModel model;
    private RuntimeCPUCCTNode lastAppNode = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MergedCategoryTrackerPanel
     */
    public MergedCategoryTrackerPanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setContext(ProfilerClient profilerClient) {
        model = new MergedCategoryTrackerModel();
        treeWalker = new CompositeCPUCCTWalker();
        treeWalker.add(0, model);
    }

    public void setSelectedMethodId(int methodId) {
        int lastSelectedId = getSelectedMethodId();
        super.setSelectedMethodId(methodId);

        if (lastSelectedId != methodId) {
            lastSelectedId = methodId;
            refresh(lastAppNode);
        }
    }

    public void refresh(RuntimeCPUCCTNode appNode) {
        if (appNode == null) {
            return;
        }

        if (model != null) {
            appNode.accept(treeWalker);
            lastAppNode = appNode;
        }
    }

    private void initComponents() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setName(METHOD_CATEGORIES_STRING);
        setToolTipText(DESCR_STRING);

        noMethods.setOpaque(false);
        noMethods.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0), noMethods.getBorder()));
        noData.setOpaque(false);
        noData.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0), noData.getBorder()));

        add(noMethods);
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
            final Set<String> paths = model.getPaths(getSelectedMethodId());

            if ((paths == null) || paths.isEmpty()) {
                uiUpdater = new Runnable() {
                        public void run() {
                            removeAll();
                            add(noData);
                            revalidate();
                            repaint();
                        }
                    };
            } else {
                uiUpdater = new Runnable() {
                        public void run() {
                            removeAll();

                            for (String path : paths) {
                                JPanel panel = new JPanel(new BorderLayout());
                                panel.setOpaque(false);

                                JLabel data = new JLabel(path);
                                data.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0),
                                                                                  data.getBorder()));
                                data.setOpaque(false);
                                panel.add(data, BorderLayout.WEST);
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
