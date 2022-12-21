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
package org.graalvm.visualvm.heapviewer.truffle.dynamicobject;

import java.util.Map;
import java.util.WeakHashMap;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class DynamicObjectDetailsProvider extends DetailsProvider.Basic {

    private static final String DYNAMIC_OBJECT_MASK = "com.oracle.truffle.api.object.DynamicObject+"; // NOI18N
    private static final String SHAPE_MASK = "com.oracle.truffle.api.object.Shape+";    // NOI18N
    private static final String JS_UNDEFIED_CLASS_FQN = "com.oracle.truffle.js.runtime.objects.Undefined";     // NOI18N
    private static final String JS_NULL_CLASS_FQN = "com.oracle.truffle.js.runtime.objects.Null";     // NOI18N
    private Map<Heap,Long> jsUndefidedId;
    private Map<Heap,Long> jsNullId;

    public DynamicObjectDetailsProvider() {
        super(DYNAMIC_OBJECT_MASK,SHAPE_MASK);
        jsUndefidedId = new WeakHashMap();
        jsNullId = new WeakHashMap();
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case DYNAMIC_OBJECT_MASK: {
                StringBuilder buf = new StringBuilder();
                Heap heap = instance.getJavaClass().getHeap();
                Long undefinedId = getJSUdefined(heap);
                Long nullId = getJSNull(heap);
                if (instance.getInstanceId() == undefinedId.longValue()) {
                    buf.append("undefined");       // NOI18N
                } else if (instance.getInstanceId() == nullId.longValue()) {
                    buf.append("null");       // NOI18N
                } else {
                    Instance shape = (Instance) instance.getValueOfField("shape");  // NOI18N
                    Instance objectType = (Instance) shape.getValueOfField("objectType");   // NOI18N
                    buf.append('(').append(getSimpleClassName(objectType)).append(')'); // NOI18N
                    buf.append(' ').append(getShortInstanceId(shape)); // NOI18N
                }
                return buf.toString();
            }
            case SHAPE_MASK: {
                Instance objectType = (Instance) instance.getValueOfField("objectType");   // NOI18N
                String name = DetailsSupport.getDetailsString(objectType);

                if (name == null) {
                    name = getSimpleClassName(objectType);
                }
                return name;
            }
            default:
                break;
        }
        return null;
    }

    private static String getShortInstanceId(Instance instance) {
        if (instance == null) return "null"; // NOI18N
        return getSimpleClassName(instance) + "#" + instance.getInstanceNumber(); // NOI18N
    }

    private static String getSimpleClassName(Instance instance) {
        String name = instance.getJavaClass().getName();
        int last = name.lastIndexOf('.'); // NOI18N
        if (last != -1) {
            name = name.substring(last + 1);
        }
        return name;
    }

    private Long getJSUdefined(Heap heap) {
        return getInstanceId(jsUndefidedId, heap, JS_UNDEFIED_CLASS_FQN);
    }

    private Long getJSNull(Heap heap) {
        return getInstanceId(jsNullId, heap, JS_NULL_CLASS_FQN);
    }

    private Long getInstanceId(Map<Heap,Long> objectId, Heap heap, String classFqn) {
        if (heap == null) {
            return Long.valueOf(0);
        }
        Long undef = objectId.get(heap);

        if (undef == null) {
            JavaClass undefinedClass = heap.getJavaClassByName(classFqn);

            if (undefinedClass != null) {
                Instance undefinedInstance = (Instance) undefinedClass.getValueOfStaticField("instance");   // NOI18N

                if (undefinedInstance != null) {
                    undef = new Long(undefinedInstance.getInstanceId());
                }
            }
            if (undef == null) {
                undef = Long.valueOf(0);
            }
            objectId.put(heap, undef);
        }
        return undef;
    }

 }
