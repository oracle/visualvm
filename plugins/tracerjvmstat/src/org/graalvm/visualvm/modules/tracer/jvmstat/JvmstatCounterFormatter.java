/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.tracer.jvmstat;

import com.sun.management.UnixOperatingSystemMXBean;
import org.graalvm.visualvm.modules.tracer.ItemValueFormatter;
import java.util.logging.Logger;
import sun.jvmstat.monitor.Monitor;

/**
 *
 * @author Tomas Hurka
 */
class JvmstatCounterFormatter extends ItemValueFormatter {
    static final String Units_STRING = "String";  // NOI18N
    static final String Units_NONE = "None";  // NOI18N
    static final String Units_INVALID = "Invalid";  // NOI18N
    static final String Units_BYTES = "Bytes";  // NOI18N
    static final String Units_EVENTS = "Events";    // NOI18N
    static final String Units_TICKS = "Ticks";  // NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(JvmstatCounterFormatter.class.getName());
    private Monitor counter;
    private ItemValueFormatter del;
    private String unitsName;

    JvmstatCounterFormatter(Monitor c) {
        counter = c;
        unitsName = Utils.getUnits(counter).toString();
        if (unitsName.equals(Units_BYTES)) {
            del = ItemValueFormatter.DEFAULT_BYTES;
        } else if (unitsName.equals(Units_EVENTS)) {
            del = ItemValueFormatter.DEFAULT_DECIMAL;
        } else if (unitsName.equals(Units_TICKS)) {
            
        }
    }

    public String formatValue(long l, int i) {
        if (del != null) {
            return del.formatValue(l,i);
        }
        return String.valueOf(l);
    }

    public String getUnits(int i) {
        if (del != null) {
            return del.getUnits(i);
        }
        return unitsName;
    }
    
}
