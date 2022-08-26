/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.datasupport;

import org.graalvm.visualvm.core.datasource.DataSource;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
 * Utils class encapsulating various helper methods.
 *
 * @author Jiri Sedlacek
 */
public final class Utils {
    
    /**
     * Shared RequestProcessor to be used for file operations that need to be synchronized.
     */
    public static final RequestProcessor FILE_QUEUE = new RequestProcessor("File Queue");   // NOI18N
    
    private static final int COPY_PACKET_SIZE = 16384;
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    /**
     * Returns true if given set contains at least one subclass of provided instance.
     * 
     * @param <X>
     * @param <Y>
     * @param classes Set of classes that will be searched.
     * @param superclassInstance instance to be searched.
     * @return true if given set contains at least one subclass of provided instance, false otherwise.
     */
    public static <X, Y> boolean containsSubclass(Set<? extends Class<? extends Y>> classes, X superclassInstance) {
        for (Class<? extends Y> classs : classes) if (classs.isInstance(superclassInstance)) return true;
        return false;
    }

    /**
     * Returns true if given set contains at least one superclass of provided instance.
     * 
     * @param <X>
     * @param <Y>
     * @param classes Set of classes that will be searched.
     * @param subclassInstance instance to be searched.
     * @return true if given set contains at least one superclass of provided instance, false otherwise.
     */
    public static <X, Y> boolean containsSuperclass(Set<? extends Class<? extends Y>> classes, X subclassInstance) {
        Class<?> subclass = subclassInstance.getClass();
        for (Class<? extends Y> classs : classes) if (classs.isAssignableFrom(subclass)) return true;
        return false;
    }

    /**
     * Returns filtered set containing only instances of the given class.
     * 
     * @param <X>
     * @param <Y>
     * @param <Z>
     * @param set Set to be filtered.
     * @param filter Class defining the filter.
     * @return filtered set containing only instances of the given class.
     */
    public static <X, Y extends X, Z extends X> Set<Z> getFilteredSet(Set<Y> set, Class<Z> filter) {
        Set<Z> filteredSet = new HashSet<>();
        for (Y item : set) if (filter.isInstance(item)) filteredSet.add((Z)item);
        return filteredSet;
    }
    
    /**
     * Returns list of given DataSources sorted by distance from DataSource.ROOT.
     * 
     * @param <X> any DataSource.
     * @param dataSources DataSources to be sorted.
     * @return list of given DataSources sorted by distance from DataSource.ROOT.
     */
    public static <X extends DataSource> List<X> getSortedDataSources(Set<X> dataSources) {
        List<DataSourcePath<X>> dataSourcePaths = getSortedDataSourcePaths(dataSources);
        List<X> sortedDataSources = new ArrayList<>();
        
        for (DataSourcePath<X> dataSourcePath : dataSourcePaths)
            sortedDataSources.add(dataSourcePath.getDataSource());
        
        return sortedDataSources;
    }
    
    /**
     * Returns true if provided DataSources are independent. Independent means that no DataSource
     * is (super)owner of any other DataSource.
     * 
     * @param <X> any DataSource.
     * @param dataSources DataSources to be checked.
     * @return true if provided DataSources are independent, false otherwise.
     */
    public static <X extends DataSource> boolean areDataSourcesIndependent(Set<X> dataSources) {
        return dataSources.size() == getIndependentDataSources(dataSources).size();
    }
    
