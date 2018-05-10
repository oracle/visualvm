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

package org.graalvm.visualvm.heapviewer.ui;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapViewPlugin extends HeapView {
    
    private boolean showing;
    private boolean updatePending;
    
    private HeapViewerNode pendingNode;
    private boolean pendingAdjusting;
    
    private JComponent component;
    
    
    public HeapViewPlugin(String name, String description, Icon icon) {
        super(name, description, icon);
    }
    
    
    protected boolean acceptsAdjustingSelection() { return false; }
    
    
    protected abstract JComponent createComponent();
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {}
    
    
    public final JComponent getComponent() {
        if (component == null) {
            component = new JPanel(new BorderLayout());
            component.setOpaque(false);
            component.add(createComponent());
            
            component.addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        showing = component.isShowing();
                        if (showing && updatePending) {
                            doNodeSelectedImpl(pendingNode, pendingAdjusting);
                        }
                    }
                }
            });
        }
        return component;
    }
    
    public final ProfilerToolbar getToolbar() {
        // TODO: eventually can be enabled to provide actions for title pane
        return null;
    }
        
    
    void doNodeSelected(HeapViewerNode node, boolean adjusting) {
        if (showing) {
            doNodeSelectedImpl(node, adjusting);
        } else {
            updatePending = true;
            pendingNode = node;
            pendingAdjusting = adjusting;
        }
    }
    
    private void doNodeSelectedImpl(HeapViewerNode node, boolean adjusting) {
        updatePending = false;
        pendingNode = null;
        if (!adjusting || acceptsAdjustingSelection()) nodeSelected(node, adjusting);
    }
    
    
    public static abstract class Provider {
        
        public abstract HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID);
        
    }
    
}
