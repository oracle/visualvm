/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer.impl.swing;

import org.graalvm.visualvm.uisupport.UISupport;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JScrollBar;

/**
 * Use only for creating ScrollBars which mimic insets of JScrollPane's SBs.
 *
 * @author Jiri Sedlacek
 */
public class ScrollBar extends JScrollBar {

    public ScrollBar(int orientation) {
        super(orientation);

        if (UISupport.isGTKLookAndFeel()) {
            Insets insets = getBorder().getBorderInsets(this);
            // Typically the insets are 2 for GTK themes except for Nimbus theme
            // which uses 3 and requires 1 (other themes seem to require 0). Lets
            // lower the insets to mimic JScrollBars used in JScrollPanes.
            setBorder(BorderFactory.createEmptyBorder(Math.max(insets.top - 2, 0),
                                                    Math.max(insets.left - 2, 0),
                                                    Math.max(insets.bottom - 2, 0),
                                                    Math.max(insets.right - 2, 0)
                                                   ));
        }
    }

}
