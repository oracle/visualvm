/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.attach.steps;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.common.AttachSettings;
import org.graalvm.visualvm.lib.common.integration.IntegrationUtils;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.attach.providers.RemotePackExporter;
import org.graalvm.visualvm.lib.profiler.attach.spi.AttachStepsProvider;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
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
    "AttachDialog_Steps_EnsureCorrectJava=Make sure the target application is configured to run using {0}.", // NOI18N
    "AttachDialog_Steps_Java6=Java 6+", // NOI18N
    "AttachDialog_Steps_Java6_32b=Java 6+, 32bit", // NOI18N
    "AttachDialog_Steps_Java6_64b=Java 6+, 64bit", // NOI18N
    "AttachDialog_Steps_Java5=Java 5", // NOI18N
    "AttachDialog_Steps_Java5_32b=Java 5, 32bit", // NOI18N
    "AttachDialog_Steps_Java5_64b=Java 5, 64bit", // NOI18N
    "AttachDialog_Steps_JavaCvm=CVM", // NOI18N
    "AttachDialog_Steps_JavaSeEmbedded=Java SE Embedded", // NOI18N
    "#{0}, {1} provide begin/end of HTML link",
    "AttachDialog_Steps_SwitchToJava6Up={0}Click{1} to update steps for profiling JDK 6+ applications.", // NOI18N
    "#{0}, {1} provide begin/end of HTML link",
    "AttachDialog_Steps_SwitchToJava5={0}Click{1} to update steps for profiling JDK 5 applications.", // NOI18N
    "#{0}, {1} provide begin/end of HTML link",
    "AttachDialog_Steps_SwitchTo32BitArch={0}Click{1} to update steps for profiling 32bit applications.", // NOI18N
    "#{0}, {1} provide begin/end of HTML link",
    "AttachDialog_Steps_SwitchTo64BitArch={0}Click{1} to update steps for profiling 64bit applications.", // NOI18N
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
    protected static final String LINK_JDK5 = "file:/jdk5"; // NOI18N
    protected static final String LINK_JDK6UP = "file:/jdk6up"; // NOI18N
    protected static final String LINK_32ARCH = "file:/32arch"; // NOI18N
    protected static final String LINK_64ARCH = "file:/64arch"; // NOI18N
    
    protected String currentJDK = LINK_JDK6UP;
    protected String currentARCH = LINK_64ARCH;
    
    
    private final Set<ChangeListener> listeners = new HashSet();
    
    public synchronized final void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
    
    public synchronized final void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    
    protected synchronized final void fireChange(ChangeEvent e) {
        if (e == null) e = new ChangeEvent(this);
        for (ChangeListener listener : listeners) listener.stateChanged(e);
    }
    
    
    public String getSteps(AttachSettings settings) {
        if (settings.isRemote()) return remoteDirectSteps(settings);
        else if (settings.isDirect()) return localDirectSteps(settings);
        else return localDynamicSteps(settings);
    }
    
    public void handleAction(String action, AttachSettings settings) {
        if (LINK_CLIPBOARD.equals(action)) copyParameters(settings);
        else if (LINK_REMOTEPACK.equals(action)) createRemotePack(settings);
        else if (LINK_JDK5.equals(action)) switchToJDK5();
        else if (LINK_JDK6UP.equals(action)) switchToJDK6Up();
        else if (LINK_32ARCH.equals(action)) switchTo32ARCH();
        else if (LINK_64ARCH.equals(action)) switchTo64ARCH();
    }
    
    
    protected void switchToJDK5() {
        currentJDK = LINK_JDK5;
        fireChange(null);
    }
    
    protected void switchToJDK6Up() {
        currentJDK = LINK_JDK6UP;
        fireChange(null);
    }
    
    protected void switchTo32ARCH() {
        currentARCH = LINK_32ARCH;
        fireChange(null);
    }
    
    protected void switchTo64ARCH() {
        currentARCH = LINK_64ARCH;
        fireChange(null);
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
        b.append(Bundle.AttachDialog_Steps_EnsureCorrectJava(
                getCorrectJavaMsg(currentJDK, currentARCH)));
        String linkStart = " <a href='"; // NOI18N
        linkStart += LINK_JDK6UP.equals(currentJDK) ? LINK_JDK5 : LINK_JDK6UP;
        linkStart += "'>"; // NOI18N
        String linkEnd = "</a> "; // NOI18N
        b.append(LINK_JDK6UP.equals(currentJDK) ?
                Bundle.AttachDialog_Steps_SwitchToJava5(linkStart, linkEnd) :
                Bundle.AttachDialog_Steps_SwitchToJava6Up(linkStart, linkEnd));
        if (!IntegrationUtils.PLATFORM_MAC_OS.equals(IntegrationUtils.getLocalPlatform(-1))) {
            linkStart = " <a href='"; // NOI18N
            linkStart += LINK_64ARCH.equals(currentARCH) ? LINK_32ARCH : LINK_64ARCH;
            linkStart += "'>"; // NOI18N
            b.append(LINK_64ARCH.equals(currentARCH) ?
                    Bundle.AttachDialog_Steps_SwitchTo32BitArch(linkStart, linkEnd) :
                    Bundle.AttachDialog_Steps_SwitchTo64BitArch(linkStart, linkEnd));
        }
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
        if (isCVMJVM(settings)) {
            b.append(Bundle.AttachDialog_Steps_EnsureCorrectJava(
                    Bundle.AttachDialog_Steps_JavaCvm()));
        } else if (isARMJVM(settings)) {
            b.append(Bundle.AttachDialog_Steps_EnsureCorrectJava(
                    Bundle.AttachDialog_Steps_JavaSeEmbedded()));
        } else {
            b.append(LINK_JDK6UP.equals(currentJDK) ?
                    Bundle.AttachDialog_Steps_EnsureCorrectJava(
                    Bundle.AttachDialog_Steps_Java6()) :
                    Bundle.AttachDialog_Steps_EnsureCorrectJava(
                    Bundle.AttachDialog_Steps_Java5()));
            String linkStart = " <a href='"; // NOI18N
            linkStart += LINK_JDK6UP.equals(currentJDK) ? LINK_JDK5 : LINK_JDK6UP;
            linkStart += "'>"; // NOI18N
            String linkEnd = "</a> "; // NOI18N
            b.append(LINK_JDK6UP.equals(currentJDK) ?
                    Bundle.AttachDialog_Steps_SwitchToJava5(linkStart, linkEnd) :
                    Bundle.AttachDialog_Steps_SwitchToJava6Up(linkStart, linkEnd));
        }
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(2));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_CreateRemotePack("'" + LINK_REMOTEPACK + "'", "<code>&lt;remote&gt;</code>")); // NOI18N
        b.append("</div>"); // NOI18N
        b.append("<br/>"); // NOI18N
        b.append("<div>"); // NOI18N
        b.append("<b>"); // NOI18N
        b.append(Bundle.AttachDialog_Steps_Step(3));
        b.append("</b> "); // NOI18N
        b.append(Bundle.AttachDialog_Steps_RunCalibrateScript("<code>" + IntegrationUtils.getRemoteCalibrateCommandString(settings.getHostOS(), getPlatform(settings, currentJDK)) + "</code>")); // NOI18N
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
        return IntegrationUtils.getProfilerAgentCommandLineArgs(getOS(settings, currentARCH),
                getPlatform(settings, currentJDK), settings.isRemote(), settings.getPort());
    }
    
    protected void copyParameters(AttachSettings settings) {
        String parameters = parameters(settings);
        parameters = parameters.replace("&lt;", "<").replace("&gt;", ">"); // NOI18N
        StringSelection s = new StringSelection(parameters);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
        ProfilerDialogs.displayInfo(Bundle.AttachDialog_CopiedToClipboard());
    }
    
    protected void createRemotePack(final AttachSettings settings) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFileChooser chooser = new JFileChooser();
                File tmpDir = new File(System.getProperty("java.io.tmpdir")); // NOI18N
                chooser.setDialogTitle(Bundle.AttachDialog_RemotePackDialogCaption());
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setSelectedFile(tmpDir);
                chooser.setCurrentDirectory(tmpDir);
                chooser.setMultiSelectionEnabled(false);
                if ((JFileChooser.CANCEL_OPTION & chooser.showSaveDialog(chooser)) == 0) {
                    final String path = chooser.getSelectedFile().getAbsolutePath();
                    final String jdkF = currentJDK;
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            try {
                                String packPath = exportRemotePack(path, settings, jdkF);
                                ProfilerDialogs.displayInfo(Bundle.AttachDialog_RemotePackSaved(packPath));
                            } catch (IOException ex) {
                                System.err.println("Exception creating remote pack: " + ex); // NOI18N
                            }
                        }
                    });
                }
            }
        });
    }
    
    private static final AtomicBoolean exportRunning = new AtomicBoolean(false);
    protected static String exportRemotePack(String path, AttachSettings settings, String jdk) throws IOException {
        if (exportRunning.compareAndSet(false, true)) {
            try {
                if (path != null && path.endsWith(File.separator))
                    path = path.substring(0, path.length() - 1);
                return RemotePackExporter.getInstance().export(
                        path, getOS(settings, null), getPlatform(settings, jdk));
            } finally {
                exportRunning.compareAndSet(true, false);
            }
        } else {
            throw new IOException();
        }
    }
    
    protected static String getOS(AttachSettings settings, String arch) {
//        if (!settings.isRemote()) return settings.getHostOS();
        if (!settings.isRemote()) {
            return IntegrationUtils.getLocalPlatform(arch == LINK_64ARCH ? 64 : 32);
        } else {
            String hostOS = settings.getHostOS();
            if (IntegrationUtils.PLATFORM_WINDOWS_CVM.equals(hostOS))
                return IntegrationUtils.PLATFORM_WINDOWS_OS;
            if (IntegrationUtils.PLATFORM_LINUX_CVM.equals(hostOS))
                return IntegrationUtils.PLATFORM_LINUX_OS;
            else return settings.getHostOS();
        }
    }
    
    protected static String getPlatform(AttachSettings settings, String jdk) {
        if (settings.isRemote() && isCVMJVM(settings))
            return IntegrationUtils.PLATFORM_JAVA_CVM;
        else if (LINK_JDK5.equals(jdk))
            return IntegrationUtils.PLATFORM_JAVA_50;
        else
            return IntegrationUtils.PLATFORM_JAVA_60;
    }
    
    protected static boolean isCVMJVM(AttachSettings settings) {
        String hostOS = settings.getHostOS();
        return IntegrationUtils.PLATFORM_WINDOWS_CVM.equals(hostOS) ||
               IntegrationUtils.PLATFORM_LINUX_CVM.equals(hostOS);
    }
    
    protected static boolean isARMJVM(AttachSettings settings) {
        String hostOS = settings.getHostOS();
        return IntegrationUtils.PLATFORM_LINUX_ARM_OS.equals(hostOS);
    }
    
    protected static String getCorrectJavaMsg(String currentJDK, String currentARCH) {
        if (IntegrationUtils.PLATFORM_MAC_OS.equals(IntegrationUtils.getLocalPlatform(-1))) {
            return LINK_JDK6UP.equals(currentJDK) ?
                    Bundle.AttachDialog_Steps_Java6() :
                    Bundle.AttachDialog_Steps_Java5();
        } else {
            if (LINK_64ARCH.equals(currentARCH)) {
                return LINK_JDK6UP.equals(currentJDK) ?
                    Bundle.AttachDialog_Steps_Java6_64b() :
                    Bundle.AttachDialog_Steps_Java5_64b();
            } else {
                return LINK_JDK6UP.equals(currentJDK) ?
                    Bundle.AttachDialog_Steps_Java6_32b() :
                    Bundle.AttachDialog_Steps_Java5_32b();
            }
        }
    }
    
}
