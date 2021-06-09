/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import java.awt.Image;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;


/**
 * Default application type, which returns main class name as 
 * application name and uses generic Java icon as application
 * icon. It is used when when application is not recognized
 * by any of more specific ApplicationTypes.
 * 
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 * @author Jiri Sedlacek
 */
public class DefaultApplicationType extends ApplicationType {
    String name;
    Application application;
    
    protected DefaultApplicationType(Application app) {
        application = app;
    }
    
    /**
     * Gets the name of the application.
     * Application's main class is used as the name
     * of the application.
     * @return this application's name
     */
    public String getName() {
        if (name == null) {
            String mainClassName = null;
            if (Stateful.STATE_AVAILABLE == application.getState()) {
                Jvm jvm = JvmFactory.getJVMFor(application);
                if (jvm.isBasicInfoSupported()) {
                    mainClassName = jvm.getMainClass();
                }
            }
            
            if (mainClassName != null && !mainClassName.isEmpty()) {
                name = mainClassName;
            } else {
                name = application.getStorage().getCustomProperty(PROPERTY_SUGGESTED_NAME);
            }
            if (name == null) {
                name = application.isLocalApplication() ?
                        NbBundle.getMessage(DefaultApplicationType.class, "LBL_Local_Application") : // NOI18N
                        NbBundle.getMessage(DefaultApplicationType.class, "LBL_Remote_Application"); // NOI18N
            }
        }
        return name;
    }
    
    /**
     * {@inheritDoc}
     */ 
    public String getVersion() {
        return NbBundle.getMessage(DefaultApplicationType.class, "LBL_Unknown_Version");    // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */ 
    public String getDescription() {
        return "";
    }

    /**
     * {@inheritDoc}
     */     
    public Image getIcon() {
        String iconPath = "org/graalvm/visualvm/application/resources/application.png";   // NOI18N
        return ImageUtilities.loadImage(iconPath, true);
    }
}
