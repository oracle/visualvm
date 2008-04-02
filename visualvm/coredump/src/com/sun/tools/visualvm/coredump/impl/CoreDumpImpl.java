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

package com.sun.tools.visualvm.coredump.impl;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.coredump.CoreDump;
import com.sun.tools.visualvm.coredump.CoreDumpSupport;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
class CoreDumpImpl extends CoreDump {
    
    private Storage storage;
    
    
    public CoreDumpImpl(File file, File javaHomeName, Storage storage) throws IOException {
        super(file, javaHomeName);
        this.storage = storage;
    }
    
    public boolean supportsDelete() {
        return false;
    }
    
    
    protected Storage createStorage() {
        return storage;
    }

    public boolean supportsUserRemove() {
        return true;
    }
    
    
    protected void remove() {
        File file = getFile();
        if (CoreDumpSupport.getStorageDirectory().equals(file.getParentFile())) Utils.delete(file, true);
        setFile(null);
        getStorage().deleteCustomPropertiesStorage();
    }
    
}
