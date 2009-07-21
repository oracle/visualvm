/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.api.caching.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * Extended {@linkplain SoftReference} to allow euqals() and hashCode()
 * to be computed from the referenced object
 *
 * @author Jaroslav Bachorik
 */
class SoftReferenceEx<T> extends SoftReference<T> {
    private int hashCode;

    public SoftReferenceEx(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
        hashCode = referent != null ? referent.hashCode() : 0;
    }

    public SoftReferenceEx(T referent) {
        super(referent);
        hashCode = referent != null ? referent.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SoftReferenceEx)) return false;
        SoftReferenceEx other = (SoftReferenceEx)obj;
        if (get() == null || other.get() == null) return false;
        return get().equals(other.get());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
