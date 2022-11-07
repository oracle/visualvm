/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.appui.about;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.ui.DesktopUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
public class AboutDialogControls extends JPanel {
    private final static Logger LOGGER = Logger.getLogger(AboutDialogControls.class.getName());

    // --- Internal API --------------------------------------------------------

    AboutDialogControls() {
        initComponents();
        
        lastLogfileSave = System.getProperty("user.home"); // NOI18N
        if (!new File(lastLogfileSave).isDirectory()) {
            lastLogfileSave = System.getProperty("java.io.tmpdir"); // NOI18N
            if (!new File(lastLogfileSave).isDirectory()) lastLogfileSave = null;
        }
        if (lastLogfileSave != null) lastLogfileSave = lastLogfileSave +
                                     File.separator + "logfile.txt"; // NOI18N
    }

    JButton getDefaultButton() {
        return closeButton;
    }

    void updateAppearance() {
        int buttonsCount = 1; // Close button always present
        
        String buildID = AboutDialog.getInstance().getBuildID();
        String details = AboutDialog.getInstance().getDetails();
        String logfile = AboutDialog.getInstance().getLogfile();
        
        buildIDLabel.setVisible(buildID != null);
        if (buildID != null) buildIDLabel.setText(buildID);
        
        detailsButton.setVisible(details != null);
        if (details != null) buttonsCount++;
        
        logfileButton.setVisible(logfile != null);
        if (logfile != null) buttonsCount++;
        
        buttonsContainer.removeAll();
        buttonsContainer.setLayout(new GridLayout(1, buttonsCount, 6, 0));
        if (details != null) buttonsContainer.add(detailsButton);
        if (logfile != null) buttonsContainer.add(logfileButton);
        buttonsContainer.add(closeButton);
    }
    
    
    // --- Private implementation ----------------------------------------------
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        buildIDLabel = new JLabel();
        buildIDLabel.setFont(buildIDLabel.getFont().deriveFont(Font.BOLD));
        buildIDLabel.setEnabled(false);
        
