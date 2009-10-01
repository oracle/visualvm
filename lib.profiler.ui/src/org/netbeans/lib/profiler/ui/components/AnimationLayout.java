/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.ui.components;

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
