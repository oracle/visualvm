/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
class StackFrame extends HprofObject {

    static final int NO_LINE_INFO = 0;
    static final int UNKNOWN_LOCATION = -1;
    static final int COMPILED_METHOD = -2;
    static final int NATIVE_METHOD = -3;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final StackFrameSegment stackFrameSegment;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StackFrame(StackFrameSegment segment, long offset) {
        super(offset);
        stackFrameSegment = segment;
        assert getHprofBuffer().get(offset) == HprofHeap.STACK_FRAME;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    long getStackFrameID() {
        return getHprofBuffer().getID(fileOffset + stackFrameSegment.stackFrameIDOffset);
    }

    String getMethodName() {
        return getStringByOffset(stackFrameSegment.methodIDOffset);
    }

    String getMethodSignature() {
        return getStringByOffset(stackFrameSegment.methodSignatureIDOffset);
    }

    String getSourceFile() {
        return getStringByOffset(stackFrameSegment.sourceIDOffset);
    }

    String getClassName() {
        int classSerial = getHprofBuffer().getInt(fileOffset + stackFrameSegment.classSerialNumberOffset);
        return stackFrameSegment.getClassNameBySerialNumber(classSerial);
    }

    int getLineNumber() {
        return getHprofBuffer().getInt(fileOffset + stackFrameSegment.lineNumberOffset);
    }

    private HprofByteBuffer getHprofBuffer() {
        return stackFrameSegment.hprofHeap.dumpBuffer;
    }
    
    private String getStringByOffset(long offset) {
        long stringID = getHprofBuffer().getID(fileOffset + offset);
        return stackFrameSegment.hprofHeap.getStringSegment().getStringByID(stringID);
    }
}
