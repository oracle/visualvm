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

import java.lang.annotation.Annotation;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import jdk.jfr.DataAmount;
import jdk.jfr.EventType;
import jdk.jfr.Experimental;
import jdk.jfr.Frequency;
import jdk.jfr.MemoryAddress;
import jdk.jfr.Percentage;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedThread;
import org.graalvm.visualvm.jfr.model.JFRDataDescriptor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.utils.DurationFormatter;
import org.graalvm.visualvm.jfr.utils.InstantFormatter;
import org.graalvm.visualvm.jfr.utils.ValuesChecker;

/**
 *
 * @author Jiri Sedlacek
 */
final class DisplayableSupport {
    
    private static final String VALUE_NA = "n/a";
    
    private static final DefaultProcessor DEFAULT_PROCESSOR = new DefaultProcessor();
    private static final DefaultFormat DEFAULT_FORMAT = new DefaultFormat();
    
    private static final FormatProcessor[] FORMAT_PROCESSORS = new FormatProcessor[] {
        new TimestampFormatProcessor(),
        new TimespanFormatProcessor(),
        new MemoryAddressFormatProcessor(),
        new FrequencyFormatProcessor(), // must be before DataAmoutFormatProcessor!
        new PercentFormatProcessor(),
        new DataAmountFormatProcessor(), // must be the last item!
    };
    
    private static final TypeProcessor[] TYPE_PROCESSORS = new TypeProcessor[] {
        new RecordedThreadProcessor(),
        new RecordedClassProcessor(),
        new RecordedClassLoaderProcessor()
    };
    
