/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapUtils_UnknownClass=unknown class",
    "HeapUtils_UnknownInstance=unknown instance",
    "HeapUtils_Class=class",
    "HeapUtils_OomeCaption=Out Of Memory",
    "HeapUtils_OomeMsg=<html><b>Not enough memory to finish the operation.</b><br/><br/>To avoid this error please increase the -Xmx value<br>in the etc/visualvm.conf file in VisualVM directory.</html>"
}) 
public final class HeapUtils {
    
   // --- Heap utils ----------------------------------------------------------
    
    public static Collection<JavaClass> getSubclasses(Heap heap, String baseClass) {
        Set<JavaClass> subclasses = new HashSet();

        String escapedClassName = "\\Q" + baseClass + "\\E"; // NOI18N
        Collection<JavaClass> jClasses = heap.getJavaClassesByRegExp(escapedClassName);

        for (JavaClass jClass : jClasses) {
            subclasses.add(jClass);
            subclasses.addAll(jClass.getSubClasses());
        }
        
        return subclasses;
    }
    
    public static Iterator<Instance> instancesIterator(Collection<JavaClass> classes) {
        return new InstancesIterator(classes);
    }
    
//    public static Map<String, Object> getValuesOfFields(Instance instance, String... fields) {
//        Map<String, Object> values = new HashMap();
//        for (String field : fields) values.put(field, null);
//        
//        List<FieldValue> fieldValues = instance.getFieldValues();
//        for (FieldValue fieldValue : fieldValues) {
//            String fieldName = fieldValue.getField().getName();
//            if (values.containsKey(fieldName)) {
//                Object value = fieldValue instanceof ObjectFieldValue ? ((ObjectFieldValue)fieldValue).getInstance() : fieldValue.getValue();
//                values.put(fieldName, value);
//            }
////            Object value = fieldValue instanceof ObjectFieldValue ? ((ObjectFieldValue)fieldValue).getInstance() : fieldValue.getValue();
////            values.replace(fieldValue.getField().getName(), value);
//        }
//    
//        return values;
//    }
    
    
    public static Object[] getValuesOfFields(Instance instance, String... fields) {
        Object[] values = new Object[fields.length];
        
        Map<String, Integer> fieldsM = new HashMap();
        for (int i = 0; i < fields.length; i++) fieldsM.put(fields[i], i);
        
        List<FieldValue> fieldValues = instance.getFieldValues();
        for (int i = fieldValues.size() - 1; i >= 0; i--) {
            FieldValue fieldValue = fieldValues.get(i);
            String fieldName = fieldValue.getField().getName();
            Integer valueIdx = fieldsM.remove(fieldName);
            if (valueIdx != null) {
                Object value = fieldValue instanceof ObjectFieldValue ? ((ObjectFieldValue)fieldValue).getInstance() : fieldValue.getValue();
                values[valueIdx] = value;
                if (fieldsM.isEmpty()) break;
            }
//            Object value = fieldValue instanceof ObjectFieldValue ? ((ObjectFieldValue)fieldValue).getInstance() : fieldValue.getValue();
//            values.replace(fieldValue.getField().getName(), value);
        }
        
        return values;
    }
    
    
    // --- Heap <-> HTML utils -------------------------------------------------
    
    private static final String CLASS_URL_PREFIX = "file://class/"; // NOI18N
    private static final String INSTANCE_URL_PREFIX = "file://instance/"; // NOI18N
    
    
    public static String classToHtml(JavaClass javaClass) {
        return classToHtml(javaClass, null);
    }
    
    private static String classToHtml(JavaClass javaClass, String prefix) {
        if (javaClass == null) return Bundle.HeapUtils_UnknownClass();

        long id = javaClass.getJavaClassId();
        return "<a href='" + CLASS_URL_PREFIX + id + "' name='" + id + "'>" + (prefix == null ? "" : prefix) + javaClass.getName() + "</a>"; // NOI18N
    }
    
    public static boolean isClass(String html) {
        return html.startsWith(CLASS_URL_PREFIX);
    }
    
    public static JavaClass classFromHtml(String html, Heap heap) {
//        if (!isClass(html)) return null;
        String classIdS = html.substring(CLASS_URL_PREFIX.length());
        try {
            long classId = Long.parseLong(classIdS);
            return heap.getJavaClassByID(classId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public static String instanceToHtml(Instance instance, boolean logicalValue, JavaClass javaClassClass) {
        if (instance == null) return Bundle.HeapUtils_UnknownInstance();
        
        JavaClass jcls = instance.getJavaClass();
        if (jcls == null) return Bundle.HeapUtils_UnknownInstance() + " #" + instance.getInstanceId(); // NOI18N
        
        if (jcls.equals(javaClassClass)) {
            Heap heap = jcls.getHeap();
            JavaClass javaClass = heap.getJavaClassByID(instance.getInstanceId());
            if (javaClass != null) return classToHtml(javaClass, Bundle.HeapUtils_Class() + " "); // NOI18N
        }
        
        long id = instance.getInstanceId();
        String instanceHtml = "<a href='" + INSTANCE_URL_PREFIX + id + "' name='" + id + "'>" + jcls.getName() + '#' + instance.getInstanceNumber() + "</a>"; // NOI18N
        if (!logicalValue) return instanceHtml;
        
        String details = DetailsUtils.getInstanceString(instance);
        if (details == null) return instanceHtml;
        
        details = "<span style=\"color: #666666\"> : " + htmlize(details) + "</span>"; // NOI18N
        return instanceHtml + details;
    }
    
    public static boolean isInstance(String html) {
        return html.startsWith(INSTANCE_URL_PREFIX);
    }
    
    public static Instance instanceFromHtml(String html, Heap heap) {
//        if (!isInstance(html)) return null;
        String classIdS = html.substring(INSTANCE_URL_PREFIX.length());
        try {
            long classId = Long.parseLong(classIdS);
            return heap.getInstanceByID(classId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    
    public static String htmlize(String text) {
        if (text == null) return null;
        return text.replace(">", "&gt;").replace("<", "&lt;"); // NOI18N
    }
    
    
    // --- OOME handling -------------------------------------------------------
    
    private static boolean OOME_NOTIFIED = false;
    
    public static void handleOOME(final boolean skipSuccessive, OutOfMemoryError e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!OOME_NOTIFIED || !skipSuccessive) {
                    OOME_NOTIFIED = true;
                    ProfilerDialogs.displayError(Bundle.HeapUtils_OomeMsg(), Bundle.HeapUtils_OomeCaption(), null);
                    // NOTE: might update the Xmx automatically and/or lower MoreObjectsNode.MAX_BUFFER_SIZE
                    //                                              (won't work for OOMEs from merged references etc.)
                }
            }
        });
    }
    
    
    // --- Private stuff -------------------------------------------------------
    
    private HeapUtils() {}
    
    
    private static class InstancesIterator implements Iterator<Instance> {
        
        private final Iterator<JavaClass> classIt;
        private Iterator<Instance> instanceIt;

        public InstancesIterator(Collection<JavaClass> cls) {
            classIt = cls.iterator();
            instanceIt = Collections.EMPTY_LIST.iterator();
        }

        @Override
        public boolean hasNext() {
            if (instanceIt.hasNext()) return true;
            if (!classIt.hasNext()) return false;
            
            instanceIt = classIt.next().getInstancesIterator();
            return hasNext();
        }

        @Override
        public Instance next() {
            return instanceIt.next();
        }
        
    }
    
}
