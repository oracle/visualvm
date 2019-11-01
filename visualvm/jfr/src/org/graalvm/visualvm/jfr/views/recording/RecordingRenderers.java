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
package org.graalvm.visualvm.jfr.views.recording;

import java.awt.Font;
import java.text.NumberFormat;
import java.time.Duration;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.utils.DurationFormatter;
import org.graalvm.visualvm.jfr.utils.InstantFormatter;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.swing.renderer.FormattedLabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
final class RecordingRenderers {
    
    private static int getMinimumWidth(String valueName) {
        return new JLabel(valueName).getPreferredSize().width + 30;
    }
    
    
    static class NameRenderer extends LabelRenderer {
        
        private static Font regular;
        private static Font bold;
            
        public void setValue(Object value, int row) {
            if (value instanceof RecordingNode) {
                RecordingNode node = (RecordingNode)value;
                RecordingNode parent = node.getParent();
                setFont(node.getChildCount() > 0 && parent != null && parent.getParent() == null ? bold() : regular());
                setText(node.name);
            } else {
                setFont(value == null || "<no recordings>".equals(value.toString()) ? regular() : bold());
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
    
    
    static class ValueRenderer extends LabelRenderer {
        
        ValueRenderer() {
            setHorizontalAlignment(TRAILING);
        }

        static String getDisplayName() {
            return "Value";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue("referenceValueString", -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }

    }
    
    static class ThreadRenderer extends LabelRenderer {
        
        ThreadRenderer() {
            setHorizontalAlignment(TRAILING);
        }

        static String getDisplayName() {
            return "Thread";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue("aLongThreadNameServingAsATemplate", -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }

    }
    
    
    static class TimeRenderer extends LabelRenderer {
        
        private final JFRModel model;
        
        TimeRenderer(JFRModel model) {
            this.model = model;
            setHorizontalAlignment(TRAILING);
        }
        
        public void setValue(Object value, int row) {
            long nanos = value instanceof Long ? (Long)value : Long.MIN_VALUE;
            setText(nanos > Long.MIN_VALUE ? InstantFormatter.format(model.nsToAbsoluteTime(nanos)) : ""); // NOI18N
        }
        
        static String getDisplayName() {
            return "Time";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(System.currentTimeMillis(), -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName())) + 30;
        }
        
    }
    
    static class StartRenderer extends TimeRenderer {
        
        StartRenderer(JFRModel model) {
            super(model);
        }
        
        static String getDisplayName() {
            return "Start";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
    }
    
    static class DurationRenderer extends LabelRenderer {
        
        DurationRenderer() {
            setHorizontalAlignment(TRAILING);
        }
        
        @Override
        public void setValue(Object value, int row) {
            if (value == null) {
                setText("");
            } else if (value instanceof Duration) {
                Duration duration = (Duration)value;
                setText(DurationFormatter.format(duration));
            } else if (value instanceof Long) {
                long duration = (Long)value;
                if (duration == -1) setText("");
                else setText(DurationFormatter.format(Duration.ofNanos(duration)));
            } else {
                super.setValue(value, row);
            }
        }
        
        static String getDisplayName() {
            return "Duration";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(Long.valueOf(999999999), -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class IdRenderer extends FormattedLabelRenderer {
        
        IdRenderer() {
            super(NumberFormat.getNumberInstance());
            setHorizontalAlignment(TRAILING);
        }
        
        public void setValue(Object value, int row) {
            if (value instanceof Long && ((Long)value) == -1) setText("");
            else super.setValue(value, row);
        }
        
        static String getDisplayName() {
            return "Id";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(Long.valueOf(123456), -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName())) + 30;
        }
        
    }
    
    static class SizeRenderer extends FormattedLabelRenderer {
        
        SizeRenderer() {
            super(Formatters.bytesFormat());
            setHorizontalAlignment(SwingConstants.TRAILING);
        }
        
        @Override
        public void setValue(Object value, int row) {
            if (!(value instanceof Long) || ((Long)value) >= 0) super.setValue(value, row);
            else setText("");
//            if (!(value instanceof Long) || ((Long)value) > 0) super.setValue(value, row);
//            else if (((Long)value) == 0) setText("-");
//            else setText("");
        }
        
        static String getDisplayName() {
            return "Max Size";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(99999999999999l, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
                
    }
    
    static class AgeRenderer extends DurationRenderer {
        
        static String getDisplayName() {
            return "Max Age";
        }
        
        static boolean isInitiallyVisible() {
            return true;
        }
        
        int getPreferredWidth() {
            setValue(Long.valueOf(999999999), -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
    static class DestinationRenderer extends LabelRenderer {
        
        DestinationRenderer() {
            setHorizontalAlignment(TRAILING);
        }
        
        static String getDisplayName() {
            return "Destination";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue("aLongDestinationPathServingAsATemplate", -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
}
