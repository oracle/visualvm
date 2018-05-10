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

package org.graalvm.visualvm.application.type;

import java.awt.Image;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * This ApplicationType represent Java-Plugin.
 * 
 * @author Luis-Miguel Alventosa
 */
public class JavaPluginApplicationType extends ApplicationType {

    private String version;

    public JavaPluginApplicationType(String version) {
        this.version = version;
    }

    /**
     * Gets the name of the Java plugin.
     * @return this application's name
     */
    public String getName() {
        return NbBundle.getMessage(JavaPluginApplicationType.class, "LBL_Java_Plug-in_ver", version);   // NOI18N
    }

    /**
     * Gets the version of the Java-Plugin.
     * The version is the same as java.version of the JVM
     * @return this Java-Plugin's version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the description of the application.
     * @return this application's description
     */
    public String getDescription() {
        return NbBundle.getMessage(JavaPluginApplicationType.class, "LBL_Java_Plug-in");    // NOI18N
    }

    /**
     * Gets the icon of the application.
     * @return this application's icon
     */
    public Image getIcon() {
        String iconPath = "org/graalvm/visualvm/application/resources/application.png";   // NOI18N
        return ImageUtilities.loadImage(iconPath, true);
    }
}
