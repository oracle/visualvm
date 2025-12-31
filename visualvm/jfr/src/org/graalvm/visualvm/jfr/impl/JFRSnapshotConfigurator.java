/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.jfr.JFRSnapshotSupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 * 
 */
class JFRSnapshotConfigurator extends JPanel {

  private boolean internalChange = false;

  public static JFRSnapshotConfigurator defineJFRSnapshot() {
    JFRSnapshotConfigurator hc = getDefault();
    hc.setupDefineJFRSnapshot();
    
    final DialogDescriptor dd = new DialogDescriptor(hc, NbBundle.getMessage(JFRSnapshotConfigurator.class, "Title_Add_VM_Coredump"), true, new Object[] { // NOI18N
      hc.okButton, DialogDescriptor.CANCEL_OPTION }, hc.okButton, 0, null, null);
    final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
    d.pack();
    d.setVisible(true);
    
    if (dd.getValue() == hc.okButton) return hc;
    else return null;
  }
  
  public String getJFRSnapshotFile() {
    return jfrSnapshotFileField.getText().trim();
  }
  
  public String getDisplayname() {
    return displaynameField.getText().trim();
  }
  
  public boolean deleteSourceFile() {
      return deleteSourceCheckbox.isSelected();
  }
  
  private static JFRSnapshotConfigurator defaultInstance;
  
  private JFRSnapshotConfigurator() {
    initComponents();
    update();
  }
  
  private static JFRSnapshotConfigurator getDefault() {
    if (defaultInstance == null) defaultInstance = new JFRSnapshotConfigurator();
    return defaultInstance;
  }
  
  private void setupDefineJFRSnapshot() {
    jfrSnapshotFileField.setEnabled(true);
    displaynameCheckbox.setSelected(false);
    displaynameCheckbox.setEnabled(true);
    jfrSnapshotFileField.setText("");
    displaynameField.setText("");
    deleteSourceCheckbox.setSelected(false);
  }
  
  private void update() {
    if (internalChange) return;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        String jfrSnapshotName = getJFRSnapshotFile();
        File jfrSnapshotFile = new File(jfrSnapshotName);
        
        if (!displaynameCheckbox.isSelected()) {
          internalChange = true;
          File file = new File(jfrSnapshotName);
          if (file.isFile()) displaynameField.setText(file.getName());
          internalChange = false;
        }
        
        String displayname = getDisplayname();
        displaynameField.setEnabled(displaynameCheckbox.isSelected());
        
        okButton.setEnabled(jfrSnapshotFile.exists() && jfrSnapshotFile.isFile() && !displayname.isEmpty());
      }
    });
  }

  private void chooseJFRSnapshot() {
      JFileChooser chooser = new JFileChooser(new File(getJFRSnapshotFile()));
      chooser.setDialogTitle(NbBundle.getMessage(JFRSnapshotConfigurator.class, "LBL_Select_VM_Coredump"));    // NOI18N
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileFilter(JFRSnapshotSupport.getCategory().getFileFilter());
      int returnVal = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
      if (returnVal == JFileChooser.APPROVE_OPTION) {
          jfrSnapshotFileField.setText(chooser.getSelectedFile().getAbsolutePath());
      }
  }
  
  private void initComponents() {
    setLayout(new GridBagLayout());
    GridBagConstraints constraints;
    
    // jfrSnapshotFileLabel
    jfrSnapshotFileLabel = new JLabel();
    Mnemonics.setLocalizedText(jfrSnapshotFileLabel, NbBundle.getMessage(JFRSnapshotConfigurator.class, "LBL_VM_Coredump_file")); // NOI18N
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(15, 10, 0, 0);
    add(jfrSnapshotFileLabel, constraints);
    
    // jfrSnapshotFileField
    jfrSnapshotFileField = new JTextField();
    jfrSnapshotFileLabel.setLabelFor(jfrSnapshotFileField);
    jfrSnapshotFileField.setPreferredSize(new Dimension(220, jfrSnapshotFileField.getPreferredSize().height));
    jfrSnapshotFileField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e)  { update(); }
      public void removeUpdate(DocumentEvent e)  { update(); }
      public void changedUpdate(DocumentEvent e) { update(); }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.weightx = 1;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(15, 5, 0, 0);
    add(jfrSnapshotFileField, constraints);
    
    // jfrSnapshotFileButton
    jfrSnapshotFileButton = new JButton();
    Mnemonics.setLocalizedText(jfrSnapshotFileButton, NbBundle.getMessage(JFRSnapshotConfigurator.class, "LBL_Browse")); // NOI18N
    jfrSnapshotFileButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            chooseJFRSnapshot();
        }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(15, 5, 0, 10);
    add(jfrSnapshotFileButton, constraints);
    
    // displaynameCheckbox
    displaynameCheckbox = new JCheckBox();
    Mnemonics.setLocalizedText(displaynameCheckbox, NbBundle.getMessage(JFRSnapshotConfigurator.class, "LBL_Display_name")); // NOI18N
    displaynameCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { update(); }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(8, 10, 0, 0);
    add(displaynameCheckbox, constraints);
    
    // displaynameField
    displaynameField = new JTextField();
    displaynameField.setPreferredSize(new Dimension(220, displaynameField.getPreferredSize().height));
    displaynameField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e)  { update(); }
      public void removeUpdate(DocumentEvent e)  { update(); }
      public void changedUpdate(DocumentEvent e) { update(); }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 2;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(8, 5, 0, 10);
    add(displaynameField, constraints);
    
    // deleteSourceCheckbox
    deleteSourceCheckbox = new JCheckBox();
    Mnemonics.setLocalizedText(deleteSourceCheckbox, NbBundle.getMessage(JFRSnapshotConfigurator.class, "LBL_Delete_source_file")); // NOI18N
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(18, 10, 0, 0);
    add(deleteSourceCheckbox, constraints);
    
    // spacer
    JPanel spacer = new JPanel(null);
    spacer.setOpaque(false);
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 4;
    constraints.weighty = 1;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.NORTHWEST;
    constraints.insets = new Insets(0, 0, 15, 0);
    add(spacer, constraints);
    
    // okButton
    okButton = new JButton(NbBundle.getMessage(JFRSnapshotConfigurator.class, "LBL_OK"));  // NOI18N
    
    // UI tweaks
    displaynameCheckbox.setBorder(jfrSnapshotFileLabel.getBorder());
    deleteSourceCheckbox.setBorder(jfrSnapshotFileLabel.getBorder());
  }
  
  private JLabel jfrSnapshotFileLabel;
  private JTextField jfrSnapshotFileField;
  private JButton jfrSnapshotFileButton;
  private JCheckBox displaynameCheckbox;
  private JTextField displaynameField;
  private JCheckBox deleteSourceCheckbox;
  
  private JButton okButton;
  
}