    private static final Set<String> PRIMITIVE_NUMERIC = new HashSet(Arrays.asList(new String[] {
        "byte", "short", "int", "long", "char", "float", "double" // char?
    }));
    
    
    static Iterator<ValueDescriptor> displayableValueDescriptors(final EventType type, final boolean includeExperimental) {
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
                if (ID_STACKTRACE.equals(descriptor.getName())) return false;
                return includeExperimental || !isExperimental(descriptor);
            }
        };
    }
    
    
    static JFRDataDescriptor getDataDescriptor(ValueDescriptor descriptor) {
        Format dataFormat = null;
        boolean isNumeric = false;
        
        for (FormatProcessor processor : FORMAT_PROCESSORS) {
            Annotation annotation = descriptor.getAnnotation(processor.getType());
            if (annotation != null) {
                dataFormat = processor.createFormat(descriptor, annotation);
                if (dataFormat != null) {
                    isNumeric = processor.isNumeric();
                    break;
                }
            }
        }
        
        if (dataFormat == null) for (TypeProcessor processor : TYPE_PROCESSORS) {
            String typeName = descriptor.getTypeName();
            if (processor.handlesType(typeName)) {
                dataFormat = processor.createFormat();
                if (dataFormat != null) {
                    isNumeric = processor.isNumeric();
                    break;
                }
            }
        }
        
        if (dataFormat == null) {
            dataFormat = DEFAULT_FORMAT;
            isNumeric = DEFAULT_PROCESSOR.isNumeric(descriptor);
        }
        
        String dataName = descriptor.getLabel();
        if (isExperimental(descriptor)) dataName = "[Experimental] " + dataName;
        
        return new JFRDataDescriptor(dataName, descriptor.getDescription(), dataFormat, null, isNumeric);
    }
    
    
    static Comparable getDisplayValue(JFRJDK11Event event, ValueDescriptor descriptor) {
//        List<AnnotationElement> annotations = descriptor.getAnnotationElements();
//        for (AnnotationElement annotation : annotations) System.err.println(">>> ANNOTATION " + annotation.getTypeName() + " - " + annotation.getValues());
//        System.err.println(">>> ContentType " + descriptor.getContentType());
//        System.err.println(">>> TypeName " + descriptor.getTypeName());
//        try { System.err.println(">>> VALUE " + event.getValue(descriptor.getName()).getClass().getName()); } catch (Throwable t) {}
//        System.err.println(">>> --------------------------");
        
        for (FormatProcessor processor : FORMAT_PROCESSORS) {
            Annotation annotation = descriptor.getAnnotation(processor.getType());
            if (annotation != null) try { return processor.createValue(event, descriptor, annotation); }
                                    catch (JFRPropertyNotAvailableException ex) { return null; }
        }
        
        for (TypeProcessor processor : TYPE_PROCESSORS) {
            String typeName = descriptor.getTypeName();
            if (processor.handlesType(typeName)) try { return processor.createValue(event, descriptor); }
                                                 catch (JFRPropertyNotAvailableException ex) { return null; }
        }
        
        try { return DEFAULT_PROCESSOR.createValue(event, descriptor); }
        catch (JFRPropertyNotAvailableException ex) { return null; }
    }
    
    
    private static boolean isExperimental(ValueDescriptor descriptor) {
        return descriptor.getAnnotation(Experimental.class) != null;
    }
    
    
    private DisplayableSupport() {}
    
    
    private static final NumberFormat DURATION_MS_FORMAT;
    private static final NumberFormat NUMBER_FORMAT;
    private static final NumberFormat PERCENT_FORMAT;
    
    static {
        DURATION_MS_FORMAT = NumberFormat.getNumberInstance();
        DURATION_MS_FORMAT.setMaximumFractionDigits(3);
        DURATION_MS_FORMAT.setMinimumFractionDigits(3);
        
        NUMBER_FORMAT = NumberFormat.getNumberInstance();
        
        PERCENT_FORMAT = NumberFormat.getPercentInstance();
        PERCENT_FORMAT.setMaximumFractionDigits(2);
        PERCENT_FORMAT.setMinimumFractionDigits(2);
    }
    
    
    private static abstract class DataFormat extends Format {
        
        @Override
        public Object parseObject(String source, ParsePosition pos) {
            throw new UnsupportedOperationException("Not supported");
        }
        
    }
    
    
    private static abstract class FormatProcessor<A extends Annotation> {
        
        abstract Class<A> getType();
        
        Format createFormat(ValueDescriptor descriptor, A annotation) {
            return null;
        }
        
        Comparable createValue(JFRJDK11Event event, ValueDescriptor descriptor, A annotation) throws JFRPropertyNotAvailableException {
            Object value = event.getValue(descriptor.getName());
            return value instanceof Comparable ? (Comparable)value :
                   value != null ? value.toString() : null;
        }
        
        boolean isNumeric() {
            return false;
        }
        
    }
    
    
    private static class TimestampFormatProcessor extends FormatProcessor<Timestamp> {
        
        @Override
        Class<Timestamp> getType() {
            return Timestamp.class;
        }

        @Override
        Format createFormat(ValueDescriptor descriptor, Timestamp annotation) {
            return new TimestampFormat();
        }
        
        @Override
        Comparable createValue(JFRJDK11Event event, ValueDescriptor descriptor, Timestamp annotation) throws JFRPropertyNotAvailableException {
            return event.getInstant(descriptor.getName());
        }
        
    }
    
    private static final class TimestampFormat extends DataFormat {

        @Override
        public StringBuffer format(Object o, StringBuffer b, FieldPosition p) {
            if (ValuesChecker.isNAValue(o)) return b.append(VALUE_NA);
            
            return o instanceof Instant ? InstantFormatter.format((Instant)o, b) :
                   o == null ? b : b.append("<unknown>");
        }
        
    }
    
    
    private static class TimespanFormatProcessor extends FormatProcessor<Timespan> {
        
        @Override
        Class<Timespan> getType() {
            return Timespan.class;
        }

        @Override
        Format createFormat(ValueDescriptor descriptor, Timespan annotation) {
            return new TimespanFormat();
        }
        
        @Override
        Comparable createValue(JFRJDK11Event event, ValueDescriptor descriptor, Timespan annotation) throws JFRPropertyNotAvailableException {
            return event.getDuration(descriptor.getName());
        }
        
        @Override
        boolean isNumeric() {
            return true;
        }
        
    }
    
    private static final class TimespanFormat extends DataFormat {

        @Override
        public StringBuffer format(Object o, StringBuffer b, FieldPosition p) {
            if (ValuesChecker.isNAValue(o)) return b.append(VALUE_NA);
            
            return o instanceof Duration ? DurationFormatter.format((Duration)o, b) :
                   o == null ? b : b.append("<unknown>");
        }
        
    }
    
    
    private static class MemoryAddressFormatProcessor extends FormatProcessor<MemoryAddress> {
        
        @Override
        Class<MemoryAddress> getType() {
            return MemoryAddress.class;
        }

        @Override
        Format createFormat(ValueDescriptor descriptor, MemoryAddress annotation) {
            return new MemoryAddressFormat();
        }
        
        @Override
        boolean isNumeric() {
            return true;
        }
        
    }
    
    private static final class MemoryAddressFormat extends DataFormat {

        @Override
        public StringBuffer format(Object o, StringBuffer b, FieldPosition p) {
            if (ValuesChecker.isNAValue(o)) return b.append(VALUE_NA);
            
            return o instanceof Number ? b.append("0x").append(Long.toHexString(((Number)o).longValue())) :
                   o == null ? b : b.append("<unknown>");
        }
        
    }
    
    
    private static class FrequencyFormatProcessor extends FormatProcessor<Frequency> {
        
        @Override
        Class<Frequency> getType() {
            return Frequency.class;
        }

        @Override
        Format createFormat(ValueDescriptor descriptor, Frequency annotation) {
            FormatProcessor dataAmountProcessor = FORMAT_PROCESSORS[FORMAT_PROCESSORS.length - 1];
            Annotation dataAmountAnnotation = descriptor.getAnnotation(dataAmountProcessor.getType());
            return new FrequencyFormat(dataAmountAnnotation == null ? null : dataAmountProcessor.createFormat(descriptor, dataAmountAnnotation));
        }
        
        @Override
        boolean isNumeric() {
            return true;
        }
        
    }
    
    private static final class FrequencyFormat extends DataFormat {
        
        private final Format originalFormat;
        
        FrequencyFormat(Format originalFormat) {
            this.originalFormat = originalFormat;
        }

        @Override
        public StringBuffer format(Object o, StringBuffer b, FieldPosition p) {
            if (ValuesChecker.isNAValue(o)) return b.append(VALUE_NA);
            
            return o instanceof Number ? formatFrequency((Number)o, b, p, originalFormat) :
                   o == null ? b : b.append("<unknown>");
        }
        
        private static StringBuffer formatFrequency(Number n, StringBuffer b, FieldPosition p, Format f) {
            if (f == null) return b.append(NUMBER_FORMAT.format(n.longValue())).append(" Hz");
            else return f.format(n, b, p).append("/s");
        }
        
    }
    
    private static class DataAmountFormatProcessor extends FormatProcessor<DataAmount> {
        
        @Override
        Class<DataAmount> getType() {
            return DataAmount.class;
        }

        @Override
        Format createFormat(ValueDescriptor descriptor, DataAmount annotation) {
            return new DataAmountFormat(annotation.value());
        }
        
        @Override
        boolean isNumeric() {
            return true;
        }
        
    }
    
    private static final class DataAmountFormat extends DataFormat {
        
        private final String dataSuffix;
        
        DataAmountFormat(String dataFormat) {
            this.dataSuffix = DataAmount.BYTES.equals(dataFormat) ? " B" :
                              DataAmount.BITS.equals(dataFormat) ? " b" :
                              "";
        }

        @Override
        public StringBuffer format(Object o, StringBuffer b, FieldPosition p) {
            if (ValuesChecker.isNAValue(o)) return b.append(VALUE_NA);
            
            return o instanceof Number ? b.append(NUMBER_FORMAT.format(((Number)o).longValue())).append(dataSuffix) :
                   o == null ? b : b.append("<unknown>");
        }
        
    }
    
    
    private static class PercentFormatProcessor extends FormatProcessor<Percentage> {
        
        @Override
        Class<Percentage> getType() {
            return Percentage.class;
        }

        @Override
        Format createFormat(ValueDescriptor descriptor, Percentage annotation) {
            return new PercentFormat();
        }
        
        @Override
        boolean isNumeric() {
            return true;
        }
        
    }
    
    private static final class PercentFormat extends DataFormat {

        @Override
        public StringBuffer format(Object o, StringBuffer b, FieldPosition p) {
            if (ValuesChecker.isNAValue(o)) return b.append(VALUE_NA);
            
            return o instanceof Number ? b.append(PERCENT_FORMAT.format(((Number)o).doubleValue())) :
                   o == null ? b : b.append("<unknown>");
        }
        
    }
    
    
    private static abstract class TypeProcessor {
        
        abstract boolean handlesType(String typeName);
        
        Format createFormat() {
            return null;
        }
        
        Comparable createValue(JFRJDK11Event event, ValueDescriptor descriptor) throws JFRPropertyNotAvailableException {
            Object value = event.getValue(descriptor.getName());
            return value instanceof Comparable ? (Comparable)value :
                   value == null ? "" : value.toString();
        }
        
        boolean isNumeric() {
            return false;
        }
        
    }
    
    
    private static class RecordedThreadProcessor extends TypeProcessor {
        
        @Override
        boolean handlesType(String typeName) {
            return Thread.class.getName().equals(typeName);
        }
        
        @Override
        String createValue(JFRJDK11Event event, ValueDescriptor descriptor) throws JFRPropertyNotAvailableException {
            Object value = event.getValue(descriptor.getName());
            RecordedThread thread = value instanceof RecordedThread ? (RecordedThread)value : null;
            if (thread == null) return "";
            String name = thread.getJavaName();
            return name != null ? name : thread.getOSName();
        }
        
    }
    
    private static class RecordedClassProcessor extends TypeProcessor {
        
        @Override
        boolean handlesType(String typeName) {
            return Class.class.getName().equals(typeName);
        }
        
        @Override
        String createValue(JFRJDK11Event event, ValueDescriptor descriptor) throws JFRPropertyNotAvailableException {
            Object value = event.getValue(descriptor.getName());
            return value instanceof RecordedClass ? ((RecordedClass)value).getName(): "";
        }
        
    }
    
    private static class RecordedClassLoaderProcessor extends TypeProcessor {
        
        @Override
        boolean handlesType(String typeName) {
            return "jdk.types.ClassLoader".equals(typeName) ||
                   "com.oracle.jfr.types.ClassLoader".equals(typeName);
        }
        
        @Override
        String createValue(JFRJDK11Event event, ValueDescriptor descriptor) throws JFRPropertyNotAvailableException {
            Object value = event.getValue(descriptor.getName());
            return value instanceof RecordedClassLoader ? ((RecordedClassLoader)value).getType().getName(): ""; // NOTE: should actually be "bootstrap"
        }
        
    }
    
    
    private static class DefaultProcessor {
        
        Comparable createValue(JFRJDK11Event event, ValueDescriptor descriptor) throws JFRPropertyNotAvailableException {
            Object value = event.getValue(descriptor.getName());
            
            if (value == null) return null;
            
            if (value instanceof Comparable) return (Comparable)value;
            
            return value.toString(); // Also includes RecordedObject which needs to be handled separately!
        }
        
        boolean isNumeric(ValueDescriptor descriptor) {
            return PRIMITIVE_NUMERIC.contains(descriptor.getTypeName());
        }
        
    }
    
    private static final class DefaultFormat extends DataFormat {

        @Override
        public StringBuffer format(Object o, StringBuffer b, FieldPosition p) {
            if (ValuesChecker.isNAValue(o)) return b.append(VALUE_NA);
            
            return o instanceof Number ? b.append(NUMBER_FORMAT.format(o)) :
                   o == null ? b : b.append(o.toString());
        }
        
    }
    
}
