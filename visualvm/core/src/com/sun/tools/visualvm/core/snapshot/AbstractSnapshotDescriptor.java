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

import com.sun.tools.visualvm.core.datasource.Snapshot;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import java.awt.Image;
import java.io.File;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractSnapshotDescriptor<X extends Snapshot> extends DataSourceDescriptor {
    
    
    public AbstractSnapshotDescriptor(X snapshot, Image icon) {
        super(snapshot, getName(snapshot),
              snapshot.getFile().getAbsolutePath(),
              icon, POSITION_AT_THE_END, EXPAND_NEVER);
    }
    
    private static String getName(Snapshot snapshot) {
        File file = snapshot.getFile();
        if (file == null) return snapshot.toString();
        
        String fileName = file.getName();
        SnapshotCategory category = snapshot.getCategory();
        String name = "[" + category.getPrefix() + "] " + fileName;
        
        if (category.isSnapshot(file)) {
            String timeStamp = category.getTimeStamp(fileName);
            if (timeStamp != null) name = "[" + category.getPrefix() + "] " + timeStamp;
        }
        
        return name;
    }

}
