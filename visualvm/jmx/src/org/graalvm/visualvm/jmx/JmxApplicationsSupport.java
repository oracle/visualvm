/*
 *  Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.jmx;

import java.io.File;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.jmx.impl.JmxApplicationProvider;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 * Support for creating JMX applications in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class JmxApplicationsSupport {
    
    private static final String STORAGE_DIRNAME = "jmxapplications";    // NOI18N
    
    private static final Object storageDirectoryLock = new Object();
    // @GuardedBy storageDirectoryLock
    private static File storageDirectory;
    private static final Object storageDirectoryStringLock = new Object();
    // @GuardedBy storageDirectoryStringLock
    private static String storageDirectoryString;

    private static JmxApplicationsSupport instance;

    private JmxApplicationProvider applicationProvider = new JmxApplicationProvider();


    /**
     * Returns singleton instance of JmxApplicationsSupport.
     *
     * @return singleton instance of JmxApplicationsSupport.
     */
    public static synchronized JmxApplicationsSupport getInstance() {
        if (instance == null) instance = new JmxApplicationsSupport();
        return instance;
    }


    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. The application won't be restored on another VisualVM
     * sessions. Throws a JmxApplicationException if the application cannot be created.
     *
     * Note that even though the created application won't be restored for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param username username for the connection, may be null
     * @param password password for the connection, may be null
     * @return created JMX application
     * @throws JmxApplicationException if creating the application failed
     */
    public Application createJmxApplication(String connectionString, String displayName,
                                            String username, String password) throws JmxApplicationException {

        return createJmxApplication(connectionString, displayName, username,
                                    password, false, false);
    }

    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. Throws a JmxApplicationException if the application
     * cannot be created.
     *
     * Note that even if the created application isn't persistent for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param username username for the connection, may be null
     * @param password password for the connection, may be null
     * @param saveCredentials if persistent, controls whether the username and password should be persisted for another VisualVM sessions
     * @param persistent controls whether the application definition will be persisted for another VisualVM sessions
     * @return created JMX application
     * @throws JmxApplicationException if creating the application failed
     */
    public Application createJmxApplication(String connectionString,
                                            String displayName, String username,
                                            String password, boolean saveCredentials,
                                            boolean persistent) throws JmxApplicationException {

        if (username == null) username = ""; // NOI18N
        if (password == null) password = ""; // NOI18N
        
        String suggestedName = JmxApplicationProvider.getSuggestedName(displayName,
                connectionString, username);

        EnvironmentProvider epr = new CredentialsProvider.Custom(username,
                password.toCharArray(), saveCredentials);
        return createJmxApplicationImpl(connectionString, displayName, suggestedName,
                                        epr, persistent, false, true, true);
    }

    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. Throws a JmxApplicationException if the application
     * cannot be created.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param provider JMX EnvironmentProvider for the Application
     * @param persistent controls whether the application definition will be persisted for another VisualVM sessions
     * @return created JMX application
     * @throws JmxApplicationException if creating the application failed
     */
    public Application createJmxApplication(String connectionString,
                                            String displayName,
                                            EnvironmentProvider provider,
                                            boolean persistent) throws JmxApplicationException {

        String username = getUsername(provider);
        String suggestedName = JmxApplicationProvider.getSuggestedName(displayName,
                connectionString, username);
        return createJmxApplicationImpl(connectionString, displayName, suggestedName,
                                        provider, persistent, false, true, true);
    }

    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. The application won't be restored on another VisualVM
     * sessions. Displays progress during application creation and opens an error
     * dialog if creating the application failed. Throws a JmxApplicationException
     * if the application cannot be created.
     *
     * Note that even though the created application won't be restored for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param username username for the connection, may be null
     * @param password password for the connection, may be null
     * @return created JMX application or null if creating the application failed
     */
    public Application createJmxApplicationInteractive(String connectionString, String displayName,
                                            String username, String password) {

        return createJmxApplicationInteractive(connectionString, displayName, username,
                                    password, false, false);
    }

    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. Displays progress during application creation and
     * opens an error dialog if creating the application failed.
     *
     * Note that even if the created application isn't persistent for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param username username for the connection, may be null
     * @param password password for the connection, may be null
     * @param saveCredentials if persistent, controls whether the username and password should be persisted for another VisualVM sessions
     * @param persistent controls whether the application definition will be persisted for another VisualVM sessions
     * @return created JMX application or null if creating the application failed
     */
    public Application createJmxApplicationInteractive(String connectionString,
                                            String displayName, String username,
                                            String password, boolean saveCredentials,
                                            boolean persistent) {

        return createJmxApplicationInteractive(connectionString, displayName, username, password,
                                               saveCredentials, persistent, true, true);
    }
    
    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. Displays progress during application creation and
     * opens an error dialog if creating the application failed.
     *
     * Note that even if the created application isn't persistent for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param username username for the connection, may be null
     * @param password password for the connection, may be null
     * @param saveCredentials if persistent, controls whether the username and password should be persisted for another VisualVM sessions
     * @param persistent controls whether the application definition will be persisted for another VisualVM sessions
     * @param connectImmediately true if the JMX connection should be attempted immediately after submitting, false otherwise
     * @param connectAutomatically true if the JMX connection should be made automatically whenever the target application is available, false otherwise
     * @return created JMX application or null if creating the application failed
     */
    public Application createJmxApplicationInteractive(String connectionString,
                                            String displayName, String username,
                                            String password, boolean saveCredentials,
                                            boolean persistent, boolean connectImmediately,
                                            boolean connectAutomatically) {

        if (username == null) username = ""; // NOI18N
        if (password == null) password = ""; // NOI18N
        
        final ProgressHandle[] pHandle = new ProgressHandle[1];
        try {
            final String suggestedName = JmxApplicationProvider.getSuggestedName(
                    displayName, connectionString, username);
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        pHandle[0] = ProgressHandle.createHandle(
                                NbBundle.getMessage(JmxApplicationsSupport.class,
                                                    "LBL_Adding", suggestedName)); // NOI18N
                        pHandle[0].setInitialDelay(0);
                        pHandle[0].start();
                    }
                });
            EnvironmentProvider epr = new CredentialsProvider.Custom(username,
                password.toCharArray(), saveCredentials);
            return createJmxApplicationImpl(connectionString, displayName,
                                            suggestedName, epr, persistent, false,
                                            connectImmediately, connectAutomatically);
        } catch (JmxApplicationException e) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(e.
                    getMessage(), NotifyDescriptor.ERROR_MESSAGE));
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (pHandle[0] != null) {
                        pHandle[0].finish();
                    }
                }
            });
        }
        return null;
    }
    
    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. Displays progress during application creation and
     * opens an error dialog if creating the application failed.
     *
     * Note that even if the created application isn't persistent for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param provider JMX EnvironmentProvider for the Application
     * @param persistent controls whether the application definition will be persisted for another VisualVM sessions
     * @return created JMX application or null if creating the application failed
     */
    public Application createJmxApplicationInteractive(String connectionString,
                                            String displayName,
                                            EnvironmentProvider provider,
                                            boolean persistent) {

        return createJmxApplicationInteractive(connectionString, displayName,
                                            provider, persistent, false);
    }

    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. Displays progress during application creation and
     * opens an error dialog if creating the application failed.
     *
     * Note that even if the created application isn't persistent for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param provider JMX EnvironmentProvider for the Application
     * @param persistent controls whether the application definition will be persisted for another VisualVM sessions
     * @param allowsInsecure true if SSL is not required for the connection, false otherwise
     * @return created JMX application or null if creating the application failed
     */
    public Application createJmxApplicationInteractive(String connectionString,
                                            String displayName,
                                            EnvironmentProvider provider,
                                            boolean persistent, boolean allowsInsecure) {

        return createJmxApplicationInteractive(connectionString, displayName, provider, persistent,
                                               allowsInsecure, true, true);
    }
    
    /**
     * Creates new Application defined by JMX connection and adds it to the
     * Applications tree. Displays progress during application creation and
     * opens an error dialog if creating the application failed.
     *
     * Note that even if the created application isn't persistent for another
     * VisualVM sessions, the host created for this application will be restored.
     *
     * @param connectionString definition of the connection, for example hostname:port
     * @param displayName display name for the application, may be null
     * @param provider JMX EnvironmentProvider for the Application
     * @param persistent controls whether the application definition will be persisted for another VisualVM sessions
     * @param allowsInsecure true if SSL is not required for the connection, false otherwise
     * @param connectImmediately true if the JMX connection should be attempted immediately after submitting, false otherwise
     * @param connectAutomatically true if the JMX connection should be made automatically whenever the target application is available, false otherwise
     * @return created JMX application or null if creating the application failed
     */
    public Application createJmxApplicationInteractive(String connectionString,
                                            String displayName,
                                            EnvironmentProvider provider,
                                            boolean persistent, boolean allowsInsecure,
                                            boolean connectImmediately, boolean connectAutomatically) {

        final ProgressHandle[] pHandle = new ProgressHandle[1];
        try {
            String username = getUsername(provider);
            final String suggestedName = JmxApplicationProvider.getSuggestedName(
                    displayName, connectionString, username);
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        pHandle[0] = ProgressHandle.createHandle(
                                NbBundle.getMessage(JmxApplicationsSupport.class,
                                                    "LBL_Adding", suggestedName)); // NOI18N
                        pHandle[0].setInitialDelay(0);
                        pHandle[0].start();
                    }
                });
            return createJmxApplicationImpl(connectionString, displayName, suggestedName,
                                            provider, persistent, allowsInsecure,
                                            connectImmediately, connectAutomatically);
        } catch (JmxApplicationException e) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(e.
                    getMessage(), NotifyDescriptor.ERROR_MESSAGE));
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (pHandle[0] != null) {
                        pHandle[0].finish();
                    }
                }
            });
        }
        return null;
    }

    private Application createJmxApplicationImpl(String connectionString,
                                            String displayName, String suggestedName,
                                            EnvironmentProvider provider,
                                            boolean persistent, boolean allowsInsecure,
                                            boolean connectImmediately, boolean connectAutomatically)
                                            throws JmxApplicationException {

        return applicationProvider.createJmxApplication(connectionString, displayName, suggestedName,
                provider, persistent, allowsInsecure, connectImmediately, connectAutomatically);
    }
    
    private static String getUsername(EnvironmentProvider provider) {
        return provider instanceof CredentialsProvider.Custom ?
                ((CredentialsProvider.Custom)provider).getUsername(null) : null;
    }
    
    static String getStorageDirectoryString() {
        synchronized(storageDirectoryStringLock) {
            if (storageDirectoryString == null)
                storageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + STORAGE_DIRNAME;
            return storageDirectoryString;
        }
    }

    /**
     * Returns storage directory for defined JMX applications.
     *
     * @return storage directory for defined JMX applications.
     */
    public static File getStorageDirectory() {
        synchronized(storageDirectoryLock) {
            if (storageDirectory == null) {
                String storageString = getStorageDirectoryString();
                storageDirectory = new File(storageString);
                if (storageDirectory.exists() && storageDirectory.isFile())
                    throw new IllegalStateException("Cannot create hosts storage directory " + storageString + ", file in the way");    // NOI18N
                if (storageDirectory.exists() && (!storageDirectory.canRead() || !storageDirectory.canWrite())) 
                    throw new IllegalStateException("Cannot access hosts storage directory " + storageString + ", read&write permission required"); // NOI18N
                if (!Utils.prepareDirectory(storageDirectory))
                    throw new IllegalStateException("Cannot create hosts storage directory " + storageString);  // NOI18N
            }
            return storageDirectory;
        }
    }

    /**
     * Returns true if the storage directory for defined JMX applications already exists, false otherwise.
     *
     * @return true if the storage directory for defined JMX applications already exists, false otherwise.
     */
    public static boolean storageDirectoryExists() {
        return new File(getStorageDirectoryString()).isDirectory();
    }


    private JmxApplicationsSupport() {
        applicationProvider.initialize();
    }
}
