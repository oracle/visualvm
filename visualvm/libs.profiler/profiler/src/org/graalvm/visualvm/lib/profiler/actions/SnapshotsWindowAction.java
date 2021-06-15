/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.actions;

import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import javax.swing.*;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.v2.SnapshotsWindow;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;


/**
 * Action to display the Snapshots window.
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LBL_SnapshotsWindowAction=&Snapshots",
    "HINT_SnapshotsWindowAction=Show Profiler Snapshots Window"
})
@ActionID(category="Profile", id="org.netbeans.modules.profiles.actions.SnapshotsWindowAction")
@ActionRegistration(displayName="#LBL_SnapshotsWindowAction", iconBase="org/graalvm/visualvm/lib/profiler/impl/icons/takeSnapshot.png")
@ActionReference(path="Menu/Window/Profile", position=99)
public final class SnapshotsWindowAction extends AbstractAction {

    public SnapshotsWindowAction() {
        putValue(Action.NAME, Bundle.LBL_SnapshotsWindowAction());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_SnapshotsWindowAction());
        putValue(Action.SMALL_ICON, Icons.getIcon(ProfilerIcons.SNAPSHOT_TAKE));
        putValue("iconBase", Icons.getResource(ProfilerIcons.SNAPSHOT_TAKE));
    }


    public void actionPerformed(final ActionEvent e) {
        SnapshotsWindow.instance().showStandalone();
    }
}
