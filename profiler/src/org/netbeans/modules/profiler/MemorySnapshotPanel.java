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

import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.memory.*;
import org.netbeans.modules.profiler.actions.CompareSnapshotsAction;
import org.netbeans.modules.profiler.actions.FindNextAction;
import org.netbeans.modules.profiler.actions.FindPreviousAction;
import org.netbeans.modules.profiler.ui.FindDialog;
import org.openide.actions.FindAction;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.utils.VMUtils;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;


/**
 * A display for snapshot of CPU profiling results
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "MemorySnapshotPanel_MemoryResultsTabName=Memory Results",
    "MemorySnapshotPanel_StackTracesTabName=Allocation Stack Traces",
    "MemorySnapshotPanel_InfoTabName=Info",
    "MemorySnapshotPanel_MemoryResultsTabDescr=Memory Results - Allocated objects and memory sizes",
    "MemorySnapshotPanel_StackTracesTabDescr=Reverse call trees for object allocations",
    "MemorySnapshotPanel_InfoTabDescr=Snapshot Information",
    "MemorySnapshotPanel_StringNotFoundMsg=String not found in results",
    "MemorySnapshotPanel_FindActionTooltip=Find in Results... (Ctrl+F)"
})
public class MemorySnapshotPanel extends SnapshotPanel implements ChangeListener, SnapshotResultsWindow.FindPerformer,
                                                                  SaveViewAction.ViewProvider, ExportAction.ExportProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SnapshotActionsHandler implements MemoryResUserActionsHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void showSourceForMethod(String className, String methodName, String methodSig) {
            // Check if primitive type/array
            if ((methodName == null && methodSig == null) && (VMUtils.isVMPrimitiveType(className) ||
                 VMUtils.isPrimitiveType(className))) ProfilerDialogs.displayWarning(CANNOT_SHOW_PRIMITIVE_SRC_MSG);
            // Check if allocated by reflection
            else if (PresoObjAllocCCTNode.VM_ALLOC_CLASS.equals(className) && PresoObjAllocCCTNode.VM_ALLOC_METHOD.equals(methodName))
                     ProfilerDialogs.displayWarning(CANNOT_SHOW_REFLECTION_SRC_MSG);
            // Display source
            else GoToSource.openSource(project, className, methodName, methodSig);
        }

        public void showStacksForClass(int selectedClassId, int sortingColumn, boolean sortingOrder) {
            displayStacksForClass(selectedClassId, sortingColumn, sortingOrder);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Icon MEMORY_RESULTS_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_MEMORY_RESULTS);
    private static final Icon INFO_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_INFO);
    private static final Icon STACK_TRACES_TAB_ICON = Icons.getIcon(ProfilerIcons.TAB_STACK_TRACES);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Component findActionPresenter;
    private Component findNextPresenter;
    private Component findPreviousPresenter;
    private JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM);
    private MemoryResultsPanel memoryPanel;
    private MemoryResultsSnapshot snapshot;
    private Lookup.Provider project;
    private SaveSnapshotAction saveAction;
    private SnapshotInfoPanel infoPanel;
    private SnapshotReverseMemCallGraphPanel reversePanel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemorySnapshotPanel(Lookup context, LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        this.snapshot = (MemoryResultsSnapshot) ls.getSnapshot();
        this.project = ls.getProject();

        setLayout(new BorderLayout());

        MemoryResUserActionsHandler memoryActionsHandler = new SnapshotActionsHandler();

        infoPanel = new SnapshotInfoPanel(ls);

        if (snapshot instanceof LivenessMemoryResultsSnapshot) {
            memoryPanel = new SnapshotLivenessResultsPanel((LivenessMemoryResultsSnapshot) snapshot, memoryActionsHandler,
                                                           ls.getSettings().getAllocTrackEvery());

            SnapshotLivenessResultsPanel lmemoryPanel = (SnapshotLivenessResultsPanel) memoryPanel;
            lmemoryPanel.setSorting(sortingColumn, sortingOrder);
            lmemoryPanel.prepareResults();
        } else {
            memoryPanel = new SnapshotAllocResultsPanel((AllocMemoryResultsSnapshot) snapshot, memoryActionsHandler);

            SnapshotAllocResultsPanel amemoryPanel = (SnapshotAllocResultsPanel) memoryPanel;
            amemoryPanel.setSorting(sortingColumn, sortingOrder);
            amemoryPanel.prepareResults();
        }

        infoPanel.updateInfo();

        tabs.addTab(Bundle.MemorySnapshotPanel_MemoryResultsTabName(), MEMORY_RESULTS_TAB_ICON, memoryPanel, Bundle.MemorySnapshotPanel_StackTracesTabDescr());

        if (snapshot.containsStacks()) {
            reversePanel = new SnapshotReverseMemCallGraphPanel(snapshot, memoryActionsHandler);
            reversePanel.prepareResults();
            tabs.addTab(Bundle.MemorySnapshotPanel_StackTracesTabName(), STACK_TRACES_TAB_ICON, reversePanel, Bundle.MemorySnapshotPanel_StackTracesTabDescr());
            tabs.setEnabledAt(tabs.getTabCount() - 1, false);
        }

        tabs.addTab(Bundle.MemorySnapshotPanel_InfoTabName(), INFO_TAB_ICON, infoPanel, Bundle.MemorySnapshotPanel_InfoTabDescr());
        add(tabs, BorderLayout.CENTER);

        tabs.addChangeListener(this);

        ProfilerToolbar toolBar = ProfilerToolbar.create(false);
        toolBar.add(saveAction = new SaveSnapshotAction(ls));
        toolBar.add(new ExportAction(this,ls));
        toolBar.add(new SaveViewAction(this));

        toolBar.addSeparator();
        
        ContextAwareAction a = SystemAction.get(FindAction.class);
        findActionPresenter = toolBar.add(a.createContextAwareInstance(context));
        findPreviousPresenter = toolBar.add(new FindPreviousAction(this));
        findNextPresenter = toolBar.add(new FindNextAction(this));
        
        if (findActionPresenter instanceof AbstractButton) {
            AbstractButton ab = (AbstractButton)findActionPresenter;
            ab.setIcon(Icons.getIcon(GeneralIcons.FIND));
            ab.setText(""); // NOI18N
            ab.setToolTipText(Bundle.MemorySnapshotPanel_FindActionTooltip());
        }

        toolBar.addSeparator();
        toolBar.add(new CompareSnapshotsAction(ls));

        // find is disabled on memory results table
        findActionPresenter.setEnabled(false);
        findPreviousPresenter.setEnabled(false);
        findNextPresenter.setEnabled(false);

        updateToolbar();

        add(toolBar.getComponent(), BorderLayout.NORTH);

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

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        if (tabs.getSelectedComponent() == memoryPanel) {
            return memoryPanel.getCurrentViewScreenshot(onlyVisibleArea);
        } else if (tabs.getSelectedComponent() == reversePanel) {
            return reversePanel.getCurrentViewScreenshot(onlyVisibleArea);
        } else if (tabs.getSelectedComponent() == infoPanel) {
            return infoPanel.getCurrentViewScreenshot(onlyVisibleArea);
        }

        return null;
    }

    public String getViewName() {
        if (tabs.getSelectedComponent() == memoryPanel) {
            return getDefaultSnapshotFileName(getSnapshot()) + "-memory_results"; // NOI18N
        } else if (tabs.getSelectedComponent() == reversePanel) {
            return getDefaultSnapshotFileName(getSnapshot()) + "-allocation_stack_traces"; // NOI18N
        } else if (tabs.getSelectedComponent() == infoPanel) {
            return getDefaultSnapshotFileName(getSnapshot()) + "-info"; // NOI18N
        }

        return null;
    }

    public void displayStacksForClass(int selectedClassId, int sortingColumn, boolean sortingOrder) {
        setReverseCallGraphClass(selectedClassId, sortingColumn, sortingOrder);
        tabs.setSelectedComponent(reversePanel);
    }

    public boolean fitsVisibleArea() {
        if (tabs.getSelectedComponent() == memoryPanel) {
            return memoryPanel.fitsVisibleArea();
        } else if (tabs.getSelectedComponent() == reversePanel) {
            return reversePanel.fitsVisibleArea();
        } else if (tabs.getSelectedComponent() == infoPanel) {
            return infoPanel.fitsVisibleArea();
        }

        return true;
    }

    public boolean hasView() {
        if (tabs.getSelectedComponent() == memoryPanel) {
            return true;
        } else if (tabs.getSelectedComponent() == reversePanel) {
            return reversePanel.hasView();
        }
        
        return false;
    }

    public void performFind() {
        if (tabs.getSelectedComponent() == memoryPanel) {
            String findString = FindDialog.getFindString();

            if (findString == null) {
                return; // cancelled
            }

            memoryPanel.setFindString(findString);
            if (reversePanel != null) reversePanel.setFindString(findString);

            if (!memoryPanel.findFirst()) {
                ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
            }
        } else if (tabs.getSelectedComponent() == reversePanel) {
            String findString = FindDialog.getFindString();

            if (findString == null) {
                return; // cancelled
            }

            memoryPanel.setFindString(findString);
            reversePanel.setFindString(findString);

            if (!reversePanel.findFirst()) {
                ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
            }
        }
    }

    public void performFindNext() {
        if (tabs.getSelectedComponent() == memoryPanel) {
            if (!memoryPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                memoryPanel.setFindString(findString);
                if (reversePanel != null) reversePanel.setFindString(findString);
            }

            if (!memoryPanel.findNext()) {
                ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
            }
        } else if (tabs.getSelectedComponent() == reversePanel) {
            if (!reversePanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                memoryPanel.setFindString(findString);
                reversePanel.setFindString(findString);
            }

            if (!reversePanel.findNext()) {
                ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
            }
        }
    }

    public void performFindPrevious() {
        if (tabs.getSelectedComponent() == memoryPanel) {
            if (!memoryPanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                memoryPanel.setFindString(findString);
                if (reversePanel != null) reversePanel.setFindString(findString);
            }

            if (!memoryPanel.findPrevious()) {
                ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
            }
        }

        if (tabs.getSelectedComponent() == reversePanel) {
            if (!reversePanel.isFindStringDefined()) {
                String findString = FindDialog.getFindString();

                if (findString == null) {
                    return; // cancelled
                }

                memoryPanel.setFindString(findString);
                reversePanel.setFindString(findString);
            }

            if (!reversePanel.findPrevious()) {
                ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
            }
        }
    }

    public void requestFocus() {
        if (memoryPanel != null) {
            memoryPanel.requestFocus(); // move focus to results table when tab is switched
        }
    }

    public void stateChanged(ChangeEvent e) {
        updateToolbar();

        if (tabs.getSelectedComponent() != null) {
            SwingUtilities.invokeLater(new Runnable() { // must be invoked lazily to override default focus of first component (top-right cornerButton)
                    public void run() {
                        tabs.getSelectedComponent().requestFocus();
                    }
                });

        }
    }

    public void updateSavedState() {
        infoPanel.updateInfo();
        saveAction.updateState();
    }

    private String getDefaultSnapshotFileName(ResultsSnapshot snapshot) {
        return "snapshot-" + snapshot.getTimeTaken(); // NOI18N
    }

    private void setReverseCallGraphClass(int selectedClassId, int sortingColumn, boolean sortingOrder) {
        reversePanel.setClassId(selectedClassId);
        reversePanel.setSorting(sortingColumn, sortingOrder);
        reversePanel.prepareResults();
        tabs.setEnabledAt(tabs.indexOfTab(Bundle.MemorySnapshotPanel_StackTracesTabName()), true);
    }

    private void moveToNextSubTab() {
        tabs.setSelectedIndex(UIUtils.getNextSubTabIndex(tabs, tabs.getSelectedIndex()));
    }

    private void moveToPreviousSubTab() {
        tabs.setSelectedIndex(UIUtils.getPreviousSubTabIndex(tabs, tabs.getSelectedIndex()));
    }

    private void updateToolbar() {
        // update the toolbar if selected tab changed
        boolean findEnabled = (tabs.getSelectedComponent() != infoPanel)
                              && ((tabs.getSelectedComponent() != reversePanel) || !reversePanel.isEmpty());
        findActionPresenter.setEnabled(findEnabled);
        findPreviousPresenter.setEnabled(findEnabled);
        findNextPresenter.setEnabled(findEnabled);
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        if (tabs.getSelectedComponent() == memoryPanel) {
            if (memoryPanel instanceof SnapshotAllocResultsPanel) {
                ((SnapshotAllocResultsPanel)memoryPanel).exportData(exportedFileType, eDD, Bundle.MemorySnapshotPanel_MemoryResultsTabName());
            } else if (memoryPanel instanceof SnapshotLivenessResultsPanel) {
                ((SnapshotLivenessResultsPanel)memoryPanel).exportData(exportedFileType, eDD, Bundle.MemorySnapshotPanel_StackTracesTabName());
            } 
        } else if (tabs.getSelectedComponent() == reversePanel) {
            reversePanel.exportData(exportedFileType, eDD, Bundle.MemorySnapshotPanel_StackTracesTabName());
        }
    }

    public boolean hasLoadedSnapshot() {
        return !(snapshot==null);
    }

    public boolean hasExportableView() {
        if (tabs.getSelectedComponent() == memoryPanel) {
            return true;
        } else if (tabs.getSelectedComponent() == reversePanel) {
            return reversePanel.hasView();
        }
        return false;
    }
}
