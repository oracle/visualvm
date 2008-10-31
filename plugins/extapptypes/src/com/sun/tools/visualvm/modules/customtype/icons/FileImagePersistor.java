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
package com.sun.tools.visualvm.modules.customtype.icons;

import com.sun.tools.visualvm.modules.customtype.cache.Persistor;
import com.sun.tools.visualvm.modules.customtype.cache.Entry;
import com.sun.tools.visualvm.core.datasource.Storage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Jaroslav Bachorik
 */
public class FileImagePersistor implements Persistor<URL, BufferedImage> {
    final private static String APPTYPE_ICON_CACHE = "apptype-icon-cache"; // NOI18N
    final private FileObject storage;

    public FileImagePersistor() throws InstantiationException {
        try {
            FileObject globalStorage = FileUtil.toFileObject(Storage.getPersistentStorageDirectory());
            FileObject aStorage = globalStorage.getFileObject(APPTYPE_ICON_CACHE);

            if (aStorage == null) {
                storage = globalStorage.createFolder(APPTYPE_ICON_CACHE);
            } else {
                storage = aStorage;
            }
        } catch (IOException ex) {
            throw new InstantiationException(ex.getLocalizedMessage());
        }
    }

    @Override
    public Entry<BufferedImage> retrieve(URL key) {
        try {
            FileObject imageFile = storage.getFileObject(entryFileName(key));
            if (imageFile != null) {
                return new Entry<BufferedImage>(ImageIO.read(imageFile.getInputStream()), imageFile.lastModified().getTime());
            } else {
                return null;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void store(URL key, Entry<BufferedImage> value) {
        if (value.getContent() == null) return;
        FileLock outputLock = null;
        try {
            String fileName = entryFileName(key);
            FileObject imageFile = storage.getFileObject(fileName);
            if (imageFile == null) {
                imageFile = storage.createData(fileName);
            }
            if (imageFile != null) {
                outputLock = imageFile.lock();
                ImageIO.write(value.getContent(), "png", imageFile.getOutputStream(outputLock));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (outputLock != null) {
                outputLock.releaseLock();
            }
        }
    }

    private static String entryFileName(URL url) {
        return url.toString().replace(':', '#').replace('/', '_');
    }
}
