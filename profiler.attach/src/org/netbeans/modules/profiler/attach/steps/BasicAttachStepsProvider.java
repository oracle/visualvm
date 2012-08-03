/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.attach.steps;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JFileChooser;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.attach.providers.RemotePackExporter;
import org.netbeans.modules.profiler.attach.spi.AttachStepsProvider;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "AttachDialog_CopiedToClipboard=Profiler parameter(s) copied to clipboard", // NOI18N
    "AttachDialog_RemotePackDialogCaption=Choose Target Folder", // NOI18N
    "AttachDialog_RemotePackSaved=Remote profiling pack saved to {0}", // NOI18N
    "AttachDialog_Steps_Step=Step {0}:", // NOI18N
    "AttachDialog_Steps_CopyToClipboard=copy to clipboard", // NOI18N
    "AttachDialog_Steps_MakeSureStarted=Make sure the target application has been started by user {0} and is running using Java 6+.", // NOI18N
    "AttachDialog_Steps_SubmitSelectProcess=Submit this dialog and click the Attach button to select the target application process.", // NOI18N
    "AttachDialog_Steps_ConfigureToRun6=Make sure the target application is configured to run using Java 6+. Click the Help button for information on how to profile Java 5 applications.", // NOI18N
    "AttachDialog_Steps_ConfigureToRunCvm=Make sure the target application is configured to run using CVM.", // NOI18N
    "AttachDialog_Steps_StartApplication=Start the target application. The process will wait for the profiler to connect.", // NOI18N
    "AttachDialog_Steps_SubmitUnblock=Submit this dialog and click the Attach button to connect to the target application and resume its execution.", // NOI18N
    "AttachDialog_Steps_AddParameters=Add the following parameter(s) to the application startup script", // NOI18N
    "AttachDialog_Steps_RunCalibrateScript=If you have not run profiling on the remote system yet, run the {0} script first to calibrate the profiler.", // NOI18N
    "AttachDialog_Steps_CreateRemotePack=If you have not done it before <a href={0}>create a Remote profiling pack</a> for the selected OS & JVM and upload it to the remote system. Remote profiling pack root directory will be referred to as {1}." // NOI18N
})
@ServiceProvider(service = AttachStepsProvider.class)
public class BasicAttachStepsProvider extends AttachStepsProvider {
    
    protected static final String LINK_CLIPBOARD = "file:/clipboard"; // NOI18N
    protected static final String LINK_REMOTEPACK = "file:/remotepack"; // NOI18N
    
    
    public String getSteps(AttachSettings settings) {
        if (settings.isRemote()) return remoteDirectSteps(settings);
        else if (settings.isDirect()) return localDirectSteps(settings);
        else return localDynamicSteps(settings);
    }
    