    /**
     * Returns Set of independent DataSources. Independent means that no DataSource
     * is (super)owner of any other DataSource - this means that (sub)children are removed.
     * 
     * @param <X> any DataSource.
     * @param dataSources DataSources to be filtered.
     * @return Set of independent DataSources.
     */
    public static <X extends DataSource> Set<X> getIndependentDataSources(Set<X> dataSources) {
        Map<Integer, Set<X>> independentDataSourcesMap = new HashMap<>();
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
                    set = new HashSet<>();
                    independentDataSourcesMap.put(dataSourcePath.size() - 1, set);
                }
                set.add(dataSourcePath.getDataSource());
            }
        }
        
        Set<X> independentDataSources = new HashSet<>();
        Collection<Set<X>> independentSetsCollection = independentDataSourcesMap.values();
        for (Set<X> independentSet : independentSetsCollection)
            independentDataSources.addAll(independentSet);
        return independentDataSources;
    }
    
    private static <X extends DataSource> List<DataSourcePath<X>> getSortedDataSourcePaths(Set<X> dataSources) {
        List<DataSourcePath<X>> dataSourcePaths = new ArrayList<>();
        for (DataSource dataSource : dataSources) dataSourcePaths.add(new DataSourcePath(dataSource));
        Collections.sort(dataSourcePaths);
        return dataSourcePaths;
    }
    
    private static class DataSourcePath<X extends DataSource> extends ArrayList<DataSource> implements Comparable<DataSourcePath> {
        
        DataSourcePath(X dataSource) {
            super();
            DataSource ds = dataSource;
            while(ds != null) {
                add(0, ds);
                ds = ds.getOwner();
            }
        }

        public int compareTo(DataSourcePath dataSourcePath) {
            int thisSize = size();
            return Integer.compare(thisSize, dataSourcePath.size());
        }
        
        public X getDataSource() {
            return (X)get(size() - 1);
        }
        
    }
    
    
    /**
     * Returns filename without extension.
     * 
     * @param fileName file name.
     * @return filename without extension.
     */
    public static String getFileBase(String fileName) {
        int extIndex = fileName.lastIndexOf("."); // NOI18N
        if (extIndex == -1) return fileName;
        return fileName.substring(0, extIndex);
    }
    
    /**
     * Returns file extension.
     * 
     * @param fileName file name.
     * @return file extension.
     */
    public static String getFileExt(String fileName) {
        int extIndex = fileName.lastIndexOf("."); // NOI18N
        if (extIndex == -1) return ""; // NOI18N
        return fileName.substring(extIndex);
    }
    
    /**
     * Returns new File in provided directory based on the given filename.
     * NOTE: the query is synchronized, however creating a new file has to be synchronized in custom code
     * 
     * @param directory directory in which to create the file.
     * @param file preferred filename.
     * @return new File in provided directory based on the given filename.
     */
    public static File getUniqueFile(File directory, String file) {
        return getUniqueFile(directory, getFileBase(file), getFileExt(file));
    }
    
    /**
     * Returns new File in provided directory based on the given filename.
     * NOTE: the query is synchronized, however creating a new file has to be synchronized in custom code
     * 
     * @param directory directory in which to create the file.
     * @param fileName file name.
     * @param fileExt file extension.
     * @return new File in provided directory based on the given filename.
     */
    public synchronized static File getUniqueFile(File directory, String fileName, String fileExt) {
        File newFile = new File(directory, fileName + fileExt);
        while (newFile.exists()) {
            fileName = fileName + "_"; // NOI18N
            newFile = new File(directory, fileName + fileExt);
        }
        return newFile;
    }
    
    /**
     * Tries to create the directory incl. all super directories, returns true if at the end of the operation the directory exists.
     * 
     * @param directory directory to be created.
     * @return true if the directory exists, false otherwise.
     */
    public static synchronized boolean prepareDirectory(File directory) {
        if (directory.exists()) return true;
        directory.mkdirs();
        return directory.exists();
    }
    
    /**
     * Copies source file to the destination file, returns true if the file was successfully copied.
     * 
     * @param file source file.
     * @param copy destination file.
     * @return true if the file was successfully copied, false otherwise.
     */
    public static boolean copyFile(File file, File copy) {
        if (file == null || copy == null) throw new NullPointerException("File cannot be null");    // NOI18N
        if (!file.isFile() || copy.isDirectory()) throw new IllegalArgumentException("Not a valid file");   // NOI18N       
        
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
            LOGGER.log(Level.SEVERE, "Error copying file", e);  // NOI18N
            return false;
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing target stream", e); } // NOI18N
            try { if (fis != null) fis.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing source stream", e); } // NOI18N
        }
    }
    
    
    /**
     * Deletes file or folder.
     * Optionally invokes deleteOnExit if necessary.
     * 
     * @param file file or folder to be deleted.
     * @param deleteOnExit true if deleteOnExit should be invoked on not deleted file or directory.
     * @return true if the file or folder has been completely deleted, false otherwise.
     */
    public static boolean delete(File file, boolean deleteOnExit) {
        
        if (file == null) throw new NullPointerException("File cannot be null");    // NOI18N
        if (!file.exists()) return true;
        
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                delete(file1, deleteOnExit);
            }
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
    
    /**
     * Creates a zip archive of the given directory. Currently doesn't support
     * archiving subdirectories (only files are added to the archive).
     * 
     * @param directory directory to be archived.
     * @param archive archive file.
     */
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
                        try { if (fis != null) fis.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing archive entry stream", e); }  // NOI18N
                        if (zos != null) zos.closeEntry();
                    }
                } else {
                    // TODO: process directory
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating archive", e);  // NOI18N
        } finally {
            try { if (zos != null) zos.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing archive stream", e); }    // NOI18N
        }
    }
    
    /**
     * Extracts given zip archive, returns extracted directory. Currently doesn't support extracting subdirectories,
     * (only extracts toplevel files).
     * 
     * @param archive archive to be extracted.
     * @param destination destination directory.
     * @return extracted directory or null if extracting the archive failed.
     */
    public static File extractArchive(File archive, File destination) {
        // TODO: implement extracting directories
        
        File directory = getUniqueFile(destination, archive.getName());
        ZipFile zipFile = null;
        
        try {
            String destinationPath = directory.getCanonicalPath();
            prepareDirectory(directory);
            
            zipFile = new ZipFile(archive);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryFile = new File(directory, entry.getName());
                
                String entryFilePath = entryFile.getCanonicalPath();
                if (!entryFilePath.startsWith(destinationPath))
                    throw new IllegalStateException("Archive entry outside of destination directory: " + entryFilePath); // NOI18N
                
                FileOutputStream fos = null;
                InputStream is = null;
                try {
                    is = zipFile.getInputStream(entry);
                    fos = new FileOutputStream(entryFile);
                    int bytes;
                    byte[] packet = new byte[COPY_PACKET_SIZE];
                    while ((bytes = is.read(packet, 0, COPY_PACKET_SIZE)) != -1) fos.write(packet, 0, bytes);
                } finally {
                    try { if (fos != null) fos.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing extracted file stream", e); } // NOI18N
                    try { if (is != null) is.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing zipentry stream", e); } // NOI18N
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error extracting archive", e);    // NOI18N
            return null;
        } finally {
            try { if (zipFile != null) zipFile.close(); } catch (Exception e) { LOGGER.log(Level.SEVERE, "Problem closing archive", e); }   // NOI18N
        }
        
        return directory;
    }
    
    /**
     * Encodes given string using the Base64 encoding.
     * 
     * @param value String to be encoded.
     * @return encoded String.
     */
    public static String encodePassword(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
    
    /**
     * Encodes given char[] using the Base64 encoding. The original parameter value is overwritten.
     * 
     * @param value char[] to be encoded.
     * @return encoded char[].
     */
    public static char[] encodePassword(char[] value) {
        byte[] bytes = charsToBytes(value);
        Arrays.fill(value, (char)0);
        
        byte[] bytes2 = Base64.getEncoder().encode(bytes);
        Arrays.fill(bytes, (byte)0);
        
        char[] chars = bytesToChars(bytes2);
        Arrays.fill(bytes2, (byte)0);
        
        return chars;
    }
    
    /**
     * Decodes given string using the Base64 encoding.
     * 
     * @param value String to be decoded.
     * @return decoded String.
     */
    public static String decodePassword(String value) {
        return new String(Base64.getDecoder().decode(value));
    }
    
    /**
     * Decodes given char[] using the Base64 encoding. The original parameter value is overwritten.
     * 
     * @param value char[] to be decoded.
     * @return decoded char[].
     */
    public static char[] decodePassword(char[] value) {
        byte[] bytes = charsToBytes(value);
        Arrays.fill(value, (char)0);
        
        byte[] bytes2 = Base64.getDecoder().decode(bytes);
        Arrays.fill(bytes, (byte)0);
        
        char[] chars = bytesToChars(bytes2);
        Arrays.fill(bytes2, (byte)0);
        
        return chars;
    }
    
    /**
     * Encodes given image to String using the Base64 encoding.
     * This is primarily intended to store small images (icons)
     * in text (properties) files, no compression algorithms are
     * used.
     * 
     * @param image Image to be encoded.
     * @param format image format.
     * @return String containing the encoded image.
     */
    public static String imageToString(Image image, String format) {
        byte[] imageBytes = imageToBytes(image, format);
        return imageBytes != null ? Base64.getEncoder().encodeToString(imageBytes) : null;
    }
    
    /**
     * Decodes an image encoded by imageToString(Image, String) method.
     * 
     * @param string String to be decoded.
     * @return decoded Image.
     */
    public static Image stringToImage(String string) {
        return Toolkit.getDefaultToolkit().createImage(Base64.getDecoder().decode(string));
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
            LOGGER.log(Level.WARNING, Utils.class.getName() + "imageToBytes", e);  // NOI18N
            return null;
        }
        
        return outputStream.toByteArray();
    }
    
    private static byte[] charsToBytes(char[] chars) {
        byte[] bytes = new byte[chars.length * 2];
        for (int i = 0; i < chars.length; i++) {
            bytes[i * 2] = (byte)((chars[i] & 0xff00) >> 8);
            bytes[i * 2 + 1] = (byte)(chars[i] & 0x00ff);
        }
        return bytes;
    }
    
    private static char[] bytesToChars(byte[] bytes) {
        char[] chars = new char[bytes.length / 2];
        for (int i = 0; i < chars.length; i++) {
            char ch = (char)(((bytes[i * 2] & 0x00ff) << 8) +
                              (bytes[i * 2 + 1] & 0x00ff));
            chars[i] = ch;
        }
        return chars;
    }
    
}
