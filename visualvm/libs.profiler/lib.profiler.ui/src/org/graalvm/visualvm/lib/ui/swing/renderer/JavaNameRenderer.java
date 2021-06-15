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

package org.graalvm.visualvm.lib.ui.swing.renderer;

import javax.swing.Icon;
import org.graalvm.visualvm.lib.ui.results.PackageColorer;

/**
 *
 * @author Jiri Sedlacek
 */
public class JavaNameRenderer extends NormalBoldGrayRenderer {

    private final Icon icon;

    public JavaNameRenderer() {
        this(null);
    }

    public JavaNameRenderer(Icon icon) {
        this.icon = icon;
    }

    public void setValue(Object value, int row) {
        if (value == null) {
            setNormalValue(""); // NOI18N
            setBoldValue(""); // NOI18N
            setGrayValue(""); // NOI18N
        } else {
            String name = value.toString();
            String gray = ""; // NOI18N

            int bracketIndex = name.indexOf('('); // NOI18N
            if (bracketIndex != -1) {
                gray = " " + name.substring(bracketIndex); // NOI18N
                name = name.substring(0, bracketIndex);
            }

            int dotIndex = name.lastIndexOf('.'); // NOI18N
            setNormalValue(name.substring(0, dotIndex + 1));
            setBoldValue(name.substring(dotIndex + 1));
            setGrayValue(gray);
        }
        setIcon(icon);
    }


    // TODO: optimize to not slow down sort/search/filter by resolving color!
    protected void setNormalValue(String value) {
        super.setNormalValue(value);
        setCustomForeground(PackageColorer.getForeground(value));
    }

}
