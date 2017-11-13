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
package com.sun.tools.visualvm.heapviewer.truffle.details;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class RubyDetailsProvider extends DetailsProvider.Basic {

    private static final String RUBY_OBJECT_TYPE_MASK = "org.truffleruby.language.RubyObjectType+";   // NOI18N
    private static final String ASCII_ROPE_MASK = "org.truffleruby.core.rope.AsciiOnlyLeafRope";   // NOI18N
    private static final String CONCAT_ROPE_MASK = "org.truffleruby.core.rope.ConcatRope"; // NOI18N
    private static final String SUB_ROPE_MASK = "org.truffleruby.core.rope.SubstringRope";  // NOI18N
    private static final String INVALID_ROPE_MASK = "org.truffleruby.core.rope.InvalidLeafRope";    // NOI18N
    private static final String VALID_ROPE_MASK = "org.truffleruby.core.rope.ValidLeafRope";    // NOI18N
    private static final String INT_ROPE_MASK = "org.truffleruby.core.rope.LazyIntRope"; // NOI18N
    private static final String ROPE_TABLE_KEY_MASK = "org.truffleruby.core.rope.RopeTable$Key"; // NOI18N
    private static final String ENCODING_MASK = "org.jcodings.Encoding+";   // NOI18N

    public RubyDetailsProvider() {
        super(RUBY_OBJECT_TYPE_MASK,ASCII_ROPE_MASK,CONCAT_ROPE_MASK,SUB_ROPE_MASK,
                ROPE_TABLE_KEY_MASK,INVALID_ROPE_MASK,VALID_ROPE_MASK,
                INT_ROPE_MASK, ENCODING_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (RUBY_OBJECT_TYPE_MASK.equals(className)) {
            String name = instance.getJavaClass().getName();
            int index = name.lastIndexOf('$');

            if (index == -1) {
                index = name.lastIndexOf('.');
            }
            return name.substring(index+1);
        }
        if (ASCII_ROPE_MASK.equals(className)) {
            Integer len = (Integer) instance.getValueOfField("byteLength");
            return getByteArrayFieldString(instance, "bytes", 0, len.intValue(), "...");
        }
        if (CONCAT_ROPE_MASK.equals(className)) {
            Object vall = instance.getValueOfField("left");   // NOI18N
            Object valr = instance.getValueOfField("right");   // NOI18N

            String left = DetailsUtils.getInstanceString((Instance)vall, heap);

            if (left == null) {
                return DetailsUtils.getInstanceString((Instance)valr, heap);
            }
            if (valr == null || left.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                return left;
            }
            String value = left + DetailsUtils.getInstanceString((Instance)valr, heap);

            if (value.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                return value.substring(0, DetailsUtils.MAX_ARRAY_LENGTH) + "...";
            }
            return value;
        }
        if (SUB_ROPE_MASK.equals(className)) {
            Object offset = instance.getValueOfField("byteOffset");   // NOI18N
            Object child = instance.getValueOfField("child");   // NOI18N
            String childString = DetailsUtils.getInstanceString((Instance) child, heap);
            int byteOffset;

            if (offset == null) {
                offset = instance.getValueOfField("offset");   // NOI18N
            }
            byteOffset = ((Integer) offset).intValue();

            if (childString.length() > byteOffset) {
                return childString.substring(byteOffset);
            }
        }
        if (ENCODING_MASK.equals(className)) {
            return getByteArrayFieldString(instance, "name", 0, -1, "...");
        }
        if (ROPE_TABLE_KEY_MASK.equals(className)) {
            byte[] bytes = getByteArrayFieldString(instance, "bytes", 0, -1);
            String encodingString = DetailsUtils.getInstanceFieldString(instance, "encoding", heap);

            return getString(bytes, encodingString, "...");
        }
        if (INVALID_ROPE_MASK.equals(className)) {
            byte[] bytes = getByteArrayFieldString(instance, "bytes", 0, -1);
            String encodingString = DetailsUtils.getInstanceFieldString(instance, "encoding", heap);

            return getString(bytes, encodingString, "...");
        }
        if (VALID_ROPE_MASK.equals(className)) {
            byte[] bytes = getByteArrayFieldString(instance, "bytes", 0, -1);
            String encodingString = DetailsUtils.getInstanceFieldString(instance, "encoding", heap);

            return getString(bytes, encodingString, "...");
        }
        if (INT_ROPE_MASK.equals(className)) {
            return Integer.toString(DetailsUtils.getIntFieldValue(instance, "value", 0));
        }
        return null;
    }

    private byte[] getByteArrayFieldString(Instance instance, String field, int offset, int count) {
        Object fieldVal = instance.getValueOfField(field);
        if (fieldVal instanceof PrimitiveArrayInstance) {
            PrimitiveArrayInstance array = (PrimitiveArrayInstance)fieldVal;
            List<String> values = array.getValues();
            if (values != null) {
                int valuesCount = count < 0 ? values.size() - offset :
                                  Math.min(count, values.size() - offset);
                int estimatedSize = Math.min(valuesCount, DetailsUtils.MAX_ARRAY_LENGTH + 1);
                byte bytes[] = new byte[estimatedSize];
                int lastValue = offset + valuesCount - 1;
                for (int i = offset; i <= lastValue; i++) {
                    bytes[i-offset] = Byte.parseByte(values.get(i));
                    if (i-offset+1 >= DetailsUtils.MAX_ARRAY_LENGTH) {
                        break;
                    }
                }
                return bytes;
            }
        }
        return null;
    }

    private String getByteArrayFieldString(Instance instance, String field, int offset, int count, String trailer) {
        byte[] bytes = getByteArrayFieldString(instance, field, offset, count);

        return getString(bytes, Charset.defaultCharset().name(), trailer);
    }

    private String getString(byte[] bytes, String encodingString, String trailer) {
        if (bytes != null) {
            String val;
            int len = Math.min(bytes.length, DetailsUtils.MAX_ARRAY_LENGTH);
            try {
                val = new String(bytes, 0, len, encodingString);
            } catch (UnsupportedEncodingException ex) {
                val = new String(bytes, 0, len);
            }
            if (bytes.length > DetailsUtils.MAX_ARRAY_LENGTH) {
                return val + trailer;
            }
            return val;
        }
        return null;
    }
}
