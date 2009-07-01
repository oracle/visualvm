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

package com.sun.tools.visualvm.modules.appui.options;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsCategory;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.DialogDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class NetworkOptionsCategory extends OptionsCategory {

    public static NetworkOptionsCategory instance() {
        return new NetworkOptionsCategory();
    }

    public Icon getIcon() {
        return new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/modules/appui/options/network.png"));  // NOI18N
    }

    public String getCategoryName() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                removeExportImport();
            }
        });
        return NbBundle.getMessage(NetworkOptionsCategory.class, "OptionsCategory_Name_Network");  // NOI18N
    }

    public String getTitle() {
        return NbBundle.getMessage(NetworkOptionsCategory.class, "OptionsCategory_Title_Network"); // NOI18N
    }

    public OptionsPanelController create() {
        return new NetworkOptionsPanelController();
    }

    private NetworkOptionsCategory() {}

    private void removeExportImport() {
        try {
            ClassLoader globalCL = Lookup.getDefault().lookup(ClassLoader.class);
            Class OptionsDisplayerImpl = globalCL.loadClass("org.netbeans.modules.options.OptionsDisplayerImpl");   // NOI18N
            Field descriptorRef = OptionsDisplayerImpl.getDeclaredField("descriptorRef");  // NOI18N
            descriptorRef.setAccessible(true);
            WeakReference<DialogDescriptor> ref = (WeakReference) descriptorRef.get(null);
            DialogDescriptor descriptor = ref.get();
            
            if (descriptor != null) {
                descriptor.setAdditionalOptions(null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
}
