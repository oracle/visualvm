/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;

/**
 *
 * @author Jiri Sedlacek
 */
public final class DurationFormatter {
    
    private static final NumberFormat DECIMAL_LZ_FORMAT;
    private static final NumberFormat DECIMAL_NLZ_FORMAT;
    
    private static final NumberFormat DURATION_MS_FORMAT;
    
    static {
        DECIMAL_LZ_FORMAT = new DecimalFormat("00"); // NOI18N
        DECIMAL_NLZ_FORMAT = new DecimalFormat("#0"); // NOI18N
        
        DURATION_MS_FORMAT = NumberFormat.getNumberInstance();
        DURATION_MS_FORMAT.setMaximumFractionDigits(3);
        DURATION_MS_FORMAT.setMinimumFractionDigits(3);
    }
    
    
    private DurationFormatter() {}
    
    
    public static String format(Duration d) {
        return format(d, new StringBuffer()).toString();
    }
    
    public static StringBuffer format(Duration d, StringBuffer b) {
        if (Long.MAX_VALUE == d.toMillis()) return b.append("∞"); // NOI18N
            
            long s = d.getSeconds();
            if (s > 0) formatSeconds(s, b);
            
            int n = d.getNano();
            return b.append(DURATION_MS_FORMAT.format(n / 1000000f)).append(" ms"); // NOI18N
        }
        
        private static StringBuffer formatSeconds(long seconds, StringBuffer b) {
            // Hours
            long hours = seconds / 3600;
            if (hours > 0) b.append(DECIMAL_NLZ_FORMAT.format(hours)).append(" h "); // NOI18N
            seconds %= 3600;
            
            // Minutes
            long minutes = seconds / 60;
            if (minutes > 0 || hours > 0) b.append((hours > 0 ? DECIMAL_LZ_FORMAT : DECIMAL_NLZ_FORMAT).format(minutes)).append(" m "); // NOI18N
            seconds %= 60;
            
            // Seconds
            return b.append((minutes > 0 || hours > 0 ? DECIMAL_LZ_FORMAT : DECIMAL_NLZ_FORMAT).format(seconds)).append(" s "); // NOI18N
        }
    
}
