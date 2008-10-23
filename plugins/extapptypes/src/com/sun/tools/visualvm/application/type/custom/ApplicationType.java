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

import com.sun.tools.visualvm.application.type.custom.images.ImageCache;
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
    private ApplicationTypeModel model;

    private Image icon;
    final private static Image DEFAULT_ICON = Utilities.loadImage("com/sun/tools/visualvm/application/resources/application.png"); // NOI18N

    public ApplicationType(ApplicationTypeModel model) {
        this.model = model;
        loadIcon();
    }

    private void loadIcon() {
        if (model.getIconURL() == null) {
            this.icon = DEFAULT_ICON;
        } else {
            try {
                this.icon = ImageIO.read(model.getIconURL());
            } catch (IOException e) {}
        }

        if (this.icon == null && model.getInfoURL() != null) {
            RequestProcessor.getDefault().post(new Runnable() {

                @Override
                public void run() {
                    BufferedImage img = ImageCache.getDefault().retrieveObject(model.getInfoURL());
                    if (img != null) {
                        setIcon(img);
                    }
                }
            });
        }
    }

    @Override
    public String getDescription() {
        return model.getDescription();
    }

    public void setDescription(String description) {
        String oldDescription = model.getDescription();
        model.setDescription(description);
        firePropertyChange(Property.DESCRIPTION, oldDescription, description);
    }

    public void setVersion(String version) {
        String oldVersion = model.getVersion();
        model.setVersion(version);
        firePropertyChange(Property.VERSION, oldVersion, version);
    }

    public String getMainClass() {
        return model.getMainClass();
    }

    public void setMainClass(String mainClass) {
        String oldClass = model.getMainClass();
        model.setMainClass(mainClass);
        firePropertyChange(Property.MAIN_CLASS, oldClass, mainClass);
    }

    @Override
    public Image getIcon() {
        return icon;
    }

    @Override
    public String getName() {
        return model.getName();
    }

    public void setName(String name) {
        String oldName = model.getName();
        model.setName(name);
        firePropertyChange(Property.NAME, oldName, name);
    }

    @Override
    public String getVersion() {
        return model.getVersion();
    }

    public URL getInfoUrl() {
        return model.getInfoURL();
    }

    public void setInfoUrl(URL url) {
        model.setInfoUrl(url);
    }

    private void setIcon(Image icon) {
        Image oldIcon = this.icon;
        this.icon = icon;
        firePropertyChange(Property.ICON, oldIcon, icon);
    }

    public ApplicationTypeModel getModel() {
        return model;
    }
}
