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

import org.graalvm.visualvm.core.snapshot.SnapshotDescriptor;
import java.awt.Image;
import org.graalvm.visualvm.core.snapshot.SnapshotsSupport;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * DataSourceDescriptor for JFRSnapshot.
 *
 * @author Jiri Sedlacek
 */
public class JFRSnapshotDescriptor extends SnapshotDescriptor<JFRSnapshot> {

    private static final Image ICON = SnapshotsSupport.getInstance().createSnapshotIcon(
            ImageUtilities.loadImage("org/graalvm/visualvm/jfr/resources/jfrSnapshot.png", true)); // NOI18N

    /**
     * Creates new instance of JFRSnapshotDescriptor.
     * 
     * @param snapshot JFRSnapshot for the descriptor.
     */
    public JFRSnapshotDescriptor(JFRSnapshot snapshot) {
        super(snapshot, resolveSnapshotName(snapshot), NbBundle.getMessage(JFRSnapshotDescriptor.class, "DESCR_CoreDump"), ICON, // NOI18N
              resolvePosition(snapshot, POSITION_AT_THE_END, true), EXPAND_NEVER);
    }
    
    
    private static String resolveName(JFRSnapshot snapshot) {
        String persistedName = resolveName(snapshot, null);
        return persistedName != null ? persistedName : snapshot.getFile().getName();
    }
    
}
