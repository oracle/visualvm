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

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.BorderLayout;
import java.awt.Image;
import javax.swing.JPanel;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
class CaptionPanel extends JPanel {
    
    private static final String TOP_LEFT_RESOURCE =   "org/graalvm/visualvm/modules/appui/welcome/resources/welcome-topleft.png";
    private static final String TOP_RIGHT_RESOURCE =  "org/graalvm/visualvm/modules/appui/welcome/resources/welcome-topright.png";
    private static final String TOP_MIDDLE_RESOURCE = "org/graalvm/visualvm/modules/appui/welcome/resources/welcome-topmiddle.png";
    
    
    public CaptionPanel() {
        initComponents();
    }
    
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        Image topLeftImage = ImageUtilities.loadImage(TOP_LEFT_RESOURCE, true);
        Image topRightImage = ImageUtilities.loadImage(TOP_RIGHT_RESOURCE, true);
        Image topMiddleImage = ImageUtilities.loadImage(TOP_MIDDLE_RESOURCE, true);
        
        add(new FixedImagePanel(topLeftImage), BorderLayout.WEST);
        add(new FixedImagePanel(topRightImage), BorderLayout.EAST);
        add(new HorizontalImagePanel(topMiddleImage), BorderLayout.CENTER);
    }

}
