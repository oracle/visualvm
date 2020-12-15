/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.modules.startup;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.concurrent.ExecutionException;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class ImportPanel extends JPanel {
    
    private static final String KEY_FOLDER = "settings_folder";                 // NOI18N
    
    
    private final JProgressBar progress;
    private final JTextPane text2;
    private final JButton importB;
    private final JButton skipB;
    private final JRadioButton latest;
    private final JRadioButton recent;
    private final JRadioButton custom;
    
    private File selected;
    private File userdirsRoot;
    
    
    ImportPanel(final File latestRelease, final File recentlyUsed, File userdirsRoot, final String firstSupported) {
        super(new GridBagLayout());
        
        Color disabledText = UIManager.getLookAndFeel().getID().equals("GTK") ? // NOI18N
                             UIManager.getColor("Label.disabledShadow") :       // NOI18N
                             UIManager.getColor("Label.disabledForeground");    // NOI18N
        
        GridBagConstraints c;
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(25, 15, 10, 15);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JTextPane text1 = new JTextPane();
        text1.setContentType("text/html");                                      // NOI18N
        text1.setFont(UIManager.getFont("Label.font"));                         // NOI18N
        text1.setOpaque(false);
        text1.setEditable(false);
        text1.setFocusable(false);
        text1.setRequestFocusEnabled(false);
        setHtmlText(text1, NbBundle.getMessage(ImportPanel.class, "ImportPanel_Msg1")); // NOI18N
        add(text1, c);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 25, 0, 15);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        latest = new JRadioButton() {
            protected void fireItemStateChanged(ItemEvent event) {
                super.fireItemStateChanged(event);
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    selected = latestRelease;
                }
            }
        };
        latest.setText(getHtmlText(latest, "<nobr>" + latestRelease.getName() + " <span style=\"color:" + getColorText(disabledText) + ";\">" + // NOI18N
                                           NbBundle.getMessage(ImportPanel.class, "ImportPanel_OptionLatestRelease") + "</span>" + "</nobr>")); // NOI18N
        latest.putClientProperty(KEY_FOLDER, latestRelease);
        latest.setSelected(true);
        latest.setToolTipText(latestRelease.getAbsolutePath());
        add(latest, c);
        
        if (recentlyUsed != null && !recentlyUsed.equals(latestRelease)) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.insets = new Insets(0, 25, 0, 15);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.gridwidth = GridBagConstraints.REMAINDER;
            recent = new JRadioButton() {
                protected void fireItemStateChanged(ItemEvent event) {
                    super.fireItemStateChanged(event);
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        selected = recentlyUsed;
                    }
                }
            };
            recent.setText(getHtmlText(recent, "<nobr>" + recentlyUsed.getName() + " <span style=\"color:" + getColorText(disabledText) + ";\">" + // NOI18N
                                           NbBundle.getMessage(ImportPanel.class, "ImportPanel_OptionRecentlyUsed") + "</span>" + "</nobr>")); // NOI18N
            recent.setToolTipText(recentlyUsed.getAbsolutePath());
            recent.putClientProperty(KEY_FOLDER, recentlyUsed);
            add(recent, c);
        } else {
            recent = null;
        }
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.insets = new Insets(0, 25, 0, 15);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        custom = new JRadioButton() {
            protected void fireItemStateChanged(ItemEvent event) {
                super.fireItemStateChanged(event);
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    selected = null;
                    setText(getHtmlText(custom, "<nobr>" + NbBundle.getMessage(ImportPanel.class, "ImportPanel_OptionSelectCustom") + " <span style=\"color:" + getColorText(disabledText) + ";\">" + // NOI18N
                                        NbBundle.getMessage(ImportPanel.class, "ImportPanel_OptionSelectCustom2", firstSupported) + "</span>" + "</nobr>")); // NOI18N
                } else if (event.getStateChange() == ItemEvent.DESELECTED) {
                    setText(getHtmlText(custom, "<nobr>" + NbBundle.getMessage(ImportPanel.class, "ImportPanel_OptionSelectCustom") + "</nobr>")); // NOI18N
                }
            }
        };
        custom.setText(getHtmlText(custom, "<nobr>" + NbBundle.getMessage(ImportPanel.class, "ImportPanel_OptionSelectCustom") + "</nobr>")); // NOI18N
        custom.setToolTipText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_OptionSelectCustomTooltip")); // NOI18N
        custom.putClientProperty(KEY_FOLDER, null);
        add(custom, c);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.insets = new Insets(30, 15, 10, 15);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        progress = new JProgressBar();
        progress.setString(NbBundle.getMessage(ImportPanel.class, "ImportPanel_ProgressText")); // NOI18N
        progress.setStringPainted(true);
        progress.setVisible(false);
        add(progress, c);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.insets = new Insets(30, 15, 10, 15);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        text2 = new JTextPane();
        text2.setContentType("text/html");                                      // NOI18N
        text2.setFont(UIManager.getFont("Label.font"));                         // NOI18N
        text2.setOpaque(false);
        text2.setEditable(false);
        text2.setFocusable(false);
        text2.setRequestFocusEnabled(false);
        setHtmlText(text2, NbBundle.getMessage(ImportPanel.class, "ImportPanel_Msg2")); // NOI18N
        add(text2, c);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JPanel f = new JPanel(null);
        f.setOpaque(false);
        add(f, c);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.weightx = 1;
        c.insets = new Insets(25, 15, 10, 5);
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        importB = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                importImpl();
            }
        };
        importB.setText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_ImportButton")); // NOI18N
        importB.setDefaultCapable(true);
        add(importB, c);
        
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 7;
        c.insets = new Insets(25, 0, 10, 10);
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        skipB = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                skipImpl();
            }
        };
        skipB.setText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_SkipButton")); // NOI18N
        skipB.setDefaultCapable(false);
        add(skipB, c);
        
        ButtonGroup userdirs = new ButtonGroup();
        userdirs.add(latest);
        if (recent != null) userdirs.add(recent);
        userdirs.add(custom);
        
        makeSameSize(importB, skipB);
        
        selected = latestRelease;
        this.userdirsRoot = userdirsRoot;
    }
    
    
    abstract boolean isSupportedImport(File dir);
    
    abstract void contentsChanged();
    
    abstract void beforeImport();
    
    abstract void doImport(File source) throws Exception;
    
    abstract void afterImport();
    
    abstract void close();
    
    
    JButton getDefaultButton() {
        return importB;
    }
    
    
    private void importImpl() {
        final File source;
        if (selected == null) {
            source = customImpl();
            if (source == null) return;
        } else {
            source = selected;
        }
        
        beforeImport();
        
        new SwingWorker<String, Void>() {
            {
                if (latest.getClientProperty(KEY_FOLDER) != selected) latest.setEnabled(false);
                if (recent != null && recent.getClientProperty(KEY_FOLDER) != selected) recent.setEnabled(false);
                if (custom.getClientProperty(KEY_FOLDER) != selected) custom.setEnabled(false);
                importB.setEnabled(false);
                skipB.setEnabled(false);
                text2.setVisible(false);
                
                // Aqua LaF doesn't support painted string for indeterminate progress
                if (!"Aqua".equals(UIManager.getLookAndFeel().getID())) progress.setIndeterminate(true); // NOI18N
                progress.setVisible(true);
            }
            @Override
            protected String doInBackground() throws Exception {
                try {
                    doImport(source);
                    return null;
                } catch (Exception e) {
                    return e.getLocalizedMessage();
                }
            }
            @Override
            public void done() {
                afterImport();
                
                String result;
                try {
                    result = get();
                } catch (InterruptedException | ExecutionException ex) {
                    result = ex.getLocalizedMessage();
                }
                
                if (!"Aqua".equals(UIManager.getLookAndFeel().getID())) progress.setIndeterminate(false); // NOI18N
                
                if (result != null) {
                    progress.setVisible(false);
                    setHint("<nobr><b>" + NbBundle.getMessage(ImportPanel.class, "ImportPanel_ImportFailed") + "</b><br>" + result + "</nobr>"); // NOI18N
                    text2.setVisible(true);
                    importB.setEnabled(true);
                    skipB.setEnabled(true);
                    latest.setEnabled(true);
                    if (recent != null) recent.setEnabled(true);
                    custom.setEnabled(true);
                } else {
                    close();
                }
            }
        }.execute();
    }
    
    private void skipImpl() {
        close();
    }
    
    private File customImpl() {
        JFileChooser ch = new JFileChooser(userdirsRoot) {
            public void approveSelection() {
                if (NbBundle.getMessage(ImportPanel.class, "ImportPanel_ImportButton").equals(getApproveButtonText())) { // NOI18N
                    super.approveSelection();
                } else {
                    File f = getSelectedFile();
                    if (f == null) f = getCurrentDirectory();
                    setCurrentDirectory(f);
                }
            }
        };
        ch.setDialogType(JFileChooser.CUSTOM_DIALOG);
        ch.setDialogTitle(NbBundle.getMessage(ImportPanel.class, "ImportPanel_CustomDirCaption")); // NOI18N
        ch.setApproveButtonText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_OpenButton")); // NOI18N
        ch.setApproveButtonToolTipText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_OpenButtonTooltip")); // NOI18N
        ch.setMultiSelectionEnabled(false);
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setAcceptAllFileFilterUsed(false);
        ch.addChoosableFileFilter(new FileFilter() {
            @Override public boolean accept(File f) { return f.isDirectory(); }
            @Override public String getDescription() { return NbBundle.getMessage(ImportPanel.class, "ImportPanel_SettingsFilter"); } // NOI18N
        });
        ch.setSelectedFile(null);
        ch.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName()) ||
                    JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
                    File f = ch.getSelectedFile();
                    if (f == null) f = ch.getCurrentDirectory();
                    if (f != null && isSupportedImport(f)) { // NOI18N
                        ch.setApproveButtonText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_ImportButton")); // NOI18N
                        ch.setApproveButtonToolTipText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_ImportButtonTooltip")); // NOI18N
                    } else {
                        ch.setApproveButtonText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_OpenButton")); // NOI18N
                        ch.setApproveButtonToolTipText(NbBundle.getMessage(ImportPanel.class, "ImportPanel_OpenButtonTooltip")); // NOI18N
                    }
                }
            }
        });
        
        if (ch.showDialog(latest, null) == JFileChooser.APPROVE_OPTION) {
            File userdir = ch.getSelectedFile();
            userdirsRoot = userdir == null ? null : userdir.getParentFile();
            return userdir;
        } else {
            return null;
        }
    }
    
    
    private void setHint(String text) {
        setHtmlText(text2, text);
        contentsChanged();
    }
    
    
    private static void makeSameSize(JButton button1, JButton button2) {
        Dimension dim1 = button1.getPreferredSize();
        Dimension dim2 = button2.getPreferredSize();
        int maxWidth = Math.max(dim1.width, dim2.width);
        int maxHeight = Math.max(dim1.height, dim2.height);
        button1.setPreferredSize(new Dimension(maxWidth, maxHeight));
        button2.setPreferredSize(new Dimension(maxWidth, maxHeight));
    }
    
    
    private static void setHtmlText(JTextPane pane, String text) {
        pane.setDocument(pane.getEditorKit().createDefaultDocument()); // Workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5042872
        pane.setText(getHtmlText(pane, text));
        pane.setCaretPosition(0);
    }
    
    private static String getHtmlText(JComponent component, String text) {
        Font font = component.getFont();
        String rgb = getColorText(component.getForeground());
        return "<html><body text=\"" + rgb + "\" style=\"font-size: " + font.getSize() + // NOI18N
               "pt; font-family: " + font.getName() + ";\">" + text + "</body></html>"; // NOI18N
    }
    
    private static String getColorText(Color color) {
        return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")"; // NOI18N
    }
    
}
