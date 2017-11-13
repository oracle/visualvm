/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.java;

import java.awt.event.ActionEvent;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.api.GoToSource;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNodeAction.Provider.class)
public class JavaGoToSourceAction extends HeapViewerNodeAction.Provider {

    public boolean supportsView(HeapContext context, String viewID) {
        return GoToSource.isAvailable() && JavaHeapFragment.isJavaHeap(context);
    }

    public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        HeapViewerNodeAction action;
        
        if (node instanceof StackFrameNode) {
            action = new GoToSourceAction((StackFrameNode)node);
        } else {
            JavaClass javaClass = HeapViewerNode.getValue(node, DataType.CLASS, context.getFragment().getHeap());
            action = new GoToSourceAction(javaClass);
        }
        
        return new HeapViewerNodeAction[] { action };
    }
    
    
    private static class GoToSourceAction extends HeapViewerNodeAction {
        
        private final String className;
        private final String methodName;
        private final int line;
        
        
        private GoToSourceAction(JavaClass javaClass) {
            super("Go to Source", 210);
            
            className = javaClass == null ? null : javaClass.getName();
            methodName = null;
            line = -1;
            
            setEnabled(className != null);
        }
        
        private GoToSourceAction(StackFrameNode sfNode) {
            super("Go to Source", 210);
            
            String name = sfNode.getName();
            
            int fileIdx = name.indexOf("(");
            String methodName = name.substring(0, fileIdx);
            String fileName = name.substring(fileIdx);
            
            int classIdx = methodName.lastIndexOf('.');
            className = methodName.substring(0, classIdx);
            this.methodName = methodName.substring(classIdx + 1);
            
            int lineIdxS = fileName.indexOf(':'); // can be 'Native Method' instead of '<file name>:<line number>'
            int lineIdxE = fileName.indexOf(')');
            line = lineIdxS == -1 ? -1 : Integer.parseInt(fileName.substring(lineIdxS + 1, lineIdxE));
            
            setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (line == -1) GoToSource.openSource(null, className, methodName, null);
            else GoToSource.openSource(null, className, methodName, line);
        }
        
    }
    
}
