/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ExportSnapshotAction_ActionName=Export IDE snapshot...",
    "ExportSnapshotAction_ActionDescr=Export IDE snapshot...",
    "ExportSnapshotAction_ProgressMsg=Exporting snapshot...",
    "ExportSnapshotAction_CannotReplaceMsg=File {0} cannot be replaced.\nCheck file permissions.",
    "ExportSnapshotAction_ExportFailedMsg=Exporting snapshot failed:",
    "ExportSnapshotAction_FileChooserCaption=Select File or Directory",
    "ExportSnapshotAction_ExportButtonText=Export",
    "ExportSnapshotAction_NpssFileFilter=IDE Snapshots (*{0})",
    "ExportSnapshotAction_ExportToItselfMsg=Exporting the snapshot to itself.",
    "ExportSnapshotAction_OverwriteFileCaption=Overwrite Existing File",
    "ExportSnapshotAction_OverwriteFileMsg=File {0} already exists.\nDo you want to replace it?"
})
final class ExportSnapshotAction extends AbstractAction {

    private static final String NPSS_EXT = "."+ResultsManager.STACKTRACES_SNAPSHOT_EXTENSION; // NOI18N
    private static String LAST_DIRECTORY;

    private final FileObject snapshotFileObject;
    
    
    ExportSnapshotAction(FileObject snapshot) {
        snapshotFileObject = snapshot;
        
        putValue(Action.NAME, Bundle.ExportSnapshotAction_ActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.ExportSnapshotAction_ActionDescr());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.EXPORT));
        putValue("iconBase", Icons.getResource(GeneralIcons.EXPORT)); // NOI18N
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFileChooser chooser = createFileChooser();
                String filename = snapshotFileObject.getName();
                File lastDir = LAST_DIRECTORY != null ? new File(LAST_DIRECTORY) :
                                                        chooser.getCurrentDirectory();
                chooser.setSelectedFile(new File(lastDir, filename));
                Component parent = WindowManager.getDefault().getRegistry().getActivated();
                if (parent == null) parent = WindowManager.getDefault().getMainWindow();
                if (chooser.showDialog(parent, null) != JFileChooser.APPROVE_OPTION) return;
                File selected = chooser.getSelectedFile();
                if (selected.isDirectory()) {
                    LAST_DIRECTORY = selected.getAbsolutePath();
                    selected = new File(selected, filename);
                } else {
                    LAST_DIRECTORY = selected.getParent();
                }
                filename = selected.getName();
                if (!filename.toLowerCase(Locale.ENGLISH).endsWith(NPSS_EXT)) {
                    filename+=NPSS_EXT;
                    selected = new File(selected.getParentFile(), filename);
                }
                if (!checkItselfOrOverwrite(snapshotFileObject, selected)) actionPerformed(e);
                else export(snapshotFileObject, selected);
            }
        });
    }
    
    // TODO: export also UI gestures file if available, preferably based on user option
    private static void export(final FileObject sourceFO, final File targetFile) {
        final ProgressHandle progress = ProgressHandle.createHandle(
                Bundle.ExportSnapshotAction_ProgressMsg());
        progress.setInitialDelay(500);
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                progress.start();
                try {
                    if (targetFile.exists() && !targetFile.delete()) {
                        ProfilerDialogs.displayError(
                                Bundle.ExportSnapshotAction_CannotReplaceMsg(targetFile.getName()));
                    } else {
                        targetFile.toPath();
                        File targetParent = FileUtil.normalizeFile(targetFile.getParentFile());
                        FileObject targetFO = FileUtil.toFileObject(targetParent);
                        String targetName = targetFile.getName();
                        FileUtil.copyFile(sourceFO, targetFO, targetName, null);
                    }
                } catch (Throwable t) {
                    ProfilerLogger.log("Failed to export NPSS snapshot: " + t.getMessage()); // NOI18N
                    String msg = t.getLocalizedMessage().replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
                    ProfilerDialogs.displayError("<html><b>" + Bundle.ExportSnapshotAction_ExportFailedMsg() + // NOI18N
                                                               "</b><br><br>" + msg + "</html>"); // NOI18N
                } finally {
                    progress.finish();
                }
            }
        });
    }
    
    private static JFileChooser createFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setDialogTitle(Bundle.ExportSnapshotAction_FileChooserCaption());
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setApproveButtonText(Bundle.ExportSnapshotAction_ExportButtonText());
        fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
        fileChooser.addChoosableFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(NPSS_EXT);
            }
            public String getDescription() {
                return Bundle.ExportSnapshotAction_NpssFileFilter(NPSS_EXT);
            }
        });
        return fileChooser;
    }
    
    private static boolean checkItselfOrOverwrite(FileObject sourceFO, File target) {
        if (!target.exists()) {
            return true;
        }
        File source = FileUtil.toFile(sourceFO);
        if (source == null) {   // sourceFO is in memory
            return true;
        }
        if (source.equals(target)) {
            ProfilerDialogs.displayError(Bundle.ExportSnapshotAction_ExportToItselfMsg());
            return false;
        } else {
            return ProfilerDialogs.displayConfirmation(
                    Bundle.ExportSnapshotAction_OverwriteFileMsg(target.getName()),
                    Bundle.ExportSnapshotAction_OverwriteFileCaption());
        }
    }
    
}
