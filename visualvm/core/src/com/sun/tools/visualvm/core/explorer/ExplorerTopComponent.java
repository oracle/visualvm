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
import javax.swing.SwingUtilities;
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

    private static final String PREFERRED_ID = "ExplorerTopComponent";
    static final String ICON_PATH = "com/sun/tools/visualvm/core/ui/resources/mgmtControlPanel.png";

    private static ExplorerTopComponent instance;


    private ExplorerTopComponent() {
        initComponents();
        setName("Applications");
        setToolTipText("Applications");
        setIcon(Utilities.loadImage(ICON_PATH, true));

        setFocusable(true);
        setRequestFocusEnabled(true);
    }
  
    private void initComponents() {
        setLayout(new BorderLayout());
        add(ExplorerUI.instance(), BorderLayout.CENTER);
    }
    
    
    public static synchronized ExplorerTopComponent getInstance() {
        if (instance == null) {
            Runnable instanceResolver = new Runnable() {
                public void run() {
                    TopComponent explorerTopComponent = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
                    if ((explorerTopComponent != null) && explorerTopComponent instanceof ExplorerTopComponent) {
                        instance = (ExplorerTopComponent)explorerTopComponent;
                    } else {
                        instance = new ExplorerTopComponent();
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) instanceResolver.run();
            else try { SwingUtilities.invokeAndWait(instanceResolver); } catch (Exception e) {}
        }

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
