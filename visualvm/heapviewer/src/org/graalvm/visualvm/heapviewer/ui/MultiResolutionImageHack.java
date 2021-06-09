/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.ui;

import java.awt.Image;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.IconUIResource;
import org.graalvm.visualvm.lib.jfluid.global.Platform;

/**
 * Components painting multi-resolution icons without image observers are stored in sun.awt.image.MultiResolutionToolkitImage.ObserverCache.
 * This cache uses SoftReferences transitively holding structures referenced by the components for an unnecessary amount of time,
 * causing slow downs due to heavy GC activity.
 * 
 * Causes a memleak-alike behavior by referencing StayOpenPopupMenu.RadioButtonItem instances and transitively HprofHeap instances.
 *
 * @author Jiri Sedlacek
 */
class MultiResolutionImageHack {
    
    // NOTE: to be invoked for a global hack
//    static void hack() {
//        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
//            public void run() {
//                SwingUtilities.invokeLater(new Runnable() {
//                    public void run() {
//                        hackIcon("RadioButtonMenuItem.checkIcon"); // NOI18N
//                    }
//                });
//            }
//        });
//    }
    
    static void hackIcon(String uiKey) {
        ImageIcon checkIcon = getCheckIcon(uiKey);
        if (checkIcon != null) hackIcon(checkIcon);
    }
    
    private static ImageIcon getCheckIcon(String uiKey) {
        Icon icon = UIManager.getIcon(uiKey);
        if (!(icon instanceof ImageIcon)) return null; // Icon must be ImageIcon
        
        Image image = ((ImageIcon)icon).getImage();
        if (image == null || !image.getClass().getName().contains("MultiResolution")) return null; // NOI18N
        
        return (ImageIcon)icon;
    }
    
    private static void hackIcon(ImageIcon icon) {
        try {
            // Create a dummy image observer
            JPanel p = new JPanel();
            
            // Set the image observer to the multi-resolution image
            icon.setImageObserver(p);
            
            // macOS specific
            if (Platform.isMac()) {
                Method getInvertedIcon = icon.getClass().getMethod("getInvertedIcon"); // NOI18N
                getInvertedIcon.setAccessible(true);
                IconUIResource invertedIcon = (IconUIResource)getInvertedIcon.invoke(icon);
                Field delegate = invertedIcon.getClass().getDeclaredField("delegate"); // NOI18N
                delegate.setAccessible(true);
                ImageIcon imageIcon = (ImageIcon)delegate.get(invertedIcon);
                // Set the image observer to the inverted multi-resolution image
                imageIcon.setImageObserver(p);
            }
        } catch (Throwable t) {
            Logger logger = Logger.getLogger(MultiResolutionImageHack.class.getName());
            logger.log(Level.FINE, "Failed to apply MultiResolutionToolkitImageCacheHack", t); // NOI18N
        }
    }
    
}
