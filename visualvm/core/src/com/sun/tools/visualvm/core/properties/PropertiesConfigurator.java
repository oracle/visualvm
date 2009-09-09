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

package com.sun.tools.visualvm.core.properties;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class PropertiesConfigurator extends JPanel {

    private static Dimension MIN_SIZE = new Dimension(400, 300);
    private static Dimension MAX_SIZE = new Dimension(700, 600);

    private static Map<String, Dimension> SAVED_SIZES = new HashMap();


    public static void editProperties(DataSource dataSource) {
        PropertiesCustomizer customizer =
                PropertiesSupport.sharedInstance().getCustomizer(dataSource, null);
        PropertiesConfigurator pc = new PropertiesConfigurator(customizer);

        final DialogDescriptor dd = new DialogDescriptor(pc, NbBundle.getMessage(
                PropertiesConfigurator.class, "CAP_EditProperties", new Object[] { // NOI18N
                DataSourceDescriptorFactory.getDescriptor(dataSource).getName() }),
                true, new Object[] { pc.okButton, DialogDescriptor.CANCEL_OPTION },
                pc.okButton, 0, null, null);
        final Dialog d = ProfilerDialogs.createDialog(dd);
        String className = dataSource.getClass().getName();
        Dimension savedSize = SAVED_SIZES.get(className);
        if (savedSize != null) pc.setPreferredSize(savedSize);
        d.pack();
        d.setVisible(true);
        SAVED_SIZES.put(className, pc.getSize());
        pc.cleanup();

        if (dd.getValue() == pc.okButton) customizer.propertiesChanged();
        else customizer.propertiesCancelled();
    }
    
  
    private PropertiesConfigurator(PropertiesCustomizer customizer) {
        this.customizer = customizer;
        initComponents();
        update();
        customizer.addChangeListener(listener);
    }


    private void cleanup() {
        customizer.removeChangeListener(listener);
        removeAll();
    }

  
    private void update() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                okButton.setEnabled(customizer.settingsValid());
            }
        });
    }

    public Dimension getPreferredSize() {
        Dimension prefSize = super.getPreferredSize();
        prefSize.width = Math.max(prefSize.width, MIN_SIZE.width);
        prefSize.width = Math.min(prefSize.width, MAX_SIZE.width);
        prefSize.height = Math.max(prefSize.height, MIN_SIZE.height);
        prefSize.height = Math.min(prefSize.height, MAX_SIZE.height);
        return prefSize;
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        add(customizer, BorderLayout.CENTER);
        okButton = new JButton(NbBundle.getMessage(PropertiesConfigurator.class, "LBL_OK"));    // NOI18N
    }


    private final PropertiesCustomizer customizer;
    private final ChangeListener listener = new ChangeListener() {
                      public void stateChanged(ChangeEvent e) { update(); }
                  };
  
    private JButton okButton;
  
}
