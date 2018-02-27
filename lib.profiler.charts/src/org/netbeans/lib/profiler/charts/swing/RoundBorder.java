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

package org.netbeans.lib.profiler.charts.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.border.AbstractBorder;

/**
 *
 * @author Jiri Sedlacek
 */
public class RoundBorder extends AbstractBorder {

    private final Color lineColor;
    private final Color fillColor;
    private final Stroke borderStroke;

    private final int arcRadius;
    private final int borderExtent;

    private final int borderStrokeWidth;
    private final int halfBorderStrokeWidth;
    private final int inset;

    private final boolean forceSpeed;


    public RoundBorder(float lineWidth, Color lineColor, Color fillColor,
                       int arcRadius, int borderExtent) {
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);
        this.arcRadius = arcRadius;
        this.borderExtent = borderExtent;

        borderStroke = new BasicStroke(lineWidth);

        borderStrokeWidth = (int)lineWidth;
        halfBorderStrokeWidth = borderStrokeWidth / 2;
        inset = borderStrokeWidth + borderExtent;

        forceSpeed = Utils.forceSpeed();
    }


    public Insets getBorderInsets(Component c)       {
        return new Insets(inset, inset, inset, inset);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = inset;
        return insets;
    }


    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D)g;

        g2.setPaint(fillColor);
        // NOTE: fillRoundRect seems to have poor performance on Linux
//            g2.fillRoundRect(x + halfBorderStrokeWidth, y + halfBorderStrokeWidth,
//                             width - borderStrokeWidth, height - borderStrokeWidth,
//                             arcRadius * 2, arcRadius * 2);

        int arcRadius2 = arcRadius * 2;
        int arcRadius2p1 = arcRadius2 + 1;

        g2.fillArc(x, y, arcRadius2, arcRadius2, 90, 90);
        g2.fillArc(x + width - arcRadius2p1, y, arcRadius2, arcRadius2, 0, 90);
        g2.fillArc(x, y + height - arcRadius2p1, arcRadius2, arcRadius2, 180, 90);
        g2.fillArc(x + width - arcRadius2p1, y + height - arcRadius2p1, arcRadius2, arcRadius2, 270, 90);

        g2.fillRect(x + arcRadius, y, width - arcRadius2p1, height);
        g2.fillRect(x, y + arcRadius, arcRadius, height - arcRadius2p1);
        g2.fillRect(x + width - arcRadius - 1, y + arcRadius, arcRadius, height - arcRadius2p1);

        Object aa = null;
        Object sc = null;
        if (!forceSpeed) {
            aa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            sc = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }

        g2.setStroke(borderStroke);
        g2.setPaint(lineColor);
        g2.drawRoundRect(x + halfBorderStrokeWidth, y + halfBorderStrokeWidth,
                         width - borderStrokeWidth, height - borderStrokeWidth,
                         arcRadius * 2, arcRadius * 2);

        if (!forceSpeed) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, sc);
        }
    }

}
