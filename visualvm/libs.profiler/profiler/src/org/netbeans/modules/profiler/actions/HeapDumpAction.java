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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.modules.profiler.api.ProfilerStorage;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.netbeans.modules.profiler.v2.SnapshotsWindow;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;


/**
 * Save heap dump of profiled application to file
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapDumpAction_ActionName=Take &Heap Dump...",
    "HeapDumpAction_RequiredJdkMsg=The profiled application must be run on JDK 1.6, 1.7 or 1.5.0_12 to take a heap dump",
    "HeapDumpAction_RemoteUnsupportedMsg=Taking heap dump on a remote machine not supported.",
    "HeapDumpAction_WrongDestinationMsg=Dumping heap failed, cannot resolve destination file.",
    "HeapDumpAction_DumpingHeapText=Dumping heap...",
    "HeapDumpAction_SavedDialogCaption=Heap Dump Saved",
    "HeapDumpAction_SavedDialogText=<html><b>The heap has been successfuly saved to a file.</b><br>Do you want to open it in HeapWalker?</html>",
    "HeapDumpAction_DumpingFailedMsg=Taking heap dump failed. See NetBeans logfile for details.",
    "HeapDumpAction_DestinationDialogCaption=Choose Heap Dump Destination",
    "HeapDumpAction_LocationProjectString=Profiled project",
    "HeapDumpAction_LocationGlobalString=Default storage for external processes",
    "HeapDumpAction_DirectoryDialogCaption=Choose Directory",
    "HeapDumpAction_OkButtonText=OK",
    "HeapDumpAction_DestinationLabelText=<html><b><nobr>Choose heap dump destination:</nobr></b></html>",
    "HeapDumpAction_DefaultLocationRadioText=Default location",
    "HeapDumpAction_CustomLocationRadioText=Custom directory:",
    "HeapDumpAction_BrowseButtonText=Browse",
    "HeapDumpAction_ToolTip=Take heap dump from the profiled process",
    "HeapDumpAction_ToolTipNoRemote=Take heap dump from the profiled application (not supported for remote profiling)"
})
public final class HeapDumpAction extends ProfilingAwareAction {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    final private static class Singleton {
        final private static HeapDumpAction INSTANCE = new HeapDumpAction();
    }
    
    private static class ChooseHeapdumpTargetPanel extends JPanel implements HelpCtx.Provider {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final String HELP_CTX_KEY = "ChooseHeapdumpTargetPanel.HelpCtx";  // NOI18N
        private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
        public static final int DESTINATION_DEFAULT = 0;
        public static final int DESTINATION_CUSTOM = 1;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JButton customLocationButton;
        private JButton okButton;
        private JLabel chooseDestinationLabel;
        private JRadioButton customLocationRadio;
        private JRadioButton defaultLocationRadio;
        private JTextField customLocationField;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ChooseHeapdumpTargetPanel() {
            initComponents();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public HelpCtx getHelpCtx() {
            return HELP_CTX;
        }
        
        public String getCustomDirectory() {
            return customLocationField.getText();
        }
        
        public void setCustomDirectory(String directory) {
            customLocationField.setText(directory);
        }

        public int getDestinationType() {
            if (defaultLocationRadio.isSelected()) {
                return DESTINATION_DEFAULT;
            } else {
                return DESTINATION_CUSTOM;
            }
        }
        
        public void setDestinationType(int type) {
            defaultLocationRadio.setSelected(type == DESTINATION_DEFAULT);
        }

        public JButton getOKButton() {
            return okButton;
        }

        public void updateDefaultLocation(String location) {
            defaultLocationRadio.setText(location);
        }

        private void initComponents() {
            okButton = new JButton(Bundle.HeapDumpAction_OkButtonText());

            setLayout(new GridBagLayout());

            GridBagConstraints c;
            ButtonGroup group = new ButtonGroup();

            chooseDestinationLabel = new JLabel(Bundle.HeapDumpAction_DestinationLabelText());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(15, 10, 8, 5);
            add(chooseDestinationLabel, c);

            defaultLocationRadio = new JRadioButton(Bundle.HeapDumpAction_DefaultLocationRadioText());
            group.add(defaultLocationRadio);
            defaultLocationRadio.setSelected(true);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 15, 0, 5);
            add(defaultLocationRadio, c);

            customLocationRadio = new JRadioButton(Bundle.HeapDumpAction_CustomLocationRadioText());
            group.add(customLocationRadio);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 15, 0, 5);
            add(customLocationRadio, c);

            String tempDir = System.getProperty("java.io.tmpdir"); // NOI18N

            if (tempDir.endsWith(File.separator)) {
                tempDir = tempDir.substring(0, tempDir.length() - File.separator.length());
            }

            customLocationField = new JTextField();
            customLocationField.setText(tempDir);
            customLocationField.setPreferredSize(new Dimension(210, customLocationField.getPreferredSize().height));
            customLocationField.setEnabled(false);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 2;
            c.weightx = 1.0d;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 15 + new JRadioButton("").getPreferredSize().width, 5, 5); // NOI18N
            add(customLocationField, c);

            customLocationButton = new JButton(Bundle.HeapDumpAction_BrowseButtonText());
            customLocationButton.setEnabled(false);
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 3;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 5, 5, 10);
            add(customLocationButton, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 4;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weighty = 1.0d;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            add(new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING)), c);

            DocumentListener documentListener = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    updateOKButton();
                }

                public void removeUpdate(DocumentEvent e) {
                    updateOKButton();
                }

                public void changedUpdate(DocumentEvent e) {
                    updateOKButton();
                }
            };

            customLocationField.getDocument().addDocumentListener(documentListener);

            defaultLocationRadio.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        updateOKButton();
                    }
                });

            customLocationRadio.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        customLocationField.setEnabled(customLocationRadio.isSelected());
                        customLocationButton.setEnabled(customLocationRadio.isSelected());
                        updateOKButton();
                    }
                });

            customLocationButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser directoryChooser = HeapDumpAction.getSnapshotDirectoryChooser();
                        directoryChooser.setCurrentDirectory(new File(getCustomDirectory()));

                        if (directoryChooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == 0) {
                            File file = directoryChooser.getSelectedFile();

                            if (file != null) {
                                customLocationField.setText(directoryChooser.getSelectedFile().getAbsolutePath());
                            }
                        }
                    }
                });

            updateOKButton();
        }

        private void updateOKButton() {
            if (defaultLocationRadio.isSelected()) {
                okButton.setEnabled(true);
            } else if (customLocationRadio.isSelected()) {
                File file = new File(getCustomDirectory());
                okButton.setEnabled(file.exists() && file.isDirectory());
            } else {
                okButton.setEnabled(false);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    private static final String SELECTING_TARGET_CANCELLED = "&*$?CANCELLED?$*&"; // NOI18N
    private static final int[] ENABLED_STATES = new int[] { Profiler.PROFILING_RUNNING };
    private static JFileChooser snapshotDirectoryChooser;

    @ActionID(id = "org.netbeans.modules.profiler.actions.HeapDumpAction", category = "Profile")
    @ActionRegistration(displayName = "#HeapDumpAction_ActionName", lazy=false)
    @ActionReference(path = "Menu/Profile", position = 600)
    public static HeapDumpAction getInstance() {
        return Singleton.INSTANCE;
    }
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ChooseHeapdumpTargetPanel heapdumpTargetSelector;

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public HeapDumpAction() {
        setIcon(Icons.getIcon(ProfilerIcons.SNAPSHOT_HEAP));
        putValue("iconBase", Icons.getResource(ProfilerIcons.SNAPSHOT_HEAP)); // NOI18N
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;

        // If you will provide context help then use:
        // return new HelpCtx(MyAction.class);
    }

    public String getName() {
        return Bundle.HeapDumpAction_ActionName();
    }

    // dumps to project and opens in HeapWalker if automatic snapshot opening is set
    public void dumpToProject() {
        takeHeapDump(false);
    }

    // asks for heap dump destination first
    public void performAction() {
        takeHeapDump(true);
    }

    protected int[] enabledStates() {
        return ENABLED_STATES;
    }

    private String getCurrentHeapDumpFilename(String targetFolder) {
        try {
            String fileName = ResultsManager.getDefault().getDefaultHeapDumpFileName(System.currentTimeMillis());
            FileObject folder = (targetFolder == null)
                                ? ProfilerStorage.getProjectFolder(NetBeansProfiler.getDefaultNB().getProfiledProject(), true)
                                : FileUtil.toFileObject(FileUtil.normalizeFile(new File(targetFolder)));

            return FileUtil.toFile(folder).getAbsolutePath() + File.separator
                   + FileUtil.findFreeFileName(folder, fileName, ResultsManager.HEAPDUMP_EXTENSION) + "."
                   + ResultsManager.HEAPDUMP_EXTENSION; // NOI18N
        } catch (IOException e) {
            return null;
        }
    }
    
    protected void updateAction() {
        boolean remote = false;
        boolean enabled = shouldBeEnabled(Profiler.getDefault());
        if (enabled) {
            String remoteHost = Profiler.getDefault().getTargetAppRunner().
                    getProfilerEngineSettings().getRemoteHost();
            if (remoteHost != null && !remoteHost.isEmpty())
                remote = true; // Not supported for remote attach
        }
        setEnabled(!remote && enabled);
        setToolTipText(remote ? Bundle.HeapDumpAction_ToolTipNoRemote() :
                                Bundle.HeapDumpAction_ToolTip());
    }
    
    private void setToolTipText(String text) {
        Object oldText = getProperty(SHORT_DESCRIPTION);
        putProperty(SHORT_DESCRIPTION, text);
        firePropertyChange(SHORT_DESCRIPTION, oldText, text);
    }

    private static JFileChooser getSnapshotDirectoryChooser() {
        if (snapshotDirectoryChooser == null) {
            snapshotDirectoryChooser = new JFileChooser();
            snapshotDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            snapshotDirectoryChooser.setMultiSelectionEnabled(false);
            snapshotDirectoryChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            snapshotDirectoryChooser.setDialogTitle(Bundle.HeapDumpAction_DirectoryDialogCaption());
        }

        return snapshotDirectoryChooser;
    }

    private ChooseHeapdumpTargetPanel getHeapdumpTargetSelector() {
        if (heapdumpTargetSelector == null) {
            heapdumpTargetSelector = new ChooseHeapdumpTargetPanel();
        }

        return heapdumpTargetSelector;
    }

    private String selectTargetDirectory() {
        // Choose heapdump destination
        ChooseHeapdumpTargetPanel targetSelector = getHeapdumpTargetSelector();
        targetSelector.updateDefaultLocation((NetBeansProfiler.getDefaultNB().getProfiledProject() != null)
                                             ? Bundle.HeapDumpAction_LocationProjectString() : 
                                               Bundle.HeapDumpAction_LocationGlobalString());

        int previousType = targetSelector.getDestinationType();
        String previousDirectory = targetSelector.getCustomDirectory();
        
        DialogDescriptor desc = new DialogDescriptor(targetSelector, Bundle.HeapDumpAction_DestinationDialogCaption(), true,
                                                     new Object[] { targetSelector.getOKButton(), DialogDescriptor.CANCEL_OPTION },
                                                     DialogDescriptor.OK_OPTION, 0, null, null);
        Object res = DialogDisplayer.getDefault().notify(desc);

        if (!res.equals(targetSelector.getOKButton())) {
            targetSelector.setDestinationType(previousType);
            targetSelector.setCustomDirectory(previousDirectory);
            return SELECTING_TARGET_CANCELLED;
        }

        // Resolve destination file
        int destinationType = targetSelector.getDestinationType();

        if (destinationType == ChooseHeapdumpTargetPanel.DESTINATION_DEFAULT) {
            targetSelector.setCustomDirectory(previousDirectory);
            return getCurrentHeapDumpFilename(null);
        } else if (destinationType == ChooseHeapdumpTargetPanel.DESTINATION_CUSTOM) {
            return getCurrentHeapDumpFilename(targetSelector.getCustomDirectory());
        }

        return null;
    }

    // askForDestination == false ? dump to project : ask for destination (project vs. external file)
    private void takeHeapDump(final boolean askForDestination) {
        ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    TargetAppRunner targetApp = Profiler.getDefault().getTargetAppRunner();

                    // not supported for JDK other than 1.6 & 1.7 & 1.5.0_12 and up
                    if (!targetApp.hasSupportedJDKForHeapDump()) {
                        ProfilerDialogs.displayWarning(Bundle.HeapDumpAction_RequiredJdkMsg());

                        return;
                    }

                    // not supported for remote attach
                    if (targetApp.getProfilingSessionStatus().remoteProfiling) {
                        ProfilerDialogs.displayWarning(Bundle.HeapDumpAction_RemoteUnsupportedMsg());

                        return;
                    }

                    try {
                        // Resolve destination file
                        String dumpFileName = askForDestination ? selectTargetDirectory() : getCurrentHeapDumpFilename(null);

                        // Selecting destination cancelled by the user
                        if (dumpFileName == SELECTING_TARGET_CANCELLED) {
                            return;
                        }

                        // Cannot resolve destination file
                        if (dumpFileName == null) {
                            ProfilerDialogs.displayError(Bundle.HeapDumpAction_WrongDestinationMsg());

                            return;
                        }

                        // Take heapdump
                        boolean heapdumpTaken = false;
                        ProgressHandle pHandle = null;

                        try {
                            pHandle = ProgressHandle.createHandle(Bundle.HeapDumpAction_DumpingHeapText());
                            pHandle.setInitialDelay(0);
                            pHandle.start();
                            heapdumpTaken = targetApp.getProfilerClient().takeHeapDump(dumpFileName);
                        } finally {
                            if (pHandle != null) {
                                pHandle.finish();
                            }
                        }

                        if (heapdumpTaken) {
                            // Refresh list of snapshots
                            File file = new File(dumpFileName);
                            FileObject folder = FileUtil.toFileObject(file.getParentFile());
                            SnapshotsWindow.instance().refreshFolder(folder, true);
//                            if (ProfilerControlPanel2.hasDefault())
//                                ProfilerControlPanel2.getDefault().refreshSnapshotsList();

                            if (askForDestination) {
                                // Heapdump saved, open in HeapWalker?
                                if (ProfilerDialogs.displayConfirmationDNSA(Bundle.HeapDumpAction_SavedDialogText(),
                                        Bundle.HeapDumpAction_SavedDialogCaption(), null, "HeapDumpAction.heapdumpSaved", false)) { //NOI18N
                                    ResultsManager.getDefault().openSnapshot(file);
                                }
                            } else {
                                if (ProfilerIDESettings.getInstance().getAutoOpenSnapshot()) {
                                    ResultsManager.getDefault().openSnapshot(file);
                                }
                            }
                        } else {
                            // Saving heapdump failed
                            ProfilerDialogs.displayError(Bundle.HeapDumpAction_DumpingFailedMsg());
                        }
                    } catch (Exception e) {
                        ProfilerDialogs.displayError(e.getMessage());
                        ProfilerLogger.log(e);
                    }
                }
            });
    }
}
