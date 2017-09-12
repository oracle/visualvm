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
package com.sun.tools.visualvm.truffle.heapwalker.details;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class RDetailsProvider extends DetailsProvider.Basic {

    private static final String RVECTOR_MASK = "com.oracle.truffle.r.runtime.data.RVector+";   // NOI18N
    private static final String RSYMBOL_MASK = "com.oracle.truffle.r.runtime.data.RSymbol"; //NOI18N
    private static final String RFUNCTION_MASK = "com.oracle.truffle.r.runtime.data.RFunction"; //NOI18N

    public RDetailsProvider() {
        super(RVECTOR_MASK, RSYMBOL_MASK, RFUNCTION_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (RVECTOR_MASK.equals(className)) {
            Object rawData = instance.getValueOfField("data");

            if (rawData != null) {
                int size;

                if (rawData instanceof ObjectArrayInstance) {
                    ObjectArrayInstance data = (ObjectArrayInstance) rawData;
                    size = data.getLength();
                    if (size == 1) {
                        Instance string = (Instance) data.getValues().get(0);
                        return DetailsSupport.getDetailsString(string, heap);
                    }
                } else if (rawData instanceof PrimitiveArrayInstance) {
                    PrimitiveArrayInstance data = (PrimitiveArrayInstance) rawData;
                    size = data.getLength();
                    if (size == 1) {
                        return (String) data.getValues().get(0);
                    }
                } else {
                    return null;
                }
                Boolean complete = (Boolean) instance.getValueOfField("complete");
                Integer refCount = (Integer) instance.getValueOfField("refCount");
                String refString;

                switch (refCount.intValue()) {
                    case 0:
                       refString = ", temporary";
                       break;
                    case 1:
                        refString = "";
                        break;
                    case Integer.MAX_VALUE:
                        refString = ", shared permanent";
                        break;
                    default:
                        refString = ", shared";
                }
                return "Size: " + size + (complete ? "" : ", has NAs") +  refString;
            }
        }
        if (RSYMBOL_MASK.equals(className)) {
            Instance name = (Instance) instance.getValueOfField("name");   // NOI18N
            if (name != null) {
                return DetailsSupport.getDetailsString(name, heap);
            }
        }
        if (RFUNCTION_MASK.equals(className)) {
            Instance target = (Instance) instance.getValueOfField("target");   // NOI18N
            String value = target == null ? null : DetailsSupport.getDetailsString(target, heap);
            return value == null || value.isEmpty() ? null : value;
        }
        return null;
    }
}
