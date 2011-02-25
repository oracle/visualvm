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

package com.sun.tools.visualvm.modules.tracer.jvmstat;

import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import static com.sun.tools.visualvm.modules.tracer.jvmstat.JvmstatCounterFormatter.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.jvmstat.monitor.Monitor;

/**
 *
 * @author Tomas Hurka
 */
class JvmstatCounterProbe extends TracerProbe {
    private static final Logger LOGGER = Logger.getLogger(JvmstatCounterProbe.class.getName());    
    private static final String Variability_MONOTONIC = "Monotonic";    // NOI18N

    private Monitor counter;
    private long lastVal;
    private long lastTime;
    private boolean ticks;
    
    JvmstatCounterProbe(String name, String desc, Monitor c) {
        super(createItemDescriptors(name, desc, c));
        counter = c;
        String u = Utils.getUnits(c).toString();
  
        ticks = u.equals(Units_TICKS) || (u.equals(Units_EVENTS) && Utils.getVariability(c).toString().equals(Variability_MONOTONIC));
    }
    
    private static final ProbeItemDescriptor[] createItemDescriptors(String name, String desc, Monitor c) {
        return new ProbeItemDescriptor[] {
            ProbeItemDescriptor.continuousLineItem(name, desc,
                    new JvmstatCounterFormatter(c), 1d, 0, ProbeItemDescriptor.MAX_VALUE_UNDEFINED),
        };
    }
    
    public long[] getItemValues(long time) {
        try {
            Object val = counter.getValue();
            if (val instanceof Number) {
                Number num = (Number) val;
                long value = num.longValue();
                
                return new long[] {convert(value)};
            }
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Failed to read UnixOperatingSystemMXBean attributes", t); // NOI18N
        }
        return new long[1]; // ??? should return null -- or -- catch exception in TracerController.fetchDataImpl() 
    }

    private long convert(long value) {
        if (ticks) {
            long now = System.currentTimeMillis();
            long diff = 0;
            if (lastTime != 0) {
                diff = 1000 * (value - lastVal) / (now - lastTime);
            }
            lastTime = now;
            lastVal = value;
            return diff;
        }
        return value;
    }
    
}
