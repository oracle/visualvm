/*
 *  Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.profiling.actions;

import org.graalvm.visualvm.application.Application;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.ui.cpu.CPUView;
import org.graalvm.visualvm.lib.ui.cpu.LiveCPUView;
import org.graalvm.visualvm.lib.ui.memory.LiveMemoryView;
import org.graalvm.visualvm.lib.ui.results.DataView;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerActionsSupport {
    
    @ServiceProvider(service=DataView.PopupCustomizer.class, position=100)
    public static final class ResultsAction extends DataView.PopupCustomizer {

        public JMenuItem[] getMenuItems(JComponent invoker, DataView dv, Object value, ClientUtils.SourceCodeSelection scs) {
            Collection<? extends ProfilerPopupCustomizer> customizers =
                    Lookup.getDefault().lookupAll(ProfilerPopupCustomizer.class);

            if (customizers.isEmpty()) return null;
            
            ProfilerPopupCustomizer.View view;
            ProfilerPopupCustomizer.Mode mode;
            
            if (dv instanceof CPUView) {
                view = ProfilerPopupCustomizer.View.CPU;
                mode = invoker instanceof LiveCPUView ?
                        ProfilerPopupCustomizer.Mode.LIVE :
                        ProfilerPopupCustomizer.Mode.SNAPSHOT;
            } else {
                view = ProfilerPopupCustomizer.View.MEMORY;
                mode = invoker instanceof LiveMemoryView ?
                        ProfilerPopupCustomizer.Mode.LIVE :
                        ProfilerPopupCustomizer.Mode.SNAPSHOT;
            }
            
            Object _application = invoker.getClientProperty(ProfilerResultsAction.PROP_APPLICATION);
            Application application = _application instanceof Application ? (Application)_application : null;

            String className = scs == null ? null : scs.getClassName();
            String methodName = scs == null ? null : scs.getMethodName();
            String methodSignature = scs == null ? null : scs.getMethodSignature();
            
            ProfiledSourceSelection pss = new ProfiledSourceSelection(application, className, methodName, methodSignature);

            List<JMenuItem> menuItems = new ArrayList(customizers.size());

            for (ProfilerPopupCustomizer customizer : customizers) {
                if (customizer.supportsDataView(view, mode)) {
                    JMenuItem[] items = customizer.getMenuItems(pss, view, mode);
                    if (items != null) Collections.addAll(menuItems, items);
                }
            }

            return menuItems.isEmpty() ? null : menuItems.toArray(new JMenuItem[0]);
        }

    }
    
}
