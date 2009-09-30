/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler;

import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.NotifyDescriptor;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbBundle;
import javax.swing.*;
import org.netbeans.modules.profiler.ppoints.ui.ProfilingPointsWindow;


/**
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class ProfilerModule extends ModuleInstall {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String QUESTION_DIALOG_CAPTION = NbBundle.getMessage(ProfilerModule.class,
                                                                              "ProfilerModule_QuestionDialogCaption"); // NOI18N
    private static final String EXITING_FROM_PROFILE_MESSAGE = NbBundle.getMessage(ProfilerModule.class,
                                                                                   "ProfilerModule_ExitingFromProfileMessage"); // NOI18N
    private static final String EXITING_FROM_ATTACH_MESSAGE = NbBundle.getMessage(ProfilerModule.class,
                                                                                  "ProfilerModule_ExitingFromAttachMessage"); // NOI18N
                                                                                                                              // -----
    public static final String LIBS_DIR = "lib"; //NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Called when the IDE is about to exit. The default implementation returns <code>true</code>.
     * The module may cancel the exit if it is not prepared to be shut down.
     *
     * @return <code>true</code> if it is ok to exit the IDE
     */
    public boolean closing() {
        final int state = Profiler.getDefault().getProfilingState();
        final int mode = Profiler.getDefault().getProfilingMode();

        if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
            if (mode == Profiler.MODE_PROFILE) {
                final NotifyDescriptor d = new NotifyDescriptor.Confirmation(EXITING_FROM_PROFILE_MESSAGE,
                                                                             QUESTION_DIALOG_CAPTION,
                                                                             NotifyDescriptor.YES_NO_OPTION);

                if (ProfilerDialogs.notify(d) != NotifyDescriptor.YES_OPTION) {
                    return false;
                }

                Profiler.getDefault().stopApp();
            } else {
                final NotifyDescriptor d = new NotifyDescriptor.Confirmation(EXITING_FROM_ATTACH_MESSAGE,
                                                                             QUESTION_DIALOG_CAPTION,
                                                                             NotifyDescriptor.YES_NO_OPTION);

                if (ProfilerDialogs.notify(d) != NotifyDescriptor.YES_OPTION) {
                    return false;
                }

                Profiler.getDefault().detachFromApp();
            }
        }

        if (!ResultsManager.getDefault().ideClosing()) {
            return false;
        }

        ProfilingPointsManager.getDefault().ideClosing(); // TODO: dirty profiling points should be persisted on document save!

        // cleanup before exiting the IDE, always returns true
        if (LiveResultsWindow.hasDefault()) {
            LiveResultsWindow.getDefault().ideClosing();
        }

        return true;
    }

    /**
     * Called when an already-installed module is restored (during IDE startup).
     * Should perform whatever initializations are required.
     * <p>Note that it is possible for module code to be run before this method
     * is called, and that code must be ready nonetheless. For example, data loaders
     * might be asked to recognize a file before the module is "restored". For this
     * reason, but more importantly for general performance reasons, modules should
     * avoid doing anything here that is not strictly necessary - often by moving
     * initialization code into the place where the initialization is actually first
     * required (if ever). This method should serve as a place for tasks that must
     * be run once during every startup, and that cannot reasonably be put elsewhere.
     * <p>Basic programmatic services are available to the module at this stage -
     * for example, its class loader is ready for general use, any objects registered
     * declaratively to lookup (e.g. system options or services) are ready to be
     * queried, and so on.
     */
    public void restored() {
        super.restored();
        MiscUtils.setVerbosePrint(); // for EA, we want as many details in the log file as possible to be able to resolve user problems
                                     // Settings have to be load on startup at least for the following calibration (saved calibration data loading) stuff
                                     // to run correctly - it needs to know the saved JVM executable file/version to run.

        MiscUtils.deleteHeapTempFiles();
        Profiler.getDefault();
    }

    /**
     * Called when the module is uninstalled (from a running IDE).
     * Should remove whatever functionality from the IDE that it had registered.
     */
    public void uninstalled() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        // stop or detach from any profiling in progress
                        final int state = Profiler.getDefault().getProfilingState();
                        final int mode = Profiler.getDefault().getProfilingMode();

                        if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
                            if (mode == Profiler.MODE_PROFILE) {
                                Profiler.getDefault().stopApp();
                            } else {
                                Profiler.getDefault().detachFromApp();
                            }
                        }

                        // force closing of all windows
                        ProfilerControlPanel2.closeIfOpened();
                        TelemetryOverviewPanel.closeIfOpened();
                        DrillDownWindow.closeIfOpened();
                        LiveResultsWindow.closeIfOpened();
                        TelemetryWindow.closeIfOpened();
                        ThreadsWindow.closeIfOpened();
                        SnapshotResultsWindow.closeAllWindows();
                        HeapWalkerManager.getDefault().closeAllHeapWalkers();
                        ProfilingPointsWindow.closeIfOpened();

                        // perform any shutdown
                        ((NetBeansProfiler) Profiler.getDefault()).shutdown();

                        new ResetResultsAction().actionPerformed(null); // cleanup client data
                    }
                });
        } catch (Exception e) {
            ProfilerLogger.log(e);
        }

        // proceed with uninstall
        super.uninstalled();
    }
}
