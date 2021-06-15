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

import java.text.Format;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.ui.Formatters;

/**
 *
 * @author Jiri Sedlacek
 */
public class NumberRenderer extends FormattedLabelRenderer implements RelativeRenderer {

    private final Format outputFormat;

    protected boolean renderingDiff;

    public NumberRenderer() {
        this(null);
    }

    public NumberRenderer(Format outputFormat) {
        super(Formatters.numberFormat());

        this.outputFormat = outputFormat;

        setHorizontalAlignment(SwingConstants.TRAILING);
    }

    public void setDiffMode(boolean diffMode) {
        renderingDiff = diffMode;
    }

    public boolean isDiffMode() {
        return renderingDiff;
    }

    protected String getValueString(Object value, int row, Format format) {
        if (value == null) return "-"; // NOI18N
        String s = super.getValueString(value, row, format);
        s = outputFormat == null ? s : formatImpl(outputFormat, s);
        if (renderingDiff && value instanceof Number)
            if (((Number)value).doubleValue() >= 0) s = '+' + s; // NOI18N
        return s;
    }

}
