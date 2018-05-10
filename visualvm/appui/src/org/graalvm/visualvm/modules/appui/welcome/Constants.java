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

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Stroke;

/**
 *
 * @author S. Aubrecht
 */
public interface Constants {

    static final String COLOR_SCREEN_BACKGROUND = "ScreenBackgroundColor"; //NOI18N
    static final String COLOR_CONTENT_BACKGROUND = "ContentBackgroundColor"; //NOI18N
    static final String BORDER_COLOR = "BorderColor"; //NOI18N
    
    
    static final int FONT_SIZE = Utils.getDefaultFontSize();
    static final Font BUTTON_FONT = new Font( null, Font.BOLD, FONT_SIZE );

    static final Stroke LINK_IN_FOCUS_STROKE = new BasicStroke(1, BasicStroke.CAP_SQUARE,
        BasicStroke.JOIN_BEVEL, 0, new float[] {0, 2}, 0);
    static final String LINK_IN_FOCUS_COLOR = "LinkInFocusColor"; //NOI18N
    static final String LINK_COLOR = "LinkColor"; //NOI18N
    static final String MOUSE_OVER_LINK_COLOR = "MouseOverLinkColor"; //NOI18N
    static final String VISITED_LINK_COLOR = "VisitedLinkColor"; //NOI18N

    
    static final int START_PAGE_MIN_WIDTH = 600;
}
