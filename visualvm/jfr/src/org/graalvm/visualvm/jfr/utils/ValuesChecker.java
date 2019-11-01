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

import java.time.Duration;
import java.time.Instant;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ValuesChecker {
    
    private ValuesChecker() {}
    
    
    public static boolean isNAValue(Object o) {
        if (o instanceof Number) {
            if (o instanceof Long) return isNALong((Long)o);
            if (o instanceof Integer) return isNAInteger((Integer)o);
            if (o instanceof Double) return isNADouble((Double)o);
            if (o instanceof Float) return isNAFloat(((Float)o));
            if (o instanceof Short) return isNAShort((Short)o);
            if (o instanceof Byte) isNAByte((Byte)o);
        } else if (o instanceof Instant) {
            return isNAInstant((Instant)o);
        } else if (o instanceof Duration) {
            return isNADuration((Duration)o);
        }
        
        return false;
    }
    
    
    public static boolean isNALong(Long val) { return Long.MIN_VALUE == val; }
    
    public static boolean isNAInteger(Integer val) { return Integer.MIN_VALUE == val; }
    
    public static boolean isNADouble(Double val) { return Double.NEGATIVE_INFINITY == val || Double.isNaN(val); }
    
    public static boolean isNAFloat(Float val) { return Float.NEGATIVE_INFINITY == val || Float.isNaN(val); }
    
    public static boolean isNAShort(Short val) { return Short.MIN_VALUE == val; }
    
    public static boolean isNAByte(Byte val) { return Byte.MIN_VALUE == val; }
    
    
    public static boolean isNAInstant(Instant val) { return Instant.MIN.equals(val); }
    
    public static boolean isNADuration(Duration val) { return Long.MIN_VALUE == val.getSeconds(); }
    
    public static boolean isMaxDuration(Duration val) { return 9223372036854775l == val.getSeconds() && 807000000 == val.getNano(); }
    
}
