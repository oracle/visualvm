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

package org.graalvm.visualvm.application.type;

import org.graalvm.visualvm.application.Application;
import java.awt.Image;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;


/**
 * This {@link ApplicationType} represents VisualVM.
 * @author Tomas Hurka
 */
public class VisualVMApplicationType extends ApplicationType {
    Application application;
    
    VisualVMApplicationType(Application app) {
        application = app;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "VisualVM";  // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public String getVersion() {
        return NbBundle.getMessage(VisualVMApplicationType.class, "LBL_Unknown");   // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return NbBundle.getMessage(VisualVMApplicationType.class, "DESCR_VisualVMApplicationType");   // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public Image getIcon() {
        String iconPath = "org/graalvm/visualvm/application/resources/visualvm.png";  // NOI18N
        return ImageUtilities.loadImage(iconPath, true);
    }
}
