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


/**
 *
 * @author Jiri Sedlacek
 */
public class VerticalLayout implements LayoutManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int hgap;
    private int vgap;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public VerticalLayout() {
        this(10, 10);
    }

    public VerticalLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getHGap() {
        return hgap;
    }

    public int getVGap() {
        return vgap;
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void layoutContainer(Container container) {
        synchronized (container.getTreeLock()) {
            Insets insets = container.getInsets();

            int x = insets.left + hgap;
            int y = insets.top + vgap;
            int w = container.getSize().width - insets.left - insets.right - (2 * hgap);
            int h;

            for (int i = 0; i < container.getComponentCount(); i++) {
                Component component = container.getComponent(i);

                if (component.isVisible()) {
                    h = component.getPreferredSize().height;
                    component.setBounds(x, y, w, h);
                    y += (h + vgap);
                }
            }
        }
    }

    public Dimension minimumLayoutSize(Container container) {
        return preferredLayoutSize(container);
    }

    public Dimension preferredLayoutSize(Container container) {
        synchronized (container.getTreeLock()) {
            Insets insets = container.getInsets();

            int width = insets.left + insets.right + (2 * hgap);
            int height = insets.top + insets.bottom + vgap;

            for (int i = 0; i < container.getComponentCount(); i++) {
                Component component = container.getComponent(i);

                if (component.isVisible()) {
                    Dimension preferredDim = component.getPreferredSize();
                    width = Math.max(width, preferredDim.width + insets.left + insets.right + (2 * hgap));
                    height += (preferredDim.height + vgap);
                }
            }

            return new Dimension(width, height);
        }
    }

    public void removeLayoutComponent(Component comp) {
    }

    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ", vgap=" + vgap + "]"; // NOI18N
    }
}
