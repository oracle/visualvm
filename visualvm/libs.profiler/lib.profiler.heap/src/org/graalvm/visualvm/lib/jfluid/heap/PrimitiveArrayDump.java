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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.List;


/**
 *
 * @author Tomas Hurka
 */
class PrimitiveArrayDump extends ArrayDump implements PrimitiveArrayInstance {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int charSize = 2;  // Character.BYTES

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    PrimitiveArrayDump(ClassDump cls, long offset) {
        super(cls, offset);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long getSize() {
        return dumpClass.classDumpSegment.getArraySize(getType(), getLength());
    }

    public List<String> getValues() {
        HprofByteBuffer dumpBuffer = dumpClass.getHprofBuffer();
        HprofHeap heap = dumpClass.getHprof();
        byte type = getType();
        long offset = getArrayStartOffset();

        return new PrimitiveArrayLazyList(dumpBuffer, getLength(), offset, heap.getValueSize(type), type);
    }

    char[] getChars(int start, int length) {
        assert getType() == HprofHeap.CHAR;

        char[] chars = new char[length];
        long offset = getArrayStartOffset() + ((long)start * (long)charSize);
        HprofByteBuffer dumpBuffer = dumpClass.getHprofBuffer();

        for (int i = 0; i < length; i++) {
            chars[i] = dumpBuffer.getChar(offset + (i * charSize));
        }

        return chars;
    }

    byte[] getBytes(int start, int length) {
        assert getType() == HprofHeap.BYTE;

        byte[] bytes = new byte[length];
        long offset = getArrayStartOffset() + ((long)start);
        HprofByteBuffer dumpBuffer = dumpClass.getHprofBuffer();

        for (int i = 0; i < length; i++) {
            bytes[i] = dumpBuffer.get(offset+i);
        }

        return bytes;
    }

    private long getArrayStartOffset() {
        int idSize = dumpClass.getHprofBuffer().getIDSize();

        return fileOffset + 1 + idSize + 4 + 4 + 1;
    }

    private byte getType() {
        HprofByteBuffer dumpBuffer = dumpClass.getHprofBuffer();
        int idSize = dumpBuffer.getIDSize();

        return dumpBuffer.get(fileOffset + 1 + idSize + 4 + 4);
    }
}
