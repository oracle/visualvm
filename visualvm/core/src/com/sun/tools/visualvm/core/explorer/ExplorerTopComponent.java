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

package com.sun.tools.visualvm.core.explorer;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
// Top component for DataSources explorer
final class ExplorerTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "ExplorerTopComponent";  // NOI18N
    private static final Logger LOGGER = Logger.getLogger(ExplorerTopComponent.class.getName());
    
    static final String ICON_PATH = "com/sun/tools/visualvm/core/ui/resources/explorer.png";    // NOI18N

    private static ExplorerTopComponent instance;


    private ExplorerTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(ExplorerTopComponent.class, "LBL_Applications"));   // NOI18N
        setToolTipText(NbBundle.getMessage(ExplorerTopComponent.class, "LBL_Applications"));    // NOI18N
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));

        setFocusable(true);
        setRequestFocusEnabled(true);
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                ExplorerComponent.instance().requestFocusInWindow();
            }
        });
    }
  
    private void initComponents() {
        setLayout(new BorderLayout());
        add(ExplorerComponent.instance(), BorderLayout.CENTER);
    }
    
    
    /**
    * Gets default instance. Do not use directly: reserved for *.settings files only,
    * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
    * To obtain the singleton instance, use {@link findInstance}.
    */
    public static synchronized ExplorerTopComponent getInstance() {
        if (instance == null) instance = new ExplorerTopComponent();
        return instance;
    }
    
    /**
    * Obtain the ExplorerTopComponent instance. Never call {@link #getDefault} directly!
    */
    public static synchronized ExplorerTopComponent findInstance() {
        TopComponent explorerTopComponent = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (explorerTopComponent == null) return getInstance();
        if (explorerTopComponent instanceof ExplorerTopComponent) return (ExplorerTopComponent)explorerTopComponent;
    
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning("There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");   // NOI18N
    }
        return getInstance();
    }
    
    private boolean needsDocking() {
        return WindowManager.getDefault().findMode(this) == null;
    }

    public void open() {
        if (needsDocking()) {
            Mode mode = WindowManager.getDefault().findMode("explorer"); // NOI18N
            if (mode != null) mode.dockInto(this);
        }
        super.open();
    }
    
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }
  
    protected String preferredID() {
        return PREFERRED_ID;
    }

}
