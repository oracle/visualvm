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

package com.sun.tools.visualvm.application;

import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
class ApplicationActionsProvider {

    private static final HeapDumpOnOOMEAction heapDumpOnOOMEAction =
            new HeapDumpOnOOMEAction();

    static void initialize() {
        ExplorerContextMenuFactory explorer = ExplorerContextMenuFactory.sharedInstance();
        explorer.addExplorerActionsProvider(
                new ApplicationActionProvider(), Application.class);
    }

    private static class HeapDumpOnOOMEAction extends AbstractAction {

        boolean oomeEnabled;

        public HeapDumpOnOOMEAction refresh(boolean oomeEnabled) {
            this.oomeEnabled = oomeEnabled;
            putValue(NAME, oomeEnabled ?
                "Enable Heap Dump on OOME" : "Disable Heap Dump on OOME");
            return this;
        }

        public void actionPerformed(final ActionEvent e) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    JVM jvm = JVMFactory.getJVMFor((Application) e.getSource());
                    jvm.setDumpOnOOMEnabled(oomeEnabled);
                }
            });
        }
    }

    private static class ApplicationActionProvider
            implements ExplorerActionsProvider<Application> {

        public ExplorerActionDescriptor getDefaultAction(Set<Application> apps) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(Set<Application> apps) {
            Set<ExplorerActionDescriptor> actions =
                    new HashSet<ExplorerActionDescriptor>();
            if (apps.size() == 1) {
                JVM jvm = JVMFactory.getJVMFor(apps.iterator().next());
                if (jvm.isDumpOnOOMEnabledSupported()) {
                    actions.add(new ExplorerActionDescriptor(null, 40));
                    actions.add(new ExplorerActionDescriptor(
                            heapDumpOnOOMEAction.refresh(!jvm.isDumpOnOOMEnabled()), 41));
                }
            }
            
            return actions;
        }
    }
}
