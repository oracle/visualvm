/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jfr;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.snapshot.SnapshotsSupport;
import java.io.File;
import java.io.IOException;
import org.openide.util.NbBundle;

/**
 * Abstract implementation of JFRSnapshot.
 *
 * @author Jiri Sedlacek
 */
public abstract class JFRSnapshot extends Snapshot {
    
    public JFRSnapshot(File file) throws IOException {
        this(file, null);
    }
    
    public JFRSnapshot(File file, DataSource master) throws IOException {
        super(file, JFRSnapshotSupport.getCategory(), master);
        
        if (!file.exists() || !file.isFile())
            throw new IOException("File " + file.getAbsolutePath() + " does not exist");    // NOI18N
    }
    
    public boolean supportsSaveAs() {
        return getFile() != null;
    }
    
    public void saveAs() {
        SnapshotsSupport.getInstance().saveAs(this, NbBundle.getMessage(JFRSnapshot.class, "LBL_Save_Core_Dump_As"));  // NOI18N
    }

}
