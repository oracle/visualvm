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

import java.util.Collection;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.cpu.*;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import org.netbeans.modules.profiler.actions.FindNextAction;
import org.netbeans.modules.profiler.actions.FindPreviousAction;
import org.netbeans.modules.profiler.ui.FindDialog;
import org.netbeans.modules.profiler.api.ProfilingSettingsManager;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.actions.FindAction;
import org.openide.util.NbBundle;
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
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.actions.CompareSnapshotsAction;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;


/**
 * A display for snapshot of CPU profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "CPUSnapshotPanel_MethodsString=Methods",
    "CPUSnapshotPanel_ClassesString=Classes",
    "CPUSnapshotPanel_PackagesString=Packages",
    "CPUSnapshotPanel_CallTreeString=Call Tree",
    "CPUSnapshotPanel_HotSpotsString=Hot Spots",
    "CPUSnapshotPanel_FindInStatement=Find in {0}",
    "CPUSnapshotPanel_CombinedString=Combined",
    "CPUSnapshotPanel_InfoString=Info",
    "CPUSnapshotPanel_CallTreeTabDescr=Call Tree View - Execution call tree for application threads",
    "CPUSnapshotPanel_HotSpotTabDescr=Hot Spots View - List of methods which the application spent most time executing",
    "CPUSnapshotPanel_CombinedTabDescr=Combined View - Call Tree and Hot Spots",
    "CPUSnapshotPanel_InfoTabDescr=Snapshot Information",
    "CPUSnapshotPanel_AllThreadsItem=<All Threads>",
    "CPUSnapshotPanel_ViewLabelString=View:",
    "CPUSnapshotPanel_ToggleDownToolTip=When selecting item in Call Tree, automatically select corresponding row in Hot Spots.",
    "CPUSnapshotPanel_ToggleUpToolTip=When selecting item in Hot Spots, automatically select first occurence in Call Tree. Use Find Previous/Next to see other occurences.",
    "CPUSnapshotPanel_AggregationComboAccessName=Results aggregation level.",
    "CPUSnapshotPanel_AggregationComboAccessDescr=Select which aggregation level will be used for showing collected results.",
    "CPUSnapshotPanel_ThreadsComboAccessName=List of application threads.",
    "CPUSnapshotPanel_ThreadsComboAccessDescr=Choose application thread to display collected results for the thread.",
    "CPUSnapshotPanel_StringNotFoundMsg=String not found in results",
    "CPUSnapshotPanel_FindActionTooltip=Find in Results... (Ctrl+F)"
})
public final class CPUSnapshotPanel extends SnapshotPanel implements ActionListener, ChangeListener,
                                                                     SnapshotResultsWindow.FindPerformer,
                                                                     SaveViewAction.ViewProvider, ExportAction.ExportProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private final class CPUActionsHandler extends CPUResUserActionsHandler.Adapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void addMethodToRoots(final String className, final String methodName, final String methodSig) {
            ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                @Override
                public void run() {
                    final Lookup.Provider project = loadedSnapshot.getProject();
                    final ProfilingSettings[] projectSettings = ProfilingSettingsManager.getProfilingSettings(project)
                                                                                .getProfilingSettings();
                    final List<ProfilingSettings> cpuSettings = new ArrayList();

                    for (ProfilingSettings settings : projectSettings) {
                        if (ProfilingSettings.isCPUSettings(settings.getProfilingType())) {
                            cpuSettings.add(settings);
                        }
                    }

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            final ProfilingSettings settings = IDEUtils.selectSettings(ProfilingSettings.PROFILE_CPU_PART,
                                                                cpuSettings.toArray(new ProfilingSettings[cpuSettings.size()]),
                                                                null);

                            if (settings == null) {
                                return; // cancelled by the user
                            }
                            
                            ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {

                                @Override
                                public void run() {
                                    settings.addRootMethod(className, methodName, methodSig);

                                    if (cpuSettings.contains(settings)) {
                                        ProfilingSettingsManager.storeProfilingSettings(projectSettings, settings, project);
                                    } else {
                                        ProfilingSettings[] newProjectSettings = new ProfilingSettings[projectSettings.length + 1];
                                        System.arraycopy(projectSettings, 0, newProjectSettings, 0, projectSettings.length);
                                        newProjectSettings[projectSettings.length] = settings;
                                        ProfilingSettingsManager.storeProfilingSettings(newProjectSettings, settings, project);
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }

        public void find(Object source, String findString) {
            if (source == cctPanel) {
                setFindString(findString);
                selectView(flatPanel);
                flatPanel.selectMethod(findString);
            } else if (source == flatPanel) {
                setFindString(findString);
                selectView(cctPanel);
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
                removeView(backtraceView);
            }

            backtraceView = new ReverseCallGraphPanel(this);
            backtraceView.setDataToDisplay(s, threadId, view);
            backtraceView.setSelectedMethodId(methodId);
            backtraceView.setSorting(sortingColumn, sortingOrder);
            backtraceView.prepareResults();
            backtraceView.setFindString(cctPanel.getFindString()); // must be after backtraceView.prepareResults()!
            addView(backtraceView.getShortTitle(), BACK_TRACES_TAB_ICON, backtraceView.getTitle(), backtraceView, null);
            selectView(backtraceView);
        }

        public void showSourceForMethod(final String className, final String methodName, final String methodSig) {
            GoToSource.openSource(loadedSnapshot.getProject(), className, methodName, methodSig);
        }

        public void showSubtreeCallGraph(CPUResultsSnapshot s, CCTNode node, int view, int sortingColumn, boolean sortingOrder) {
            if (!(node instanceof PrestimeCPUCCTNode)) {
                return;
            }

            if (subtreeView != null) {
                removeView(subtreeView);
            }

            subtreeView = new SubtreeCallGraphPanel(this);
            subtreeView.setDataToDisplay(s, (PrestimeCPUCCTNode) node, view);
            subtreeView.setSorting(sortingColumn, sortingOrder);
            subtreeView.prepareResults();
            subtreeView.setFindString(cctPanel.getFindString()); // must be after backtraceView.prepareResults()!
            addView(subtreeView.getShortTitle(), SUBTREE_TAB_ICON, subtreeView.getTitle(), subtreeView, null);
            selectView(subtreeView);
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

    private static class CombinedViewTracker extends FocusAdapter {
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
    
    private class CustomCCTDisplay extends CCTDisplay {
        private CustomCCTDisplay(CPUResUserActionsHandler actionsHandler, boolean sampling) {
            super(actionsHandler, sampling);
        }

        private CustomCCTDisplay(CPUResUserActionsHandler actionsHandler, CPUSelectionHandler selectionHandler, boolean sampling) {
            super(actionsHandler, selectionHandler, sampling);
        }

        protected JPopupMenu createPopupMenu() {
            JPopupMenu popup = super.createPopupMenu();
            enhancePopupMenu(popup,this);
            return popup;
        }

        protected void enableDisablePopup(PrestimeCPUCCTNode node) {
            super.enableDisablePopup(node);
            CPUSnapshotPanel.this.enableDisablePopup(node);
        }
        
    }
    
    public interface CCTPopupEnhancer {
        public void enhancePopup(JPopupMenu popup, LoadedSnapshot snapshot, CCTDisplay cctDisplay);
        public void enableDisablePopup(LoadedSnapshot snapshot, PrestimeCPUCCTNode node);
    }
    
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Icon CLASSES_ICON = Icons.getIcon(LanguageIcons.CLASS);
    private static final Icon METHODS_ICON = Icons.getIcon(LanguageIcons.METHODS);
    private static final Icon PACKAGES_ICON = Icons.getIcon(LanguageIcons.PACKAGE);
    private static final Icon THREADS_ICON = Icons.getIcon(ProfilerIcons.THREAD);
    private static final Icon CALL_TREE_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_CALL_TREE);
    private static final Icon HOTSPOTS_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS);
    private static final Icon COMBINED_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_COMBINED);
    private static final Icon INFO_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_INFO);
    private static final Icon BACK_TRACES_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_BACK_TRACES);
    private static final Icon SUBTREE_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_SUBTREE);
    private static final Icon SLAVE_DOWN_ICON = Icons.getIcon(GeneralIcons.SLAVE_DOWN);
    private static final Icon SLAVE_UP_ICON = Icons.getIcon(GeneralIcons.SLAVE_UP);
    private static final double SPLIT_HALF = 0.5d;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CCTDisplay cctPanel;
    private CCTDisplay combinedCCT;
    private CPUResultsSnapshot snapshot;
    private CombinedPanel combined;
    private CombinedViewTracker combinedViewTracker;
    private Component findActionPresenter;
    private Component findNextPresenter;
    private Component findPreviousPresenter;
    private JComboBox aggregationCombo;
    private JComboBox threadsCombo;
    private JToggleButton slaveToggleButtonDown;
    private JToggleButton slaveToggleButtonUp;
    private LoadedSnapshot loadedSnapshot;
    private ReverseCallGraphPanel backtraceView;
    private SaveSnapshotAction saveAction;
    private SaveViewAction saveViewAction;
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

    public CPUSnapshotPanel(Lookup context, final LoadedSnapshot ls, final int sortingColumn, final boolean sortingOrder) {
        this.loadedSnapshot = ls;
        this.snapshot = (CPUResultsSnapshot) ls.getSnapshot();

        CPUActionsHandler actionsHandler = new CPUActionsHandler();
        CPUSnapshotSelectionHandler combinedActionsHandlerCCT = new CPUSnapshotSelectionHandler(true);
        CPUSnapshotSelectionHandler combinedActionsHandlerFlat = new CPUSnapshotSelectionHandler(false);
        
        boolean sampling = ls.getSettings().getCPUProfilingType() == CommonConstants.CPU_SAMPLED;

        flatPanel = new SnapshotFlatProfilePanel(actionsHandler, sampling);
        cctPanel = new CustomCCTDisplay(actionsHandler, sampling);
        infoPanel = new SnapshotInfoPanel(ls);
        combinedFlat = new SnapshotFlatProfilePanel(actionsHandler, combinedActionsHandlerFlat, sampling);
        combinedCCT = new CustomCCTDisplay(actionsHandler, combinedActionsHandlerCCT, sampling);

        flatPanel.setSorting(sortingColumn, sortingOrder);
        cctPanel.setSorting(sortingColumn, sortingOrder);
        combinedFlat.setSorting(sortingColumn, sortingOrder);
        combinedCCT.setSorting(sortingColumn, sortingOrder);

        if (cctPanel.getPopupFindItem() != null) {
            cctPanel.getPopupFindItem().setText(Bundle.CPUSnapshotPanel_FindInStatement(Bundle.CPUSnapshotPanel_HotSpotsString()));
            cctPanel.getPopupFindItem().setVisible(true);
        }

        if (flatPanel.getPopupFindItem() != null) {
            flatPanel.getPopupFindItem().setText(Bundle.CPUSnapshotPanel_FindInStatement(Bundle.CPUSnapshotPanel_CallTreeString()));
            flatPanel.getPopupFindItem().setVisible(true);
        }

        if (combinedFlat.getPopupFindItem() != null) {
            combinedFlat.getPopupFindItem().setText(Bundle.CPUSnapshotPanel_FindInStatement(Bundle.CPUSnapshotPanel_CallTreeString()));
            combinedFlat.getPopupFindItem().setVisible(true);
        }

        if (combinedCCT.getPopupFindItem() != null) {
            combinedCCT.getPopupFindItem().setText(Bundle.CPUSnapshotPanel_FindInStatement(Bundle.CPUSnapshotPanel_HotSpotsString()));
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

        flatPanel.addFilterListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (!internalFilterChange) {
                        internalFilterChange = true;
                        combinedFlat.setFilterValues(flatPanel.getFilterValue(), flatPanel.getFilterType());
                        internalFilterChange = false;
                    }
                }
            });

        combinedFlat.addFilterListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
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
        
        addView(Bundle.CPUSnapshotPanel_CallTreeString(), CALL_TREE_TAB_ICON, Bundle.CPUSnapshotPanel_CallTreeTabDescr(), cctPanel, null);
        addView(Bundle.CPUSnapshotPanel_HotSpotsString(), HOTSPOTS_TAB_ICON, Bundle.CPUSnapshotPanel_HotSpotTabDescr(), flatPanel, null);
        addView(Bundle.CPUSnapshotPanel_CombinedString(), COMBINED_TAB_ICON, Bundle.CPUSnapshotPanel_CombinedTabDescr(), combined, null);
        addView(Bundle.CPUSnapshotPanel_InfoString(), INFO_TAB_ICON, Bundle.CPUSnapshotPanel_InfoTabDescr(), infoPanel, null);

        addChangeListener(this);

        ProfilerToolbar toolBar = ProfilerToolbar.create(true);

        toolBar.add(saveAction = new SaveSnapshotAction(loadedSnapshot));
        toolBar.add(new ExportAction(this, loadedSnapshot));
        toolBar.add(saveViewAction = new SaveViewAction(this));

        toolBar.addSeparator();

        aggregationCombo = new JComboBox(new Object[] { 
            Bundle.CPUSnapshotPanel_MethodsString(), 
            Bundle.CPUSnapshotPanel_ClassesString(), 
            Bundle.CPUSnapshotPanel_PackagesString()}) {
                public Dimension getMaximumSize() {
                    return new Dimension(getPreferredSize().width + 20, getPreferredSize().height);
                }
                ;
            };
        aggregationCombo.getAccessibleContext().setAccessibleName(Bundle.CPUSnapshotPanel_AggregationComboAccessName());
        aggregationCombo.getAccessibleContext().setAccessibleDescription(Bundle.CPUSnapshotPanel_AggregationComboAccessDescr());

        currentAggregationMode = CPUResultsSnapshot.METHOD_LEVEL_VIEW;

        String[] tn = snapshot.getThreadNames();
        Object[] threadNames = new Object[tn.length + 1];
        threadNames[0] = new Object() {
                public String toString() {
                    return Bundle.CPUSnapshotPanel_AllThreadsItem();
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
                public Dimension getMinimumSize() {
                    Dimension d = super.getMinimumSize();
                    d.width = 1;
                    return d;
                }
                public Dimension getMaximumSize() {
                    Dimension d = super.getPreferredSize();
                    d.width += 50;
                    return d;
                }
            };
        threadsCombo.getAccessibleContext().setAccessibleName(Bundle.CPUSnapshotPanel_ThreadsComboAccessName());
        threadsCombo.getAccessibleContext().setAccessibleDescription(Bundle.CPUSnapshotPanel_ThreadsComboAccessDescr());

        aggregationCombo.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                              final boolean isSelected, final boolean cellHasFocus) {
                    DefaultListCellRenderer dlcr = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value,
                                                                                                                index,
                                                                                                                isSelected,
                                                                                                                cellHasFocus);

                    if (Bundle.CPUSnapshotPanel_MethodsString().equals(value)) {
                        dlcr.setIcon(METHODS_ICON);
                    } else if (Bundle.CPUSnapshotPanel_ClassesString().equals(value)) {
                        dlcr.setIcon(CLASSES_ICON);
                    } else if (Bundle.CPUSnapshotPanel_PackagesString().equals(value)) {
                        dlcr.setIcon(PACKAGES_ICON);
                    }

                    return dlcr;
                }
            });
        threadsCombo.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                              final boolean isSelected, final boolean cellHasFocus) {
                    DefaultListCellRenderer dlcr = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value,
                                                                                                                index,
                                                                                                                isSelected,
                                                                                                                cellHasFocus);

                    if (Bundle.CPUSnapshotPanel_AllThreadsItem().equals(value.toString())) {
                        dlcr.setIcon(null);
                    } else {
                        dlcr.setIcon(THREADS_ICON);
                    }

                    return dlcr;
                }
            });

        JLabel lab = new JLabel(Bundle.CPUSnapshotPanel_ViewLabelString());
        lab.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        lab.setLabelFor(aggregationCombo);

        int mnemCharIndex = 0;
        lab.setDisplayedMnemonic(lab.getText().charAt(mnemCharIndex));
        lab.setDisplayedMnemonicIndex(mnemCharIndex);
        toolBar.add(lab);
        toolBar.add(aggregationCombo);
        aggregationCombo.addActionListener(this);

        toolBar.addSpace(6);

        slaveToggleButtonDown = new JToggleButton(SLAVE_DOWN_ICON);
        slaveToggleButtonDown.setSelected(slaveModeDown);
        slaveToggleButtonDown.addActionListener(this);
        slaveToggleButtonDown.setToolTipText(Bundle.CPUSnapshotPanel_ToggleDownToolTip());
        slaveToggleButtonDown.getAccessibleContext().setAccessibleName(Bundle.CPUSnapshotPanel_ToggleDownToolTip());
        toolBar.add(slaveToggleButtonDown);

        slaveToggleButtonUp = new JToggleButton(SLAVE_UP_ICON);
        slaveToggleButtonUp.setSelected(slaveModeUp);
        slaveToggleButtonUp.addActionListener(this);
        slaveToggleButtonUp.setToolTipText(Bundle.CPUSnapshotPanel_ToggleUpToolTip());
        slaveToggleButtonUp.getAccessibleContext().setAccessibleName(Bundle.CPUSnapshotPanel_ToggleUpToolTip());
        toolBar.add(slaveToggleButtonUp);

        toolBar.add(threadsCombo);
        threadsCombo.addActionListener(this);

        toolBar.addSeparator();
        
        ContextAwareAction a = SystemAction.get(FindAction.class);
        findActionPresenter = toolBar.add(a.createContextAwareInstance(context));
        findPreviousPresenter = toolBar.add(new FindPreviousAction(this));
        findNextPresenter = toolBar.add(new FindNextAction(this));
        
        if (findActionPresenter instanceof AbstractButton) {
            AbstractButton ab = (AbstractButton)findActionPresenter;
            ab.setIcon(Icons.getIcon(GeneralIcons.FIND));
            ab.setText(""); // NOI18N
            ab.setToolTipText(Bundle.CPUSnapshotPanel_FindActionTooltip());
        }

        findActionPresenter.setEnabled(false);
        findPreviousPresenter.setEnabled(false);
        findNextPresenter.setEnabled(false);
        
        toolBar.addSeparator();
        toolBar.add(new CompareSnapshotsAction(ls));

        updateToolbar();
        setMainToolbar(toolBar.getComponent());

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

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        Component selectedView = getSelectedView();
        if (!(selectedView instanceof ScreenshotProvider)) {
            return null;
        }

        return ((ScreenshotProvider) selectedView).getCurrentViewScreenshot(onlyVisibleArea);
    }

    public String getViewName() {
        Component selectedView = getSelectedView();
        if (!(selectedView instanceof ScreenshotProvider)) {
            return null;
        }

        String viewName = ((ScreenshotProvider) selectedView).getDefaultViewName();

        return getDefaultSnapshotFileName(getSnapshot()) + "-" + viewName; // NOI18N
    }

    public void actionPerformed(final ActionEvent e) {
        if (internalChange) {
            return;
        }

        Object src = e.getSource();

        if (src == aggregationCombo) {
            Object sel = ((JComboBox) aggregationCombo).getSelectedItem();

            if (Bundle.CPUSnapshotPanel_MethodsString().equals(sel)) {
                changeView(CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            } else if (Bundle.CPUSnapshotPanel_ClassesString().equals(sel)) {
                changeView(CPUResultsSnapshot.CLASS_LEVEL_VIEW);
            } else if (Bundle.CPUSnapshotPanel_PackagesString().equals(sel)) {
                changeView(CPUResultsSnapshot.PACKAGE_LEVEL_VIEW);
            }
        } else if (src == threadsCombo) {
            // this should only be possible if flatPanel is the currently selected tab
            assert (getSelectedView() == flatPanel);

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
        flatPanel.clearSelection();
        flatPanel.changeView(view);
        cctPanel.clearSelection();
        cctPanel.changeView(view);
        combinedCCT.clearSelection();
        combinedCCT.changeView(view);
        combinedFlat.clearSelection();
        combinedFlat.changeView(view);
        //viewTypeHasChanged();
        viewChanged(view);
    }

    public boolean fitsVisibleArea() {
        Component selectedView = getSelectedView();
        if (!(selectedView instanceof ScreenshotProvider)) {
            return false;
        }

        return ((ScreenshotProvider) selectedView).fitsVisibleArea();
    }

    // --- Save Current View action support --------------------------------------
    public boolean hasView() {
        Component selectedView = getSelectedView();
        return ((selectedView != null) && (selectedView instanceof ScreenshotProvider) && (selectedView != infoPanel));
    }

    // TODO use polymorphism instead of "if-else" dispatchig; curreant approach doesn't scale well
    public void performFind() {
        if (getSelectedView() != infoPanel) {
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

        Component selectedView = getSelectedView();
        if (selectedView == cctPanel) {
            if (!cctPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = cctPanel.findFirst();
        } else if (selectedView == flatPanel) {
            if (!flatPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = flatPanel.findFirst();
        } else if (selectedView == combined) {
            if ((combinedViewTracker.getLastFocusOwner() == null)
                    || (combinedViewTracker.getLastFocusOwner() == combinedFlat.getResultsViewReference())) {
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
        } else if (selectedView == backtraceView) {
            if (!backtraceView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = backtraceView.findFirst();
        } else if (selectedView == subtreeView) {
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
            ProfilerDialogs.displayInfo(Bundle.CPUSnapshotPanel_StringNotFoundMsg());
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

        Component selectedView = getSelectedView();
        if (selectedView == cctPanel) {
            if (!cctPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = cctPanel.findNext();
        } else if (selectedView == flatPanel) {
            if (!flatPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = flatPanel.findNext();
        } else if (selectedView == combined) {
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
        } else if (selectedView == backtraceView) {
            if (!backtraceView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = backtraceView.findNext();
        } else if (selectedView == subtreeView) {
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
            ProfilerDialogs.displayInfo(Bundle.CPUSnapshotPanel_StringNotFoundMsg());
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

        Component selectedView = getSelectedView();
        if (selectedView == cctPanel) {
            if (!cctPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = cctPanel.findPrevious();
        } else if (selectedView == flatPanel) {
            if (!flatPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = flatPanel.findPrevious();
        } else if (selectedView == combined) {
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
        } else if (selectedView == backtraceView) {
            if (!backtraceView.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                setFindString(findString);
            }

            found = backtraceView.findPrevious();
        } else if (selectedView == subtreeView) {
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
            ProfilerDialogs.displayInfo(Bundle.CPUSnapshotPanel_StringNotFoundMsg());
        }
    }

    public void requestFocus() {
        if (cctPanel != null) {
            cctPanel.requestFocus();
        }
    }

    public void stateChanged(final ChangeEvent e) {
        updateToolbar();

        Component selectedView = getSelectedView();
        if (selectedView != null) {
            selectedView.requestFocus(); // move focus to results table when tab is switched
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
            removeView(backtraceView);
            backtraceView = null;
        }
    }

    private void updateToolbar() {
        Component selectedView = getSelectedView();
        
        // threads combo is only visible on the Hotspots tab
        threadsCombo.setVisible(selectedView == flatPanel);
        slaveToggleButtonDown.setVisible(selectedView == combined);
        slaveToggleButtonUp.setVisible(selectedView == combined);
        aggregationCombo.setEnabled((selectedView != backtraceView) && (selectedView != infoPanel)
                                    && (selectedView != subtreeView));

        // update the toolbar if selected tab changed
        boolean findEnabled = selectedView != infoPanel;
        saveViewAction.setEnabled(findEnabled);
        findActionPresenter.setEnabled(findEnabled);
        findPreviousPresenter.setEnabled(findEnabled);
        findNextPresenter.setEnabled(findEnabled);
    }

    private void viewChanged(final int viewType) {
        viewTypeHasChanged();
        internalChange = true;

        switch (viewType) {
            case CPUResultsSnapshot.PACKAGE_LEVEL_VIEW:
                aggregationCombo.setSelectedItem(Bundle.CPUSnapshotPanel_PackagesString());

                break;
            case CPUResultsSnapshot.CLASS_LEVEL_VIEW:
                aggregationCombo.setSelectedItem(Bundle.CPUSnapshotPanel_ClassesString());

                break;
            case CPUResultsSnapshot.METHOD_LEVEL_VIEW:default:
                aggregationCombo.setSelectedItem(Bundle.CPUSnapshotPanel_MethodsString());

                break;
        }

        internalChange = false;
    }

    private void viewTypeHasChanged() {
        cctPanel.prepareResults();
        flatPanel.prepareResults();
        combinedCCT.prepareResults();
        combinedFlat.prepareResults();
        revalidate();
        repaint();
    }

    private void enhancePopupMenu(JPopupMenu popup, CCTDisplay customCCTDisplay) {
        Collection<? extends CCTPopupEnhancer> col = Lookup.getDefault().lookupAll(CCTPopupEnhancer.class);
        for(CCTPopupEnhancer en : col) {
            en.enhancePopup(popup,loadedSnapshot,customCCTDisplay);
        }
    }

    private void enableDisablePopup(PrestimeCPUCCTNode node) {
        Collection<? extends CCTPopupEnhancer> col = Lookup.getDefault().lookupAll(CCTPopupEnhancer.class);
        for(CCTPopupEnhancer en : col) {
            en.enableDisablePopup(loadedSnapshot,node);
        }
    }
    
    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        Component selectedView = getSelectedView();
        if (selectedView instanceof CCTDisplay) { // Call tree
            cctPanel.exportData(exportedFileType,eDD,false, Bundle.CPUSnapshotPanel_CallTreeString());
        } else if (selectedView instanceof SnapshotFlatProfilePanel) { // Hot Spots
            flatPanel.exportData(exportedFileType,eDD,false, Bundle.CPUSnapshotPanel_HotSpotsString());
        } else if (selectedView instanceof SubtreeCallGraphPanel) { //Subtree
            subtreeView.exportData(exportedFileType,eDD, subtreeView.getShortTitle());
        } else if (selectedView instanceof ReverseCallGraphPanel) { //Back Trace
            backtraceView.exportData(exportedFileType,eDD, backtraceView.getShortTitle());
        } else if (selectedView==combined) { // Combined
            combined.exportData(exportedFileType,eDD, Bundle.CPUSnapshotPanel_CombinedString());
        }
    }

    public boolean hasLoadedSnapshot() {
        return true;
    }

    public boolean hasExportableView() {
        Component selectedView = getSelectedView();
        return ((selectedView != null) && (selectedView!=infoPanel));
    }
}
