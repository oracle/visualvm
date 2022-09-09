/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java;

import java.awt.event.ActionEvent;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNodeAction.Provider.class)
@NbBundle.Messages({
    "JavaGoToSourceAction_GoToSource=Go to Source"
})
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
            super(Bundle.JavaGoToSourceAction_GoToSource(), 210);
            
            className = javaClass == null ? null : javaClass.getName();
            methodName = null;
            line = -1;
            
            setEnabled(className != null);
        }
        
        private GoToSourceAction(StackFrameNode sfNode) {
            super(Bundle.JavaGoToSourceAction_GoToSource(), 210);
            
            String name = sfNode.getName();
            
            int fileIdx = name.indexOf('('); // NOI18N
            String methodName = name.substring(0, fileIdx);
            String fileName = name.substring(fileIdx);
            
            int classIdx = methodName.lastIndexOf('.'); // NOI18N
            className = methodName.substring(0, classIdx);
            this.methodName = methodName.substring(classIdx + 1);
            
            int lineIdxS = fileName.indexOf(':'); // can be 'Native Method' instead of '<file name>:<line number>'  // NOI18N
            int lineIdxE = fileName.indexOf(')'); // NOI18N
            line = lineIdxS == -1 ? -1 : Integer.parseInt(fileName.substring(lineIdxS + 1, lineIdxE));
            
            setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (line == -1) GoToSource.openSource(null, className, methodName, null);
            else GoToSource.openSource(null, className, methodName, line);
        }
        
    }
    
}
