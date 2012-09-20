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

import org.netbeans.modules.profiler.utilities.Delegate;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.openide.actions.FindAction;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import javax.swing.*;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.spi.actions.AbstractSavable;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ServiceProvider;


/**
 * An IDE TopComponent to display a snapshot of profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "SnapshotResultsWindow_SaveSnapshotDialogMsg=The results snapshot is not saved. Do you want to save it?",
    "SnapshotResultsWindow_CpuSnapshotAccessDescr=Profiler snapshot with CPU results",
    "SnapshotResultsWindow_FragmentSnapshotAccessDescr=Profiler snapshot with code fragment results",
    "SnapshotResultsWindow_MemorySnapshotAccessDescr=Profiler snapshot with memory results"
})
public final class SnapshotResultsWindow extends ProfilerTopComponent {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------
    
    /* 
     * The following code is an externalization of various listeners registered
     * in the global lookup and needing access to an enclosing instance of
     * SnapshotResultsWindow. 
     * The enclosing instance will use the FQN registration to obtain the shared instance
     * of the listener implementation and inject itself as a delegate into the listener.
     */
    @ServiceProvider(service=SnapshotsListener.class)   
    public static class SnapshotListener extends Delegate<SnapshotResultsWindow> implements SnapshotsListener {
        @Override
        public void snapshotLoaded(LoadedSnapshot snapshot) {
            // ignore
        }

        @Override
        public void snapshotRemoved(LoadedSnapshot snapshot) {
            // ignore
        }

        @Override
        public void snapshotSaved(LoadedSnapshot snapshot) {
            if (getDelegate() != null) getDelegate().updateSaveState();
        }

        @Override
        public void snapshotTaken(LoadedSnapshot snapshot) {
            // ignore
        }
    }
    
    public static interface FindPerformer {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void performFind();

        public void performFindNext();

        public void performFindPrevious();
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SavePerformer extends AbstractSavable {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void add() {
            register();
            ic.add(this);
        }
        
        private void remove() {
             unregister();
             ic.remove(this);
        }
        
        @Override
        protected String findDisplayName() {
            return tabName;
        }

        @Override
        protected void handleSave() {
            LoadedSnapshot toSave = snapshot;
            if (toSave == null) return; // #218565 snapshot already closed
            
            ResultsManager.getDefault().saveSnapshot(snapshot);
            ic.remove(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final String HELP_CTX_KEY_CPU = "CpuSnapshot.HelpCtx"; // NOI18N
    private static final String HELP_CTX_KEY_MEM = "MemorySnapshot.HelpCtx"; // NOI18N
    
    private static final Image WINDOW_ICON_CPU = Icons.getImage(ProfilerIcons.CPU);
    private static final Image WINDOWS_ICON_FRAGMENT = Icons.getImage(ProfilerIcons.FRAGMENT);
    private static final Image WINDOWS_ICON_MEMORY = Icons.getImage(ProfilerIcons.MEMORY);
    private static final HashMap /*<ResultsSnapshot, SnapshotResultsWindow>*/ windowsList = new HashMap();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private LoadedSnapshot snapshot;
    private InstanceContent ic = new InstanceContent();
    private SavePerformer savePerformer = new SavePerformer();
    private SnapshotPanel displayedPanel;
    private String tabName = ""; // NOI18N // default
    private SnapshotListener listener;
    private boolean forcedClose = false;
    private HelpCtx helpCtx = HelpCtx.DEFAULT_HELP;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * This constructor cannot be called, instances of this window cannot be persisted.
     */
    public SnapshotResultsWindow() {
        throw new InternalError("This constructor should never be called"); // NOI18N
    } // NOI18N

    /**
     * Creates a new SnapshotResultsWindow for provided snapshot. The content of this window will vary depending on
     * the type of snapshot provided.
     *
     * @param ls The results snapshot to display
     */
    public SnapshotResultsWindow(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        associateLookup(new AbstractLookup(ic));
        ic.add(getActionMap());
        this.snapshot = ls;
        updateSaveState();

        setLayout(new BorderLayout());
        setFocusable(true);
        setRequestFocusEnabled(true);
        
        refreshTabName();

        switch (snapshot.getType()) {
            case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                getAccessibleContext().setAccessibleDescription(Bundle.SnapshotResultsWindow_CpuSnapshotAccessDescr());
                displayCPUResults(ls, sortingColumn, sortingOrder);

                break;
            case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                getAccessibleContext().setAccessibleDescription(Bundle.SnapshotResultsWindow_FragmentSnapshotAccessDescr());
                displayCodeRegionResults(ls);

                break;
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
                getAccessibleContext().setAccessibleDescription(Bundle.SnapshotResultsWindow_MemorySnapshotAccessDescr());
                displayMemoryResults(ls, sortingColumn, sortingOrder);

                break;
        }
        listener = Lookup.getDefault().lookup(SnapshotListener.class);
        listener.setDelegate(this);
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
    
    public HelpCtx getHelpCtx() {
        return helpCtx;
    }

    public boolean canClose() {
        if (forcedClose) {
            savePerformer.remove();
            return true;
        }

        if (snapshot.isSaved()) {
            return true; // already saved
        }

        Boolean ret = ProfilerDialogs.displayCancellableConfirmationDNSA(Bundle.SnapshotResultsWindow_SaveSnapshotDialogMsg(),
                null, null, "org.netbeans.modules.profiler.SnapshotResultsWindow.canClose", false); // NOI18N

        if (Boolean.TRUE.equals(ret)) {
            try {
                savePerformer.save();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            return true;
        } else if (Boolean.FALSE.equals(ret)) {
            // clean up to avoid being held in memory
            savePerformer.remove();
            return true;
        } else {
            return false;
        }
    }
    
    protected Component defaultFocusOwner() {
        return displayedPanel;
    }

    public void displayStacksForClass(int selectedClassId, int sortingColumn, boolean sortingOrder) {
        if (displayedPanel instanceof MemorySnapshotPanel) {
            ((MemorySnapshotPanel) displayedPanel).displayStacksForClass(selectedClassId, sortingColumn, sortingOrder);
        }
    }
    
    public void refreshTabName() {
        tabName = ResultsManager.getDefault().getSnapshotDisplayName(snapshot);
        File snapshotFile = snapshot.getFile();
        if (snapshotFile != null) setToolTipText(snapshotFile.getAbsolutePath());
        updateTitle();
    }

    public void updateTitle() {
        if (snapshot.isSaved()) {
            setName(tabName);
        } else {
            // XXX consider using DataEditorSupport.annotateName
            setName(tabName + " *"); // NOI18N
        }
        // Called when snapshot is saved, update also tooltip
        if (snapshot.getFile() != null)
            setToolTipText(snapshot.getFile().getAbsolutePath());
    }

    protected void componentClosed() {
        synchronized (SnapshotResultsWindow.class) {
            windowsList.remove(snapshot);
        }

        ResultsManager.getDefault().closeSnapshot(snapshot);
        snapshot = null;
    }

    protected String preferredID() {
        return this.getClass().getName();
    }

    // -- Private methods --------------------------------------------------------------------------------------------------

    private void displayCPUResults(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        CPUSnapshotPanel cpuPanel = new CPUSnapshotPanel(getLookup(), ls, sortingColumn, sortingOrder);
        displayedPanel = cpuPanel;
        updateFind(true, cpuPanel);
        add(cpuPanel, BorderLayout.CENTER);
        setIcon(WINDOW_ICON_CPU);
        helpCtx = new HelpCtx(HELP_CTX_KEY_CPU);
    }

    private void displayCodeRegionResults(LoadedSnapshot ls) {
        updateFind(false, null);

        FragmentSnapshotPanel codeRegionPanel = new FragmentSnapshotPanel(ls);
        displayedPanel = codeRegionPanel;
        add(codeRegionPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setIcon(WINDOWS_ICON_FRAGMENT);
    }

    private void displayMemoryResults(LoadedSnapshot ls, int sortingColumn, boolean sortingOrder) {
        MemorySnapshotPanel memoryPanel = new MemorySnapshotPanel(getLookup(), ls, sortingColumn, sortingOrder);
        displayedPanel = memoryPanel;
        updateFind(true, memoryPanel);
        add(memoryPanel, BorderLayout.CENTER);
        setIcon(WINDOWS_ICON_MEMORY);
        helpCtx = new HelpCtx(HELP_CTX_KEY_MEM);
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
        if (snapshot != null) { // snapshot == null means the window has been closed (#202992)
            if (snapshot.isSaved()) {
                savePerformer.remove();
            } else {
                savePerformer.add();
            }

            if (displayedPanel != null) {
                displayedPanel.updateSavedState();
            }
        } else {
            // just to be sure
            savePerformer.remove();
        }
    }
}
