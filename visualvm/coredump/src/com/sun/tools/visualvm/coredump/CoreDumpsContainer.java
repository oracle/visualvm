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

package com.sun.tools.visualvm.coredump;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import java.awt.Image;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Toplevel node VM Coredumps in Applications window.
 *
 * @author Jiri Sedlacek
 */
public final class CoreDumpsContainer extends DataSource {
    
    private static CoreDumpsContainer sharedInstance;
    
    
    /**
     * Returns singleton instance of CoreDumpsContainer.
     * 
     * @return singleton instance of CoreDumpsContainer.
     */
    public static synchronized CoreDumpsContainer sharedInstance() {
        if (sharedInstance == null) sharedInstance = new CoreDumpsContainer();
        return sharedInstance;
    }
    
    
    private CoreDumpsContainer() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new CoreDumpContainerDescriptorProvider());
        DataSource.ROOT.getRepository().addDataSource(this);
    }
    
    
    private static class CoreDumpsContainerDescriptor extends DataSourceDescriptor {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/coredump/resources/coredumps.png", true);    // NOI18N

        CoreDumpsContainerDescriptor() {
            super(CoreDumpsContainer.sharedInstance(), NbBundle.getMessage(CoreDumpsContainer.class, "LBL_VM_Coredumps"), null, NODE_ICON, 20, EXPAND_ON_EACH_NEW_CHILD);   // NOI18N
        }

    }
    
    private static class CoreDumpContainerDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor,DataSource> {
    
        public DataSourceDescriptor createModelFor(DataSource ds) {
            if (CoreDumpsContainer.sharedInstance().equals(ds)) {
                return new CoreDumpsContainerDescriptor();
            }
            return null;
        }
    }

}
