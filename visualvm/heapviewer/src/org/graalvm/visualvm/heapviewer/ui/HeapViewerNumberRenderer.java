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

package org.graalvm.visualvm.heapviewer.ui;

import java.text.Format;
import java.util.Objects;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberPercentRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeapViewerNumberRenderer extends NumberPercentRenderer {
    
    private final DataType dataType;
    
    
    public static HeapViewerNumberRenderer decimalInstance(DataType dataType) {
        return new HeapViewerNumberRenderer(null, dataType);
    }
    
    public static HeapViewerNumberRenderer bytesInstance(DataType dataType) {
        return new HeapViewerNumberRenderer(Formatters.bytesFormat(), dataType);
    }
    
    
    private HeapViewerNumberRenderer(Format format, DataType dataType) {
        super(createNumberRenderer(format, dataType));
        this.dataType = dataType;
    }
    
    
    public void setValue(Object value, int row) {
        super.setValue(value, row);
        
        boolean showsPercents = !isDiffMode() &&
                                !(Objects.equals(value, dataType.getNoValue())) &&
                                !(Objects.equals(value, dataType.getUnsupportedValue())) &&
                                !(Objects.equals(value, dataType.getNotAvailableValue()));
        valueRenderers()[1].getComponent().setVisible(showsPercents);
    }
    
    
    private static ProfilerRenderer createNumberRenderer(Format customFormat, final DataType dataType) {
        NumberRenderer numberRenderer = new NumberRenderer(customFormat) {
            protected String getValueString(Object value, int row, Format format) {
                if (Objects.equals(value, dataType.getNoValue())) return "-"; // NOI18N
                if (Objects.equals(value, dataType.getUnsupportedValue())) return ""; // NOI18N
                if (Objects.equals(value, dataType.getNotAvailableValue())) return "n/a"; // NOI18N
                return super.getValueString(value, row, format);
            }
        };
        numberRenderer.setMargin(3, 3, 3, 3);
        return numberRenderer;
    }
    
}
