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

package com.sun.tools.visualvm.core.datasupport;

import com.sun.tools.visualvm.core.datasource.DataSource;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
public final class Utils {
    
    public static final RequestProcessor FILE_QUEUE = new RequestProcessor("File Queue");
    
    private static final int COPY_PACKET_SIZE = 16384;
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    public static <X, Y> boolean containsSubclass(Set<? extends Class<? extends Y>> classes, X superclassInstance) {
        for (Class<? extends Y> classs : classes) if (classs.isInstance(superclassInstance)) return true;
        return false;
    }

    public static <X, Y> boolean containsSuperclass(Set<? extends Class<? extends Y>> classes, X subclassInstance) {
        Class subclass = subclassInstance.getClass();
        for (Class<? extends Y> classs : classes) if (classs.isAssignableFrom(subclass)) return true;
        return false;
    }

    public static <X, Y extends X, Z extends X> Set<Z> getFilteredSet(Set<Y> set, Class<Z> filter) {
        Set<Z> filteredSet = new HashSet();
        for (Y item : set) if (filter.isInstance(item)) filteredSet.add((Z)item);
        return filteredSet;
    }
    
    public static <X extends DataSource> List<X> getSortedDataSources(Set<X> dataSources) {
        List<DataSourcePath<X>> dataSourcePaths = getSortedDataSourcePaths(dataSources);
        List<X> sortedDataSources = new ArrayList();
        
        for (DataSourcePath<X> dataSourcePath : dataSourcePaths)
            sortedDataSources.add(dataSourcePath.getDataSource());
        
        return sortedDataSources;
    }
    
    public static <X extends DataSource> boolean areDataSourcesIndependent(Set<X> dataSources) {
        return dataSources.size() == getIndependentDataSources(dataSources).size();
    }
    
    public static <X extends DataSource> Set<X> getIndependentDataSources(Set<X> dataSources) {
        Map<Integer, Set<X>> independentDataSourcesMap = new HashMap();
        List<DataSourcePath<X>> dataSourcePaths = getSortedDataSourcePaths(dataSources);
        
        for (DataSourcePath<X> dataSourcePath : dataSourcePaths) {
            boolean independent = true;
            for (int i = 0; i < dataSourcePath.size(); i++) {
                DataSource dataSource = dataSourcePath.get(i);
                Set<X> set = independentDataSourcesMap.get(i);
                if (set != null && set.contains(dataSource)) {
                    independent = false;
                    break;
                }
            }
            
            if (independent) {
                Set<X> set = independentDataSourcesMap.get(dataSourcePath.size() - 1);
                if (set == null) {
                    set = new HashSet();
                    independentDataSourcesMap.put(dataSourcePath.size() - 1, set);
                }
                set.add(dataSourcePath.getDataSource());
            }
        }
        
        Set<X> independentDataSources = new HashSet();
        Collection<Set<X>> independentSetsCollection = independentDataSourcesMap.values();
        for (Set<X> independentSet : independentSetsCollection)
            independentDataSources.addAll(independentSet);
        return independentDataSources;
    }
    
    private static <X extends DataSource> List<DataSourcePath<X>> getSortedDataSourcePaths(Set<X> dataSources) {
        List<DataSourcePath<X>> dataSourcePaths = new ArrayList();
        for (DataSource dataSource : dataSources) dataSourcePaths.add(new DataSourcePath(dataSource));
        Collections.sort(dataSourcePaths);
        return dataSourcePaths;
    }
    
    private static class DataSourcePath<X extends DataSource> extends ArrayList<DataSource> implements Comparable<DataSourcePath> {
        
        public DataSourcePath(X dataSource) {
            super();
            DataSource ds = dataSource;
            while(ds != null) {
                add(0, ds);
                ds = ds.getOwner();
            }
        }

        public int compareTo(DataSourcePath dataSourcePath) {
            Integer thisSize = size();
            return thisSize.compareTo(dataSourcePath.size());
        }
        
        public X getDataSource() {
            return (X)get(size() - 1);
        }
        
    }
    
    
    public static String getFileBase(String fileName) {
        int extIndex = fileName.lastIndexOf(".");
        if (extIndex == -1) return fileName;
        return fileName.substring(0, extIndex);
    }
    
