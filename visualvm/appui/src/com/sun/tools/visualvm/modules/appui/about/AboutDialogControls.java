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

package com.sun.tools.visualvm.modules.appui.about;

import com.sun.tools.visualvm.core.ui.DesktopUtils;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class AboutDialogControls extends JPanel {
    private final static Logger LOGGER = Logger.getLogger(AboutDialogControls.class.getName());

    // --- Internal API --------------------------------------------------------

    AboutDialogControls() {
        initComponents();
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
        
        detailsButton = new JButton("Details");
        detailsButton.setDefaultCapable(false);
        detailsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDetails();
            }
        });
        
        logfileButton = new JButton("Logfile");
        logfileButton.setDefaultCapable(false);
        logfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showLogfile();
            }
        });
        
        closeButton = new JButton("Close");
        closeButton.setDefaultCapable(true);
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
        TextBrowser.getInstance().setCaption("Details");
        TextBrowser.getInstance().setPreferredBrowserSize(new Dimension(450, 250));
        TextBrowser.getInstance().showHTMLText(AboutDialog.getInstance().getDetails());
    }
    
    private void showLogfile() {
        Runnable logfileDisplayer = new Runnable() {
            public void run() {
                logfileButton.setEnabled(false);
                
                File logfile = new File(AboutDialog.getInstance().getLogfile());
        
                try {
                    if (!logfile.exists() || !logfile.isFile() || !logfile.canRead()) {
                        JOptionPane.showMessageDialog(AboutDialog.getInstance().getDialog(), "Cannot open the logfile " + logfile.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    TextBrowser.getInstance().setCaption("Logfile Viewer (" + logfile.getAbsolutePath() + ")");
                    if (DesktopUtils.isOpenAvailable()) {
                        try {
                            DesktopUtils.open(logfile);
                        } catch (Exception ex) {
                            showLogfileInBrowser(logfile);
                            LOGGER.throwing(AboutDialogControls.class.getName(), "showLogFile", ex);
                        }
                    } else {
                        showLogfileInBrowser(logfile);
                    }
                } catch (Exception e) {
                    LOGGER.throwing(AboutDialogControls.class.getName(), "showLogFile", e);
                    JOptionPane.showMessageDialog(AboutDialog.getInstance().getDialog(), "Cannot open the logfile " + logfile.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    logfileButton.setEnabled(true);
                }
            }
        };

        new Thread(logfileDisplayer).start();
    }
    
    private void showLogfileInBrowser(File logfile) throws Exception {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(logfile, "r"); // NOI18N
            final byte[] buffer = new byte[(int)raf.length()];
            raf.readFully(buffer);
            SwingUtilities.invokeAndWait(new Runnable() {
               public void run() {
                   TextBrowser.getInstance().setPreferredBrowserSize(new Dimension(700, 550));
                   TextBrowser.getInstance().showCodeText(new String(buffer));
               } 
            });
        } finally {
            if (raf != null) raf.close();
        }
    }
    
    
    private JLabel buildIDLabel;
    private JButton closeButton;
    private JButton detailsButton;
    private JButton logfileButton;
    private JPanel buttonsContainer;

}
