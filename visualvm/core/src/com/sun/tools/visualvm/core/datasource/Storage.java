/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.core.datasource;

import com.sun.tools.visualvm.core.datasupport.Utils;
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

/**
 *
 * @author Jiri Sedlacek
 */
public final class Storage {
    
    private static final String TEMPORARY_STORAGE_DIRNAME = "visualvm.dat";
    private static final String PERSISTENT_STORAGE_DIRNAME = "repository";
    
    private static final Logger LOGGER = Logger.getLogger(Storage.class.getName());
    
    public static final String DEFAULT_PROPERTIES_EXT = ".properties";
    
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
    
    
    public Storage(File directory) {
        this(directory, null);
    }
    
    public Storage(File directory, String propertiesFile) {
        if (directory == null) throw new NullPointerException("Directory cannot be null");
        if (directory.isFile()) throw new IllegalArgumentException("Not a valid directory: " + directory);
        this.directory = directory;
        this.propertiesFile = propertiesFile != null ? new File(directory, propertiesFile) : null;
    }
    
    
    public synchronized boolean directoryExists() {
        return directory.exists();
    }
    
    public synchronized File getDirectory() {
        if (!Utils.prepareDirectory(directory)) throw new IllegalStateException("Cannot create storage directory " + directory);
        return directory;
    }
    
    public String getCustomProperty(String key) {
        return getCustomProperties(new String[] { key })[0];
    }
    
    public synchronized String[] getCustomProperties(String[] keys) {
        String[] values = new String[keys.length];
        Properties prop = getCustomProperties();
        for (int i = 0; i < keys.length; i++) values[i] = prop.getProperty(keys[i]);
        return values;
    }
    
    public void setCustomProperty(String key, String value) {
        setCustomProperties(new String[] { key }, new String[] { value });
    }
    
    public synchronized void setCustomProperties(String[] keys, String[] values) {
        Properties prop = getCustomProperties();
        for (int i = 0; i < keys.length; i++) prop.put(keys[i], values[i]);
        storeCustomProperties(); // NOTE: this could be done lazily if storeCustomProperties() was public
    }
    
    public synchronized void saveCustomPropertiesTo(File file) {
        if (file == null) throw new NullPointerException("File cannot be null");
        if (file.isDirectory()) throw new IllegalArgumentException("Not a valid file: " + file);
        
        Properties prop = getCustomProperties();
        if (!prop.isEmpty()) storeProperties(prop, file);
    }
    
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
            if (temporaryStorageDirectoryString == null)
                temporaryStorageDirectoryString = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + File.separator + TEMPORARY_STORAGE_DIRNAME;
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
                    throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString + ", file in the way");
                if (temporaryStorageDirectory.exists() && (!temporaryStorageDirectory.canRead() || !temporaryStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access temporary storage directory " + temporaryStorageString + ", read&write permission required");
                if (!Utils.prepareDirectory(temporaryStorageDirectory))
                    throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString);
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
                persistentStorageDirectoryString = new File(System.getProperty("netbeans.user")).getAbsolutePath() + File.separator + PERSISTENT_STORAGE_DIRNAME;
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
                    throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString + ", file in the way");
                if (persistentStorageDirectory.exists() && (!persistentStorageDirectory.canRead() || !persistentStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access persistent storage directory " + persistentStorageString + ", read&write permission required");
                if (!Utils.prepareDirectory(persistentStorageDirectory))
                    throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString);
            }
            return persistentStorageDirectory;
        }
    }
    
    public static boolean persistentStorageDirectoryExists() {
        return new File(getPersistentStorageDirectoryString()).isDirectory();
    }
    
    
    private void storeCustomProperties() {
        if (properties != null && propertiesFile != null) storeProperties(properties, propertiesFile);
    }
    
    private Properties getCustomProperties() {
        if (properties == null && propertiesFile != null) properties = loadProperties(propertiesFile);
        if (properties == null) properties = new Properties();
        return properties;
    }
    
    
    private static Properties loadProperties(File file) {
        if (!file.exists() || !file.isFile()) return null;
            
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = new FileInputStream(file);
            bis = new BufferedInputStream(is);
            Properties properties = new Properties();
            properties.loadFromXML(bis);
            return properties;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading properties", e);
            return null;
        } finally {
            try {
                if (bis != null) bis.close();
                if (is != null) is.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Problem closing input stream", e);
            }
        }
    }
    
    private static void storeProperties(Properties properties, File file) {
        OutputStream os = null;
        BufferedOutputStream bos = null;
        try {
            os = new FileOutputStream(file);
            bos = new BufferedOutputStream(os);
            properties.storeToXML(os, null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error storing properties", e);
        } finally {
            try {
                if (bos != null) bos.close();
                if (os != null) os.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Problem closing output stream", e);
            }
        }
    }

}
