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
 * @author  jfdenise
 */
public class JConsoleCustomizer extends JPanel {
    private boolean initialized;
    private PathController pluginsController;
    private PathController classpathController;
    private JFileChooser classPathchooser;
    private JFileChooser pluginsChooser;
    private JconsoleOptionsPanelController controler;
    
    private static class CustomizerFileFilter extends FileFilter {
        private String type;
        CustomizerFileFilter(String type) {
            this.type = type;
        }
        public boolean accept(File f) {
            if(f != null) {
                if(f.isDirectory()) {
                    return true;
                }
                String extension = getExtension(f);
                if(extension != null && extension.equals( "jar")) {// NOI18N
                    return true;
                };
            }
            return false;
        }
        
        public static String getExtension(File f) {
            if(f != null) {
                String filename = f.getName();
                int i = filename.lastIndexOf('.');
                if(i>0 && i<filename.length()-1) {
                    return filename.substring(i+1).toLowerCase();
                };
            }
            return null;
        }
        
        public String getDescription() {
            return  "JConsole "+ type + " path (jar or dir)";// NOI18N
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
                    opposite instanceof JComponent ) {
                
                ((JTextField)source).selectAll();
            }
        }
        
        public void focusLost(FocusEvent e) {
            
        }
    }
    
    /** Creates new form JConsoleCustomizer */
    public JConsoleCustomizer(JconsoleOptionsPanelController contr) {
        this.controler = contr;
        initComponents();
        
        classPathchooser = new JFileChooser();
        classPathchooser.setMultiSelectionEnabled(true);
        classPathchooser.setFileFilter(new CustomizerFileFilter("Class"));// NOI18N
        classPathchooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        pluginsChooser = new JFileChooser();
        pluginsChooser.setMultiSelectionEnabled(true);
        pluginsChooser.setFileFilter(new CustomizerFileFilter("Plugins"));// NOI18N
        pluginsChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        
        ChangedListener changedListener = new ChangedListener();
        
        pluginsController = new PathController(jList1, pathLabel, jButtonAddJarC,
                pluginsChooser,
                jButtonRemoveC,
                jButtonMoveUpC,jButtonMoveDownC, changedListener);
        
        classpathController = new PathController(jList2, pathLabel1, jButtonAddJarC1,
                classPathchooser,
                jButtonRemoveC1,
                jButtonMoveUpC1,jButtonMoveDownC1, changedListener);
        
        pluginsController.setVisible(JConsoleSettings.isNetBeansJVMGreaterThanJDK15());
        
        KeyLstnr listener = new KeyLstnr();
        period.addKeyListener(listener);
        
//        otherArgs.addKeyListener(changedListener);
//        vmOptions.addKeyListener(changedListener);
//        defaultUrl.addKeyListener(changedListener);
    }
    
    synchronized void changed() {
        controler.changed();
    }
    
    synchronized void load() {
        String path = JConsoleSettings.getDefault().getClassPath();
        String url = JConsoleSettings.getDefault().getDefaultUrl();
        String plugins = JConsoleSettings.getDefault().getPluginsPath();
        Integer polling = JConsoleSettings.getDefault().getPolling();
        Boolean tileVal = JConsoleSettings.getDefault().getTile();
        String vmArgs = JConsoleSettings.getDefault().getVMOptions();
        String otherArgsVal = JConsoleSettings.getDefault().getOtherArgs();
        
        classpathController.updateModel(path);
//        defaultUrl.setText(url);
        pluginsController.updateModel(plugins);
        period.setText(polling.toString());
        tile.setSelected(tileVal);
//        vmOptions.setText(vmArgs);
//        otherArgs.setText(otherArgsVal);
        initialized = true;
    }
    
    synchronized void store() {
        if(!initialized) return;
        JConsoleSettings.getDefault().setClassPath(classpathController.toString());
//        JConsoleSettings.getDefault().setDefaultUrl(defaultUrl.getText());
        JConsoleSettings.getDefault().setPluginsPath(pluginsController.toString());
        JConsoleSettings.getDefault().setPolling(Integer.valueOf(period.getText()));
        JConsoleSettings.getDefault().setTile(tile.isSelected());
//        JConsoleSettings.getDefault().setVMOptions(vmOptions.getText());
//        JConsoleSettings.getDefault().setOtherArgs(otherArgs.getText());
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

        jLabel1 = new javax.swing.JLabel();
        tile = new javax.swing.JCheckBox();
        period = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jButtonAddJarC = new javax.swing.JButton();
        jButtonMoveUpC = new javax.swing.JButton();
        jButtonMoveDownC = new javax.swing.JButton();
        jButtonRemoveC = new javax.swing.JButton();
        pathLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jButtonAddJarC1 = new javax.swing.JButton();
        jButtonMoveUpC1 = new javax.swing.JButton();
        jButtonMoveDownC1 = new javax.swing.JButton();
        jButtonRemoveC1 = new javax.swing.JButton();
        pathLabel1 = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder("null"));

        jLabel1.setDisplayedMnemonic('P');
        jLabel1.setLabelFor(period);
        jLabel1.setText("null");
        jLabel1.setToolTipText("null");

        tile.setMnemonic('T');
        tile.setText("null");
        tile.setToolTipText("null");

        period.setText("null");

        jScrollPane1.setViewportView(jList1);
        jList1.getAccessibleContext().setAccessibleName("null");
        jList1.getAccessibleContext().setAccessibleDescription("null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonAddJarC, "null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveUpC, "null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveDownC, "null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonRemoveC, "null");
        jButtonRemoveC.setActionCommand("null");

        pathLabel.setDisplayedMnemonic('l');
        pathLabel.setLabelFor(jList1);
        pathLabel.setText("null");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jButtonRemoveC, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonAddJarC, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonMoveUpC, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonMoveDownC, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(24, 24, 24))
            .add(pathLabel)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(12, 12, 12)
                .add(pathLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jButtonAddJarC)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonRemoveC)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonMoveUpC)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonMoveDownC))
                    .add(jScrollPane1, 0, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jButtonAddJarC.getAccessibleContext().setAccessibleDescription("null");
        jButtonMoveUpC.getAccessibleContext().setAccessibleDescription("null");
        jButtonMoveDownC.getAccessibleContext().setAccessibleDescription("null");
        jButtonMoveDownC.getAccessibleContext().setAccessibleParent(jButtonAddJarC);
        jButtonRemoveC.getAccessibleContext().setAccessibleDescription("null");
        pathLabel.getAccessibleContext().setAccessibleDescription("null");

        jScrollPane2.setViewportView(jList2);
        jList2.getAccessibleContext().setAccessibleName("null");
        jList2.getAccessibleContext().setAccessibleDescription("null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonAddJarC1, "null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveUpC1, "null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveDownC1, "null");

        org.openide.awt.Mnemonics.setLocalizedText(jButtonRemoveC1, "null");

        pathLabel1.setDisplayedMnemonic('C');
        pathLabel1.setLabelFor(jList2);
        pathLabel1.setText("null");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jButtonRemoveC1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonAddJarC1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonMoveUpC1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButtonMoveDownC1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(16, 16, 16))
            .add(pathLabel1)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(12, 12, 12)
                .add(pathLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jButtonAddJarC1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonRemoveC1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonMoveUpC1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButtonMoveDownC1))
                    .add(jScrollPane2, 0, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jButtonAddJarC1.getAccessibleContext().setAccessibleDescription("null");
        jButtonMoveUpC1.getAccessibleContext().setAccessibleDescription("null");
        jButtonMoveDownC1.getAccessibleContext().setAccessibleDescription("null");
        jButtonRemoveC1.getAccessibleContext().setAccessibleDescription("null");
        pathLabel1.getAccessibleContext().setAccessibleDescription("null");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(tile)
                            .add(layout.createSequentialGroup()
                                .add(jLabel1)
                                .add(22, 22, 22)
                                .add(period, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)))
                        .addContainerGap())
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(tile)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(period, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(14, 14, 14)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        period.getAccessibleContext().setAccessibleName("null");
        period.getAccessibleContext().setAccessibleDescription("null");
    }// </editor-fold>//GEN-END:initComponents
        
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddJarC;
    private javax.swing.JButton jButtonAddJarC1;
    private javax.swing.JButton jButtonMoveDownC;
    private javax.swing.JButton jButtonMoveDownC1;
    private javax.swing.JButton jButtonMoveUpC;
    private javax.swing.JButton jButtonMoveUpC1;
    private javax.swing.JButton jButtonRemoveC;
    private javax.swing.JButton jButtonRemoveC1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList jList1;
    private javax.swing.JList jList2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JLabel pathLabel1;
    private javax.swing.JTextField period;
    private javax.swing.JCheckBox tile;
    // End of variables declaration//GEN-END:variables
    
}

