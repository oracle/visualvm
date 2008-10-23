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

package com.sun.tools.visualvm.application.type.custom;

import com.sun.tools.visualvm.application.type.custom.images.ImageUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import javax.imageio.ImageIO;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem.AtomicAction;
import org.openide.filesystems.Repository;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ApplicationTypeManager {
    final private FileObject defRepository;

    final private static class Singleton {

        final private static ApplicationTypeManager INSTANCE = new ApplicationTypeManager();
    }

    final public static ApplicationTypeManager getDefault() {
        return Singleton.INSTANCE;
    }

    private ApplicationTypeManager() {
        defRepository = Repository.getDefault().getDefaultFileSystem().findResource("VisualVM/ApplicationTypes"); // NOI18N
    }

    public ApplicationTypeModel findModel(String mainClass) {
        Enumeration<? extends FileObject> defs = defRepository.getFolders(false);
        while (defs.hasMoreElements()) {
            FileObject def = defs.nextElement();
            if (def.getExt().equals("def")) { // NOI18N
                String defMainClass = (String)def.getAttribute("mainClass"); // NOI18N
                if (defMainClass != null && defMainClass.equals(mainClass)) {
                    String name = (String)def.getAttribute("displayName"); // NOI18N
                    String description = (String)def.getAttribute("descritpion"); // NOI18N
                    String iconPath = (String)def.getAttribute("icon"); // NOI18N
                    String urlPath = (String)def.getAttribute("url"); // NOI18N

                    URL infoUrl = null;
                    URL iconUrl = null;
                    try {
                        if (urlPath != null) {
                            infoUrl = new URL(urlPath);
                        }
                        if (iconPath != null) {
                            iconUrl = new URL(iconPath);
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return new ApplicationTypeModel(mainClass, name, "", description, iconUrl, infoUrl);
                }
            }
        }
        return null;
    }

    public void storeModel(final ApplicationTypeModel model) throws IOException {
        Repository.getDefault().getDefaultFileSystem().runAtomicAction(new AtomicAction() {

            @Override
            public void run() throws IOException {
                String defName = model.getName().replace(' ', '_') + ".def";
                FileObject defFolder = defRepository.getFileObject(defName);
                if (defFolder == null) {
                    defFolder = defRepository.createFolder(defName);
                }
                defFolder.setAttribute("displayName", model.getName());
                defFolder.setAttribute("mainClass", model.getMainClass());
                if (model.getInfoURL() != null) {
                    defFolder.setAttribute("url", model.getInfoURL().toString());
                }
                if (model.getIconURL() != null) {
                    FileObject iconFile = defFolder.getFileObject("icon.png");
                    if (iconFile == null) {
                        iconFile = defFolder.createData("icon.png");
                    }
                    BufferedImage bIcon = ImageIO.read(model.getIconURL());

                    FileLock lock = iconFile.lock();
                    OutputStream os = iconFile.getOutputStream(lock);
                    try {
                        ImageIO.write(ImageUtils.resizeImage(bIcon, 16, 16), "png", os);
                        defFolder.setAttribute("icon", iconFile.getURL().toString());
                        iconFile = null;
                    } finally {
                        os.close();
                        lock.releaseLock();
                    }
                }
            }
        });
    }
}
