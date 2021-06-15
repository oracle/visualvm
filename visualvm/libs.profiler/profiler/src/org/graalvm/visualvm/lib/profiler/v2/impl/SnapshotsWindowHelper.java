/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.v2.impl;

import java.io.File;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.SnapshotResultsWindow;
import org.graalvm.visualvm.lib.profiler.SnapshotsListener;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.v2.SnapshotsWindow;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=SnapshotsListener.class)
public final class SnapshotsWindowHelper implements SnapshotsListener {

    static final WeakProcessor PROCESSOR = new WeakProcessor("Snapshots Window Processor"); // NOI18N

    public void snapshotTaken(LoadedSnapshot snapshot) {
        if (ProfilerIDESettings.getInstance().getAutoOpenSnapshot()) {
//            int sortingColumn = LiveResultsWindow.hasDefault() ? LiveResultsWindow.getDefault().getSortingColumn()
//                                                               : CommonConstants.SORTING_COLUMN_DEFAULT;
//            boolean sortingOrder = LiveResultsWindow.hasDefault() ? LiveResultsWindow.getDefault().getSortingOrder() : false;
//            ResultsManager.getDefault().openSnapshot(ls, sortingColumn, sortingOrder);

            ResultsManager.getDefault().openSnapshot(snapshot);
        }

        if (ProfilerIDESettings.getInstance().getAutoSaveSnapshot()) {
            ResultsManager.getDefault().saveSnapshot(snapshot);
            if (!ProfilerIDESettings.getInstance().getAutoOpenSnapshot())
                ResultsManager.getDefault().closeSnapshot(snapshot);
        }
    }

    public void snapshotLoaded(LoadedSnapshot snapshot) {}

    public void snapshotSaved(final LoadedSnapshot snapshot) {
        refreshSnapshots(snapshot);
        PROCESSOR.post(new Runnable() {
            public void run() { SnapshotsWindow.instance().snapshotSaved(snapshot); }
        });
    }

    public void snapshotRemoved(LoadedSnapshot snapshot) {
        SnapshotResultsWindow.closeWindow(snapshot);
        refreshSnapshots(snapshot);
    }

    private void refreshSnapshots(final LoadedSnapshot snapshot) {
        PROCESSOR.post(new Runnable() {
            public void run() {
                File f = snapshot.getFile();
                File p = f == null ? null : f.getParentFile();
                FileObject fo = p == null ? null : FileUtil.toFileObject(p);
                if (fo != null) SnapshotsWindow.instance().refreshFolder(fo, true);
            }
        });
    }
    
}
