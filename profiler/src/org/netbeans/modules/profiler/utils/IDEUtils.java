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

package org.netbeans.modules.profiler.utils;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.ProfilerModule;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.ui.stp.ProfilingSettingsManager;
import org.openide.DialogDescriptor;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.profiler.utilities.queries.SettingsFolderQuery;
import org.openide.filesystems.URLMapper;


/**
 * Utilities for interaction with the NetBeans IDE
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class IDEUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CREATE_NEW_CONFIGURATION_HINT = NbBundle.getMessage(IDEUtils.class,
                                                                                    "IDEUtils_CreateNewConfigurationHint"); // NOI18N
    private static final String SELECT_SETTINGS_CONFIGURATION_LABEL_TEXT = NbBundle.getMessage(IDEUtils.class,
                                                                                               "IDEUtils_SelectSettingsConfigurationLabelText"); // NOI18N
    private static final String SELECT_SETTINGS_CONFIGURATION_DIALOG_CAPTION = NbBundle.getMessage(IDEUtils.class,
                                                                                                   "IDEUtils_SelectSettingsConfigurationDialogCaption"); // NOI18N
    private static final String INVALID_PLATFORM_PROJECT_MSG = NbBundle.getMessage(IDEUtils.class,
                                                                                   "IDEUtils_InvalidPlatformProjectMsg"); // NOI18N
    private static final String INVALID_PLATFORM_PROFILER_MSG = NbBundle.getMessage(IDEUtils.class,
                                                                                    "IDEUtils_InvalidPlatformProfilerMsg"); // NOI18N
    private static final String INVALID_TARGET_JVM_EXEFILE_ERROR = NbBundle.getMessage(IDEUtils.class,
                                                                                       "IDEUtils_InvalidTargetJVMExeFileError"); // NOI18N // TODO: move to this package's bundle
    private static final String ERROR_CONVERTING_PROFILING_SETTINGS_MESSAGE = NbBundle.getMessage(IDEUtils.class,
                                                                                                  "IDEUtils_ErrorConvertingProfilingSettingsMessage"); //NOI18N
    private static final String LIST_ACCESS_NAME = NbBundle.getMessage(IDEUtils.class, "IDEUtils_ListAccessName"); //NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(IDEUtils.class, "IDEUtils_OkButtonText"); //NOI18N
                                                                                                               // -----
    private static final String SETTINGS_FOR_ATTR = "settingsFor";
    private static final RequestProcessor profilerRequestProcessor = new RequestProcessor("Profiler Request Processor", 1); // NOI18N
    private static final ErrorManager profilerErrorManager = ErrorManager.getDefault().getInstance("org.netbeans.modules.profiler"); // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getAntProfilerStartArgument15(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_15_STRING);
    }

    public static String getAntProfilerStartArgument16(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_16_STRING);
    }

    public static String getAntProfilerStartArgument17(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_17_STRING);
    }

    // Searches for a localized help. The default directory is <profiler_cluster>/docs/profiler,
    // localized help is in <profiler_cluster>/docs/profiler_<locale_suffix> as obtained by NbBundle.getLocalizingSuffixes()
    // see Issue 65429 (http://www.netbeans.org/issues/show_bug.cgi?id=65429)
    public static String getHelpDir() {
        Iterator suffixesIterator = NbBundle.getLocalizingSuffixes();
        File localizedHelpDir = null;

        while (suffixesIterator.hasNext() && (localizedHelpDir == null)) {
            localizedHelpDir = InstalledFileLocator.getDefault()
                                                   .locate("docs/profiler" + suffixesIterator.next(),
                                                           "org.netbeans.modules.profiler", false); //NOI18N
        }

        if (localizedHelpDir == null) {
            return null;
        } else {
            return localizedHelpDir.getPath();
        }
    }

    public static JavaPlatform getJavaPlatformByName(String platformName) {
        if (platformName != null) {
            JavaPlatform[] platforms = JavaPlatformManager.getDefault().getPlatforms(platformName, null);

            for (int i = 0; i < platforms.length; i++) {
                JavaPlatform platform = platforms[i];

                if ((platformName != null) && platform.getDisplayName().equals((platformName))) {
                    return platform;
                }
            }
        }

        return null;
    }

    public static String getLibsDir() {
        final File dir = InstalledFileLocator.getDefault()
                                             .locate(ProfilerModule.LIBS_DIR + "/jfluid-server.jar",
                                                     "org.netbeans.lib.profiler", false); //NOI18N

        if (dir == null) {
            return null;
        } else {
            return dir.getParentFile().getPath();
        }
    }

    public static Component getMainWindow() {
        return WindowManager.getDefault().getMainWindow();
    }

    public static int getPlatformArchitecture(JavaPlatform platform) {
        assert platform != null : "Platform may not be NULL"; // NOI18N

        Map props = platform.getSystemProperties();
        String arch = (String) props.get("sun.arch.data.model"); // NOI18N

        if (arch == null) {
            return 32;
        }

        return Integer.parseInt(arch);
    }

    public static int getPlatformJDKMinor(JavaPlatform platform) {
        assert platform != null : "Platform may not be NULL"; // NOI18N

        Map props = platform.getSystemProperties();
        String ver = (String) props.get("java.version"); // NOI18N

        return Platform.getJDKMinorNumber(ver);
    }

    /** Gets a version for to provided JavaPlatform. The platform passed cannot be null.
     *
     * @param platform A JavaPlatform for which we need the java executable path
     * @return A path to java executable or null if not found
     * @see CommonConstants.JDK_15_STRING
     * @see CommonConstants.JDK_16_STRING
     * @see CommonConstants.JDK_17_STRING
     */
    public static String getPlatformJDKVersion(JavaPlatform platform) {
        assert platform != null : "Platform may not be NULL"; // NOI18N

        Map props = platform.getSystemProperties();
        String ver = (String) props.get("java.version"); // NOI18N

        if (ver == null) {
            return null;
        }

        if (ver.startsWith("1.5")) {
            return CommonConstants.JDK_15_STRING; // NOI18N
        } else if (ver.startsWith("1.6")) {
            return CommonConstants.JDK_16_STRING; // NOI18N
        } else if (ver.startsWith("1.7")) {
            return CommonConstants.JDK_17_STRING; // NOI18N
        } else {
            return null;
        }
    }

    /** Gets a path to java executable for specified platform. The platform passed cannot be null.
     * Errors when obtaining the java executable will be reported to the user and null will be returned.
     *
     * @param platform A JavaPlatform for which we need the java executable path
     * @return A path to java executable or null if not found
     */
    public static String getPlatformJavaFile(JavaPlatform platform) {
        assert platform != null : "Platform may not be NULL"; // NOI18N

        FileObject fo = platform.findTool("java"); // NOI18N

        if (fo == null) { // probably invalid platform

            if (ProfilerIDESettings.getInstance().getJavaPlatformForProfiling() == null) {
                // used platform defined for project
                Profiler.getDefault()
                        .displayError(MessageFormat.format(INVALID_PLATFORM_PROJECT_MSG,
                                                           new Object[] { platform.getDisplayName() }));
            } else {
                // used platform defined in Options / Profiler
                Profiler.getDefault()
                        .displayError(MessageFormat.format(INVALID_PLATFORM_PROFILER_MSG,
                                                           new Object[] { platform.getDisplayName() }));
            }

            return null;
        }

        String jvmExe = null;

        try {
            File file = FileUtil.toFile(fo);
            jvmExe = file.getAbsolutePath();
            MiscUtils.checkFileForName(jvmExe);
        } catch (Exception e) {
            ErrorManager.getDefault()
                        .annotate(e,
                                  MessageFormat.format(INVALID_TARGET_JVM_EXEFILE_ERROR, new Object[] { jvmExe, e.getMessage() }));
            ErrorManager.getDefault().notify(ErrorManager.ERROR, e);
        }

        return jvmExe;
    }

    public static RequestProcessor getProfilerRequestProcessor() {
        return profilerRequestProcessor;
    }

    public static Project getProjectFromSettingsFolder(FileObject settingsFolder) {
        Object o = settingsFolder.getAttribute(SETTINGS_FOR_ATTR);
        if (o instanceof URL) {
            FileObject d = URLMapper.findFileObject((URL) o);
            if (d != null && d.isFolder()) {
                try {
                    return ProjectManager.getDefault().findProject(d);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        Project p = FileOwnerQuery.getOwner(settingsFolder);
        try {
            if (p != null && getProjectSettingsFolder(p, false) == settingsFolder) {
                return p;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public static FileObject getProjectSettingsFolder(Project project, boolean create)
                                               throws IOException {
        if (project == null) { // global folder for attach

            return getSettingsFolder(true);
        } else {
            // resolve 'nbproject'
            FileObject nbproject = project.getProjectDirectory().getFileObject("nbproject"); // NOI18N
            FileObject d;
            if (nbproject != null) {
                // For compatibility, continue to use nbproject/private/profiler for Ant-based projects.
                d = create ? FileUtil.createFolder(nbproject, "private/profiler") : nbproject.getFileObject("private/profiler"); // NOI18N
            } else {
                // Maven projects, autoprojects, etc.
                d = ProjectUtils.getCacheDirectory(project, IDEUtils.class);
            }
            if (d != null) {
                d.setAttribute(SETTINGS_FOR_ATTR, project.getProjectDirectory().getURL()); // NOI18N
            }
            return d;
        }
    }

    public static FileObject getSettingsFolder(final boolean create)
                                        throws IOException {
        return SettingsFolderQuery.getDefault().getSettingsFolder(create);
    }

    /**
     * @param temp the component
     * @return the TopComponent that contains this component or null if not found (the componenet is not in any container or not under TopComponent).
     */
    public static TopComponent getTopComponent(Component temp) {
        while (!(temp instanceof TopComponent)) {
            temp = temp.getParent();

            if (temp == null) {
                return null;
            }
        }

        return (TopComponent) temp;
    }

    // Stores settings from .properties file to .xml properties file and then deletes the .properties file
    public static void convertPropertiesToXML(FileObject propertiesFO, FileObject xmlFO) {
        Properties properties;
        FileLock propertiesFOLock = null;
        FileLock xmlFOLock = null;

        try {
            // load the configuration from .properties file
            propertiesFOLock = propertiesFO.lock();

            final InputStream fis = propertiesFO.getInputStream();
            final BufferedInputStream bis = new BufferedInputStream(fis);
            properties = new Properties();
            properties.load(bis);
            bis.close();
            fis.close();

            // save the configuration to .xml file
            xmlFOLock = xmlFO.lock();

            final OutputStream fos = xmlFO.getOutputStream(xmlFOLock);
            final BufferedOutputStream bos = new BufferedOutputStream(fos);
            properties.storeToXML(bos, ""); //NOI18N
            bos.close();
            fos.close();

            // delete the .properties file
            propertiesFO.delete(propertiesFOLock);
        } catch (Exception e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.notify(new NotifyDescriptor.Message(MessageFormat.format(ERROR_CONVERTING_PROFILING_SETTINGS_MESSAGE,
                                                                                     new Object[] {
                                                                                         FileUtil.toFile(propertiesFO).getPath(),
                                                                                         FileUtil.toFile(xmlFO).getPath(),
                                                                                         e.getMessage()
                                                                                     }), NotifyDescriptor.WARNING_MESSAGE));
        } finally {
            if (propertiesFOLock != null) {
                propertiesFOLock.releaseLock();
            }

            if (xmlFOLock != null) {
                xmlFOLock.releaseLock();
            }
        }
    }

    public static Properties duplicateProperties(Properties props) {
        Properties ret = new Properties();

        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            ret.setProperty(key, props.getProperty(key));
        }

        return ret;
    }

    public static ProgressHandle indeterminateProgress(String title, final int timeout) {
        final ProgressHandle ph = ProgressHandleFactory.createHandle(title);
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
                public void run() {
                    ph.setInitialDelay(timeout);
                    ph.start();
                }
            });

        return ph;
    }

    public static void runInEventDispatchThread(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    public static void runInEventDispatchThreadAndWait(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InvocationTargetException e) {
                profilerErrorManager.notify(e);
            } catch (InterruptedException e) {
                profilerErrorManager.notify(e);
            }
        }
    }

    public static void runInProfilerRequestProcessor(final Runnable r) {
        profilerRequestProcessor.post(r);
    }

    /**
     * Opens a dialog that allows the user to select one of existing profiling settings
     */
    public static ProfilingSettings selectSettings(Project project, int type, ProfilingSettings[] availableSettings,
                                                   ProfilingSettings settingsToSelect) {
        Object[] settings = new Object[availableSettings.length + 1];

        for (int i = 0; i < availableSettings.length; i++) {
            settings[i] = availableSettings[i];
        }

        settings[availableSettings.length] = CREATE_NEW_CONFIGURATION_HINT;

        // constuct the UI
        final JLabel label = new JLabel(SELECT_SETTINGS_CONFIGURATION_LABEL_TEXT);
        final JButton okButton = new JButton(OK_BUTTON_TEXT);
        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(450, 250));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setLayout(new BorderLayout(0, 5));
        panel.add(label, BorderLayout.NORTH);

        final JList list = new JList(settings);
        label.setLabelFor(list);
        list.getAccessibleContext().setAccessibleName(LIST_ACCESS_NAME);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    okButton.setEnabled(list.getSelectedIndex() != -1);
                }
            });

        if (settingsToSelect != null) {
            list.setSelectedValue(settingsToSelect, true);
        } else {
            list.setSelectedIndex(0);
        }

        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        final DialogDescriptor dd = new DialogDescriptor(panel, SELECT_SETTINGS_CONFIGURATION_DIALOG_CAPTION, true,
                                                         new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton, 0,
                                                         null, null);
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            final int selectedIndex = list.getSelectedIndex();

            if (selectedIndex != -1) { // TODO [ian]: do not allow this, disable OK button if there is no selection

                if (selectedIndex < (settings.length - 1)) {
                    ProfilingSettings selectedSettings = (ProfilingSettings) settings[selectedIndex];
                    selectedSettings.setProfilingType(type);

                    return selectedSettings;
                } else { // create a new setting

                    ProfilingSettings newSettings = ProfilingSettingsManager.getDefault()
                                                                            .createNewSettings(type, availableSettings);

                    if (newSettings == null) {
                        return null; // cancelled by the user
                    }

                    newSettings.setProfilingType(type);

                    return newSettings;
                }
            }
        }

        return null;
    }

    private static String getAntProfilerStartArgument(int port, int architecture, String jdkVersion) {
        String ld = IDEUtils.getLibsDir();

        // -agentpath:D:/Testing/41 userdir/lib/deployed/jdk15/windows/profilerinterface.dll=D:\Testing\41 userdir\lib,5140
        return "-agentpath:" // NOI18N
               + Platform.getAgentNativeLibFullName(ld, false, jdkVersion, architecture) + "=" // NOI18N
               + ld + "," // NOI18N
               + port + "," // NOI18N
               + System.getProperty("profiler.agent.connect.timeout", "10"); // NOI18N // 10 seconds timeout by default
    }

    private static ArrayList getSettings(final Project project, final int mask) {
        final ArrayList matching = new ArrayList();
        ProfilingSettings[] projectSettings = ProfilingSettingsManager.getDefault().getProfilingSettings(project)
                                                                      .getProfilingSettings();

        for (ProfilingSettings settings : projectSettings) {
            if (matchesMask(settings, mask)) {
                matching.add(settings);
            }
        }

        return matching;
    }

    private static String forwardSlashes(String text) {
        return text.replace('\\', '/'); // NOI18N
    }

    private static boolean matchesMask(final ProfilingSettings settings, final int mask) {
        // TODO: should only check Monitor/CPU/Memory
        return org.netbeans.modules.profiler.ui.stp.Utils.isCPUSettings(settings);

        //    return (settings.getProfilingType() & mask) != 0;    
    }
}
