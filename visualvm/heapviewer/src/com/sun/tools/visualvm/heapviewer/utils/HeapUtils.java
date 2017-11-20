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

package com.sun.tools.visualvm.heapviewer.utils;

import java.util.Collection;
import java.util.HashSet;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public final class HeapUtils {
    
   // --- Heap utils ----------------------------------------------------------
    
    public static Collection<JavaClass> getSubclasses(Heap heap, String baseClass) {
        HashSet subclasses = new HashSet();

        String escapedClassName = "\\Q" + baseClass + "\\E";
        Collection<JavaClass> jClasses = heap.getJavaClassesByRegExp(escapedClassName);

        for (JavaClass jClass : jClasses) {
            subclasses.add(jClass);
            subclasses.addAll(jClass.getSubClasses());
        }
        
        return subclasses;
    }
    
    
    // --- Heap <-> HTML utils -------------------------------------------------
    
    private static final String CLASS_URL_PREFIX = "file://class/"; // NOI18N
    private static final String INSTANCE_URL_PREFIX = "file://instance/"; // NOI18N
    
    
    public static String classToHtml(JavaClass javaClass) {
        return classToHtml(javaClass, null);
    }
    
    private static String classToHtml(JavaClass javaClass, String prefix) {
        if (javaClass == null) return "unknown class";

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
    
    public static String instanceToHtml(Instance instance, boolean logicalValue, Heap heap, JavaClass javaClassClass) {
        if (instance == null) return "unknown instance";
        
        JavaClass jcls = instance.getJavaClass();
        if (jcls == null) return "unknown instance #" + instance.getInstanceId();
        
        if (jcls.equals(javaClassClass)) {
            JavaClass javaClass = heap.getJavaClassByID(instance.getInstanceId());
            if (javaClass != null) return classToHtml(javaClass, "class ");
        }
        
        long id = instance.getInstanceId();
        String instanceHtml = "<a href='" + INSTANCE_URL_PREFIX + id + "' name='" + id + "'>" + jcls.getName() + '#' + instance.getInstanceNumber() + "</a>"; // NOI18N
        if (!logicalValue) return instanceHtml;
        
        String details = DetailsUtils.getInstanceString(instance, heap);
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
        return text.replace(">", "&gt;").replace("<", "&lt;"); // NOI18N
    }
    
    
    private HeapUtils() {}
    
}
