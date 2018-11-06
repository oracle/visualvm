/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapViewerRendererWrapper implements HeapViewerRenderer {
        
    private HeapViewerRenderer renderer;

    @Override
    public void setValue(Object value, int row) {
        HeapViewerNode node = getNode(value);
        renderer = getRenderer(node);
        renderer.setValue(node, row);
    }

    @Override
    public int getHorizontalAlignment() {
        return renderer.getHorizontalAlignment();
    }

    @Override
    public JComponent getComponent() {
        return renderer.getComponent();
    }

    @Override
    public void move(int x, int y) {
        renderer.move(x, y);
    }
    
    @Override
    public String toString() {
        return renderer.toString();
    }
    
    @Override
    public String getShortName() {
        return renderer.getShortName();
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return renderer.getAccessibleContext();
    }

    protected abstract HeapViewerNode getNode(Object value);

    protected abstract HeapViewerRenderer getRenderer(HeapViewerNode node);
    
}
