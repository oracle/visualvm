/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.snaptracer.impl.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.Box;

/**
 *
 * @author Jiri Sedlacek
 */
public final class VerticalLayout implements LayoutManager {

    private final boolean proportionalWidth;
    private final int vGap;


    public VerticalLayout(boolean proportionalWidth) {
        this(proportionalWidth, 0);
    }

    public VerticalLayout(boolean proportionalWidth, int vGap) {
        this.proportionalWidth = proportionalWidth;
        this.vGap = vGap;
    }


    public void layoutContainer(final Container parent) {
        final Insets insets = parent.getInsets();
        final int posX = insets.left;
        int posY = insets.top;
        final int width = parent.getWidth() - insets.left - insets.right;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                Dimension pref = comp.getPreferredSize();
                if (proportionalWidth) {
                    int w = Math.min(pref.width, width);
                    int o = (width - w) / 2;
                    comp.setBounds(posX, posY + o, w, pref.height);
                } else {
                    comp.setBounds(posX, posY, width, pref.height);
                }
                pref.height += vGap;
                posY += pref.height;
            }
        }
    }

    public Dimension minimumLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxWidth = 0;
        int visibleCount = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible() && !(comp instanceof Box.Filler)) {
                final Dimension size = comp.getPreferredSize();
                maxWidth = Math.max(maxWidth, size.width);
                d.height += size.height;
                visibleCount++;
            }
        }

        d.height += (visibleCount - 1) * vGap;
        d.width += maxWidth;

        return d;
    }

    public Dimension preferredLayoutSize(final Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension d = new Dimension(insets.left + insets.right,
                                          insets.top + insets.bottom);
        int maxWidth = 0;
        int visibleCount = 0;

        for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                final Dimension size = comp.getPreferredSize();
                maxWidth = Math.max(maxWidth, size.width);
                d.height += size.height;
                visibleCount++;
            }
        }

        d.height += (visibleCount - 1) * vGap;
        d.width += maxWidth;

        return d;
    }


    public void addLayoutComponent(final String name, final Component comp) {}

    public void removeLayoutComponent(final Component comp) {}

}
