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

import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.modules.profiler.api.project.ProjectStorage;
import org.netbeans.modules.profiler.spi.SessionListener;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;
import org.openide.util.lookup.ServiceProvider;


/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "HeapDumpWatch_OOME_PROTECTION_OPEN_HEAPDUMP=Profiled application crashed and generated heap dump.\nDo you wish to open it in heapwalker?",
    "HeapDumpWatch_OOME_PROTECTION_REMOVE_HEAPDUMP=You chose not to open the generated heap dump.\nThe heap dump can take a significant amount of disk space.\nShould it be deleted?"
})
@ServiceProvider(service=SessionListener.class)
public class HeapDumpWatch extends SessionListener.Adapter {
    private static final Logger LOG = Logger.getLogger(HeapDumpWatch.class.getName());
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private  class HeapDumpFolderListener extends FileChangeAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void fileDataCreated(FileEvent fileEvent) {
            captureHeapDump(fileEvent.getFile());
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FileObject monitoredPath;
    private HeapDumpFolderListener listener;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of HeapDumpWatch */
    public HeapDumpWatch() {
        listener = new HeapDumpFolderListener();
    }

    public static String getHeapDumpPath(Lookup.Provider project) {
        ProfilerIDESettings gps = ProfilerIDESettings.getInstance();
        if (!gps.isOOMDetectionEnabled()) return null;
        
        int oomeDetectionMode = gps.getOOMDetectionMode();

        switch (oomeDetectionMode) {
            case ProfilerIDESettings.OOME_DETECTION_TEMPDIR:
                return System.getProperty("java.io.tmpdir"); // NOI18N
            case ProfilerIDESettings.OOME_DETECTION_PROJECTDIR:

                try {
                    return FileUtil.toFile(ProjectStorage.getSettingsFolder(project, true)).getAbsolutePath();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Cannot resolve project settings directory:\n" + e.getMessage(), e);
                    
                    return null;
                }
            case ProfilerIDESettings.OOME_DETECTION_CUSTOMDIR:
                return gps.getCustomHeapdumpPath();
        }

        return null;
    }
    
    @Override
    public void onShutdown() {
        release();
    }

    @Override
    public void onStartup(ProfilingSettings ps, Provider p) {
        if (ProfilerIDESettings.getInstance().isOOMDetectionEnabled()) {
            String oomePath = getHeapDumpPath(p);
            if (oomePath != null) {
                monitor(oomePath);
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    private void monitor(String path) throws IllegalArgumentException {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("The path \"" + path + "\" can't be null."); // NOI18N
        }

        FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(new File(path)));

        if (fo != null) {
            if (!fo.isFolder()) {
                throw new IllegalArgumentException("The given path \"" + path + "\" is invalid. It must be a folder"); // NOI18N
            }

            fo.getChildren();
            fo.addFileChangeListener(listener);
            monitoredPath = fo;
        }
    }
    
    private void captureHeapDump(FileObject heapDump) {
        if (!heapDump.getExt().equals(ResultsManager.HEAPDUMP_EXTENSION)) {
            return; // NOI18N
        }

        if (heapDump.getName().startsWith(HeapDumpAction.TAKEN_HEAPDUMP_PREFIX)) {
            return; // custom heapdump
        }

        ProfilerControlPanel2.getDefault().refreshSnapshotsList(); // refresh list of snapshots

        try {
            if (ProfilerDialogs.displayConfirmation(Bundle.HeapDumpWatch_OOME_PROTECTION_OPEN_HEAPDUMP())) {
                ResultsManager.getDefault().openSnapshot(heapDump);
            } else if (ProfilerDialogs.displayConfirmation(Bundle.HeapDumpWatch_OOME_PROTECTION_REMOVE_HEAPDUMP())) {
                heapDump.delete();
                ProfilerControlPanel2.getDefault().refreshSnapshotsList();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            release();
        }
    }

    private void release() {
        if (monitoredPath != null) {
            monitoredPath.removeFileChangeListener(listener);
            monitoredPath = null;
        }
    }
}
