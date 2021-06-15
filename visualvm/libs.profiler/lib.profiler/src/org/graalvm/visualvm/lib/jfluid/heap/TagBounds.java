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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static org.graalvm.visualvm.lib.jfluid.heap.HprofHeap.*;

/**
 *
 * @author Tomas Hurka
 */
class TagBounds {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    final int tag;
    final long startOffset;
    long endOffset;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    TagBounds(int t, long start, long end) {
        tag = t;
        startOffset = start;
        endOffset = end;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    TagBounds union(TagBounds otherTagBounds) {
        if (otherTagBounds == null) {
            return this;
        }

        long start = Math.min(startOffset, otherTagBounds.startOffset);
        long end = Math.max(endOffset, otherTagBounds.endOffset);

        return new TagBounds(-1, start, end);
    }

    //---- Serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(tag);
        out.writeLong(startOffset);
        out.writeLong(endOffset);
    }

    TagBounds(DataInputStream dis) throws IOException {
        tag = dis.readInt();
        startOffset = dis.readLong();
        endOffset = dis.readLong();
    }

    static void writeToStream(TagBounds[] bounds, DataOutputStream out) throws IOException {
        int tags = 0;
        for (int i = 0; i < bounds.length; i++) {
            if (bounds[i] != null) {
                tags++;
            }
        }
        out.writeInt(tags);
        for (int i = 0; i < bounds.length; i++) {
            if (bounds[i] != null) {
                bounds[i].writeToStream(out);
            }
        }
    }

    static void readFromStream(DataInputStream dis, HprofHeap heap, TagBounds[] heapTagBounds) throws IOException {
        int tags = dis.readInt();
        for (int i = 0; i<tags; i++) {
            int tag = dis.readInt();
            long startOffset = dis.readLong();
            long endOffset = dis.readLong();
            TagBounds newBounds;
                        
            if (tag == LOAD_CLASS) {
                newBounds = new LoadClassSegment(heap, startOffset, endOffset);
            } else if (tag == STRING) {
                newBounds = new StringSegment(heap, startOffset, endOffset);
            } else if (tag == STACK_TRACE) {
                newBounds = new StackTraceSegment(heap, startOffset, endOffset);
            } else if (tag == STACK_FRAME) {
                newBounds = new StackFrameSegment(heap, startOffset, endOffset);
            } else if (tag == CLASS_DUMP) {
                newBounds = new ClassDumpSegment(heap, startOffset, endOffset, dis);
            } else {
                newBounds = new TagBounds(tag, startOffset, endOffset);
            }
            heapTagBounds[newBounds.tag] = newBounds;
        }
    }
}
