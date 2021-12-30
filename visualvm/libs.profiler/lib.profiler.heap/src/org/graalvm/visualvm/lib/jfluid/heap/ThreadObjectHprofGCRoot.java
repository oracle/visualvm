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
class ThreadObjectHprofGCRoot extends HprofGCRoot implements ThreadObjectGCRoot {

    ThreadObjectHprofGCRoot(HprofGCRoots r, long offset) {
        super(r, offset);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public StackTraceElement[] getStackTrace() {
        int stackTraceSerialNumber = getStackTraceSerialNumber();

        if (stackTraceSerialNumber != 0) {
            StackTrace stackTrace = roots.heap.getStackTraceSegment().getStackTraceBySerialNumber(stackTraceSerialNumber);
            if (stackTrace != null) {
                StackFrame[] frames = stackTrace.getStackFrames();
                StackTraceElement[] stackElements = new StackTraceElement[frames.length];

                for (int i=0;i<frames.length;i++) {
                    StackFrame f = frames[i];
                    String className = f.getClassName();
                    String method = f.getMethodName();
                    String source = f.getSourceFile();
                    int number = f.getLineNumber();

                    if (number == StackFrame.NATIVE_METHOD) {
                        number = -2;
                    } else if (number == StackFrame.NO_LINE_INFO || number == StackFrame.UNKNOWN_LOCATION) {
                        number = -1;
                    }
                    stackElements[i] = new StackTraceElement(className,method,source,number);
                }
                return stackElements;
            }
        }
        return null;
    }

    int getThreadSerialNumber() {
        return getHprofBuffer().getInt(fileOffset + 1 + getHprofBuffer().getIDSize());
    }

    private int getStackTraceSerialNumber() {
        return getHprofBuffer().getInt(fileOffset + 1 + getHprofBuffer().getIDSize() + 4);
    }    

}
