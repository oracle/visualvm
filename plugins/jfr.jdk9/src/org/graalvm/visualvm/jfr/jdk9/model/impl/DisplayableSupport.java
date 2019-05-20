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
package org.graalvm.visualvm.jfr.jdk9.model.impl;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import jdk.jfr.EventType;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedThread;

/**
 *
 * @author Jiri Sedlacek
 */
final class DisplayableSupport {
    
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();
    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    
    
    static Iterator<ValueDescriptor> displayableValueDescriptors(final EventType type) {
        return new Iterator<ValueDescriptor>() {
            private final String ID_STACKTRACE;
            
            private final Iterator<ValueDescriptor> master;
            private ValueDescriptor next;
            
            {
                ID_STACKTRACE = "stackTrace"; // NOI18N
                
                master = type.getFields().iterator();
                next = computeNext();
            }
            
            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public ValueDescriptor next() {
                ValueDescriptor ret = next;
                next = computeNext();
                return ret;
            }
            
            private ValueDescriptor computeNext() {
                while (master.hasNext()) {
                    ValueDescriptor _next = master.next();
                    if (isDisplayable(_next)) return _next;
                }
                return null;
            }
            
            private boolean isDisplayable(ValueDescriptor descriptor) {
                return !ID_STACKTRACE.equals(descriptor.getName());
            }
        };
    }
    
    
    static String getDisplayString(ValueDescriptor descriptor, Object object) {
        if (object == null) return ""; // NOI18N
        
        String contentType = descriptor.getContentType();
        if (contentType != null) switch (contentType) {
            case "jdk.jfr.Percentage":
//                System.err.println(">>> Timestamp " + object.getClass().getName());
                if (object instanceof Number) return PERCENT_FORMAT.format(object);
            case "jdk.jfr.Timestamp":
                System.err.println(">>> Timestamp " + descriptor.getAnnotation(Timestamp.class).value());
                if (object instanceof Number) return TIME_FORMAT.format(new Date(((Number)object).longValue()));
            case "jdk.jfr.Timespan":
                System.err.println(">>> Timespan " + descriptor.getAnnotation(Timespan.class).value());
                if (object instanceof Number) return NUMBER_FORMAT.format(((Number)object).longValue() / 10000f) + " ms"; // ???
            case "jdk.jfr.DataAmount":
                System.err.println(">>> DataAmount " + object.getClass().getName());
                return object.toString();
        }
        
        String typeName = descriptor.getTypeName();
        switch (typeName) {
            case "boolean":
//                System.err.println(">>> boolean " + object.getClass().getName());
                if (object instanceof Boolean) return object.toString();
            case "int":
//                System.err.println(">>> int " + object.getClass().getName());
                if (object instanceof Number) return NUMBER_FORMAT.format(object);
            case "long":
//                System.err.println(">>> long " + object.getClass().getName());
                if (object instanceof Number) return NUMBER_FORMAT.format(object);
            case "double":
//                System.err.println(">>> long " + object.getClass().getName());
                if (object instanceof Number) return NUMBER_FORMAT.format(object);
            case "java.lang.Thread":
//                System.err.println(">>> Thread " + object.getClass().getName());
                if (object instanceof RecordedThread) return ((RecordedThread)object).getJavaName();
            case "java.lang.Class":
//                System.err.println(">>> Class " + object.getClass().getName());
                if (object instanceof RecordedClass) return ((RecordedClass)object).getName();
            case "java.lang.String":
//                System.err.println(">>> String " + object.getClass().getName());
                return object.toString();
            case "jdk.types.ClassLoader":
//                System.err.println(">>> ClassLoader " + object.getClass().getName());
                if (object instanceof RecordedClassLoader) return ((RecordedClassLoader)object).getType().getName();
        }
        
        System.err.println(">>> contentType " + contentType + " -- typeName " + typeName);
        return object.toString();
    }
    
    
    private DisplayableSupport() {}
    
}
