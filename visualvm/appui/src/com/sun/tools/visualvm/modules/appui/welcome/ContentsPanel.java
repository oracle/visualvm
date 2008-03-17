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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 *
 * @author Jiri Sedlacek
 */
class ContentsPanel extends JPanel implements Constants {
    
    public ContentsPanel() {
        initComponents();
    }
    
    
    private void initComponents() {
        setLayout(new GridLayout(4, 2, 0, 0));
        
        addLink( "Link1", false, false );
        addLink( "Link2", false, true );
        addLink( "Link3", false, false );
        addLink( "Link4", false, true );
        addLink( "Link5", false, false );
        addLink( "Link6", false, true );
        addLink( "Link7", true, false );
        addLink( "Link8", true, true );
        
        setBackground(Utils.getColor(COLOR_CONTENT_BACKGROUND));
    }
    
    private void addLink( String resourceKey, boolean drawBottom, boolean drawRight ) {
        JPanel panel = new JPanel();
        panel.setOpaque( false );
        panel.add( new WebLink(resourceKey) );
        panel.setBorder( new MyBorder(drawBottom, drawRight) );
        add( panel );
    }
    
    private static class MyBorder implements Border {
        private static final Color COLOR = Utils.getColor(BORDER_COLOR);
        private boolean drawBottom;
        private boolean drawRight;
        public MyBorder( boolean drawBottom, boolean drawRight ) {
            this.drawBottom = drawBottom;
            this.drawRight = drawRight;
        }
        
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(COLOR);
            g.drawLine(x, y, x+width, y);
            g.drawLine(x, y, x, y+height);
            if( drawRight ) 
                g.drawLine(x+width-1, y, x+width-1, y+height);
            if( drawBottom )
                g.drawLine(x, y+height-1, x+width, y+height-1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(5,5,5,5);
        }

        public boolean isBorderOpaque() {
            return false;
        }
    }
}
