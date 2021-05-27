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

package org.graalvm.visualvm.jfr.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class JFRSnapshotCategory extends SnapshotCategory<JFRSnapshot> {
    
    private static final Logger LOGGER = Logger.getLogger(JFRSnapshotCategory.class.getName());
    
    private static final String NAME = NbBundle.getMessage(JFRSnapshotCategory.class, "LBL_Core_Dumps");   // NOI18N
    private static final String PREFIX = "jfr";   // NOI18N
    private static final String SUFFIX = ".jfr";  // NOI18N
    
    
    public JFRSnapshotCategory() {
        super(NAME, JFRSnapshot.class, PREFIX, SUFFIX, 40);
    }
    
    
    @Override
    public boolean supportsOpenSnapshot() {
        return true;
    }
    
    @Override
    public void openSnapshot(File file) {
        try {
            JFRSnapshot snapshot = new JFRSnapshotImpl(file);
            DataSourceWindowManager.sharedInstance().openDataSource(snapshot); // TODO: instance should be created by JFRSnapshotProvider!
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error opening JFR snapshot", ex); // NOI18N
        }
    }

}
