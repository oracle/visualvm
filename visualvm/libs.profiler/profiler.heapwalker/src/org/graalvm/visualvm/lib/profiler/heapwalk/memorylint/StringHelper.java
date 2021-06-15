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

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import java.util.List;


/**
 *
 * @author nenik
 */
public class StringHelper {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldCount;
    private FieldAccess fldOffset;
    private FieldAccess fldValue;
    private Heap heap;
    private JavaClass clsString;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StringHelper(Heap heap) {
        this.heap = heap;
        clsString = heap.getJavaClassByName("java.lang.String"); // NOI18N
        fldOffset = new FieldAccess(clsString, "offset"); // NOI18N
        fldCount = new FieldAccess(clsString, "count"); // NOI18N
        fldValue = new FieldAccess(clsString, "value"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String decodeString(Instance in) {
        if (in == null) {
            return "null"; // NOI18N
        }

        if (!"java.lang.String".equals(in.getJavaClass().getName())) { // NOI18N
            return "<<" + in.getJavaClass().getName() + ">>"; // NOI18N
        }

        int off = fldOffset.getIntValue(in);
        int cnt = fldCount.getIntValue(in);
        PrimitiveArrayInstance arrValue = (PrimitiveArrayInstance) fldValue.getRefValue(in);

        if (arrValue == null) {
            return ""; // NOI18N
        }

        char[] data = getCharArray(arrValue);

        return new String(data, off, cnt);
    }

    private char[] getCharArray(PrimitiveArrayInstance in) {
        @SuppressWarnings("unchecked")
        List<String> vals = in.getValues();
        char[] ret = new char[in.getLength()];
        assert (ret.length == vals.size());

        int i = 0;

        for (String v : vals) {
            ret[i++] = v.charAt(0);
        }

        return ret;
    }
}
