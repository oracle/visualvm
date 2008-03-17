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
class FooterPanel extends JPanel {
    
    private static final String BOTTOM_LEFT_RESOURCE =    "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-bottomleft.png";
    private static final String BOTTOM__RIGHT_RESOURCE =  "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-bottomright.png";
    private static final String BOTTOM__MIDDLE_RESOURCE = "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-bottommiddle.png";
    private static final String BOTTOM__LOGO_RESOURCE =   "com/sun/tools/visualvm/modules/appui/welcome/resources/welcome-bottomlogo.png";
    
    
    public FooterPanel() {
        initComponents();
    }
    
    
    private void initComponents() {
        Image bottomLeftImage = Utilities.loadImage(BOTTOM_LEFT_RESOURCE, true);
        Image bottomRightImage = Utilities.loadImage(BOTTOM__RIGHT_RESOURCE, true);
        Image bottomMiddleImage = Utilities.loadImage(BOTTOM__MIDDLE_RESOURCE, true);
        
        setLayout(new BorderLayout());
        setOpaque(false);
        add(new FixedImagePanel(bottomLeftImage), BorderLayout.WEST);
        add(new FixedImagePanel(bottomRightImage), BorderLayout.EAST);
        
        HorizontalImagePanel topMiddlePanel = new HorizontalImagePanel(bottomMiddleImage);
        Logo logo = new Logo( BOTTOM__LOGO_RESOURCE, BundleSupport.getURL("SunLogo") );
        topMiddlePanel.setLayout(new GridBagLayout());
        topMiddlePanel.add(logo);
        
        add(topMiddlePanel, BorderLayout.CENTER);
    }

}
