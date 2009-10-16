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

package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.properties.PropertiesPanel;
import com.sun.tools.visualvm.core.properties.PropertiesProvider;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class GeneralPropertiesProvider extends PropertiesProvider<Snapshot> {

    public GeneralPropertiesProvider() {
        super(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_SourceProperties"), // NOI18N
              NbBundle.getMessage(GeneralPropertiesProvider.class, "DESCR_SourceProperties"), // NOI18N
              CATEGORY_GENERAL, 50);
    }


    public PropertiesPanel createPanel(final Snapshot dataSource) {
        PropertiesPanel panel = new PropertiesPanel();
        panel.setLayout(new BorderLayout());
        
        JLabel l = new JLabel();
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        panel.add(l, BorderLayout.WEST);

        JTextField f = new JTextField() {
            public void setUI(TextUI ui) { super.setUI(new BasicTextFieldUI()); }
        };
        f.setEditable(false);
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        Dimension size = f.getPreferredSize();
        size.width = 1;
        f.setPreferredSize(size);
        f.setMinimumSize(size);
        panel.add(f, BorderLayout.CENTER);

        File file = dataSource.getFile();
        if (file == null) {
            l.setText(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_File")); // NOI18N
            f.setText(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_NoFile")); // NOI18N
        } else if (file.isFile()) {
            String path = null;
            try { path = file.getCanonicalPath(); }
            catch (IOException e) { path = file.getAbsolutePath(); }
            l.setText(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_File")); // NOI18N
            f.setText(path);
        } else if (file.isDirectory()) {
            String path = null;
            try { path = file.getCanonicalPath(); }
            catch (IOException e) { path = file.getAbsolutePath(); }
            l.setText(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_Directory")); // NOI18N
            f.setText(path);
        } else {
            l.setText(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_File")); // NOI18N
            f.setText(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_UnavailableFile")); // NOI18N
        }

        f.setCaretPosition(0);
        
        return panel;
    }


    public boolean supportsDataSource(Snapshot dataSource) { return dataSource != null; }

    public void propertiesDefined(PropertiesPanel panel, Snapshot dataSource) {}

    public void propertiesChanged(PropertiesPanel panel, Snapshot dataSource) {}

    public void propertiesCancelled(PropertiesPanel panel, Snapshot dataSource) {}

}
