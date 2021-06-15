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

package org.graalvm.visualvm.lib.profiler.actions;

import org.graalvm.visualvm.lib.common.Profiler;
import org.openide.util.NbBundle;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.utilities.ProfilerUtils;
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

//    @ActionID(category="Profile", id="org.graalvm.visualvm.lib.profiler.actions.StopAction")
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
