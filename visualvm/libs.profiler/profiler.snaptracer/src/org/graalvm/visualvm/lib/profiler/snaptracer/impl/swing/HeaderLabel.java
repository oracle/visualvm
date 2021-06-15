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
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public final class HeaderLabel extends HeaderPanel {

    public static final int DEFAULT_HEIGHT = computeHeight();


    private String text;
    private int hAlign = SwingConstants.CENTER;


    public HeaderLabel() {
        this(""); // NOI18N
    }

    public HeaderLabel(String text) {
        this.text = text;
    }


    public final void setText(String text) {
        this.text = text;
        repaint();
    }

    public final String getText() {
        return text;
    }

    public final void setHorizontalAlignment(int align) {
        hAlign = align;
        repaint();
    }

    public final int getHorizontalAlignment() {
        return hAlign;
    }


    protected Object getRendererValue() {
        return getText();
    }


    protected void setupRenderer(Component renderer) {
        if (renderer instanceof JLabel) {
            JLabel label = (JLabel)renderer;
            label.setHorizontalAlignment(hAlign);
        }
    }


    public Dimension getPreferredSize() {
        Dimension dim = getPreferredSizeSuper();
        dim.height = DEFAULT_HEIGHT;
        return dim;
    }

    private Dimension getPreferredSizeSuper() {
        return super.getPreferredSize();
    }


    private static int computeHeight() {
        int height = new HeaderLabel("X").getPreferredSizeSuper().height; // NOI18N
        if (UIUtils.isMetalLookAndFeel()) height += 4;
//        else if (UISupport.isAquaLookAndFeel()) height += 6;
        return height;
    }

}
