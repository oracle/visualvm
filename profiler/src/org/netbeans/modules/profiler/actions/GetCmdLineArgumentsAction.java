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

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;


/**
 * An action to print command-line arguments from target app.
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "GetCmdLineArgumentsAction_TargetJvmInactiveMsg=Target JVM is inactive",
    "GetCmdLineArgumentsAction_JvmArgumentsString=JVM arguments:",
    "GetCmdLineArgumentsAction_MainClassAndArgsString=Main class (JAR) and its arguments:",
    "LBL_GetCmdLineArgumentsAction=&View Command-line Arguments",
    "HINT_GetCmdLineArgumentsAction=View Command-line Arguments",
    "CAPTION_JVMandMainClassCommandLineArguments=JVM and Main Class Command-line Arguments",
    "MSG_NotAvailableNow=Not available at this time: {0}"
})
@ActionID(category="Profile", id="org.netbeans.modules.profiler.actions.GetCmdLineArgumentsAction")
@ActionRegistration(displayName="#LBL_GetCmdLineArgumentsAction")
@ActionReference(path="Menu/Profile/Advanced", position=200)
public final class GetCmdLineArgumentsAction extends ProfilingAwareAction {
    final private static int[] enabledStates = new int[]{Profiler.PROFILING_RUNNING};
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public GetCmdLineArgumentsAction() {
        putValue(Action.NAME, Bundle.LBL_GetCmdLineArgumentsAction());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_GetCmdLineArgumentsAction());
        putValue("noIconInMenu", Boolean.TRUE); //NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when an action occurs.
     */
    public void performAction() {
        try {
            final TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
            final ProfilerClient profilerClient = runner.getProfilerClient();
            final ProfilingSessionStatus status = runner.getProfilingSessionStatus();
            final ProfilerEngineSettings settings = runner.getProfilerEngineSettings();

            if (!profilerClient.targetJVMIsAlive()) {
                throw new ClientUtils.TargetAppOrVMTerminated(1, Bundle.GetCmdLineArgumentsAction_TargetJvmInactiveMsg());
            }

            final String jvmArgs;
            final String javaCommand;

            if (status.runningInAttachedMode) {
                jvmArgs = status.jvmArguments;
                javaCommand = status.javaCommand;
            } else {
                jvmArgs = settings.getJVMArgsAsSingleString();
                javaCommand = settings.getMainClassName() + " " + settings.getMainArgsAsSingleString(); // NOI18N
            }

            final StringBuffer s = new StringBuffer();
            s.append("<b>"); // NOI18N
            s.append(Bundle.GetCmdLineArgumentsAction_JvmArgumentsString());
            s.append("</b><br>"); // NOI18N
            s.append(jvmArgs);
            s.append("<br><br>"); // NOI18N
            s.append("<b>"); // NOI18N
            s.append(Bundle.GetCmdLineArgumentsAction_MainClassAndArgsString());
            s.append("</b><br>"); // NOI18N
            s.append(javaCommand);

            final HTMLTextArea textArea = new HTMLTextArea(s.toString());
            textArea.getAccessibleContext()
                    .setAccessibleName(Bundle.CAPTION_JVMandMainClassCommandLineArguments());

            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            p.add(new JScrollPane(textArea), BorderLayout.CENTER);
            p.setPreferredSize(new Dimension(600, 200));

            DialogDisplayer.getDefault().createDialog(new DialogDescriptor(p,
                                                              Bundle.CAPTION_JVMandMainClassCommandLineArguments(),
                                                              true, new Object[] { DialogDescriptor.CLOSED_OPTION },
                                                              DialogDescriptor.CLOSED_OPTION, DialogDescriptor.BOTTOM_ALIGN,
                                                              null, null)).setVisible(true);
        } catch (ClientUtils.TargetAppOrVMTerminated e) {
            ProfilerDialogs.displayWarning(Bundle.MSG_NotAvailableNow(e.getMessage()));
            ProfilerLogger.log(e.getMessage());
        }
    }

    @Override
    protected int[] enabledStates() {
        return enabledStates;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public String getName() {
        return Bundle.LBL_GetCmdLineArgumentsAction();
    }
}
