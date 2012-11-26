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

package com.sun.tools.visualvm.jmx.impl;

import com.sun.tools.visualvm.application.ApplicationDescriptor;
import java.awt.Image;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class JmxApplicationDescriptor extends ApplicationDescriptor {
     
    private static final Image NODE_BADGE = ImageUtilities.loadImage(
            "com/sun/tools/visualvm/jmx/resources/jmxBadge.png", true); // NOI18N
     

    protected JmxApplicationDescriptor(JmxApplication application) {
        super(application, resolvePosition(application, POSITION_AT_THE_END, true));
    }

    public boolean supportsRename() {
        return true;
    }
    
    public Image getIcon() {
        Image originalIcon = super.getIcon();
        return originalIcon == null ? null : ImageUtilities.mergeImages(
                                             originalIcon, NODE_BADGE, 0, 0);
    }
    
}
