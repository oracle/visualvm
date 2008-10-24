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

package com.sun.tools.visualvm.modules.customtype;

import com.sun.tools.visualvm.modules.customtype.icons.IconCache;
import com.sun.tools.visualvm.modules.customtype.icons.ImageUtils;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ApplicationType extends com.sun.tools.visualvm.application.type.ApplicationType {
    public static class Property extends com.sun.tools.visualvm.application.type.ApplicationType.Property {
        protected Property(String value) {
            super(value);
        }

        public static final Property INFO_URL = new ApplicationType.Property("info_url"); // NOI18N
        public static final Property MAIN_CLASS = new ApplicationType.Property("main-class"); // NOI18N
    }
    private String mainClass;
    private String name;
    private String version;
    private String description;
    private URL iconUrl;
    private URL infoUrl;

    private BufferedImage icon;
    final private static Image DEFAULT_ICON = Utilities.loadImage("com/sun/tools/visualvm/application/resources/application.png"); // NOI18N

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
            setIcon(null);
        } else {
            try {
                setIcon(ImageIO.read(iconUrl));
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
        firePropertyChange(Property.DESCRIPTION, oldDescription, description);
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
        firePropertyChange(Property.INFO_URL, oldUrl, infoUrl);
//        IconCache.getDefault().invalidateObject(oldUrl);
        loadIcon();
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        String oldClass = this.mainClass;
        this.mainClass = mainClass;
        firePropertyChange(Property.MAIN_CLASS, oldClass, mainClass);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        firePropertyChange(Property.NAME, oldName, name);
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        String oldVersion = this.version;
        this.version = version;
        firePropertyChange(Property.VERSION, oldVersion, version);
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
        this.icon = icon;
        firePropertyChange(Property.ICON, oldIcon, icon);
    }
}
