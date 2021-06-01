/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.impl;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.jfr.JFRSnapshotSupport;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class JFRRecordingProvider {

    private final static Logger LOGGER = Logger.getLogger(JFRRecordingProvider.class.getName());

    public void jfrStartRecording(Application application) {
        jfrStartRecording(application, null);
    }
    
    public void jfrStartRecording(Application application, String params) {
        JFRParameters parameters = JFRParameters.parse(params);
        jfrStartRecording(application,
                          parameters.get(JFRParameters.NAME),
                          parameters.get(JFRParameters.SETTINGS),
                          null, // delay
                          null, // duration
                          null, // disk
                          null, // path
                          null, // maxAge
                          null, // maxSize
                          null  // dumpOnExit
                         );
    }
    
    public void jfrStartRecording(final Application application, final String name,
                                  final String settings, final String delay,
                                  final String duration, final Boolean disk,
                                  final String path, final String maxAge,
                                  final String maxSize, final Boolean dumpOnExit) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                Jvm jvm = JvmFactory.getJVMFor(application);
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandle.createHandle(NbBundle.getMessage(JFRRecordingProvider.class, "LBL_Starting_JFR_Recording"));    // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    String[] _settings = settings == null ? null : new String[] { settings };
                    if (!jvm.startJfrRecording(name, _settings, delay, duration,
                            disk, path, maxAge, maxSize, dumpOnExit)) {
                        notifyJfrDumpFailed(application);
                    } else {
                        Set<DataSource> ds = ActionUtils.getSelectedDataSources();
                        JFRDumpAction.instance().updateState(ds);
                        JFRStartAction.instance().updateState(ds);
                        JFRStopAction.instance().updateState(ds);
                    }
                } finally {
                    if (pHandle != null) {
                        final ProgressHandle pHandleF = pHandle;
                        SwingUtilities.invokeLater(() -> pHandleF.finish());
                    }
                }
            }
        });
    }

    public void remoteJfrStartRecording(Application application) {
        jfrStartRecording(application);
    }

    public void jfrStopRecording(Application application) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                Jvm jvm = JvmFactory.getJVMFor(application);
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandle.createHandle(NbBundle.getMessage(JFRRecordingProvider.class, "LBL_Stopping_JFR_Recording"));    // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    if (!jvm.stopJfrRecording()) {
                        notifyJfrStopFailed(application);
                    } else {
                        Set<DataSource> ds = ActionUtils.getSelectedDataSources();
                        JFRDumpAction.instance().updateState(ds);
                        JFRStartAction.instance().updateState(ds);
                        JFRStopAction.instance().updateState(ds);
                    }
                } finally {
                    if (pHandle != null) {
                        final ProgressHandle pHandleF = pHandle;
                        SwingUtilities.invokeLater(() -> pHandleF.finish());
                    }
                }
            }
        });
    }

    public void remoteJfrStopRecording(Application application) {
        jfrStopRecording(application);
    }

    public void createJfrDump(final Application application, final boolean stopJfr, final boolean openView) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                Jvm jvm = JvmFactory.getJVMFor(application);
                List<Long> recordings = Collections.EMPTY_LIST;
                if (jvm != null) {
                    recordings = jvm.jfrCheck();
                }
                if (jvm == null || recordings.isEmpty()) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(JFRRecordingProvider.class,
                            "MSG_Cannot_Take_JFR_dump", DataSourceDescriptorFactory. // NOI18N
                                    getDescriptor(application).getName()), NotifyDescriptor.ERROR_MESSAGE));
                    return;
                }

                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandle.createHandle(NbBundle.getMessage(JFRRecordingProvider.class, "LBL_Creating_JFR_Dump"));    // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    try {
                        File snapshotDir = application.getStorage().getDirectory();
                        String name = JFRSnapshotSupport.getCategory().createFileName();
                        File file = new File(snapshotDir, name);
                        jvm.takeJfrDump(recordings.get(0), file.getAbsolutePath());
                        if (file.isFile()) {
                            final JFRDumpImpl jfrDump = new JFRDumpImpl(file, application);
                            application.getRepository().addDataSource(jfrDump);
                            if (openView) {
                                DataSourceWindowManager.sharedInstance().openDataSource(jfrDump);
                            }
                            if (stopJfr) jfrStopRecording(application);
                        } else {
                            notifyJfrDumpFailed(application);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.INFO, "createJFRDump-Application", ex); // NOI18N
                        notifyJfrDumpFailed(application);
                    }
                } finally {
                    if (pHandle != null) {
                        final ProgressHandle pHandleF = pHandle;
                        SwingUtilities.invokeLater(() -> pHandleF.finish());
                    }
                }
            }
        });
    }

    public void createRemoteJfrDump(final Application application, final String dumpFile,
            final boolean customizeDumpFile) {

        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                JmxModel model = JmxModelFactory.getJmxModelFor(application);
                if (model == null || !model.isJfrAvailable()) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(JFRRecordingProvider.class,
                            "MSG_Dump_failed"), NotifyDescriptor.ERROR_MESSAGE)); // NOI18N
                    return;
                }
                List<Long> recordings = model.jfrCheck();
                if (recordings.isEmpty()) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(JFRRecordingProvider.class,
                            "MSG_Cannot_Take_JFR_dump", DataSourceDescriptorFactory. // NOI18N
                                    getDescriptor(application).getName()), NotifyDescriptor.ERROR_MESSAGE));
                    return;
                }
                String file = dumpFile;
                if (file == null) {
                    file = defineRemoteFile(model, customizeDumpFile);
                }
                if (file == null) {
                    return;
                }

                if (model.takeJfrDump(recordings.get(0), file) != null) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(JFRRecordingProvider.class,
                            "MSG_Dump_ok", file), NotifyDescriptor.INFORMATION_MESSAGE)); // NOI18N
                } else {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(JFRRecordingProvider.class,
                            "MSG_Dump_save_failed", file), NotifyDescriptor.ERROR_MESSAGE)); // NOI18N
                }
            }
        });
    }

    private static String defineRemoteFile(JmxModel model, boolean customizeDumpFile) {
        final String[] path = new String[1];
        path[0] = defaultJfrDumpPath(model);

        if (customizeDumpFile) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        JLabel label = new JLabel();
                        Mnemonics.setLocalizedText(label, NbBundle.getMessage(JFRRecordingProvider.class, "MSG_Remote_JFR_dump")); // NOI18N
                        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
                        JTextField field = new JTextField();
                        label.setLabelFor(field);
                        field.setText(path[0]);
                        Dimension dim = field.getPreferredSize();
                        dim.width = 350;
                        field.setPreferredSize(dim);
                        field.selectAll();
                        JPanel selector = new JPanel(new BorderLayout());
                        selector.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
                        selector.add(label, BorderLayout.NORTH);
                        selector.add(field, BorderLayout.SOUTH);

                        DialogDescriptor dd = new DialogDescriptor(selector,
                                NbBundle.getMessage(JFRRecordingProvider.class,
                                        "CAPTION_Remote_JFR_Dump"), true, null); // NOI18N
                        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                        d.pack();
                        d.setVisible(true);

                        path[0] = dd.getValue() == DialogDescriptor.OK_OPTION
                                ? field.getText() : null;
                    }
                });
            } catch (Throwable t) {
                path[0] = null;
            }
        }

        return path[0];
    }

    private static String defaultJfrDumpPath(JmxModel model) {
        String fileName = JFRSnapshotSupport.getCategory().createFileName();

        Properties sysprops = model.getSystemProperties();
        if (sysprops == null) {
            return fileName;
        }
        String jfrDumpTarget = getJfrDumpTarget(sysprops);
        if (jfrDumpTarget == null || jfrDumpTarget.isEmpty()) {
            return fileName;
        }

        String pathsep = sysprops.getProperty("file.separator"); // NOI18N
        if (!jfrDumpTarget.endsWith(pathsep)) {
            jfrDumpTarget += pathsep;
        }
        return jfrDumpTarget + fileName;
    }

    // OS codes listed in org.graalvm.visualvm.lib.jfluid.global.Platform.getOperatingSystem()
    private static String getJfrDumpTarget(Properties sysprops) {
        String targetDir = null;

        // Select directory based on target OS
        String osName = sysprops.getProperty("os.name"); // NOI18N
        if (osName != null) {
            if (osName.equals("Solaris") || osName.startsWith("SunOS")) // NOI18N
            {
                targetDir = sysprops.getProperty("user.home"); // NOI18N
            }
            targetDir = sysprops.getProperty("java.io.tmpdir"); // NOI18N
        }

        // Fallback to current working directory
        if (targetDir == null) {
            targetDir = sysprops.getProperty("user.dir"); // NOI18N
        }
        return targetDir;
    }

    public void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(new SnapshotListener(), Snapshot.class);
        DataSourceRepository.sharedInstance().addDataChangeListener(new ApplicationListener(), Application.class);
    }

    private void processNewSnapshot(Snapshot snapshot) {
        if (snapshot instanceof JFRSnapshotImpl) {
            return;
        }
        boolean appSnapshot = snapshot instanceof ApplicationSnapshot;
        File snapshotFile = snapshot.getFile();
        if (snapshotFile != null && snapshotFile.isDirectory()) {
            File[] files = snapshotFile.listFiles(JFRSnapshotSupport.getCategory().getFilenameFilter());
            if (files == null) {
                return;
            }
            Set<JFRDumpImpl> jfrDumps = new HashSet();
            for (File file : files) {
                try {
                    JFRDumpImpl jfrDump = new JFRDumpImpl(file, snapshot);
                    if (appSnapshot) {
                        jfrDump.forceViewClosable(true);
                    }
                    jfrDumps.add(jfrDump);
                } catch (IOException ex) {

                }
            }
            snapshot.getRepository().addDataSources(jfrDumps);
        }
    }

    private void processNewApplication(Application application) {
        Storage storage = application.getStorage();
        if (storage.directoryExists()) {
            File[] files = storage.getDirectory().listFiles(JFRSnapshotSupport.getCategory().getFilenameFilter());
            if (files == null) {
                return;
            }
            Set<JFRDumpImpl> jfrDumps = new HashSet();
            for (File file : files) {
                try {
                    jfrDumps.add(new JFRDumpImpl(file, application));
                } catch (IOException ex) {

                }
            }
            application.getRepository().addDataSources(jfrDumps);
        }
    }

    private void notifyJfrDumpFailed(final DataSource dataSource) {
        String displayName = DataSourceDescriptorFactory.getDescriptor(dataSource).getName();
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(JFRRecordingProvider.class,
                "MSG_Cannot_Take_JFR_dump", displayName), // NOI18N
                NotifyDescriptor.ERROR_MESSAGE));
    }

    private void notifyJfrStopFailed(final DataSource dataSource) {
        String displayName = DataSourceDescriptorFactory.getDescriptor(dataSource).getName();
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(JFRRecordingProvider.class,
                "MSG_Cannot_Stop_JFR", displayName), // NOI18N
                NotifyDescriptor.ERROR_MESSAGE));
    }

    private class SnapshotListener implements DataChangeListener<Snapshot> {

        public void dataChanged(DataChangeEvent<Snapshot> event) {
            final Set<Snapshot> snapshots = event.getAdded();
            if (!snapshots.isEmpty()) {
                VisualVM.getInstance().runTask(new Runnable() {
                    public void run() {
                        for (Snapshot snapshot : snapshots) {
                            processNewSnapshot(snapshot);
                        }
                    }
                });
            }
        }

    }

    private class ApplicationListener implements DataChangeListener<Application> {

        public void dataChanged(DataChangeEvent<Application> event) {
            final Set<Application> applications = event.getAdded();
            if (!applications.isEmpty()) {
                VisualVM.getInstance().runTask(new Runnable() {
                    public void run() {
                        for (Application application : applications) {
                            processNewApplication(application);
                        }
                    }
                });
            }
        }
    }
}
