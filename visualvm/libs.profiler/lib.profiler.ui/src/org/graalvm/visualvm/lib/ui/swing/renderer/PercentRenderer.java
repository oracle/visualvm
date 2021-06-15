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
import org.graalvm.visualvm.lib.ui.Formatters;

/**
 *
 * @author Jiri Sedlacek
 */
public class PercentRenderer extends FormattedLabelRenderer implements RelativeRenderer {

    private static final String NUL = Formatters.percentFormat().format(0);
    private static final String NAN = NUL.replace('0', '-');  // NOI18N

    private long maxValue = 100;

    protected boolean renderingDiff;


    public PercentRenderer() {
        super(Formatters.percentFormat());
    }


    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public long getMaxValue() {
        return maxValue;
    }


    public void setDiffMode(boolean diffMode) {
        renderingDiff = diffMode;
    }

    public boolean isDiffMode() {
        return renderingDiff;
    }


    protected String getValueString(Object value, int row, Format format) {
        if (value == null) return "-"; // NOI18N

        StringBuilder s = new StringBuilder();
        s.append("("); // NOI18N

        if (maxValue == 0) {
            s.append(NAN);
        } else {
            double number = ((Number)value).doubleValue();
            if (number == 0) {
                if (renderingDiff) s.append('+'); // NOI18N
                s.append(NUL);
            } else {
                number = number / maxValue;
                if (renderingDiff && number > 0) s.append('+'); // NOI18N
                s.append(format.format(number));
            }
        }

        s.append(")"); // NOI18N
        return s.toString();
    }

}
