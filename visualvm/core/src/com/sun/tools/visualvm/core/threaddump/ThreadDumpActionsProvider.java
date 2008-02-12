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

import com.sun.tools.visualvm.core.explorer.ThreadDumpNode;
import com.sun.tools.visualvm.core.explorer.ApplicationNode;
import com.sun.tools.visualvm.core.explorer.CoreDumpNode;
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

   void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ApplicationNodeActionProvider(), ApplicationNode.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new CoreDumpNodeActionProvider(), CoreDumpNode.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ThreadDumpNodeActionProvider(), ThreadDumpNode.class);
    }    
    
    
    private class TakeApplicationThreadDumpAction extends AbstractAction {
        
        private Application application;
        
        public TakeApplicationThreadDumpAction(Application application) {
            super("Thread Dump");
            this.application = application;
        }
        
        public void actionPerformed(ActionEvent e) {
            ThreadDumpSupport.getInstance().getThreadDumpProvider().createThreadDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    private class TakeCoreDumpThreadDumpAction extends AbstractAction {
        
        private CoreDump coreDump;
        
        public TakeCoreDumpThreadDumpAction(CoreDump coreDump) {
            super("Thread Dump");
            this.coreDump = coreDump;
        }
        
        public void actionPerformed(ActionEvent e) {
            ThreadDumpSupport.getInstance().getThreadDumpProvider().createThreadDump(coreDump, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    private class DeleteThreadDumpAction extends AbstractAction {
        
        private ThreadDumpImpl threadDump;
        
        public DeleteThreadDumpAction(ThreadDumpImpl threadDump) {
            super("Delete");
            this.threadDump = threadDump;
        }
        
        public void actionPerformed(ActionEvent e) {
            ThreadDumpSupport.getInstance().getThreadDumpProvider().deleteThreadDump(threadDump);
        }
        
    }
    
    private class ApplicationNodeActionProvider implements ExplorerActionsProvider<ApplicationNode> {

        public ExplorerActionDescriptor getDefaultAction(ApplicationNode node) { return null; }

        public List<ExplorerActionDescriptor> getActions(ApplicationNode node) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            Application application = node.getDataSource();
            JVM jvm = JVMFactory.getJVMFor(application);
            if (jvm.isTakeThreadDumpSupported())
                actions.add(new ExplorerActionDescriptor(new TakeApplicationThreadDumpAction(node.getDataSource()), 10));
            
            return actions;
        }
        
    }
    
    private class CoreDumpNodeActionProvider implements ExplorerActionsProvider<CoreDumpNode> {

        public ExplorerActionDescriptor getDefaultAction(CoreDumpNode node) { return null; }

        public List<ExplorerActionDescriptor> getActions(CoreDumpNode node) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            actions.add(new ExplorerActionDescriptor(new TakeCoreDumpThreadDumpAction(node.getDataSource()), 10));
            
            return actions;
        }
        
    }
    
    private class ThreadDumpNodeActionProvider implements ExplorerActionsProvider<ThreadDumpNode> {

        public ExplorerActionDescriptor getDefaultAction(ThreadDumpNode node) { return null; }

        public List<ExplorerActionDescriptor> getActions(ThreadDumpNode node) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            if (node.getDataSource() instanceof ThreadDumpImpl)
                actions.add(new ExplorerActionDescriptor(new DeleteThreadDumpAction((ThreadDumpImpl)node.getDataSource()), 10));
            
            return actions;
        }
        
    }

}
