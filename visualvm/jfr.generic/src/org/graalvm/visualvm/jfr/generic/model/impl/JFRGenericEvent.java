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
package org.graalvm.visualvm.jfr.generic.model.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.graalvm.visualvm.jfr.model.JFRClass;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRGenericEvent extends JFREvent {
    
    final IItem item;
    
    
    JFRGenericEvent(IItem event) {
        this.item = event;
    }
    
    
    @Override
    public Duration getDuration(String key) throws JFRPropertyNotAvailableException {
        Object duration;
        switch (key) {
            case "eventDuration": // NOI18N
                try {
                    duration = getValue(item, JfrAttributes.DURATION);
                } catch (JFRPropertyNotAvailableException e) {
                    IQuantity startTime = getTime("eventTime"); // NOI18N
                    if (startTime == null) throw new JFRPropertyNotAvailableException("No start time to compute duration: " + key);
                    IQuantity endTime = getTime("endTime"); // NOI18N
                    if (endTime == null) throw new JFRPropertyNotAvailableException("No end time to compute duration: " + key);
                    return durationFromQuantity(endTime.subtract(startTime));
                }
                break;
            default:
                duration = getValue(key);
        }

        if (duration instanceof IQuantity) return durationFromQuantity((IQuantity)duration);
        else if (duration == null) return null;
        else throw new JFRPropertyNotAvailableException("No duration value available: " + key);
    }
    
    private static Duration durationFromQuantity(IQuantity quantity) {
        // quantity can be in ticks or time units - ms, us, ns etc.
        try {
            long nanos = quantity.longValueIn(UnitLookup.NANOSECOND);
            return Duration.ofNanos(nanos);
        } catch (QuantityConversionException ex1) {
//            try {
//                long micros = quantity.longValueIn(UnitLookup.MICROSECOND);
//                System.err.println(">>> micros");
//                return Duration.ofNanos(micros * 1000);
//            } catch (QuantityConversionException ex2) {
                try {
                    long millis = quantity.longValueIn(UnitLookup.MILLISECOND);
//                    System.err.println(">>> FALLBACK DURATION TO MILLIS");
                    return Duration.ofMillis(millis);
                } catch (QuantityConversionException ex3) {
                    long seconds = quantity.clampedLongValueIn(UnitLookup.SECOND);
//                    System.err.println(">>> FALLBACK DURATION TO SECONDS");
                    return Duration.ofSeconds(seconds);
                }
//            }
        }
    }
    
    
    private IQuantity getTime(String key) throws JFRPropertyNotAvailableException {
        Object time;
        switch (key) {
            case "eventTime": // NOI18N
            case "startTime": // NOI18N
                try {
                    time = getValue(item, JfrAttributes.START_TIME);
                } catch (JFRPropertyNotAvailableException e) {
                    time = getValue(item, JfrAttributes.END_TIME);
                }
                break;
            case "endTime": // NOI18N
                try {
                    time = getValue(item, JfrAttributes.END_TIME);
                } catch (JFRPropertyNotAvailableException e) {
                    time = getValue("endTime"); // NOI18N
                }
                break;
            default:
                time = getValue(key);
        }

        if (time instanceof IQuantity) return (IQuantity)time;
        else if (time == null) return null;
        else throw new JFRPropertyNotAvailableException("No instant value available: " + key);
    }
    
    @Override
    public Instant getInstant(String key) throws JFRPropertyNotAvailableException {
        IQuantity quantity = getTime(key);
        return quantity == null ? null : instantFromQuantity(quantity);
    }
    
    private static Instant instantFromQuantity(IQuantity quantity) {
//        TimestampUnit ms = null;
//        for (Object u : quantity.getType().getCommonUnits()) {
//            if (u instanceof TimestampUnit) {
//                TimestampUnit lu = (TimestampUnit)u;
//                if ("epochms".equals(lu.getIdentifier())) {
//                    ms = lu;
//                    break;
//                }
//            }
//        }
//        
//        try {
//            return Instant.ofEpochMilli(quantity.longValueIn(ms));
//        } catch (QuantityConversionException ex) {
////            System.err.println(">>> " + ex);
//            return null;
//        }
        
        
        // quantity expected to be always in epochns or epochms
        try {
            long nanos = quantity.longValueIn(UnitLookup.EPOCH_NS);
            return Instant.EPOCH.plusNanos(nanos);
        } catch (QuantityConversionException ex1) {
            try {
                long millis = quantity.longValueIn(UnitLookup.EPOCH_MS);
//                System.err.println(">>> FALLBACK INSTANT TO MILLIS");
                return Instant.ofEpochMilli(millis);
            } catch (QuantityConversionException ex3) {
                long seconds = quantity.clampedLongValueIn(UnitLookup.EPOCH_S);
//                System.err.println(">>> FALLBACK INSTANT TO SECONDS");
                return Instant.ofEpochSecond(seconds);
            }
        }
        
//        IQuantity seconds = quantity.in(UnitLookup.EPOCH_S);
//        IQuantity nanos = quantity.subtract(seconds).in(UnitLookup.NANOSECOND);
//
//        return Instant.ofEpochSecond(seconds.longValue(), nanos.longValue());
    }
    
    
    @Override
    public JFRClass getClass(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof IMCType) return new JFRGenericClass((IMCType)value);
        else if (value == null) return null;
        else throw new JFRPropertyNotAvailableException("No class value available: " + key);
    }

    @Override
    public JFRThread getThread(String key) throws JFRPropertyNotAvailableException {
        Object thread;
        switch (key) {
            case "eventThread": // NOI18N
                thread = getValue(item, JfrAttributes.EVENT_THREAD);
                break;
            case "sampledThread": // NOI18N
                try {
                    thread = getValue("sampledThread"); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {
                    thread = getValue(item, JfrAttributes.EVENT_THREAD);
                }
                break;
            case "thread": // NOI18N
                try {
                    thread = getValue("thread"); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {
                    thread = getValue("javalangthread"); // NOI18N
                }
                break;
            default:
                thread = getValue(key);
        }

        if (thread instanceof IMCThread) return new JFRGenericThread((IMCThread)thread);
        else if (thread == null) return null;
        else throw new JFRPropertyNotAvailableException("No thread value available: " + key);
    }
    
    @Override
    public JFRStackTrace getStackTrace(String key) throws JFRPropertyNotAvailableException {
        Object stackTrace;
        switch (key) {
            case "eventStackTrace": // NOI18N
                stackTrace = getValue(item, JfrAttributes.EVENT_STACKTRACE);
                break;
            default:
                stackTrace = getValue(key);
        }

        if (stackTrace instanceof IMCStackTrace) return new JFRGenericStackTrace((IMCStackTrace)stackTrace);
        else if (stackTrace == null) return null;
        else throw new JFRPropertyNotAvailableException("No stacktrace value available: " + key);
    }
    
    
    @Override
    public Object getValue(String key) throws JFRPropertyNotAvailableException {
        return getValue(item, key);
    }
    
    
    protected static Object getValue(IItem item, IAttribute attribute) throws JFRPropertyNotAvailableException {
        IType<?> type = item.getType();
        IMemberAccessor accessor = type.getAccessor(attribute.getKey());
        if (accessor != null) return accessor.getMember(item);
        
        throw new JFRPropertyNotAvailableException("No value available: " + attribute.getIdentifier());
    }
    
    static Object getValue(IItem item, String key) throws JFRPropertyNotAvailableException {
        key = key.replace('.', ':'); // NOI18N
        
        IType<?> type = item.getType();
        for (IAccessorKey accessor : type.getAccessorKeys().keySet()) {
            if (key.equals(accessor.getIdentifier())) 
                return type.getAccessor(accessor).getMember(item);
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
        return o instanceof JFRGenericEvent ? item.equals(((JFRGenericEvent)o).item) : false;
    }
    
    
    @Override
    public String toString() {
        return item.toString() + " [" + item.getType().getAccessorKeys().keySet() + "]"; // NOI18N
    }
    
}
