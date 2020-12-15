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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.modules.startup.dialogs.StartupDialog;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.NbBundle;

/**
 * Imports VisualVM settings from another compatible VisualVM userdir.
 * 
 * To test right from NetBeans:
 * 
 *  1a/ Predefined import:
 *     - add -J-Dnetbeans.import_userdir=... to visualvm/nbproject/project.properties
 *
 *  1b/ Interactive import:
 *     - add -J-Dnetbeans.importclass=org.graalvm.visualvm.modules.startup.ImportSettings to visualvm/nbproject/project.properties
 *     - add -J-Dnetbeans.default_userdir_root.dev=... to visualvm/nbproject/project.properties
 * 
 *  2/ Uncomment lines marked as *DEV*
 * 
 *  3/ Make sure the development userdir visualvm/build/testuserdir does not exist
 *
 *  4/ Invoke Run on the VisualVM-Startup module
 *
 * 
 * @author Jiri Sedlacek
 */
public class ImportSettings {
    
    // List of userdirs supported by the settings importer
    // Most recent release userdir is the last one
    private static final String[] SUPPORTED_USERDIRS = new String[] {
        "2.0",                                                                  // NOI18N
        "2.0.1",                                                                // NOI18N
        "2.0.2",                                                                // NOI18N
        "2.0.3",                                                                // NOI18N
        "2.0.4",                                                                // NOI18N
        "2.0.5"                                                                 // NOI18N
    };
    
    
    private final static Logger LOGGER = Logger.getLogger(ImportSettings.class.getName());
    
    
    public static void main(String[] args) throws Exception {
        String importUserdirS = System.getProperty("netbeans.import_userdir");  // NOI18N
        if (importUserdirS != null && !importUserdirS.isEmpty()) {
            // immediate import based on the provided system property
            File importUserdir = new File(importUserdirS);
            if (importUserdir.isDirectory()) {
                copyToUserdir(importUserdir);
            } else {
                LOGGER.info("Skipping import from netbeans.import_userdir, wrong userdir provided " + importUserdir); // NOI18N
            }
        } else {
            // interactive selection & import of the userdir
            String userdirsRootS = System.getProperty("netbeans.default_userdir_root"); // NOI18
///*DEV*/     if (userdirsRootS == null || userdirsRootS.isEmpty()) userdirsRootS = System.getProperty("netbeans.default_userdir_root.dev"); // NOI18

            File userdirsRoot = userdirsRootS == null ? null : new File(userdirsRootS);
            if (userdirsRoot == null || !userdirsRoot.isDirectory()) return;

            List<File> userdirs = availableUserdirs(userdirsRoot);
            if (userdirs.isEmpty()) return;

            File latestRelease = latestReleaseUserdir(userdirs);
            File recentlyUsed = lastRecentlyUsedUserdir(userdirs);


            Utils.setSystemLaF();

            final JDialog d = StartupDialog.create(NbBundle.getMessage(
                    ImportSettings.class, "ImportSettings_Caption"), null, -1); // NOI18N

            ImportPanel p = new ImportPanel(latestRelease, recentlyUsed, userdirsRoot) {
                @Override
                void contentsChanged() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { d.pack(); }
                    });
                }

                @Override
                void beforeImport() {
                    d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                }

                @Override
                void doImport(File source) throws Exception {
                    copyToUserdir(source);
                }

                @Override
                void afterImport() {
                    d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                }

                @Override
                void close() {
                    d.setVisible(false);
                    d.dispose();
                }
            };
            d.getContentPane().add(p, BorderLayout.CENTER);

            d.getRootPane().setDefaultButton(p.getDefaultButton());
            d.getRootPane().registerKeyboardAction(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        if (d.getDefaultCloseOperation() == JDialog.DISPOSE_ON_CLOSE) {
                            d.setVisible(false);
                            d.dispose();
                        }
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

    //        d.setResizable(true);
            d.pack();
            d.setLocationRelativeTo(null);

            d.setVisible(true);
        }
    }
    
    
    private static List<File> availableUserdirs(File userdirsRoot) {
        List<File> userdirs = new ArrayList();
        
        for (String supported : SUPPORTED_USERDIRS) {
            File userdir = availableUserdir(userdirsRoot, supported);
            if (userdir != null) userdirs.add(userdir);
        }
        
        return userdirs;
    }
    
    private static File availableUserdir(File userdirsFolder, String userdir) {
        File file = new File(userdirsFolder, userdir);
        
        if (!file.isDirectory() || !new File(file, "config").isDirectory()) return null; // NOI18N
        
        return file;
    }
    
    private static File latestReleaseUserdir(List<File> userdirs) {
        return userdirs.isEmpty() ? null : userdirs.get(userdirs.size() - 1);
    }
    
    private static File lastRecentlyUsedUserdir(List<File> userdirs) {
        File lastRecent = null;
        long lastModified = -1;
                
        for (File userdir : userdirs) {
            long modified = userdir.lastModified();
            if (modified > lastModified) {
                lastModified = modified;
                lastRecent = userdir;
            }
        }
        
        return lastRecent;
    }
    
    
    /* Copy files from source folder to current userdir according to include/exclude
     * patterns in etc/netbeans.import file. */
    private static void copyToUserdir(File source) throws IOException, PropertyVetoException {
        File userdir = new File(System.getProperty("netbeans.user", ""));       // NOI18N
        File netBeansDir = InstalledFileLocator.getDefault().locate("modules", null, false).getParentFile().getParentFile(); // NOI18N
        
        File importFile = new File(netBeansDir, "etc/visualvm.import");         // NOI18N
///*DEV*/ if (!importFile.isFile() && "testuserdir".equals(userdir.getName())) {  // NOI18N
///*DEV*/     File parent = netBeansDir.getParentFile();
///*DEV*/     if (parent != null && parent.isDirectory()) {
///*DEV*/         importFile = new File(parent, "launcher/visualvm.import");      // NOI18N
///*DEV*/     }
///*DEV*/ }
        
        LOGGER.fine("Import file: " + importFile);                              // NOI18N
        LOGGER.info("Importing from " + source + " to " + userdir);             // NOI18N
        
        File[] files1 = userdir.listFiles();
        try {
            CopyFiles.copyDeep(source, userdir, importFile);
        } catch (Exception e) {
            try {
                cleanupUserdir(userdir, files1);
            } catch (Exception ee) {
                LOGGER.info("Failed to cleanup after import failure " + ee);    // NOI18N
            }
            throw e;
        }
        
    }
    
    
    private static void cleanupUserdir(File userdir, File[] initialFiles) {
        // Delete newly created directories
        File[] files = userdir.listFiles();
        if (initialFiles.length != files.length) {
            List<File> imported = new ArrayList(Arrays.asList(files));
            imported.removeAll(Arrays.asList(initialFiles));
            for (File f : imported) delete(f);
        }
        
        // Cleanup config directory (may be created but empty before the import)
        File config = new File(userdir, "config");                              // NOI18N
        if (config.isDirectory()) for (File f : config.listFiles()) delete(f);
    }
    
    private static void delete(File file) {
        if (file.isDirectory()) for (File f : file.listFiles()) delete(f);
        file.delete();
    }
    
}
