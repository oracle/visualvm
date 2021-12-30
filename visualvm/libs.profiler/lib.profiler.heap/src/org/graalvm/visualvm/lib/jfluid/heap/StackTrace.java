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
class StackTrace extends HprofObject {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final StackTraceSegment stackTraceSegment;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StackTrace(StackTraceSegment segment, long offset) {
        super(offset);
        stackTraceSegment = segment;
        assert getHprofBuffer().get(offset) == HprofHeap.STACK_TRACE;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    long getSerialNumber() {
        return getHprofBuffer().getInt(fileOffset + stackTraceSegment.stackTraceSerialNumberOffset);
    }

    long getThreadSerialNumber() {
        return getHprofBuffer().getInt(fileOffset + stackTraceSegment.threadSerialNumberOffset);
    }

    StackFrame[] getStackFrames() {
        HprofByteBuffer buf = getHprofBuffer();
        int frames = buf.getInt(fileOffset + stackTraceSegment.numberOfFramesOffset);
        StackFrame[] fr = new StackFrame[frames];
        long idOffset = fileOffset + stackTraceSegment.framesListOffset;
        StackFrameSegment stackFrameSegment = stackTraceSegment.hprofHeap.getStackFrameSegment();

         for (int i=0;i<frames;i++,idOffset+=buf.getIDSize()) {
             long frameID = buf.getID(idOffset);
             fr[i] = stackFrameSegment.getStackFrameByID(frameID);
        }
        return fr;
    }

    private HprofByteBuffer getHprofBuffer() {
        return stackTraceSegment.hprofHeap.dumpBuffer;
    }

}
