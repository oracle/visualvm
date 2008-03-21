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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Action which shows ExplorerTopComponent component.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
// TODO: should be moved to private package
class ExplorerTopComponentAction extends AbstractAction {
    
    private static Action instance;
    
    
    public static Action getInstance() {
        if (instance == null) instance = new ExplorerTopComponentAction();
        return instance;
    }
  
    public void actionPerformed(ActionEvent evt) {
        ExplorerTopComponent win = ExplorerTopComponent.getInstance();
        win.open();
        win.requestActive();
    }
    
    
    private ExplorerTopComponentAction() {
        super(NbBundle.getMessage(ExplorerTopComponentAction.class, "CTL_ExplorerTopComponentAction"));
        putValue(SMALL_ICON, new ImageIcon(Utilities.loadImage(ExplorerTopComponent.ICON_PATH, true)));
    }
  
}

