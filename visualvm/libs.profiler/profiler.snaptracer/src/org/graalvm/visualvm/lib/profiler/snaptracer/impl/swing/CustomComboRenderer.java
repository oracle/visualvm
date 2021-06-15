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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing;

import java.awt.Component;
import java.text.NumberFormat;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class CustomComboRenderer implements ListCellRenderer {

    private final JComboBox combo;
    private final ListCellRenderer renderer;
    private final JLabel rendererL;


    private CustomComboRenderer(JComboBox combo) {
        this.combo = combo;
        renderer = combo.getRenderer();
        if (renderer instanceof JLabel) rendererL = (JLabel)renderer;
        else rendererL = null;

        this.combo.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)   { repaint(); }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { repaint(); }
            public void popupMenuCanceled(PopupMenuEvent e)            { repaint(); }
            private void repaint() { CustomComboRenderer.this.combo.repaint(); }
        });
    }


    protected void setupRenderer(ListCellRenderer renderer, boolean popupVisible) {}

    protected void setupRenderer(JLabel renderer, boolean popupVisible) {}

    public abstract java.lang.String value(Object value);


    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {

        if (rendererL != null) setupRenderer(rendererL, combo.isPopupVisible());
        else setupRenderer(renderer, combo.isPopupVisible());
        
        return renderer.getListCellRendererComponent(list, value(value), index,
                                                     isSelected, cellHasFocus);
    }


    public static final class String extends CustomComboRenderer {

        public String(JComboBox combo) {
            super(combo);
        }

        public java.lang.String value(Object value) {
            return value == null ? "null" : value.toString(); // NOI18N
        }

    }


    public static final class Boolean extends CustomComboRenderer {

        public Boolean(JComboBox combo) {
            super(combo);
        }

        public java.lang.String value(Object value) {
            if (java.lang.Boolean.TRUE.equals(value)) return "enabled";
            if (java.lang.Boolean.FALSE.equals(value)) return "disabled";
            return "default";
        }

    }


    public static final class Number extends CustomComboRenderer {

        private final java.lang.String units;
        private final boolean lAlign;

        public Number(JComboBox combo, java.lang.String units, boolean lAlign) {
            super(combo);
            this.units = units;
            this.lAlign = lAlign;
        }

        protected void setupRenderer(JLabel renderer, boolean popupVisible) {
            if (popupVisible || !lAlign) renderer.setHorizontalAlignment(SwingConstants.TRAILING);
            else renderer.setHorizontalAlignment(SwingConstants.LEADING);
        }

        public java.lang.String value(Object value) {
            java.lang.String sunits = units == null ? "" : " " + units;
            return Integer.valueOf(-1).equals(value) ? "default" :
                   NumberFormat.getInstance().format(value) + sunits;
        }

    }

}
