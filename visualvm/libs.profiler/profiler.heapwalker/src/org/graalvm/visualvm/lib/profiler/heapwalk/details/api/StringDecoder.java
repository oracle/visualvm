/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.api;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;

/**
 *
 * @author Tomas Hurka
 */
public final class StringDecoder {

    private static final Map<Heap,int[]> CACHE = new WeakHashMap<>();

    private final byte coder;
    private final List<String> values;
    private int HI_BYTE_SHIFT;
    private int LO_BYTE_SHIFT;

    public StringDecoder(Heap heap, byte c, List<String> val) {
        coder = c;
        values = val;
        if (coder == 1) {
            int[] shiftBytes = CACHE.get(heap);

            if (shiftBytes == null) {
                JavaClass utf16Class = heap.getJavaClassByName("java.lang.StringUTF16"); // NOI18N
                Integer hiShift = (Integer) utf16Class.getValueOfStaticField("HI_BYTE_SHIFT"); // NOI18N
                Integer loShift = (Integer) utf16Class.getValueOfStaticField("LO_BYTE_SHIFT"); // NOI18N

                if (hiShift != null && loShift != null) {
                    shiftBytes = new int[] {hiShift.intValue(), loShift.intValue()};
                } else {
                    // use default
                    shiftBytes = new int[] {0,8};
                }
                CACHE.put(heap, shiftBytes);
            }
            HI_BYTE_SHIFT = shiftBytes[0];
            LO_BYTE_SHIFT = shiftBytes[1];
        }
    }

    public int getStringLength() {
        int size = values.size();
        switch (coder) {
            case -1:
                return size;
            case 0:
                return size;
            case 1:
                return size / 2;
            default:
                return size;
        }
    }

    public String getValueAt(int index) {
        switch (coder) {
            case -1:
                return values.get(index);
            case 0: {
                char ch = (char) (Byte.valueOf(values.get(index)) & 0xff);
                return String.valueOf(ch);
            }
            case 1: {
                index *= 2;
                byte hiByte = Byte.valueOf(values.get(index));
                byte lowByte = Byte.valueOf(values.get(index + 1));
                char ch = (char) (((hiByte & 0xff) << HI_BYTE_SHIFT) |
                                 ((lowByte & 0xff) << LO_BYTE_SHIFT));
                return String.valueOf(ch);
            }
            default:
                return "?"; // NOI18N
        }
    }  
}
