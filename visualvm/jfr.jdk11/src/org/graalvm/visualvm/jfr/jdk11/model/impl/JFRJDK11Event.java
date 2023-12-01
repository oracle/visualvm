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
package org.graalvm.visualvm.jfr.jdk11.model.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.graalvm.visualvm.jfr.model.JFRClass;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.model.JFRThread;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRJDK11Event extends JFREvent {
    
    protected final RecordedEvent event;

    public JFRJDK11Event(RecordedEvent event, long id) {
        super(id);
        this.event = event;
    }

    @Override
    public JFRClass getClass(String key) throws JFRPropertyNotAvailableException {
        Object rclass = getValue(key);

        if (rclass == null) return null;
        else if (rclass instanceof RecordedClass) return new JFRJDK11Class((RecordedClass)rclass);
        else throw new JFRPropertyNotAvailableException("No class value available: " + key);
    }

    @Override
    public JFRThread getThread(String key) throws JFRPropertyNotAvailableException {
        if ("eventThread".equals(key)) { // NOI18N
            RecordedThread thread = event.getThread();
            return thread == null ? null : new JFRJDK11Thread(thread);
        }

        Object thread = getValue(key);
        if (thread instanceof RecordedThread) return new JFRJDK11Thread((RecordedThread)thread);
        else if (thread == null) return null;
        else throw new JFRPropertyNotAvailableException("No thread value available: " + key);
    }

    @Override
    public JFRStackTrace getStackTrace(String key) throws JFRPropertyNotAvailableException {
        if ("eventStackTrace".equals(key)) { // NOI18N
            RecordedStackTrace stackTrace = event.getStackTrace();
            return stackTrace == null ? null : new JFRJDK11StackTrace(stackTrace);
        }

        Object stackTrace = getValue(key);
        if (stackTrace instanceof RecordedStackTrace) return new JFRJDK11StackTrace((RecordedStackTrace)stackTrace);
        else if (stackTrace == null) return null;
        else throw new JFRPropertyNotAvailableException("No stacktrace value available: " + key);
    }

    @Override
    public Object getValue(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getValue(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }


    @Override
    public List<Comparable> getDisplayableValues(boolean includeExperimental) {
        List<Comparable> values = new ArrayList();
        Iterator<ValueDescriptor> descriptors = DisplayableSupport.displayableValueDescriptors(event.getEventType(), includeExperimental);
        while (descriptors.hasNext()) values.add(DisplayableSupport.getDisplayValue(this, descriptors.next()));
        return values;
    }

    
    @Override
    public boolean getBoolean(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getBoolean(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public byte getByte(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getByte(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public char getChar(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getChar(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public double getDouble(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getDouble(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public float getFloat(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getFloat(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public int getInt(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getInt(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public long getLong(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getLong(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }

    @Override
    public short getShort(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getShort(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    @Override
    public String getString(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getString(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    
    @Override
    public Instant getInstant(String key) throws JFRPropertyNotAvailableException {
        switch (key) {
            case "eventTime": // NOI18N
            case "startTime": // NOI18N
                return event.getStartTime();
                
            case "endTime": // NOI18N
                return event.getEndTime();
        }
        
        try {
            return event.getInstant(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    @Override
    public Duration getDuration(String key) throws JFRPropertyNotAvailableException {
        if ("eventDuration".equals(key)) { // NOI18N
            return event.getDuration();
        } else try {
            return event.getDuration(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    
    @Override
    public int hashCode() {
        return event.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JFRJDK11Event ? event.equals(((JFRJDK11Event)o).event) : false;
    }
    
}
