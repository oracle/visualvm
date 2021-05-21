/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.coredump.impl;

import org.graalvm.visualvm.coredump.CoreDumpSupport;
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
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 * 
 */
class CoreDumpConfigurator extends JPanel {

  private boolean internalChange = false;

  public static CoreDumpConfigurator defineCoreDump() {
    CoreDumpConfigurator hc = getDefault();
    hc.setupDefineCoreDump();
    
    final DialogDescriptor dd = new DialogDescriptor(hc, NbBundle.getMessage(CoreDumpConfigurator.class, "Title_Add_VM_Coredump"), true, new Object[] { // NOI18N
      hc.okButton, DialogDescriptor.CANCEL_OPTION }, hc.okButton, 0, null, null);
    final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
    d.pack();
    d.setVisible(true);
    
    if (dd.getValue() == hc.okButton) return hc;
    else return null;
  }
  
  public String getCoreDumpFile() {
    return coreDumpFileField.getText().trim();
  }
  
  public String getDisplayname() {
    return displaynameField.getText().trim();
  }
  
  public String getJavaHome() {
    return javaHomeFileField.getText().trim();
  }
  
  public boolean deleteSourceFile() {
      return deleteSourceCheckbox.isSelected();
  }
  
  private static CoreDumpConfigurator defaultInstance;
  
  private CoreDumpConfigurator() {
    initComponents();
    update();
  }
  
  private static CoreDumpConfigurator getDefault() {
    if (defaultInstance == null) defaultInstance = new CoreDumpConfigurator();
    return defaultInstance;
  }
  
  private void setupDefineCoreDump() {
    coreDumpFileField.setEnabled(true);
    displaynameCheckbox.setSelected(false);
    displaynameCheckbox.setEnabled(true);
    coreDumpFileField.setText("");
    displaynameField.setText("");
    javaHomeFileField.setText(CoreDumpSupport.getCurrentJDKHome());
    javaHomeFileField.setEnabled(true);
    deleteSourceCheckbox.setSelected(false);
  }
  
  private void update() {
    if (internalChange) return;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        String coreDumpname = getCoreDumpFile();
        File coreDumpFile = new File(coreDumpname);
        
        String jdkHome = getJavaHome();
        File jdkHomeFile = new File(jdkHome);
        
        if (!displaynameCheckbox.isSelected()) {
          internalChange = true;
          File file = new File(coreDumpname);
          if (file.isFile()) displaynameField.setText(file.getName());
          internalChange = false;
        }
        
        String displayname = getDisplayname();
        displaynameField.setEnabled(displaynameCheckbox.isSelected());
        
        okButton.setEnabled(coreDumpFile.exists() && coreDumpFile.isFile() &&
                jdkHomeFile.exists() && jdkHomeFile.isDirectory() && !displayname.isEmpty());
      }
    });
  }
  
  private void chooseJavaHome() {
      JFileChooser chooser = new JFileChooser(new File(getJavaHome()));
      chooser.setDialogTitle(NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_Select_JDK_Home"));   // NOI18N
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int returnVal = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
      if (returnVal == JFileChooser.APPROVE_OPTION) {
          javaHomeFileField.setText(chooser.getSelectedFile().getAbsolutePath());
      }
  }

  private void chooseCoreDump() {
      JFileChooser chooser = new JFileChooser(new File(getCoreDumpFile()));
      chooser.setDialogTitle(NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_Select_VM_Coredump"));    // NOI18N
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileFilter(CoreDumpSupport.getCategory().getFileFilter());
      int returnVal = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
      if (returnVal == JFileChooser.APPROVE_OPTION) {
          coreDumpFileField.setText(chooser.getSelectedFile().getAbsolutePath());
      }
  }
  
  private void initComponents() {
    setLayout(new GridBagLayout());
    GridBagConstraints constraints;
    
    // coreDumpFileLabel
    coreDumpFileLabel = new JLabel();
    Mnemonics.setLocalizedText(coreDumpFileLabel, NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_VM_Coredump_file")); // NOI18N
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(15, 10, 0, 0);
    add(coreDumpFileLabel, constraints);
    
    // coreDumpFileField
    coreDumpFileField = new JTextField();
    coreDumpFileLabel.setLabelFor(coreDumpFileField);
    coreDumpFileField.setPreferredSize(new Dimension(220, coreDumpFileField.getPreferredSize().height));
    coreDumpFileField.getDocument().addDocumentListener(new DocumentListener() {
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
    add(coreDumpFileField, constraints);
    
    // coreDumpFileButton
    coreDumpFileButton = new JButton();
    Mnemonics.setLocalizedText(coreDumpFileButton, NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_Browse")); // NOI18N
    coreDumpFileButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            chooseCoreDump();
        }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(15, 5, 0, 10);
    add(coreDumpFileButton, constraints);    
    
    // javaHomeFileLabel
    javaHomeFileLabel = new JLabel();
    Mnemonics.setLocalizedText(javaHomeFileLabel, NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_JDK_home")); // NOI18N
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(8, 10, 0, 0);
    add(javaHomeFileLabel, constraints);
    
    // javaHomeFileField
    javaHomeFileField = new JTextField();
    javaHomeFileLabel.setLabelFor(javaHomeFileField);
    javaHomeFileField.setPreferredSize(new Dimension(220, javaHomeFileField.getPreferredSize().height));
    javaHomeFileField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e)  { update(); }
      public void removeUpdate(DocumentEvent e)  { update(); }
      public void changedUpdate(DocumentEvent e) { update(); }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(8, 5, 0, 0);
    add(javaHomeFileField, constraints);
    
    // javaHomeFileButton
    javaHomeFileButton = new JButton();
    Mnemonics.setLocalizedText(javaHomeFileButton, NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_Browse1")); // NOI18N
    javaHomeFileButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            chooseJavaHome();
        }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 1;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(8, 5, 0, 10);
    add(javaHomeFileButton, constraints);        
    
    // displaynameCheckbox
    displaynameCheckbox = new JCheckBox();
    Mnemonics.setLocalizedText(displaynameCheckbox, NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_Display_name")); // NOI18N
    displaynameCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { update(); };
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
    Mnemonics.setLocalizedText(deleteSourceCheckbox, NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_Delete_source_file")); // NOI18N
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
    okButton = new JButton(NbBundle.getMessage(CoreDumpConfigurator.class, "LBL_OK"));  // NOI18N
    
    // UI tweaks
    displaynameCheckbox.setBorder(coreDumpFileLabel.getBorder());
    deleteSourceCheckbox.setBorder(coreDumpFileLabel.getBorder());
  }
  
  private JLabel coreDumpFileLabel;
  private JTextField coreDumpFileField;
  private JButton coreDumpFileButton;
  private JLabel javaHomeFileLabel;
  private JTextField javaHomeFileField;
  private JButton javaHomeFileButton;
  private JCheckBox displaynameCheckbox;
  private JTextField displaynameField;
  private JCheckBox deleteSourceCheckbox;
  
  private JButton okButton;
  
}
