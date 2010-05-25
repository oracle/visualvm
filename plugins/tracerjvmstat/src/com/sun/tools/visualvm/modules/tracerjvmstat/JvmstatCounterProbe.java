/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracerjvmstat;

import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.jvmstat.monitor.Monitor;
import sun.management.counter.Units;
import sun.management.counter.Variability;

/**
 *
 * @author Tomas Hurka
 */
class JvmstatCounterProbe extends TracerProbe {
    
    private static final Logger LOGGER = Logger.getLogger(JvmstatCounterProbe.class.getName());
    private Monitor counter;
    private long lastVal;
    private long lastTime;
    private boolean ticks;
    
    JvmstatCounterProbe(String name, String desc, Monitor c) {
        super(createItemDescriptors(name, desc, c));
        counter = c;
        Units u = c.getUnits();
        ticks = u.equals(Units.TICKS) || (u.equals(Units.EVENTS) && c.getVariability().equals(Variability.MONOTONIC));
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
