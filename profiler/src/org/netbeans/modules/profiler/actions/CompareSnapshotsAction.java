/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.*;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.DialogDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;


public class CompareSnapshotsAction extends AbstractAction {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SelectExternalSnapshotsPanel extends JPanel {
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
            okButton = new JButton(OK_BUTTON_TEXT);

            setLayout(new GridBagLayout());

            GridBagConstraints c;

            snapshot1Label = new JLabel();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot1Label, SNAPSHOT1_STRING);
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
            snapshot1Field.getAccessibleContext().setAccessibleDescription(SNAPSHOT_ACCESS_DESCR);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1.0d;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(15, 5, 5, 5);
            add(snapshot1Field, c);

            snapshot1Button = new JButton();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot1Button, BROWSE_BUTTON_TEXT);
            snapshot1Button.getAccessibleContext().setAccessibleDescription(BROWSE_BUTTON_ACCESS_DESCR);
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(15, 5, 5, 10);
            add(snapshot1Button, c);

            snapshot2Label = new JLabel();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot2Label, SNAPSHOT2_STRING);
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
            snapshot2Field.getAccessibleContext().setAccessibleDescription(SNAPSHOT_ACCESS_DESCR);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 5, 5, 5);
            add(snapshot2Field, c);

            snapshot2Button = new JButton();
            org.openide.awt.Mnemonics.setLocalizedText(snapshot2Button, BROWSE2_BUTTON_TEXT);
            snapshot2Button.getAccessibleContext().setAccessibleDescription(BROWSE_BUTTON_ACCESS_DESCR);
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

                        if (snapshotChooser.showOpenDialog(IDEUtils.getMainWindow()) == 0) {
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

                        if (snapshotChooser.showOpenDialog(IDEUtils.getMainWindow()) == 0) {
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
                File s1 = new File(s1f);
                File s2 = new File(s2f);

                if (s1.exists() && s1.isFile() && s2.exists() && s2.isFile()) {
                    // files exist
                    if (s1.equals(s2)) {
                        // comparing the same snapshot
                        hintLabel.setText(COMPARING_SAME_SNAPSHOTS_MSG);
                        okButton.setEnabled(false);
                    } else {
                        // comparing different snapshots
                        FileObject s1fo = FileUtil.toFileObject(s1);
                        FileObject s2fo = FileUtil.toFileObject(s2);
                        int s1t = ResultsManager.getDefault().getSnapshotType(s1fo);
                        int s2t = ResultsManager.getDefault().getSnapshotType(s2fo);

                        if (s1t != s2t) {
                            // snapshot types don't match
                            hintLabel.setText(DIFFERENT_SNAPSHOTS_TYPE_MSG);
                            okButton.setEnabled(false);
                        } else if ((s1t != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS)
                                       && (s1t != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS)) {
                            // TODO: remove after Compare CPU snapshots is implemented
                            // not a memory snapshot
                            hintLabel.setText(ONLY_MEMORY_SNAPSHOTS_MSG);
                            okButton.setEnabled(false);

                            return;
                        } else if (ResultsManager.getDefault().getSnapshotSettings(s1fo).getAllocTrackEvery() != ResultsManager.getDefault()
                                                                                                                                   .getSnapshotSettings(s2fo)
                                                                                                                                   .getAllocTrackEvery()) {
                            // memory snapshots have different track every N objects
                            hintLabel.setText(DIFFERENT_OBJECTS_COUNTS_MSG);
                            okButton.setEnabled(false);
                        } else {
                            // comparable snapshots (from the hint point of view!)
                            hintLabel.setText(" "); // NOI18N
                        }

                        okButton.setEnabled(areComparableSnapshots(s1fo, s2fo));
                    }
                } else {
                    // files don't exist
                    hintLabel.setText(INVALID_FILES_MSG);
                    okButton.setEnabled(false);
                }
            } else {
                // filenames are empty string
                hintLabel.setText(SELECT_SNAPSHOTS_STRING);
                okButton.setEnabled(false);
            }
        }
    }

    private class SelectSecondSnapshotPanel extends JPanel {
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
            LoadedSnapshot[] loadedSnapshots = ResultsManager.getDefault().getLoadedSnapshots();

            for (int i = 0; i < loadedSnapshots.length; i++) {
                if ((loadedSnapshots[i] != snapshot) && (loadedSnapshots[i].getFile() == null)
                        && areComparableSnapshots(snapshot, loadedSnapshots[i])) {
                    listModel.addElement(loadedSnapshots[i]);
                }
            }

            // Add saved snapshots
            FileObject[] snapshotsOnDisk = ResultsManager.getDefault().listSavedSnapshots(snapshot.getProject());
            FileObject snapshotFile = (snapshot.getFile() != null) ? FileUtil.toFileObject(snapshot.getFile()) : null;

            for (int i = 0; i < snapshotsOnDisk.length; i++) {
                if (((snapshotFile == null) || !snapshotsOnDisk[i].equals(snapshotFile))
                        && areComparableSnapshots(snapshot, snapshotsOnDisk[i])) {
                    listModel.addElement(snapshotsOnDisk[i]);
                }
            }

            if (listModel.getSize() == 0) {
                listModel.addElement(NO_COMPARABLE_SNAPSHOTS_FOUND_MSG);
            }
        }

        private void initComponents() {
            okButton = new JButton(OK_BUTTON_TEXT);

            setLayout(new GridBagLayout());

            GridBagConstraints c;
            ButtonGroup group = new ButtonGroup();

            selectSnapshotLabel = new JLabel(SELECT_SNAPSHOT_STRING);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(15, 10, 8, 10);
            add(selectSnapshotLabel, c);

            fromProjectRadio = new JRadioButton();
            org.openide.awt.Mnemonics.setLocalizedText(fromProjectRadio, FROM_PROJECT_RADIO_TEXT);
            group.add(fromProjectRadio);
            fromProjectRadio.getAccessibleContext().setAccessibleDescription(SELECT_SNAPSHOT_STRING + FROM_PROJECT_RADIO_TEXT);
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
            projectSnapshotsList.getAccessibleContext().setAccessibleName(SNAPSHOTS_LIST_ACCESS_DESCR);
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

            projectSnapshotsHintLabel = new JLabel(ONLY_COMPARABLE_LISTED_STRING);
            projectSnapshotsHintLabel.setForeground(Color.darkGray);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 15 + new JRadioButton("").getPreferredSize().width, 5, 10); // NOI18N
            add(projectSnapshotsHintLabel, c);

            fromFileRadio = new JRadioButton(FROM_FILE_RADIO_TEXT);
            org.openide.awt.Mnemonics.setLocalizedText(fromFileRadio, FROM_FILE_RADIO_TEXT);
            group.add(fromFileRadio);
            fromProjectRadio.getAccessibleContext().setAccessibleDescription(SELECT_SNAPSHOT_STRING + FROM_FILE_RADIO_TEXT);
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
            org.openide.awt.Mnemonics.setLocalizedText(externalFileButton, BROWSE_BUTTON_TEXT);
            externalFileButton.getAccessibleContext().setAccessibleDescription(BROWSE_BUTTON_ACCESS_DESCR);
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

                        if (value instanceof LoadedSnapshot) {
                            LoadedSnapshot ls = (LoadedSnapshot) value;
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                            c.setText(StringUtils.formatUserDate(new Date(ls.getSnapshot().getTimeTaken())));

                            switch (ls.getType()) {
                                case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                    c.setIcon(cpuIcon);

                                    break;
                                case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                    c.setIcon(fragmentIcon);

                                    break;
                                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                                    c.setIcon(memoryIcon);

                                    break;
                            }
                        } else if (value instanceof FileObject) {
                            FileObject fo = (FileObject) value;
                            LoadedSnapshot ls = ResultsManager.getDefault().findLoadedSnapshot(FileUtil.toFile(fo));

                            if (ls != null) {
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                                c.setText(StringUtils.formatUserDate(new Date(ls.getSnapshot().getTimeTaken())));

                                switch (ls.getType()) {
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                        c.setIcon(cpuIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                        c.setIcon(fragmentIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                                        c.setIcon(memoryIcon);

                                        break;
                                }
                            } else {
                                String fileName = fo.getName();

                                if (fileName.startsWith("snapshot-")) { // NOI18N

                                    String time = fileName.substring("snapshot-".length(), fileName.length()); // NOI18N

                                    try {
                                        long timeStamp = Long.parseLong(time);
                                        c.setText(StringUtils.formatUserDate(new Date(timeStamp)));
                                    } catch (NumberFormatException e) {
                                        // file name is probably customized
                                        c.setText(fileName);
                                    }
                                } else {
                                    c.setText(fileName);
                                }

                                int type = ResultsManager.getDefault().getSnapshotType(fo);

                                switch (type) {
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                        c.setIcon(cpuIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                        c.setIcon(fragmentIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
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
                        projectSnapshotsHintLabel.setText(fromProjectRadio.isSelected() ? ONLY_COMPARABLE_LISTED_STRING : " "); // NOI18N
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

                        if (snapshotChooser.showOpenDialog(IDEUtils.getMainWindow()) == 0) {
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
                            externalFileHintLabel.setText(COMPARING_SAME_SNAPSHOTS_MSG);
                            okButton.setEnabled(false);
                        } else {
                            // comparing different snapshots
                            FileObject snapshot2f = FileUtil.toFileObject(s);

                            if (snapshot.getType() != ResultsManager.getDefault().getSnapshotType(snapshot2f)) {
                                // snapshot types doesn't match
                                externalFileHintLabel.setText(DIFFERENT_SNAPSHOT_TYPE_MSG);
                            } else if ((snapshot.getType() != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS)
                                           && (snapshot.getType() != LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS)) {
                                // TODO: remove after Compare CPU snapshots is implemented
                                // not a memory snapshot
                                externalFileHintLabel.setText(ONLY_MEMORY_SNAPSHOTS_MSG);
                                okButton.setEnabled(false);

                                return;
                            } else if (snapshot.getSettings().getAllocTrackEvery() != ResultsManager.getDefault()
                                                                                                        .getSnapshotSettings(snapshot2f)
                                                                                                        .getAllocTrackEvery()) {
                                // memory snapshots have different track every N objects
                                externalFileHintLabel.setText(DIFFERENT_OBJECTS_COUNT_MSG);
                            } else {
                                // comparable snapshots (from the hint point of view!)
                                externalFileHintLabel.setText(" "); // NOI18N
                            }

                            okButton.setEnabled(areComparableSnapshots(snapshot, snapshot2f));
                        }
                    } else {
                        // file doesn't exist
                        externalFileHintLabel.setText(INVALID_FILE_MSG);
                        okButton.setEnabled(false);
                    }
                } else {
                    // filename is empty string
                    externalFileHintLabel.setText(ENTER_FILE_MSG);
                    okButton.setEnabled(false);
                }
            } else {
                okButton.setEnabled(false);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ACTION_NAME = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                  "CompareSnapshotsAction_ActionName"); // NOI18N
    private static final String ACTION_DESCR = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                   "CompareSnapshotsAction_ActionDescr"); // NOI18N
    private static final String SELECT_DIALOG_CAPTION = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                            "CompareSnapshotsAction_SelectDialogCaption"); // NOI18N
    private static final String OPEN_CHOOSER_CAPTION = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                           "CompareSnapshotsAction_OpenChooserCaption"); // NOI18N
    private static final String OPEN_CHOOSER_FILTER = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                          "CompareSnapshotsAction_OpenChooserFilter"); // NOI18N
    private static final String NO_COMPARABLE_SNAPSHOTS_FOUND_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                        "CompareSnapshotsAction_NoComparableSnapshotsFoundMsg"); // NOI18N
    private static final String COMPARING_SAME_SNAPSHOTS_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                   "CompareSnapshotsAction_ComparingSameSnapshotsMsg"); // NOI18N
    private static final String DIFFERENT_SNAPSHOT_TYPE_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                  "CompareSnapshotsAction_DifferentSnapshotTypeMsg"); // NOI18N
    private static final String DIFFERENT_SNAPSHOTS_TYPE_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                   "CompareSnapshotsAction_DifferentSnapshotsTypeMsg"); // NOI18N
    private static final String ONLY_MEMORY_SNAPSHOTS_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                "CompareSnapshotsAction_OnlyMemorySnapshotsMsg"); // NOI18N
    private static final String DIFFERENT_OBJECTS_COUNT_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                  "CompareSnapshotsAction_DifferentObjectsCountMsg"); // NOI18N
    private static final String DIFFERENT_OBJECTS_COUNTS_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                   "CompareSnapshotsAction_DifferentObjectsCountsMsg"); // NOI18N
    private static final String INVALID_FILE_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                       "CompareSnapshotsAction_InvalidFileMsg"); // NOI18N
    private static final String INVALID_FILES_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                        "CompareSnapshotsAction_InvalidFilesMsg"); // NOI18N
    private static final String ENTER_FILE_MSG = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                     "CompareSnapshotsAction_EnterFileMsg"); // NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                     "CompareSnapshotsAction_OkButtonText"); // NOI18N
    private static final String SELECT_SNAPSHOT_STRING = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                             "CompareSnapshotsAction_SelectSnapshotString"); // NOI18N
    private static final String SELECT_SNAPSHOTS_STRING = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                              "CompareSnapshotsAction_SelectSnapshotsString"); // NOI18N
    private static final String FROM_PROJECT_RADIO_TEXT = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                              "CompareSnapshotsAction_FromProjectRadioText"); // NOI18N
    private static final String ONLY_COMPARABLE_LISTED_STRING = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                    "CompareSnapshotsAction_OnlyComparableListedString"); // NOI18N
    private static final String FROM_FILE_RADIO_TEXT = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                           "CompareSnapshotsAction_FromFileRadioText"); // NOI18N
    private static final String BROWSE_BUTTON_TEXT = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                         "CompareSnapshotsAction_BrowseButtonText"); // NOI18N
    private static final String BROWSE2_BUTTON_TEXT = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                          "CompareSnapshotsAction_Browse2ButtonText"); // NOI18N
    private static final String BROWSE_BUTTON_ACCESS_DESCR = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                          "CompareSnapshotsAction_BrowseButtonAccessDescr"); // NOI18N
    private static final String SNAPSHOT1_STRING = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                       "CompareSnapshotsAction_Snapshot1String"); // NOI18N
    private static final String SNAPSHOT2_STRING = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                       "CompareSnapshotsAction_Snapshot2String"); // NOI18N
    private static final String SNAPSHOT_ACCESS_DESCR = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                       "CompareSnapshotsAction_SnapshotAccessDescr"); // NOI18N
    private static final String SNAPSHOTS_LIST_ACCESS_DESCR = NbBundle.getMessage(CompareSnapshotsAction.class,
                                                                                  "CompareSnapshotsAction_SnapshotsListAccessDescr"); // NOI18N
                                                                                                                                      // -----
    private static final ImageIcon cpuIcon = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/cpuSmall.png", false); // NOI18N
    private static final ImageIcon fragmentIcon = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/fragmentSmall.png", false); // NOI18N
    private static final ImageIcon memoryIcon = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/memorySmall.png", false); // NOI18N
    private static JFileChooser snapshotFileChooser;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private LoadedSnapshot snapshot;
    private SelectExternalSnapshotsPanel externalSnapshotsSelector;
    private SelectSecondSnapshotPanel secondSnapshotSelector;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CompareSnapshotsAction() {
        snapshot = null;
        putValue(Action.NAME, ACTION_NAME);
        putValue(Action.SHORT_DESCRIPTION, ACTION_DESCR);
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/actions/resources/compareSnapshots.png", false)); // NOI18N
    }

    public CompareSnapshotsAction(LoadedSnapshot snapshot) {
        this();
        this.snapshot = snapshot;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isEnabled() {
        return NetBeansProfiler.isInitialized();
    }

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
            snapshotFileChooser.setDialogTitle(OPEN_CHOOSER_CAPTION);
            snapshotFileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".nps"); // NOI18N
                    }

                    public String getDescription() {
                        return MessageFormat.format(OPEN_CHOOSER_FILTER, new Object[] { "nps" }); // NOI18N
                    }
                });
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

        DialogDescriptor desc = new DialogDescriptor(getSecondSnapshotSelector(), SELECT_DIALOG_CAPTION, true,
                                                     new Object[] {
                                                         getSecondSnapshotSelector().getOKButton(), DialogDescriptor.CANCEL_OPTION
                                                     }, DialogDescriptor.OK_OPTION, 0, null, null);
        Object res = ProfilerDialogs.notify(desc);

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
        DialogDescriptor desc = new DialogDescriptor(getExternalSnapshotsSelector(), SELECT_DIALOG_CAPTION, true,
                                                     new Object[] {
                                                         getExternalSnapshotsSelector().getOKButton(),
                                                         DialogDescriptor.CANCEL_OPTION
                                                     }, DialogDescriptor.OK_OPTION, 0, null, null);
        Object res = ProfilerDialogs.notify(desc);

        if (res.equals(getExternalSnapshotsSelector().getOKButton())) {
            ResultsManager.getDefault()
                          .compareSnapshots(FileUtil.toFileObject(new File(getExternalSnapshotsSelector().getSnapshot1Filename())),
                                            FileUtil.toFileObject(new File(getExternalSnapshotsSelector().getSnapshot2Filename())));
        }
    }
}
