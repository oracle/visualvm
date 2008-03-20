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

package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import java.io.File;

/**
 * Abstract implementation of Snapshot.
 *
 * @author Jiri Sedlacek
 */
public abstract class Snapshot extends DataSource {
    
    /**
     * Named property for snapshot file.
     */
    public static final String PROPERTY_FILE = "prop_file";
    
    private File file;
    private final SnapshotCategory category;
    
    
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
    

    // NOTE: file can be null (snapshot not saved or doesn't support saving to file)
    public File getFile() {
        return file;
    }
    
    public SnapshotCategory getCategory() {
        return category;
    }
    
    /**
     * Sets the file where data of this snapshot are stored.
     * 
     * @param newFile file where data of this snapshot are stored.
     */
    protected void setFile(File newFile) {
        if (file == null && newFile == null) return;
        File oldFile = file;
        file = newFile;
        getChangeSupport().firePropertyChange(PROPERTY_FILE, oldFile, newFile);
    }
    
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
    
    public boolean supportsSaveAs() {
        return false;
    }
    
    public void saveAs() {
        throw new UnsupportedOperationException("Save as not supported");
    }
    
    public boolean supportsDelete() {
        return true;
    }
    
    public void delete() {
        getOwner().getRepository().removeDataSource(this);
    }
    
    
    protected void remove() {
        File f = getFile();
        if (f != null) Utils.delete(f, true);
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

}
