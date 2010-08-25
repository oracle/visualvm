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

import org.netbeans.modules.profiler.snaptracer.impl.timeline.items.ContinuousXYItemDescriptor;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.items.DiscreteXYItemDescriptor;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.items.ValueItemDescriptor;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.items.XYItemDescriptor;
import java.awt.Color;
import org.netbeans.modules.profiler.snaptracer.ProbeItemDescriptor;
import org.netbeans.modules.profiler.snaptracer.impl.IdeSnapshot;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.items.IconItemDescriptor;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelinePaintersFactory {

    static TimelineXYPainter createPainter(ProbeItemDescriptor descriptor,
                                           int itemIndex, PointsComputer c,
                                           IdeSnapshot snapshot) {

        // --- ValueItem -------------------------------------------------------
        if (descriptor instanceof ValueItemDescriptor)
            return createValuePainter((ValueItemDescriptor)descriptor, itemIndex, c, snapshot);

        return null;
    }

    private static TimelineXYPainter createValuePainter(
            ValueItemDescriptor descriptor, int itemIndex, PointsComputer c,
            IdeSnapshot snapshot) {

        // --- XYItem ----------------------------------------------------------
        if (descriptor instanceof ContinuousXYItemDescriptor)
            return createContinuousPainter((ContinuousXYItemDescriptor)descriptor, itemIndex, c);
        
        // --- BarItem ---------------------------------------------------------
        if (descriptor instanceof DiscreteXYItemDescriptor)
            return createDiscretePainter((DiscreteXYItemDescriptor)descriptor, itemIndex, c);

        // --- IconItem --------------------------------------------------------
        if (descriptor instanceof IconItemDescriptor)
            return createIconPainter((IconItemDescriptor)descriptor, itemIndex, snapshot);

        return null;
    }

    private static TimelineXYPainter createContinuousPainter(
            XYItemDescriptor descriptor, int itemIndex, PointsComputer c) {

        double dataFactor = descriptor.getDataFactor();

        float lineWidth = descriptor.getLineWidth();
        if (lineWidth == ProbeItemDescriptor.DEFAULT_LINE_WIDTH)
            lineWidth = 2f;

        Color lineColor = descriptor.getLineColor();
        if (lineColor == ProbeItemDescriptor.DEFAULT_COLOR)
            lineColor = TimelineColorFactory.getColor(itemIndex);

        Color fillColor = descriptor.getFillColor();
        if (fillColor == ProbeItemDescriptor.DEFAULT_COLOR) {
            if (lineColor == null)
                fillColor = TimelineColorFactory.getColor(itemIndex);
            else
                fillColor = TimelineColorFactory.getGradient(itemIndex)[0];
        }

        return new ContinuousXYPainter(lineWidth, lineColor, fillColor, dataFactor, c);
    }

    private static DiscreteXYPainter createDiscretePainter(
            DiscreteXYItemDescriptor descriptor, int itemIndex, PointsComputer c) {

        double dataFactor = descriptor.getDataFactor();

        float lineWidth = descriptor.getLineWidth();
        if (lineWidth == ProbeItemDescriptor.DEFAULT_LINE_WIDTH)
            lineWidth = 2f;

        Color lineColor = descriptor.getLineColor();
        if (lineColor == ProbeItemDescriptor.DEFAULT_COLOR)
            lineColor = TimelineColorFactory.getColor(itemIndex);

        Color fillColor = descriptor.getFillColor();
        if (fillColor == ProbeItemDescriptor.DEFAULT_COLOR) {
            if (lineColor == null)
                fillColor = TimelineColorFactory.getColor(itemIndex);
            else
                fillColor = TimelineColorFactory.getGradient(itemIndex)[0];
        }

        return new DiscreteXYPainter(lineWidth, lineColor, fillColor, descriptor.getWidth(),
                                     descriptor.isFixedWidth(), descriptor.isTopLineOnly(),
                                     descriptor.isOutlineOnly(), dataFactor, c);
    }

    private static TimelineIconPainter createIconPainter(
            IconItemDescriptor descriptor, int itemIndex, IdeSnapshot snapshot) {

        Color color = descriptor.getColor();
        if (color == ProbeItemDescriptor.DEFAULT_COLOR)
            color = TimelineColorFactory.getColor(itemIndex);

        return new TimelineIconPainter(color, snapshot);
    }

}
