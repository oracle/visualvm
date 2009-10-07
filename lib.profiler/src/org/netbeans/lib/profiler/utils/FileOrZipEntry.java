/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

        String lcd = dirOrJar.toLowerCase();
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
            ZipFile zip = new ZipFile(dirOrJar);

            return zip.getEntry(fileName).getSize();
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
