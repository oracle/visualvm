/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.application.type;

import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.Application;
import java.awt.Image;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import static com.sun.tools.visualvm.application.type.NetBeansApplicationTypeFactory.PRODUCT_VERSION_PROPERTY;
import java.util.Properties;

/**
 * This {@link ApplicationType} represents application based on GraalVM.
 *
 * @author Tomas Hurka
 */
public class GraalVMApplicationType extends ApplicationType {

    private Application application;
    private String name;
    private String fullVersionString;

    GraalVMApplicationType(Application app, Jvm jvm, String n) {
        application = app;
        name = n;
        if (jvm.isGetSystemPropertiesSupported()) {
            Properties p = jvm.getSystemProperties();
            if (p != null) {
                fullVersionString = p.getProperty(PRODUCT_VERSION_PROPERTY);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion() {
        return getMessage("LBL_Unknown");  // NOI18N
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        if (fullVersionString != null) {
            return fullVersionString;
        }
        return getMessage("DESCR_GraalVMBasedApplicationType"); // NOI18N
    }

    /**
     * {@inheritDoc}
     */
    public Image getIcon() {
        String iconPath = "com/sun/tools/visualvm/application/type/resources/GraalVM.png";   // NOI18N
        return ImageUtilities.loadImage(iconPath, true);
    }

    String getMessage(String string) {
        return NbBundle.getMessage(GraalVMApplicationType.class, string);
    }
}
