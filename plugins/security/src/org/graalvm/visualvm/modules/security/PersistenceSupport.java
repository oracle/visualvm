/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
class PersistenceSupport {

    private static final String SNAPSHOT_VERSION = "snapshot_version";  // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION =
                                                CURRENT_SNAPSHOT_VERSION_MAJOR +
                                                SNAPSHOT_VERSION_DIVIDER +
                                                CURRENT_SNAPSHOT_VERSION_MINOR;


    private static final Logger LOGGER =
            Logger.getLogger(PersistenceSupport.class.getName());


    static File chooseLoadFile(String title, File startFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setSelectedFile(startFile);
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) ==
                                   JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile();
        return null;
    }

    static File chooseSaveFile(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow()) ==
                                   JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile();
        return null;
    }


    static void saveToFile(SecurityOptionsPanel panel) {
        final File file = chooseSaveFile(NbBundle.getMessage(
                          PersistenceSupport.class, "MSG_SaveToFile")); // NOI18N
        if (file == null) return;

        final Properties props = new Properties();

        props.put(SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);

        String keyStore = panel.getKeyStore();
        if (keyStore != null) {
            props.put(SecurityModel.KEYSTORE_LOCATION, keyStore);

            char[] keyStorePassword = panel.getKeyStorePassword();
            if (keyStorePassword != null)
                props.put(SecurityModel.KEYSTORE_PASSWORD, new String(keyStorePassword));

            String keyStoreType = panel.getKeyStoreType();
            if (keyStoreType != null)
                props.put(SecurityModel.KEYSTORE_TYPE, keyStoreType);
        }

        String trustStore = panel.getTrustStore();
        if (trustStore != null) {
            props.put(SecurityModel.TRUSTSTORE_LOCATION, trustStore);

            char[] trustStorePassword = panel.getTrustStorePassword();
            if (trustStorePassword != null)
                props.put(SecurityModel.TRUSTSTORE_PASSWORD, new String(trustStorePassword));

            String trustStoreType = panel.getTrustStoreType();
            if (trustStoreType != null)
                props.put(SecurityModel.TRUSTSTORE_TYPE, trustStoreType);
        }

        String enabledProtocols = panel.getEnabledProtocols();
        if (enabledProtocols != null)
            props.put(SecurityModel.ENABLED_PROTOCOLS, enabledProtocols);

        String enabledCipherSuites = panel.getEnabledCipherSuites();
        if (enabledCipherSuites != null)
            props.put(SecurityModel.ENABLED_CIPHER_SUITES, enabledCipherSuites);

        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                boolean saved = saveProperties(props, file);
                if (!saved) {
                    NotifyDescriptor nd = new NotifyDescriptor.Message(
                            NbBundle.getMessage(PersistenceSupport.class,
                            "MSG_FailedSaveToFile"), NotifyDescriptor.ERROR_MESSAGE); // NOI18N
                    DialogDisplayer.getDefault().notifyLater(nd);
                    return;
                }
            }
        });
    }

    static void loadFromFile(final SecurityOptionsPanel panel) {
        final File file = chooseLoadFile(NbBundle.getMessage(PersistenceSupport.class,
                                         "MSG_LoadFromFile"), null); // NOI18N
        if (file == null) return;

        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                final Properties props = loadProperties(file);
                if (props == null) {
                    NotifyDescriptor nd = new NotifyDescriptor.Message(
                            NbBundle.getMessage(PersistenceSupport.class,
                            "MSG_FailedLoadFromFile"), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notifyLater(nd);
                    return;
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        panel.setKeyStore(props.getProperty(SecurityModel.KEYSTORE_LOCATION));
                        String keyStorePassword = props.getProperty(SecurityModel.KEYSTORE_PASSWORD);
                        panel.setKeyStorePassword(keyStorePassword == null ? null : keyStorePassword.toCharArray());
                        panel.setKeyStoreType(props.getProperty(SecurityModel.KEYSTORE_TYPE));
                        panel.setTrustStore(props.getProperty(SecurityModel.TRUSTSTORE_LOCATION));
                        String trustStorePassword = props.getProperty(SecurityModel.TRUSTSTORE_PASSWORD);
                        panel.setTrustStorePassword(trustStorePassword == null ? null : trustStorePassword.toCharArray());
                        panel.setTrustStoreType(props.getProperty(SecurityModel.TRUSTSTORE_TYPE));
                        panel.setEnabledProtocols(props.getProperty(SecurityModel.ENABLED_PROTOCOLS));
                        panel.setEnabledCipherSuites(props.getProperty(SecurityModel.ENABLED_CIPHER_SUITES));
                    }
                });
            }
        });
    }


    private static boolean saveProperties(Properties properties, File file) {
        OutputStream os = null;
        BufferedOutputStream bos = null;
        try {
            os = new FileOutputStream(file);
            bos = new BufferedOutputStream(os);
            properties.storeToXML(os, null);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error storing properties to " + file, e);    // NOI18N
            return false;
        } finally {
            try {
                if (bos != null) bos.close();
                if (os != null) os.close();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Problem closing output stream", e);   // NOI18N
            }
        }
    }

    private static Properties loadProperties(File file) {
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = new FileInputStream(file);
            bis = new BufferedInputStream(is);
            Properties properties = new Properties();
            properties.loadFromXML(bis);
            return properties;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error loading properties from " + file, e);    // NOI18N
            return null;
        } finally {
            try {
                if (bis != null) bis.close();
                if (is != null) is.close();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Problem closing input stream", e);    // NOI18N
            }
        }
    }

}