    public void handleAction(String action, AttachSettings settings) {
        if (LINK_CLIPBOARD.equals(action)) copyParameters(settings);
        else if (LINK_REMOTEPACK.equals(action)) createRemotePack(settings);
    }
    
    
    protected String localDynamicSteps(AttachSettings settings) {
        StringBuilder b = new StringBuilder();
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(1));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_MakeSureStarted(System.getProperty("user.name"))); // NOI18N
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(2));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_SubmitSelectProcess());
        b.append("</div>"); // NOI18N
        return b.toString();
    }
    
    protected String localDirectSteps(AttachSettings settings) {
        StringBuilder b = new StringBuilder();
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(1));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_ConfigureToRun6());
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(2));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_AddParameters());
        b.append(" (<a href='"); // NOI18N
        b.append(LINK_CLIPBOARD);
        b.append("'>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_CopyToClipboard());
        b.append("</a>):"); // NOI18N
        b.append("</div>"); // NOI18N
        b.append("<pre>"); // NOI18N
        b.append(parameters(settings));
        b.append("</pre>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(3));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_StartApplication());
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(4));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_SubmitUnblock());
        b.append("</div>"); // NOI18N
        return b.toString();
    }
    
    protected String remoteDirectSteps(AttachSettings settings) {
        StringBuilder b = new StringBuilder();
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(1));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_CreateRemotePack("'" + LINK_REMOTEPACK + "'", "<code>&lt;remote&gt;</code>")); // NOI18N
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(2));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_RunCalibrateScript("<code>" + IntegrationUtils.getRemoteCalibrateCommandString(settings.getHostOS(), IntegrationUtils.PLATFORM_JAVA_60) + "</code>")); // NOI18N
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(3));
        b.append("</b> "); // NOI18N
        b.append(isCVMJVM(settings) ? Bundle.AttachDialog_Steps_ConfigureToRunCvm() :
                                      Bundle.AttachDialog_Steps_ConfigureToRun6());
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(4));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_AddParameters());
        b.append(" (<a href='"); // NOI18N
        b.append(LINK_CLIPBOARD);
        b.append("'>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_CopyToClipboard());
        b.append("</a>):"); // NOI18N
        b.append("</div>"); // NOI18N
        b.append("<pre>"); // NOI18N
        b.append(parameters(settings));
        b.append("</pre>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(5));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_StartApplication());
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(6));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_SubmitUnblock());
        b.append("</div>"); // NOI18N
        return b.toString();
    }
    
    protected String parameters(AttachSettings settings) {
        return IntegrationUtils.getProfilerAgentCommandLineArgs(getOS(settings),
                getPlatform(settings), settings.isRemote(), settings.getPort());
    }
    
    protected void copyParameters(AttachSettings settings) {
        String parameters = parameters(settings);
        parameters = parameters.replace("&lt;", "<").replace("&gt;", ">"); // NOI18N
        StringSelection s = new StringSelection(parameters);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
        ProfilerDialogs.displayInfo(Bundle.AttachDialog_CopiedToClipboard());
    }
    
    protected void createRemotePack(final AttachSettings settings) {
        try {
            final JFileChooser chooser = new JFileChooser();
            final File tmpDir = new File(System.getProperty("java.io.tmpdir")); // NOI18N
            chooser.setDialogTitle(Bundle.AttachDialog_RemotePackDialogCaption());
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setSelectedFile(tmpDir);
            chooser.setCurrentDirectory(tmpDir);
            chooser.setMultiSelectionEnabled(false);
            if ((JFileChooser.CANCEL_OPTION & chooser.showSaveDialog(chooser)) == 0) {
                String packPath = exportRemotePack(chooser.getSelectedFile().getAbsolutePath(), settings);
                ProfilerDialogs.displayInfo(Bundle.AttachDialog_RemotePackSaved(packPath));
            }
        } catch (IOException ex) {
            System.err.println("Exception creating remote pack: " + ex); // NOI18N
        }
    }
    
    private static final AtomicBoolean exportRunning = new AtomicBoolean(false);
    protected static String exportRemotePack(String path, AttachSettings settings) throws IOException {
        if (exportRunning.compareAndSet(false, true)) {
            try {
                return RemotePackExporter.getInstance().export(
                        path, getOS(settings), getPlatform(settings));
            } finally {
                exportRunning.compareAndSet(true, false);
            }
        } else {
            throw new IOException();
        }
    }
    
    protected static String getOS(AttachSettings settings) {
        if (!settings.isRemote()) return settings.getHostOS();
        String hostOS = settings.getHostOS();
        if (IntegrationUtils.PLATFORM_WINDOWS_CVM.equals(hostOS))
            return IntegrationUtils.PLATFORM_WINDOWS_OS;
        if (IntegrationUtils.PLATFORM_LINUX_CVM.equals(hostOS))
            return IntegrationUtils.PLATFORM_LINUX_OS;
        else return settings.getHostOS();
    }
    
    protected static String getPlatform(AttachSettings settings) {
        if (settings.isRemote() && isCVMJVM(settings))
            return IntegrationUtils.PLATFORM_JAVA_CVM;
        else return IntegrationUtils.PLATFORM_JAVA_60;
    }
    
    protected static boolean isCVMJVM(AttachSettings settings) {
        String hostOS = settings.getHostOS();
        return IntegrationUtils.PLATFORM_WINDOWS_CVM.equals(hostOS) ||
               IntegrationUtils.PLATFORM_LINUX_CVM.equals(hostOS);
    }
    
}
