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

package org.graalvm.visualvm.core.snapshot;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.util.Objects;

/**
 * Abstract implementation of Snapshot.
 *
 * @author Jiri Sedlacek
 */
public abstract class Snapshot extends DataSource {
    
    /**
     * Named property for snapshot file.
     */
    public static final String PROPERTY_FILE = "prop_file"; // NOI18N
    
    private File file;
    private final SnapshotCategory category;
    
    private String snapshotID;
    
    
    /**
     * Creates new instance of AbstractSnapshot with the data stored in a file.
     * 
     * @param file file where snapshot is saved,
     * @param category category of the snapshot.
     */
    public Snapshot(File file, SnapshotCategory category) {
        this(file, category, null);
    }
    
    /**
     * Creates new instance of AbstractSnapshot with the data stored in a file and defined master.
     * 
     * @param file file where snapshot is saved,
     * @param category category of the snapshot,
     * @param master DataSource in whose window the snapshot will be displayed.
     */
    public Snapshot(File file, SnapshotCategory category, DataSource master) {
        super(master);
        this.file = file;
        this.category = category;
    }
    

    /**
     * Returns snapshot file or null if no file context is defined or snapshot doesn't support saving to file.
     * 
     * @return snapshot file or null if no file context is defined or snapshot doesn't support saving to file.
     */
    public final File getFile() {
        return file;
    }
    
    /**
     * Returns snapshot category.
     * 
     * @return snapshot category.
     */
    public final SnapshotCategory getCategory() {
        return category;
    }
    
    /**
     * Sets the file where data of this snapshot are stored.
     * 
     * @param newFile file where data of this snapshot are stored.
     */
    protected final void setFile(File newFile) {
        if (file == null && newFile == null) return;
        File oldFile = file;
        file = newFile;
        if (oldFile == null) snapshotID = null;
        getChangeSupport().firePropertyChange(PROPERTY_FILE, oldFile, newFile);
    }
    
    /**
     * Saves the snapshot to a directory.
     * 
     * @param directory directory where to save the snapshot.
     */
    public void save(File directory) {
        File f = getFile();
        if (f != null && f.isFile()) {  
            // File is not null and will be copied to the directory
            File saveFile = Utils.getUniqueFile(directory, f.getName());    
            Utils.copyFile(f, saveFile);
            // If there are any custom properties defined, store them to <file>.properties
            getStorage().saveCustomPropertiesTo(new File(saveFile.getAbsolutePath() + Storage.DEFAULT_PROPERTIES_EXT));
        }
    }
    
    /**
     * Returns true if the snapshot supports saving to an external (user defined) destination, false otherwise.
     * 
     * @return true if the snapshot supports saving to an external (user defined) destination, false otherwise.
     */
    public boolean supportsSaveAs() {
        return false;
    }
    
    /**
     * Saves the snapshot to an external (user defined) destination.
     * Default implementation does nothing, custom implementations should open
     * a Save File dialog and save the snapshot to selected destination.
     * Throws an UnsupportedOperationException if supportsSaveAs() returns false.
     */
    public void saveAs() {
        throw new UnsupportedOperationException("Save as not supported");   // NOI18N
    }
    
    /**
     * Returns true if the snapshot can be deleted by the user from UI, false otherwise.
     * 
     * @return true if the snapshot can be deleted by the user from UI, false otherwise.
     */
    public boolean supportsDelete() {
        return true;
    }
    
    /**
     * Deletes the snapshot.
     */
    public void delete() {
        DataSourceWindowManager.sharedInstance().closeDataSource(this);
        getOwner().getRepository().removeDataSource(this);
    }
    
    
    protected void remove() {
        final File f = getFile();
        if (f != null) Utils.FILE_QUEUE.post(new Runnable() {
            public void run() { Utils.delete(f, true); }
        });
        setFile(null);
        super.remove();
    }
    
    protected Storage createStorage() {
        File f = getFile();
        
        if (f != null) {
            String customPropertiesFileName = f.getName() + Storage.DEFAULT_PROPERTIES_EXT;
            if (f.isDirectory()) {
                return new Storage(f, customPropertiesFileName);
//                if (new File(f, customPropertiesFileName).exists()) return new Storage(f, customPropertiesFileName);
//                else return new Storage(f);
            } else if (f.isFile()) {
                File directory = f.getParentFile();
                return new Storage(directory, customPropertiesFileName);
//                if (new File(directory, customPropertiesFileName).exists()) return new Storage(directory, customPropertiesFileName);
//                else return new Storage(directory);
            }
        }
        
        return super.createStorage();
    }

    /**
     * Returns true if the Snapshot is present in other Snapshot's repository, false otherwise.
     *
     * @return true if the Snapshot is present in other Snapshot's repository
     *
     * @since VisualVM 1.3
     */
    protected final boolean isInSnapshot() {
        return getOwner() instanceof Snapshot;
    }
    
    
    /**
     * Returns ID of the Snapshot. The ID should be based on the snapshot file
     * if available and will only be computed for the first non-null file.
     *
     * @return ID of the Snapshot
     *
     * @since VisualVM 1.4
     */
    protected String computeSnapshotID() {
        File f = getFile();
        return f == null ? super.hashCode() + "-no_file" : f.getPath(); // NOI18N
    }
    
    private String getSnapshotID() {
        if (snapshotID == null) snapshotID = computeSnapshotID();
        return snapshotID;
    }
    
    
    public boolean equals(Object o) {
        if (!(o instanceof Snapshot)) return false;
        return Objects.equals(getSnapshotID(), ((Snapshot)o).getSnapshotID());
    }
    
    public int hashCode() {
        return Objects.hashCode(getSnapshotID());
    }

}
