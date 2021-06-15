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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
class StackTraceSegment extends TagBounds {

    private static final int SERIALNUM_DIV = 16;
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofHeap hprofHeap;
    final int threadSerialNumberOffset;
    final int stackTraceSerialNumberOffset;
    final int lengthOffset;
    final int framesListOffset;
    final int numberOfFramesOffset;
    final int timeOffset;
    private Map<Long,Long> serialNumToStackTrace;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StackTraceSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.STACK_TRACE, start, end);

        hprofHeap = heap;
        timeOffset = 1;
        lengthOffset = timeOffset + 4;
        stackTraceSerialNumberOffset = lengthOffset + 4;
        threadSerialNumberOffset = stackTraceSerialNumberOffset + 4;
        numberOfFramesOffset = threadSerialNumberOffset + 4;
        framesListOffset = numberOfFramesOffset + 4;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    StackTrace getStackTraceBySerialNumber(long stackTraceSerialNumber) {
        Long initialOffset;
        long[] offset;

        initSerialNumToFrame();
        initialOffset = serialNumToStackTrace.get(new Long(stackTraceSerialNumber/SERIALNUM_DIV));
        if (initialOffset == null) {
            initialOffset = new Long(startOffset);
        }
        offset = new long[] { initialOffset.longValue() };
        while (offset[0] < endOffset) {
            long start = offset[0];
            long serialNumber = readStackTraceTag(offset);

            if (serialNumber == stackTraceSerialNumber) {
                return new StackTrace(this, start);
            }
        }
        return null;
    }

    private HprofByteBuffer getDumpBuffer() {
        HprofByteBuffer dumpBuffer = hprofHeap.dumpBuffer;

        return dumpBuffer;
    }

    private int readStackTraceTag(long[] offset) {
        long start = offset[0];

        if (hprofHeap.readTag(offset) != HprofHeap.STACK_TRACE) {
            return 0;
        }
        return getDumpBuffer().getInt(start + stackTraceSerialNumberOffset);
    }

    private synchronized void initSerialNumToFrame() {
        if (serialNumToStackTrace == null) {
            long[] offset = new long[] { startOffset };

            serialNumToStackTrace = new HashMap();
            while (offset[0] < endOffset) {
                long start = offset[0];
                long serialNumber = readStackTraceTag(offset);
                Long serialNumberMask = new Long(serialNumber/SERIALNUM_DIV);
                Long minOffset = serialNumToStackTrace.get(serialNumberMask);
                
                if (minOffset == null || minOffset > start) {
                    serialNumToStackTrace.put(serialNumberMask, new Long(start));
                }
            }
//            System.out.println("serialNumToStackTrace size:"+serialNumToStackTrace.size());
        }
    }
}
