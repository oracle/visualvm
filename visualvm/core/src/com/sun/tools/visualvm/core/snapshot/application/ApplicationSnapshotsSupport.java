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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * Support for application snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationSnapshotsSupport {
    
    private static ApplicationSnapshotsSupport instance;
    
    private static final String SNAPSHOTS_STORAGE_DIRNAME = "snapshots";
    static final String PROPERTIES_FILE = "_application_snapshot.properties";
    
    private static final int COPY_PACKET_SIZE = 4096;
    
    private File snapshotsStorageDirectory;
    private String snapshotsStorageDirectoryString;

    private ApplicationSnapshotProvider snapshotProvider;
    private ApplicationSnapshotCategory snapshotCategory = new ApplicationSnapshotCategory();


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
    
    ApplicationSnapshotCategory getApplicationSnapshotCategory() {
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
    
    static void createArchive(File directory, File archive) {        
        ZipOutputStream zos = null;
        FileInputStream fis = null;
        
        File[] contents = directory.listFiles();
        
        try {
            zos = new ZipOutputStream(new FileOutputStream(archive));
            for (File file : contents) {
                if (file.isFile()) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    try {
                        fis = new FileInputStream(file);
                        int bytes;
                        byte[] packet = new byte[COPY_PACKET_SIZE];
                        while ((bytes = fis.read(packet, 0, COPY_PACKET_SIZE)) != -1) zos.write(packet, 0, bytes);
                    } finally {
                        if (zos != null) zos.closeEntry();
                        try { if (fis != null) fis.close(); } catch (Exception e) { System.err.println("Problem closing archived file stream: " + e.getMessage()); }
                    }
                } else {
                    // TODO: process directory
                }
            }
        } catch (Exception e) {
            System.err.println("Error archiving snapshot: " + e.getMessage());
        } finally {
            try { if (zos != null) zos.close(); } catch (Exception e) { System.err.println("Problem closing archive stream: " + e.getMessage()); }
        }
    }
    
    static File extractArchive(File archive, File destination) {
        // TODO: implement extracting directories
        
        File directory = new File(destination, archive.getName());
        ZipFile zipFile = null;
        
        try {
            directory.mkdirs();
            
            zipFile = new ZipFile(archive);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                FileOutputStream fos = null;
                InputStream is = null;
                try {
                    is = zipFile.getInputStream(entry);
                    fos = new FileOutputStream(new File(directory, entry.getName()));
                    int bytes;
                    byte[] packet = new byte[COPY_PACKET_SIZE];
                    while ((bytes = is.read(packet, 0, COPY_PACKET_SIZE)) != -1) fos.write(packet, 0, bytes);
                } finally {
                    try { if (fos != null) fos.close(); } catch (Exception e) { System.err.println("Problem closing extracted file stream: " + e.getMessage()); }
                    try { if (is != null) is.close(); } catch (Exception e) { System.err.println("Problem closing zipentry stream: " + e.getMessage()); }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting snapshot: " + e.getMessage());
            return null;
        } finally {
            try { if (zipFile != null) zipFile.close(); } catch (Exception e) { System.err.println("Problem closing archive: " + e.getMessage()); }
        }
        
        return directory;
    }
    
    
    private ApplicationSnapshotsSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new ApplicationSnapshotDescriptorProvider());
        snapshotProvider = ApplicationSnapshotProvider.sharedInstance();
        
        RegisteredSnapshotCategories.sharedInstance().addCategory(snapshotCategory);
        
        snapshotProvider.initialize();
        ApplicationSnapshotActionProvider.getInstance().initialize();
    }

}
