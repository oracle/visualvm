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

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsDiff;
import org.openide.actions.FindAction;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;


/**
 * An IDE TopComponent to display a diff of two Profiler snapshots.
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "SnapshotDiffWindow_SampledCaption=Sampling Comparison",
    "SnapshotDiffWindow_AllocCaption=Allocations Comparison",
    "SnapshotDiffWindow_LivenessCaption=Liveness Comparison",
    "SnapshotDiffWindow_CpuCaption=CPU Comparison",
    "SnapshotDiffWindow_SampledAccessDescr=Comparison of two memory sampling snapshots",
    "SnapshotDiffWindow_AllocAccessDescr=Comparison of two memory allocations snapshots",
    "SnapshotDiffWindow_LivenessAccessDescr=Comparison of two memory liveness snapshots",
    "SnapshotDiffWindow_CpuAccessDescr=Comparison of two cpu snapshots"
})
public final class SnapshotsDiffWindow extends ProfilerTopComponent {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY_CPU = "CpuDiff.HelpCtx"; // NOI18N
    private static final String HELP_CTX_KEY_MEM = "MemoryDiff.HelpCtx"; // NOI18N
    
    private static final Image WINDOW_ICON_CPU = Icons.getImage(ProfilerIcons.SNAPSHOTS_COMPARE);
    private static final Image WINDOW_ICON_MEMORY = Icons.getImage(ProfilerIcons.SNAPSHOTS_COMPARE);
    
    private HelpCtx helpCtx;


    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * This constructor cannot be called, instances of this window cannot be persisted.
     */
    public SnapshotsDiffWindow() {
        throw new InternalError("This constructor should never be called");
    } // NOI18N

    /**
     * Creates a new SnapshotsDiffWindow for provided diff. The content of this window will vary depending on
     * the type of snapshot provided.
     *
     * @param ls The diff to display
     */
    public SnapshotsDiffWindow(ResultsSnapshot ls, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2, int sortingColumn,
                               boolean sortingOrder, Lookup.Provider project) {
        setLayout(new BorderLayout());
        setFocusable(true);
        setRequestFocusEnabled(true);

        if (ls instanceof SampledMemoryResultsDiff) {
            getAccessibleContext().setAccessibleDescription(Bundle.SnapshotDiffWindow_SampledAccessDescr());
            displayMemorySampledDiff((SampledMemoryResultsDiff) ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        } else if (ls instanceof AllocMemoryResultsDiff) {
            getAccessibleContext().setAccessibleDescription(Bundle.SnapshotDiffWindow_AllocAccessDescr());
            displayMemoryAllocDiff((AllocMemoryResultsDiff) ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        } else if (ls instanceof LivenessMemoryResultsDiff) {
            getAccessibleContext().setAccessibleDescription(Bundle.SnapshotDiffWindow_LivenessAccessDescr());
            displayMemoryLivenessDiff((LivenessMemoryResultsDiff) ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        } else if (ls instanceof CPUResultsSnapshot) {
            getAccessibleContext().setAccessibleDescription(Bundle.SnapshotDiffWindow_CpuAccessDescr());
            displayCPUDiff((CPUResultsSnapshot)ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized SnapshotsDiffWindow get(ResultsSnapshot ls, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2) {
        // target component decides which column will be used for sorting
        return SnapshotsDiffWindow.get(ls, snapshot1, snapshot2, CommonConstants.SORTING_COLUMN_DEFAULT, false, null);
    }

    public static synchronized SnapshotsDiffWindow get(ResultsSnapshot ls, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                                       int sortingColumn, boolean sortingOrder, Lookup.Provider project) {
        return new SnapshotsDiffWindow(ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }
    
    public HelpCtx getHelpCtx() {
        return helpCtx;
    }

    protected String preferredID() {
        return this.getClass().getName();
    }

    // -- Private methods --------------------------------------------------------------------------------------------------
    private void displayMemorySampledDiff(MemoryResultsSnapshot diff, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                        int sortingColumn, boolean sortingOrder, Lookup.Provider project) {
        MemoryDiffPanel sampledDiffPanel = new MemoryDiffPanel(getLookup(), diff, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        updateFind(true, sampledDiffPanel);
        add(sampledDiffPanel, BorderLayout.CENTER);
        setName(Bundle.SnapshotDiffWindow_SampledCaption());
        setIcon(WINDOW_ICON_MEMORY);
        helpCtx = new HelpCtx(HELP_CTX_KEY_MEM);
    }

    private void displayMemoryAllocDiff(MemoryResultsSnapshot diff, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                        int sortingColumn, boolean sortingOrder, Lookup.Provider project) {
        MemoryDiffPanel allocDiffPanel = new MemoryDiffPanel(getLookup(), diff, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        updateFind(true, allocDiffPanel);
        add(allocDiffPanel, BorderLayout.CENTER);
        setName(Bundle.SnapshotDiffWindow_AllocCaption());
        setIcon(WINDOW_ICON_MEMORY);
        helpCtx = new HelpCtx(HELP_CTX_KEY_MEM);
    }

    private void displayMemoryLivenessDiff(MemoryResultsSnapshot diff, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                           int sortingColumn, boolean sortingOrder, Lookup.Provider project) {
        MemoryDiffPanel livenessDiffPanel = new MemoryDiffPanel(getLookup(), diff, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        updateFind(true, livenessDiffPanel);
        add(livenessDiffPanel, BorderLayout.CENTER);
        setName(Bundle.SnapshotDiffWindow_LivenessCaption());
        setIcon(WINDOW_ICON_MEMORY);
        helpCtx = new HelpCtx(HELP_CTX_KEY_MEM);
    }
    
    private void displayCPUDiff(CPUResultsSnapshot diff, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                           int sortingColumn, boolean sortingOrder, Lookup.Provider project) {
        LoadedSnapshot diffLS = new LoadedSnapshot(diff, snapshot1.getSettings(), null, snapshot1.getProject());
        CPUDiffPanel cpuDiffPanel = new CPUDiffPanel(getLookup(), diffLS, snapshot1, snapshot2, sortingColumn, sortingOrder);
        updateFind(true, cpuDiffPanel);
        add(cpuDiffPanel, BorderLayout.CENTER);
        setName(Bundle.SnapshotDiffWindow_CpuCaption());
        setIcon(WINDOW_ICON_CPU);
        helpCtx = new HelpCtx(HELP_CTX_KEY_CPU);
    }

    private void updateFind(boolean enabled, final SnapshotResultsWindow.FindPerformer performer) {
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
}
