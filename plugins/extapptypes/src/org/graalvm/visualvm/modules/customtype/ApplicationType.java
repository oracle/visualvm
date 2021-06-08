/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.customtype;

import org.graalvm.visualvm.modules.customtype.icons.IconCache;
import org.graalvm.visualvm.modules.customtype.icons.ImageUtils;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ApplicationType extends org.graalvm.visualvm.application.type.ApplicationType {
    public static final String PROPERTY_INFO_URL = "info_url"; // NOI18N
    public static final String PROPERTY_MAIN_CLASS = "main-class"; // NOI18N

    private String defName;
    private String mainClass;
    private String name;
    private String version;
    private String description;
    private URL iconUrl;
    private URL infoUrl;

    private BufferedImage icon;
    private static BufferedImage DEFAULT_ICON;

    static {
        try {
            DEFAULT_ICON = ImageIO.read(new URL("nbres:/org/graalvm/visualvm/application/resources/application.png")); // NOI18N
        } catch (Exception e) {
            Logger.getLogger(ApplicationType.class.getName()).log(Level.SEVERE, "Can not initialize default icon", e);
            DEFAULT_ICON = null;
            throw new RuntimeException(e);
            // something is seriously broken - can't continue -> shut the whole application down
        }
    }

    ApplicationType(String mainClass, String name, String version, String description, URL iconUrl, URL infoUrl) {
        this.mainClass = mainClass;
        this.name = name;
        this.version = version;
        this.description = description;
        this.iconUrl = iconUrl;
        this.infoUrl = infoUrl;
    }

    void loadIcon() {
        if (iconUrl == null) {
            setIcon(DEFAULT_ICON);
        } else {
            try {
                setIcon(ImageUtils.resizeImage(ImageIO.read(iconUrl), 16, 16));
                return;
            } catch (IOException e) {}
        }

        if (iconUrl == null && infoUrl != null) {
            RequestProcessor.getDefault().post(new Runnable() {

                @Override
                public void run() {
                    BufferedImage img = IconCache.getDefault().retrieveObject(infoUrl);
                    if (img != null) {
                        setIcon(img);
                    }
                }
            });
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        String oldDescription = this.description;
        this.description = description;
        firePropertyChange(PROPERTY_DESCRIPTION, oldDescription, description);
    }

    public URL getIconURL() {
        return iconUrl;
    }

    public void setIconURL(URL iconUrl) {
        this.iconUrl = iconUrl;
        loadIcon();
    }

    public URL getInfoURL() {
        return infoUrl;
    }

    public void setInfoUrl(URL infoUrl) {
        URL oldUrl = this.infoUrl;
        this.infoUrl = infoUrl;
        firePropertyChange(PROPERTY_INFO_URL, oldUrl, infoUrl);
        loadIcon();
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        String oldClass = this.mainClass;
        this.mainClass = mainClass;
        firePropertyChange(PROPERTY_MAIN_CLASS, oldClass, mainClass);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        firePropertyChange(PROPERTY_NAME, oldName, name);
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        String oldVersion = this.version;
        this.version = version;
        firePropertyChange(PROPERTY_VERSION, oldVersion, version);
    }

    @Override
    public Image getIcon() {
        if (icon == null) {
            return DEFAULT_ICON;
        }

        return ImageUtils.resizeImage(icon, 16, 16);
    }

    private void setIcon(BufferedImage icon) {
        BufferedImage oldIcon = this.icon;
        this.icon = icon != null ? icon : DEFAULT_ICON;
        firePropertyChange(PROPERTY_ICON, oldIcon, icon);
    }

    String getDefName() {
        return defName;
    }

    void setDefName(String defName) {
        this.defName = defName;
    }
}
