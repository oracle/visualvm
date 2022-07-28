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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Tomas Hurka
 */
class InstanceDump extends HprofObject implements Instance {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    final ClassDump dumpClass;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    InstanceDump(ClassDump cls, long offset) {
        super(offset);
        dumpClass = cls;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public List<FieldValue> getFieldValues() {
        long offset = fileOffset + getInstanceFieldValuesOffset();
        List<Field> fields = dumpClass.getAllInstanceFields();
        List<FieldValue> values = new ArrayList<>(fields.size());

        for (Field f : fields) {
            HprofField field = (HprofField)f;

            if (field.getValueType() == HprofHeap.OBJECT) {
                values.add(new HprofInstanceObjectValue(this, field, offset));
            } else {
                values.add(new HprofInstanceValue(this, field, offset));
            }

            offset += field.getValueSize();
        }

        return values;
    }

    public boolean isGCRoot() {
        return getHprof().isGCRoot(this);
    }

    public long getInstanceId() {
        return dumpClass.getHprofBuffer().getID(fileOffset + 1);
    }

    public int getInstanceNumber() {
        return getHprof().idToOffsetMap.get(getInstanceId()).getIndex();
    }

    public JavaClass getJavaClass() {
        return dumpClass;
    }

    public Instance getNearestGCRootPointer() {
        return getHprof().getNearestGCRootPointer(this);
    }

    public long getReachableSize() {
        return 0;
    }

    public List<Value> getReferences() {
        return getHprof().findReferencesFor(getInstanceId());
    }

    public long getRetainedSize() {
        return getHprof().getRetainedSize(this);
    }

    public long getSize() {
        return dumpClass.getInstanceSize();
    }

    public List<FieldValue> getStaticFieldValues() {
        return dumpClass.getStaticFieldValues();
    }

    public Object getValueOfField(String name) {
        List<FieldValue> fieldValues = getFieldValues();
        
        for (int i = fieldValues.size() - 1; i >= 0; i--) {
            FieldValue fieldValue = fieldValues.get(i);
            if (fieldValue.getField().getName().equals(name)) {
                if (fieldValue instanceof HprofInstanceObjectValue) {
                    return ((HprofInstanceObjectValue) fieldValue).getInstance();
                } else {
                    return ((HprofInstanceValue) fieldValue).getTypeValue();
                }
            }
        }
        
        return null;
        
//        Iterator fIt = getFieldValues().iterator();
//        FieldValue matchingFieldValue = null;
//
//        while (fIt.hasNext()) {
//            FieldValue fieldValue = (FieldValue) fIt.next();
//
//            if (fieldValue.getField().getName().equals(name)) {
//                matchingFieldValue = fieldValue;
//            }
//        }
//
//        if (matchingFieldValue == null) {
//            return null;
//        }
//
//        if (matchingFieldValue instanceof HprofInstanceObjectValue) {
//            return ((HprofInstanceObjectValue) matchingFieldValue).getInstance();
//        } else {
//            return ((HprofInstanceValue) matchingFieldValue).getTypeValue();
//        }
    }

    private int getInstanceFieldValuesOffset() {
        int idSize = dumpClass.getHprofBuffer().getIDSize();

        return 1 + idSize + 4 + idSize + 4;
    }
    
    private HprofHeap getHprof() {
        return dumpClass.getHprof();
    }
}
