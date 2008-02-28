/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.snapshot.application;

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
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.ui.stp.Utils;
import org.openide.DialogDescriptor;

/**
 *
 * @author Jiri Sedlacek
 * 
 */
class ApplicationSnapshotConfigurator extends JPanel {

  public static ApplicationSnapshotConfigurator defineSnapshot() {
    ApplicationSnapshotConfigurator hc = getDefault();
    hc.setupDefineCoreDump();
    
    final DialogDescriptor dd = new DialogDescriptor(hc, "Add Application Snapshot", true, new Object[] {
      hc.okButton, DialogDescriptor.CANCEL_OPTION }, hc.okButton, 0, null, null);
    final Dialog d = ProfilerDialogs.createDialog(dd);
    d.pack();
    d.setVisible(true);
    
    if (dd.getValue() == hc.okButton) return hc;
    else return null;
  }
  
  public File getSnapshotFile() {
    return new File(snapshotFileField.getText().trim());
  }
  
  public boolean deleteSourceFile() {
      return deleteSourceCheckbox.isSelected();
  }
  
  private static ApplicationSnapshotConfigurator defaultInstance;
  
  private ApplicationSnapshotConfigurator() {
    initComponents();
    update();
  }
  
  private static ApplicationSnapshotConfigurator getDefault() {
    if (defaultInstance == null) defaultInstance = new ApplicationSnapshotConfigurator();
    return defaultInstance;
  }
  
  private void setupDefineCoreDump() {
    snapshotFileField.setText("");
    deleteSourceCheckbox.setSelected(false);
  }
  
  private void update() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        File snapshotFile = getSnapshotFile();
        okButton.setEnabled(ApplicationSnapshotsSupport.getInstance().getApplicationSnapshotCategory().isSnapshotArchive(snapshotFile));
      }
    });
  }

  private void chooseSnapshot() {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Select Application Snapshot");
      chooser.setSelectedFile(getSnapshotFile());
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileFilter(ApplicationSnapshotsSupport.getInstance().getCategory().getFileFilter());
      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) snapshotFileField.setText(chooser.getSelectedFile().getAbsolutePath());
  }
  
  private void initComponents() {
    setLayout(new GridBagLayout());
    GridBagConstraints constraints;
    
    // coreDumpFileLabel
    coreDumpFileLabel = new JLabel("Snapshot file:");
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(15, 10, 0, 0);
    add(coreDumpFileLabel, constraints);
    
    // coreDumpFileField
    snapshotFileField = new JTextField();
    snapshotFileField.setPreferredSize(new Dimension(220, snapshotFileField.getPreferredSize().height));
    snapshotFileField.getDocument().addDocumentListener(new DocumentListener() {
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
    add(snapshotFileField, constraints);
    
    // snapshotFileButton
    snapshotFileButton = new JButton("Browse...");
    snapshotFileButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            chooseSnapshot();
        }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(15, 5, 0, 10);
    add(snapshotFileButton, constraints);         
    
    // deleteSourceCheckbox
    deleteSourceCheckbox = new JCheckBox("Delete source file");
    deleteSourceCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { update(); };
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(18, 10, 0, 0);
    add(deleteSourceCheckbox, constraints);
    
    // spacer
    JPanel spacer = Utils.createFillerPanel();
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.weighty = 1;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.NORTHWEST;
    constraints.insets = new Insets(0, 0, 15, 0);
    add(spacer, constraints);
    
    // okButton
    okButton = new JButton("OK");
    
    // UI tweaks
    deleteSourceCheckbox.setBorder(coreDumpFileLabel.getBorder());
  }
  
  private JLabel coreDumpFileLabel;
  private JTextField snapshotFileField;
  private JButton snapshotFileButton;
  private JCheckBox deleteSourceCheckbox;
  
  private JButton okButton;
  
}
