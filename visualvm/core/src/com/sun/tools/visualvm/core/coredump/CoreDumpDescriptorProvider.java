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

package com.sun.tools.visualvm.core.coredump;

import com.sun.tools.visualvm.core.datasource.CoreDump;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.snapshot.AbstractSnapshotDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
class CoreDumpDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor,DataSource> {
    
    CoreDumpDescriptorProvider() {
    }
    
    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (CoreDumpsContainer.sharedInstance().equals(ds)) {
            return new CoreDumpsContainerDescriptor();
        }
        if (ds instanceof CoreDump) {
            return CoreDumpDescriptor.newInstance((CoreDump) ds);
        }
        return null;
    }
    
    private static class CoreDumpDescriptor extends AbstractSnapshotDescriptor<CoreDump> {
        
        private static final Image ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/coredump.png", true);
        
        static CoreDumpDescriptor newInstance(CoreDump coreDump) {
            CoreDumpDescriptor desc = new CoreDumpDescriptor(coreDump);
            desc.setName(coreDump.getDisplayName());
            return desc; 
        }
        
        CoreDumpDescriptor(CoreDump coreDump) {
            super(coreDump, ICON);
        }
        
        public boolean supportsRename() {
            return true;
        }
        
    }

    private static class CoreDumpsContainerDescriptor extends DataSourceDescriptor {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/coredumps.png", true);
        
        CoreDumpsContainerDescriptor() {
            super(CoreDumpsContainer.sharedInstance(), "VM Coredumps", null, NODE_ICON, 20, EXPAND_ON_EACH_NEW_CHILD);
        }
        
    }
}
