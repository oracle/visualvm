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

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 *
 * @author Jiri Sedlacek
 */
public final class Utils {
    
    private static final int COPY_PACKET_SIZE = 4096;
    

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
    
    public static File extractArchive(File archive, File destination) {
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
            System.err.println(e);
            return null;
        }
        
        return outputStream.toByteArray();
    }

}
