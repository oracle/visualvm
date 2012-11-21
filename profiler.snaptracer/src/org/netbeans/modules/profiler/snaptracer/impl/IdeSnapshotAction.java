/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.profiler.snaptracer.impl;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
public final class IdeSnapshotAction implements ActionListener {

    private File lastDirectory;
    
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final File file = snapshotFile();
                if (file == null) return;
                openSnapshot(FileUtil.toFileObject(file));
            }
        });
    }
    
    @NbBundle.Messages("MSG_SnapshotLoadFailedMsg=Error while loading snapshot {0}:\n{1}")
    static void openSnapshot(final FileObject primary) {
        TracerSupportImpl.getInstance().perform(new Runnable() {
            public void run() {
                try {
                    // First try to find the log file in the same directory, same file name with .log extension
                    FileObject uigestureFO = primary.getParent().getFileObject(primary.getName(), "log"); // NOI18N
                    // Second try to find the log file using the virtual filesystem
                    if (uigestureFO == null) uigestureFO = primary.getParent().getFileObject(primary.getPath(), "log"); // NOI18N
                    IdeSnapshot snapshot = new IdeSnapshot(primary, uigestureFO);
                    openSnapshotImpl(snapshot);
                } catch (Throwable t) {
                    ProfilerDialogs.displayError(Bundle.MSG_SnapshotLoadFailedMsg(
                                                 primary.getNameExt(), t.getLocalizedMessage()));
                }
            }
        });
    }

    private static void openSnapshotImpl(final IdeSnapshot snapshot) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TracerModel model = new TracerModel(snapshot);
                TracerController controller = new TracerController(model);
                TopComponent ui = ui(model, controller, snapshot.getNpssFileName(), snapshot.getNpssFilePath());
                ui.open();
                ui.requestActive();
            }
        });
    }

    private static TopComponent ui(TracerModel model, TracerController controller, String npssFileName, String npssFilePath) {
        TopComponent tc = new IdeSnapshotComponent(npssFileName, npssFilePath);
        TracerView tracer = new TracerView(model, controller);
        tc.add(tracer.createComponent(), BorderLayout.CENTER);
        return tc;
    }

    private File snapshotFile() {
        JFileChooser chooser = createFileChooser(lastDirectory);
        Frame mainWindow = WindowManager.getDefault().getMainWindow();
        if (chooser.showOpenDialog(mainWindow) == JFileChooser.APPROVE_OPTION) {
            lastDirectory = chooser.getCurrentDirectory();
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    @NbBundle.Messages({
        "ACTION_IdeSnapshot_dialog=Load IDE Snapshot",
        "ACTION_IdeSnapshot_filter=IDE Snapshots"
    })
    private static JFileChooser createFileChooser(File directory) {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle(Bundle.ACTION_IdeSnapshot_dialog());
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        chooser.setAcceptAllFileFilterUsed(false);
        if (directory != null) {
            chooser.setCurrentDirectory(directory);
        }
        
        String descr = Bundle.ACTION_IdeSnapshot_filter();
        String ext = "."+ResultsManager.STACKTRACES_SNAPSHOT_EXTENSION; // NOI18N
        Filter filter = Filter.create(descr, ext);
        chooser.addChoosableFileFilter(filter);

        return chooser;
    }

    private static class IdeSnapshotComponent extends ProfilerTopComponent {

        IdeSnapshotComponent(String npssFileName, String npssFilePath) {
            setDisplayName(npssFileName);
            setToolTipText(npssFilePath);
            setLayout(new BorderLayout());
        }

        public int getPersistenceType() { return PERSISTENCE_NEVER; }

    }

    private static abstract class Filter extends FileFilter {

        abstract String getExt();

        static Filter create(final String descr, final String ext) {
            return new Filter() {
                public boolean accept(File f) {
                    return f.isDirectory() || getFileExt(f.getName()).equals(ext);
                }
                public String getExt() {
                    return ext;
                }
                public String getDescription() {
                    return descr + " (*" + ext + ")";  // NOI18N
                }
            };
        }

        private static String getFileExt(String fileName) {
            int extIndex = fileName.lastIndexOf('.'); // NOI18N
            if (extIndex == -1) return ""; // NOI18N
            return fileName.substring(extIndex);
        }

        private Filter() {}

    }

}
