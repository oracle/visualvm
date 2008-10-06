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
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.openide.actions.FindAction;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;


/**
 * An IDE TopComponent to display a diff of two Profiler snapshots.
 *
 * @author Jiri Sedlacek
 */
public final class SnapshotsDiffWindow extends TopComponent {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ALLOC_CAPTION = NbBundle.getMessage(SnapshotResultsWindow.class, "SnapshotDiffWindow_AllocCaption"); // NOI18N
    private static final String LIVENESS_CAPTION = NbBundle.getMessage(SnapshotResultsWindow.class,
                                                                       "SnapshotDiffWindow_LivenessCaption"); // NOI18N
    private static final String ALLOC_ACCESS_DESCR = NbBundle.getMessage(SnapshotResultsWindow.class,
                                                                         "SnapshotDiffWindow_AllocAccessDescr"); // NOI18N
    private static final String LIVENESS_ACCESS_DESCR = NbBundle.getMessage(SnapshotResultsWindow.class,
                                                                            "SnapshotDiffWindow_LivenessAccessDescr"); // NOI18N
                                                                                                                       // -----
    private static final Image WINDOW_ICON_MEMORY = ImageUtilities.loadImage("org/netbeans/modules/profiler/actions/resources/compareSnapshots.png"); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Component lastFocusOwner;

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
                               boolean sortingOrder, Project project) {
        setLayout(new BorderLayout());
        setFocusable(true);
        setRequestFocusEnabled(true);

        if (ls instanceof AllocMemoryResultsDiff) {
            getAccessibleContext().setAccessibleDescription(ALLOC_ACCESS_DESCR);
            displayMemoryAllocDiff((AllocMemoryResultsDiff) ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        } else if (ls instanceof LivenessMemoryResultsDiff) {
            getAccessibleContext().setAccessibleDescription(LIVENESS_ACCESS_DESCR);
            displayMemoryLivenessDiff((LivenessMemoryResultsDiff) ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized SnapshotsDiffWindow get(ResultsSnapshot ls, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2) {
        // target component decides which column will be used for sorting
        return SnapshotsDiffWindow.get(ls, snapshot1, snapshot2, CommonConstants.SORTING_COLUMN_DEFAULT, false, null);
    }

    public static synchronized SnapshotsDiffWindow get(ResultsSnapshot ls, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                                       int sortingColumn, boolean sortingOrder, Project project) {
        return new SnapshotsDiffWindow(ls, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    public void componentActivated() {
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        }
    }

    public void componentDeactivated() {
        lastFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    protected String preferredID() {
        return this.getClass().getName();
    }

    // -- Private methods --------------------------------------------------------------------------------------------------
    private void displayMemoryAllocDiff(MemoryResultsSnapshot diff, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                        int sortingColumn, boolean sortingOrder, Project project) {
        MemoryDiffPanel allocDiffPanel = new MemoryDiffPanel(diff, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        updateFind(true, allocDiffPanel);
        add(allocDiffPanel, BorderLayout.CENTER);
        setName(ALLOC_CAPTION);
        setIcon(WINDOW_ICON_MEMORY);
    }

    private void displayMemoryLivenessDiff(MemoryResultsSnapshot diff, LoadedSnapshot snapshot1, LoadedSnapshot snapshot2,
                                           int sortingColumn, boolean sortingOrder, Project project) {
        MemoryDiffPanel livenessDiffPanel = new MemoryDiffPanel(diff, snapshot1, snapshot2, sortingColumn, sortingOrder, project);
        updateFind(true, livenessDiffPanel);
        add(livenessDiffPanel, BorderLayout.CENTER);
        setName(LIVENESS_CAPTION);
        setIcon(WINDOW_ICON_MEMORY);
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
