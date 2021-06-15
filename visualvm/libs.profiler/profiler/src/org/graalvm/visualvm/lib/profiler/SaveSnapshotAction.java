/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.profiler;

import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import javax.swing.*;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.ui.NBSwingWorker;

@NbBundle.Messages({
    "SaveSnapshotAction_ActionName=Save Snapshot",
    "SaveSnapshotAction_ActionDescr=Save Snapshot to Project"
})
class SaveSnapshotAction extends AbstractAction {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final LoadedSnapshot snapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    SaveSnapshotAction(LoadedSnapshot snapshot) {
        putValue(Action.NAME, Bundle.SaveSnapshotAction_ActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.SaveSnapshotAction_ActionDescr());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.SAVE));
        putValue("iconBase", Icons.getResource(GeneralIcons.SAVE)); // NOI18N
        this.snapshot = snapshot;
        updateState();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        new NBSwingWorker() {
            final private ProgressHandle ph = ProgressHandle.createHandle(Bundle.MSG_SavingSnapshot());
            @Override
            protected void doInBackground() {
                ph.setInitialDelay(500);
                ph.start();
                ResultsManager.getDefault().saveSnapshot(snapshot);
            }

            @Override
            protected void done() {
                ph.finish();
                updateState();
            }
        }.execute();
    }

    public void updateState() {
        setEnabled(!snapshot.isSaved());
    }
}
