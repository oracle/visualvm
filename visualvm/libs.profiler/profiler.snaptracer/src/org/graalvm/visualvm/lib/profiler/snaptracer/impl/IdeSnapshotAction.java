/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.profiler.ProfilerTopComponent;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.ActionsSupport;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
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
                    FileObject uigestureFO = primary.getParent().getFileObject(primary.getName(), "log"); // NOI18N
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
                TopComponent ui = ui(model, controller, snapshot.getNpssFileObject());
                ui.open();
                ui.requestActive();
            }
        });
    }

    private static TopComponent ui(TracerModel model, TracerController controller, FileObject snapshotFo) {
        String npssFileName = snapshotFo.getName();
        TopComponent tc = new IdeSnapshotComponent(npssFileName, FileUtil.toFile(snapshotFo));
        final JComponent tracer = new TracerView(model, controller).createComponent();
        tc.add(tracer, BorderLayout.CENTER);
        
        InputMap inputMap = tc.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = tc.getActionMap();
        
        final String filterKey = org.graalvm.visualvm.lib.ui.swing.FilterUtils.FILTER_ACTION_KEY;
        Action filterAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Action action = tracer.getActionMap().get(filterKey);
                if (action != null && action.isEnabled()) action.actionPerformed(e);
            }
        };
        ActionsSupport.registerAction(filterKey, filterAction, actionMap, inputMap);
        
        final String findKey = SearchUtils.FIND_ACTION_KEY;
        Action findAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Action action = tracer.getActionMap().get(findKey);
                if (action != null && action.isEnabled()) action.actionPerformed(e);
            }
        };
        ActionsSupport.registerAction(findKey, findAction, actionMap, inputMap);
        
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

        IdeSnapshotComponent(String displayName, File npssFile) {
            setDisplayName(displayName);
            if (npssFile != null) {
                putClientProperty(ProfilerTopComponent.RECENT_FILE_KEY, npssFile);
                setToolTipText(npssFile.getAbsolutePath());
            }
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
