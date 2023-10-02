/*
 *  Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.datasource;

import org.graalvm.visualvm.core.datasupport.Utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage for a DataSource.
 *
 * @author Jiri Sedlacek
 */
public final class Storage {

    private static final String VISUALVM_TMP_DIR = System.getProperty("visualvm.tmpdir");  // NOI18N
    private static final String TEMPORARY_STORAGE_DIRNAME = "visualvm.dat";  // NOI18N
    private static final String TEMPORARY_STORAGE_DIRNAME_EX = "visualvm_{0}.dat";  // NOI18N
    private static final String PERSISTENT_STORAGE_DIRNAME = "repository";  // NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(Storage.class.getName());
    
    /**
     * Default extension for storage file.
     */
    public static final String DEFAULT_PROPERTIES_EXT = ".properties";  // NOI18N
    
    private static final Object temporaryStorageDirectoryLock = new Object();
    // @GuardedBy temporaryStorageDirectory
    private static File temporaryStorageDirectory;
    private static final Object temporaryStorageDirectoryStringLock = new Object();
    // @GuardedBy temporaryStorageDirectoryString
    private static String temporaryStorageDirectoryString;
    private static final Object persistentStorageDirectoryLock = new Object();
    // @GuardedBy persistentStorageDirectory
    private static File persistentStorageDirectory;
    private static final Object persistentStorageDirectoryStringLock = new Object();
    // @GuardedBy persistentStorageDirectoryString
    private static String persistentStorageDirectoryString;
    
    private final File directory;
    private final File propertiesFile;
    
    private Properties properties;


    /**
     * Creates new instance of Storage for storing temporary data. The Storage
     * directory is initialized by getTemporaryStorageDirectory() value.
     */
    public Storage() {
        this(new File(getTemporaryStorageDirectoryString())); // Do not create immediately
    }
    
    /**
     * Creates new instance of Storage.
     * 
     * @param directory directory where storage data will be stored.
     */
    public Storage(File directory) {
        this(directory, null);
    }
    
    /**
     * Creates new instance of Storage.
     * 
     * @param directory directory where storage data will be stored.
     * @param propertiesFile filename of storage file.
     */
    public Storage(File directory, String propertiesFile) {
        if (directory == null) throw new NullPointerException("Directory cannot be null");  // NOI18N
        if (directory.isFile()) throw new IllegalArgumentException("Not a valid directory: " + directory);  // NOI18N
        this.directory = directory;
        this.propertiesFile = propertiesFile != null ? new File(directory, propertiesFile) : null;
    }
    
    
    /**
     * Returns true if storage directory exists, false otherwise.
     * 
     * @return true if storage directory exists, false otherwise.
     */
    public synchronized boolean directoryExists() {
        return directory.exists();
    }
    
    /**
     * Returns storage directory.
     * 
     * @return storage directory.
     */
    public synchronized File getDirectory() {
        if (!Utils.prepareDirectory(directory)) throw new IllegalStateException("Cannot create storage directory " + directory);    // NOI18N
        return directory;
    }
    
    /**
     * Returns defined custom property.
     * 
     * @param key property name.
     * @return defined custom property.
     */
    public String getCustomProperty(String key) {
        return getCustomProperties(new String[] { key })[0];
    }
    
    /**
     * Returns defined custom properties.
     * 
     * @param keys property names.
     * @return defined custom properties.
     */
    public synchronized String[] getCustomProperties(String[] keys) {
        String[] values = new String[keys.length];
        Properties prop = getCustomProperties(false);
        if (prop != null)
            for (int i = 0; i < keys.length; i++)
                    values[i] = prop.getProperty(keys[i]);
        return values;
    }
    
    /**
     * Sets persistent custom property.
     * Since VisualVM 1.2 clears the property for null value.
     * 
     * @param key property name.
     * @param value property value or (since VisualVM 1.2) null
     */
    public void setCustomProperty(String key, String value) {
        setCustomProperties(new String[] { key }, new String[] { value });
    }
    
    /**
     * Sets persistent custom properties.
     * Since VisualVM 1.2 a property is cleared for null value.
     * 
     * @param keys property names.
     * @param values property values.
     */
    public synchronized void setCustomProperties(String[] keys, String[] values) {
        Properties prop = getCustomProperties(true);
        for (int i = 0; i < keys.length; i++)
            if (values[i] != null) prop.put(keys[i], values[i]);
            else prop.remove(keys[i]);
        storeCustomProperties(); // NOTE: this could be done lazily if storeCustomProperties() was public
    }

    /**
     * Clears custom property.
     *
     * @param key property name
     */
    public void clearCustomProperty(String key) {
        clearCustomProperties(new String[] { key });
    }

    /**
     * Clears custom properties.
     *
     * @param keys property names
     */
    public synchronized void clearCustomProperties(String[] keys) {
        Properties prop = getCustomProperties(false);
        if (prop != null)
            for (String key : keys) {
                prop.remove(key);
            }
        storeCustomProperties(); // NOTE: this could be done lazily if storeCustomProperties() was public
    }

    /**
     * Returns true if the Storage contains any custom properties, false otherwise.
     *
     * @return true if the Storage contains any custom properties, false otherwise
     */
    public synchronized boolean hasCustomProperties() {
        Properties prop = getCustomProperties(false);
        return prop != null && !prop.isEmpty();
    }
    
