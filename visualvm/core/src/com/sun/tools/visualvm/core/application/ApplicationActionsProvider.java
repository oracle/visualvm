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

package com.sun.tools.visualvm.core.application;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ApplicationActionsProvider implements ExplorerActionsProvider<Application> {
    
    private final HeapDumpOnOOMEAction heapDumpOnOOMEAction = new HeapDumpOnOOMEAction();
    
    
    public ExplorerActionDescriptor getDefaultAction(Application application) {
        return null;
    }
    
    public List<ExplorerActionDescriptor> getActions(Application application) {
        List<ExplorerActionDescriptor> actions = new ArrayList();
        JVM jvm = JVMFactory.getJVMFor(application);
        if (jvm.isDumpOnOOMEnabledSupported()) {
            actions.add(new ExplorerActionDescriptor(null, 40));
            actions.add(new ExplorerActionDescriptor(heapDumpOnOOMEAction.refresh(!jvm.isDumpOnOOMEnabled()), 41));
        }
        return actions;
        
    }
    
    
    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(this, Application.class);
    }
    
    
    private static class HeapDumpOnOOMEAction extends AbstractAction {
        
        boolean oomeEnabled;
        
        public HeapDumpOnOOMEAction refresh(boolean oomeEnabled) {
            this.oomeEnabled = oomeEnabled;
            putValue(NAME, oomeEnabled ? "Enable Heap Dump on OOME" : "Disable Heap Dump on OOME");
            return this;
        }
        
        public void actionPerformed(final ActionEvent e) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    JVM jvm = JVMFactory.getJVMFor((Application)e.getSource());
                    jvm.setDumpOnOOMEnabled(oomeEnabled);
                }
            });
        }
    }
    
}
