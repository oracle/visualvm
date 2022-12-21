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

import java.util.Collections;
import java.util.List;


/**
 *
 * @author Tomas Hurka
 */
class ClassDumpInstance implements Instance {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    ClassDump classDump;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    ClassDumpInstance(ClassDump cls) {
        classDump = cls;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public List<FieldValue> getFieldValues() {
        return Collections.emptyList();
    }

    public boolean isGCRoot() {
        return classDump.getHprof().isGCRoot(this);
    }

    public long getInstanceId() {
        return classDump.getJavaClassId();
    }

    public int getInstanceNumber() {
        return classDump.getHprof().idToOffsetMap.get(getInstanceId()).getIndex();
    }

    public JavaClass getJavaClass() {
        return classDump.classDumpSegment.java_lang_Class;
    }

    public Instance getNearestGCRootPointer() {
        return classDump.getHprof().getNearestGCRootPointer(this);
    }

    public long getReachableSize() {
        return 0;
    }

    public List<Value> getReferences() {
        return classDump.getReferences();
    }

    public long getRetainedSize() {
        return classDump.getHprof().getRetainedSize(this);
    }

    public long getSize() {
        return getJavaClass().getInstanceSize();
    }

    public List<FieldValue> getStaticFieldValues() {
        return getJavaClass().getStaticFieldValues();
    }

    public Object getValueOfField(String name) {
        return null;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ClassDumpInstance) {
            return classDump.equals(((ClassDumpInstance) obj).classDump);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return classDump.hashCode();
    }
}
