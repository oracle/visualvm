/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.core.ui.components.Spacer;
import com.sun.tools.visualvm.uisupport.HTMLTextArea;
import com.sun.tools.visualvm.uisupport.UISupport;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Caret;
import org.netbeans.lib.profiler.global.Platform;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaPlatformSelector extends JPanel {

  // NOTE: to be called outside of EDT
  // TODO: fix updating UI outside of EDT
  static File selectJavaBinary(String javaName, String archName, String java, String arch) {
    JavaPlatformSelector hc = getDefault();
    hc.setupSelectJavaPlatform(javaName, archName);
    
    final DialogDescriptor dd = new DialogDescriptor(hc, NbBundle.getMessage(
            JavaPlatformSelector.class, "CAP_Calibrate_java"), true, new Object[] { // NOI18N
            hc.okButton, DialogDescriptor.CANCEL_OPTION }, hc.okButton, 0, null, null);
    final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
    d.pack();
    d.setVisible(true);
    
    if (dd.getValue() == hc.okButton) {
        File javaBinary = hc.getJavaBinary();
        String[] props = JavaInfo.getSystemProperties(javaBinary, "java.version", "sun.arch.data.model"); // NOI18N
        if (props == null || props.length < 2 ||
            !java.equals(Platform.getJDKVersionString(props[0])) ||
            !arch.equals(props[1])) {
            NotifyDescriptor nd = new NotifyDescriptor.Message(
                    NbBundle.getMessage(JavaPlatformSelector.class,
                    "MSG_Incorrect_java_binary", javaName, archName), // NOI18N
                    NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            javaBinary = null;
        }
        return javaBinary == null ? selectJavaBinary(javaName, archName, java, arch) : javaBinary;
    } else {
        return null;
    }
  }
  
  private File getJavaBinary() {
    return new File(javaPlatformFileField.getText().trim());
  }
  
  private static JavaPlatformSelector defaultInstance;
  
  private JavaPlatformSelector() {
    initComponents();
    update();
  }
  
  private static synchronized JavaPlatformSelector getDefault() {
    if (defaultInstance == null) defaultInstance = new JavaPlatformSelector();
    return defaultInstance;
  }
  
  private void setupSelectJavaPlatform(String javaName, String archName) {
      hintArea.setText(NbBundle.getMessage(JavaPlatformSelector.class,
              "MSG_Calibration_required", javaName, archName)); // NOI18N
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              javaPlatformFileField.selectAll();
              javaPlatformFileField.requestFocusInWindow();
          }
      });
  }
  
  private void update() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        File snapshotFile = getJavaBinary();
        okButton.setEnabled(snapshotFile.isFile());
      }
    });
  }

  private void chooseSnapshot() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(NbBundle.getMessage(
            JavaPlatformSelector.class, "CAP_Select_java_binary")); // NOI18N
    chooser.setSelectedFile(getJavaBinary());
    if (Platform.isWindows()) {
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileFilter(new FileFilter() {
          private String java = "java.exe"; // NOI18N
          public boolean accept(File f) {
              return f.isDirectory() || (f.isFile() && java.equals(f.getName()));
          }
          public String getDescription() {
              return NbBundle.getMessage(JavaPlatformSelector.class,
                      "LBL_Java_file_filter", java); // NOI18N
          }
      });
    } else {
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileFilter(new FileFilter() {
          private String java = "java"; // NOI18N
          public boolean accept(File f) {
              return f.isDirectory() || (f.isFile() && java.equals(f.getName())); // NOI18N
          }
          public String getDescription() {
              return NbBundle.getMessage(JavaPlatformSelector.class,
                      "LBL_Java_file_filter", java); // NOI18N
          }
      });
    }
    if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION)
        javaPlatformFileField.setText(chooser.getSelectedFile().getAbsolutePath());
  }
  
  private void initComponents() {
    setLayout(new GridBagLayout());
    GridBagConstraints constraints;
    
    // snapshotFileLabel
    hintArea = new HTMLTextArea();
    hintArea.setOpaque(false);
    if (UISupport.isNimbusLookAndFeel()) hintArea.setBackground(new Color(0, 0, 0, 0));
    hintArea.setCaret(new NullCaret());
    hintArea.setBorder(BorderFactory.createEmptyBorder());
    hintArea.setFocusable(false);
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.NORTHWEST;
    constraints.insets = new Insets(15, 10, 0, 10);
    add(hintArea, constraints);
    
    // snapshotFileField
    javaPlatformFileField = new JTextField();
    javaPlatformFileField.setPreferredSize(new Dimension(220, javaPlatformFileField.getPreferredSize().height));
    javaPlatformFileField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e)  { update(); }
      public void removeUpdate(DocumentEvent e)  { update(); }
      public void changedUpdate(DocumentEvent e) { update(); }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.weightx = 1;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(15, 10, 0, 0);
    add(javaPlatformFileField, constraints);
    
    // snapshotFileButton
    snapshotFileButton = new JButton();
    Mnemonics.setLocalizedText(snapshotFileButton, NbBundle.getMessage(
            JavaPlatformSelector.class, "BTN_Browse")); // NOI18N
    snapshotFileButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            chooseSnapshot();
        }
    });
    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 1;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(15, 5, 0, 10);
    add(snapshotFileButton, constraints);
    
    // spacer
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.weighty = 1;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.NORTHWEST;
    constraints.insets = new Insets(0, 0, 15, 0);
    add(Spacer.create(), constraints);
    
    // okButton
    okButton = new JButton(NbBundle.getMessage(JavaPlatformSelector.class, "BTN_Ok")); // NOI18N
  }
  
  private HTMLTextArea hintArea;
  private JTextField javaPlatformFileField;
  private JButton snapshotFileButton;
  
  private JButton okButton;
  
  
  private static final class NullCaret implements Caret {
        public void install(javax.swing.text.JTextComponent c) {}
        public void deinstall(javax.swing.text.JTextComponent c) {}
        public void paint(Graphics g) {}
        public void addChangeListener(ChangeListener l) {}
        public void removeChangeListener(ChangeListener l) {}
        public boolean isVisible() { return false; }
        public void setVisible(boolean v) {}
        public boolean isSelectionVisible() { return false; }
        public void setSelectionVisible(boolean v) {}
        public void setMagicCaretPosition(Point p) {}
        public Point getMagicCaretPosition() { return new Point(0, 0); }
        public void setBlinkRate(int rate) {}
        public int getBlinkRate() { return 0; }
        public int getDot() { return 0; }
        public int getMark() { return 0; }
        public void setDot(int dot) {}
        public void moveDot(int dot) {}
    }
  
}
