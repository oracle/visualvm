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

package com.sun.tools.visualvm.core.application;

import com.sun.tools.visualvm.core.datasupport.Storage;
import java.io.File;

/**
 * For now a package-private class encapsulating entrypoints to JmxApplication persistence.
 * After code cleanup it should probably implement JmxApplication-related stuff currently
 * available in ApplicationsSupport (ApplicationsSupport should be divided into
 * JvmstatApplicationSupport and JmxApplicationSupport).
 *
 * @author Jiri Sedlacek
 */
class JmxApplicationsSupport {
    
    private static final String STORAGE_DIRNAME = "jmxapplications";
    
    private static File storageDirectory;
    private static String storageDirectoryString;
    
    
    static String getStorageDirectoryString() {
        if (storageDirectoryString == null)
            storageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + STORAGE_DIRNAME;
        return storageDirectoryString;
    }
    
    static File getStorageDirectory() {
        if (storageDirectory == null) {
            String storageString = getStorageDirectoryString();
            storageDirectory = new File(storageString);
            if (storageDirectory.exists() && storageDirectory.isFile())
                throw new IllegalStateException("Cannot create hosts storage directory " + storageString + ", file in the way");
            if (storageDirectory.exists() && (!storageDirectory.canRead() || !storageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access hosts storage directory " + storageString + ", read&write permission required");
            if (!storageDirectory.exists() && !storageDirectory.mkdirs())
                throw new IllegalStateException("Cannot create hosts storage directory " + storageString);
        }
        return storageDirectory;
    }
    
    static boolean storageDirectoryExists() {
        return new File(getStorageDirectoryString()).isDirectory();
    }

}
