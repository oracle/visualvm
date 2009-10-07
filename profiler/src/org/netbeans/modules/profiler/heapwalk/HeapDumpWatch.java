/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.heapwalk;

import org.netbeans.modules.profiler.ProfilerControlPanel2;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.NotifyDescriptor;
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


/**
 *
 * @author Jaroslav Bachorik
 */
public class HeapDumpWatch {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class HeapDumpFolderListener extends FileChangeAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void fileDataCreated(FileEvent fileEvent) {
            captureHeapDump(fileEvent.getFile());
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String OOME_PROTECTION_ENABLED_KEY = "profiler.info.oomeprotection"; // NOI18N
    public static final String OOME_PROTECTION_DUMPPATH_KEY = "profiler.info.oomeprotection.dumppath"; // NOI18N
    private static HeapDumpWatch instance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Collection<FileObject> watchedFolders;
    private HeapDumpFolderListener listener;
    private Thread refreshThread;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of HeapDumpWatch */
    private HeapDumpWatch() {
        watchedFolders = new ArrayList<FileObject>();
        listener = new HeapDumpFolderListener();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized HeapDumpWatch getDefault() {
        if (instance == null) {
            instance = new HeapDumpWatch();
        }

        return instance;
    }

    public void monitor(String path) throws IllegalArgumentException {
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
            watchedFolders.add(fo);
        }
    }

    public void releaseAll() {
        for (FileObject fo : watchedFolders) {
            fo.removeFileChangeListener(listener);
        }

        watchedFolders.clear();
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
            NotifyDescriptor ndOpen = new NotifyDescriptor.Confirmation(NbBundle.getMessage(this.getClass(),
                                                                                            "OOME_PROTECTION_OPEN_HEAPDUMP"),
                                                                        NotifyDescriptor.YES_NO_OPTION,
                                                                        NotifyDescriptor.QUESTION_MESSAGE); // NOI18N
            NotifyDescriptor ndDelete = new NotifyDescriptor.Confirmation(NbBundle.getMessage(this.getClass(),
                                                                                              "OOME_PROTECTION_REMOVE_HEAPDUMP"),
                                                                          NotifyDescriptor.YES_NO_OPTION,
                                                                          NotifyDescriptor.QUESTION_MESSAGE); // NOI18N

            if (ProfilerDialogs.notify(ndOpen) == NotifyDescriptor.YES_OPTION) {
                HeapWalkerManager.getDefault().openHeapWalker(FileUtil.toFile(heapDump));
            } else {
                if (ProfilerDialogs.notify(ndDelete) == NotifyDescriptor.YES_OPTION) {
                    heapDump.delete();
                    ProfilerControlPanel2.getDefault().refreshSnapshotsList();
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            release(heapDump.getParent());
        }
    }

    private void release(FileObject watchedFolder) {
        watchedFolder.removeFileChangeListener(listener);
        watchedFolders.remove(watchedFolder);
    }
}
