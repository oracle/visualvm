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

package com.sun.tools.visualvm.modules.jconsole;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import javax.swing.ImageIcon;
import org.openide.util.Utilities;

/**
 * @author Leif Samuelsson
 * @author Luis-Miguel Alventosa
 */
class JConsoleView extends DataSourceView {

    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/jconsole/ui/resources/jconsole.png"; // NOI18N
    
    private Application application;
    private DataViewComponent view;

    public JConsoleView(Application application) {
        super(application, "JConsole Plugins", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
        this.application = application;
    }

    @Override
    public synchronized DataViewComponent getView() {
        if (view == null) view = createViewComponent();
        return view;
    }

    private DataViewComponent createViewComponent() {
        return new DataViewComponent(
                new DataViewComponent.MasterView("JConsole Plugins", null, new JConsolePluginWrapper(application).getView()),
                new DataViewComponent.MasterViewConfiguration(true));
    }
}
