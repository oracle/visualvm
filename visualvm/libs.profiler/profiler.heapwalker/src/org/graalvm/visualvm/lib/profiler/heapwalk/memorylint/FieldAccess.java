/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;


/**
 *
 * @author nenik
 */
public class FieldAccess {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    Field fld;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Field */
    public FieldAccess(JavaClass jc, String name) {
        List<Field> fields = jc.getFields();

        for (Field f : fields) {
            if (f.getName().equals(name)) {
                fld = f;

                break;
            }
        }
        assert (fld != null);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getIntValue(Instance in) {
        @SuppressWarnings("unchecked")
        List<FieldValue> values = in.getFieldValues();

        for (FieldValue fv : values) {
            if (fv.getField().equals(fld)) {
                try {
                    return Integer.parseInt(fv.getValue());
                } catch (NumberFormatException nfe) {
                }
            }
        }
        assert false; // shouldn't reach

        return -1;
    }

    public Instance getRefValue(Instance in) {
        assert fld.getType().getName().equals("object");

        @SuppressWarnings("unchecked")
        List<FieldValue> values = in.getFieldValues();

        for (FieldValue fv : values) {
            if (fv.getField().equals(fld)) {
                return ((ObjectFieldValue) fv).getInstance();
            }
        }
        assert false; // shouldn't reach

        return null;
    }
}
