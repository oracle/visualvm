/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.datasource.AbstractSnapshot;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import java.io.File;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/**
 * Support for snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class SnapshotsSupport {
    
    private static final String TEMPORARY_STORAGE_DIRNAME = "visualvm.dat";
    private static final String PERSISTENT_STORAGE_DIRNAME = "repository";
    
    private static SnapshotsSupport instance;
    
    private File temporaryStorageDirectory;
    private String temporaryStorageDirectoryString;
    private File persistentStorageDirectory;
    private String persistentStorageDirectoryString;


    /**
     * Returns singleton instance of SnapshotsSupport.
     * 
     * @return singleton instance of SnapshotsSupport.
     */
    public static synchronized SnapshotsSupport getInstance() {
        if (instance == null) instance = new SnapshotsSupport();
        return instance;
    }
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource data
     * 
     * @return default storage directory for temporary (runtime) DataSource data
     */
    public String getTemporaryStorageDirectoryString() {
        if (temporaryStorageDirectoryString == null)
            temporaryStorageDirectoryString = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + File.separator + TEMPORARY_STORAGE_DIRNAME;
        return temporaryStorageDirectoryString;
    }
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource data
     * 
     * @return default storage directory for temporary (runtime) DataSource data
     */
    public File getTemporaryStorageDirectory() {
        if (temporaryStorageDirectory == null) {
            String temporaryStorageString = getTemporaryStorageDirectoryString();
            temporaryStorageDirectory = new File(temporaryStorageString);
            if (temporaryStorageDirectory.exists() && temporaryStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString + ", file in the way");
            if (temporaryStorageDirectory.exists() && (!temporaryStorageDirectory.canRead() || !temporaryStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access temporary storage directory " + temporaryStorageString + ", read&write permission required");
            if (!temporaryStorageDirectory.exists() && !temporaryStorageDirectory.mkdir())
                throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString);
        }
        return temporaryStorageDirectory;
    }
    
    /**
     * Returns default storage directory for persistent DataSource data
     * 
     * @return default storage directory for persistent DataSource data
     */
    public String getPersistentStorageDirectoryString() {
        if (persistentStorageDirectoryString == null)
            persistentStorageDirectoryString = new File(System.getProperty("netbeans.user")).getAbsolutePath() + File.separator + PERSISTENT_STORAGE_DIRNAME;
        return persistentStorageDirectoryString;
    }
    
    /**
     * Returns default storage directory for persistent DataSource data
     * 
     * @return default storage directory for persistent DataSource data
     */
    public File getPersistentStorageDirectory() {
        if (persistentStorageDirectory == null) {
            String persistentStorageString = getPersistentStorageDirectoryString();
            persistentStorageDirectory = new File(persistentStorageString);
            if (persistentStorageDirectory.exists() && persistentStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString + ", file in the way");
            if (persistentStorageDirectory.exists() && (!persistentStorageDirectory.canRead() || !persistentStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access persistent storage directory " + persistentStorageString + ", read&write permission required");
            if (!persistentStorageDirectory.exists() && !persistentStorageDirectory.mkdir())
                throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString);
        }
        return persistentStorageDirectory;
    }
    
    
    public void saveAs(final AbstractSnapshot snapshot, String dialogTitle) {
        final File file = snapshot.getFile();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setSelectedFile(new File(snapshot.getFile().getName()));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(snapshot.getCategory().getFileFilter());
//        chooser.setFileView(category.getFileView());
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File copy = chooser.getSelectedFile();
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    ProgressHandle pHandle = null;
                    try {
                        pHandle = ProgressHandleFactory.createHandle("Saving " + DataSourceDescriptorFactory.getDescriptor(snapshot).getName() + "...");
                        pHandle.setInitialDelay(0);
                        pHandle.start();
                        copyFile(file, copy);
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
    
    public boolean copyFile(File file, File copy) {
        if (file == null || !file.isFile()) return false;
        
        FileObject fileO = FileUtil.toFileObject(file);
        FileObject directoryO = FileUtil.toFileObject(FileUtil.normalizeFile(copy.getParentFile()));
        try {
            FileUtil.copyFile(fileO, directoryO, file.getName(), "");
            return true;
        } catch (Exception e) {
            System.err.println("Error copying snapshot to " + copy + ": " + e.getMessage());
            return false;
        }
    }
    
    public String getTimeStamp(long time) {
        return org.netbeans.lib.profiler.utils.StringUtils.formatUserDate(new Date(time));
    }
    
    
    private SnapshotsSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new SnapshotsContainerDescriptorProvider());
        new SnapshotsContainerProvider().initialize();
        SnapshotActionProvider.getInstance().initialize();
    }

}
