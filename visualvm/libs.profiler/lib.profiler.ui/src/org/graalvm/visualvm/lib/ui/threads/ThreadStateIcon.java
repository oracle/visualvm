/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.threads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;


/**
 * @author Jiri Sedlacek
 */
public class ThreadStateIcon implements Icon {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int ICON_NONE = -100;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected Color threadStateColor;
    protected int height;
    protected int width;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ThreadStateIcon(int threadState, int width, int height) {
        this.threadStateColor = getThreadStateColor(threadState);
        this.width = width;
        this.height = height;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getIconHeight() {
        return height;
    }

    public int getIconWidth() {
        return width;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (threadStateColor != null) {
            g.setColor(threadStateColor);
            g.fillRect(x + 1, y + 1, width - 1, height - 1);
        }
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width - 1, height - 1);
    }

    protected Color getThreadStateColor(int threadState) {
        if (threadState == ICON_NONE) return null;
        return ThreadData.getThreadStateColor(threadState);
    }
}
