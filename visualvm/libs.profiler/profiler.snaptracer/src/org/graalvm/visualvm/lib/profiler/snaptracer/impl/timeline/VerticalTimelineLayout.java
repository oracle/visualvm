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

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
final class VerticalTimelineLayout implements LayoutManager2 {

    private final TimelineChart chart;


    // --- Constructor ---------------------------------------------------------

    VerticalTimelineLayout(TimelineChart chart) {
        this.chart = chart;
    }


    // --- Public API ----------------------------------------------------------

    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    public Dimension maximumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    public Dimension preferredLayoutSize(Container parent) {
        Dimension dim = new Dimension(0, Utils.checkedInt(chart.getChartContext().getViewHeight()));

        for (int i = 0; i < parent.getComponentCount(); i++)
            dim.width = Math.max(dim.width, parent.getComponent(i).
                                     getPreferredSize().width);

        return dim;
    }

    public void layoutContainer(Container parent) {
        int width = parent.getWidth();
        for (int i = 0; i < parent.getComponentCount(); i++) {
                ChartContext context = chart.getRow(i).getContext();
                parent.getComponent(i).setBounds(0, Utils.checkedInt(context.getViewportOffsetY() + chart.getOffsetY()),
                                                 width, context.getViewportHeight());
        }
    }


    // --- Implicit implementation ---------------------------------------------

    public void addLayoutComponent(Component comp, Object constraints) {}

    public void addLayoutComponent(String name, Component comp) {}

    public void removeLayoutComponent(Component comp) {}

    public float getLayoutAlignmentX(Container target) { return 0.5f; }

    public float getLayoutAlignmentY(Container target) { return 0.5f; }

    public void invalidateLayout(Container target) {}

}
