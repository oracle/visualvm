/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.actions;

import org.netbeans.lib.profiler.common.Profiler;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import javax.swing.Action;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;


/**
 * Rerun the profiling using the same settings as last executed one
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LBL_RerunAction=&Rerun Profiling Session",
    "HINT_RerunAction=Rerun Last Profiling Session",
    "MSG_ReRunOnProfile=Profiling session is currently in progress.\nDo you want to stop the current session and start it again?",
    "MSG_ReRunOnAttach=Profiling session is currently in progress\nDo you want to detach from the target application and rerun the last profiling session?"
})
@ActionID(id = "org.netbeans.modules.profiler.actions.RerunAction", category = "Profile")
@ActionRegistration(displayName = "#LBL_RerunAction", lazy=false, asynchronous=false)
@ActionReferences(value = {
    @ActionReference(path = "Shortcuts", name = "CS-F2"),
    @ActionReference(path = "Menu/Profile", position = 500, separatorBefore=490)})
public final class RerunAction extends ProfilingAwareAction {
    final private static int[] ENABLED_STATES = new int[]{Profiler.PROFILING_STOPPED, Profiler.PROFILING_PAUSED, Profiler.PROFILING_INACTIVE};    

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RerunAction() {
        setIcon(Icons.getIcon(GeneralIcons.RERUN));
        putValue("iconBase", Icons.getResource(GeneralIcons.RERUN)); // NOI18N
        putProperty(Action.SHORT_DESCRIPTION, Bundle.HINT_RerunAction());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isEnabled() {
        return super.isEnabled() && Profiler.getDefault().rerunAvailable();
    }
    
    @Override
    protected int[] enabledStates() {
        return ENABLED_STATES;
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;

        // If you will provide context help then use:
        // return new HelpCtx(MyAction.class);
    }

    public String getName() {
        return Bundle.LBL_RerunAction();
    }

    @Override
    public void performAction() {
        final int state = Profiler.getDefault().getProfilingState();
        final int mode = Profiler.getDefault().getProfilingMode();

        if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
            if (mode == Profiler.MODE_PROFILE) {
                if (!ProfilerDialogs.displayConfirmation(
                    Bundle.MSG_ReRunOnProfile(), 
                    Bundle.CAPTION_Question())) {
                    return;
                }

                Profiler.getDefault().stopApp();
            } else {
                if (!ProfilerDialogs.displayConfirmation(
                    Bundle.MSG_ReRunOnAttach(),
                    Bundle.CAPTION_Question())) {
                    return;
                }

                Profiler.getDefault().detachFromApp();
            }
        }

        Profiler.getDefault().rerunLastProfiling();
    }
}
