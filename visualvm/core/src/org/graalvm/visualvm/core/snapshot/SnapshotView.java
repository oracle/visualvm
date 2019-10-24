/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.core.snapshot;

import java.awt.Image;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.ui.DataSourceView;

/**
 * Abstract superclass of DataSourceView for a Snapshot.
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotView extends DataSourceView {
    
    public SnapshotView(Snapshot snapshot, String name, Image icon, int preferredPosition) {
        super(snapshot, name, icon, preferredPosition, isClosableView(snapshot));
    }
    
    public SnapshotView(Snapshot snapshot, String name, Image icon, int preferredPosition, boolean isClosable) {
        super(snapshot, name, icon, preferredPosition, isClosable);
    }
    
    
    private static boolean isClosableView(Snapshot snapshot) {
        String closable = snapshot.getStorage().getCustomProperty(Snapshot.PROPERTY_VIEW_CLOSABLE);
        if (Boolean.TRUE.toString().equals(closable)) return true;
        if (Boolean.FALSE.toString().equals(closable)) return false;
        
        // ProfilerSnapshot invisible
        if (!snapshot.isVisible()) return false;
        
        // ProfilerSnapshot not in DataSources tree
        DataSource owner = snapshot.getOwner();
        if (owner == null) return false;
        
        while (owner != null && owner != DataSource.ROOT) {
            // Subtree containing ProfilerSnapshot invisible
            if (!owner.isVisible()) return false;
            owner = owner.getOwner();
        }
        
        // ProfilerSnapshot visible in DataSources tree
        if (owner == DataSource.ROOT) return true;
        
        // ProfilerSnapshot not in DataSources tree
        return false;
    }
    
}
