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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ColorIcon implements javax.swing.Icon {

    private static int WIDTH = 8;
    private static int HEIGHT = 8;
    private static Color BACKGROUND_COLOR = Color.WHITE;
    private static Color FOREGROUND_COLOR = Color.BLACK;

    public static final ColorIcon BOTTOM_SHADOW = new ColorIcon(null, true);

    private final boolean shadow;
    private final Color color;

    private static final Map<Color, ColorIcon> icons = new HashMap();


    private ColorIcon(Color color) {
        this(color, false);
    }

    private ColorIcon(Color color, boolean shadow) {
        this.color = color;
        this.shadow = shadow;
    }


    public static void setup(int width, int height, Color foreground, Color background) {
        WIDTH = width;
        HEIGHT = height;
        BACKGROUND_COLOR = background;
        FOREGROUND_COLOR = foreground;
        icons.clear();
    }

    public static ColorIcon fromColor(Color color) {
        ColorIcon icon = icons.get(color);
        if (icon == null) {
            icon = new ColorIcon(color);
            icons.put(color, icon);
        }
        return icon;
    }


    public int getIconWidth() {
        return WIDTH;
    }

    public int getIconHeight() {
        return HEIGHT;
    }

    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        if (shadow) {
            g.setColor(BACKGROUND_COLOR);
            g.drawLine(x, y + HEIGHT + 1, x + WIDTH - 1, y + HEIGHT + 1);
        } else {
            g.setColor(color);
            g.fillRect(x, y, WIDTH, HEIGHT);
            g.setColor(FOREGROUND_COLOR);
            g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
        }
    }
}
