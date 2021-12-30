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

package org.graalvm.visualvm.lib.jfluid.results.memory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * A terminal node used in Object Allocation Profiling Calling Context Tree (CCT).
 * Contains the information gathered during object allocation profiling, which can be calculated  for intermediate nodes
 * if known for terminal nodes.
 * <p>
 * Normally used as a leaf, except in case there are multiple same paths in the tree with different length
 * <p>
 * The information in TermCCTNode represents all objects of the same type allocated using same call path.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class RuntimeObjAllocTermCCTNode extends RuntimeMemoryCCTNode {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public long nCalls; // # of invocations
    public long totalObjSize; // object size in Bytes

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RuntimeObjAllocTermCCTNode(int methodId) {
        super(methodId);
    }

    protected RuntimeObjAllocTermCCTNode() {
    } // only for I/O

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getType() {
        return TYPE_RuntimeObjAllocTermCCTNode;
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);
        nCalls = in.readLong();
        totalObjSize = in.readLong();
    }

    public void updateForNewObject(long objSize) {
        nCalls++;
        totalObjSize += objSize;
    }

    public void updateForRemovedObject(long objSize) {
        totalObjSize -= objSize;
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);
        out.writeLong(nCalls);
        out.writeLong(totalObjSize);
    }
}
