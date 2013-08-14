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

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.*;
import org.openide.DialogDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

@NbBundle.Messages({
    "CompareSnapshotsAction_ActionName=&Compare Snapshots...",
    "CompareSnapshotsAction_ActionDescr=Computes the difference between two comparable snapshots",
    "CompareSnapshotsAction_SelectSnapshotDialogCaption=Select Snapshot to Compare",
    "CompareSnapshotsAction_SelectSnapshotsDialogCaption=Select Snapshots to Compare",
    "CompareSnapshotsAction_OpenChooserCaption=Open Snapshot",
    "CompareSnapshotsAction_OpenChooserFilter=Profiler Snapshot File (*.{0})",
    "CompareSnapshotsAction_NoComparableSnapshotsFoundMsg=<No comparable snapshots found>",
    "CompareSnapshotsAction_ComparingSameSnapshotsMsg=The snapshot cannot be compared to itself.",
    "CompareSnapshotsAction_DifferentSnapshotsTypeMsg=Snapshots must be of same type.",
    "CompareSnapshotsAction_OnlyMemorySnapshotsMsg=Only memory snapshots can be compared!",
    "CompareSnapshotsAction_DifferentObjectsCountsMsg=\"Track every N object allocations\" values don't match!",
    "CompareSnapshotsAction_InvalidFileMsg=Invalid snapshot file",
    "CompareSnapshotsAction_InvalidFilesMsg=Invalid snapshot file(s)",
    "CompareSnapshotsAction_EnterFileMsg=Enter snapshot file",
    "CompareSnapshotsAction_OkButtonText=OK",
    "CompareSnapshotsAction_SelectSnapshotString=<html><b><nobr>Select snapshot to compare:</nobr></b></html>",
    "CompareSnapshotsAction_SelectSnapshotsString=Specify snapshots you want to compare",
    "CompareSnapshotsAction_FromProjectRadioText=From &project:",
    "CompareSnapshotsAction_OnlyComparableListedString=Note: only comparable snapshots are listed",
    "CompareSnapshotsAction_FromFileRadioText=From &file:",
    "CompareSnapshotsAction_FromCurrentLocationRadioText=From &current location:",
    "CompareSnapshotsAction_BrowseButtonText=&Browse...",
    "CompareSnapshotsAction_Browse2ButtonText=B&rowse...",
    "CompareSnapshotsAction_BrowseButtonAccessDescr=Select snapshot file",
    "CompareSnapshotsAction_Snapshot1String=Snapshot &1:",
    "CompareSnapshotsAction_Snapshot2String=Snapshot &2:",
    "CompareSnapshotsAction_SnapshotAccessDescr=Selected snapshot file",
    "CompareSnapshotsAction_SnapshotsListAccessDescr=List of comparable snapshots in current project"
})
@ActionID(id = "org.netbeans.modules.profiler.actions.CompareSnapshotsAction", category = "Profile")
@ActionRegistration(iconInMenu = true, displayName = "#CompareSnapshotsAction_ActionName", iconBase = "org/netbeans/modules/profiler/impl/icons/compareSnapshots.png")
@ActionReference(path = "Menu/Profile", position = 1600, separatorAfter=1700)
public class CompareSnapshotsAction extends AbstractAction {
    //~ Static fields/initializers -------------------------------------------------------------------------------------------
        private static final HelpCtx EXTERNAL_SNAPSHOT_HELP_CTX = new HelpCtx("SelectExternalSnapshotsPanel.HelpCtx"); // NOI18N
        private static final HelpCtx SECOND_SNAPSHOT_HELP_CTX = new HelpCtx("SelectSecondSnapshotPanel.HelpCtx"); // NOI18N

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class SelectExternalSnapshotsPanel extends JPanel implements HelpCtx.Provider {
            
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JButton okButton;
        private JButton snapshot1Button;
        private JButton snapshot2Button;
        private JLabel hintLabel;
        private JLabel snapshot1Label;
        private JLabel snapshot2Label;
        private JTextField snapshot1Field;
        private JTextField snapshot2Field;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public SelectExternalSnapshotsPanel() {
            initComponents();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public HelpCtx getHelpCtx() {
            return EXTERNAL_SNAPSHOT_HELP_CTX;
        }        
        
        public JButton getOKButton() {
            return okButton;
        }

        public String getSnapshot1Filename() {
            return snapshot1Field.getText();
        }

        public String getSnapshot2Filename() {
            return snapshot2Field.getText();
        }

        private void initComponents() {
            okButton = new JButton(Bundle.CompareSnapshotsAction_OkButtonText());

            setLayout(new GridBagLayout());

            GridBagConstraints c;

            snapshot1Label = new JLabel();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot1Label, Bundle.CompareSnapshotsAction_Snapshot1String());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(15, 10, 5, 5);
            add(snapshot1Label, c);

            snapshot1Field = new JTextField();
            snapshot1Field.setPreferredSize(new Dimension(250, snapshot1Field.getPreferredSize().height));
            snapshot1Label.setLabelFor(snapshot1Field);
            snapshot1Field.getAccessibleContext().setAccessibleDescription(Bundle.CompareSnapshotsAction_SnapshotAccessDescr());
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1.0d;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(15, 5, 5, 5);
            add(snapshot1Field, c);

            snapshot1Button = new JButton();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot1Button, Bundle.CompareSnapshotsAction_BrowseButtonText());
            snapshot1Button.getAccessibleContext().setAccessibleDescription(Bundle.CompareSnapshotsAction_BrowseButtonAccessDescr());
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(15, 5, 5, 10);
            add(snapshot1Button, c);

            snapshot2Label = new JLabel();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot2Label, Bundle.CompareSnapshotsAction_Snapshot2String());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(5, 10, 5, 5);
            add(snapshot2Label, c);

