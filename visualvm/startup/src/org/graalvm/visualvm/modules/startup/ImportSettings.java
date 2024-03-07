/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
 *     - add -J-Dvisualvm.import_userdir=... to visualvm/nbproject/project.properties
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
    // SHOULD NOT BE EMPTY! At least the current release should be listed
    //
    // First supported release userdir is the first one
    // Most recent release userdir is the last one
    private static final String[] SUPPORTED_USERDIRS = new String[] {
        "2.0",                                                                  // NOI18N
        "2.0.1",                                                                // NOI18N
        "2.0.2",                                                                // NOI18N
        "2.0.3",                                                                // NOI18N
        "2.0.4",                                                                // NOI18N
        "2.0.5",                                                                // NOI18N
        "2.0.6",                                                                // NOI18N
        "2.0.7",                                                                // NOI18N
        "2.1",                                                                  // NOI18N
        "2.1.1",                                                                // NOI18N
        "2.1.2",                                                                // NOI18N
        "2.1.3",                                                                // NOI18N
        "2.1.4",                                                                // NOI18N
        "2.1.5",                                                                // NOI18N
        "2.1.6",                                                                // NOI18N
        "2.1.7",                                                                // NOI18N
        "2.1.8"                                                                 // NOI18N
    };
    
    
    private final static Logger LOGGER = Logger.getLogger(ImportSettings.class.getName());
    
    
    public static void main(String[] args) throws Exception {
        // Check current VisualVM userdir
        String userdirS = System.getProperty("netbeans.user");                  // NOI18N
        final File userdir = userdirS == null || userdirS.isEmpty() ? null : new File(userdirS);
        if (userdir == null || !userdir.isDirectory()) {
            LOGGER.info("Skipping import, could not resolve VisualVM userdir: " + userdirS); // NOI18N
            return;
        }
        
        // Immediate import based on the provided system property
        String importUserdirS = System.getProperty("visualvm.import_userdir");  // NOI18N
        if (importUserdirS != null && !importUserdirS.isEmpty()) {
            File importUserdir = new File(importUserdirS);
            
            String msg;
            if (!importUserdir.isDirectory()) msg = "not a directory";          // NOI18N
            else if (userdir.equals(importUserdir)) msg = "own userdir";        // NOI18N
            else msg = null;
            
            if (msg == null) copyToUserdir(importUserdir, userdir);
            else LOGGER.info("Skipping import from visualvm.import_userdir, wrong directory provided (" + msg + "): " + importUserdirS); // NOI18N
            
            return;
        }
        
        // Check VisualVM userdirs root
        String userdirsRootS = System.getProperty("netbeans.default_userdir_root"); // NOI18
///*DEV*/ if (userdirsRootS == null || userdirsRootS.isEmpty()) userdirsRootS = System.getProperty("netbeans.default_userdir_root.dev"); // NOI18
        File userdirsRoot = userdirsRootS == null || userdirsRootS.isEmpty() ? null : new File(userdirsRootS);
        if (userdirsRoot == null || !userdirsRoot.isDirectory()) {
            LOGGER.info("Skipping import, could not resolve VisualVM userdirs root: " + userdirsRootS); // NOI18N
            return;
        }

        // Read available userdirs supported for import
        List<File> userdirs = availableUserdirs(userdirsRoot, userdir);
        if (userdirs.isEmpty()) {
            LOGGER.info("Skipping import, no supported userdirs found in: " + userdirsRootS); // NOI18N
            return;
        }
        

        File latestRelease = latestReleaseUserdir(userdirs);
        File recentlyUsed = lastRecentlyUsedUserdir(userdirs);


        // Interactive selection & import of the userdir
        Utils.setSystemLaF();

        final JDialog d = StartupDialog.create(NbBundle.getMessage(
                ImportSettings.class, "ImportSettings_Caption"), null, -1); // NOI18N

        ImportPanel p = new ImportPanel(latestRelease, recentlyUsed, userdirsRoot, SUPPORTED_USERDIRS[0]) {
            @Override
            boolean isSupportedImport(File dir) {
                return isSupportedUserdir(dir, userdir);
            }
            
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
                copyToUserdir(source, userdir);
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
    
    
    private static List<File> availableUserdirs(File userdirsRoot, File userdir) {
        List<File> userdirs = new ArrayList<>();
        
        for (String supported : SUPPORTED_USERDIRS) {
            File available = availableUserdir(userdirsRoot, supported, userdir);
            if (available != null) userdirs.add(available);
        }
        
        return userdirs;
    }
    
    private static File availableUserdir(File userdirsFolder, String dir, File current) {
        File file = new File(userdirsFolder, dir);
        return isSupportedUserdir(file, current) ? file : null;
    }
    
    private static boolean isSupportedUserdir(File userdir, File current) {
        return !current.equals(userdir) &&
               userdir.isDirectory() &&
               new File(userdir, "config").isDirectory();                       // NOI18N
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
     * patterns in etc/visualvm.import file. */
    private static void copyToUserdir(File source, File userdir) throws IOException, PropertyVetoException {
        File visualvmDir = InstalledFileLocator.getDefault().locate("modules", null, false).getParentFile().getParentFile(); // NOI18N
        
        File importFile = new File(visualvmDir, "etc/visualvm.import");         // NOI18N
///*DEV*/ if (!importFile.isFile() && "testuserdir".equals(userdir.getName())) {  // NOI18N
///*DEV*/     File parent = visualvmDir.getParentFile();
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
                LOGGER.info("Failed to cleanup after import failure: " + ee);    // NOI18N
            }
            throw e;
        }
        System.setProperty("plugin.manager.import.from", source.getAbsolutePath()); // NOI18N
    }
    
    
    private static void cleanupUserdir(File userdir, File[] initialFiles) {
        // Delete newly created directories
        File[] files = userdir.listFiles();
        if (initialFiles.length != files.length) {
            List<File> imported = new ArrayList<>(Arrays.asList(files));
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
