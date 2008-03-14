/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.application;

import com.sun.tools.visualvm.application.snapshot.ApplicationSnapshotsSupport;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationSnapshot extends Snapshot {
    
    private Storage storage;
    
    
    public ApplicationSnapshot(File directory, Storage storage) {
        super(directory, ApplicationSnapshotsSupport.getInstance().getCategory());
        this.storage = storage;
    }
    
    
    // Save to a file within given directory
    public void save(File directory) {
        saveArchive(new File(directory, getFile().getName()));
    }
    
    public boolean supportsSaveAs() {
        return true;
    }
    
    public void saveAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Application Snapshot As");
        chooser.setSelectedFile(new File(getFile().getName()));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(getCategory().getFileFilter());
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    ProgressHandle pHandle = null;
                    try {
                        pHandle = ProgressHandleFactory.createHandle("Saving " + DataSourceDescriptorFactory.getDescriptor(ApplicationSnapshot.this).getName() + "...");
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
    
        
    public void delete() {
        super.delete();
//        ApplicationSnapshotsSupport.getInstance().getSnapshotProvider().unregisterSnapshot(this);
        // NOTE: instance should be also unregistered from the provider but this won't
        // be neccessary once DataSourceRepository will track DataSources automatically
        getOwner().getRepository().removeDataSource(this);
    }
    
    
    private void saveArchive(File archive) {
        Utils.createArchive(getFile(), archive);
    }
    
}
