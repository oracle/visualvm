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

package com.sun.tools.visualvm.core.options;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

/**
 * JComponent to be used as a toplevel container for options category view.
 * The contents resizes horizontally and scrolls vertically. Default insets are
 * set, the contents doesn't need to specify its outer insets.
 *
 * @author Jiri Sedlacek
 */
public final class ScrollableContents extends JScrollPane {

    // --- Constructor ---------------------------------------------------------

    /**
     * Creates a new <code>ScrollableContents</code> containing the provided
     * component.
     *
     * @param component component to be wrapped by the ScollableContents
     */
    public ScrollableContents(JComponent component) {
        setViewportView(new ScrollableContainer(component));
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        setViewportBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
    }


    // --- Scrollable container ------------------------------------------------

    private static class ScrollableContainer extends JPanel implements Scrollable {

        public ScrollableContainer(JComponent contents) {
            setLayout(new BorderLayout());
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
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

    }

}
