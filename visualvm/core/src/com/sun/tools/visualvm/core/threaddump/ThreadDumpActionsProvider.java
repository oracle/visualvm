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

package com.sun.tools.visualvm.core.threaddump;

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
class ThreadDumpActionsProvider {
    
    private final TakeApplicationThreadDumpAction takeApplicationThreadDumpAction = new TakeApplicationThreadDumpAction();
    private final TakeCoreDumpThreadDumpAction takeCoreDumpThreadDumpAction = new TakeCoreDumpThreadDumpAction();
    private final DeleteThreadDumpAction deleteThreadDumpAction = new DeleteThreadDumpAction();
    

    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ApplicationNodeActionProvider(), Application.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new CoreDumpNodeActionProvider(), CoreDump.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ThreadDumpNodeActionProvider(), ThreadDumpImpl.class);
    }    
    
    
    private class TakeApplicationThreadDumpAction extends AbstractAction {
        
        public TakeApplicationThreadDumpAction() {
            super("Thread Dump");
        }
        
        public void actionPerformed(ActionEvent e) {
            Application application = (Application)e.getSource();
            ThreadDumpSupport.getInstance().getThreadDumpProvider().createThreadDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    private class TakeCoreDumpThreadDumpAction extends AbstractAction {
        
        public TakeCoreDumpThreadDumpAction() {
            super("Thread Dump");
        }
        
        public void actionPerformed(ActionEvent e) {
            CoreDump coreDump = (CoreDump)e.getSource();
            ThreadDumpSupport.getInstance().getThreadDumpProvider().createThreadDump(coreDump, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    private class DeleteThreadDumpAction extends AbstractAction {
        
        public DeleteThreadDumpAction() {
            super("Delete");
        }
        
        public void actionPerformed(ActionEvent e) {
            ThreadDumpImpl threadDump = (ThreadDumpImpl)e.getSource();
            ThreadDumpSupport.getInstance().getThreadDumpProvider().deleteThreadDump(threadDump);
        }
        
    }
    
    private class ApplicationNodeActionProvider implements ExplorerActionsProvider<Application> {

        public ExplorerActionDescriptor getDefaultAction(Application application) { return null; }

        public List<ExplorerActionDescriptor> getActions(Application application) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            JVM jvm = JVMFactory.getJVMFor(application);
            if (jvm.isTakeThreadDumpSupported())
                actions.add(new ExplorerActionDescriptor(takeApplicationThreadDumpAction, 10));
            
            return actions;
        }
        
    }
    
    private class CoreDumpNodeActionProvider implements ExplorerActionsProvider<CoreDump> {

        public ExplorerActionDescriptor getDefaultAction(CoreDump coreDump) { return null; }

        public List<ExplorerActionDescriptor> getActions(CoreDump coreDump) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            actions.add(new ExplorerActionDescriptor(takeCoreDumpThreadDumpAction, 10));
            
            return actions;
        }
        
    }
    
    private class ThreadDumpNodeActionProvider implements ExplorerActionsProvider<ThreadDumpImpl> {

        public ExplorerActionDescriptor getDefaultAction(ThreadDumpImpl threadDump) { return null; }

        public List<ExplorerActionDescriptor> getActions(ThreadDumpImpl threadDump) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            actions.add(new ExplorerActionDescriptor(deleteThreadDumpAction, 10));
            
            return actions;
        }
        
    }

}