    public static String getFileExt(String fileName) {
        int extIndex = fileName.lastIndexOf(".");
        if (extIndex == -1) return "";
        return fileName.substring(extIndex);
    }
    
    public static File getUniqueFile(File directory, String file) {
        return getUniqueFile(directory, getFileBase(file), getFileExt(file));
    }
    
    public synchronized static File getUniqueFile(File directory, String fileName, String fileExt) {
        File newFile = new File(directory, fileName + fileExt);
        while (newFile.exists()) {
            fileName = fileName + "_";
            newFile = new File(directory, fileName + fileExt);
        }
        return newFile;
    }
    
    public static synchronized boolean prepareDirectory(File directory) {
        if (directory.exists()) return true;
        directory.mkdirs();
        return directory.exists();
    }
    
    public static boolean copyFile(File file, File copy) {
        if (file == null || copy == null) throw new NullPointerException("File cannot be null");
        if (!file.isFile() || copy.isDirectory()) throw new IllegalArgumentException("Not a valid file");        
        
        FileInputStream fis = null;
        FileOutputStream fos = null;
        
        try {
            fis = new FileInputStream(file);
            fos = new FileOutputStream(copy);
            
            int bytes;
            byte[] packet = new byte[COPY_PACKET_SIZE];
            while ((bytes = fis.read(packet, 0, COPY_PACKET_SIZE)) != -1) fos.write(packet, 0, bytes);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error copying file", e);
            return false;
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing target stream", e); }
            try { if (fis != null) fis.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing source stream", e); }
        }
    }
    
    
    /**
     * Deletes file or folder.
     * Optionally invokes deleteOnExit if necessary.
     * 
     * @param file file or folder to be deleted,
     * @param deleteOnExit true if deleteOnExit should be invoked on not deleted file or directory.
     * @return true if the file or folder has been completely deleted, false otherwise.
     */
    public static boolean delete(File file, boolean deleteOnExit) {
        
        if (file == null) throw new NullPointerException("File cannot be null");
        if (!file.exists()) return true;
        
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) delete(files[i], deleteOnExit);
        }

        if (!file.delete()) {
            if (Utilities.isWindows() && file.isFile()) {
                for (int i = 0; i < 5; i++) {
                    System.gc();
                    if (file.delete()) return true;
                }
            }
            if (deleteOnExit) file.deleteOnExit();
            return false;
        }
        
        return true;
        
    }
    
    public static void createArchive(File directory, File archive) {        
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
                        try { if (fis != null) fis.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing archive entry stream", e); }
                    }
                } else {
                    // TODO: process directory
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating archive", e);
        } finally {
            try { if (zos != null) zos.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing archive stream", e); }
        }
    }
    
    public static File extractArchive(File archive, File destination) {
        // TODO: implement extracting directories
        
        File directory = getUniqueFile(destination, archive.getName());
        ZipFile zipFile = null;
        
        try {
            prepareDirectory(directory);
            
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
                    try { if (fos != null) fos.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing extracted file stream", e); }
                    try { if (is != null) is.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing zipentry stream", e); }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error extracting archive", e);
            return null;
        } finally {
            try { if (zipFile != null) zipFile.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing archive", e); }
        }
        
        return directory;
    }
    
    public static String imageToString(Image image, String format) {
        byte[] imageBytes = imageToBytes(image, format);
        return imageBytes != null ? Base64.byteArrayToBase64(imageBytes) : null;
    }
    
    public static Image stringToImage(String string) {
        return Toolkit.getDefaultToolkit().createImage(Base64.base64ToByteArray(string));
    }
    
    
    private static BufferedImage imageToBuffered(Image image) {
        if (image instanceof BufferedImage) return (BufferedImage)image;
        
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        bufferedImage.createGraphics().drawImage(image, null, null);
        return bufferedImage;
    }
    
    private static byte[] imageToBytes(Image image, String format) {
        BufferedImage bufferedImage = imageToBuffered(image);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            ImageIO.write(bufferedImage, format, outputStream);
        } catch (Exception e) {
            LOGGER.throwing(Utils.class.getName(), "imageToBytes", e); // NOI18N
            return null;
        }
        
        return outputStream.toByteArray();
    }

}
