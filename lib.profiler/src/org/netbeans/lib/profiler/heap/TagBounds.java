/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.heap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static org.netbeans.lib.profiler.heap.HprofHeap.*;

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
