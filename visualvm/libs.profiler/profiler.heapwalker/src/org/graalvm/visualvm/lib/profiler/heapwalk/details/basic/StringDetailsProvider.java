/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.basic;

import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.StringDecoder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public final class StringDetailsProvider extends DetailsProvider.Basic {

    static final String STRING_MASK = "java.lang.String";                           // NOI18N
    static final String BUILDERS_MASK = "java.lang.AbstractStringBuilder+";         // NOI18N

    public StringDetailsProvider() {
        super(STRING_MASK, BUILDERS_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        if (STRING_MASK.equals(className)) {                                        // String
            byte coder = DetailsUtils.getByteFieldValue(instance, "coder", (byte) -1);     // NOI18N
            if (coder == -1) {
                int offset = DetailsUtils.getIntFieldValue(instance, "offset", 0);      // NOI18N
                int count = DetailsUtils.getIntFieldValue(instance, "count", -1);       // NOI18N
                return DetailsUtils.getPrimitiveArrayFieldString(instance, "value",     // NOI18N
                        offset, count, null,
                        "...");                // NOI18N
            } else {
                return getJDK9String(instance, "value", coder, null, "...");          // NOI18N
            }
        } else if (BUILDERS_MASK.equals(className)) {                               // AbstractStringBuilder+
            byte coder = DetailsUtils.getByteFieldValue(instance, "coder", (byte) -1);  // NOI18N
            if (coder == -1) {
                int count = DetailsUtils.getIntFieldValue(instance, "count", -1);       // NOI18N
                return DetailsUtils.getPrimitiveArrayFieldString(instance, "value",     // NOI18N
                        0, count, null,
                        "...");                // NOI18N
            } else {
                return getJDK9String(instance, "value", coder, null, "...");          // NOI18N
            }
        }
        return null;
    }
    
    public View getDetailsView(String className, Instance instance) {
        return new ArrayValueView(className, instance);
    }
    
    private String getJDK9String(Instance instance, String field, byte coder, String separator, String trailer) {
        Object byteArray = instance.getValueOfField(field);
        if (byteArray instanceof PrimitiveArrayInstance) {
            List<String> values = ((PrimitiveArrayInstance) byteArray).getValues();
            if (values != null) {
                Heap heap = instance.getJavaClass().getHeap();
                StringDecoder decoder = new StringDecoder(heap, coder, values);
                int valuesCount = decoder.getStringLength();
                int separatorLength = separator == null ? 0 : separator.length();
                int trailerLength = trailer == null ? 0 : trailer.length();
                int estimatedSize = Math.min(valuesCount * (1 + separatorLength), DetailsUtils.MAX_ARRAY_LENGTH + trailerLength);
                StringBuilder value = new StringBuilder(estimatedSize);
                int lastValue = valuesCount - 1;
                for (int i = 0; i <= lastValue; i++) {
                    if (value.length() >= DetailsUtils.MAX_ARRAY_LENGTH) {
                        if (trailerLength > 0) {
                            value.append(trailer);
                        }
                        break;
                    }
                    value.append(decoder.getValueAt(i));
                    if (separator != null && i < lastValue) {
                        value.append(separator);
                    }
                }
                return value.toString();
            }
        }
        return null;
    }
}
