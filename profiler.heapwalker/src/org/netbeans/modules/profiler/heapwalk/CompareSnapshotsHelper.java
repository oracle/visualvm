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

package org.netbeans.modules.profiler.heapwalk;

import org.netbeans.lib.profiler.utils.StringUtils;
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
import java.text.MessageFormat;
import java.util.Date;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


public class CompareSnapshotsHelper {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

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

        public File getSnapshot() {
            if (fromProjectRadio.isSelected()) {
                Object selectedItem = projectSnapshotsList.getSelectedValue();

                if ((selectedItem == null) || !(selectedItem instanceof FileObject)) {
                    return null;
                }

                return FileUtil.toFile((FileObject)selectedItem);
            } else if (fromFileRadio.isSelected()) {
                String sf = externalFileField.getText();

                if (sf.length() > 0) {
                    File s = new File(sf);

                    if (s.exists() && s.isFile()) {
                        return s;
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

            // Add saved snapshots
            final Lookup.Provider project = heapWalker.getHeapDumpProject();
            File heapdumpFile = heapWalker.getHeapDumpFile();
            final File heapdumpDir = heapdumpFile != null ? heapdumpFile.getParentFile() : null;
            FileObject[] snapshotsOnDisk = ResultsManager.getDefault().listSavedHeapdumps(project, heapdumpDir);
            FileObject snapshotFile = (heapdumpFile != null) ? FileUtil.toFileObject(heapdumpFile) : null;

            for (int i = 0; i < snapshotsOnDisk.length; i++) {
                if (((snapshotFile == null) || !snapshotsOnDisk[i].equals(snapshotFile))) {
                    listModel.addElement(snapshotsOnDisk[i]);
                }
            }

            if (listModel.getSize() == 0) {
                listModel.addElement(NO_COMPARABLE_SNAPSHOTS_FOUND_MSG);
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
                        org.openide.awt.Mnemonics.setLocalizedText(fromProjectRadio, FROM_PROJECT_RADIO_TEXT);
                        fromProjectRadio.setToolTipText(null);
                    } else {
                        org.openide.awt.Mnemonics.setLocalizedText(fromProjectRadio, FROM_CURRENT_LOCATION_RADIO_TEXT);
                        fromProjectRadio.setToolTipText(heapdumpDir != null ?
                                heapdumpDir.getAbsolutePath() : null);
                    }
                }
            });
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

            projectSnapshotsHintLabel = new JLabel(" "); // NOI18N
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

                        if (value instanceof FileObject) {
                            FileObject fo = (FileObject) value;
                            c.setIcon(memoryIcon);

                            String fileName = fo.getName();

                            Set<TopComponent> tcs = WindowManager.getDefault().getRegistry().getOpened();
                            for (TopComponent tc : tcs) {
                                Object o = tc.getClientProperty("HeapDumpFileName"); // NOI18N
                                if (o != null && FileUtil.toFile(fo).equals(new File(o.toString()))) {
                                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                                    break;
                                }
                            }

                            if (fileName.startsWith("heapdump-")) { // NOI18N

                                String time = fileName.substring("heapdump-".length(), fileName.length()); // NOI18N

                                try {
                                    long timeStamp = Long.parseLong(time);
                                    c.setText(MessageFormat.format(HEAP_SNAPSHOT_DISPLAYNAME,
                                                                   new Object[] { StringUtils.formatUserDate(new Date(timeStamp)) }));
                                } catch (NumberFormatException e) {
                                    // file name is probably customized
                                    c.setText(MessageFormat.format(HEAP_SNAPSHOT_DISPLAYNAME, new Object[] { fileName }));
                                }
                            } else {
                                c.setText(MessageFormat.format(HEAP_SNAPSHOT_DISPLAYNAME, new Object[] { fileName }));
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
                        projectSnapshotsHintLabel.setText(" "); // NOI18N
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
                        JFileChooser snapshotChooser = CompareSnapshotsHelper.getSnapshotFileChooser();
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
                        if (s.equals(heapWalker.getHeapDumpFile())) {
                            // comparing snapshot with itself
                            externalFileHintLabel.setText(COMPARING_SAME_SNAPSHOTS_MSG);
                            okButton.setEnabled(false);
                        } else {
                            // comparing different snapshots
                            externalFileHintLabel.setText(" "); // NOI18N
                            okButton.setEnabled(true);
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
    private static final String SELECT_DIALOG_CAPTION = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                            "CompareSnapshotsHelper_SelectDialogCaption"); // NOI18N
    private static final String OPEN_CHOOSER_CAPTION = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                           "CompareSnapshotsHelper_OpenChooserCaption"); // NOI18N
    private static final String OPEN_CHOOSER_FILTER = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                          "CompareSnapshotsHelper_OpenChooserFilter"); // NOI18N
    private static final String NO_COMPARABLE_SNAPSHOTS_FOUND_MSG = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                                        "CompareSnapshotsHelper_NoComparableSnapshotsFoundMsg"); // NOI18N
    private static final String COMPARING_SAME_SNAPSHOTS_MSG = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                                   "CompareSnapshotsHelper_ComparingSameSnapshotsMsg"); // NOI18N
    private static final String INVALID_FILE_MSG = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                       "CompareSnapshotsHelper_InvalidFileMsg"); // NOI18N
    private static final String ENTER_FILE_MSG = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                     "CompareSnapshotsHelper_EnterFileMsg"); // NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                     "CompareSnapshotsHelper_OkButtonText"); // NOI18N
    private static final String SELECT_SNAPSHOT_STRING = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                             "CompareSnapshotsHelper_SelectSnapshotString"); // NOI18N
    private static final String FROM_PROJECT_RADIO_TEXT = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                              "CompareSnapshotsHelper_FromProjectRadioText"); // NOI18N
    private static final String FROM_CURRENT_LOCATION_RADIO_TEXT = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                              "CompareSnapshotsHelper_FromCurrentLocationRadioText"); // NOI18N
    private static final String FROM_FILE_RADIO_TEXT = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                           "CompareSnapshotsHelper_FromFileRadioText"); // NOI18N
    private static final String BROWSE_BUTTON_TEXT = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                         "CompareSnapshotsHelper_BrowseButtonText"); // NOI18N
    private static final String BROWSE_BUTTON_ACCESS_DESCR = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                          "CompareSnapshotsHelper_BrowseButtonAccessDescr"); // NOI18N
    private static final String SNAPSHOTS_LIST_ACCESS_DESCR = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                                  "CompareSnapshotsHelper_SnapshotsListAccessDescr"); // NOI18N
    private static final String HEAP_SNAPSHOT_DISPLAYNAME = NbBundle.getMessage(CompareSnapshotsHelper.class,
                                                                                "CompareSnapshotsHelper_HeapSnapshotDisplayName"); // NOI18N
                                                                                                                                      // -----
    private static final Icon memoryIcon = Icons.getIcon(ProfilerIcons.MEMORY);
    private static JFileChooser snapshotFileChooser;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HeapFragmentWalker heapWalker;
    private SelectSecondSnapshotPanel secondSnapshotSelector;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private CompareSnapshotsHelper(HeapFragmentWalker heapWalker) {
        this.heapWalker = heapWalker;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static File selectSnapshot(HeapFragmentWalker heapWalker) {
        CompareSnapshotsHelper helper = new CompareSnapshotsHelper(heapWalker);
        SelectSecondSnapshotPanel panel = helper.getSecondSnapshotSelector();
        panel.populateSnapshotsList();

        DialogDescriptor desc = new DialogDescriptor(panel, SELECT_DIALOG_CAPTION, true,
                                                     new Object[] {
                                                         panel.getOKButton(), DialogDescriptor.CANCEL_OPTION
                                                     }, DialogDescriptor.OK_OPTION, 0, null, null);
        Object res = DialogDisplayer.getDefault().notify(desc);

        File ret = null;
        if (res.equals(panel.getOKButton()))
            ret = panel.getSnapshot();
        
        return ret;
    }

    private static JFileChooser getSnapshotFileChooser() {
        if (snapshotFileChooser == null) {
            snapshotFileChooser = new JFileChooser();
            snapshotFileChooser.setAcceptAllFileFilterUsed(false);
            snapshotFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            snapshotFileChooser.setMultiSelectionEnabled(false);
            snapshotFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            snapshotFileChooser.setDialogTitle(OPEN_CHOOSER_CAPTION);
            snapshotFileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || ResultsManager.checkHprofFile(f);
                    }

                    public String getDescription() {
                        return OPEN_CHOOSER_FILTER; // NOI18N
                    }
                });
        }

        return snapshotFileChooser;
    }
    
    private SelectSecondSnapshotPanel getSecondSnapshotSelector() {
        if (secondSnapshotSelector == null) {
            secondSnapshotSelector = new SelectSecondSnapshotPanel();
        }

        return secondSnapshotSelector;
    }
}
