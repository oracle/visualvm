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
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import java.awt.Image;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Toplevel node Snapshots in Applications window.
 *
 * @author Jiri Sedlacek
 */
public final class SnapshotsContainer extends DataSource {
    
    private static SnapshotsContainer sharedInstance;
    
    
    /**
     * Returns singleton instance of SnapshotsContainer.
     * 
     * @return singleton instance of SnapshotsContainer.
     */
    public static synchronized SnapshotsContainer sharedInstance() {
        if (sharedInstance == null) sharedInstance = new SnapshotsContainer();
        return sharedInstance;
    }
    
    
    private SnapshotsContainer() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new SnapshotsContainerDescriptorProvider());
        DataSource.ROOT.getRepository().addDataSource(this);
    }
    
    
    private static class SnapshotsContainerDescriptor extends DataSourceDescriptor {
            private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/snapshots.png", true); // NOI18N

            SnapshotsContainerDescriptor() {
                super(SnapshotsContainer.sharedInstance(), NbBundle.getMessage(SnapshotsContainer.class, "LBL_Snapshots"), null, NODE_ICON, 30, EXPAND_ON_FIRST_CHILD); // NOI18N
            }

        }
    
    private class SnapshotsContainerDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor, DataSource> {
    
        SnapshotsContainerDescriptorProvider() {
        }

        public DataSourceDescriptor createModelFor(DataSource ds) {
            if (SnapshotsContainer.sharedInstance().equals(ds)) {
                return new SnapshotsContainerDescriptor();
            }
            return null;
        }
    }

}
