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

package com.sun.tools.visualvm.core.heapdump;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.CoreDump;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;

/**
 *
 * @author Jiri Sedlacek
 */
class HeapDumpActionsProvider {
    
    private final TakeApplicationHeapDumpAction takeApplicationHeapDumpAction = new TakeApplicationHeapDumpAction();
    private final TakeCoreDumpHeapDumpAction takeCoreDumpHeapDumpAction = new TakeCoreDumpHeapDumpAction();
    private final DeleteHeapDumpAction deleteHeapDumpAction = new DeleteHeapDumpAction();
    

    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ApplicationActionProvider(), Application.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new CoreDumpActionProvider(), CoreDump.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new HeapDumpActionProvider(), HeapDumpImpl.class);
    }    
    
    
    private class TakeApplicationHeapDumpAction extends AbstractAction {
        
        public TakeApplicationHeapDumpAction() {
            super("Heap Dump");
        }
        
        public void actionPerformed(ActionEvent e) {
            Application application = (Application)e.getSource();
            HeapDumpSupport.getInstance().getHeapDumpProvider().createHeapDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    private class TakeCoreDumpHeapDumpAction extends AbstractAction {
        
        public TakeCoreDumpHeapDumpAction() {
            super("Heap Dump");
        }
        
        public void actionPerformed(ActionEvent e) {
            CoreDump coreDump = (CoreDump)e.getSource();
            HeapDumpSupport.getInstance().getHeapDumpProvider().createHeapDump(coreDump, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    private class DeleteHeapDumpAction extends AbstractAction {
        
        public DeleteHeapDumpAction() {
            super("Delete");
        }
        
        public void actionPerformed(ActionEvent e) {
            HeapDumpImpl heapDump = (HeapDumpImpl)e.getSource();
            HeapDumpSupport.getInstance().getHeapDumpProvider().deleteHeapDump(heapDump);
        }
        
    }
    
    private class ApplicationActionProvider implements ExplorerActionsProvider<Application> {

        public ExplorerActionDescriptor getDefaultAction(Application application) { return null; }

        public List<ExplorerActionDescriptor> getActions(Application application) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            JVM jvm = JVMFactory.getJVMFor(application);
            if (jvm.isTakeHeapDumpSupported())
                actions.add(new ExplorerActionDescriptor(takeApplicationHeapDumpAction, 20));
            
            return actions;
        }
        
    }
    
    private class CoreDumpActionProvider implements ExplorerActionsProvider<CoreDump> {

        public ExplorerActionDescriptor getDefaultAction(CoreDump coreDump) { return null; }

        public List<ExplorerActionDescriptor> getActions(CoreDump coreDump) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            actions.add(new ExplorerActionDescriptor(takeCoreDumpHeapDumpAction, 20));
            
            return actions;
        }
        
    }
    
    private class HeapDumpActionProvider implements ExplorerActionsProvider<HeapDumpImpl> {

        public ExplorerActionDescriptor getDefaultAction(HeapDumpImpl heapDump) { return null; }

        public List<ExplorerActionDescriptor> getActions(HeapDumpImpl heapDump) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            actions.add(new ExplorerActionDescriptor(deleteHeapDumpAction, 10));
            
            return actions;
        }
        
    }

}
