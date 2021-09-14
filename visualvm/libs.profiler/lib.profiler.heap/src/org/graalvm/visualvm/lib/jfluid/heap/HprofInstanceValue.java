/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
class HprofInstanceValue extends HprofObject implements FieldValue {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofField field;
    long instanceOffset;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofInstanceValue(InstanceDump i, HprofField f, long fieldOffset) {
        super(fieldOffset);
        instanceOffset = i.fileOffset;
        field = f;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Instance getDefiningInstance() {
        return field.classDump.getHprof().getInstanceByOffset(new long[] {instanceOffset});
    }

    public Field getField() {
        return field;
    }

    public String getValue() {
        return getTypeValue().toString();
    }

    Object getTypeValue() {
        byte type = field.getValueType();
        HprofByteBuffer dumpBuffer = field.classDump.getHprofBuffer();

        return getTypeValue(dumpBuffer, fileOffset, type);
    }

    static Object getTypeValue(final HprofByteBuffer dumpBuffer, final long position, final byte type) {
        switch (type) {
            case HprofHeap.OBJECT:

                long obj = dumpBuffer.getID(position);

                return new Long(obj);
            case HprofHeap.BOOLEAN:

                byte b = dumpBuffer.get(position);

                return Boolean.valueOf(b != 0);
            case HprofHeap.CHAR:

                char ch = dumpBuffer.getChar(position);

                return Character.valueOf(ch);
            case HprofHeap.FLOAT:

                float f = dumpBuffer.getFloat(position);

                return new Float(f);
            case HprofHeap.DOUBLE:

                double d = dumpBuffer.getDouble(position);

                return new Double(d);
            case HprofHeap.BYTE:

                byte bt = dumpBuffer.get(position);

                return Byte.valueOf(bt);
            case HprofHeap.SHORT:

                short sh = dumpBuffer.getShort(position);

                return Short.valueOf(sh);
            case HprofHeap.INT:

                int i = dumpBuffer.getInt(position);

                return Integer.valueOf(i);
            case HprofHeap.LONG:

                long lg = dumpBuffer.getLong(position);

                return Long.valueOf(lg);
            default:
                return "Invalid type " + type; // NOI18N
        }
    }
}
