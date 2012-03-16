/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.core.ui.components;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * Predefined JScrollPane to be used in VisualVM, for example in details views.
 * Use UISupport.createScrollableContainer() method instead of instantiating
 * this class directly if creating scrollable container for the Options panel.
 *
 * @author Jiri Sedlacek
 */
public final class ScrollableContainer extends JScrollPane {

    /**
     * Creates new instance of ScrollableContainer.
     * 
     * @param view component to be displayed
     */
    public ScrollableContainer(JComponent view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED,
             HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Creates new instance of ScrollableContainer.
     *
     * @param view component to be displayed
     * @param vsbPolicy policy flag for the vertical scrollbar
     * @param hsbPolicy policy flag for the horizontal scrollbar
     */
    public ScrollableContainer(JComponent view, int vsbPolicy, int hsbPolicy) {
        setViewportView(new ScrollableContents(view));

        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);

        setBorder(BorderFactory.createEmptyBorder());
        setViewportBorder(BorderFactory.createEmptyBorder());

        getViewport().setOpaque(false);
        setOpaque(false);
    }


    // --- Scrollable container ------------------------------------------------

    private class ScrollableContents extends JPanel implements Scrollable {

        private ScrollableContents(JComponent contents) {
            super(new BorderLayout());
            setOpaque(false);
            add(contents, BorderLayout.CENTER);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect,
                                              int orientation, int direction) {
            return 20;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect,
                                               int orientation, int direction) {
            return (int)(visibleRect.height * 0.9d);
        }

        public boolean getScrollableTracksViewportWidth() {
            if (getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_NEVER)
                return true;

            Container parent = getParent();
            if ((parent == null) || !(parent instanceof JViewport)) return false;
            return getMinimumSize().width < ((JViewport)parent).getWidth();
        }

        public boolean getScrollableTracksViewportHeight() {
            if (getVerticalScrollBarPolicy() == VERTICAL_SCROLLBAR_NEVER)
                return true;

            Container parent = getParent();
            if ((parent == null) || !(parent instanceof JViewport)) return false;
            return getMinimumSize().height < ((JViewport)parent).getHeight();
        }

    }

}
