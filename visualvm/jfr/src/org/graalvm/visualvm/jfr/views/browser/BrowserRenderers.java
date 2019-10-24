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
package org.graalvm.visualvm.jfr.views.browser;

import javax.swing.JLabel;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
final class BrowserRenderers {
    
    private static int getMinimumWidth(String valueName) {
        return new JLabel(valueName).getPreferredSize().width + 30;
    }
    
    
    static class NameRenderer extends NormalBoldGrayRenderer {
        
        private boolean showsCount;
        
        void setShowsCount(boolean showsCount) {
            this.showsCount = showsCount;
        }
            
        public void setValue(Object value, int row) {
            if (value instanceof BrowserNode) {
                BrowserNode node = (BrowserNode)value;
                boolean cat = node instanceof BrowserNode.Category;
                setNormalValue(cat ? null : node.toString());
                setBoldValue(cat ? node.toString() : null);
                setGrayValue(showsCount ? (node.eventsCount == 1 ? " (1 event)" : " (" + node.eventsCount + " events)") : null);
            } else {
                super.setValue(value, row);
            }
        }

        static String getDisplayName() {
            return "Name";
        }

    }
    
    
    static class TypeIDRenderer extends LabelRenderer {
        
        static String getDisplayName() {
            return "ID";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
//            setText("com.oracle.jdk.MetaspaceChunkFreeListSummary"); // NOI18N
            setText("jdk.MetaspaceChunkFreeListSummary"); // NOI18N
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }

    }
    
    
    static class EventsCountRenderer extends NumberRenderer {
        
        static String getDisplayName() {
            return "Count";
        }
        
        static boolean isInitiallyVisible() {
            return false;
        }
        
        int getPreferredWidth() {
            setValue(1234567, -1);
            return Math.max(getPreferredSize().width, getMinimumWidth(getDisplayName()));
        }
        
    }
    
}
