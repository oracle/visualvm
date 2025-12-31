/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.ui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapViewerNodeAction extends AbstractAction {
    
    private final int position;
    
    public HeapViewerNodeAction(String name, int position) {
        putValue(NAME, name);
        this.position = position;
    }
    
    public int getPosition() {
        return position;
    }
    
    public boolean isDefault() {
        return false;
    }
    
    public boolean isMiddleButtonDefault(ActionEvent e) {
        return false;
    }
    
    
    public static abstract class Provider {
        
        public abstract boolean supportsView(HeapContext context, String viewID);

        public abstract HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions);
        
    }
    
    
    public static final class Actions {
        
        private final List<HeapViewerNodeAction> actions;
        
        private Actions(List<HeapViewerNodeAction> actions) {
            this.actions = actions;
        }
        
        public static Actions forNode(HeapViewerNode node, Collection<HeapViewerNodeAction.Provider> actionProviders,
                               HeapContext context, HeapViewerActions actions, HeapViewerNodeAction... additionalActions) {
            HeapViewerNode loop = HeapViewerNode.getValue(node, DataType.LOOP, context.getFragment().getHeap());
            if (loop != null) node = loop;
            
            List<HeapViewerNodeAction> actionsList = new ArrayList<>();
            for (HeapViewerNodeAction.Provider provider : actionProviders) {
                HeapViewerNodeAction[] providerActions = provider.getActions(node, context, actions);
                if (providerActions != null) Collections.addAll(actionsList, providerActions);
            }
            if (additionalActions != null) Collections.addAll(actionsList, additionalActions);
            actionsList.sort((HeapViewerNodeAction a1, HeapViewerNodeAction a2) -> Integer.compare(a1.getPosition(), a2.getPosition()));
            return new Actions(actionsList);
        }
        
        
        public void performDefaultAction(ActionEvent e) {
            for (HeapViewerNodeAction action : actions) {
                if (action.isDefault()) {
                    if (action.isEnabled()) action.actionPerformed(e);
                    return; // return for the first default action
                }
            }
        }
        
        public void performMiddleButtonAction(ActionEvent e) {
            for (HeapViewerNodeAction action : actions) {
                if (action.isMiddleButtonDefault(e)) {
                    if (action.isEnabled()) {
                        action.actionPerformed(e);
                        return; // return for the first enabled middleButton action
                    }
                }
            }
        }
        
        public void populatePopup(JPopupMenu popup) {
            int lastPosition = -1;
            for (HeapViewerNodeAction action : actions) {
                int position = action.getPosition() / 100;
                if (position > lastPosition && lastPosition != -1) popup.addSeparator();
                JMenuItem mi = new JMenuItem(action);
                if (action.isDefault()) mi.setFont(mi.getFont().deriveFont(Font.BOLD));
                popup.add(mi);
                lastPosition = position;
            }
        }
        
    }
    
}
