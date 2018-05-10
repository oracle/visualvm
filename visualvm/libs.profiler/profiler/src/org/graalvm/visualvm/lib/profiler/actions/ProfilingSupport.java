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

import org.openide.util.NbBundle;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;


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
