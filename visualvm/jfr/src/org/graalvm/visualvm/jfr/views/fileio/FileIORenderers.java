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
package org.graalvm.visualvm.jfr.views.fileio;

import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.swing.renderer.FormattedLabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.McsTimeRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
final class FileIORenderers {
    
    private static int getMinimumWidth(String valueName) {
        return new JLabel(valueName).getPreferredSize().width + 30;
    }
    
    
    static class NameRenderer extends LabelRenderer {
        
        private static Font regular;
        private static Font bold;
            
        public void setValue(Object value, int row) {
            if (value instanceof FileIONode) {
                FileIONode node = (FileIONode)value;
                FileIONode parent = node.getParent();
                setFont(parent == null || parent.getParent() == null ? bold() : regular());
                setText(node.name);
                setIcon(node.icon);
            } else {
                super.setValue(value, row);
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
    
    
    static class TotalTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Total Time";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(999999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class ReadTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Read Time";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(999999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class MaxReadTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Max Read Time";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(999999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class WriteTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Write Time";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(999999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class MaxWriteTimeRenderer extends TimeRenderer {
        
        static String getDisplayName() {
            return "Max Write Time";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(999999999999999l, -1);
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
    
    static class ReadCountRenderer extends NumberRenderer {
        
        static String getDisplayName() {
            return "Read Count";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class WriteCountRenderer extends NumberRenderer {
        
        static String getDisplayName() {
            return "Write Count";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class ReadBytesRenderer extends BytesRenderer {
        
        static String getDisplayName() {
            return "Read Bytes";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(99999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class WriteBytesRenderer extends BytesRenderer {
        
        static String getDisplayName() {
            return "Write Bytes";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(99999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    
    private static class TimeRenderer extends McsTimeRenderer {
        
        @Override
        public void setValue(Object value, int row) {
            if (value == null) {
                setText("-"); // NOI18N
            } else if (value instanceof Number) {
                if (((Number)value).longValue() == 0) setText("< 0.001 ms"); // NOI18N
                else super.setValue(value, row);
            }
        }
        
    }
    
    private static class BytesRenderer extends FormattedLabelRenderer {
        
        BytesRenderer() {
            super(Formatters.bytesFormat());
            setHorizontalAlignment(SwingConstants.TRAILING);
        }
        
        @Override
        public void setValue(Object value, int row) {
            if (value == null) setText("-");
            else super.setValue(value, row);
        }
                
    }
    
}
