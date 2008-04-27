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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
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
        setIcon(Utilities.loadImage(ICON_PATH, true));

        setFocusable(true);
        setRequestFocusEnabled(true);
    }
  
    private void initComponents() {
        setLayout(new BorderLayout());
        add(ExplorerComponent.instance(), BorderLayout.CENTER);
    }
    
    
    public static synchronized ExplorerTopComponent getComp() {
        if (instance == null) {
            TopComponent tc = WindowManager.getDefault().findTopComponent(PREFERRED_ID); // NOI18N
            if (tc != null) {
                if (tc instanceof ExplorerTopComponent) {
                    instance = (ExplorerTopComponent)tc;
                } else {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        //Incorrect settings file?
                        String exception = new String
                        ("Incorrect settings file. Unexpected class returned." // NOI18N
                        + " Expected:" + ExplorerTopComponent.class.getName() // NOI18N
                        + " Returned:" + tc.getClass().getName()); // NOI18N
                        LOGGER.warning(exception);   // NOI18N
                    }
                    //Fallback to accessor reserved for window system.
                    instance = ExplorerTopComponent.createComp();
                }
            } else {
                //Component cannot be deserialized
                //Fallback to accessor reserved for window system.
                instance = ExplorerTopComponent.createComp();
            }
        }       
        return instance;
    }
    
    public static synchronized ExplorerTopComponent createComp() {
        if (instance == null)
            instance = new ExplorerTopComponent();
        return instance;
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
