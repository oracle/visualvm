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

package org.graalvm.visualvm.lib.ui;

import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public final class Formatters {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.Bundle"); // NOI18N

    private static NumberFormat NUMBER_FORMAT;
    /**
     * Returns Format instance for formatting numbers according to current Locale.
     *
     * @return Format instance for formatting numbers according to current Locale
     */
    public static Format numberFormat() {
        if (NUMBER_FORMAT == null) {
            NUMBER_FORMAT = NumberFormat.getNumberInstance();
            NUMBER_FORMAT.setGroupingUsed(true);
        }
        return NUMBER_FORMAT;
    }

    private static NumberFormat PERCENT_FORMAT;
    /**
     * Returns Format instance for formatting percents according to current Locale.
     *
     * @return Format instance for formatting percents according to current Locale
     */
    public static Format percentFormat() {
        if (PERCENT_FORMAT == null) {
            PERCENT_FORMAT = NumberFormat.getPercentInstance();
            PERCENT_FORMAT.setMaximumFractionDigits(1);
            PERCENT_FORMAT.setMinimumFractionDigits(0);
        }
        return PERCENT_FORMAT;
    }

    private static Format MILLISECONDS_FORMAT;
    /**
     * Returns Format instance to post-process a formatted milliseconds value.
     * By default adds a " ms" suffix to a formatted long value.
     *
     * @return Format instance to post-process a formatted milliseconds value
     */
    public static Format millisecondsFormat() {
        if (MILLISECONDS_FORMAT == null) {
            MILLISECONDS_FORMAT = new MessageFormat(BUNDLE.getString("Formatters.MillisecondsFormat")); // NOI18N
        }
        return MILLISECONDS_FORMAT;
    }
    
    private static Format BYTES_FORMAT;
    /**
     * Returns Format instance to post-process a formatted Bytes (B) value.
     * By default adds a " B" suffix to a formatted long value.
     * 
     * @return Format instance to post-process a formatted Bytes value
     */
    public static Format bytesFormat() {
        if (BYTES_FORMAT == null) {
            BYTES_FORMAT = new MessageFormat(BUNDLE.getString("Formatters.BytesFormat")); // NOI18N
        }
        return BYTES_FORMAT;
    }
    
}
