/*
 *  Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.profiler.startup;

import java.awt.Dialog;
import java.awt.Image;
import java.awt.Window;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.swing.SwingUtilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

/**
 *
 * @author Jiri Sedlacek
 */
final class Dialogs {
    
    private static WeakReference<List<Image>> icons;
    
    
    static Dialog dialog(String caption, Object message) {
        return dialogImpl(caption, message, DialogDescriptor.PLAIN_MESSAGE);
    }
    
    static Dialog info(String caption, String message) {
        return dialogImpl(caption, message, DialogDescriptor.INFORMATION_MESSAGE, DialogDescriptor.OK_OPTION);
    }
    
    static Dialog warning(String caption, String message) {
        return dialogImpl(caption, message, DialogDescriptor.WARNING_MESSAGE, DialogDescriptor.OK_OPTION);
    }
    
    static Dialog error(String caption, String message) {
        return dialogImpl(caption, message, DialogDescriptor.ERROR_MESSAGE, DialogDescriptor.OK_OPTION);
    }
    
    private static Dialog dialogImpl(String caption, Object message, int type, Object... options) {
        DialogDescriptor dd = new DialogDescriptor(message, caption);
        dd.setMessageType(type);
        
        dd.setOptions(options);
        
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        
        d.setIconImages(getIcons());
        d.setResizable(false);
        
        return d;
    }
    
    private static List<Image> getIcons() {
        List<Image> i = icons == null ? null : icons.get();
        
        if (i == null)
            for (Window w : Dialog.getWindows()) {
                List<Image> images = w.getIconImages();
                if (images != null && !images.isEmpty()) {
                    i = images;
                    icons = new WeakReference(images);
                    break;
                }
            }
        
        return i;
    }
    
    static void show(final Dialog d) {
        Runnable r = new Runnable() {
            public void run() { d.setVisible(true); }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
    
}
