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

package org.netbeans.modules.profiler.actions;

import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;


/**
 * An action to prompt to select file and load/open snapshot from it.
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LoadSnapshotAction_ActionName=&Load Snapshot...",
    "LoadSnapshotAction_ActionDescr=Load profiling results snapshot from disk",
    "LoadSnapshotAction_OpenSnapshotDialogCaption=Open Snapshot File",
    "LoadSnapshotAction_ProfilerSnapshotFileFilter=Profiler Snapshot Files (*.{0})",
    "LoadSnapshotAction_OpenSnapshotHeapdumpDialogCaption=Open Snapshot or Heap Dump",
    "LoadSnapshotAction_ProfilerSnapshotHeapdumpFileFilter=Profiler Snapshot or Heap Dump Files (*.{0} | *.{1})",
    "LoadSnapshotAction_No_Snapshot_Selected=Not a .nps snapshot file"
})
@ActionID(id = "org.netbeans.modules.profiler.actions.LoadSnapshotAction", category = "Profile")
@ActionRegistration(iconInMenu = true, displayName = "#LoadSnapshotAction_ActionName", iconBase = "org/netbeans/modules/profiler/impl/icons/openSnapshot.png")
@ActionReference(path = "Menu/Profile", position = 1400)
public final class LoadSnapshotAction extends AbstractAction {
    //~ Static fields/initializers ----------------------------------------------------------------------------------------------- 
    private static File importDir;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LoadSnapshotAction() {
        putValue(Action.SHORT_DESCRIPTION, Bundle.LoadSnapshotAction_ActionDescr());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        loadSnapshot(false);
    }

    public void loadSnapshotOrHeapdump() {
        loadSnapshot(true);
    }

    // NOTE: supports also loading HeapDumps to simplify implementation of Load button in Control Panel
    private void loadSnapshot(final boolean handleHeapdumps) {
        JFileChooser chooser = new JFileChooser();

        if (importDir != null) {
            chooser.setCurrentDirectory(importDir);
        }

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setDialogTitle(handleHeapdumps ? 
            Bundle.LoadSnapshotAction_OpenSnapshotHeapdumpDialogCaption() : 
            Bundle.LoadSnapshotAction_OpenSnapshotDialogCaption());
        chooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    String fname = f.getName();
                    if (fname.endsWith("." + ResultsManager.SNAPSHOT_EXTENSION)) return true;
                    if (fname.endsWith("." + ResultsManager.STACKTRACES_SNAPSHOT_EXTENSION)) return true;
                    if (handleHeapdumps && fname.endsWith("." + ResultsManager.HEAPDUMP_EXTENSION)) return true;
                    return false;
                }

                public String getDescription() {
                    return handleHeapdumps
                           ? Bundle.LoadSnapshotAction_ProfilerSnapshotHeapdumpFileFilter(
                                ResultsManager.SNAPSHOT_EXTENSION, 
                                ResultsManager.HEAPDUMP_EXTENSION)
                           : Bundle.LoadSnapshotAction_ProfilerSnapshotFileFilter(
                                ResultsManager.SNAPSHOT_EXTENSION);
                }
            });

        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
            final File[] files = chooser.getSelectedFiles();
            final ArrayList<FileObject> snapshotsFOArr = new ArrayList();
            final ArrayList<File> heapdumpsFArr = new ArrayList();

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String fname = file.getName();

                if (fname.endsWith("." + ResultsManager.SNAPSHOT_EXTENSION) || fname.endsWith("." + ResultsManager.STACKTRACES_SNAPSHOT_EXTENSION)) {
                    snapshotsFOArr.add(FileUtil.toFileObject(FileUtil.normalizeFile(file))); // NOI18N
                } else if (fname.endsWith("." + ResultsManager.HEAPDUMP_EXTENSION)) {
                    heapdumpsFArr.add(file); // NOI18N
                }
            }

            if (!snapshotsFOArr.isEmpty()) {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        final LoadedSnapshot[] imported = ResultsManager.getDefault().loadSnapshots(
                                snapshotsFOArr.toArray(new FileObject[snapshotsFOArr.size()]));
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ResultsManager.getDefault().openSnapshots(imported);
                            }
                        });
                    }
                });
            } else if (!handleHeapdumps) {
                ProfilerDialogs.displayError(Bundle.LoadSnapshotAction_No_Snapshot_Selected());

            }

            if (!heapdumpsFArr.isEmpty()) {
                RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            for (File heapDump : heapdumpsFArr) {
                                ResultsManager.getDefault().openSnapshot(heapDump);
                            }
                        }
                    });

            }

            importDir = chooser.getCurrentDirectory();
        }
    }
}
