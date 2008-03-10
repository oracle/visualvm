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

import com.sun.tools.visualvm.core.datasource.*;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import java.io.File;


/**
 * Represents a persistable DataSource which can be saved to a file.
 *
 * @author Jiri Sedlacek
 */
public interface Snapshot extends DataSource {
    
    /**
     * Named property for snapshot file.
     */
    public static final String PROPERTY_FILE = "prop_file";
    
    /**
     * Returns file context of this snapshot.
     * 
     * @return file context of this snapshot.
     */
    public File getFile();
    
    /**
     * Returns category of this snapshot.
     * 
     * @return category of this snapshot.
     */
    public SnapshotCategory getCategory();
    
    /**
     * Invoked when the snapshot should be saved into some location.
     * This happens for example when saving ThreadDump to ApplicationSnapshot.
     * 
     * Note that saving snapshot data into directories isn't currently supported,
     * snapshot should always be saved into file/files.
     * 
     * @param directory directory where to save the snapshot.
     */
    public void save(File directory);
    
    /**
     * Returns true if the Save As... action should be available for this snapshot, false otherwise.
     * 
     * @return true if the Save As... action should be available for this snapshot, false otherwise.
     */
    public boolean supportsSaveAs();
    
    /**
     * Ivoked when Save As action has been invoked by the user.
     */
    public void saveAs();
    
    public boolean supportsDelete();
    
    public void delete();
    
}
