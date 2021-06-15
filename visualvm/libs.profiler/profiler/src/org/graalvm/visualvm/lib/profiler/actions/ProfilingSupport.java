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
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.utilities.ProfilerUtils;


/**
 * A supporting class for the IDE profiling actions.
 * It centralizes all the code that has to do with figuring out context
 * from the IDE and interface it to the actual profiling.
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "CAPTION_Question=Question",
    "ProfilingSupport_StopStartProfileSessionMessage=Profiling session is currently in progress.\nDo you want to stop the current session and start a new one?",
    "ProfilingSupport_StopStartAttachSessionMessage=Profiling session is currently in progress\nDo you want to detach from the target application and start a new profiling session?",
    "ProfilingSupport_FailedLoadSettingsMsg=Failed to load attach settings: {0}"
})
public final class ProfilingSupport {

    public static boolean checkProfilingInProgress() {
        final Profiler profiler = Profiler.getDefault();
        final int state = profiler.getProfilingState();
        final int mode = profiler.getProfilingMode();

        if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
            if (mode == Profiler.MODE_PROFILE) {
                if (!ProfilerDialogs.displayConfirmation(
                    Bundle.ProfilingSupport_StopStartProfileSessionMessage(),
                    Bundle.CAPTION_Question())) {
                    return true;
                }
                // TODO remove the condition when the method is only called in awt or only in RP
                if (SwingUtilities.isEventDispatchThread()) {
                    StopAction.getInstance().setEnabled(false);
                    ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                        @Override
                        public void run() {
                            profiler.stopApp();
                        }
                    });
                } else {
                    profiler.stopApp();
                }
                
            } else {
                if (!ProfilerDialogs.displayConfirmation(
                    Bundle.ProfilingSupport_StopStartAttachSessionMessage(), 
                    Bundle.CAPTION_Question())) {
                    return true;
                }

                profiler.detachFromApp();
            }
        }

        return false;
    }
    
}
