/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.profiling.snapshot;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.profiling.actions.ProfilerResultsAction;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.SnapshotResultsWindow;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ProfilerSnapshotNPS extends ProfilerSnapshot {

    private static final Logger LOGGER = Logger.getLogger(ProfilerSnapshotNPS.class.getName());
    private LoadedSnapshot loadedSnapshot;
    private TopComponent srw;

    ProfilerSnapshotNPS() {
        super();
    }

    ProfilerSnapshotNPS(File snapshot, DataSource master) {
        super(snapshot, master);
        FileObject fobj = FileUtil.toFileObject(FileUtil.normalizeFile(snapshot));
        loadedSnapshot = ResultsManager.getDefault().loadSnapshot(fobj);
    }

    @Override
    public LoadedSnapshot getLoadedSnapshot() {
        return loadedSnapshot;
    }

    @Override
    protected void remove() {
        super.remove();
        ResultsManager.getDefault().closeSnapshot(loadedSnapshot);

    }

    @Override
    protected Image resolveIcon() {
        try {
            int snapshotType = getLoadedSnapshot().getType();
            switch (snapshotType) {
                case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                    return ImageUtilities.mergeImages(CPU_ICON, NODE_BADGE, 0, 0);
                case LoadedSnapshot.SNAPSHOT_TYPE_CPU_JDBC:
                    return ImageUtilities.mergeImages(JDBC_ICON, NODE_BADGE, 0, 0);
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
                    return ImageUtilities.mergeImages(MEMORY_ICON, NODE_BADGE, 0, 0);
                default:
                    // Fallback icon, cannot return null - throws NPE in DataSourceView
                    return ImageUtilities.mergeImages(SNAPSHOT_ICON, NODE_BADGE, 0, 0);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to determine profiler snapshot type", e);  // NOI18N
            // Fallback icon, cannot return null - throws NPE in DataSourceView
            return ImageUtilities.mergeImages(SNAPSHOT_ICON, NODE_BADGE, 0, 0);
        }
    }

    @Override
    JComponent getUIComponent() {
        if (srw == null) {
            srw = SnapshotResultsWindow.get(loadedSnapshot, CommonConstants.SORTING_COLUMN_DEFAULT, false);
            srw.setPreferredSize(new Dimension(1, 1));
            
            DataSource master = getMaster();
            if (master instanceof Application && srw.getComponentCount() > 0) {
                Component c = srw.getComponent(0);
                if (c instanceof JComponent) ((JComponent)c).putClientProperty(ProfilerResultsAction.PROP_APPLICATION, master);
            }
        }
        return srw;
    }

    @Override
    void closeComponent() {
        if (srw != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        Method method = srw.getClass().getDeclaredMethod("componentClosed");   // NOI18N
                        if (method != null) {
                            method.setAccessible(true);
                            method.invoke(srw);
                        }
                    } catch (NoSuchMethodException noSuchMethodException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                "removed", noSuchMethodException);   // NOI18N
                    } catch (SecurityException securityException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                "removed", securityException);   // NOI18N
                    } catch (IllegalAccessException illegalAccessException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                "removed", illegalAccessException);   // NOI18N
                    } catch (IllegalArgumentException illegalArgumentException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                "removed", illegalArgumentException);   // NOI18N
                    } catch (InvocationTargetException invocationTargetException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                "removed", invocationTargetException);   // NOI18N
                    }
                    srw = null;
                }
            });
        }
    }
}
