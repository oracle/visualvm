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

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.cpu.*;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import org.netbeans.modules.profiler.actions.FindNextAction;
import org.netbeans.modules.profiler.actions.FindPreviousAction;
import org.netbeans.modules.profiler.ui.FindDialog;
import org.netbeans.modules.profiler.ui.Utils;
import org.netbeans.modules.profiler.ui.stp.ProfilingSettingsManager;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.actions.FindAction;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * A display for snapshot of CPU profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class CPUSnapshotPanel extends SnapshotPanel implements ActionListener, ChangeListener,
                                                                     SnapshotResultsWindow.FindPerformer,
                                                                     SaveViewAction.ViewProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private final class CPUActionsHandler extends CPUResUserActionsHandler.Adapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void addMethodToRoots(final String className, final String methodName, final String methodSig) {
            Project project = loadedSnapshot.getProject();
            ProfilingSettings[] projectSettings = ProfilingSettingsManager.getDefault().getProfilingSettings(project)
                                                                          .getProfilingSettings();
            List<ProfilingSettings> cpuSettings = new ArrayList();

            for (ProfilingSettings settings : projectSettings) {
                if (org.netbeans.modules.profiler.ui.stp.Utils.isCPUSettings(settings.getProfilingType())) {
                    cpuSettings.add(settings);
                }
            }

            ProfilingSettings settings = IDEUtils.selectSettings(project, ProfilingSettings.PROFILE_CPU_PART,
                                                                 cpuSettings.toArray(new ProfilingSettings[cpuSettings.size()]),
                                                                 null);

            if (settings == null) {
                return; // cancelled by the user
            }

            settings.addRootMethod(className, methodName, methodSig);

            if (cpuSettings.contains(settings)) {
                ProfilingSettingsManager.getDefault().storeProfilingSettings(projectSettings, settings, project);
            } else {
                ProfilingSettings[] newProjectSettings = new ProfilingSettings[projectSettings.length + 1];
                System.arraycopy(projectSettings, 0, newProjectSettings, 0, projectSettings.length);
                newProjectSettings[projectSettings.length] = settings;
                ProfilingSettingsManager.getDefault().storeProfilingSettings(newProjectSettings, settings, project);
            }
        }

        public void find(Object source, String findString) {
            if (source == cctPanel) {
                setFindString(findString);
                tabs.setSelectedComponent(flatPanel);
                flatPanel.selectMethod(findString);
            } else if (source == flatPanel) {
                setFindString(findString);
                tabs.setSelectedComponent(cctPanel);
                performFindFirst();
            } else if (source == combinedFlat) {
                setFindString(findString);
                //tabs.setSelectedComponent(combined);
                performFindFirst();
            } else if (source == combinedCCT) {
                setFindString(findString);
                //tabs.setSelectedComponent(combined);
                combinedFlat.selectMethod(findString);
            }
        }

        public void showReverseCallGraph(final CPUResultsSnapshot s, final int threadId, final int methodId, final int view,
                                         final int sortingColumn, final boolean sortingOrder) {
            if (backtraceView != null) {
                tabs.remove(backtraceView);
            }

            backtraceView = new ReverseCallGraphPanel(this);
            backtraceView.setDataToDisplay(s, threadId, view);
            backtraceView.setSelectedMethodId(methodId);
            backtraceView.setSorting(sortingColumn, sortingOrder);
            backtraceView.prepareResults();
            backtraceView.setFindString(cctPanel.getFindString()); // must be after backtraceView.prepareResults()!
            tabs.addTab(backtraceView.getShortTitle(), BACK_TRACES_TAB_ICON, backtraceView, backtraceView.getTitle());
            tabs.setSelectedComponent(backtraceView);
        }

        public void showSourceForMethod(final String className, final String methodName, final String methodSig) {
            NetBeansProfiler.getDefaultNB().openJavaSource(loadedSnapshot.getProject(), className, methodName, methodSig);
        }

        public void showSubtreeCallGraph(CPUResultsSnapshot s, CCTNode node, int view, int sortingColumn, boolean sortingOrder) {
            if (!(node instanceof PrestimeCPUCCTNode)) {
                return;
            }

            if (subtreeView != null) {
                tabs.remove(subtreeView);
            }

            subtreeView = new SubtreeCallGraphPanel(this);
            subtreeView.setDataToDisplay(s, (PrestimeCPUCCTNode) node, view);
            subtreeView.setSorting(sortingColumn, sortingOrder);
            subtreeView.prepareResults();
            subtreeView.setFindString(cctPanel.getFindString()); // must be after backtraceView.prepareResults()!
            tabs.addTab(subtreeView.getShortTitle(), SUBTREE_TAB_ICON, subtreeView, subtreeView.getTitle());
            tabs.setSelectedComponent(subtreeView);
        }
    }

    private final class CPUSnapshotSelectionHandler implements CPUSelectionHandler {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean cct;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private CPUSnapshotSelectionHandler(boolean cct) {
            this.cct = cct;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void methodSelected(final int threadId, final int methodId, final int view) {
            if (internalSelChange) {
                return;
            }

            if (methodId == -1) {
                return; // all methods deselected
            }

            if (cct) {
                // -1 is reserved for the all threads merged flat profile
                if ((threadId >= -1) && (combinedFlat.getCurrentThreadId() != threadId)) {
                    combinedFlat.setDataToDisplay(combinedFlat.getSnapshot(), threadId, view);
                    combinedFlat.prepareResults();
                }

                if (slaveModeDown) {
                    if (combinedCCT.getPopupFindItem() != null) {
                        combinedCCT.getPopupFindItem().setEnabled(false);
                    }

                    internalSelChange = true;
                    combinedFlat.selectMethod(methodId);
                    internalSelChange = false;
                } else {
                    if (combinedCCT.getPopupFindItem() != null) {
                        combinedCCT.getPopupFindItem().setEnabled(true);
                    }
                }
            } else {
                if (slaveModeUp) {
                    if (combinedFlat.getPopupFindItem() != null) {
                        combinedFlat.getPopupFindItem().setEnabled(false);
                    }

                    int curView = combinedFlat.getCurrentView();
                    String[] names = snapshot.getMethodClassNameAndSig(methodId, curView);
                    //          combinedCCT.setFindString(new MethodNameFormatter(names[0], names[1], names[2]).getFormattedClassAndMethod());
                    combinedCCT.setFindString(MethodNameFormatterFactory.getDefault().getFormatter()
                                                                        .formatMethodName(names[0], names[1], names[2])
                                                                        .toFormatted());
                    internalSelChange = true;
                    combinedCCT.silentlyFindFirst();
                    internalSelChange = false;
                } else {
                    if (combinedFlat.getPopupFindItem() != null) {
                        combinedFlat.getPopupFindItem().setEnabled(true);
                    }
                }
            }
        }
    }

    private class CombinedViewTracker extends FocusAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Object lastFocusOwner;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Object getLastFocusOwner() {
            return lastFocusOwner;
        }

        public void focusGained(FocusEvent e) {
            lastFocusOwner = e.getSource();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String METHODS_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_MethodsString"); // NOI18N
    private static final String CLASSES_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_ClassesString"); // NOI18N
    private static final String PACKAGES_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_PackagesString"); // NOI18N
    private static final String CALLTREE_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_CallTreeString"); // NOI18N
    private static final String HOTSPOTS_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_HotSpotsString"); // NOI18N
    private static final String COMBINED_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_CombinedString"); // NOI18N
    private static final String INFO_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_InfoString"); // NOI18N
    private static final String CALLTREE_TAB_DESCR = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                         "CPUSnapshotPanel_CallTreeTabDescr"); // NOI18N
    private static final String HOTSPOT_TAB_DESCR = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_HotSpotTabDescr"); // NOI18N
    private static final String COMBINED_TAB_DESCR = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                         "CPUSnapshotPanel_CombinedTabDescr"); // NOI18N
    private static final String INFO_TAB_DESCR = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_InfoTabDescr"); // NOI18N
    private static final String ALL_THREADS_ITEM = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_AllThreadsItem"); // NOI18N
    private static final String VIEW_LABEL_STRING = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_ViewLabelString"); // NOI18N
    private static final String TOGGLE_DOWN_TOOLTIP = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                          "CPUSnapshotPanel_ToggleDownToolTip"); // NOI18N
    private static final String TOGGLE_UP_TOOLTIP = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_ToggleUpToolTip"); // NOI18N
    private static final String PANEL_TITLE = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_PanelTitle"); // NOI18N
    private static final String FIND_IN_STATEMENT = NbBundle.getMessage(CPUSnapshotPanel.class, "CPUSnapshotPanel_FindInStatement"); // NOI18N
    private static final String AGGREGATION_COMBO_ACCESS_NAME = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                                    "CPUSnapshotPanel_AggregationComboAccessName"); // NOI18N
    private static final String AGGREGATION_COMBO_ACCESS_DESCR = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                                     "CPUSnapshotPanel_AggregationComboAccessDescr"); // NOI18N
    private static final String THREADS_COMBO_ACCESS_NAME = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                                "CPUSnapshotPanel_ThreadsComboAccessName"); // NOI18N
    private static final String THREADS_COMBO_ACCESS_DESCR = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                                 "CPUSnapshotPanel_ThreadsComboAccessDescr"); // NOI18N
    private static final String STRING_NOT_FOUND_MSG = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                           "CPUSnapshotPanel_StringNotFoundMsg"); // NOI18N
    private static final String FIND_ACTION_TOOLTIP = NbBundle.getMessage(CPUSnapshotPanel.class,
                                                                           "CPUSnapshotPanel_FindActionTooltip"); // NOI18N
    private static final String FIND_IN_HOTSPOTS_STRING = MessageFormat.format(FIND_IN_STATEMENT, new Object[] { HOTSPOTS_STRING });
    private static final String FIND_IN_CALLTREE_STRING = MessageFormat.format(FIND_IN_STATEMENT, new Object[] { CALLTREE_STRING });

    // -----
    private static final ImageIcon CLASSES_ICON = Utils.CLASS_ICON;
    private static final ImageIcon METHODS_ICON = Utils.METHODS_ICON;
    private static final ImageIcon PACKAGES_ICON = Utils.PACKAGE_ICON;
    private static final ImageIcon CALL_TREE_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/callTreeTab.png") // NOI18N
    );
    private static final ImageIcon HOTSPOTS_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/hotspotsTab.png") // NOI18N
    );
    private static final ImageIcon COMBINED_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/combinedTab.png") // NOI18N
    );
    private static final ImageIcon INFO_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/infoTab.png") // NOI18N
    );
    private static final ImageIcon BACK_TRACES_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/backTracesTab.png") // NOI18N
    );
    private static final ImageIcon SUBTREE_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/subtree.png") // NOI18N
    );
    private static final ImageIcon SLAVE_DOWN_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/slaveDown.png") // NOI18N
    );
    private static final ImageIcon SLAVE_UP_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/slaveUp.png") // NOI18N
    );
    private static final double SPLIT_HALF = 0.5d;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CCTDisplay cctPanel;
    private CCTDisplay combinedCCT;
    private CPUResultsSnapshot snapshot;
    private CombinedPanel combined;
    private CombinedViewTracker combinedViewTracker;
    private JButton findActionPresenter;
    private JButton findNextPresenter;
    private JButton findPreviousPresenter;
    private JComboBox aggregationCombo;
    private JComboBox threadsCombo;
    private JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM);
    private JToggleButton slaveToggleButtonDown;
    private JToggleButton slaveToggleButtonUp;
    private LoadedSnapshot loadedSnapshot;
    private ReverseCallGraphPanel backtraceView;
    private SaveSnapshotAction saveAction;
    private SnapshotFlatProfilePanel combinedFlat;
    private SnapshotFlatProfilePanel flatPanel;
    private SnapshotInfoPanel infoPanel;
    private SubtreeCallGraphPanel subtreeView;
    private int[] threadIds;
    private boolean internalChange = false;
    private boolean internalFilterChange = false;
    private boolean internalSelChange = false;
    private boolean slaveModeDown = true;
    private boolean slaveModeUp = true;
    private int currentAggregationMode;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CPUSnapshotPanel(final LoadedSnapshot ls, final int sortingColumn, final boolean sortingOrder) {
        this.loadedSnapshot = ls;
        this.snapshot = (CPUResultsSnapshot) ls.getSnapshot();

        CPUActionsHandler actionsHandler = new CPUActionsHandler();
        CPUSnapshotSelectionHandler combinedActionsHandlerCCT = new CPUSnapshotSelectionHandler(true);
        CPUSnapshotSelectionHandler combinedActionsHandlerFlat = new CPUSnapshotSelectionHandler(false);

        setLayout(new BorderLayout());

        flatPanel = new SnapshotFlatProfilePanel(actionsHandler);
        cctPanel = new CCTDisplay(actionsHandler);
        infoPanel = new SnapshotInfoPanel(ls);
        combinedFlat = new SnapshotFlatProfilePanel(actionsHandler, combinedActionsHandlerFlat);
        combinedCCT = new CCTDisplay(actionsHandler, combinedActionsHandlerCCT);

        flatPanel.setSorting(sortingColumn, sortingOrder);
        cctPanel.setSorting(sortingColumn, sortingOrder);
        combinedFlat.setSorting(sortingColumn, sortingOrder);
        combinedCCT.setSorting(sortingColumn, sortingOrder);

        if (cctPanel.getPopupFindItem() != null) {
            cctPanel.getPopupFindItem().setText(FIND_IN_HOTSPOTS_STRING);
            cctPanel.getPopupFindItem().setVisible(true);
        }

        if (flatPanel.getPopupFindItem() != null) {
            flatPanel.getPopupFindItem().setText(FIND_IN_CALLTREE_STRING);
            flatPanel.getPopupFindItem().setVisible(true);
        }

        if (combinedFlat.getPopupFindItem() != null) {
            combinedFlat.getPopupFindItem().setText(FIND_IN_CALLTREE_STRING);
            combinedFlat.getPopupFindItem().setVisible(true);
        }

        if (combinedCCT.getPopupFindItem() != null) {
            combinedCCT.getPopupFindItem().setText(FIND_IN_HOTSPOTS_STRING);
            combinedCCT.getPopupFindItem().setVisible(true);
        }

        flatPanel.setDataToDisplay(snapshot, -1, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
        cctPanel.setDataToDisplay(snapshot, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
        combinedFlat.setDataToDisplay(snapshot, -1, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
        combinedCCT.setDataToDisplay(snapshot, CPUResultsSnapshot.METHOD_LEVEL_VIEW);

        flatPanel.prepareResults();
        cctPanel.prepareResults();
        combinedCCT.prepareResults();
        combinedFlat.prepareResults();
        infoPanel.updateInfo();

        flatPanel.addFilterListener(new FilterComponent.FilterListener() {
                public void filterChanged() {
                    if (!internalFilterChange) {
                        internalFilterChange = true;
                        combinedFlat.setFilterValues(flatPanel.getFilterValue(), flatPanel.getFilterType());
                        internalFilterChange = false;
                    }
                }
            });

        combinedFlat.addFilterListener(new FilterComponent.FilterListener() {
                public void filterChanged() {
                    if (!internalFilterChange) {
                        internalFilterChange = true;
                        flatPanel.setFilterValues(combinedFlat.getFilterValue(), combinedFlat.getFilterType());
                        internalFilterChange = false;
                    }
                }
            });

        combined = new CombinedPanel(JSplitPane.VERTICAL_SPLIT, combinedCCT, combinedFlat) {
                public void requestFocus() {
                    if (combinedCCT != null) {
                        combinedCCT.requestFocus();
                    }
                }
            };
        // to make the split be even when resized
        combined.setResizeWeight(SPLIT_HALF);
        // to avoid border buildup
        combined.setBorder(BorderFactory.createEmptyBorder());
        combined.addComponentListener(new ComponentAdapter() { // to set the initial split correctly
                public void componentShown(final ComponentEvent e) {
                    combined.setDividerLocation(SPLIT_HALF);
                }
            });

        tabs.addTab(CALLTREE_STRING, CALL_TREE_TAB_ICON, cctPanel, CALLTREE_TAB_DESCR);
        tabs.addTab(HOTSPOTS_STRING, HOTSPOTS_TAB_ICON, flatPanel, HOTSPOT_TAB_DESCR);
        tabs.addTab(COMBINED_STRING, COMBINED_TAB_ICON, combined, COMBINED_TAB_DESCR);
        tabs.addTab(INFO_STRING, INFO_TAB_ICON, infoPanel, INFO_TAB_DESCR);
        add(tabs, BorderLayout.CENTER);

        tabs.addChangeListener(this);

        JToolBar toolBar = new JToolBar() {
            public Component add(Component comp) {
                if (comp instanceof JButton) {
                    UIUtils.fixButtonUI((JButton) comp);
                }

                return super.add(comp);
            }
        };

        toolBar.setFloatable(false);
        toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE); //NOI18N
        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        toolBar.add(saveAction = new SaveSnapshotAction(loadedSnapshot));
        toolBar.add(new ExportSnapshotAction(loadedSnapshot));
        toolBar.add(new SaveViewAction(this));

        toolBar.addSeparator();

        aggregationCombo = new JComboBox(new Object[] { METHODS_STRING, CLASSES_STRING, PACKAGES_STRING }) {
                public Dimension getMaximumSize() {
                    return new Dimension(getPreferredSize().width + 20, getPreferredSize().height);
                }
                ;
            };
        aggregationCombo.getAccessibleContext().setAccessibleName(AGGREGATION_COMBO_ACCESS_NAME);
        aggregationCombo.getAccessibleContext().setAccessibleDescription(AGGREGATION_COMBO_ACCESS_DESCR);

        currentAggregationMode = CPUResultsSnapshot.METHOD_LEVEL_VIEW;

        String[] tn = snapshot.getThreadNames();
        Object[] threadNames = new Object[tn.length + 1];
        threadNames[0] = new Object() {
                public String toString() {
                    return ALL_THREADS_ITEM;
                }
            };

        for (int i = 0; i < tn.length; i++) {
            final String tname = tn[i];
            threadNames[i + 1] = new Object() {
                    public String toString() {
                        return tname;
                    }
                };
        }

        threadIds = snapshot.getThreadIds();

        threadsCombo = new JComboBox(threadNames) {
                public Dimension getMaximumSize() {
                    return new Dimension(getPreferredSize().width + 50, getPreferredSize().height);
                }
                ;
            };
        threadsCombo.getAccessibleContext().setAccessibleName(THREADS_COMBO_ACCESS_NAME);
        threadsCombo.getAccessibleContext().setAccessibleDescription(THREADS_COMBO_ACCESS_DESCR);

        aggregationCombo.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                              final boolean isSelected, final boolean cellHasFocus) {
                    DefaultListCellRenderer dlcr = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value,
                                                                                                                index,
                                                                                                                isSelected,
                                                                                                                cellHasFocus);

                    if (value == METHODS_STRING) {
                        dlcr.setIcon(METHODS_ICON);
                    } else if (value == CLASSES_STRING) {
                        dlcr.setIcon(CLASSES_ICON);
                    } else if (value == PACKAGES_STRING) {
                        dlcr.setIcon(PACKAGES_ICON);
                    }

                    return dlcr;
                }
            });

        JLabel lab = new JLabel(VIEW_LABEL_STRING);
        lab.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        lab.setLabelFor(aggregationCombo);

        int mnemCharIndex = 0;
        lab.setDisplayedMnemonic(lab.getText().charAt(mnemCharIndex));
        lab.setDisplayedMnemonicIndex(mnemCharIndex);
        toolBar.add(lab);
        toolBar.add(aggregationCombo);
        aggregationCombo.addActionListener(this);

        // height is 0 to prevent painting grabber line
        toolBar.addSeparator(new Dimension(6, 0));

        slaveToggleButtonDown = new JToggleButton(SLAVE_DOWN_ICON);
        slaveToggleButtonDown.setSelected(slaveModeDown);
        slaveToggleButtonDown.addActionListener(this);
        slaveToggleButtonDown.setToolTipText(TOGGLE_DOWN_TOOLTIP);
        slaveToggleButtonDown.getAccessibleContext().setAccessibleName(TOGGLE_DOWN_TOOLTIP);
        toolBar.add(slaveToggleButtonDown);

        slaveToggleButtonUp = new JToggleButton(SLAVE_UP_ICON);
        slaveToggleButtonUp.setSelected(slaveModeUp);
        slaveToggleButtonUp.addActionListener(this);
        slaveToggleButtonUp.setToolTipText(TOGGLE_UP_TOOLTIP);
        slaveToggleButtonUp.getAccessibleContext().setAccessibleName(TOGGLE_UP_TOOLTIP);
        toolBar.add(slaveToggleButtonUp);

        toolBar.add(threadsCombo);
        threadsCombo.addActionListener(this);

        toolBar.addSeparator();
        
        findActionPresenter = toolBar.add(SystemAction.get(FindAction.class));
        findPreviousPresenter = toolBar.add(new FindPreviousAction(this));
        findNextPresenter = toolBar.add(new FindNextAction(this));
        
        if (findActionPresenter instanceof AbstractButton) {
            AbstractButton ab = (AbstractButton)findActionPresenter;
            ab.setIcon(Utils.FIND_ACTION_ICON);
            ab.setText(""); // NOI18N
            ab.setToolTipText(FIND_ACTION_TOOLTIP);
        }

        findActionPresenter.setEnabled(false);
        findPreviousPresenter.setEnabled(false);
        findNextPresenter.setEnabled(false);

        updateToolbar();

        add(toolBar, BorderLayout.NORTH);

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

        // support for Find Next / Find Previous using F3 / Shift + F3
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK), "FIND_PREVIOUS"); // NOI18N
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK), "FIND_PREVIOUS"); // NOI18N
        getActionMap().put("FIND_PREVIOUS",
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performFindPrevious();
                }
            }); // NOI18N
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "FIND_NEXT"); // NOI18N
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "FIND_NEXT"); // NOI18N
        getActionMap().put("FIND_NEXT",
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performFindNext();
                }
            }); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ResultsSnapshot getSnapshot() {
        return snapshot;
    }

    public String getTitle() {
        return MessageFormat.format(PANEL_TITLE, new Object[] { StringUtils.formatUserDate(new Date(snapshot.getTimeTaken())) });
    }

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        if (!(tabs.getSelectedComponent() instanceof ScreenshotProvider)) {
            return null;
        }

        return ((ScreenshotProvider) tabs.getSelectedComponent()).getCurrentViewScreenshot(onlyVisibleArea);
    }

    public String getViewName() {
        if (!(tabs.getSelectedComponent() instanceof ScreenshotProvider)) {
            return null;
        }

        String viewName = ((ScreenshotProvider) tabs.getSelectedComponent()).getDefaultViewName();

        return getDefaultSnapshotFileName(getSnapshot()) + "-" + viewName; // NOI18N
    }

    public void actionPerformed(final ActionEvent e) {
        if (internalChange) {
            return;
        }

        Object src = e.getSource();

        if (src == aggregationCombo) {
            Object sel = ((JComboBox) aggregationCombo).getSelectedItem();

            if (sel == METHODS_STRING) {
                changeView(CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            } else if (sel == CLASSES_STRING) {
                changeView(CPUResultsSnapshot.CLASS_LEVEL_VIEW);
            } else if (sel == PACKAGES_STRING) {
                changeView(CPUResultsSnapshot.PACKAGE_LEVEL_VIEW);
            }
        } else if (src == threadsCombo) {
            // this should only be possible if flatPanel is the currently selected tab
            assert (tabs.getSelectedComponent() == flatPanel);

            int tid = -1; // all threads;

            if (threadsCombo.getSelectedIndex() > 0) {
                tid = threadIds[threadsCombo.getSelectedIndex() - 1];
            }

            if (flatPanel.getCurrentThreadId() != tid) {
                flatPanel.setDataToDisplay(snapshot, tid, flatPanel.getCurrentView());
                flatPanel.prepareResults();
            }
        } else if (src == slaveToggleButtonDown) {
            slaveModeDown = slaveToggleButtonDown.isSelected();
        } else if (src == slaveToggleButtonUp) {
            slaveModeUp = slaveToggleButtonUp.isSelected();
        }
    }

    /**
     * Changes the aggregation level for the CPU Results.
     *
     * @param view one of CPUResultsSnapshot.METHOD_LEVEL_VIEW,
     *             CPUResultsSnapshot.CLASS_LEVEL_VIEW,
     *             CPUResultsSnapshot.PACKAGE_LEVEL_VIEW
     * @see CPUResultsSnapshot.METHOD_LEVEL_VIEW
     * @see CPUResultsSnapshot.CLASS_LEVEL_VIEW
     * @see CPUResultsSnapshot.PACKAGE_LEVEL_VIEW
     */
    public void changeView(final int view) {
        if (currentAggregationMode == view) {
            return;
        }

        currentAggregationMode = view;
        flatPanel.changeView(view);
        cctPanel.changeView(view);
        combinedCCT.changeView(view);
        combinedFlat.changeView(view);
        //viewTypeHasChanged();
        viewChanged(view);
    }

    public boolean fitsVisibleArea() {
        if (!(tabs.getSelectedComponent() instanceof ScreenshotProvider)) {
            return false;
        }

        return ((ScreenshotProvider) tabs.getSelectedComponent()).fitsVisibleArea();
    }

    // --- Save Current View action support --------------------------------------
    public boolean hasView() {
        return ((tabs.getSelectedComponent() != null) && tabs.getSelectedComponent() instanceof ScreenshotProvider);
    }

    // TODO use polymorphism instead of "if-else" dispatchig; curreant approach doesn't scale well
    public void performFind() {
        if (tabs.getSelectedComponent() != infoPanel) {
            String findString = FindDialog.getFindString();

            if (findString == null) {
                return; // cancelled
            }

            setFindString(findString);
            performFindFirst();
        }
    }

    public void performFindFirst() {
        // lazily initialize focus listeners once components are created
        if (combinedViewTracker == null) {
            combinedViewTracker = new CombinedViewTracker();
            combinedFlat.addResultsViewFocusListener(combinedViewTracker);
            combinedCCT.addResultsViewFocusListener(combinedViewTracker);
        }

        boolean found = false;

        if (tabs.getSelectedComponent() == cctPanel) {
            if (!cctPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = cctPanel.findFirst();
        } else if (tabs.getSelectedComponent() == flatPanel) {
            if (!flatPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = flatPanel.findFirst();
        } else if (tabs.getSelectedComponent() == combined) {
            if ((combinedViewTracker.getLastFocusOwner() == null)
                    || (combinedViewTracker.getLastFocusOwner() == combinedCCT.getResultsViewReference())) {
                if (!combinedCCT.isFindStringDefined()) {
                    String findString = FindDialog.getFindString();

                    if (findString == null) {
                        return; // cancelled
                    }

                    setFindString(findString);
                }

                found = combinedCCT.findFirst();
            } else {
                if (!combinedFlat.isFindStringDefined()) {
                    String findString = FindDialog.getFindString();

                    if (findString == null) {
                        return; // cancelled
                    }

                    setFindString(findString);
                }

                found = combinedFlat.findFirst();
            }
        } else if (tabs.getSelectedComponent() == backtraceView) {
            if (!backtraceView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = backtraceView.findFirst();
        } else if (tabs.getSelectedComponent() == subtreeView) {
            if (!subtreeView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = subtreeView.findFirst();
        }

        if (!found) {
            NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
        }
    }

    public void performFindNext() {
        // lazily initialize focus listeners once components are created
        if (combinedViewTracker == null) {
            combinedViewTracker = new CombinedViewTracker();
            combinedFlat.addResultsViewFocusListener(combinedViewTracker);
            combinedCCT.addResultsViewFocusListener(combinedViewTracker);
        }

        boolean found = false;

        if (tabs.getSelectedComponent() == cctPanel) {
            if (!cctPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = cctPanel.findNext();
        } else if (tabs.getSelectedComponent() == flatPanel) {
            if (!flatPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = flatPanel.findNext();
        } else if (tabs.getSelectedComponent() == combined) {
            if ((combinedViewTracker.getLastFocusOwner() == null)
                    || (combinedViewTracker.getLastFocusOwner() == combinedCCT.getResultsViewReference())) {
                if (!combinedCCT.isFindStringDefined()) {
                    String findString = FindDialog.getFindString();

                    if (findString == null) {
                        return; // cancelled
                    }

                    setFindString(findString);
                }

                found = combinedCCT.findNext();
            } else {
                if (!combinedFlat.isFindStringDefined()) {
                    String findString = FindDialog.getFindString();

                    if (findString == null) {
                        return; // cancelled
                    }

                    setFindString(findString);
                }

                found = combinedFlat.findNext();
            }
        } else if (tabs.getSelectedComponent() == backtraceView) {
            if (!backtraceView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = backtraceView.findNext();
        } else if (tabs.getSelectedComponent() == subtreeView) {
            if (!subtreeView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = subtreeView.findNext();
        }

        if (!found) {
            NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
        }
    }

    public void performFindPrevious() {
        // lazily initialize focus listeners once components are created
        if (combinedViewTracker == null) {
            combinedViewTracker = new CombinedViewTracker();
            combinedFlat.addResultsViewFocusListener(combinedViewTracker);
            combinedCCT.addResultsViewFocusListener(combinedViewTracker);
        }

        boolean found = false;

        if (tabs.getSelectedComponent() == cctPanel) {
            if (!cctPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = cctPanel.findPrevious();
        } else if (tabs.getSelectedComponent() == flatPanel) {
            if (!flatPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = flatPanel.findPrevious();
        } else if (tabs.getSelectedComponent() == combined) {
            if ((combinedViewTracker.getLastFocusOwner() == null)
                    || (combinedViewTracker.getLastFocusOwner() == combinedCCT.getResultsViewReference())) {
                if (!combinedCCT.isFindStringDefined()) {
                    String findString = FindDialog.getFindString();

                    if (findString == null) {
                        return; // cancelled
                    }

                    setFindString(findString);
                }

                found = combinedCCT.findPrevious();
            } else {
                if (!combinedFlat.isFindStringDefined()) {
                    String findString = FindDialog.getFindString();

                    if (findString == null) {
                        return; // cancelled
                    }

                    setFindString(findString);
                }

                found = combinedFlat.findPrevious();
            }
        } else if (tabs.getSelectedComponent() == backtraceView) {
            if (!backtraceView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = backtraceView.findPrevious();
        } else if (tabs.getSelectedComponent() == subtreeView) {
            if (!subtreeView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = subtreeView.findPrevious();
        }

        if (!found) {
            NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
        }
    }

    public void requestFocus() {
        if (cctPanel != null) {
            cctPanel.requestFocus();
        }
    }

    public void stateChanged(final ChangeEvent e) {
        updateToolbar();

        if (tabs.getSelectedComponent() != null) {
            tabs.getSelectedComponent().requestFocus(); // move focus to results table when tab is switched
        }
    }

    public void updateSavedState() {
        infoPanel.updateInfo();
        saveAction.updateState();
    }

    private String getDefaultSnapshotFileName(ResultsSnapshot snapshot) {
        return "snapshot-" + snapshot.getTimeTaken(); // NOI18N
    }

    private void setFindString(String findString) {
        cctPanel.setFindString(findString);
        flatPanel.setFindString(findString);
        combinedFlat.setFindString(findString);
        combinedCCT.setFindString(findString);

        if (backtraceView != null) {
            backtraceView.setFindString(findString);
        }

        if (subtreeView != null) {
            subtreeView.setFindString(findString);
        }
    }

    private void closeReverseCallsGraphs() {
        if (backtraceView != null) {
            tabs.remove(backtraceView);
            backtraceView = null;
        }
    }

    private void moveToNextSubTab() {
        tabs.setSelectedIndex(UIUtils.getNextSubTabIndex(tabs, tabs.getSelectedIndex()));
    }

    private void moveToPreviousSubTab() {
        tabs.setSelectedIndex(UIUtils.getPreviousSubTabIndex(tabs, tabs.getSelectedIndex()));
    }

    private void updateToolbar() {
        // threads combo is only visible on the Hotspots tab
        threadsCombo.setVisible(tabs.getSelectedComponent() == flatPanel);
        slaveToggleButtonDown.setVisible(tabs.getSelectedComponent() == combined);
        slaveToggleButtonUp.setVisible(tabs.getSelectedComponent() == combined);
        aggregationCombo.setEnabled((tabs.getSelectedComponent() != backtraceView) && (tabs.getSelectedComponent() != infoPanel)
                                    && (tabs.getSelectedComponent() != subtreeView));

        // update the toolbar if selected tab changed
        boolean findEnabled = tabs.getSelectedComponent() != infoPanel;
        findActionPresenter.setEnabled(findEnabled);
        findPreviousPresenter.setEnabled(findEnabled);
        findNextPresenter.setEnabled(findEnabled);
    }

    private void viewChanged(final int viewType) {
        viewTypeHasChanged();
        internalChange = true;

        switch (viewType) {
            case CPUResultsSnapshot.PACKAGE_LEVEL_VIEW:
                aggregationCombo.setSelectedItem(PACKAGES_STRING);

                break;
            case CPUResultsSnapshot.CLASS_LEVEL_VIEW:
                aggregationCombo.setSelectedItem(CLASSES_STRING);

                break;
            case CPUResultsSnapshot.METHOD_LEVEL_VIEW:default:
                aggregationCombo.setSelectedItem(METHODS_STRING);

                break;
        }

        internalChange = false;
    }

    private void viewTypeHasChanged() {
        cctPanel.prepareResults();
        flatPanel.prepareResults();
        combinedCCT.prepareResults();
        combinedFlat.prepareResults();
    }
}
