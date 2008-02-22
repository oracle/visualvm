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

package com.sun.tools.visualvm.core.datasource;

import java.io.File;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Abstract implementation of Snapshot.
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractSnapshot extends AbstractDataSource implements Snapshot {
    
    private File file;
    
    
    /**
     * Creates new instance of AbstractSnapshot with the data stored in a file.
     * 
     * @param file file where snapshot is saved.
     */
    public AbstractSnapshot(File file) {
        this(file, null);
    }
    
    /**
     * Creates new instance of AbstractSnapshot with the data stored in a file and defined master.
     * 
     * @param file file where snapshot is saved,
     * @param master DataSource in whose window the snapshot will be displayed.
     */
    public AbstractSnapshot(File file, DataSource master) {
        super(master);
        this.file = file;
    }
    

    // NOTE: file can be null (snapshot not saved or doesn't support saving to file)
    public File getFile() {
        return file;
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
    
    /**
     * Deletes the file where data of this snapshot are stored.
     * Note that this method only deletes the file, not the Snapshot instance.
     * 
     * @return true if the file has been successfully deleted, false otherwise.
     */
    protected boolean deleteFile() {
        boolean deleted = false;
        
        File f = getFile();
        if (f != null) {
            if (f.isFile()) deleted = f.delete();
            else {
                FileObject directory = FileUtil.toFileObject(f);
                try {
                    directory.delete();
                    deleted = true;
                } catch (Exception e) {
                    deleted = false;
                }
            }
            setFile(null);
        }
        
        return deleted;
    }

}
