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

import com.sun.tools.visualvm.core.profiler.*;
import java.io.File;

/**
 * Support for snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public class SnapshotSupport {
    
    private static final String DEFAULT_DATASOURCES_STORAGE_DIR = "visualvm.dat";
    
    private String defaultStorageDirectoryString;
    private File defaultStorageDirectory;

    private static SnapshotSupport instance;


    /**
     * Returns singleton instance of SnapshotSupport.
     * 
     * @return singleton instance of SnapshotSupport.
     */
    public static synchronized SnapshotSupport getInstance() {
        if (instance == null) instance = new SnapshotSupport();
        return instance;
    }
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource snapshots
     * 
     * @return default storage directory for temporary (runtime) DataSource snapshots
     */
    public String getTemporaryStorageDirectoryString() {
        if (defaultStorageDirectoryString == null)
            defaultStorageDirectoryString = System.getProperty("java.io.tmpdir") + File.separator + DEFAULT_DATASOURCES_STORAGE_DIR;
        return defaultStorageDirectoryString;
    }
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource snapshots
     * 
     * @return default storage directory for temporary (runtime) DataSource snapshots
     */
    public File getTemporaryStorageDirectory() {
        if (defaultStorageDirectory == null) {
            String defaultStorageString = getTemporaryStorageDirectoryString();
            defaultStorageDirectory = new File(defaultStorageString);
            if (!defaultStorageDirectory.exists() && !defaultStorageDirectory.mkdir())
                throw new IllegalStateException("Cannot create storage directory " + defaultStorageString);
        }
        return defaultStorageDirectory;
    }
    
    
    private SnapshotSupport() {
    }

}
