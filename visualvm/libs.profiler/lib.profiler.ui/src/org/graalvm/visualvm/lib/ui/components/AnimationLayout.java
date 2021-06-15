/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components;

import java.awt.*;


public class AnimationLayout implements LayoutManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Dimension lockedSize;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Constructs a new <code>AnimationLayout</code>.
     */
    public AnimationLayout() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setLockedSize(Dimension lockedSize) {
        this.lockedSize = lockedSize;
    }

    /**
     * Adds the specified component to the layout. Not used by this class.
     * @param name the name of the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Lays out the container. This method lets each component take
     * its preferred size by reshaping the components in the
     * target container in order to satisfy the alignment of
     * this <code>AnimationLayout</code> object.
     *
     * @param target the specified component being laid out
     * @see Container
     * @see java.awt.Container#doLayout
     */
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();

            if (target.getComponentCount() > 0) {
                Component m = target.getComponent(0);

                if (m.isVisible()) {
                    Dimension d = lockedSize;

                    if (d == null) {
                        d = target.getSize();
                        d.width -= insets.left;
                        d.width -= insets.right;
                        d.height -= insets.top;
                        d.height -= insets.bottom;
                    }

                    m.setLocation(insets.left, insets.top);
                    m.setSize(d.width, d.height);
                }
            }
        }
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i>
     * components contained in the specified target container.
     *
     * @param target the component which needs to be laid out
     * @return the minimum dimensions to lay out the
     *         subcomponents of the specified container
     * @see #preferredLayoutSize
     * @see java.awt.Container
     * @see java.awt.Container#doLayout
     */
    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            if (target.getComponentCount() > 0) {
                Component m = target.getComponent(0);

                if (m.isVisible()) {
                    dim = m.getMinimumSize();
                }
            }

            // actually this resizes the component instead of container - cannot be here      
            //    	Insets insets = target.getInsets();
            //    	dim.width += insets.left + insets.right;
            //    	dim.height += insets.top + insets.bottom;
            return dim;
        }
    }

    /**
     * Returns the preferred dimensions for this layout given the
     * <i>visible</i> components in the specified target container.
     *
     * @param target the component which needs to be laid out
     * @return the preferred dimensions to lay out the
     *         subcomponents of the specified container
     * @see Container
     * @see #minimumLayoutSize
     * @see java.awt.Container#getPreferredSize
     */
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            if (target.getComponentCount() > 0) {
                Component m = target.getComponent(0);

                if (m.isVisible()) {
                    dim = m.getPreferredSize();
                }
            }

            // actually this resizes the component instead of container - cannot be here
            //    	Insets insets = target.getInsets();
            //    	dim.width += insets.left + insets.right;
            //    	dim.height += insets.top + insets.bottom;
            //    	
            return dim;
        }
    }

    /**
     * Removes the specified component from the layout. Not used by
     * this class.
     * @param comp the component to remove
     * @see       java.awt.Container#removeAll
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Returns a string representation of this <code>AnimationLayout</code>
     * object and its values.
     *
     * @return a string representation of this layout
     */
    public String toString() {
        return getClass().getName() + ", lockedSize: " + lockedSize; // NOI18N
    }
}
