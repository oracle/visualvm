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
import org.openide.util.NbBundle;
import javax.swing.*;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;


/**
 * Stop/Finish the currently profiled target application
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "StopAction_DoYouWantToTerminateCap=Detach Profiler",
    "StopAction_DoYouWantToTerminateMsg=Do you want to terminate the profiled application upon detach?",
    "LBL_StopAction=&Stop Profiling Session",
    "HINT_StopAction=Stop (Terminate) the Profiled Application",
    "LBL_DetachAction=Detach...",
    "HINT_DetachAction=Detach from the Profiled Application"
})
public final class StopAction extends ProfilingAwareAction {
    final private static int[] enabledStates = new int[]{Profiler.PROFILING_PAUSED, Profiler.PROFILING_RUNNING, Profiler.PROFILING_STARTED};
    
    final private static class Singleton {
        final private static StopAction INSTANCE = new StopAction();
    }
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private boolean taskPosted = false;
    private int mode = -1; // not determined yet

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    private StopAction() {
        updateAction();
    }

//    @ActionID(category="Profile", id="org.netbeans.modules.profiler.actions.StopAction")
//    @ActionRegistration(displayName="#LBL_StopAction")
//    @ActionReferences({
//        @ActionReference(path="Menu/Profile", position=300, separatorAfter=400),
//        @ActionReference(path="Shortcuts", name="S-F2")
//    })
    public static StopAction getInstance() {
        return Singleton.INSTANCE;
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when an action occurs.
     */
    public void performAction() {
        if (taskPosted) { // TODO this doesn't prevent from multiple stop tasks being posted!!!

            return; // already performing
        }

        Runnable task = null;

        if (mode == Profiler.MODE_ATTACH) {
            Boolean ret = ProfilerDialogs.displayCancellableConfirmationDNSA(
                Bundle.StopAction_DoYouWantToTerminateMsg(), Bundle.StopAction_DoYouWantToTerminateCap(),
                null, StopAction.class.getName(), false);

            if (Boolean.TRUE.equals(ret)) {
                task = new Runnable() {
                        public void run() {
                            Profiler.getDefault().stopApp();
                            taskPosted = false;
                        }
                    };
            } else if (Boolean.FALSE.equals(ret)) {
                task = new Runnable() {
                        public void run() {
                            Profiler.getDefault().detachFromApp();
                            taskPosted = false;
                        }
                    };
            }
        } else {
            task = new Runnable() {
                    public void run() {
                        Profiler.getDefault().stopApp();
                        taskPosted = false;
                    }
                };
        }

        if (task != null) {
            taskPosted = true;
            updateAction();
            ProfilerUtils.runInProfilerRequestProcessor(task);
        }
    }

    @Override
    protected int[] enabledStates() {
        return enabledStates;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(StopAction.class);
    }

    @Override
    public String getName() {
        return (mode == Profiler.MODE_PROFILE) ? Bundle.LBL_StopAction() : Bundle.LBL_DetachAction();
    }

    @Override
    protected void updateAction() {
        super.updateAction();
        mode = Profiler.getDefault().getProfilingMode();

        if (mode == Profiler.MODE_PROFILE) {
            setToStop(); 
        } else if (mode == Profiler.MODE_ATTACH) {
            setToDetach(); 
        }
        
        firePropertyChange(SMALL_ICON, null, null);
    }

    @Override
    protected boolean shouldBeEnabled(Profiler profiler) {
        return super.shouldBeEnabled(profiler) && (profiler.getProfilingState() == Profiler.PROFILING_INACTIVE
                || profiler.getServerState() == CommonConstants.SERVER_RUNNING);
    }
    
    private void setToDetach() {
        putValue(Action.NAME, Bundle.LBL_DetachAction());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_DetachAction());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.DETACH));
        putValue("iconBase", Icons.getResource(GeneralIcons.DETACH)); // NOI18N
        setIcon(Icons.getIcon(GeneralIcons.DETACH));
    }

    private void setToStop() {
        putValue(Action.NAME, Bundle.LBL_StopAction());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_StopAction());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.STOP));
        putValue("iconBase", Icons.getResource(GeneralIcons.STOP)); // NOI18N
        setIcon(Icons.getIcon(GeneralIcons.STOP));
    }
}
