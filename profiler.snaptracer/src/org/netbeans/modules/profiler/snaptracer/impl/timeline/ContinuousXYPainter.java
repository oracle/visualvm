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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChartContext;

/**
 *
 * @author Jiri Sedlacek
 */
final class ContinuousXYPainter extends TimelineXYPainter {

    private static final Polygon POLYGON = new Polygon();
    
    protected final int lineWidth;
    protected final Color lineColor;
    protected final Color fillColor;
    protected final Color definingColor;

    protected final Stroke lineStroke;

    private final PointsComputer computer;


    ContinuousXYPainter(float lineWidth, Color lineColor, Color fillColor,
                        double dataFactor, PointsComputer computer) {

        super((int)Math.ceil(lineWidth), fillColor != null, dataFactor);

        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("lineColor or fillColor must not be null"); // NOI18N

        this.lineWidth = (int)Math.ceil(lineWidth);
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);

        definingColor = lineColor != null ? lineColor : fillColor;

        this.lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND);

        this.computer = computer;
    }


    protected Color getDefiningColor() {
        return definingColor;
    }

    protected void paint(XYItem item, List<ItemSelection> highlighted,
                         List<ItemSelection> selected, Graphics2D g,
                         Rectangle dirtyArea, SynchronousXYChartContext context) {

        int valuesCount = item.getValuesCount();
        int extraTrailing = fillColor != null ? 2 : 0;

        Rectangle dirtyExtended = new Rectangle(dirtyArea);
        dirtyExtended.x -= lineWidth;
        dirtyExtended.width += lineWidth * 2;
        int[][] idxs = computer.getVisible(dirtyExtended, valuesCount, context, 1,
                                           extraTrailing);
        if (idxs == null) return;
        int[] visibleIndexes = idxs[0];
        int npoints = idxs[1][0];
        int[][] points = computer.createPoints(visibleIndexes, npoints, item,
                                               dataFactor, context);

        if (fillColor != null) {
            points[0][npoints - 2] = points[0][npoints - 3];
            points[1][npoints - 2] = computer.getZeroY(context);
            points[0][npoints - 1] = points[0][0];
            points[1][npoints - 1] = points[1][npoints - 2];

            POLYGON.xpoints = points[0];
            POLYGON.ypoints = points[1];
            POLYGON.npoints = npoints;

            g.setPaint(fillColor);
            g.fill(POLYGON);
        }

        if (lineColor != null) {
            g.setPaint(lineColor);
            g.setStroke(lineStroke);
            g.drawPolyline(points[0], points[1], npoints - extraTrailing);
        }
    }

}
