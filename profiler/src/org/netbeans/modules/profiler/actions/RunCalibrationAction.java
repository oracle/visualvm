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
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import javax.swing.*;
import org.netbeans.modules.profiler.api.JavaPlatform;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;


/**
 * Provisionary action to explicitely run Profiler calibration.
 *
 * @author Ian Formanek
 */
public final class RunCalibrationAction extends AbstractAction {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RunCalibrationAction() {
        putValue(Action.NAME, NbBundle.getMessage(RunCalibrationAction.class, "LBL_RunCalibrationAction" // NOI18N
        ));
        putValue(Action.SHORT_DESCRIPTION, NbBundle.getMessage(RunCalibrationAction.class, "HINT_RunCalibrationAction" // NOI18N
        ));
        putValue("noIconInMenu", Boolean.TRUE); //NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isEnabled() {
        if (!NetBeansProfiler.isInitialized()) {
            return false;
        }

        return true;
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        final int state = Profiler.getDefault().getProfilingState();
        final int mode = Profiler.getDefault().getProfilingMode();
        boolean terminate = false;
        boolean detach = false;

        if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
            if (mode == Profiler.MODE_PROFILE) {
                if (!ProfilerDialogs.displayConfirmation(NbBundle.getMessage(RunCalibrationAction.class,
                        "MSG_CalibrationOnProfile"), NbBundle.getMessage(RunCalibrationAction.class, "CAPTION_Question"))) { //NOI18N
                    return;
                }

                terminate = true;
            } else {
                if (!ProfilerDialogs.displayConfirmation(NbBundle.getMessage(RunCalibrationAction.class,
                        "MSG_CalibrationOnAttach"), NbBundle.getMessage(RunCalibrationAction.class, "CAPTION_Question"))) { //NOI18N
                    return;
                }

                detach = true;
            }
        }

        final boolean doDetach = detach;
        final boolean doStop = terminate;

        JavaPlatform platform = JavaPlatformSelector.getDefault().selectPlatformForCalibration();

        if (platform == null) {
            return;
        }

        final JavaPlatform calibratePlatform = platform;
        ProfilerUtils.getProfilerRequestProcessor().post(new Runnable() {
                public void run() {
                    if (doDetach) {
                        Profiler.getDefault().detachFromApp();
                    } else if (doStop) {
                        Profiler.getDefault().stopApp();
                    }

                    if (!Profiler.getDefault()
                                     .runCalibration(false, calibratePlatform.getPlatformJavaFile(),
                                                         calibratePlatform.getPlatformJDKVersion(),
                                                         calibratePlatform.getPlatformArchitecture())) {
                        ProfilerDialogs.displayError(NbBundle.getMessage(RunCalibrationAction.class, "MSG_CalibrationFailed")); //NOI18N
                    }
                }
            }, 0, Thread.MAX_PRIORITY);
    }
}
