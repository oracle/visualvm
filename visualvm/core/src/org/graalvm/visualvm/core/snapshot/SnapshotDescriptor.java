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

import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import java.io.File;
import org.openide.util.NbBundle;

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
        this(snapshot, null, icon);
    }

    /**
     * Creates new instance of SnapshotDescriptor.
     *
     * @param snapshot Snapshot for the descriptor.
     * @param description description of the snapshot
     * @param icon icon for the Snapshot.
     *
     * @since VisualVM 1.3
     */
    public SnapshotDescriptor(X snapshot, String description, Image icon) {
        super(snapshot, resolveSnapshotName(snapshot), description, icon,
              resolvePosition(snapshot, POSITION_AT_THE_END, snapshot.
              isInSnapshot()), EXPAND_NEVER);
    }

    /**
     * Creates new instance of SnapshotDescriptor.
     *
     * @param snapshot snapshot.
     * @param name snapshot name.
     * @param description snapshot description.
     * @param icon snapshot icon.
     * @param position snapshot position.
     * @param autoExpansionPolicy snapshot expansion policy.
     *
     * @since VisualVM 1.3
     */
    public SnapshotDescriptor(X snapshot, String name, String description,
                              Image icon, int position, int autoExpansionPolicy) {
        super(snapshot, name, description, icon, position, autoExpansionPolicy);
    }

    /**
     * Returns Snapshot name if available in Snapshot Storage as PROPERTY_NAME
     * or generates new name using Snapshot's Category.
     *
     * @param snapshot Snapshot for which to resolve the name
     * @return persisted Snapshot name if available or new generated name
     *
     * @since VisualVM 1.3
     */
    protected static String resolveSnapshotName(Snapshot snapshot) {
        String persistedName = resolveName(snapshot, null);
        if (persistedName != null) return persistedName;
        
        File file = snapshot.getFile();
        String fileName = file != null ? file.getName() :
               NbBundle.getMessage(SnapshotDescriptor.class, "LBL_NoFile");    // NOI18N
        SnapshotCategory category = snapshot.getCategory();
        String name = "[" + category.getPrefix() + "] " + fileName; // NOI18N
        
        if (file != null && category.isSnapshot(file)) {
            String timeStamp = category.getTimeStamp(fileName);
            if (timeStamp != null) name = "[" + category.getPrefix() + "] " + timeStamp;    // NOI18N
        }
        
        return name;
    }
    
    /**
     * Returns true if the snapshot can be renamed from UI, false otherwise.
     * 
     * @return true if the snapshot can be renamed from UI, false otherwise
     */
    public boolean supportsRename() {
        return true;
    }

    public boolean providesProperties() {
        return true;
    }

}
