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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.api.project.Project;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ppoints.GlobalProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPointWizard;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import java.awt.Dialog;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;


/**
 * Opens the Insert New Profiling Points Wizard.
 *
 * @author Jiri Sedlacek
 */
public class InsertProfilingPointAction extends NodeAction {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ACTION_NAME = NbBundle.getMessage(InsertProfilingPointAction.class,
                                                                  "InsertProfilingPointAction_ActionName"); // NOI18N
    private static final String PROFILING_IN_PROGRESS_MSG = NbBundle.getMessage(InsertProfilingPointAction.class,
                                                                                "InsertProfilingPointAction_ProfilingInProgressMsg"); // NOI18N
    private static final String NO_PROJECT_MSG = NbBundle.getMessage(InsertProfilingPointAction.class,
                                                                     "InsertProfilingPointAction_NoProjectMsg"); // NOI18N
                                                                                                                 // -----

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public InsertProfilingPointAction() {
        putValue("noIconInMenu", Boolean.TRUE); //NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return new HelpCtx(InsertProfilingPointAction.class);
    }

    public String getName() {
        return ACTION_NAME;
    }

    public void performAction(Project project) {
        if (ProfilingPointsManager.getDefault().isProfilingSessionInProgress()) {
            NetBeansProfiler.getDefaultNB().displayWarning(PROFILING_IN_PROGRESS_MSG);

            return;
        }

        if (ProjectUtilities.getOpenedProjects().length == 0) {
            NetBeansProfiler.getDefaultNB().displayWarning(NO_PROJECT_MSG);

            return;
        }

        ProfilingPointWizard ppWizard = ProfilingPointWizard.getDefault();
        final WizardDescriptor wd = ppWizard.getWizardDescriptor(project);

        if (wd != null) { // if null then another PP is currently being created/customized and user is already notified

            final Dialog d = ProfilerDialogs.createDialog(wd);
            d.setVisible(true);

            boolean createPPoint = wd.getValue() == WizardDescriptor.FINISH_OPTION;
            ProfilingPoint profilingPoint = ppWizard.finish(!createPPoint); // Wizard must be finished even in cancelled to release its resources

            if (createPPoint) {
                ProfilingPointsManager.getDefault().addProfilingPoint(profilingPoint);

                if (profilingPoint instanceof GlobalProfilingPoint) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!ProfilingPointsWindow.getDefault().isOpened()) {
                                ProfilingPointsWindow.getDefault().open();
                                ProfilingPointsWindow.getDefault().requestVisible();
                            }
                        }
                    });
                }
            }
        }
    }

    protected boolean asynchronous() {
        return false;
    }

    protected boolean enable(Node[] nodes) {
        //    return Utils.getOpenedProjects().length > 0;
        return true; // Let's have this action enabled and show a warning if no project is opened, otherwise the user might not understand why it's disabled
    }

    protected void performAction(Node[] nodes) {
        performAction((Project) null);
    }
}
