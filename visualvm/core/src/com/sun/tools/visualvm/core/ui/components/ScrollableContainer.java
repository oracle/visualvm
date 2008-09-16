/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 * Predefined JScrollPane to be used for details views. Scrollbars are displayed
 * only when needed.
 *
 * @author Jiri Sedlacek
 */
public final class ScrollableContainer extends JScrollPane {

    /**
     * Creates new instance of ScrollableContainer.
     * 
     * @param view component to be displayed.
     */
    public ScrollableContainer(JComponent view) {
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());

        getViewport().setOpaque(false);
        setViewportBorder(BorderFactory.createEmptyBorder());
        
        getVerticalScrollBar().setUnitIncrement(20);
        getVerticalScrollBar().setBlockIncrement(50);
        getHorizontalScrollBar().setUnitIncrement(20);
        getHorizontalScrollBar().setBlockIncrement(50);

        setViewportView(view);
    }

}
