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
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.util.Utilities;

/**
 *
 * @author S. Aubrecht
 */
public class Logo extends JPanel implements Constants, MouseListener {

    private String url;

//    public static Logo createSunLogo() {
//        return new Logo( SUN_LOGO_IMAGE, BundleSupport.getURL( "SunLogo" ) ); // NOI18N
//    }
//
//    public static Logo createJavaLogo() {
//        return new Logo( JAVA_LOGO_IMAGE, BundleSupport.getURL( "JavaLogo" ) ); // NOI18N
//    }
//
    /** Creates a new instance of RecentProjects */
    public Logo( String img, String url ) {
        super( new BorderLayout() );
        Icon image = new ImageIcon(Utilities.loadImage(img, true));
        JLabel label = new JLabel( image );
        label.setBorder( BorderFactory.createEmptyBorder() );
        label.setOpaque( false );
        label.addMouseListener( this );
        setOpaque( false );
        add( label, BorderLayout.CENTER );
        setCursor( Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) );
        this.url = url;
    }

    public void mouseClicked(MouseEvent e) {
        Utils.showURL( url );
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
//        StatusDisplayer.getDefault().setStatusText( url );
    }

    public void mouseExited(MouseEvent e) {
//        StatusDisplayer.getDefault().setStatusText( null );
    }
}
