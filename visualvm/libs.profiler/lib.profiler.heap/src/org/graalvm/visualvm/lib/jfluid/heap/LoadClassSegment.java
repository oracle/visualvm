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

/**
 *
 * @author Tomas Hurka
 */
class LoadClassSegment extends TagBounds {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofHeap hprofHeap;
    final int classIDOffset;
    final int classSerialNumberOffset;
    final int lengthOffset;
    final int nameStringIDOffset;
    final int stackTraceSerialOffset;
    final int timeOffset;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LoadClassSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.LOAD_CLASS, start, end);

        int idSize = heap.dumpBuffer.getIDSize();
        hprofHeap = heap;
        timeOffset = 1;
        lengthOffset = timeOffset + 4;
        classSerialNumberOffset = lengthOffset + 4;
        classIDOffset = classSerialNumberOffset + 4;
        stackTraceSerialOffset = classIDOffset + idSize;
        nameStringIDOffset = stackTraceSerialOffset + 4;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    LoadClass getClassByID(long classObjectID) {
        long[] offset = new long[] { startOffset };

        while (offset[0] < endOffset) {
            long start = offset[0];
            long classID = readLoadClassID(offset);

            if (classID == classObjectID) {
                return new LoadClass(this, start);
            }
        }

        return null;
    }

    LoadClass getClassBySerialNumber(int classSerialNumber) {
        long[] offset = new long[] { startOffset };

        while (offset[0] < endOffset) {
            long start = offset[0];
            int serial = readLoadClassSerialNumber(offset);

            if (serial == classSerialNumber) {
                return new LoadClass(this, start);
            }
        }

        return null;
    }
    
    void setLoadClassOffsets() {
        ClassDumpSegment classDumpSegment = hprofHeap.getClassDumpSegment();
        long[] offset = new long[] { startOffset };

        while (offset[0] < endOffset) {
            long start = offset[0];
            long classID = readLoadClassID(offset);
            ClassDump classDump = classDumpSegment.getClassDumpByID(classID);

            if (classDump != null) {
                classDump.setClassLoadOffset(start);
            }
        }
    }

    private HprofByteBuffer getDumpBuffer() {
        HprofByteBuffer dumpBuffer = hprofHeap.dumpBuffer;

        return dumpBuffer;
    }

    private int readLoadClassSerialNumber(long[] offset) {
        long start = offset[0];

        if (hprofHeap.readTag(offset) != HprofHeap.LOAD_CLASS) {
            return 0;
        }

        return getDumpBuffer().getInt(start + classSerialNumberOffset);
    }
    
    private long readLoadClassID(long[] offset) {
        long start = offset[0];

        if (hprofHeap.readTag(offset) != HprofHeap.LOAD_CLASS) {
            return 0;
        }

        return getDumpBuffer().getID(start + classIDOffset);
    }
}
