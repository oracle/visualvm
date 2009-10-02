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

package org.netbeans.modules.profiler.actions;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import javax.swing.*;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;


/**
 * An action to print command-line arguments from target app.
 *
 * @author Ian Formanek
 */
public final class GetCmdLineArgumentsAction extends AbstractAction implements ProfilingStateListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String TARGET_JVM_INACTIVE_MSG = NbBundle.getMessage(GetCmdLineArgumentsAction.class,
                                                                              "GetCmdLineArgumentsAction_TargetJvmInactiveMsg"); // NOI18N
    private static final String JVM_ARGUMENTS_STRING = NbBundle.getMessage(GetCmdLineArgumentsAction.class,
                                                                           "GetCmdLineArgumentsAction_JvmArgumentsString"); // NOI18N
    private static final String MAIN_CLASS_AND_ARGS_STRING = NbBundle.getMessage(GetCmdLineArgumentsAction.class,
                                                                                 "GetCmdLineArgumentsAction_MainClassAndArgsString"); // NOI18N
                                                                                                                                      // -----

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public GetCmdLineArgumentsAction() {
        putValue(Action.NAME, NbBundle.getMessage(GetCmdLineArgumentsAction.class, "LBL_GetCmdLineArgumentsAction") // NOI18N
        );
        putValue(Action.SHORT_DESCRIPTION,
                 NbBundle.getMessage(GetCmdLineArgumentsAction.class, "HINT_GetCmdLineArgumentsAction") // NOI18N
        );
        putValue("noIconInMenu", Boolean.TRUE); //NOI18N
        
        updateEnabledState();
        Profiler.getDefault().addProfilingStateListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent evt) {
        try {
            final TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
            final ProfilerClient profilerClient = runner.getProfilerClient();
            final ProfilingSessionStatus status = runner.getProfilingSessionStatus();
            final ProfilerEngineSettings settings = runner.getProfilerEngineSettings();

            if (!profilerClient.targetJVMIsAlive()) {
                throw new ClientUtils.TargetAppOrVMTerminated(1, TARGET_JVM_INACTIVE_MSG);
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
            s.append(JVM_ARGUMENTS_STRING);
            s.append("</b><br>"); // NOI18N
            s.append(jvmArgs);
            s.append("<br><br>"); // NOI18N
            s.append("<b>"); // NOI18N
            s.append(MAIN_CLASS_AND_ARGS_STRING);
            s.append("</b><br>"); // NOI18N
            s.append(javaCommand);

            final HTMLTextArea textArea = new HTMLTextArea(s.toString());
            textArea.getAccessibleContext()
                    .setAccessibleName(NbBundle.getMessage(GetCmdLineArgumentsAction.class,
                                                           "CAPTION_JVMandMainClassCommandLineArguments")); // NOI18N

            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            p.add(new JScrollPane(textArea), BorderLayout.CENTER);
            p.setPreferredSize(new Dimension(600, 200));

            ProfilerDialogs.createDialog(new DialogDescriptor(p,
                                                              NbBundle.getMessage(GetCmdLineArgumentsAction.class,
                                                                                  "CAPTION_JVMandMainClassCommandLineArguments"), // NOI18N
                                                              true, new Object[] { DialogDescriptor.CLOSED_OPTION },
                                                              DialogDescriptor.CLOSED_OPTION, DialogDescriptor.BOTTOM_ALIGN,
                                                              null, null)).setVisible(true);
        } catch (ClientUtils.TargetAppOrVMTerminated e) {
            Profiler.getDefault()
                    .displayWarning(MessageFormat.format(NbBundle.getMessage(GetCmdLineArgumentsAction.class,
                                                                             "MSG_NotAvailableNow"),
                                                         new Object[] { e.getMessage() })); // NOI18N
        }
    }
    
    public void profilingStateChanged(final ProfilingStateEvent e) {
        updateEnabledState();
    }
    
    public void threadsMonitoringChanged() {} // ignore
    public void instrumentationChanged(final int oldInstrType, final int currentInstrType) {} // ignore
    
    private void updateEnabledState() {
        setEnabled(Profiler.getDefault().getProfilingState() == Profiler.PROFILING_RUNNING);
    }
}
