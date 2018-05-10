/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.modules.jconsole;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * @author Leif Samuelsson
 * @author Luis-Miguel Alventosa
 */
class JConsoleView extends DataSourceView {

    private static final String IMAGE_PATH = "org/graalvm/visualvm/modules/jconsole/ui/resources/jconsole.png"; // NOI18N
    
    private Application application;
    private JConsolePluginWrapper wrapper;

    public JConsoleView(Application application) {
        super(application, NbBundle.getMessage(JConsoleView.class, "JConsole_Plugins"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false); // NOI18N
        this.application = application;
    }

    @Override
    protected void removed() {
        wrapper.releasePlugins();
    }

    protected DataViewComponent createComponent() {
        wrapper = new JConsolePluginWrapper(application);
        return new DataViewComponent(
                new DataViewComponent.MasterView(NbBundle.getMessage(JConsoleView.class, "JConsole_Plugins"), null, wrapper.getView()), //NOI18N
                new DataViewComponent.MasterViewConfiguration(true));
    }
}
