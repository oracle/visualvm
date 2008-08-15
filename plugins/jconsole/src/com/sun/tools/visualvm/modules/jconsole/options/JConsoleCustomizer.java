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

package com.sun.tools.visualvm.modules.jconsole.options;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author  Jean-Francois Denise
 */
public class JConsoleCustomizer extends JPanel {

    private boolean initialized;
    private PathController pluginsController;
    private JFileChooser pluginsChooser;
    private JConsoleOptionsPanelController controler;

    private static class CustomizerFileFilter extends FileFilter {

        private String type;

        CustomizerFileFilter(String type) {
            this.type = type;
        }

        public boolean accept(File f) {
            if (f != null) {
                if (f.isDirectory()) {
                    return true;
                }
                String extension = getExtension(f);
                if (extension != null && extension.equals("jar")) { // NOI18N
                    return true;
                }
            }
            return false;
        }

        public static String getExtension(File f) {
            if (f != null) {
                String filename = f.getName();
                int i = filename.lastIndexOf('.');
                if (i > 0 && i < filename.length() - 1) {
                    return filename.substring(i + 1).toLowerCase();
                }
            }
            return null;
        }

        public String getDescription() {
            return "JConsole " + type + " path (.jar or directory)"; // NOI18N
        }
    }

    private class ChangedListener implements ListDataListener, KeyListener {

        public void intervalAdded(ListDataEvent arg0) {
        }

        public void intervalRemoved(ListDataEvent arg0) {
        }

        public void contentsChanged(ListDataEvent arg0) {
            changed();
        }

        public void keyTyped(KeyEvent arg0) {
            changed();
        }

        public void keyPressed(KeyEvent arg0) {

        }

        public void keyReleased(KeyEvent arg0) {

        }
    }

    private class KeyLstnr implements KeyListener {

        KeyLstnr() {
        }

        public void keyTyped(KeyEvent e) {
            changed();
            char c = e.getKeyChar();
            if (!(Character.isDigit(c) ||
                    c == KeyEvent.VK_BACK_SPACE ||
                    c == KeyEvent.VK_DELETE)) {
                e.consume();
            }
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
        }

        public void focusGained(FocusEvent e) {
            Object source = e.getSource();
            Component opposite = e.getOppositeComponent();

            if (!e.isTemporary() &&
                    source instanceof JTextField &&
                    opposite instanceof JComponent) {

                ((JTextField) source).selectAll();
            }
        }

        public void focusLost(FocusEvent e) {

        }
    }

    /** Creates new form JConsoleCustomizer */
    public JConsoleCustomizer(JConsoleOptionsPanelController contr) {
        this.controler = contr;
        initComponents();

        pluginsChooser = new JFileChooser();
        pluginsChooser.setMultiSelectionEnabled(true);
        pluginsChooser.setFileFilter(new CustomizerFileFilter("Plugins")); // NOI18N
        pluginsChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        ChangedListener changedListener = new ChangedListener();

        pluginsController = new PathController(jList1, pathLabel, jButtonAddJar,
                pluginsChooser,
                jButtonRemove,
                jButtonMoveUp, jButtonMoveDown, changedListener);

        pluginsController.setVisible(true);

        KeyLstnr listener = new KeyLstnr();
        period.addKeyListener(listener);

    }

    synchronized void changed() {
        controler.changed();
    }

    synchronized void load() {
        String plugins = JConsoleSettings.getDefault().getPluginsPath();
        Integer polling = JConsoleSettings.getDefault().getPolling();
        pluginsController.updateModel(plugins);
        period.setText(polling.toString());
        initialized = true;
    }

    synchronized void store() {
        if (!initialized) {
            return;
        }
        JConsoleSettings.getDefault().setPluginsPath(pluginsController.toString());
        JConsoleSettings.getDefault().setPolling(Integer.valueOf(period.getText()));
    }

    void cancel() {

    }

    boolean valid() {
        return true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        periodLabel = new javax.swing.JLabel();
        period = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jButtonAddJar = new javax.swing.JButton();
        jButtonMoveUp = new javax.swing.JButton();
        jButtonMoveDown = new javax.swing.JButton();
        jButtonRemove = new javax.swing.JButton();
        pathLabel = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.border.title"))); // NOI18N

        periodLabel.setDisplayedMnemonic('P');
        periodLabel.setLabelFor(period);
        periodLabel.setText(org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.periodLabel.text")); // NOI18N

        jScrollPane1.setViewportView(jList1);

        org.openide.awt.Mnemonics.setLocalizedText(jButtonAddJar, org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.jButtonAddJar.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveUp, org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.jButtonMoveUp.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveDown, org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.jButtonMoveDown.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonRemove, org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.jButtonRemove.text")); // NOI18N
        jButtonRemove.setActionCommand(org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.jButtonRemove.actionCommand")); // NOI18N

        pathLabel.setDisplayedMnemonic('l');
        pathLabel.setLabelFor(jList1);
        pathLabel.setText(org.openide.util.NbBundle.getMessage(JConsoleCustomizer.class, "JConsoleCustomizer.pathLabel.text")); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 295, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jButtonRemove, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonAddJar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonMoveUp, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonMoveDown, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .add(pathLabel)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(9, 9, 9)
                .add(pathLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jButtonAddJar)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonRemove)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonMoveUp)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonMoveDown))
                    .add(jScrollPane1, 0, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jButtonMoveDown.getAccessibleContext().setAccessibleParent(jButtonAddJar);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(periodLabel)
                        .add(18, 18, 18)
                        .add(period, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 138, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(layout.createSequentialGroup()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(24, 24, 24))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(16, 16, 16)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(period, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(periodLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddJar;
    private javax.swing.JButton jButtonMoveDown;
    private javax.swing.JButton jButtonMoveUp;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JList jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JTextField period;
    private javax.swing.JLabel periodLabel;
    // End of variables declaration//GEN-END:variables
}
