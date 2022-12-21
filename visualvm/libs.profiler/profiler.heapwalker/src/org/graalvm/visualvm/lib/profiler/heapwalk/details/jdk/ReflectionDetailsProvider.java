/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.lang.reflect.Modifier;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public class ReflectionDetailsProvider extends DetailsProvider.Basic {

    private static final String CLASS_MASK = "java.lang.Class";                     // NOI18N
    private static final String CONSTRUCTOR_MASK = "java.lang.reflect.Constructor"; // NOI18N
    private static final String METHOD_MASK = "java.lang.reflect.Method";           // NOI18N
    private static final String FIELD_MASK = "java.lang.reflect.Field";             // NOI18N
    private static final String PARAMETER_MASK = "java.lang.reflect.Parameter";     // NOI18N

    public ReflectionDetailsProvider() {
        super(CLASS_MASK,CONSTRUCTOR_MASK, METHOD_MASK, FIELD_MASK, PARAMETER_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case CLASS_MASK: {                                     // Class
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                if (name == null && CLASS_MASK.equals(instance.getJavaClass().getName())) {
                    Heap heap = instance.getJavaClass().getHeap();
                    JavaClass jclass = heap.getJavaClassByID(instance.getInstanceId());
                    if (jclass != null) name = BrowserUtils.getSimpleType(jclass.getName());
//                if (jclass != null) name = jclass.getName();
                }
                return name;
            }
            case CONSTRUCTOR_MASK: {   // Constructor
                Object value = instance.getValueOfField("clazz");                   // NOI18N
                if (value instanceof Instance) return getDetailsString("java.lang.Class", (Instance)value); // NOI18N
                break;
            }
            case METHOD_MASK:          // Method
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case FIELD_MASK: {         // Field
                int mod = DetailsUtils.getIntFieldValue(instance, "modifiers", 0);
                String type = DetailsUtils.getInstanceFieldString(instance, "type"); // NOI18N
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                String clazz = DetailsUtils.getInstanceFieldString(instance, "clazz"); // NOI18N

                return (((mod == 0) ? "" : (Modifier.toString(mod) + " "))
                        + type + " " + clazz + "." + name);
            }
            case PARAMETER_MASK:        // Parameter
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            default:
                break;
        }
        return null;
    }
    
}
