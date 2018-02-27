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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.api.ActionsSupport;
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
        
        final String filterKey = org.netbeans.lib.profiler.ui.swing.FilterUtils.FILTER_ACTION_KEY;
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