    /**
     * Saves persistent custom properties to a file.
     * 
     * @param file file where the properties will be saved.
     */
    public synchronized void saveCustomPropertiesTo(File file) {
        if (file == null) throw new NullPointerException("File cannot be null");    // NOI18N
        if (file.isDirectory()) throw new IllegalArgumentException("Not a valid file: " + file);    // NOI18N
        
        Properties prop = getCustomProperties(false);
        if (prop != null && !prop.isEmpty()) storeProperties(prop, file);
    }
    
    /**
     * Deletes properties file.
     */
    public synchronized void deleteCustomPropertiesStorage() {
        if (propertiesFile != null && propertiesFile.exists())
            if (!propertiesFile.delete()) propertiesFile.deleteOnExit();
    }
    
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource data
     * 
     * @return default storage directory for temporary (runtime) DataSource data
     */
    public static String getTemporaryStorageDirectoryString() {
        synchronized(temporaryStorageDirectoryStringLock) {
            if (temporaryStorageDirectoryString == null) {
                if (VISUALVM_TMP_DIR != null) {
                    temporaryStorageDirectoryString = new File(VISUALVM_TMP_DIR).getAbsolutePath() +
                                                      File.separator + TEMPORARY_STORAGE_DIRNAME;
                } else {
                    String tmpDir = System.getProperty("java.io.tmpdir"); // NOI18N
                    String storageDir;
                    
                    String userDir = System.getProperty("user.home"); // NOI18N
                    if (userDir != null && !userDir.isEmpty()) {
                        String userName = new File(userDir).getName();
                        storageDir = MessageFormat.format(TEMPORARY_STORAGE_DIRNAME_EX, userName);
                    } else {
                        storageDir = TEMPORARY_STORAGE_DIRNAME;
                    }
                    
                    temporaryStorageDirectoryString = new File(tmpDir).getAbsolutePath() +
                                                      File.separator + storageDir;
                }
            }
            return temporaryStorageDirectoryString;
        }
    }
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource data.
     * This directory is deleted when VisualVM session finishes, eventually on
     * new VisualVM session startup.
     * 
     * @return default storage directory for temporary (runtime) DataSource data
     */
    public static File getTemporaryStorageDirectory() {
        synchronized(temporaryStorageDirectoryLock) {
            if (temporaryStorageDirectory == null) {
                String temporaryStorageString = getTemporaryStorageDirectoryString();
                temporaryStorageDirectory = new File(temporaryStorageString);
                if (temporaryStorageDirectory.exists() && temporaryStorageDirectory.isFile())
                    throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString + ", file in the way");   // NOI18N
                if (temporaryStorageDirectory.exists() && (!temporaryStorageDirectory.canRead() || !temporaryStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access temporary storage directory " + temporaryStorageString + ", read&write permission required");    // NOI18N
                if (!Utils.prepareDirectory(temporaryStorageDirectory))
                    throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString); // NOI18N
            }
            return temporaryStorageDirectory;
        }
    }
    
    /**
     * Returns default storage directory for persistent DataSource data
     * 
     * @return default storage directory for persistent DataSource data
     */
    public static String getPersistentStorageDirectoryString() {
        synchronized(persistentStorageDirectoryStringLock) {
            if (persistentStorageDirectoryString == null)
                persistentStorageDirectoryString = new File(System.getProperty("netbeans.user")).getAbsolutePath() + File.separator + PERSISTENT_STORAGE_DIRNAME;   // NOI18N
            return persistentStorageDirectoryString;
        }
    }
    
    /**
     * Returns default storage directory for persistent DataSource data
     * 
     * @return default storage directory for persistent DataSource data
     */
    public static File getPersistentStorageDirectory() {
        synchronized(persistentStorageDirectoryLock) {
            if (persistentStorageDirectory == null) {
                String persistentStorageString = getPersistentStorageDirectoryString();
                persistentStorageDirectory = new File(persistentStorageString);
                if (persistentStorageDirectory.exists() && persistentStorageDirectory.isFile())
                    throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString + ", file in the way"); // NOI18N
                if (persistentStorageDirectory.exists() && (!persistentStorageDirectory.canRead() || !persistentStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access persistent storage directory " + persistentStorageString + ", read&write permission required");  // NOI18N
                if (!Utils.prepareDirectory(persistentStorageDirectory))
                    throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString);   // NOI18N
            }
            return persistentStorageDirectory;
        }
    }
    
    /**
     * Returns true if persistent storage directory exists, false otherwise.
     * @return true if persistent storage directory exists, false otherwise.
     */
    public static boolean persistentStorageDirectoryExists() {
        return new File(getPersistentStorageDirectoryString()).isDirectory();
    }
    
    
    private void storeCustomProperties() {
        if (properties != null && propertiesFile != null) storeProperties(properties, propertiesFile);
    }
    
    private Properties getCustomProperties(boolean createEmpty) {
        if (properties == null && propertiesFile != null) properties = loadProperties(propertiesFile);
        if (properties == null && createEmpty) properties = new Properties();
        return properties;
    }
    
    
    private static Properties loadProperties(File file) {
        if (!file.exists() || !file.isFile()) return null;
            
        try (InputStream is = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(is)) {
            Properties properties = new Properties();
            properties.loadFromXML(bis);
            return properties;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading properties", e);    // NOI18N
            return null;
        }
    }
    
    private static void storeProperties(Properties properties, File file) {
        Utils.prepareDirectory(file.getParentFile()); // Directories may not be created yet

        try (OutputStream os = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            properties.storeToXML(bos, null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error storing properties", e);    // NOI18N
        }
    }

}
