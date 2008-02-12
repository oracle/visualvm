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

package com.sun.tools.visualvm.core.coredump;

import com.sun.tools.visualvm.core.explorer.CoreDumpNode;
import com.sun.tools.visualvm.core.datasource.CoreDump;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.explorer.ExplorerNode;
import com.sun.tools.visualvm.core.explorer.ExplorerRoot;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;

/**
 *
 * @author Tomas Hurka
 */
class CoreDumpActionsProvider {
    
    private static final AddNewCoreDumpAction addNewCoreDumpAction = new AddNewCoreDumpAction();
    
    
    static void register() {
        ExplorerContextMenuFactory explorer = ExplorerContextMenuFactory.sharedInstance();
        explorer.addExplorerActionsProvider(new CoreDumpNodeActionProvider(), CoreDumpNode.class);
        explorer.addExplorerActionsProvider(new CoreDumpsNodeActionProvider(), CoreDumpsNode.class);
        explorer.addExplorerActionsProvider(new RootNodeActionProvider(), ExplorerRoot.class);
    }
    
    
    private static class AddNewCoreDumpAction extends AbstractAction {
        
        public AddNewCoreDumpAction() {
            super("Add VM Coredump...");
        }
        
        public void actionPerformed(ActionEvent e) {
            CoreDumpConfigurator newCoreDumpConfiguration = CoreDumpConfigurator.defineCoreDump();
            if (newCoreDumpConfiguration != null) {
                CoreDumpProvider provider = CoreDumpProvider.sharedInstance();
                provider.createHost(newCoreDumpConfiguration.getCoreDumpFile(),newCoreDumpConfiguration.getDisplayname(), newCoreDumpConfiguration.getJavaHome());
            }
        }
    }
    
    private static class RenameCoreDumpAction extends AbstractAction {
        
        private CoreDump coreDump;
        
        public RenameCoreDumpAction(CoreDump dmp) {
            super("Rename...");
            coreDump = dmp;
        }
        
        public void actionPerformed(ActionEvent e) {
            CoreDumpConfigurator newCoreDumpConfiguration = CoreDumpConfigurator.renameCoreDump(coreDump);
            if (newCoreDumpConfiguration != null) {
                coreDump.setDisplayName(newCoreDumpConfiguration.getDisplayname());
            }
        }
        
    }
    
    private static class RemoveCoreDumpAction extends AbstractAction {
        
        private CoreDumpImpl coreDump;
        
        public RemoveCoreDumpAction(CoreDumpImpl dmp) {
            super("Remove");
            this.coreDump = dmp;
        }
        
        public void actionPerformed(ActionEvent e) {
            CoreDumpProvider.sharedInstance().removeCoreDump(coreDump, true);
        }
        
    }
    
    private static abstract class CoreDumpActionProvider<T extends ExplorerNode> implements ExplorerActionsProvider<T> {
        
        public ExplorerActionDescriptor getDefaultAction(T coreDumpNode) {
            return null;
        }
        
    }
    
    private static class CoreDumpNodeActionProvider extends CoreDumpActionProvider<CoreDumpNode> {
        
        public List<ExplorerActionDescriptor> getActions(CoreDumpNode coreDumpNode) {
            CoreDump coreDump = coreDumpNode.getCoreDump();
            
            List<ExplorerActionDescriptor> actions = new ArrayList();
            
            actions.add(new ExplorerActionDescriptor(null, 30));
            actions.add(new ExplorerActionDescriptor(new RenameCoreDumpAction(coreDump), 40));
            if (coreDump instanceof CoreDumpImpl) actions.add(new ExplorerActionDescriptor(new RemoveCoreDumpAction((CoreDumpImpl)coreDump), 50));
            
            return actions;
        }
    }
    
    private static class CoreDumpsNodeActionProvider extends CoreDumpActionProvider<CoreDumpsNode> {
        
        public List<ExplorerActionDescriptor> getActions(CoreDumpsNode node) {
            List<ExplorerActionDescriptor> actions = new ArrayList();
            
            actions.add(new ExplorerActionDescriptor(addNewCoreDumpAction, 0));
            
            return actions;
        }
        
    }
    
    private static class RootNodeActionProvider implements ExplorerActionsProvider<ExplorerRoot> {
        
        public ExplorerActionDescriptor getDefaultAction(ExplorerRoot node) { return null; }
        
        public List<ExplorerActionDescriptor> getActions(ExplorerRoot node) {
            List<ExplorerActionDescriptor> actions = new ArrayList();
            
            actions.add(new ExplorerActionDescriptor(addNewCoreDumpAction, 20));
            
            return actions;
        }
        
    }
    
}
