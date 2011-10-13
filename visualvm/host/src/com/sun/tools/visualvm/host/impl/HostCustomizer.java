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

package com.sun.tools.visualvm.host.impl;

import com.sun.tools.visualvm.core.properties.PropertiesCustomizer;
import com.sun.tools.visualvm.core.properties.PropertiesSupport;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import com.sun.tools.visualvm.host.Host;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class HostCustomizer extends JPanel {

  private static Dimension MIN_PROPERTIES_SIZE = new Dimension(400, 200);
  private static Dimension MAX_PROPERTIES_SIZE = new Dimension(700, 400);

  private boolean internalChange = false;

  
  public static HostProperties defineHost() {
    HostCustomizer hc = getInstance();
    hc.setup();

    ScrollableContainer sc = new ScrollableContainer(hc,
            ScrollableContainer.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollableContainer.HORIZONTAL_SCROLLBAR_NEVER);
    sc.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    sc.setViewportBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    
    final DialogDescriptor dd = new DialogDescriptor(sc, NbBundle.getMessage(
            HostCustomizer.class, "Title_Add_Remote_Host"), true, new Object[] {   // NOI18N
            hc.okButton, DialogDescriptor.CANCEL_OPTION }, hc.okButton, 0, null, null);
    dd.setAdditionalOptions(new Object[] { hc.settingsButton });
    final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
    d.pack();
    d.setVisible(true);

    if (dd.getValue() == hc.okButton) {
        HostProperties hp = new HostProperties(hc.getHostName(), hc.getDisplayName(),
                                               hc.getPropertiesCustomizer());
        hc.accepted();
        return hp;
    } else {
        hc.cancelled();
        return null;
    }
  }
  
  
  private static HostCustomizer instance;
  
  private HostCustomizer() {
    initComponents();
    update();
  }
  
  private static HostCustomizer getInstance() {
    if (instance == null) instance = new HostCustomizer();
    return instance;
  }
  
  private String getHostName() {
      return hostnameField.getText().trim();
  }
  
  private String getDisplayName() {
      return displaynameField.getText().trim();
  }

  private PropertiesCustomizer getPropertiesCustomizer() {
      return settingsPanel;
  }
  
  private void setup() {
    hostnameField.setEnabled(true);
    displaynameCheckbox.setSelected(false);
    displaynameCheckbox.setEnabled(true);
    hostnameField.setText(""); // NOI18N
    displaynameField.setText(""); // NOI18N

    PropertiesSupport support = PropertiesSupport.sharedInstance();
    settingsPanel = !support.hasProperties(Host.class) ? null :
                     support.getCustomizer(Host.class);
    if (settingsPanel != null) settingsPanel.addChangeListener(listener);
    settingsButton.setVisible(settingsPanel != null);
    if (!settingsButton.isVisible()) settingsButton.setSelected(false);
    else settingsButton.setSelected(!settingsPanel.settingsValid());

    updateSettings();
  }

  private void accepted() {
      cleanup();
  }

  private void cancelled() {
      if (settingsPanel != null) settingsPanel.propertiesCancelled();
      cleanup();
  }

  private void cleanup() {
      if (settingsPanel != null) settingsPanel.removeChangeListener(listener);
      settingsContainer.removeAll();
      settingsPanel = null;
  }
  
  private void update() {
    if (internalChange) return;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        String hostname = getHostName();
        
        if (!displaynameCheckbox.isSelected()) {
          internalChange = true;
          displaynameField.setText(hostname);
          internalChange = false;
        }
        
        String displayname = getDisplayName();
        displaynameField.setEnabled(displaynameCheckbox.isSelected());

        boolean hostValid = hostname.length() > 0 && displayname.length() > 0;
        boolean settingsValid = settingsPanel == null ? true :
                                settingsPanel.settingsValid();
        
        okButton.setEnabled(hostValid && settingsValid);
      }
    });
  }

  private void updateSettings() {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              if (settingsButton.isSelected()) {
                  settingsContainer.add(settingsPanel, BorderLayout.CENTER);
                  settingsContainer.setBorder(BorderFactory.
                          createEmptyBorder(10, 0, 0, 0));

                  Dimension prefSize = settingsPanel.getPreferredSize();
//                  prefSize.width = Math.max(prefSize.width, MIN_PROPERTIES_SIZE.width);
//                  prefSize.width = Math.min(prefSize.width, MAX_PROPERTIES_SIZE.width);
                  prefSize.height = Math.max(prefSize.height, MIN_PROPERTIES_SIZE.height);
                  prefSize.height = Math.min(prefSize.height, MAX_PROPERTIES_SIZE.height);
                  settingsPanel.setPreferredSize(prefSize);

              } else {
                  settingsContainer.removeAll();
                  settingsContainer.setBorder(BorderFactory.createEmptyBorder());
              }
              Window w = SwingUtilities.getWindowAncestor(HostCustomizer.this);
              if (w != null) w.pack();
              update();
          }
      });
  }
  
  private void initComponents() {
    setLayout(new GridBagLayout());
    GridBagConstraints constraints;
    
    // hostnameLabel
    hostnameLabel = new JLabel();
    Mnemonics.setLocalizedText(hostnameLabel, NbBundle.getMessage(HostCustomizer.class, "LBL_Host_name")); // NOI18N
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(5, 0, 0, 0);
    add(hostnameLabel, constraints);
    
    // hostnameField
    hostnameField = new JTextField();
    hostnameLabel.setLabelFor(hostnameField);
    hostnameField.setPreferredSize(new Dimension(250, hostnameField.getPreferredSize().height));
    hostnameField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e)  { update(); }
      public void removeUpdate(DocumentEvent e)  { update(); }
      public void changedUpdate(DocumentEvent e) { update(); }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(5, 5, 0, 0);
    add(hostnameField, constraints);
    
    // displaynameCheckbox
    displaynameCheckbox = new JCheckBox();
    Mnemonics.setLocalizedText(displaynameCheckbox, NbBundle.getMessage(HostCustomizer.class, "LBL_Display_name")); // NOI18N
    displaynameCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { update(); };
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(8, 0, 5, 0);
    add(displaynameCheckbox, constraints);
    
    // displaynameField
    displaynameField = new JTextField();
    displaynameField.setPreferredSize(new Dimension(250, displaynameField.getPreferredSize().height));
    displaynameField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e)  { update(); }
      public void removeUpdate(DocumentEvent e)  { update(); }
      public void changedUpdate(DocumentEvent e) { update(); }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(8, 5, 5, 0);
    add(displaynameField, constraints);
    
    // spacer
    settingsContainer = new JPanel(new BorderLayout(0, 0));
    settingsContainer.setOpaque(false);
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.NORTHWEST;
    constraints.insets = new Insets(0, 0, 0, 0);
    add(settingsContainer, constraints);
    
    // okButton
    okButton = new JButton();
    Mnemonics.setLocalizedText(okButton, NbBundle.getMessage(
            HostCustomizer.class, "LBL_OK")); // NOI18N

    settingsButton = new JToggleButton() {
        protected void fireActionPerformed(ActionEvent e) {
            updateSettings();
        }
    };
    Mnemonics.setLocalizedText(settingsButton, NbBundle.getMessage(
            HostCustomizer.class, "BTN_AdavancedSettings")); // NOI18N
    
    // UI tweaks
    displaynameCheckbox.setBorder(hostnameLabel.getBorder());
  }
  
  private JLabel hostnameLabel;
  private JTextField hostnameField;
  private JCheckBox displaynameCheckbox;
  private JTextField displaynameField;
  private JPanel settingsContainer;

  private PropertiesCustomizer settingsPanel;
  
  private JButton okButton;
  private JToggleButton settingsButton;

  private final ChangeListener listener = new ChangeListener() {
                    public void stateChanged(ChangeEvent e) { update(); }
                };
  
}
