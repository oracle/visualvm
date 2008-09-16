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

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import java.io.File;

/**
 * Abstract implementation of DataSourceDescriptor for snapshots.
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotDescriptor<X extends Snapshot> extends DataSourceDescriptor<X> {
    
    /**
     * Creates new instance of SnapshotDescriptor.
     * 
     * @param snapshot Snapshot for the descriptor.
     * @param icon icon for the Snapshot.
     */
    public SnapshotDescriptor(X snapshot, Image icon) {
        super(snapshot, resolveName(snapshot),
              snapshot.getFile().getAbsolutePath(),
              icon, POSITION_AT_THE_END, EXPAND_NEVER);
    }
    
    private static String resolveName(Snapshot snapshot) {
        String persistedName = snapshot.getStorage().getCustomProperty(PROPERTY_NAME);
        if (persistedName != null) return persistedName;
        
        File file = snapshot.getFile();
        if (file == null) return snapshot.toString();
        
        String fileName = file.getName();
        SnapshotCategory category = snapshot.getCategory();
        String name = "[" + category.getPrefix() + "] " + fileName; // NOI18N
        
        if (category.isSnapshot(file)) {
            String timeStamp = category.getTimeStamp(fileName);
            if (timeStamp != null) name = "[" + category.getPrefix() + "] " + timeStamp;    // NOI18N
        }
        
        return name;
    }
    
    /**
     * Returns true if the snapshot can be renamed from UI, false otherwise.
     * 
     * @return true if the snapshot can be renamed from UI, false otherwise.
     */
    public boolean supportsRename() {
        return true;
    }

}
