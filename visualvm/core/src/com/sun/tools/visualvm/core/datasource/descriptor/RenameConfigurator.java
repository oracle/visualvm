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

package com.sun.tools.visualvm.core.datasource.descriptor;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.snapshot.application.*;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
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
class RenameConfigurator extends JPanel {

  public static RenameConfigurator defineName(DataSource dataSource) {
    RenameConfigurator hc = getDefault();
    hc.setupDefineName(dataSource);
    
    final DialogDescriptor dd = new DialogDescriptor(hc, "Rename", true, new Object[] {
      hc.okButton, DialogDescriptor.CANCEL_OPTION }, hc.okButton, 0, null, null);
    final Dialog d = ProfilerDialogs.createDialog(dd);
    d.pack();
    d.setVisible(true);
    
    if (dd.getValue() == hc.okButton) return hc;
    else return null;
  }
  
  public String getDisplayName() {
      return nameField.getText().trim();
  }
  
  private static RenameConfigurator defaultInstance;
  
  private RenameConfigurator() {
    initComponents();
    update();
  }
  
  private static RenameConfigurator getDefault() {
    if (defaultInstance == null) defaultInstance = new RenameConfigurator();
    return defaultInstance;
  }
  
  private void setupDefineName(DataSource dataSource) {
    nameField.setText(DataSourceDescriptorFactory.getDescriptor(dataSource).getName());
    nameField.selectAll();
  }
  
  private void update() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        okButton.setEnabled(getDisplayName().length() > 0);
      }
    });
  }

  private void initComponents() {
    setLayout(new GridBagLayout());
    GridBagConstraints constraints;
    
    // nameLabel
    nameLabel = new JLabel("New Name:");
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(15, 10, 0, 0);
    add(nameLabel, constraints);
    
    // coreDumpFileField
    nameField = new JTextField();
    nameField.setPreferredSize(new Dimension(220, nameField.getPreferredSize().height));
    nameField.getDocument().addDocumentListener(new DocumentListener() {
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
    constraints.insets = new Insets(15, 5, 0, 10);
    add(nameField, constraints);
    
    // spacer
    JPanel spacer = Utils.createFillerPanel();
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weighty = 1;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.NORTHWEST;
    constraints.insets = new Insets(0, 0, 15, 0);
    add(spacer, constraints);
    
    // okButton
    okButton = new JButton("OK");
  }
  
  private JLabel nameLabel;
  private JTextField nameField;
  
  private JButton okButton;
  
}
