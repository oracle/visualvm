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
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.memory.*;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.actions.FindNextAction;
import org.netbeans.modules.profiler.actions.FindPreviousAction;
import org.netbeans.modules.profiler.ui.FindDialog;
import org.openide.actions.FindAction;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import javax.swing.*;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.utils.VMUtils;
import org.netbeans.modules.profiler.ui.Utils;


/**
 * A display for diff of memory snapshots
 *
 * @author Jiri Sedlacek
 */
public class MemoryDiffPanel extends JPanel implements SnapshotResultsWindow.FindPerformer, SaveViewAction.ViewProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class DiffActionsHandler implements MemoryResUserActionsHandler {
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
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String PANEL_TITLE = NbBundle.getMessage(MemoryDiffPanel.class, "MemoryDiffPanel_PanelTitle"); // NOI18N
    private static final String SNAPSHOTS_COMPARISON_STRING = NbBundle.getMessage(MemoryDiffPanel.class,
                                                                                  "MemoryDiffPanel_SnapshotsComparisonString"); // NOI18N
    private static final String SNAPSHOT_NOT_AVAILABLE_MSG = NbBundle.getMessage(MemoryDiffPanel.class,
                                                                                 "MemoryDiffPanel_SnapshotNotAvailableMsg"); // NOI18N
    private static final String STRING_NOT_FOUND_MSG = NbBundle.getMessage(MemoryDiffPanel.class,
                                                                           "MemorySnapshotPanel_StringNotFoundMsg"); // NOI18N
    private static final String FIND_ACTION_TOOLTIP = NbBundle.getMessage(MemoryDiffPanel.class,
                                                                           "MemorySnapshotPanel_FindActionTooltip"); // NOI18N
                                                                                                                     // -----
    private static final ImageIcon MEMORY_RESULTS_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/memoryResultsTab.png") // NOI18N
    );
    private static final ImageIcon INFO_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/infoTab.png") // NOI18N
    );
    private static final ImageIcon STACK_TRACES_TAB_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/stackTracesTab.png") // NOI18N
    );

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JButton findActionPresenter;
    private JButton findNextPresenter;
    private JButton findPreviousPresenter;
    private MemoryResultsPanel memoryPanel;
    private Project project;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemoryDiffPanel(MemoryResultsSnapshot snapshot, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2, int sortingColumn,
                           boolean sortingOrder, Project project) {
        this.project = project;

        setLayout(new BorderLayout());

        MemoryResUserActionsHandler memoryActionsHandler = new DiffActionsHandler();

        if (snapshot instanceof AllocMemoryResultsDiff) {
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

        //    toolBar.add(saveAction = new SaveSnapshotAction(ls));
        //    toolBar.add(new ExportSnapshotAction(ls));
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

        findActionPresenter.setEnabled(true);
        findPreviousPresenter.setEnabled(true);
        findNextPresenter.setEnabled(true);

        // Filler to align rest of the toolbar to the right
        JPanel toolBarFiller = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        toolBarFiller.setOpaque(false);
        toolBar.add(toolBarFiller);

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

        final String SNAPSHOT_1_MASK = "file:/1"; //NOI18N
        final String SNAPSHOT_2_MASK = "file:/2"; //NOI18N

        final String SNAPSHOT_1_LINK = "<a href='" + SNAPSHOT_1_MASK + "'>"
                                       + StringUtils.formatUserDate(new Date(snapshot1.getSnapshot().getTimeTaken())) + "</a>"; //NOI18N
        final String SNAPSHOT_2_LINK = "<a href='" + SNAPSHOT_2_MASK + "'>"
                                       + StringUtils.formatUserDate(new Date(snapshot2.getSnapshot().getTimeTaken())) + "</a>"; //NOI18N

        HTMLLabel descriptionLabel = new HTMLLabel(MessageFormat.format(SNAPSHOTS_COMPARISON_STRING,
                                                                        new Object[] { SNAPSHOT_1_LINK, SNAPSHOT_2_LINK })) {
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
                        if (f.exists()) ls = ResultsManager.getDefault().loadSnapshot(FileUtil.toFileObject(f));
                    } else {
                        ls = loadedSnapshots[0].get();
                    }
                } else if (SNAPSHOT_2_MASK.equals(url.toString())) {
                    if (s2File != null) {
                        File f = new File(s2File);
                        if (f.exists()) ls = ResultsManager.getDefault().loadSnapshot(FileUtil.toFileObject(f));
                    } else {
                        ls = loadedSnapshots[1].get();
                    }
                }

                if (ls != null) {
                    SnapshotResultsWindow srw = SnapshotResultsWindow.get(ls);
                    srw.open();
                    srw.requestActive();
                } else {
                    NetBeansProfiler.getDefaultNB().displayWarning(SNAPSHOT_NOT_AVAILABLE_MSG);
                }
            }
        };

        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        toolBar.add(descriptionLabel);

        add(toolBar, BorderLayout.NORTH);

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

    public String getTitle() {
        return PANEL_TITLE;
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
            NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
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
            NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
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
            NetBeansProfiler.getDefaultNB().displayInfoAndWait(STRING_NOT_FOUND_MSG);
        }
    }

    public void requestFocus() {
        memoryPanel.requestFocus(); // move focus to results table when tab is switched
    }
}
