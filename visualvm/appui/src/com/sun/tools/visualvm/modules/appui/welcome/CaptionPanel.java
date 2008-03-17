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

package com.sun.tools.visualvm.modules.appui.welcome;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Image;
import javax.swing.JPanel;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class CaptionPanel extends JPanel {
    
    private static final String TOP_LEFT_RESOURCE =   "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-topleft.png";
    private static final String TOP_RIGHT_RESOURCE =  "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-topright.png";
    private static final String TOP_MIDDLE_RESOURCE = "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-topmiddle.png";
    private static final String TOP_LOGO_RESOURCE =   "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-toplogo.png";
    
    
    public CaptionPanel() {
        initComponents();
    }
    
    
    private void initComponents() {
        Image topLeftImage = Utilities.loadImage(TOP_LEFT_RESOURCE, true);
        Image topRightImage = Utilities.loadImage(TOP_RIGHT_RESOURCE, true);
        Image topMiddleImage = Utilities.loadImage(TOP_MIDDLE_RESOURCE, true);
        Image topLogoImage = Utilities.loadImage(TOP_LOGO_RESOURCE, true);
        
        setLayout(new BorderLayout());
        setOpaque(false);
        add(new FixedImagePanel(topLeftImage), BorderLayout.WEST);
        add(new FixedImagePanel(topRightImage), BorderLayout.EAST);
        
        HorizontalImagePanel topMiddlePanel = new HorizontalImagePanel(topMiddleImage);
        topMiddlePanel.setLayout(new GridBagLayout());
        topMiddlePanel.add(new FixedImagePanel(topLogoImage));
        
        add(topMiddlePanel, BorderLayout.CENTER);
    }

}
