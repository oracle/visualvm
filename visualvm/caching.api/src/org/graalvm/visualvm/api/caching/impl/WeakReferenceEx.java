/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.api.caching.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Extended {@linkplain WeakReference} to allow euqals() and hashCode()
 * to be computed from the referenced object
 *
 * @author Jaroslav Bachorik
 */
class WeakReferenceEx<T> extends WeakReference<T> {
    private int hashCode;

    public WeakReferenceEx(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
        hashCode = referent != null ? referent.hashCode() : 0;
    }

    public WeakReferenceEx(T referent) {
        super(referent);
        hashCode = referent != null ? referent.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WeakReferenceEx)) return false;
        WeakReferenceEx other = (WeakReferenceEx)obj;
        if (get() == null || other.get() == null) return false;
        return get().equals(other.get());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
