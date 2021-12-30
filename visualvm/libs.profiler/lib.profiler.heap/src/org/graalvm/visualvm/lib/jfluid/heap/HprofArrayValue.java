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
class HprofArrayValue extends HprofObject implements ArrayItemValue {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    final ClassDump dumpClass;
    final int index;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofArrayValue(ClassDump cls, long offset, int number) {
        // compute HprofArrayValue unique (file) offset.
        // (+1) added to differ this instance with number == 0 from defining instance
        super(offset + number + 1);
        dumpClass = cls;
        index = number;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Instance getDefiningInstance() {
        return new ObjectArrayDump(dumpClass, getInstanceArrayOffset());
    }

    public int getIndex() {
        return index;
    }

    public Instance getInstance() {
        HprofHeap heap = dumpClass.getHprof();
        HprofByteBuffer dumpBuffer = heap.dumpBuffer;
        int idSize = dumpBuffer.getIDSize();

        long instanceId = dumpBuffer.getID(getInstanceArrayOffset() + 1 + idSize + 4 + 4 + idSize + ((long)index * (long)idSize));

        return heap.getInstanceByID(instanceId);
    }

    private long getInstanceArrayOffset() {
        // see computation in super() above
        return fileOffset - index - 1;
    }
}
