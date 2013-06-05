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

import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.common.*;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ui.panels.PIDSelectPanel;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.io.IOException;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.project.ProjectStorage;
import org.netbeans.modules.profiler.api.TaskConfigurator;
import org.netbeans.modules.profiler.api.project.ProjectProfilingSupport;
import org.netbeans.modules.profiler.attach.AttachWizard;
import org.openide.util.Lookup;


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
    public static final String RERUN_PROP = "profiler.rerun";
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class AttachSTPData {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private ProfilingSettings ps;
        private Lookup.Provider p;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private AttachSTPData(ProfilingSettings ps, Lookup.Provider p) {
            this.ps = ps;
            this.p = p;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Lookup.Provider getProject() {
            return p;
        }

        public ProfilingSettings getSettings() {
            return ps;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    private static ProfilingSupport defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private volatile boolean profilingActionInvoked = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Private constructor to avoid multiple instances creation
     */
    private ProfilingSupport() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized static ProfilingSupport getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new ProfilingSupport();
        }

        return defaultInstance;
    }

    public boolean checkProfilingInProgress() {
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

                profiler.stopApp();
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
    
    public int selectApplicationPid() {
        return PIDSelectPanel.selectPID();
    }

    public AttachSTPData selectTaskForAttach(final Lookup.Provider project, final SessionSettings sessionSettings) {
        TaskConfigurator.Configuration configuration = TaskConfigurator.getDefault().configureAttachProfilerTask(project);

        if (configuration == null) {
            return null; // Cancelled by the user
        } else {
            return new AttachSTPData(configuration.getProfilingSettings(), configuration.getProject());
        }
    }

    public ProfilingSettings selectTaskForProfiling(final Lookup.Provider project, final SessionSettings sessionSettings,
                                                    FileObject profiledFile, boolean enableOverride) {
        TaskConfigurator.Configuration configuration = TaskConfigurator.getDefault().configureProfileProjectTask(project, profiledFile,
                                                                                                       enableOverride);

        if (configuration == null) {
            return null; // Cancelled by the user
        } else {
            return configuration.getProfilingSettings();
        }
    }

    public void setProfilingActionInvoked(boolean pai) {
        profilingActionInvoked = pai;
    }

    public boolean isProfilingActionInvoked() {
        return profilingActionInvoked;
    }

    // ---- Package-Private stuff to be used by actions in this package ----------------------------------------------------

    /**
     * Performs attaching profiler.
     */
    // To be invoked outside of EDT
    void doAttach() {
        if (isProfilingActionInvoked()) return;
        setProfilingActionInvoked(true);

        try {
            // 1. if there is profiling in progress, ask the user and possibly cancel
            if (checkProfilingInProgress()) {
                return;
            }

            // NOTE: Let's not preselect main project, force the user to choose from list of projects and remember selection
            //       Project should be passed here from hypotetic Attach To Project action (not implemented yet)
            //Project project = ProjectUtilities.getMainProject();
            Lookup.Provider project = null;
            Profiler profiler = Profiler.getDefault();

            //2. load or ask the user for attach settings
            final GlobalProfilingSettings gps = profiler.getGlobalProfilingSettings();

            SessionSettings ss = new SessionSettings();
            ss.setPortNo(gps.getPortNo());

//                        ProjectTypeProfiler ptp = null;
//
//                        if (project != null) {
//                            ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);
//                        }
//
//                        if (ptp != null) {
//                            ptp.setupProjectSessionSettings(project, ss); // can be null in case of global attach
//                        }

            ProjectProfilingSupport pps = ProjectProfilingSupport.get(project);
            pps.setupProjectSessionSettings(ss);

            boolean settingsAccepted = false;
            ProfilingSettings ps = null;

            while (!settingsAccepted) {
                // 4. let the user choose the task to perform => result is selected ProfilingSettings and modified shared
                //    singleton of global settings, all changes are persisted to disk
                final AttachSTPData asd = selectTaskForAttach(project, ss);

                if (asd == null) {
                    return; // cancelled by the user
                }

                project = asd.getProject(); // project could have changed
                ps = asd.getSettings();
                ProfilerLogger.log(">>> Project: " + project); // NOI18N
                ProfilerLogger.log(">>> Profiling settings: " + ps); // NOI18N
                                                                     // Here was a check for enormous profiling overhead when profiling Web Projects.
                                                                     // Generally, this is the right place to give ProjectTypeProfiler a chance to
                                                                     // accept/reject current profiling settings before starting new profiling session.

                settingsAccepted = true;
            }

            // 5. start the actual attach process with selected settings
            ((NetBeansProfiler) profiler).setProfiledProject(project, null);

            // 6. the user may have altered the attach settings from the Select task panel, let's reread them
            AttachSettings as = null;

            try {
                as = ProjectStorage.loadAttachSettings(project);
            } catch (IOException e) {
                ProfilerDialogs.displayWarning(Bundle.ProfilingSupport_FailedLoadSettingsMsg(e.getMessage()));
                ProfilerLogger.log(e);
            }

            ProfilerLogger.log(">>> Attach settings: " + as); // NOI18N

            if (as == null) {
//                            AttachWizard attachWizard = new AttachWizard();
//                            return attachWizard.init(); // as == null resets previous attach wizard settings
//
//                            final WizardDescriptor wd = attachWizard.getWizardDescriptor();
//                            final Dialog d = ProfilerDialogs.createDialog(wd);
//                            d.setVisible(true);
//
//                            if (wd.getValue() != WizardDescriptor.FINISH_OPTION) {
//                                return; // cancelled by the user
//                            }
//
//                            attachWizard.finish(); // wizard correctly finished
//
//                            as = attachWizard.getAttachSettings();
                as = AttachWizard.getDefault().configure(as);
                if (as == null) return; // cancelled by the user
                ProjectStorage.saveAttachSettings(project, as);
            }

            if (!as.isRemote() && as.isDynamic16()) {
                // we need to prompt the user for PID
                int pid = selectApplicationPid();

                if (pid == -1) {
                    return; // cancelled by the user
                }

                as.setPid(pid);
            }

            // 7. start the actual attach
            profiler.attachToApp(ps, as);
        } finally {
            setProfilingActionInvoked(false);
        }
    }

    // To be invoked outside of EDT
    void modifyProfiling() {
        if (isProfilingActionInvoked()) return;
        setProfilingActionInvoked(true);
        
        NetBeansProfiler profiler = NetBeansProfiler.getDefaultNB();
        TargetAppRunner targetAppRunner = profiler.getTargetAppRunner();

        try {
            ProfilingSettings settings;
            boolean attach = profiler.getProfilingMode() == Profiler.MODE_ATTACH;

            targetAppRunner.getAppStatusHandler().pauseLiveUpdates();
            settings = selectTaskForProfiling(profiler.getProfiledProject(),null,profiler.getProfiledSingleFile(),attach);

            if (settings == null) {
                return; // Cancelled by the user
            } else {
                profiler.modifyCurrentProfiling(settings);
            }
        } finally {
            setProfilingActionInvoked(false);
            targetAppRunner.getAppStatusHandler().resumeLiveUpdates();
        }
    }
}