            snapshot2Field = new JTextField();
            snapshot2Field.setPreferredSize(new Dimension(250, snapshot2Field.getPreferredSize().height));
            snapshot2Label.setLabelFor(snapshot2Field);
            snapshot2Field.getAccessibleContext().setAccessibleDescription(Bundle.CompareSnapshotsAction_SnapshotAccessDescr());
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 5, 5, 5);
            add(snapshot2Field, c);

            snapshot2Button = new JButton();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot2Button, Bundle.CompareSnapshotsAction_Browse2ButtonText());
            snapshot2Button.getAccessibleContext().setAccessibleDescription(Bundle.CompareSnapshotsAction_BrowseButtonAccessDescr());
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(5, 5, 5, 10);
            add(snapshot2Button, c);

            hintLabel = new JLabel(" "); // NOI18N
            hintLabel.setForeground(Color.darkGray);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(5, 5, 5, 10);
            add(hintLabel, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 3;
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

            snapshot1Field.getDocument().addDocumentListener(documentListener);
            snapshot2Field.getDocument().addDocumentListener(documentListener);

            snapshot1Button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser snapshotChooser = CompareSnapshotsAction.getSnapshotFileChooser();
                        snapshotChooser.setCurrentDirectory(new File(getSnapshot1Filename()));

                        if (snapshotChooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == 0) {
                            File file = snapshotChooser.getSelectedFile();

                            if (file != null) {
                                snapshot1Field.setText(file.getAbsolutePath());
                            }
                        }
                    }
                });

            snapshot2Button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser snapshotChooser = CompareSnapshotsAction.getSnapshotFileChooser();
                        snapshotChooser.setCurrentDirectory(new File((getSnapshot2Filename().length() == 0)
                                                                     ? getSnapshot1Filename() : getSnapshot2Filename()));

                        if (snapshotChooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == 0) {
                            File file = snapshotChooser.getSelectedFile();

                            if (file != null) {
                                snapshot2Field.setText(file.getAbsolutePath());
                            }
                        }
                    }
                });

            updateOKButton();
        }

        private void updateOKButton() {
            String s1f = getSnapshot1Filename();
            String s2f = getSnapshot2Filename();

            if ((s1f.length() > 0) && (s2f.length() > 0)) {
                // filenames not empty string
                final File s1 = new File(s1f);
                final File s2 = new File(s2f);

                if (s1.exists() && s1.isFile() && s2.exists() && s2.isFile()) {
                    // files exist
                    if (s1.equals(s2)) {
                        // comparing the same snapshot
                        hintLabel.setText(Bundle.CompareSnapshotsAction_ComparingSameSnapshotsMsg());
                        okButton.setEnabled(false);
                    } else {
                        // comparing different snapshots
                        new NBSwingWorker(true) {
                            private String hintStr;
                            private boolean enabledOk;
                            
                            @Override
                            protected void doInBackground() {
                                FileObject s1fo = FileUtil.toFileObject(s1);
                                FileObject s2fo = FileUtil.toFileObject(s2);
                                int s1t = ResultsManager.getDefault().getSnapshotType(s1fo);
                                int s2t = ResultsManager.getDefault().getSnapshotType(s2fo);

                                if (s1t != s2t) {
                                    // snapshot types don't match
                                    hintStr = Bundle.CompareSnapshotsAction_DifferentSnapshotsTypeMsg();
                                    enabledOk = false;
//                                } else if ((s1t != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS)
//                                       && (s1t != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS)) {
//                                    // not a memory snapshot
//                                    hintStr = Bundle.CompareSnapshotsAction_OnlyMemorySnapshotsMsg();
//                                    enabledOk = false;
                                } else if (ResultsManager.getDefault().getSnapshotSettings(s1fo).getAllocTrackEvery() != ResultsManager.getDefault()
                                                                                                                                   .getSnapshotSettings(s2fo)
                                                                                                                                   .getAllocTrackEvery()) {
                                    // memory snapshots have different track every N objects
                                    hintStr = Bundle.CompareSnapshotsAction_DifferentObjectsCountsMsg();
                                    enabledOk = false;
                                } else {
                                    // comparable snapshots (from the hint point of view!)
                                    hintStr = " "; // NOI18N
                                    enabledOk = areComparableSnapshots(s1fo, s2fo);
                                }
                            }

                            @Override
                            protected void done() {
                                hintLabel.setText(hintStr);
                                okButton.setEnabled(enabledOk);
                            }
                        }.execute();
                    }
                } else {
                    // files don't exist
                    hintLabel.setText(Bundle.CompareSnapshotsAction_InvalidFilesMsg());
                    okButton.setEnabled(false);
                }
            } else {
                // filenames are empty string
                hintLabel.setText(Bundle.CompareSnapshotsAction_SelectSnapshotsString());
                okButton.setEnabled(false);
            }
        }
    }

    private class SelectSecondSnapshotPanel extends JPanel implements HelpCtx.Provider {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JButton externalFileButton;
        private JButton okButton;
        private JLabel externalFileHintLabel;
        private JLabel projectSnapshotsHintLabel;
        private JLabel selectSnapshotLabel;
        private JList projectSnapshotsList;
        private JRadioButton fromFileRadio;
        private JRadioButton fromProjectRadio;
        private JTextField externalFileField;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public SelectSecondSnapshotPanel() {
            initComponents();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public HelpCtx getHelpCtx() {
            return SECOND_SNAPSHOT_HELP_CTX;
        }        

        public JButton getOKButton() {
            return okButton;
        }

        public Object getSnapshot() {
            if (fromProjectRadio.isSelected()) {
                Object selectedItem = projectSnapshotsList.getSelectedValue();

                if ((selectedItem == null) || selectedItem instanceof String) {
                    return null;
                }

                return selectedItem;
            } else if (fromFileRadio.isSelected()) {
                String sf = externalFileField.getText();

                if (sf.length() > 0) {
                    File s = new File(sf);

                    if (s.exists() && s.isFile()) {
                        return FileUtil.toFileObject(s);
                    }
                }

                return null;
            } else {
                return null;
            }
        }

        public void populateSnapshotsList() {
            // Get list model
            DefaultListModel listModel = (DefaultListModel) projectSnapshotsList.getModel();

            // Clear the list
            listModel.removeAllElements();

            // Add opened but not-yet-saved snapshots
            // TODO: check that this works correctly in VisualVM!
            LoadedSnapshot[] loadedSnapshots = ResultsManager.getDefault().getLoadedSnapshots();

            for (int i = 0; i < loadedSnapshots.length; i++) {
                if ((loadedSnapshots[i] != snapshot) && (loadedSnapshots[i].getFile() == null)
                        && areComparableSnapshots(snapshot, loadedSnapshots[i])) {
                    listModel.addElement(loadedSnapshots[i]);
                }
            }

            // Add saved snapshots
            final Lookup.Provider project = snapshot.getProject();
            File snapFile = snapshot.getFile();
            final File snapshotDir = snapFile != null ? snapFile.getParentFile() : null;
            FileObject[] snapshotsOnDisk = ResultsManager.getDefault().listSavedSnapshots(project, snapshotDir);
            FileObject snapshotFile = (snapFile != null) ? FileUtil.toFileObject(snapFile) : null;
            
            for (int i = 0; i < snapshotsOnDisk.length; i++) {
                if (((snapshotFile == null) || !snapshotsOnDisk[i].equals(snapshotFile))
                        && areComparableSnapshots(snapshot, snapshotsOnDisk[i])) {
                    listModel.addElement(snapshotsOnDisk[i]);
                }
            }

            if (listModel.getSize() == 0) {
                listModel.addElement(Bundle.CompareSnapshotsAction_NoComparableSnapshotsFoundMsg());
                fromFileRadio.setSelected(true);
                externalFileField.addHierarchyListener(new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && externalFileField.isShowing()) {
                            externalFileField.removeHierarchyListener(this);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    externalFileField.requestFocusInWindow();
                                }
                            });
                        }
                    }
                });
            } else {
                projectSnapshotsList.setSelectedIndex(0);
                projectSnapshotsList.addHierarchyListener(new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && externalFileField.isShowing()) {
                            projectSnapshotsList.removeHierarchyListener(this);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    projectSnapshotsList.requestFocusInWindow();
                                }
                            });
                        }
                    }
                });
            }
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (project != null) {
                        org.openide.awt.Mnemonics.setLocalizedText(fromProjectRadio, Bundle.CompareSnapshotsAction_FromProjectRadioText());
                        fromProjectRadio.setToolTipText(null);
                    } else {
                        org.openide.awt.Mnemonics.setLocalizedText(fromProjectRadio, Bundle.CompareSnapshotsAction_FromCurrentLocationRadioText());
                        fromProjectRadio.setToolTipText(snapshotDir != null ?
                                snapshotDir.getAbsolutePath() : null);
                    }
                }
            });
        }

        private void initComponents() {
            okButton = new JButton(Bundle.CompareSnapshotsAction_OkButtonText());

            setLayout(new GridBagLayout());

            GridBagConstraints c;
            ButtonGroup group = new ButtonGroup();

            selectSnapshotLabel = new JLabel(Bundle.CompareSnapshotsAction_SelectSnapshotString());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(15, 10, 8, 10);
            add(selectSnapshotLabel, c);

            fromProjectRadio = new JRadioButton();
            org.openide.awt.Mnemonics.setLocalizedText(fromProjectRadio, Bundle.CompareSnapshotsAction_FromProjectRadioText());
            group.add(fromProjectRadio);
            fromProjectRadio.getAccessibleContext().setAccessibleDescription(
                Bundle.CompareSnapshotsAction_SelectSnapshotString() + 
                Bundle.CompareSnapshotsAction_FromProjectRadioText());
            fromProjectRadio.setSelected(true);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 15, 0, 10);
            add(fromProjectRadio, c);

            projectSnapshotsList = new JList(new DefaultListModel());
            projectSnapshotsList.getAccessibleContext().setAccessibleName(Bundle.CompareSnapshotsAction_SnapshotsListAccessDescr());
            projectSnapshotsList.setVisibleRowCount(5);
            projectSnapshotsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JScrollPane projectSnapshotsListScroll = new JScrollPane(projectSnapshotsList,
                                                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            projectSnapshotsListScroll.setPreferredSize(new Dimension(1, projectSnapshotsListScroll.getPreferredSize().height));
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.weighty = 1d;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, 15 + new JRadioButton("").getPreferredSize().width, 5, 10); // NOI18N
            add(projectSnapshotsListScroll, c);

            projectSnapshotsHintLabel = new JLabel(Bundle.CompareSnapshotsAction_OnlyComparableListedString());
            projectSnapshotsHintLabel.setForeground(Color.darkGray);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 15 + new JRadioButton("").getPreferredSize().width, 5, 10); // NOI18N
            add(projectSnapshotsHintLabel, c);

            fromFileRadio = new JRadioButton(Bundle.CompareSnapshotsAction_FromFileRadioText());
            org.openide.awt.Mnemonics.setLocalizedText(fromFileRadio, Bundle.CompareSnapshotsAction_FromFileRadioText());
            group.add(fromFileRadio);
            fromProjectRadio.getAccessibleContext().setAccessibleDescription(
                Bundle.CompareSnapshotsAction_SelectSnapshotString() + 
                Bundle.CompareSnapshotsAction_FromFileRadioText());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 4;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(5, 15, 0, 10);
            add(fromFileRadio, c);

            externalFileField = new JTextField();
            externalFileField.setPreferredSize(new Dimension(250, externalFileField.getPreferredSize().height));
            externalFileField.setEnabled(false);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 5;
            c.weightx = 1d;
            c.gridwidth = 2;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 15 + new JRadioButton("").getPreferredSize().width, 5, 5); // NOI18N
            add(externalFileField, c);

            externalFileButton = new JButton();
            org.openide.awt.Mnemonics.setLocalizedText(externalFileButton, Bundle.CompareSnapshotsAction_BrowseButtonText());
            externalFileButton.getAccessibleContext().setAccessibleDescription(Bundle.CompareSnapshotsAction_BrowseButtonAccessDescr());
            externalFileButton.setEnabled(false);
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 5;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 5, 5, 10);
            add(externalFileButton, c);

            externalFileHintLabel = new JLabel(" "); // NOI18N
            externalFileHintLabel.setForeground(Color.darkGray);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 6;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 15 + new JRadioButton("").getPreferredSize().width, 5, 10); // NOI18N
            add(externalFileHintLabel, c);

            projectSnapshotsList.setCellRenderer(new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                                  boolean cellHasFocus) {
                        JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                        ResultsManager rm = ResultsManager.getDefault();
                        if (value instanceof LoadedSnapshot) {
                            LoadedSnapshot ls = (LoadedSnapshot) value;
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                            c.setText(rm.getSnapshotDisplayName(ls));

                            switch (ls.getType()) {
                                case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                    c.setIcon(cpuIcon);

                                    break;
                                case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                    c.setIcon(fragmentIcon);

                                    break;
                                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
                                    c.setIcon(memoryIcon);

                                    break;
                            }
                        } else if (value instanceof FileObject) {
                            FileObject fo = (FileObject) value;
                            LoadedSnapshot ls = rm.findLoadedSnapshot(FileUtil.toFile(fo));

                            if (ls != null) {
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                                c.setText(rm.getSnapshotDisplayName(ls));

                                switch (ls.getType()) {
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                        c.setIcon(cpuIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                        c.setIcon(fragmentIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
                                        c.setIcon(memoryIcon);

                                        break;
                                }
                            } else {
                                int type = rm.getSnapshotType(fo);
                                c.setText(rm.getSnapshotDisplayName(fo.getName(), type));
                                switch (type) {
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                        c.setIcon(cpuIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                        c.setIcon(fragmentIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
                                        c.setIcon(memoryIcon);

                                        break;
                                }
                            }
                        }

                        return c;
                    }
                });

            projectSnapshotsList.addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        updateOKButton();
                    }
                });

            projectSnapshotsList.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)
                                && (projectSnapshotsList.getSelectedValue() != null)) {
                            SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        okButton.doClick();
                                    }
                                });
                        }
                    }
                });

            fromProjectRadio.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        projectSnapshotsList.setEnabled(fromProjectRadio.isSelected());
                        projectSnapshotsHintLabel.setText(fromProjectRadio.isSelected() ? Bundle.CompareSnapshotsAction_OnlyComparableListedString() : " "); // NOI18N
                        updateOKButton();
                    }
                });

            fromFileRadio.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        externalFileField.setEnabled(fromFileRadio.isSelected());
                        externalFileButton.setEnabled(fromFileRadio.isSelected());

                        if (!fromFileRadio.isSelected()) {
                            externalFileHintLabel.setText(" "); // NOI18N
                        }

                        updateOKButton();
                    }
                });

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

            externalFileField.getDocument().addDocumentListener(documentListener);

            externalFileButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser snapshotChooser = CompareSnapshotsAction.getSnapshotFileChooser();
                        snapshotChooser.setCurrentDirectory(new File(externalFileField.getText()));

                        if (snapshotChooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == 0) {
                            File file = snapshotChooser.getSelectedFile();

                            if (file != null) {
                                externalFileField.setText(file.getAbsolutePath());
                            }
                        }
                    }
                });

            updateOKButton();
        }

        private void updateOKButton() {
            if (fromProjectRadio.isSelected()) {
                // Snapshot from project
                Object selectedItem = projectSnapshotsList.getSelectedValue();
                okButton.setEnabled((selectedItem != null) && !(selectedItem instanceof String));
            } else if (fromFileRadio.isSelected()) {
                // Snapshot from file
                String sf = externalFileField.getText();

                if (sf.length() > 0) {
                    // filename not empty string
                    File s = new File(sf);

                    if (s.exists() && s.isFile()) {
                        // file exists
                        if (s.equals(snapshot.getFile())) {
                            // comparing snapshot with itself
                            externalFileHintLabel.setText(Bundle.CompareSnapshotsAction_ComparingSameSnapshotsMsg());
                            okButton.setEnabled(false);
                        } else {
                            // comparing different snapshots
                            FileObject snapshot2f = FileUtil.toFileObject(s);

                            if (snapshot.getType() != ResultsManager.getDefault().getSnapshotType(snapshot2f)) {
                                // snapshot types doesn't match
                                externalFileHintLabel.setText(Bundle.CompareSnapshotsAction_DifferentSnapshotsTypeMsg());
//                            } else if ((snapshot.getType() != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS)
//                                           && (snapshot.getType() != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS)) {
//                                // TODO: remove after Compare CPU snapshots is implemented
//                                // not a memory snapshot
//                                externalFileHintLabel.setText(Bundle.CompareSnapshotsAction_OnlyMemorySnapshotsMsg());
//                                okButton.setEnabled(false);
//
//                                return;
                            } else if (snapshot.getSettings().getAllocTrackEvery() != ResultsManager.getDefault()
                                                                                                        .getSnapshotSettings(snapshot2f)
                                                                                                        .getAllocTrackEvery()) {
                                // memory snapshots have different track every N objects
                                externalFileHintLabel.setText(Bundle.CompareSnapshotsAction_DifferentObjectsCountsMsg());
                            } else {
                                // comparable snapshots (from the hint point of view!)
                                externalFileHintLabel.setText(" "); // NOI18N
                            }

                            okButton.setEnabled(areComparableSnapshots(snapshot, snapshot2f));
                        }
                    } else {
                        // file doesn't exist
                        externalFileHintLabel.setText(Bundle.CompareSnapshotsAction_InvalidFileMsg());
                        okButton.setEnabled(false);
                    }
                } else {
                    // filename is empty string
                    externalFileHintLabel.setText(Bundle.CompareSnapshotsAction_EnterFileMsg());
                    okButton.setEnabled(false);
                }
            } else {
                okButton.setEnabled(false);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Icon cpuIcon = Icons.getIcon(ProfilerIcons.CPU);
    private static final Icon fragmentIcon = Icons.getIcon(ProfilerIcons.FRAGMENT);
    private static final Icon memoryIcon = Icons.getIcon(ProfilerIcons.MEMORY);
    private static JFileChooser snapshotFileChooser;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private LoadedSnapshot snapshot;
    private SelectExternalSnapshotsPanel externalSnapshotsSelector;
    private SelectSecondSnapshotPanel secondSnapshotSelector;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CompareSnapshotsAction() {
        snapshot = null;
        putValue(Action.NAME, Bundle.CompareSnapshotsAction_ActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CompareSnapshotsAction_ActionDescr());
        putValue(Action.SMALL_ICON, Icons.getIcon(ProfilerIcons.SNAPSHOTS_COMPARE));
    }

    public CompareSnapshotsAction(LoadedSnapshot snapshot) {
        this();
        this.snapshot = snapshot;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        if (snapshot != null) {
            compareDefinedSnapshot();
        } else {
            compareExternalSnapshots();
        }
    }

    public static boolean areComparableSnapshots(LoadedSnapshot s1, LoadedSnapshot s2) {
        // compare snapshot types
        if (s1.getType() != s2.getType()) {
            return false;
        }

        // for memory snapshots compare track every n allocations
        if ((s1.getType() == LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS)
                || (s1.getType() == LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS)) {
            if (s1.getSettings().getAllocTrackEvery() != s2.getSettings().getAllocTrackEvery()) {
                return false;
            }
        }

        return true;
    }

    public static boolean areComparableSnapshots(LoadedSnapshot s1, FileObject s2f) {
        // try to find already loaded snapshot
        LoadedSnapshot s2 = ResultsManager.getDefault().findLoadedSnapshot(FileUtil.toFile(s2f));

        // compare loaded snapshots
        if (s2 != null) {
            return areComparableSnapshots(s1, s2);
        }

        // compare snapshot types
        if (s1.getType() != ResultsManager.getDefault().getSnapshotType(s2f)) {
            return false;
        }

        // for memory snapshots compare track every n allocations
        if ((s1.getType() == LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS)
                || (s1.getType() == LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS)) {
            ProfilingSettings s2settings = ResultsManager.getDefault().getSnapshotSettings(s2f);

            if (s2settings == null) {
                return false;
            }

            if (s1.getSettings().getAllocTrackEvery() != s2settings.getAllocTrackEvery()) {
                return false;
            }
        }

        return true;
    }

    public static boolean areComparableSnapshots(FileObject s1f, FileObject s2f) {
        // try to find already loaded snapshots
        LoadedSnapshot s1 = ResultsManager.getDefault().findLoadedSnapshot(FileUtil.toFile(s1f));
        LoadedSnapshot s2 = ResultsManager.getDefault().findLoadedSnapshot(FileUtil.toFile(s2f));

        // compare loaded snapshots
        if (s1 != null) {
            if (s2 != null) {
                return areComparableSnapshots(s1, s2);
            } else {
                return areComparableSnapshots(s1, s2f);
            }
        }

        // compare snapshot types
        int s1t = ResultsManager.getDefault().getSnapshotType(s1f);

        if (s1t != ResultsManager.getDefault().getSnapshotType(s2f)) {
            return false;
        }

        // for memory snapshots compare track every n allocations
        if ((s1t == LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS) || (s1t == LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS)) {
            ProfilingSettings s1s = ResultsManager.getDefault().getSnapshotSettings(s1f);
            ProfilingSettings s2s = ResultsManager.getDefault().getSnapshotSettings(s2f);

            if ((s1s == null) || (s2s == null)) {
                return false;
            }

            if (s1s.getAllocTrackEvery() != s2s.getAllocTrackEvery()) {
                return false;
            }
        }

        return true;
    }

    private static JFileChooser getSnapshotFileChooser() {
        if (snapshotFileChooser == null) {
            snapshotFileChooser = new JFileChooser();
            snapshotFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            snapshotFileChooser.setMultiSelectionEnabled(false);
            snapshotFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            snapshotFileChooser.setDialogTitle(Bundle.CompareSnapshotsAction_OpenChooserCaption());
            snapshotFileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".nps"); // NOI18N
                    }

                    public String getDescription() {
                        return Bundle.CompareSnapshotsAction_OpenChooserFilter("nps"); // NOI18N
                    }
                });
            snapshotFileChooser.setAcceptAllFileFilterUsed(false);
        }

        return snapshotFileChooser;
    }

    private SelectExternalSnapshotsPanel getExternalSnapshotsSelector() {
        if (externalSnapshotsSelector == null) {
            externalSnapshotsSelector = new SelectExternalSnapshotsPanel();
        }

        return externalSnapshotsSelector;
    }

    private SelectSecondSnapshotPanel getSecondSnapshotSelector() {
        if (secondSnapshotSelector == null) {
            secondSnapshotSelector = new SelectSecondSnapshotPanel();
        }

        return secondSnapshotSelector;
    }

    private void compareDefinedSnapshot() {
        getSecondSnapshotSelector().populateSnapshotsList();

        DialogDescriptor desc = new DialogDescriptor(getSecondSnapshotSelector(), Bundle.CompareSnapshotsAction_SelectSnapshotDialogCaption(), true,
                                                     new Object[] {
                                                         getSecondSnapshotSelector().getOKButton(), DialogDescriptor.CANCEL_OPTION
                                                     }, DialogDescriptor.OK_OPTION, 0, null, null);
        Object res = DialogDisplayer.getDefault().notify(desc);

        if (res.equals(getSecondSnapshotSelector().getOKButton())) {
            Object selectedSnapshot = getSecondSnapshotSelector().getSnapshot();

            if (selectedSnapshot instanceof LoadedSnapshot) {
                ResultsManager.getDefault().compareSnapshots(snapshot, (LoadedSnapshot) selectedSnapshot);
            } else if (selectedSnapshot instanceof FileObject) {
                if (snapshot.getFile() == null) {
                    LoadedSnapshot snapshot2 = ResultsManager.getDefault().getSnapshotFromFileObject((FileObject) selectedSnapshot);

                    if (snapshot2 != null) {
                        ResultsManager.getDefault().compareSnapshots(snapshot, snapshot2);
                    }
                } else {
                    ResultsManager.getDefault()
                                  .compareSnapshots(FileUtil.toFileObject(snapshot.getFile()), (FileObject) selectedSnapshot);
                }
            }
        }
    }

    private void compareExternalSnapshots() {
        DialogDescriptor desc = new DialogDescriptor(getExternalSnapshotsSelector(), Bundle.CompareSnapshotsAction_SelectSnapshotsDialogCaption(), true,
                                                     new Object[] {
                                                         getExternalSnapshotsSelector().getOKButton(),
                                                         DialogDescriptor.CANCEL_OPTION
                                                     }, DialogDescriptor.OK_OPTION, 0, null, null);
        Object res = DialogDisplayer.getDefault().notify(desc);

        if (res.equals(getExternalSnapshotsSelector().getOKButton())) {
            ResultsManager.getDefault()
                          .compareSnapshots(FileUtil.toFileObject(new File(getExternalSnapshotsSelector().getSnapshot1Filename())),
                                            FileUtil.toFileObject(new File(getExternalSnapshotsSelector().getSnapshot2Filename())));
        }
    }
}
