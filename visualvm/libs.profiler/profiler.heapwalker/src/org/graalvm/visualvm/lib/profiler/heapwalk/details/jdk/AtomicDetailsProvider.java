/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class AtomicDetailsProvider extends DetailsProvider.Basic {

    private static final String BOOLEAN_MASK = "java.util.concurrent.atomic.AtomicBoolean+";    // NOI18N
    private static final String INTEGER_MASK = "java.util.concurrent.atomic.AtomicInteger+";    // NOI18N
    private static final String LONG_MASK = "java.util.concurrent.atomic.AtomicLong+";          // NOI18N
    private static final String REFERENCE_MASK = "java.util.concurrent.atomic.AtomicReference+";// NOI18N

    public AtomicDetailsProvider() {
        super(BOOLEAN_MASK, INTEGER_MASK, LONG_MASK, REFERENCE_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        if (BOOLEAN_MASK.equals(className)) {
            int value = DetailsUtils.getIntFieldValue(instance, "value", 0);                    // NOI18N
            return Boolean.toString(value != 0);
        } else if (INTEGER_MASK.equals(className)) {
            int value = DetailsUtils.getIntFieldValue(instance, "value", 0);                    // NOI18N
            return Integer.toString(value);
        } else if (LONG_MASK.equals(className)) {
            long value = DetailsUtils.getLongFieldValue(instance, "value", 0);                  // NOI18N
            return Long.toString(value);
        } else if (REFERENCE_MASK.equals(className)) {
            Object value = instance.getValueOfField("value");                                   // NOI18N
            if (value instanceof Instance) {
                Instance i = (Instance)value;
                String s = DetailsUtils.getInstanceString(i);
                s = s == null ? "#" + i.getInstanceNumber() : ": " + s;                         // NOI18N
                return BrowserUtils.getSimpleType(i.getJavaClass().getName()) + s;
            }
        }
        return null;
    }
    
}
