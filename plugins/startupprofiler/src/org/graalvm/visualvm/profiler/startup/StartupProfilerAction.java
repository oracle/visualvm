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
package com.sun.tools.visualvm.profiler.startup;

import com.sun.tools.visualvm.profiler.ProfilerSupport;
import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "NAME_ProfileStartup=Prof&ile Startup",
    "DESC_ProfileStartup=Start new process and profile its startup"
})
final class StartupProfilerAction extends AbstractAction {
    
    private static final String ICON_PATH = "com/sun/tools/visualvm/profiler/startup/resources/profiler.png";  // NOI18N
    static final Image ICON = ImageUtilities.loadImage(ICON_PATH);
    
    private static StartupProfilerAction menuInstance;
    private static StartupProfilerAction toolbarAction;
    
    
    static synchronized StartupProfilerAction toolbarInstance() {
        if (menuInstance == null) {
            menuInstance = new StartupProfilerAction();
            menuInstance.putValue(SMALL_ICON, new ImageIcon(ICON));
            menuInstance.putValue("iconBase", ICON_PATH);  // NOI18N
        }
        return menuInstance;
    }
    
    static synchronized StartupProfilerAction menuInstance() {
        if (toolbarAction == null) {
            toolbarAction = new StartupProfilerAction();
        }
        return toolbarAction;
    }
    
    
    public void actionPerformed(ActionEvent actionEvent) {
        StartupProfiler.sharedInstance().profileStartup();
    }
    
    public boolean isEnabled() {
        return ProfilerSupport.getInstance().hasSupportedJavaPlatforms();
    }
    
    
    private StartupProfilerAction() {
        putValue(Action.NAME, Bundle.NAME_ProfileStartup());
        putValue(Action.SHORT_DESCRIPTION, Bundle.DESC_ProfileStartup());
    }
}
