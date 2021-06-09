/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.jvmcap;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.graalvm.visualvm.uisupport.HTMLTextArea;

/**
 *
 * @author Jiri Sedlacek
 */
class JvmCapabilitiesViewComponent extends JPanel {

    public JvmCapabilitiesViewComponent(JvmCapabilitiesModel model) {
        initComponents(model);
    }
    
    
    private void initComponents(JvmCapabilitiesModel model) {
        HTMLTextArea dataArea = new HTMLTextArea("<nobr>" + content(model) + "</nobr>");
        
        setLayout(new BorderLayout());
        dataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        setBackground(dataArea.getBackground());
        add(dataArea, BorderLayout.CENTER);
    }
    
    
    private static String content(JvmCapabilitiesModel model) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("<b>Attachable:</b> " + model.isAttachable() + "<br>");
        builder.append("<b>Basic info supported:</b> " + model.isBasicInfoSupported() + "<br>");
        builder.append("<b>System properties supported:</b> " + model.isGetSystemPropertiesSupported() + "<br>");
        builder.append("<b>Monitoring supported:</b> " + model.isMonitoringSupported() + "<br>");
        builder.append("<b>CPU monitoring supported:</b> " + model.isCpuMonitoringSupported() + "<br>");
        builder.append("<b>Memory monitoring supported:</b> " + model.isMemoryMonitoringSupported() + "<br>");
        builder.append("<b>GC activity monitoring supported:</b> " + model.isCollectionTimeMonitoringSupported() + "<br>");
        builder.append("<b>Class monitoring supported:</b> " + model.isClassMonitoringSupported() + "<br>");
        builder.append("<b>Thread monitoring supported:</b> " + model.isThreadMonitoringSupported() + "<br>");
        builder.append("<b>Thread dump supported:</b> " + model.isTakeThreadDumpSupported() + "<br>");
        builder.append("<b>Heap dump supported:</b> " + model.isTakeHeapDumpSupported() + "<br>");
        builder.append("<b>Heap dump on OOME supported:</b> " + model.isDumpOnOOMSupported() + "<br>");
        
        return builder.toString();
    }

}
