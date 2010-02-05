/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeaderLabel extends HeaderPanel {

    public static final int DEFAULT_HEIGHT =
            new HeaderLabel("X").getPreferredSize().height; // NOI18N


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

}
