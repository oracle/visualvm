/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler;

import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;

@NbBundle.Messages({
    "SnapshotInfoAction_ActionName=Snapshot information",
    "SnapshotInfoAction_ActionDescr=Display snapshot information",
    "SnapshotInfoAction_WindowCaption=Snapshot Information"
})
class SnapshotInfoAction extends AbstractAction {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final LoadedSnapshot snapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    SnapshotInfoAction(LoadedSnapshot snapshot) {
        putValue(Action.NAME, Bundle.SnapshotInfoAction_ActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.SnapshotInfoAction_ActionDescr());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.INFO));
        putValue("iconBase", Icons.getResource(GeneralIcons.INFO)); // NOI18N
        this.snapshot = snapshot;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        HelpCtx helpCtx = new HelpCtx("SnapshotInfo.HelpCtx"); // NOI18N
        DialogDescriptor dd = new DialogDescriptor(new SnapshotInfoPanel(snapshot),
                              Bundle.SnapshotInfoAction_WindowCaption(), true,
                              new Object[] { DialogDescriptor.OK_OPTION }, 
                              DialogDescriptor.OK_OPTION, DialogDescriptor.DEFAULT_ALIGN,
                              helpCtx, null);
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
    }
}