        detailsButton = new JButton();
        Mnemonics.setLocalizedText(detailsButton, NbBundle.getMessage(AboutDialogControls.class, "LBL_Details")); // NOI18N
        detailsButton.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(AboutDialogControls.class, "DESCR_Details")); // NOI18N
        detailsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDetails();
            }
        });
        
        logfileButton = new JButton();
        Mnemonics.setLocalizedText(logfileButton, NbBundle.getMessage(AboutDialogControls.class, "LBL_Logfile")); // NOI18N
        logfileButton.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(AboutDialogControls.class, "DESCR_Logfile")); // NOI18N
        logfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showLogfile();
            }
        });
        
        closeButton = new JButton();
        Mnemonics.setLocalizedText(closeButton, NbBundle.getMessage(AboutDialogControls.class, "LBL_Close")); // NOI18N
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        AboutDialog.getInstance().close();
                    }
                });
            }
        });
        
        buttonsContainer = new JPanel(new GridLayout(1, 3, 6, 0));
        buttonsContainer.add(detailsButton);
        buttonsContainer.add(logfileButton);
        buttonsContainer.add(closeButton);
        
        setBorder(BorderFactory.createEmptyBorder(10, 10, 7, 10));
        add(buildIDLabel, BorderLayout.WEST);
        add(buttonsContainer, BorderLayout.EAST);
    }
    
    private void showDetails() {
        final TextBrowser tb = TextBrowser.getInstance();
        JButton helperButton = new JButton() {
            protected void fireActionPerformed(ActionEvent event) {
                tb.copyAllHtmlToClipboard();
                JOptionPane.showMessageDialog(AboutDialog.getInstance().getDialog(),
                        NbBundle.getMessage(AboutDialogControls.class, "MSG_Copy_Clipboard"), // NOI18N
                        NbBundle.getMessage(AboutDialogControls.class, "CAPTION_Copy_Clipbard"), // NOI18N
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        Mnemonics.setLocalizedText(helperButton, NbBundle.getMessage(AboutDialogControls.class, "BTN_Copy_Clipboard")); // NOI18N
        tb.setCaption(NbBundle.getMessage(AboutDialogControls.class, "TITLE_Details"));    // NOI18N
        tb.setPreferredBrowserSize(new Dimension(450, 250));
        tb.setHelperButton(helperButton);
        tb.showHTMLText(AboutDialog.getInstance().getDetails());
    }
    
    private void showLogfile() {
        Runnable logfileDisplayer = new Runnable() {
            public void run() {
                logfileButton.setEnabled(false);
                
                File logfile = new File(AboutDialog.getInstance().getLogfile());
        
                try {
                    if (!logfile.exists() || !logfile.isFile() || !logfile.canRead()) {
                        JOptionPane.showMessageDialog(AboutDialog.getInstance().getDialog(), 
                                NbBundle.getMessage(AboutDialogControls.class, "LBL_Cannot_open_the_logfile", logfile.getAbsolutePath()),   // NOI18N
                                NbBundle.getMessage(AboutDialogControls.class, "LBL_Error"),    // NOI18N
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    TextBrowser.getInstance().setCaption(NbBundle.getMessage(AboutDialogControls.class, "LBL_Logfile_Viewer_", logfile.getAbsolutePath())); // NOI18N
                    if (DesktopUtils.isOpenAvailable()) {
                        try {
                            DesktopUtils.open(logfile);
                        } catch (Exception ex) {
                            showLogfileInBrowser(logfile);
                            LOGGER.throwing(AboutDialogControls.class.getName(), "showLogFile", ex);    // NOI18N
                        }
                    } else {
                        showLogfileInBrowser(logfile);
                    }
                } catch (Exception e) {
                    LOGGER.throwing(AboutDialogControls.class.getName(), "showLogFile", e); // NOI18N
                    JOptionPane.showMessageDialog(AboutDialog.getInstance().getDialog(), 
                            NbBundle.getMessage(AboutDialogControls.class, "LBL_Cannot_open_the_logfile", logfile.getAbsolutePath()),   // NOI18N
                            NbBundle.getMessage(AboutDialogControls.class, "LBL_Error"),    // NOI18N
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    logfileButton.setEnabled(true);
                }
            }
        };

        new Thread(logfileDisplayer).start();
    }
    
    private void showLogfileInBrowser(final File logfile) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(logfile, "r")) { // NOI18N
            byte[] buffer = new byte[(int)raf.length()];
            raf.readFully(buffer);
            final String logfileContents = new String(buffer);
            SwingUtilities.invokeAndWait(new Runnable() {
               public void run() {
                   String string = logfileContents;
                   if (string.isEmpty()) string = NbBundle.getMessage(AboutDialogControls.class, "MSG_Logfile_notready"); // NOI18N
                   final TextBrowser tb = TextBrowser.getInstance();
                   JButton helperButton = new JButton() {
                       protected void fireActionPerformed(ActionEvent event) {
                            saveFileAs(logfile);
                        }
                   };
                   Mnemonics.setLocalizedText(helperButton, NbBundle.getMessage(AboutDialogControls.class, "BTN_Save_file")); // NOI18N
                   helperButton.setEnabled(!logfileContents.isEmpty());
                   tb.setPreferredBrowserSize(new Dimension(700, 550));
                   tb.setHelperButton(helperButton);
                   tb.showCodeText(string);
               } 
            });
        }
    }
    
    private void saveFileAs(final File file) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(NbBundle.getMessage(AboutDialogControls.class, "CAPTION_Save_logfile")); // NOI18N
        chooser.setSelectedFile(new File(lastLogfileSave));
        if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
            final File copy = chooser.getSelectedFile();
//            if (copy.isFile()) // TODO: show a confirmation dialog for already existing file
            lastLogfileSave = copy.getAbsolutePath();
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    ProgressHandle pHandle = null;
                    try {
                        pHandle = ProgressHandle.createHandle(
                                NbBundle.getMessage(AboutDialogControls.class,
                                "MSG_Saving_logfile", file.getName()));  // NOI18N
                        pHandle.setInitialDelay(0);
                        pHandle.start();
                        if (!Utils.copyFile(file, copy)) JOptionPane.showMessageDialog(AboutDialog.getInstance().getDialog(), 
                            NbBundle.getMessage(AboutDialogControls.class, "MSG_Save_logfile_failed"),   // NOI18N
                            NbBundle.getMessage(AboutDialogControls.class, "CAPTION_Save_logfile_failed"),    // NOI18N
                            JOptionPane.ERROR_MESSAGE);
                    } finally {
                        final ProgressHandle pHandleF = pHandle;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { if (pHandleF != null) pHandleF.finish(); }
                        });
                    }
                }
            });
        }
    }
    
    
    private JLabel buildIDLabel;
    private JButton closeButton;
    private JButton detailsButton;
    private JButton logfileButton;
    private JPanel buttonsContainer;
    
    private String lastLogfileSave;

}
