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

package org.graalvm.visualvm.charts.xy;

import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartDecorator;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import org.graalvm.visualvm.lib.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class XYBackground implements ChartDecorator {

    private static final Color GRADIENT_TOP = new Color(240, 240, 240);
    private static final Color GRADIENT_BOTTOM = new Color(250, 250, 250);

    public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        if (Utils.forceSpeed()) g.setPaint(GRADIENT_BOTTOM);
        else g.setPaint(new GradientPaint(
                        new Point(0, Utils.checkedInt(context.getViewportOffsetY())),
                        GRADIENT_TOP,
                        new Point(0, context.getViewportHeight()),
                        GRADIENT_BOTTOM));
        g.fill(dirtyArea);
    }

}
