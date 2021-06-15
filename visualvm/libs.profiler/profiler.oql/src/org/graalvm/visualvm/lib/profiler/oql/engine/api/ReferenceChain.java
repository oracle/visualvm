/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.oql.engine.api;

import java.lang.ref.WeakReference;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;

/**
 * Represents a chain of references to some target object
 *
 * @author      Bill Foote
 */
final public class ReferenceChain {
    private WeakReference<Object> obj;	// Object referred to
    ReferenceChain next;	// Next in chain
    private Heap heap;
    private long id;
    private char type;

    private static char TYPE_INSTANCE = 0;
    private static char TYPE_CLASS = 1;

    public ReferenceChain(Heap heap, Object obj, ReferenceChain next) {
        this.obj = new WeakReference(obj);
        this.next = next;
        this.heap = heap;

        if (obj instanceof Instance) {
            type = TYPE_INSTANCE;
            id = ((Instance)obj).getInstanceId();
        } else if (obj instanceof JavaClass) {
            type = TYPE_CLASS;
            id = ((JavaClass)obj).getJavaClassId();
        }
    }

    public Object getObj() {
        Object o = obj.get();
        if (o == null) {
            if (type == TYPE_INSTANCE) {
                o = heap.getInstanceByID(id);
            } else if (type == TYPE_CLASS) {
                o = heap.getJavaClassByID(id);
            }
            obj = new WeakReference(o);
        }
        return o;
    }

    public ReferenceChain getNext() {
        return next;
    }

    public boolean contains(Object obj) {
        ReferenceChain tmp = this;
        while (tmp != null) {
            if (tmp.getObj().equals(obj)) return true;
            tmp = tmp.next;
        }
        return false;
    }

    public int getDepth() {
        int count = 1;
        ReferenceChain tmp = next;
        while (tmp != null) {
            count++;
            tmp = tmp.next;
        }
        return count;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (obj instanceof Instance) {
            Instance inst = (Instance)obj;
            sb.append(inst.getJavaClass().getName()).append("#").append(inst.getInstanceNumber());
        } else if (obj instanceof JavaClass) {
            sb.append("class of ").append(((JavaClass)obj).getName());
        }
        sb.append(next != null ? ("->" + next.toString()) : "");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReferenceChain other = (ReferenceChain) obj;
        if (this.obj != other.obj && (this.obj == null || !this.obj.equals(other.obj))) {
            return false;
        }
        if (this.next != other.next && (this.next == null || !this.next.equals(other.next))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.obj != null ? this.obj.hashCode() : 0);
        hash = 79 * hash + (this.next != null ? this.next.hashCode() : 0);
        return hash;
    }
}
