/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class JFREvent {
    
    private final long id;
    
    
    protected JFREvent(long id) {
        this.id = id;
    }
    
    
    public final long getID() {
        return id;
    }
    
    
    public boolean getBoolean(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Boolean) return (Boolean)value;
        else throw new JFRPropertyNotAvailableException("No boolean value available: " + key);
    }

    public byte getByte(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Number) return ((Number)value).byteValue();
        else throw new JFRPropertyNotAvailableException("No byte value available: " + key);
    }

    public char getChar(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Character) return (Character)value;
        else throw new JFRPropertyNotAvailableException("No char value available: " + key);
    }

    public double getDouble(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Number) return ((Number)value).doubleValue();
        else throw new JFRPropertyNotAvailableException("No double value available: " + key);
    }

    public float getFloat(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Number) return ((Number)value).floatValue();
        else throw new JFRPropertyNotAvailableException("No float value available: " + key);
    }

    public int getInt(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Number) return ((Number)value).intValue();
        else throw new JFRPropertyNotAvailableException("No int value available: " + key);
    }

    public long getLong(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Number) return ((Number)value).longValue();
        else throw new JFRPropertyNotAvailableException("No long value available: " + key);
    }

    public short getShort(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof Number) return ((Number)value).shortValue();
        else throw new JFRPropertyNotAvailableException("No short value available: " + key);
    }
    
    public String getString(String key) throws JFRPropertyNotAvailableException {
        Object value = getValue(key);
        if (value instanceof String) return (String)value;
        else if (value == null) return null;
        else throw new JFRPropertyNotAvailableException("No string value available: " + key);
    }
    
    
    public abstract Instant getInstant(String key) throws JFRPropertyNotAvailableException;
    
    public abstract Duration getDuration(String key) throws JFRPropertyNotAvailableException;
    
    
    public abstract JFRClass getClass(String key) throws JFRPropertyNotAvailableException;
    
    public abstract JFRThread getThread(String key) throws JFRPropertyNotAvailableException;
    
    public abstract JFRStackTrace getStackTrace(String key) throws JFRPropertyNotAvailableException;
    
    
    public abstract Object getValue(String key) throws JFRPropertyNotAvailableException;
    
    
    public abstract List<Comparable> getDisplayableValues(boolean includeExperimental);
    
}
