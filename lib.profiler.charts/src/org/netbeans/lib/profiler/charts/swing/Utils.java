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

package org.netbeans.lib.profiler.charts.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.lang.reflect.Method;
import javax.swing.UIManager;

/**
 *
 * @author Jiri Sedlacek
 */
public final class Utils {

    private static boolean FORCE_SPEED = Boolean.getBoolean("graphs.forceSpeed"); // NOI18N

    private static boolean forceSpeed = FORCE_SPEED || !isLocalDisplay();
//    private static boolean forceSpeed = true;


    public static boolean forceSpeed() {
        return forceSpeed;
    }


    public static Color checkedColor(Color color) {
        if (color == null) return null;
        if (!forceSpeed || color.getAlpha() == 255) return color;
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static RenderingHints checkedRenderingHints(RenderingHints rHints) {
        if (!forceSpeed) return rHints;
        RenderingHints hints = (RenderingHints)rHints.clone();
        hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        return hints;
    }


    /**
     * Returns width of the Stroke. Note that this only works correctly for instances
     * of BasicStroke, for other Strokes it always returns 1. Returns zero width
     * for null Stroke.
     *
     * @param stroke Stroke
     * @return width of Stroke for instances of BasicStroke, 1 for other Stroke, 0 for null Stroke
     */
    public static float getStrokeWidth(Stroke stroke) {
        if (stroke == null) return 0f;
        if (!(stroke instanceof BasicStroke)) return 1f;
        return ((BasicStroke)stroke).getLineWidth();
    }


    public static Color getSystemSelection() {
        return UIManager.getColor("List.selectionBackground"); // NOI18N
    }


    // Copied from sun.swing.SwingUtilities2.isLocalDisplay()
    private static boolean isLocalDisplay() {
        try {
            // On Windows just return true. Permission to read os.name
            // is granted to all code but wrapped in try to be safe.
            if (System.getProperty("os.name").startsWith("Windows")) { // NOI18N
                return true;
            }
            // Else probably Solaris or Linux in which case may be remote X11
            Class x11Class = Class.forName("sun.awt.X11GraphicsEnvironment"); // NOI18N
            Method isDisplayLocalMethod = x11Class.getMethod(
                      "isDisplayLocal", new Class[0]); // NOI18N
            return (Boolean)isDisplayLocalMethod.invoke(null, (Object[])null);
        } catch (Throwable t) {
        }
        // If we get here we're most likely being run on some other O/S
        // or we didn't properly detect Windows.
        return true;
    }


    // --- long <-> int conversions --------------------------------------------

    public static final int VALUE_OUT_OF_RANGE_NEG = Integer.MIN_VALUE;
    public static final int VALUE_OUT_OF_RANGE_POS = Integer.MAX_VALUE;


    public static final int checkedInt(double value) {
        if (value < Integer.MIN_VALUE) return VALUE_OUT_OF_RANGE_NEG;
        if (value > Integer.MAX_VALUE) return VALUE_OUT_OF_RANGE_POS;
        else return (int)value;
    }

    public static final Rectangle checkedRectangle(LongRect rect) {
        // TODO: this is incorrect, width/height don't reflect x/y truncation!
        return new Rectangle(checkedInt(rect.x),
                            checkedInt(rect.y),
                            checkedInt(rect.width),
                            checkedInt(rect.height));
    }

}
