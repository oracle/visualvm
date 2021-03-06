/*
 *  Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import java.util.Comparator;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * DataSourceDescriptor for Snapshots node in Applications window.
 *
 * @author Jiri Sedlacek
 */
public final class SnapshotsContainerDescriptor extends DataSourceDescriptor<SnapshotsContainer> {

    private static final Image NODE_ICON = ImageUtilities.loadImage(
                "org/graalvm/visualvm/core/ui/resources/snapshots.png", true);  // NOI18N

    SnapshotsContainerDescriptor() {
        super(SnapshotsContainer.sharedInstance(), NbBundle.getMessage(
              SnapshotsContainer.class, "LBL_Snapshots"), null, NODE_ICON, 30, // NOI18N
              EXPAND_ON_EACH_FIRST_CHILD);
        
        // Initialize sorting
        setChildrenComparator(SnapshotsSorting.instance().getInitialSorting());
    }

    /**
     * Sets a custom comparator for sorting DataSources within the SnapshotsContainer.
     * Use setChildrenComparator(null) to restore the default sorting.
     *
     * @param newComparator comparator for sorting DataSources within the SnapshotsContainer
     */
    public void setChildrenComparator(Comparator<DataSource> newComparator) {
        super.setChildrenComparator(newComparator);
    }

}
