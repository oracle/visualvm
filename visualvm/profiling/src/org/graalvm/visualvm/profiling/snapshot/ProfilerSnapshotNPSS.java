/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package com.sun.tools.visualvm.profiling.snapshot;

import com.sun.tools.visualvm.core.datasource.DataSource;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.swing.JComponent;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.snaptracer.impl.IdeSnapshot;
import org.netbeans.modules.profiler.snaptracer.impl.TracerController;
import org.netbeans.modules.profiler.snaptracer.impl.TracerModel;
import org.netbeans.modules.profiler.snaptracer.impl.TracerView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * This is prototype to handle npss files.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
@NbBundle.Messages("MSG_SnapshotLoadFailedMsg=Error while loading snapshot: {0}")
class ProfilerSnapshotNPSS extends ProfilerSnapshot {

    private IdeSnapshot loadedSnapshot;

    ProfilerSnapshotNPSS(File file, DataSource master) {
        super(file, master);
        try {
            FileObject primary = FileUtil.toFileObject(file);
            FileObject uigestureFO = primary.getParent().getFileObject(primary.getName(), "log"); // NOI18N
            loadedSnapshot = new IdeSnapshot(primary, uigestureFO);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public LoadedSnapshot getLoadedSnapshot() {
        throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
    }

    @Override
    Image resolveIcon() {
        return ImageUtilities.mergeImages(CPU_ICON, NODE_BADGE, 0, 0);
    }

    @Override
    protected void remove() {
        super.remove();
        loadedSnapshot = null;
    }

    @Override
    JComponent getUIComponent() {
        TracerModel model = new TracerModel(loadedSnapshot);
        TracerController controller = new TracerController(model);
        TracerView view = new TracerView(model, controller);
        return view.createComponent();
    }

    @Override
    void closeComponent() {
        // no op
    }
}
