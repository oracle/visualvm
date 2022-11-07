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

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.graalvm.visualvm.core.ui.DesktopUtils;
import org.graalvm.visualvm.modules.appui.AboutAction;
import org.openide.ErrorManager;
import org.openide.util.NbBundle;

/**
 *
 * @author S. Aubrecht
 */
public class Utils {
    
    private final static Logger LOGGER = Logger.getLogger(AboutAction.class.getName());
    
    /** Creates a new instance of Utils */
    private Utils() {
    }

    public static Graphics2D prepareGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Map<?,?> rhints = (Map)(Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints")); //NOI18N
        if( rhints == null && Boolean.getBoolean("swing.aatext") ) { //NOI18N
             g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
        } else if( rhints != null ) {
            g2.addRenderingHints( rhints );
        }
        return g2;
    }

    public static void showURL(String href) {
        boolean opened = false;
        
        if (DesktopUtils.isBrowseAvailable()) {
            try {
                URL url = new URL(href);
                DesktopUtils.browse(url.toURI());
                opened = true;
            } catch (Exception e) {
                LOGGER.throwing(Utils.class.getName(), "showURL", e);
            }
        }
        
        if (!opened)
            JOptionPane.showMessageDialog(null, "<html><b>Unable to launch web browser.</b><br><br>" + 
                    "Please open the following link manually:<br><code>" + href +
                    "</code></html>", "Unable To Launch Web Browser", JOptionPane.ERROR_MESSAGE);
    }

    static int getDefaultFontSize() {
        Integer customFontSize = (Integer)UIManager.get("customFontSize"); // NOI18N
        if (customFontSize != null) {
            return customFontSize.intValue();
        } else {
            Font systemDefaultFont = UIManager.getFont("TextField.font"); // NOI18N
            return (systemDefaultFont != null)
                ? systemDefaultFont.getSize()
                : 12;
        }
    }

    public static Color getColor( String resId ) {
        ResourceBundle bundle = NbBundle.getBundle("org.graalvm.visualvm.modules.appui.welcome.resources.Bundle"); // NOI18N
        try {
            Integer rgb = Integer.decode(bundle.getString(resId));
            return new Color(rgb.intValue());
        } catch( NumberFormatException nfE ) {
            ErrorManager.getDefault().notify( ErrorManager.INFORMATIONAL, nfE );
            return Color.BLACK;
        }
    }

    /**
     * Try to extract the URL from the given DataObject using reflection.
     * (The DataObject should be URLDataObject in most cases)
     */
//    public static String getUrlString(DataObject dob) {
//        try {
//            Method m = dob.getClass().getDeclaredMethod( "getURLString", new Class[] {} ); //NOI18N
//            m.setAccessible( true );
//            Object res = m.invoke( dob );
//            if( null != res ) {
//                return res.toString();
//            }
//        } catch (Exception ex) {
//            //ignore
//        }
//        return null;
//    }
}
