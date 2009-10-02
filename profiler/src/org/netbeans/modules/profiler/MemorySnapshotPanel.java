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

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.memory.*;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.actions.CompareSnapshotsAction;
import org.netbeans.modules.profiler.actions.FindNextAction;
import org.netbeans.modules.profiler.actions.FindPreviousAction;
import org.netbeans.modules.profiler.ui.FindDialog;
import org.openide.actions.FindAction;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.utils.VMUtils;
import org.netbeans.modules.profiler.ui.Utils;


/**
 * A display for snapshot of CPU profiling results
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public class MemorySnapshotPanel extends SnapshotPanel implements ChangeListener, SnapshotResultsWindow.FindPerformer,
                                                                  SaveViewAction.ViewProvider, ExportAction.ExportProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SnapshotActionsHandler implements MemoryResUserActionsHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void showSourceForMethod(String className, String methodName, String methodSig) {
            // Check if primitive type/array
            if ((methodName == null && methodSig == null) && (VMUtils.isVMPrimitiveType(className) ||
                 VMUtils.isPrimitiveType(className))) Profiler.getDefault().displayWarning(CANNOT_SHOW_PRIMITIVE_SRC_MSG);
            // Check if allocated by reflection
            else if (PresoObjAllocCCTNode.VM_ALLOC_CLASS.equals(className) && PresoObjAllocCCTNode.VM_ALLOC_METHOD.equals(methodName))
                     Profiler.getDefault().displayWarning(CANNOT_SHOW_REFLECTION_SRC_MSG);
            // Display source
            else NetBeansProfiler.getDefaultNB().openJavaSource(project, className, methodName, methodSig);
        }

        public void showStacksForClass(int selectedClassId, int sortingColumn, boolean sortingOrder) {
            displayStacksForClass(selectedClassId, sortingColumn, sortingOrder);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String MEMORY_RESULTS_TAB_NAME = NbBundle.getMessage(MemorySnapshotPanel.class,
                                                                              "MemorySnapshotPanel_MemoryResultsTabName"); // NOI18N
    private static final String STACK_TRACES_TAB_NAME = NbBundle.getMessage(MemorySnapshotPanel.class,
                                                                            "MemorySnapshotPanel_StackTracesTabName"); // NOI18N
    private static final String INFO_TAB_NAME = NbBundle.getMessage(MemorySnapshotPanel.class, "MemorySnapshotPanel_InfoTabName"); // NOI18N
    private static final String MEMORY_RESULTS_TAB_DESCR = NbBundle.getMessage(MemorySnapshotPanel.class,
                                                                               "MemorySnapshotPanel_MemoryResultsTabDescr"); // NOI18N
    private static final String STACK_TRACES_TAB_DESCR = NbBundle.getMessage(MemorySnapshotPanel.class,
                                                                             "MemorySnapshotPanel_StackTracesTabDescr"); // NOI18N
    private static final String INFO_TAB_DESCR = NbBundle.getMessage(MemorySnapshotPanel.class, "MemorySnapshotPanel_InfoTabDescr"); // NOI18N
    private static final String PANEL_TITLE = NbBundle.getMessage(MemorySnapshotPanel.class, "MemorySnapshotPanel_PanelTitle"); // NOI18N
    private static final String STRING_NOT_FOUND_MSG = NbBundle.getMessage(MemorySnapshotPanel.class,
                                                                           "MemorySnapshotPanel_StringNotFoundMsg"); // NOI18N
    private static final String FIND_ACTION_TOOLTIP = NbBundle.getMessage(MemorySnapshotPanel.class,
                                                                           "MemorySnapshotPanel_FindActionTooltip"); // NOI18N
                                                                                                                     // -----
    private static final ImageIcon MEMORY_RESULTS_TAB_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/memoryResultsTab.png", false); //NOI18N
    private static final ImageIcon INFO_TAB_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/infoTab.png", false); //NOI18N
    private static final ImageIcon STACK_TRACES_TAB_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/stackTracesTab.png", false); //NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JButton findActionPresenter;
    private JButton findNextPresenter;
    private JButton findPreviousPresenter;
    private JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM);
    private MemoryResultsPanel memoryPanel;
    private MemoryResultsSnapshot snapshot;
    private Project project;
    private SaveSnapshotAction saveAction;
    private SnapshotInfoPanel infoPanel;
    private SnapshotReverseMemCallGraphPanel reversePanel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemorySnapshotPanel(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
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

        tabs.addTab(MEMORY_RESULTS_TAB_NAME, MEMORY_RESULTS_TAB_ICON, memoryPanel, MEMORY_RESULTS_TAB_DESCR);

        if (snapshot.containsStacks()) {
            reversePanel = new SnapshotReverseMemCallGraphPanel(snapshot, memoryActionsHandler);
            reversePanel.prepareResults();
            tabs.addTab(STACK_TRACES_TAB_NAME, STACK_TRACES_TAB_ICON, reversePanel, STACK_TRACES_TAB_DESCR);
            tabs.setEnabledAt(tabs.getTabCount() - 1, false);
        }

        tabs.addTab(INFO_TAB_NAME, INFO_TAB_ICON, infoPanel, INFO_TAB_DESCR);
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

        toolBar.add(saveAction = new SaveSnapshotAction(ls));
        toolBar.add(new ExportAction(this,ls));
        toolBar.add(new SaveViewAction(this));

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

        toolBar.addSeparator();
        toolBar.add(new CompareSnapshotsAction(ls));

        // find is disabled on memory results table
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
                NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
            }
        } else if (tabs.getSelectedComponent() == reversePanel) {
            String findString = FindDialog.getFindString();

            if (findString == null) {
                return; // cancelled
            }

            memoryPanel.setFindString(findString);
            reversePanel.setFindString(findString);

            if (!reversePanel.findFirst()) {
                NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
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
                NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
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
                NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
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
                NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
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
                NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
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
        tabs.setEnabledAt(tabs.indexOfTab(STACK_TRACES_TAB_NAME), true);
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
                ((SnapshotAllocResultsPanel)memoryPanel).exportData(exportedFileType, eDD, MEMORY_RESULTS_TAB_NAME);
            } else if (memoryPanel instanceof SnapshotLivenessResultsPanel) {
                ((SnapshotLivenessResultsPanel)memoryPanel).exportData(exportedFileType, eDD, MEMORY_RESULTS_TAB_NAME);
            } 
        } else if (tabs.getSelectedComponent() == reversePanel) {
            reversePanel.exportData(exportedFileType, eDD, STACK_TRACES_TAB_NAME);
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
