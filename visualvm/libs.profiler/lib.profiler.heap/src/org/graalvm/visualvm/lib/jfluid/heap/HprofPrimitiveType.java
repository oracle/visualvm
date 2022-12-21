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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Tomas Hurka
 */
class HprofPrimitiveType implements PrimitiveType {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Map<Integer,Type> primitiveTypeMap;

    static {
        primitiveTypeMap = new HashMap<>(10);
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.BOOLEAN), new HprofPrimitiveType("boolean")); //NOI18N
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.CHAR), new HprofPrimitiveType("char")); //NOI18N
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.FLOAT), new HprofPrimitiveType("float")); //NOI18N
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.DOUBLE), new HprofPrimitiveType("double")); //NOI18N
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.BYTE), new HprofPrimitiveType("byte")); //NOI18N
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.SHORT), new HprofPrimitiveType("short")); //NOI18N
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.INT), new HprofPrimitiveType("int")); //NOI18N
        primitiveTypeMap.put(Integer.valueOf(HprofHeap.LONG), new HprofPrimitiveType("long")); //NOI18N
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String typeName;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private HprofPrimitiveType(String name) {
        typeName = name;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getName() {
        return typeName;
    }

    static Type getType(byte type) {
        return primitiveTypeMap.get(Integer.valueOf(type));
    }
}
