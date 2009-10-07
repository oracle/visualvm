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

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerControlPanel2;
import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.heapwalk.HeapWalker;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.DialogDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Save heap dump of profiled application to file
 *
 * @author Jiri Sedlacek
 */
public final class HeapDumpAction extends ProfilingAwareAction {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class ChooseHeapdumpTargetPanel extends JPanel {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

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

        public String getCustomDirectory() {
            return customLocationField.getText();
        }

        public int getDestinationType() {
            if (defaultLocationRadio.isSelected()) {
                return DESTINATION_DEFAULT;
            } else {
                return DESTINATION_CUSTOM;
            }
        }

        public JButton getOKButton() {
            return okButton;
        }

        public void updateDefaultLocation(String location) {
            defaultLocationRadio.setText(location);
        }

        private void initComponents() {
            okButton = new JButton(OK_BUTTON_TEXT);

            setLayout(new GridBagLayout());

            GridBagConstraints c;
            ButtonGroup group = new ButtonGroup();

            chooseDestinationLabel = new JLabel(DESTINATION_LABEL_TEXT);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(15, 10, 8, 5);
            add(chooseDestinationLabel, c);

            defaultLocationRadio = new JRadioButton(DEFAULT_LOCATION_RADIO_TEXT);
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

            customLocationRadio = new JRadioButton(CUSTOM_LOCATION_RADIO_TEXT);
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

            customLocationButton = new JButton(BROWSE_BUTTON_TEXT);
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

                        if (directoryChooser.showOpenDialog(IDEUtils.getMainWindow()) == 0) {
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
    // I18N String constants
    private static final String ACTION_NAME = NbBundle.getMessage(HeapDumpAction.class, "HeapDumpAction_ActionName"); // NOI18N
    private static final String REQUIRED_JDK_MSG = NbBundle.getMessage(HeapDumpAction.class, "HeapDumpAction_RequiredJdkMsg"); // NOI18N
    private static final String REMOTE_UNSUPPORTED_MSG = NbBundle.getMessage(HeapDumpAction.class,
                                                                             "HeapDumpAction_RemoteUnsupportedMsg"); // NOI18N
    private static final String WRONG_DESTINATION_MSG = NbBundle.getMessage(HeapDumpAction.class,
                                                                            "HeapDumpAction_WrongDestinationMsg"); // NOI18N
    private static final String DUMPING_HEAP_TEXT = NbBundle.getMessage(HeapDumpAction.class, "HeapDumpAction_DumpingHeapText"); // NOI18N
    private static final String SAVED_DIALOG_CAPTION = NbBundle.getMessage(HeapDumpAction.class,
                                                                           "HeapDumpAction_SavedDialogCaption"); // NOI18N
    private static final String SAVED_DIALOG_TEXT = NbBundle.getMessage(HeapDumpAction.class, "HeapDumpAction_SavedDialogText"); // NOI18N
    private static final String DUMPING_FAILED_MSG = NbBundle.getMessage(HeapDumpAction.class, "HeapDumpAction_DumpingFailedMsg"); // NOI18N
    private static final String DESTINATION_DIALOG_CAPTION = NbBundle.getMessage(HeapDumpAction.class,
                                                                                 "HeapDumpAction_DestinationDialogCaption"); // NOI18N
    private static final String LOCATION_PROJECT_STRING = NbBundle.getMessage(HeapDumpAction.class,
                                                                              "HeapDumpAction_LocationProjectString"); // NOI18N
    private static final String LOCATION_GLOBAL_STRING = NbBundle.getMessage(HeapDumpAction.class,
                                                                             "HeapDumpAction_LocationGlobalString"); // NOI18N
    private static final String DIRECTORY_DIALOG_CAPTION = NbBundle.getMessage(HeapDumpAction.class,
                                                                               "HeapDumpAction_DirectoryDialogCaption"); // NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(HeapDumpAction.class, "HeapDumpAction_OkButtonText"); // NOI18N
    private static final String DESTINATION_LABEL_TEXT = NbBundle.getMessage(HeapDumpAction.class,
                                                                             "HeapDumpAction_DestinationLabelText"); // NOI18N
    private static final String DEFAULT_LOCATION_RADIO_TEXT = NbBundle.getMessage(HeapDumpAction.class,
                                                                                  "HeapDumpAction_DefaultLocationRadioText"); // NOI18N
    private static final String CUSTOM_LOCATION_RADIO_TEXT = NbBundle.getMessage(HeapDumpAction.class,
                                                                                 "HeapDumpAction_CustomLocationRadioText"); // NOI18N
    private static final String BROWSE_BUTTON_TEXT = NbBundle.getMessage(HeapDumpAction.class, "HeapDumpAction_BrowseButtonText"); // NOI18N
                                                                                                                                   // -----
    public static final String TAKEN_HEAPDUMP_PREFIX = "heapdump-"; // NOI18N // should differ from generated OOME heapdumps not to be detected as OOME
    private static final String SELECTING_TARGET_CANCELLED = "&*$?CANCELLED?$*&"; // NOI18N
    private static final int[] ENABLED_STATES = new int[] { Profiler.PROFILING_RUNNING };
    private static JFileChooser snapshotDirectoryChooser;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ChooseHeapdumpTargetPanel heapdumpTargetSelector;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;

        // If you will provide context help then use:
        // return new HelpCtx(MyAction.class);
    }

    public String getName() {
        return ACTION_NAME;
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

    protected String iconResource() {
        return "org/netbeans/modules/profiler/actions/resources/dumpHeap.png"; //NOI18N
    }

    private String getCurrentHeapDumpFilename(String targetFolder) {
        try {
            String fileName = TAKEN_HEAPDUMP_PREFIX + System.currentTimeMillis();
            FileObject folder = (targetFolder == null)
                                ? IDEUtils.getProjectSettingsFolder(NetBeansProfiler.getDefaultNB().getProfiledProject(), true)
                                : FileUtil.toFileObject(FileUtil.normalizeFile(new File(targetFolder)));

            return FileUtil.toFile(folder).getAbsolutePath() + File.separator
                   + FileUtil.findFreeFileName(folder, fileName, ResultsManager.HEAPDUMP_EXTENSION) + "."
                   + ResultsManager.HEAPDUMP_EXTENSION; // NOI18N
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isHeapDumpSupported() {
        // not supported for JDK other than 1.6 & 1.7 & 1.5.0_12 and up
        TargetAppRunner targetApp = Profiler.getDefault().getTargetAppRunner();
        String jdkVersion = targetApp.getProfilerEngineSettings().getTargetJDKVersionString();

        if (CommonConstants.JDK_16_STRING.equals(jdkVersion) || CommonConstants.JDK_17_STRING.equals(jdkVersion)) {
            return true;
        }

        if (CommonConstants.JDK_15_STRING.equals(jdkVersion)) {
            String fullJDKString = targetApp.getProfilingSessionStatus().fullTargetJDKVersionString;
            int minorNumber = Platform.getJDKMinorNumber(fullJDKString);
            
            if (minorNumber >= 12) {
                return true;
            }
        }
        return false;
    }

    private static JFileChooser getSnapshotDirectoryChooser() {
        if (snapshotDirectoryChooser == null) {
            snapshotDirectoryChooser = new JFileChooser();
            snapshotDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            snapshotDirectoryChooser.setMultiSelectionEnabled(false);
            snapshotDirectoryChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            snapshotDirectoryChooser.setDialogTitle(DIRECTORY_DIALOG_CAPTION);
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
                                             ? LOCATION_PROJECT_STRING : LOCATION_GLOBAL_STRING);

        DialogDescriptor desc = new DialogDescriptor(targetSelector, DESTINATION_DIALOG_CAPTION, true,
                                                     new Object[] { targetSelector.getOKButton(), DialogDescriptor.CANCEL_OPTION },
                                                     DialogDescriptor.OK_OPTION, 0, null, null);
        Object res = ProfilerDialogs.notify(desc);

        if (!res.equals(targetSelector.getOKButton())) {
            return SELECTING_TARGET_CANCELLED;
        }

        // Resolve destination file
        int destinationType = targetSelector.getDestinationType();

        if (destinationType == ChooseHeapdumpTargetPanel.DESTINATION_DEFAULT) {
            return getCurrentHeapDumpFilename(null);
        } else if (destinationType == ChooseHeapdumpTargetPanel.DESTINATION_CUSTOM) {
            return getCurrentHeapDumpFilename(targetSelector.getCustomDirectory());
        }

        return null;
    }

    // askForDestination == false ? dump to project : ask for destination (project vs. external file)
    private void takeHeapDump(final boolean askForDestination) {
        IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    NetBeansProfiler nbProfiler = NetBeansProfiler.getDefaultNB();

                    TargetAppRunner targetApp = nbProfiler.getTargetAppRunner();

                    // not supported for JDK other than 1.6 & 1.7 & 1.5.0_12 and up
                    if (!targetApp.hasSupportedJDKForHeapDump()) {
                        nbProfiler.displayWarning(REQUIRED_JDK_MSG);

                        return;
                    }

                    // not supported for remote attach
                    if (targetApp.getProfilingSessionStatus().remoteProfiling) {
                        nbProfiler.displayWarning(REMOTE_UNSUPPORTED_MSG);

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
                            nbProfiler.displayError(WRONG_DESTINATION_MSG);

                            return;
                        }

                        // Take heapdump
                        boolean heapdumpTaken = false;
                        ProgressHandle pHandle = null;

                        try {
                            pHandle = ProgressHandleFactory.createHandle(DUMPING_HEAP_TEXT);
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
                            ProfilerControlPanel2.getDefault().refreshSnapshotsList();

                            if (askForDestination) {
                                // Heapdump saved, open in HeapWalker?
                                ProfilerDialogs.DNSAConfirmation dnsa = new ProfilerDialogs.DNSAConfirmation("HeapDumpAction.heapdumpSaved", //NOI18N
                                                                                                             SAVED_DIALOG_TEXT,
                                                                                                             SAVED_DIALOG_CAPTION,
                                                                                                             ProfilerDialogs.DNSAConfirmation.YES_NO_OPTION);
                                dnsa.setDNSADefault(false);

                                if (ProfilerDialogs.notify(dnsa).equals(ProfilerDialogs.DNSAConfirmation.YES_OPTION)) {
                                    HeapWalkerManager.getDefault().openHeapWalker(new File(dumpFileName));
                                }
                            } else {
                                if (ProfilerIDESettings.getInstance().getAutoOpenSnapshot()) {
                                    HeapWalkerManager.getDefault().openHeapWalker(new File(dumpFileName));
                                }
                            }
                        } else {
                            // Saving heapdump failed
                            nbProfiler.displayError(DUMPING_FAILED_MSG);
                        }
                    } catch (Exception e) {
                        nbProfiler.displayError(e.getMessage());
                        ProfilerLogger.log(e);
                    }
                }
            });
    }
}
