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

import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Tomas Hurka
 */
public class PythonObjectNode extends InstanceNode {

    private final PythonObject pyobject;
    private final String type;

    private String nameString;

    public PythonObjectNode(PythonObject pyobject) {
        this(pyobject, pyobject.getType());
    }

    public PythonObjectNode(PythonObject pyobject, String type) {
        super(pyobject.getInstance());
        this.pyobject = pyobject;
        this.type = type;
    }


    public PythonObject getPythonObject() {
        return pyobject;
    }

    public String getName(Heap heap) {
        if (nameString == null) {
            nameString = getType();
            nameString = nameString.substring(nameString.lastIndexOf('.') + 1) + "#" + getInstance().getInstanceNumber();
        }
        return nameString;
    }

    public String getType() {
        return type;
    }


    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.OWN_SIZE) return pyobject.getSize();

        if (type == PythonObject.DATA_TYPE) return getPythonObject();

        return super.getValue(type, heap);
    }


    public PythonObjectNode createCopy() {
        PythonObjectNode copy = new PythonObjectNode(pyobject, type);
        setupCopy(copy);
        return copy;
    }

    protected void setupCopy(PythonObjectNode copy) {
        super.setupCopy(copy);
    }


    public static class Renderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {

        private static final Icon ICON = PythonSupport.createBadgedIcon(LanguageIcons.INSTANCE);
        private static Icon LOOP_ICON;

        private final Heap heap;

        public Renderer(Heap heap) {
            this.heap = heap;
        }

        public void setValue(Object value, int row) {
            HeapViewerNode loop = HeapViewerNode.getValue((HeapViewerNode)value, DataType.LOOP, heap);
            boolean isLoop = loop != null;
            PythonObjectNode node = isLoop ? (PythonObjectNode)loop : (PythonObjectNode)value;

            String name = node.getName(heap);
            if (name != null && !"null".equals(name)) {
                super.setNormalValue(isLoop ? "loop to " : "");
                super.setBoldValue(name);
            } else {
                super.setNormalValue("null");
                super.setBoldValue(null);
            }

            String logValue = DetailsSupport.getDetailsString(node.getInstance(), heap);
            setGrayValue(logValue == null ? "" : " : " + logValue);

            setIcon(isLoop ? loopIcon() : ICON);

            setIconTextGap(isLoop ? 4 : 0);
            ((LabelRenderer)valueRenderers()[0]).setMargin(3, isLoop ? 3 : 2, 3, 0);
        }

        public String getShortName() {
            return getBoldValue();
        }


        private static Icon loopIcon() {
            if (LOOP_ICON == null) {
                Image iconImage = ImageUtilities.icon2Image(ICON);
                Image loopImage = Icons.getImage(HeapWalkerIcons.LOOP);
                LOOP_ICON = new ImageIcon(ImageUtilities.mergeImages(iconImage, loopImage, 0, 0));
            }
            return LOOP_ICON;
        }

    }

}
