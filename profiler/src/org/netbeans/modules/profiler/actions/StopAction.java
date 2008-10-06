/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.spi.LoadGenPlugin;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import java.awt.event.ActionEvent;
import java.util.Properties;
import javax.swing.*;


/**
 * Stop/Finish the currently profiled target application
 *
 * @author Ian Formanek
 */
public final class StopAction extends AbstractAction implements ProfilingStateListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String MSG_DO_YOU_WANT_TO_TERMINATE_MSG = NbBundle.getMessage(StopAction.class,
                                                                                       "StopAction_DoYouWantToTerminateMsg"); // NOI18N
                                                                                                                              // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private boolean taskPosted = false;
    private int mode = -1; // not determined yet

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public StopAction() {
        updateDisplayName();
        updateEnabledState();
        Profiler.getDefault().addProfilingStateListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent evt) {
        if (taskPosted) { // TODO this doesn't prevent from multiple stop tasks being posted!!!

            return; // already performing
        }

        Runnable task = null;

        if (mode == Profiler.MODE_ATTACH) {
            ProfilerDialogs.DNSAConfirmation dd = new ProfilerDialogs.DNSAConfirmation(StopAction.class.getName(),
                                                                                       MSG_DO_YOU_WANT_TO_TERMINATE_MSG,
                                                                                       NotifyDescriptor.YES_NO_CANCEL_OPTION);
            dd.setDNSADefault(false);

            final Object res = ProfilerDialogs.notify(dd);

            if (res.equals(NotifyDescriptor.YES_OPTION)) {
                task = new Runnable() {
                        public void run() {
                            Profiler.getDefault().stopApp();
                            taskPosted = false;
                        }
                    };
            } else if (res.equals(NotifyDescriptor.NO_OPTION)) {
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
            updateEnabledState();
            IDEUtils.runInProfilerRequestProcessor(task);
        }
    }

    public void instrumentationChanged(final int oldInstrType, final int currentInstrType) {
    } // ignore

    public void profilingStateChanged(final ProfilingStateEvent e) {
        if (mode != Profiler.getDefault().getProfilingMode()) {
            updateDisplayName();
        }

        updateEnabledState();
    }

    public void threadsMonitoringChanged() {
    } // ignore

    private void updateDisplayName() {
        mode = Profiler.getDefault().getProfilingMode();

        if (mode == Profiler.MODE_PROFILE) {
            putValue(Action.NAME, NbBundle.getMessage(StopAction.class, "LBL_StopAction" // NOI18N
            ));
            putValue(Action.SHORT_DESCRIPTION, NbBundle.getMessage(StopAction.class, "HINT_StopAction" // NOI18N
            ));
            putValue(Action.SMALL_ICON,
                     new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/actions/resources/stop.png")) //NOI18N
            );
            putValue("iconBase", // NOI18N
                     "org/netbeans/modules/profiler/actions/resources/stop.png" // NOI18N
            );
        } else {
            putValue(Action.NAME, NbBundle.getMessage(StopAction.class, "LBL_DetachAction" // NOI18N
            ));
            putValue(Action.SHORT_DESCRIPTION, NbBundle.getMessage(StopAction.class, "HINT_DetachAction" // NOI18N
            ));
            putValue(Action.SMALL_ICON,
                     new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/actions/resources/detach.png")) //NOI18N
            );
            putValue("iconBase", // NOI18N
                     "org/netbeans/modules/profiler/actions/resources/detach.png" // NOI18N
            );
        }
    }

    private void updateEnabledState() {
        final boolean shouldBeEnabled = !taskPosted
                                        && ((Profiler.getDefault().getProfilingState() == Profiler.PROFILING_PAUSED)
                                           || (Profiler.getDefault().getProfilingState() == Profiler.PROFILING_RUNNING)
                                           || (Profiler.getDefault().getProfilingState() == Profiler.PROFILING_STARTED));

        setEnabled(shouldBeEnabled);
    }
}
