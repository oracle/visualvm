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
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.memory.*;
import org.netbeans.modules.profiler.actions.FindNextAction;
import org.netbeans.modules.profiler.actions.FindPreviousAction;
import org.netbeans.modules.profiler.ui.FindDialog;
import org.openide.actions.FindAction;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import javax.swing.*;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsDiff;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.utils.VMUtils;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;


/**
 * A display for diff of memory snapshots
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MemoryDiffPanel_PanelTitle=Memory Diff",
    "MemoryDiffPanel_SnapshotsComparisonString=Comparison of {0} to {1}",
    "MemoryDiffPanel_SnapshotNotAvailableMsg=Compared snapshot no longer available."
})
public class MemoryDiffPanel extends JPanel implements SnapshotResultsWindow.FindPerformer, SaveViewAction.ViewProvider, ExportAction.ExportProvider {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class DiffActionsHandler implements MemoryResUserActionsHandler {
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
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Component findActionPresenter;
    private Component findNextPresenter;
    private Component findPreviousPresenter;
    private MemoryResultsPanel memoryPanel;
    private Lookup.Provider project;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemoryDiffPanel(Lookup context, MemoryResultsSnapshot snapshot, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2, int sortingColumn,
                           boolean sortingOrder, Lookup.Provider project) {
        this.project = project;

        setLayout(new BorderLayout());

        MemoryResUserActionsHandler memoryActionsHandler = new DiffActionsHandler();

        if (snapshot instanceof SampledMemoryResultsDiff) {
            memoryPanel = new DiffSampledResultsPanel((SampledMemoryResultsDiff) snapshot, memoryActionsHandler);

            DiffSampledResultsPanel amemoryPanel = (DiffSampledResultsPanel) memoryPanel;
            amemoryPanel.setSorting(sortingColumn, sortingOrder);
            amemoryPanel.prepareResults();
        } else if (snapshot instanceof AllocMemoryResultsDiff) {
            memoryPanel = new DiffAllocResultsPanel((AllocMemoryResultsDiff) snapshot, memoryActionsHandler);

            DiffAllocResultsPanel amemoryPanel = (DiffAllocResultsPanel) memoryPanel;
            amemoryPanel.setSorting(sortingColumn, sortingOrder);
            amemoryPanel.prepareResults();
        } else if (snapshot instanceof LivenessMemoryResultsDiff) {
            memoryPanel = new DiffLivenessResultsPanel((LivenessMemoryResultsDiff) snapshot, memoryActionsHandler, 1);

            SnapshotLivenessResultsPanel lmemoryPanel = (SnapshotLivenessResultsPanel) memoryPanel;
            lmemoryPanel.setSorting(sortingColumn, sortingOrder);
            lmemoryPanel.prepareResults();
        }

        add(memoryPanel, BorderLayout.CENTER);

        ProfilerToolbar toolBar = ProfilerToolbar.create(true);
        //    toolBar.add(saveAction = new SaveSnapshotAction(ls));
        toolBar.add(new ExportAction(this,null));
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

        findActionPresenter.setEnabled(true);
        findPreviousPresenter.setEnabled(true);
        findNextPresenter.setEnabled(true);

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

        add(toolBar.getComponent(), BorderLayout.NORTH);

        // support for Find Next / Find Previous using F3 / Shift + F3
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK), "FIND_PREVIOUS"); // NOI18N
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK), "FIND_PREVIOUS"); // NOI18N
        getActionMap().put("FIND_PREVIOUS", //NOI18N
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performFindPrevious();
                }
            }); // NOI18N
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "FIND_NEXT"); // NOI18N
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "FIND_NEXT"); // NOI18N
        getActionMap().put("FIND_NEXT", //NOI18N
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performFindNext();
                }
            }); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getTitle() {
        return Bundle.MemoryDiffPanel_PanelTitle();
    }

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        return memoryPanel.getCurrentViewScreenshot(onlyVisibleArea);
    }

    public String getViewName() {
        return "memory_diff"; // NOI18N
    }

    public boolean fitsVisibleArea() {
        return memoryPanel.fitsVisibleArea();
    }

    public boolean hasView() {
        return true;
    }

    public void performFind() {
        String findString = FindDialog.getFindString();

        if (findString == null) {
            return; // cancelled
        }

        memoryPanel.setFindString(findString);

        if (!memoryPanel.findFirst()) {
            ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
        }
    }

    public void performFindNext() {
        if (!memoryPanel.isFindStringDefined()) {
            String findString = FindDialog.getFindString();

            if (findString == null) {
                return; // cancelled
            }

            memoryPanel.setFindString(findString);
        }

        if (!memoryPanel.findNext()) {
            ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
        }
    }

    public void performFindPrevious() {
        if (!memoryPanel.isFindStringDefined()) {
            String findString = FindDialog.getFindString();

            if (findString == null) {
                return; // cancelled
            }

            memoryPanel.setFindString(findString);
        }

        if (!memoryPanel.findPrevious()) {
            ProfilerDialogs.displayInfo(Bundle.MemorySnapshotPanel_StringNotFoundMsg());
        }
    }

    public void requestFocus() {
        memoryPanel.requestFocus(); // move focus to results table when tab is switched
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        if (memoryPanel instanceof DiffSampledResultsPanel) {
            ((DiffSampledResultsPanel) memoryPanel).exportData(exportedFileType, eDD, Bundle.MemoryDiffPanel_PanelTitle());
        } else if (memoryPanel instanceof DiffAllocResultsPanel) {
            ((DiffAllocResultsPanel) memoryPanel).exportData(exportedFileType, eDD, Bundle.MemoryDiffPanel_PanelTitle());
        } else {
            ((DiffLivenessResultsPanel) memoryPanel).exportData(exportedFileType, eDD, Bundle.MemoryDiffPanel_PanelTitle());
        }
    }

    public boolean hasLoadedSnapshot() {
        return false;
    }
    
    public boolean hasExportableView() {
        return true;
    }
}
