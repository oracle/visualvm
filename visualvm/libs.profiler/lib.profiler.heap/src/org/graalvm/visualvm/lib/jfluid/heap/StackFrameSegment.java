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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
class StackFrameSegment extends TagBounds {

    private static final int FRAME_DIV = 512;
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofHeap hprofHeap;
    final int methodIDOffset;
    final int stackFrameIDOffset;
    final int lengthOffset;
    final int sourceIDOffset;
    final int methodSignatureIDOffset;
    final int timeOffset;
    final int classSerialNumberOffset;
    final int lineNumberOffset;
    private Map<Long,Long> idToFrame;
    private Map<Integer,String> classCache = Collections.synchronizedMap(new LoadClassCache<>());

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StackFrameSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.STACK_FRAME, start, end);

        int idSize = heap.dumpBuffer.getIDSize();
        hprofHeap = heap;
        timeOffset = 1;
        lengthOffset = timeOffset + 4;
        stackFrameIDOffset = lengthOffset + 4;
        methodIDOffset = stackFrameIDOffset + idSize;
        methodSignatureIDOffset = methodIDOffset + idSize;
        sourceIDOffset = methodSignatureIDOffset + idSize;
        classSerialNumberOffset = sourceIDOffset + idSize;
        lineNumberOffset = classSerialNumberOffset + 4;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    StackFrame getStackFrameByID(long stackFrameID) {
        Long initialOffset;
        long[] offset;

        initIdToFrame();
        initialOffset = idToFrame.get(new Long(stackFrameID/FRAME_DIV));
        if (initialOffset == null) {
            initialOffset = new Long(startOffset);
        }
        offset = new long[] { initialOffset.longValue() };
        while (offset[0] < endOffset) {
            long start = offset[0];
            long frameID = readStackFrameTag(offset);

            if (frameID == stackFrameID) {
                return new StackFrame(this, start);
            }
        }
        return null;
    }

    private HprofByteBuffer getDumpBuffer() {
        return  hprofHeap.dumpBuffer;
    }

    private long readStackFrameTag(long[] offset) {
        long start = offset[0];

        if (hprofHeap.readTag(offset) != HprofHeap.STACK_FRAME) {
            return 0;
        }

        return getDumpBuffer().getID(start + stackFrameIDOffset);
    }
    
    private synchronized void initIdToFrame() {
        if (idToFrame == null) {
            long[] offset = new long[] { startOffset };

            idToFrame = new HashMap<>();
            while (offset[0] < endOffset) {
                long start = offset[0];
                long frameID = readStackFrameTag(offset);
                Long frameIDMask = new Long(frameID/FRAME_DIV);
                Long minOffset = idToFrame.get(frameIDMask);
                
                if (minOffset == null || minOffset > start) {
                    idToFrame.put(frameIDMask, new Long(start));
                }
            }
//            System.out.println("idToFrame size:"+idToFrame.size());
        }
    }
    
    String getClassNameBySerialNumber(int classSerialNumber) {
        Integer classSerialNumberObj = Integer.valueOf(classSerialNumber);
        String className = classCache.get(classSerialNumberObj);
        
        if (className == null) {
            LoadClass loadClass = hprofHeap.getLoadClassSegment().getClassBySerialNumber(classSerialNumber);
            
            if (loadClass != null) {
                className = loadClass.getName();
            } else {
                className = "N/A";      // NOI18N
            }
            classCache.put(classSerialNumberObj, className);
        }
        return className;
    }

    private static class LoadClassCache<K,V> extends LinkedHashMap<K,V> {
        private static final int SIZE = 1000;
        
        LoadClassCache() {
            super(SIZE,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > SIZE;
        }
    }

}
