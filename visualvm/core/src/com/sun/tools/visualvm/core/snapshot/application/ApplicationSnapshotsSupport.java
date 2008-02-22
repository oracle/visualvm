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

package com.sun.tools.visualvm.core.snapshot.application;

import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import javax.imageio.ImageIO;

/**
 * Support for application snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationSnapshotsSupport {
    
    private static ApplicationSnapshotsSupport instance;
    
    private static final String SNAPSHOTS_STORAGE_DIRNAME = "snapshots";
    static final String DISPLAY_NAME = "display_name";
    static final String DISPLAY_ICON = "display_icon";
    static final String PROPERTIES_FILE = "_display_properties.xml";
    
    private File snapshotsStorageDirectory;
    private String snapshotsStorageDirectoryString;

    private ApplicationSnapshotProvider snapshotProvider;
    private SnapshotCategory snapshotCategory = new ApplicationSnapshotCategory();


    /**
     * Returns singleton instance of ApplicationSnapshotsSupport.
     * 
     * @return singleton instance of ApplicationSnapshotsSupport.
     */
    public static synchronized ApplicationSnapshotsSupport getInstance() {
        if (instance == null) instance = new ApplicationSnapshotsSupport();
        return instance;
    }
    
    
    /**
     * Returns SnapshotCategory instance for application snapshots.
     * 
     * @return SnapshotCategory instance for application snapshots.
     */
    public SnapshotCategory getCategory() {
        return snapshotCategory;
    }
    
    
    ApplicationSnapshotProvider getSnapshotProvider() {
        return snapshotProvider;
    }
    
    String getSnapshotsStorageDirectoryString() {
        if (snapshotsStorageDirectoryString == null)
            snapshotsStorageDirectoryString = new File(SnapshotsSupport.getInstance().getPersistentStorageDirectory(), SNAPSHOTS_STORAGE_DIRNAME).getAbsolutePath();
        return snapshotsStorageDirectoryString;
    }
    
    File getSnapshotsStorageDirectory() {
        if (snapshotsStorageDirectory == null) {
            String snapshotsStorageString = getSnapshotsStorageDirectoryString();
            snapshotsStorageDirectory = new File(snapshotsStorageString);
            if (snapshotsStorageDirectory.exists() && snapshotsStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create snapshots storage directory " + snapshotsStorageString + ", file in the way");
            if (snapshotsStorageDirectory.exists() && (!snapshotsStorageDirectory.canRead() || !snapshotsStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access snapshots storage directory " + snapshotsStorageString + ", read&write permission required");
            if (!snapshotsStorageDirectory.exists() && !snapshotsStorageDirectory.mkdir())
                throw new IllegalStateException("Cannot create snapshots storage directory " + snapshotsStorageString);
        }
        return snapshotsStorageDirectory;
    }
    
    
    static File saveImage(File directory, String prefix, String type, Image image) {
        File file = getUniqueFile(directory, prefix, "." + type);
        BufferedImage bImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        bImage.createGraphics().drawImage(image, null, null);
        try {
            ImageIO.write(bImage, type, file);
        } catch(Exception e) {
            return null;
        }
        return file;
    }
    
    static Image loadImage(File file) {
        try {
            return ImageIO.read(file);
        } catch(Exception e) {
            System.err.println("Error reading image: " + e.getMessage());
            return null;
        }
    }
    
    private static File getUniqueFile(File directory, String prefix, String suffix) {
        File file = new File(directory, prefix + suffix);
        while (file.exists()) {
            prefix = prefix + "_";
            file = new File(directory, prefix + suffix);
        }
        return file;
    }
    
    static void storeProperties(Properties properties, File directory) {
        File file = new File(directory, PROPERTIES_FILE);
        OutputStream os = null;
        BufferedOutputStream bos = null;
        try {
            os = new FileOutputStream(file);
            bos = new BufferedOutputStream(os);
            properties.storeToXML(os, null);
        } catch (Exception e) {
            System.err.println("Error storing properties: " + e.getMessage());
        } finally {
            try {
                if (bos != null) bos.close();
                if (os != null) os.close();
            } catch (Exception e) {
                System.err.println("Problem closing output stream: " + e.getMessage());
            }
        }
    }
    
    static Properties loadProperties(File directory) {
        File file = new File(directory, PROPERTIES_FILE);
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = new FileInputStream(file);
            bis = new BufferedInputStream(is);
            Properties properties = new Properties();
            properties.loadFromXML(bis);
            return properties;
        } catch (Exception e) {
            System.err.println("Error loading properties: " + e.getMessage());
            return null;
        } finally {
            try {
                if (bis != null) bis.close();
                if (is != null) is.close();
            } catch (Exception e) {
                System.err.println("Problem closing input stream: " + e.getMessage());
            }
        }
    }
    
    
    private ApplicationSnapshotsSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new ApplicationSnapshotDescriptorProvider());
        snapshotProvider = ApplicationSnapshotProvider.sharedInstance();
        
        RegisteredSnapshotCategories.sharedInstance().addCategory(snapshotCategory);
        
        snapshotProvider.initialize();
        ApplicationSnapshotActionProvider.getInstance().initialize();
    }

}
