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

package org.graalvm.visualvm.lib.ui.swing;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.JLabel;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class GrayLabel extends JLabel {

    { setFocusable(false); }


    public GrayLabel() { super(); }

    public GrayLabel(Icon icon) { super(icon); }

    public GrayLabel(String text) { super(text); }

    public GrayLabel(Icon icon, int alignment) { super(icon, alignment); }

    public GrayLabel(String text, int alignment) { super(text, alignment); }

    public GrayLabel(String text, Icon icon, int alignment) { super(text, icon, alignment); }


    public Color getForeground() {
        return UIUtils.getDisabledLineColor();
    }


    public void setEnabled(boolean enabled) {
        super.setEnabled(true); // To workaround the 3D look on some LaFs
    }

}
