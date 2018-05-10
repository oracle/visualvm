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

package org.graalvm.visualvm.application.snapshot;

import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 * Snapshot of an Application.
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationSnapshot extends Snapshot {
    
    private Storage storage;
    
    
    /**
     * Creates new instance of an application snapshot.
     * 
     * @param directory directory for snapshot data
     * @param storage Storage object for the snapshot.
     */
    public ApplicationSnapshot(File directory, Storage storage) {
        super(directory, ApplicationSnapshotsSupport.getInstance().getCategory());
        this.storage = storage;
    }
    
    
    // Save to a file within given directory
    public void save(File directory) {
        saveArchive(new File(directory, getFile().getName()));
    }
    
    public boolean supportsSaveAs() {
        return getFile() != null;
    }

    public void saveAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(NbBundle.getMessage(ApplicationSnapshot.class, "LBL_Save_Application_Snapshot_As")); // NOI18N
        chooser.setSelectedFile(new File(getFile().getName()));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(getCategory().getFileFilter());
        if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
            String categorySuffix = ApplicationSnapshotCategory.SUFFIX;
            String filePath = chooser.getSelectedFile().getAbsolutePath();
            if (!filePath.endsWith(categorySuffix)) filePath += categorySuffix;
            final File file = new File(filePath);
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    ProgressHandle pHandle = null;
                    try {
                        pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(ApplicationSnapshot.class, "MSG_Saving", DataSourceDescriptorFactory.getDescriptor(ApplicationSnapshot.this).getName()));  // NOI18N
                        pHandle.setInitialDelay(0);
                        pHandle.start();
                        saveArchive(file);
                    } finally {
                        final ProgressHandle pHandleF = pHandle;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { if (pHandleF != null) pHandleF.finish(); }
                        });
                    }
                }
            });
        }
    }
    
    
    protected Storage createStorage() {
        return storage;
    }
    
    
    private void saveArchive(File archive) {
        Utils.createArchive(getFile(), archive);
    }
    
}
