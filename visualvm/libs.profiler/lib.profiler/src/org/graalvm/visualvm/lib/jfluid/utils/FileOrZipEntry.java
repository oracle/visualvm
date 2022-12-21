/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * A container that can represent either a plain file, or an entry in the zip/jar archive.
 * Used for unification of read operations on both types of files. So far likely not the most clean implementation.
 *
 * @author  Misha Dmitriev
 */
public class FileOrZipEntry {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private File file;
    private String dirOrJar;
    private String fileName;
    private boolean isZipEntry;
    private long len;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public FileOrZipEntry(String dirOrJar, String fileName) {
        this.dirOrJar = dirOrJar;
        this.fileName = fileName;

        String lcd = dirOrJar.toLowerCase(Locale.ENGLISH);
        isZipEntry = (lcd.endsWith(".jar") || lcd.endsWith(".zip")); // NOI18N
        len = -1;
    }

    public FileOrZipEntry(File file) {
        this.file = file;
        isZipEntry = false;
        len = -1;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public File getFile() {
        if (isZipEntry) {
            return null;
        }

        if (file == null) {
            file = new File(dirOrJar, fileName);
        }

        return file;
    }

    public boolean isFile() {
        return (!isZipEntry);
    }

    public String getFullName() {
        if (!isZipEntry) {
            if (file == null) {
                file = new File(dirOrJar, fileName);
            }

            return file.getAbsolutePath();
        } else {
            return dirOrJar + "/" + fileName; // NOI18N
        }
    }

    public InputStream getInputStream() throws IOException {
        if (file != null) {
            return new FileInputStream(file);
        } else if (!isZipEntry) {
            file = new File(dirOrJar, fileName);
            len = file.length();

            return new FileInputStream(file);
        } else {
            ZipFile zip = new ZipFile(dirOrJar);
            ZipEntry entry = zip.getEntry(fileName);
            len = entry.getSize();

            return zip.getInputStream(entry);
        }
    }

    public long getLength() throws IOException {
        if (len != -1) {
            return len;
        } else if (file != null) {
            return file.length();
        } else if (!isZipEntry) {
            return (new File(dirOrJar, fileName)).length();
        } else {
            try (ZipFile zip = new ZipFile(dirOrJar)) {
                return zip.getEntry(fileName).getSize();
            }
        }
    }

    public String getName() {
        if (isZipEntry) {
            int lastSlashIdx = fileName.lastIndexOf('/'); // NOI18N

            if (lastSlashIdx == -1) {
                return fileName;
            } else {
                return fileName.substring(lastSlashIdx + 1);
            }
        } else {
            if (fileName == null) {
                fileName = file.getName();
            }

            return fileName;
        }
    }
}
