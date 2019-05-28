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
package org.graalvm.visualvm.jfr.model.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.graalvm.visualvm.jfr.model.JFRClass;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.TimestampUnit;
import org.openjdk.jmc.common.util.TypeHandling;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFREventImpl extends JFREvent {
    
    final IItem item;
    
    
    JFREventImpl(IItem event) {
        this.item = event;
    }
    
    
    @Override
    public Duration getDuration(String key) throws JFRPropertyNotAvailableException {
        Object duration;
        switch (key) {
            case "eventDuration": // NOI18N
                try {
                    duration = getValue("duration");
                } catch (JFRPropertyNotAvailableException e) {
                    // TODO: use more precise computation!
                    Instant startTime = getInstant("eventTime");
                    Instant endTime = getInstant("endTime");
                    long durationL = endTime.toEpochMilli() - startTime.toEpochMilli();
                    duration = Duration.ofMillis(durationL);
                }
                break;
            default:
                duration = getValue(key);
        }

        if (duration == null) return null;
        else if (duration instanceof Duration) return (Duration)duration;
        else if (duration instanceof IQuantity) return durationFromQuantity((IQuantity)duration);
        else throw new JFRPropertyNotAvailableException("No duration value available: " + key);
    }
    
    private static Duration durationFromQuantity(IQuantity quantity) {
        LinearUnit ms = null;
        for (Object u : quantity.getType().getCommonUnits()) {
            if (u instanceof LinearUnit) {
                LinearUnit lu = (LinearUnit)u;
                if ("ms".equals(lu.getIdentifier())) {
                    ms = lu;
                    break;
                }
            }
        }
        
        try {
            return Duration.ofMillis(quantity.longValueIn(ms));
        } catch (QuantityConversionException ex) {
//            System.err.println(">>> " + ex);
            return null;
        }
    }

    @Override
    public Instant getInstant(String key) throws JFRPropertyNotAvailableException {
        Object instant;
        switch (key) {
            case "eventTime": // NOI18N
            case "startTime": // NOI18N
                try {
                    instant = getValue("startTime");
                } catch (JFRPropertyNotAvailableException e) {
                    instant = getValue("(endTime)");
                }
                break;
            case "endTime": // NOI18N
                try {
                    instant = getValue("(endTime)");
                } catch (JFRPropertyNotAvailableException e) {
                    instant = getValue("endTime");
                }
                break;
            default:
                instant = getValue(key);
        }

        if (instant == null) return null;
        else if (instant instanceof Instant) return (Instant)instant;
        else if (instant instanceof IQuantity) return instantFromQuantity((IQuantity)instant);
        else throw new JFRPropertyNotAvailableException("No instant value available: " + key);
    }
    
    private static Instant instantFromQuantity(IQuantity quantity) {
        TimestampUnit ms = null;
        for (Object u : quantity.getType().getCommonUnits()) {
            if (u instanceof TimestampUnit) {
                TimestampUnit lu = (TimestampUnit)u;
                if ("epochms".equals(lu.getIdentifier())) {
                    ms = lu;
                    break;
                }
            }
        }
        
        try {
            return Instant.ofEpochMilli(quantity.longValueIn(ms));
        } catch (QuantityConversionException ex) {
//            System.err.println(">>> " + ex);
            return null;
        }
    }
    
    
    @Override
    public JFRClass getClass(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value == null) return null;
        else if (value instanceof IMCType) return new JFRClassImpl((IMCType)value);
        else throw new JFRPropertyNotAvailableException("No class value available: " + key);
    }

    @Override
    public JFRThread getThread(String key) throws JFRPropertyNotAvailableException {
        Object thread;
        switch (key) {
            case "eventThread": // NOI18N
                thread = getValue("eventThread"); // TODO
                break;
            case "sampledThread": // NOI18N
                try {
                    thread = getValue("sampledThread");
                } catch (JFRPropertyNotAvailableException e) {
                    thread = getValue("eventThread");
                }
                break;
            case "thread": // NOI18N
                try {
                    thread = getValue("thread");
                } catch (JFRPropertyNotAvailableException e) {
                    thread = getValue("javalangthread");
                }
                break;
            default:
                thread = getValue(key);
        }

        if (thread == null) return null;
        else if (thread instanceof IMCThread) return new JFRThreadImpl((IMCThread)thread);
        else throw new JFRPropertyNotAvailableException("No thread value available: " + key);
    }
    
    @Override
    public JFRStackTrace getStackTrace(String key) throws JFRPropertyNotAvailableException {
        Object stackTrace;
        switch (key) {
            case "eventStackTrace": // NOI18N
                stackTrace = getValue("stackTrace"); // TODO
                break;
            default:
                stackTrace = getValue(key);
        }

        if (stackTrace == null) return null;
        else if (stackTrace instanceof IMCStackTrace) return new JFRStackTraceImpl((IMCStackTrace)stackTrace);
        else throw new JFRPropertyNotAvailableException("No stacktrace value available: " + key);
    }
    
    
    @Override
    public Object getValue​(String key) throws JFRPropertyNotAvailableException {
        return getValue(item, key);
    }
    
    
    static Object getValue(IItem item, String key) throws JFRPropertyNotAvailableException {
        key = key.replace('.', ':'); // NOI18N
        
        for (IAccessorKey accessor : item.getType().getAccessorKeys().keySet()) {
            if (key.equals(accessor.getIdentifier())) 
                return item.getType().getAccessor(accessor).getMember(item);
        }
        
        throw new JFRPropertyNotAvailableException("No value available: " + key);
    }
    
    
    @Override
    public List<Comparable> getDisplayableValues(boolean includeExperimental) {
        IType type = item.getType();        
        List<Comparable> values = new ArrayList();
        Iterator<IAccessorKey> keys = DisplayableSupport.displayableAccessorKeys(type, includeExperimental);
        while (keys.hasNext()) {
            IAccessorKey key = keys.next();
            Object value = type.getAccessor(key).getMember(item);
            values.add(value == null ? "" : key.getContentType().getDefaultFormatter().format(value)); // NOI18N
        }
        return values;
    }
    
    
    @Override
    public int hashCode() {
        return item.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFREventImpl ? item.equals(((JFREventImpl)o).item) : false;
    }
    
    
    @Override
    public String toString() {
        return item.toString() + " [" + item.getType().getAccessorKeys().keySet() + "]";
    }
    
}
