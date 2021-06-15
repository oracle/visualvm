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

import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
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
@ActionID(category="Profile", id="org.graalvm.visualvm.lib.profiler.actions.GetCmdLineArgumentsAction")
//@ActionRegistration(displayName="#LBL_GetCmdLineArgumentsAction")
//@ActionReference(path="Menu/Profile/Advanced", position=200)
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
