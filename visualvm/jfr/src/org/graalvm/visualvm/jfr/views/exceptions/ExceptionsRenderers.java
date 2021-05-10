/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.exceptions;

import java.awt.Font;
import java.time.Duration;
import javax.swing.JLabel;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.McsTimeRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.MultiRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
final class ExceptionsRenderers {
    
    private static int getMinimumWidth(String valueName) {
        return new JLabel(valueName).getPreferredSize().width + 30;
    }
    
    
    static class NameRenderer extends MultiRenderer {
        
        private static Font regular;
        private static Font bold;
        
        private final JavaNameRenderer javaRenderer;
        private final LabelRenderer simpleRenderer;
        
        private final ProfilerRenderer[] renderers;
        
        
        NameRenderer() {
            javaRenderer = new JavaNameRenderer(Icons.getIcon(LanguageIcons.CLASS));
            simpleRenderer = new LabelRenderer();
            
            renderers = new ProfilerRenderer[] { javaRenderer, simpleRenderer };
        }
        
        
        @Override
        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        
        public void setValue(Object value, int row) {
            ExceptionsNode node = (value instanceof ExceptionsNode) ? (ExceptionsNode)value : null;
            if (node instanceof ExceptionsNode.Class) {
                javaRenderer.setVisible(true);
                String name = node.name;
                int colon = name.indexOf(":");                                  // NOI18N
                if (colon > 0) {
                    javaRenderer.setValue(name.substring(0, colon).trim(), row);
                    simpleRenderer.setVisible(true);
                    simpleRenderer.setFont(regular());
                    simpleRenderer.setText(name.substring(colon).trim());
                    simpleRenderer.setIcon(null);
                } else {
                    javaRenderer.setValue(name, row);
                    simpleRenderer.setVisible(false);
                }
            } else {
                javaRenderer.setVisible(false);
                simpleRenderer.setVisible(true);
                if (node != null) {
                    ExceptionsNode parent = node.getParent();
                    simpleRenderer.setFont((parent == null || parent.getParent() == null) && !(value instanceof ExceptionsNode.Label) ? bold() : regular());
                    simpleRenderer.setText(node.name);
                    simpleRenderer.setIcon(node.icon);
                } else {
                    simpleRenderer.setFont(regular());
                    simpleRenderer.setValue(value, row);
                }
            }
        }
        

        static String getDisplayName() {
            return "Name";
        }
        
        private static Font regular() {
            if (regular == null) regular = new LabelRenderer().getFont();
            return regular;
        }
        
        private static Font bold() {
            if (bold == null) bold = new LabelRenderer().getFont().deriveFont(Font.BOLD);
            return bold;
        }

    }
    
    
    static class CountRenderer extends NumberRenderer {
        
        static String getDisplayName() {
            return "Count";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class TotalTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Total Time";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(Duration.ofMillis(999999999999l), -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class MaxTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Max Time";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(Duration.ofMillis(999999999999l), -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    
    private static class TimeRenderer extends McsTimeRenderer {
        
        @Override
        public void setValue(Object value, int row) {
            if (value instanceof Duration) {
                long micros = ValuesConverter.durationToMicros((Duration)value);
                if (micros == 0) setText("< 0.001 ms"); // NOI18N
                else super.setValue(micros, row);
            } else {
                setText("-"); // NOI18N
            }
        }
        
    }
    
}
