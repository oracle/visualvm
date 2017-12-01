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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.model.ContainerNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import java.awt.Font;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;

/**
 *
 * @author Tomas Hurka
 */
class PythonObjectsContainer extends ContainerNode<Instance> {

    public PythonObjectsContainer(String name) {
        this(name, UIThresholds.MAX_CLASS_INSTANCES);
    }

    public PythonObjectsContainer(String name, int maxItems) {
        super(name, maxItems);
    }


    protected int getCount(Instance instance, Heap heap) {
        return 1;
    }

    protected long getOwnSize(Instance instance, Heap heap) {
        return new PythonObject(instance).getSize();
    }

    protected long getRetainedSize(Instance instance, Heap heap) {
        return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
               instance.getRetainedSize() : DataType.RETAINED_SIZE.getNotAvailableValue();
    }


    protected PythonObjectNode createNode(Instance instance) {
        return new PythonObjectNode(new PythonObject(instance), name);
    }

    protected String getMoreNodesString(String moreNodesCount)  {
        return "<another " + moreNodesCount + " objects left>";
    }

    protected String getSamplesContainerString(String objectsCount)  {
        return "<sample " + objectsCount + " objects>";
    }

    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
        return "<objects " + firstNodeIdx + "-" + lastNodeIdx + ">";
    }


    public PythonObjectsContainer createCopy() {
        PythonObjectsContainer copy = new PythonObjectsContainer(name, maxNodes);
        setupCopy(copy);
        return copy;
    }

    protected void setupCopy(PythonObjectsContainer copy) {
        super.setupCopy(copy);
        copy.items.addAll(items); // TODO: should be shared (add protected constructor to access this.items)
        copy.count = count;
        copy.ownSize = ownSize;
        copy.retainedSize = retainedSize;
    }


    public static class Renderer extends LabelRenderer implements HeapViewerRenderer {

        public Renderer() {
            setIcon(PythonSupport.createBadgedIcon(LanguageIcons.PACKAGE));
            setFont(getFont().deriveFont(Font.BOLD));
        }

    }

}
