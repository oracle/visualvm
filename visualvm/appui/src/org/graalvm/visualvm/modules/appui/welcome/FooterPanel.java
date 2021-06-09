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

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.BorderLayout;
import java.awt.Image;
import javax.swing.JPanel;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
class FooterPanel extends JPanel {
    
    private static final String BOTTOM_LEFT_RESOURCE =    "org/graalvm/visualvm/modules/appui/welcome/resources/welcome-bottomleft.png";
    private static final String BOTTOM__RIGHT_RESOURCE =  "org/graalvm/visualvm/modules/appui/welcome/resources/welcome-bottomright.png";
    private static final String BOTTOM__MIDDLE_RESOURCE = "org/graalvm/visualvm/modules/appui/welcome/resources/welcome-bottommiddle.png";
    
    
    FooterPanel() {
        initComponents();
    }
    
    
    private void initComponents() {
        Image bottomLeftImage = ImageUtilities.loadImage(BOTTOM_LEFT_RESOURCE, true);
        Image bottomRightImage = ImageUtilities.loadImage(BOTTOM__RIGHT_RESOURCE, true);
        Image bottomMiddleImage = ImageUtilities.loadImage(BOTTOM__MIDDLE_RESOURCE, true);
        
        setLayout(new BorderLayout());
        setOpaque(false);
        add(new FixedImagePanel(bottomLeftImage), BorderLayout.WEST);
        add(new FixedImagePanel(bottomRightImage), BorderLayout.EAST);
        add(new HorizontalImagePanel(bottomMiddleImage), BorderLayout.CENTER);
    }

}
