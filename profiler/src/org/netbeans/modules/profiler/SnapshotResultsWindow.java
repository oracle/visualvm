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

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.actions.FindAction;
import org.openide.cookies.SaveCookie;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import javax.swing.*;


/**
 * An IDE TopComponent to display a snapshot of profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class SnapshotResultsWindow extends TopComponent implements SnapshotsListener {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface FindPerformer {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void performFind();

        public void performFindNext();

        public void performFindPrevious();
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SaveNode extends AbstractNode {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Create a new abstract node with a given child set.
         */
        public SaveNode() {
            super(Children.LEAF);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setSaveEnabled(boolean saveEnabled) {
            if (saveEnabled) {
                if (getCookie(SaveCookie.class) == null) {
                    getCookieSet().add(savePerformer);
                }
            } else {
                if (getCookie(SaveCookie.class) != null) {
                    getCookieSet().remove(savePerformer);
                }
            }
        }
    }

    private class SavePerformer implements SaveCookie {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void save() throws IOException {
            ResultsManager.getDefault().saveSnapshot(snapshot);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SAVE_SNAPSHOT_DIALOG_MSG = NbBundle.getMessage(SnapshotResultsWindow.class,
                                                                               "SnapshotResultsWindow_SaveSnapshotDialogMsg"); // NOI18N
    private static final String CPU_SNAPSHOT_ACCESS_DESCR = NbBundle.getMessage(SnapshotResultsWindow.class,
                                                                                "SnapshotResultsWindow_CpuSnapshotAccessDescr"); // NOI18N
    private static final String FRAGMENT_SNAPSHOT_ACCESS_DESCR = NbBundle.getMessage(SnapshotResultsWindow.class,
                                                                                     "SnapshotResultsWindow_FragmentSnapshotAccessDescr"); // NOI18N
    private static final String MEMORY_SNAPSHOT_ACCESS_DESCR = NbBundle.getMessage(SnapshotResultsWindow.class,
                                                                                   "SnapshotResultsWindow_MemorySnapshotAccessDescr"); // NOI18N
                                                                                                                                       // -----
    private static final Image WINDOW_ICON_CPU = ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/cpu.png"); // NOI18N
    private static final Image WINDOWS_ICON_FRAGMENT = ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/fragment.png"); // NOI18N
    private static final Image WINDOWS_ICON_MEMORY = ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/memory.png"); // NOI18N
    private static final HashMap /*<ResultsSnapshot, SnapshotResultsWindow>*/ windowsList = new HashMap();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Component lastFocusOwner;
    private LoadedSnapshot snapshot;
    private SaveNode saveSupport = new SaveNode();
    private SavePerformer savePerformer = new SavePerformer();
    private SnapshotPanel displayedPanel;
    private String tabName = ""; // NOI18N // default
    private boolean forcedClose = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * This constructor cannot be called, instances of this window cannot be persisted.
     */
    public SnapshotResultsWindow() {
        throw new InternalError("This constructor should never be called");
    } // NOI18N

    /**
     * Creates a new SnapshotResultsWindow for provided snapshot. The content of this window will vary depending on
     * the type of snapshot provided.
     *
     * @param ls The results snapshot to display
     */
    public SnapshotResultsWindow(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        this.snapshot = ls;
        ResultsManager.getDefault().addSnapshotsListener(this);
        updateSaveState();

        setLayout(new BorderLayout());
        setFocusable(true);
        setRequestFocusEnabled(true);

        switch (ls.getType()) {
            case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                getAccessibleContext().setAccessibleDescription(CPU_SNAPSHOT_ACCESS_DESCR);
                displayCPUResults(ls, sortingColumn, sortingOrder);

                break;
            case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                getAccessibleContext().setAccessibleDescription(FRAGMENT_SNAPSHOT_ACCESS_DESCR);
                displayCodeRegionResults(ls);

                break;
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                getAccessibleContext().setAccessibleDescription(MEMORY_SNAPSHOT_ACCESS_DESCR);
                displayMemoryResults(ls, sortingColumn, sortingOrder);

                break;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized void closeAllWindows() {
        Collection windows = windowsList.values();

        if (!windows.isEmpty()) {
            SnapshotResultsWindow[] toClose = new SnapshotResultsWindow[windows.size()];
            windows.toArray(toClose);

            for (int i = 0; i < toClose.length; i++) {
                SnapshotResultsWindow snapshotResultsWindow = toClose[i];
                snapshotResultsWindow.forcedClose();
            }
        }
    }

    public static synchronized void closeWindow(LoadedSnapshot snapshot) {
        SnapshotResultsWindow win = (SnapshotResultsWindow) windowsList.get(snapshot);

        if (win != null) {
            win.forcedClose();
        }
    }

    public static synchronized SnapshotResultsWindow get(LoadedSnapshot ls) {
        // target component decides which column will be used for sorting
        return SnapshotResultsWindow.get(ls, CommonConstants.SORTING_COLUMN_DEFAULT, false);
    }

    public static synchronized SnapshotResultsWindow get(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        SnapshotResultsWindow win = (SnapshotResultsWindow) windowsList.get(ls);

        if (win == null) {
            win = new SnapshotResultsWindow(ls, sortingColumn, sortingOrder);
            windowsList.put(ls, win);
        }

        return win;
    }

    public static synchronized boolean hasSnapshotWindow(LoadedSnapshot ls) {
        return windowsList.get(ls) != null;
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    public boolean canClose() {
        if (forcedClose) {
            // clean up to avoid being held in memory
            setActivatedNodes(new Node[0]);

            return true;
        }

        if (snapshot.isSaved()) {
            return true; // already saved
        }

        ProfilerDialogs.DNSAConfirmation dd = new ProfilerDialogs.DNSAConfirmation("org.netbeans.modules.profiler.SnapshotResultsWindow.canClose", // NOI18N
                                                                                   SAVE_SNAPSHOT_DIALOG_MSG,
                                                                                   DialogDescriptor.YES_NO_CANCEL_OPTION);
        dd.setDNSADefault(false);

        Object ret = ProfilerDialogs.notify(dd);

        if (ret.equals(DialogDescriptor.CANCEL_OPTION) || ret.equals(DialogDescriptor.CLOSED_OPTION)) {
            return false;
        } else if (ret.equals(DialogDescriptor.YES_OPTION)) {
            ResultsManager.getDefault().saveSnapshot(snapshot);
            // clean up to avoid being held in memory
            setActivatedNodes(new Node[0]);

            return true;
        } else {
            // clean up to avoid being held in memory
            setActivatedNodes(new Node[0]);

            return true;
        }
    }

    public void componentActivated() {
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        } else if (displayedPanel != null) {
            displayedPanel.requestFocus();
        }
    }

    public void componentDeactivated() {
        lastFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    public void displayStacksForClass(int selectedClassId, int sortingColumn, boolean sortingOrder) {
        if (displayedPanel instanceof MemorySnapshotPanel) {
            ((MemorySnapshotPanel) displayedPanel).displayStacksForClass(selectedClassId, sortingColumn, sortingOrder);
        }
    }

    public void snapshotLoaded(LoadedSnapshot snapshot) {
        // ignore
    }

    public void snapshotRemoved(LoadedSnapshot snapshot) {
        // ignore
    }

    public void snapshotSaved(LoadedSnapshot snapshot) {
        updateSaveState();
    }

    public void snapshotTaken(LoadedSnapshot snapshot) {
        // ignore
    }

    public void updateTitle() {
        if (snapshot.isSaved()) {
            setName(tabName);
        } else {
            // XXX consider using DataEditorSupport.annotateName
            setName(tabName + " *"); // NOI18N
        }
    }

    protected void componentClosed() {
        synchronized (SnapshotResultsWindow.class) {
            windowsList.remove(snapshot);
        }

        ResultsManager.getDefault().closeSnapshot(snapshot);
        ResultsManager.getDefault().removeSnapshotsListener(this);
        snapshot = null;
    }

    protected String preferredID() {
        return this.getClass().getName();
    }

    // -- Private methods --------------------------------------------------------------------------------------------------
    private void setTabName(String innerName) {
        tabName = innerName;
        updateTitle();
    }

    private void displayCPUResults(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        CPUSnapshotPanel cpuPanel = new CPUSnapshotPanel(ls, sortingColumn, sortingOrder);
        displayedPanel = cpuPanel;
        updateFind(true, cpuPanel);
        add(cpuPanel, BorderLayout.CENTER);
        setTabName(cpuPanel.getTitle());
        setIcon(WINDOW_ICON_CPU);
    }

    private void displayCodeRegionResults(LoadedSnapshot ls) {
        updateFind(false, null);

        FragmentSnapshotPanel codeRegionPanel = new FragmentSnapshotPanel(ls);
        displayedPanel = codeRegionPanel;
        add(codeRegionPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setTabName(codeRegionPanel.getTitle());
        setIcon(WINDOWS_ICON_FRAGMENT);
    }

    private void displayMemoryResults(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        MemorySnapshotPanel memoryPanel = new MemorySnapshotPanel(ls, sortingColumn, sortingOrder);
        displayedPanel = memoryPanel;
        updateFind(true, memoryPanel);
        add(memoryPanel, BorderLayout.CENTER);
        setTabName(memoryPanel.getTitle());
        setIcon(WINDOWS_ICON_MEMORY);
    }

    private void forcedClose() {
        forcedClose = true;
        close();
    }

    private void updateFind(boolean enabled, final FindPerformer performer) {
        CallbackSystemAction globalFindAction = (CallbackSystemAction) SystemAction.get(FindAction.class);
        Object findActionKey = globalFindAction.getActionMapKey();

        if (enabled) {
            getActionMap().put(findActionKey,
                               new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        performer.performFind();
                    }
                });
        } else {
            getActionMap().remove(findActionKey);
        }
    }

    private void updateSaveState() {
        saveSupport.setSaveEnabled(!snapshot.isSaved());
        setActivatedNodes(new Node[] { saveSupport });

        if (displayedPanel != null) {
            displayedPanel.updateSavedState();
        }
    }
}
