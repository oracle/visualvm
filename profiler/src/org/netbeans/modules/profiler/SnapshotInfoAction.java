/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler;

import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
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

    public SnapshotInfoAction(LoadedSnapshot snapshot) {
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
