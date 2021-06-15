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

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 *
 * @author Tomas Hurka
 */
class StringSegment extends TagBounds {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final int UTF8CharsOffset;
    private final int lengthOffset;
    private final int stringIDOffset;
    private final int timeOffset;
    private LongHashMap stringIDMap;
    private HprofHeap hprofHeap;
    private Map<Long,String> stringCache = Collections.synchronizedMap(new StringCache());
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StringSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.STRING, start, end);

        int idSize = heap.dumpBuffer.getIDSize();
        hprofHeap = heap;
        timeOffset = 1;
        lengthOffset = timeOffset + 4;
        stringIDOffset = lengthOffset + 4;
        UTF8CharsOffset = stringIDOffset + idSize;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    String getStringByID(long stringID) {
        Long stringIDObj = new Long(stringID);
        String string = stringCache.get(stringIDObj);
        if (string == null) {
            string = createStringByID(stringID);
            stringCache.put(stringIDObj,string);
        }
        return string;
    }

    private String createStringByID(long stringID) {
        return getString(getStringOffsetByID(stringID));
    }

    private String getString(long start) {
        HprofByteBuffer dumpBuffer = getDumpBuffer();

        if (start == -1) {
            return "<unknown string>"; // NOI18N
        }

        int len = dumpBuffer.getInt(start + lengthOffset);
        byte[] chars = new byte[len - dumpBuffer.getIDSize()];
        dumpBuffer.get(start + UTF8CharsOffset, chars);

        String s = "Error"; // NOI18N

        try {
            s = new String(chars, "UTF-8"); // NOI18N
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        return s;
    }

    private synchronized long getStringOffsetByID(long stringID) {
        if (stringIDMap == null) {
            stringIDMap = new LongHashMap(32768);

            long[] offset = new long[] { startOffset };

            while (offset[0] < endOffset) {
                long start = offset[0];
                long sID = readStringTag(offset);
                if (sID != 0) {
                    stringIDMap.put(sID, start);
                }
            }
        }
        if (stringID == 0) {
            return -1; // string not found
        }
        return stringIDMap.get(stringID);
    }

    private HprofByteBuffer getDumpBuffer() {
        HprofByteBuffer dumpBuffer = hprofHeap.dumpBuffer;

        return dumpBuffer;
    }

    private long readStringTag(long[] offset) {
        long start = offset[0];

        if (hprofHeap.readTag(offset) != HprofHeap.STRING) {
            return 0;
        }

        return getDumpBuffer().getID(start + stringIDOffset);
    }

    private static class StringCache extends LinkedHashMap {
        private static final int SIZE = 1000;
        
        StringCache() {
            super(SIZE,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size() > SIZE) {
                return true;
            }
            return false;
        }
    }

}
