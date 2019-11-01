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

import java.util.Comparator;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TimeRecord {
    
    public static Comparator<TimeRecord> COMPARATOR = new Comparator<TimeRecord>() {
        @Override public int compare(TimeRecord r1, TimeRecord r2) {
            return Long.compare(r1.time, r2.time);
        }
    };
    
    
    public final long time;
    
    
    public TimeRecord(JFREvent event, JFRModel model) throws JFRPropertyNotAvailableException {
        time = getTime(event, model);
    }
    
    
    public static long getTime(JFREvent event, JFRModel model) throws JFRPropertyNotAvailableException {
        return ValuesConverter.instantToRelativeNanos(event.getInstant("eventTime"), model); // NOI18N
    }
    
    
    @Override
    public int hashCode() {
        return Long.hashCode(time);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof TimeRecord) return time == ((TimeRecord)o).time;
        return false;
    }
    
}
