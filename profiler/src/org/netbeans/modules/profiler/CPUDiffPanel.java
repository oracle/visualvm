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

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.cpu.*;
import org.netbeans.modules.profiler.actions.FindNextAction;
import org.netbeans.modules.profiler.actions.FindPreviousAction;
import org.netbeans.modules.profiler.ui.FindDialog;
import org.netbeans.modules.profiler.api.ProfilingSettingsManager;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.actions.FindAction;
import org.openide.util.actions.SystemAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.cpu.CPUResultsDiff;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;


/**
 * A display for snapshot of CPU profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
//@NbBundle.Messages({
//    "CPUSnapshotPanel_MethodsString=Methods",
//    "CPUSnapshotPanel_ClassesString=Classes",
//    "CPUSnapshotPanel_PackagesString=Packages",
//    "CPUSnapshotPanel_CallTreeString=Call Tree",
//    "CPUSnapshotPanel_HotSpotsString=Hot Spots",
//    "CPUSnapshotPanel_FindInStatement=Find in {0}",
//    "CPUSnapshotPanel_CombinedString=Combined",
//    "CPUSnapshotPanel_InfoString=Info",
//    "CPUSnapshotPanel_CallTreeTabDescr=Call Tree View - Execution call tree for application threads",
//    "CPUSnapshotPanel_HotSpotTabDescr=Hot Spots View - List of methods which the application spent most time executing",
//    "CPUSnapshotPanel_CombinedTabDescr=Combined View - Call Tree and Hot Spots",
//    "CPUSnapshotPanel_InfoTabDescr=Snapshot Information",
//    "CPUSnapshotPanel_AllThreadsItem=<All Threads>",
//    "CPUSnapshotPanel_ViewLabelString=View:",
//    "CPUSnapshotPanel_ToggleDownToolTip=When selecting item in Call Tree, automatically select corresponding row in Hot Spots.",
//    "CPUSnapshotPanel_ToggleUpToolTip=When selecting item in Hot Spots, automatically select first occurence in Call Tree. Use Find Previous/Next to see other occurences.",
//    "CPUSnapshotPanel_AggregationComboAccessName=Results aggregation level.",
//    "CPUSnapshotPanel_AggregationComboAccessDescr=Select which aggregation level will be used for showing collected results.",
//    "CPUSnapshotPanel_ThreadsComboAccessName=List of application threads.",
//    "CPUSnapshotPanel_ThreadsComboAccessDescr=Choose application thread to display collected results for the thread.",
//    "CPUSnapshotPanel_StringNotFoundMsg=String not found in results",
//    "CPUSnapshotPanel_FindActionTooltip=Find in Results... (Ctrl+F)"
//})
public final class CPUDiffPanel extends SnapshotPanel implements ActionListener, ChangeListener,
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
            }
        }

        public void showReverseCallGraph(final CPUResultsSnapshot s, final int threadId, final int methodId, final int view,
                                         final int sortingColumn, final boolean sortingOrder) {}

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
    
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Icon CLASSES_ICON = Icons.getIcon(LanguageIcons.CLASS);
    private static final Icon METHODS_ICON = Icons.getIcon(LanguageIcons.METHODS);
    private static final Icon PACKAGES_ICON = Icons.getIcon(LanguageIcons.PACKAGE);
    private static final Icon THREADS_ICON = Icons.getIcon(ProfilerIcons.THREAD);
    private static final Icon CALL_TREE_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_CALL_TREE);
    private static final Icon HOTSPOTS_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS);
    private static final Icon SUBTREE_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_SUBTREE);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CCTDisplay cctPanel;
    private CPUResultsDiff snapshot;
    private JComboBox aggregationCombo;
    private JComboBox threadsCombo;
    private LoadedSnapshot loadedSnapshot;
    private SnapshotFlatProfilePanel flatPanel;
    private SubtreeCallGraphPanel subtreeView;
    private int[] threadIds;
    private boolean internalChange = false;
    private int currentAggregationMode;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CPUDiffPanel(Lookup context, final LoadedSnapshot ls, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2, final int sortingColumn, final boolean sortingOrder) {
        this.loadedSnapshot = ls;
        this.snapshot = (CPUResultsDiff)ls.getSnapshot();

        CPUActionsHandler actionsHandler = new CPUActionsHandler();
        
        boolean sampling = ls.getSettings().getCPUProfilingType() == CommonConstants.CPU_SAMPLED;

        flatPanel = new DiffFlatProfilePanel(actionsHandler, sampling);
        cctPanel = new DiffCCTDisplay(actionsHandler, sampling);

        flatPanel.setSorting(sortingColumn, sortingOrder);
        cctPanel.setSorting(sortingColumn, sortingOrder);

        if (cctPanel.getPopupFindItem() != null) {
            cctPanel.getPopupFindItem().setText(Bundle.CPUSnapshotPanel_FindInStatement(Bundle.CPUSnapshotPanel_HotSpotsString()));
            cctPanel.getPopupFindItem().setVisible(true);
        }

        if (flatPanel.getPopupFindItem() != null) {
            flatPanel.getPopupFindItem().setText(Bundle.CPUSnapshotPanel_FindInStatement(Bundle.CPUSnapshotPanel_CallTreeString()));
            flatPanel.getPopupFindItem().setVisible(true);
        }

        flatPanel.setDataToDisplay(snapshot, -1, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
        cctPanel.setDataToDisplay(snapshot, CPUResultsSnapshot.METHOD_LEVEL_VIEW);

        flatPanel.prepareResults();
        cctPanel.prepareResults();
        
        addView(Bundle.CPUSnapshotPanel_CallTreeString(), CALL_TREE_TAB_ICON, Bundle.CPUSnapshotPanel_CallTreeTabDescr(), cctPanel, null);
        addView(Bundle.CPUSnapshotPanel_HotSpotsString(), HOTSPOTS_TAB_ICON, Bundle.CPUSnapshotPanel_HotSpotTabDescr(), flatPanel, null);

        addChangeListener(this);

        ProfilerToolbar toolBar = ProfilerToolbar.create(true);

        toolBar.add(new ExportAction(this, loadedSnapshot));
        toolBar.add(new SaveViewAction(this));

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

        toolBar.add(threadsCombo);
        threadsCombo.addActionListener(this);

        toolBar.addSeparator();
        
        ContextAwareAction a = SystemAction.get(FindAction.class);
        Component findActionPresenter = toolBar.add(a.createContextAwareInstance(context));
        toolBar.add(new FindPreviousAction(this));
        toolBar.add(new FindNextAction(this));
        
        if (findActionPresenter instanceof AbstractButton) {
            AbstractButton ab = (AbstractButton)findActionPresenter;
            ab.setIcon(Icons.getIcon(GeneralIcons.FIND));
            ab.setText(""); // NOI18N
            ab.setToolTipText(Bundle.CPUSnapshotPanel_FindActionTooltip());
        }
        
        // Filler to align rest of the toolbar to the right
        toolBar.addFiller();

        // Information about source snapshot
        final WeakReference<LoadedSnapshot>[] loadedSnapshots = new WeakReference[2];

        final String s1File = (snapshot1.getFile() == null) ? null : snapshot1.getFile().getAbsolutePath();
        final String s2File = (snapshot2.getFile() == null) ? null : snapshot2.getFile().getAbsolutePath();

        if (s1File == null) {
            loadedSnapshots[0] = new WeakReference(snapshot1);
        }

        if (s2File == null) {
            loadedSnapshots[1] = new WeakReference(snapshot2);
        }
        
        final ResultsManager rm = ResultsManager.getDefault();

        final String SNAPSHOT_1_MASK = "file:/1"; //NOI18N
        final String SNAPSHOT_2_MASK = "file:/2"; //NOI18N

        final String SNAPSHOT_1_LINK = "<a href='" + SNAPSHOT_1_MASK + "'>" //NOI18N
                                       + rm.getSnapshotDisplayName(snapshot1) + "</a>"; //NOI18N
        final String SNAPSHOT_2_LINK = "<a href='" + SNAPSHOT_2_MASK + "'>" //NOI18N
                                       + rm.getSnapshotDisplayName(snapshot2) + "</a>"; //NOI18N

        HTMLLabel descriptionLabel = new HTMLLabel(Bundle.MemoryDiffPanel_SnapshotsComparisonString(
                                                    SNAPSHOT_1_LINK, 
                                                    SNAPSHOT_2_LINK)) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

            protected void showURL(URL url) {
                LoadedSnapshot ls = null;

                if (SNAPSHOT_1_MASK.equals(url.toString())) {
                    if (s1File != null) {
                        File f = new File(s1File);
                        if (f.exists()) ls = rm.loadSnapshot(FileUtil.toFileObject(f));
                    } else {
                        ls = loadedSnapshots[0].get();
                    }
                } else if (SNAPSHOT_2_MASK.equals(url.toString())) {
                    if (s2File != null) {
                        File f = new File(s2File);
                        if (f.exists()) ls = rm.loadSnapshot(FileUtil.toFileObject(f));
                    } else {
                        ls = loadedSnapshots[1].get();
                    }
                }

                if (ls != null) {
                    SnapshotResultsWindow srw = SnapshotResultsWindow.get(ls);
                    srw.open();
                    srw.requestActive();
                } else {
                    ProfilerDialogs.displayWarning(Bundle.MemoryDiffPanel_SnapshotNotAvailableMsg());
                }
            }
        };

        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        toolBar.add(descriptionLabel);

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
        return ((selectedView != null) && (selectedView instanceof ScreenshotProvider) /*&& (selectedView != infoPanel)*/);
    }

    // TODO use polymorphism instead of "if-else" dispatchig; curreant approach doesn't scale well
    public void performFind() {
        String findString = FindDialog.getFindString();

        if (findString == null) {
            return; // cancelled
        }

        setFindString(findString);
        performFindFirst();
    }

    public void performFindFirst() {
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

    public void updateSavedState() {}

    private String getDefaultSnapshotFileName(ResultsSnapshot snapshot) {
        return "snapshot-" + snapshot.getTimeTaken(); // NOI18N
    }

    private void setFindString(String findString) {
        cctPanel.setFindString(findString);
        flatPanel.setFindString(findString);
        if (subtreeView != null) {
            subtreeView.setFindString(findString);
        }
    }

    private void updateToolbar() {
        Component selectedView = getSelectedView();
        
        // threads combo is only visible on the Hotspots tab
        threadsCombo.setVisible(selectedView == flatPanel);
        aggregationCombo.setEnabled(selectedView != subtreeView);
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
        revalidate();
        repaint();
    }
    
    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        Component selectedView = getSelectedView();
        if (selectedView instanceof CCTDisplay) { // Call tree
            cctPanel.exportData(exportedFileType,eDD,false, Bundle.CPUSnapshotPanel_CallTreeString());
        } else if (selectedView instanceof SnapshotFlatProfilePanel) { // Hot Spots
            flatPanel.exportData(exportedFileType,eDD,false, Bundle.CPUSnapshotPanel_HotSpotsString());
        } else if (selectedView instanceof SubtreeCallGraphPanel) { //Subtree
            subtreeView.exportData(exportedFileType,eDD, subtreeView.getShortTitle());
        }
    }

    public boolean hasLoadedSnapshot() {
        return false;
    }

    public boolean hasExportableView() {
        Component selectedView = getSelectedView();
        return (selectedView != null);
    }
}
