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
package org.graalvm.visualvm.jfr.views.locks;

import java.awt.Font;
import java.time.Duration;
import javax.swing.JLabel;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
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
final class LocksRenderers {
    
    private static int getMinimumWidth(String valueName) {
        return new JLabel(valueName).getPreferredSize().width + 30;
    }
    
    
    static class LocksNameRenderer extends MultiRenderer {
        
        private static Font regular;
        private static Font bold;
        
        private final LabelRenderer simpleRenderer;
        private final JavaNameRenderer javaRenderer;
        private final LabelRenderer threadRenderer;
        
        private final ProfilerRenderer[] renderers;
        
        
        LocksNameRenderer() {
            simpleRenderer = new LabelRenderer() {
                @Override
                public void setValue(Object value, int row) {
                    LocksNode node = value instanceof LocksNode ? (LocksNode)value : null;
                    LocksNode parent = node == null ? null : node.getParent();
                    setVisible(node instanceof LocksNode.Label || parent != null && parent.getParent() != null && node.icon != null);
                    if (isVisible()) {
                        if (node instanceof LocksNode.Thread) {
                            if (parent instanceof LocksNode.Thread) {
                                if ("<timed out>".equals(parent.name)) setText("in");
                                else setText(((LocksNode.Thread)parent).blocking ? "blocked" : "blocked by");
                            } else {
                                setText(((LocksNode.Thread)node).blocking ? "held by" : "blocked");
                            }
                        } else if (parent instanceof LocksNode.Thread) {
                            if ("<timed out>".equals(parent.name)) setText("on");
                            else setText(((LocksNode.Thread)parent).blocking ? "held" : "blocked on");
                        } else if (node instanceof LocksNode.Label) {
                            setText(node.name);
                        }
                    }
                }
            };
            
            javaRenderer = new JavaNameRenderer() {
                @Override
                public void setValue(Object value, int row) {
                    setVisible(value instanceof LocksNode.LockClass || value instanceof LocksNode.LockObject);
                    if (isVisible()) {
                        LocksNode node = (LocksNode)value;
                        super.setValue(node.name, row);
                        setIcon(node.icon);
                    }
                }
            };
            
            threadRenderer = new LabelRenderer() {
                @Override
                public void setValue(Object value, int row) {
                    setVisible(value instanceof LocksNode.Thread);
                    if (isVisible()) {
                        LocksNode node = (LocksNode)value;
                        LocksNode parent = node.getParent();
                        boolean toplevel = parent == null || parent.getParent() == null;
                        setFont(toplevel ? bold() : regular());
                        super.setValue(node.name, row);
                        setIcon("<timed out>".equals(node.name) ? (toplevel ? Icons.getIcon(GeneralIcons.EMPTY) : null) : node.icon);
                    }
                }
            };
            
            renderers = new ProfilerRenderer[] { simpleRenderer, javaRenderer, threadRenderer };
        }

        
        @Override
        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        
        public void setValue(Object value, int row) {
            for (ProfilerRenderer renderer : valueRenderers())
                renderer.setValue(value, row);
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
    
    
    static class TotalTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Total Time";
        }
        
        static boolean isInitiallyVisible() {
            return true;
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
    
    static class TotalCountRenderer extends NumberRenderer {
        
        static String getDisplayName() {
            return "Total Count";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(999999999999l, -1);
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
