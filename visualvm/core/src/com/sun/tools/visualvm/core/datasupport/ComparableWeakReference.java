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
package com.sun.tools.visualvm.core.datasupport;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * WeakReference delegating hashCode() and equals(Object) methods to the referenced object.
 *
 * @author Jaroslav Bachorik
 */
public final class ComparableWeakReference<T> extends WeakReference<T> {

    
    /**
     * Creates new instance of ComparableWeakReference that refers to the given object and is
     * registered with the given queue.
     *
     * @param referent object the new weak reference will refer to
     * @param q the queue with which the reference is to be registered,
     *          or <tt>null</tt> if registration is not required
     */
    public ComparableWeakReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

    /**
     * Creates new instance of ComparableWeakReference that refers to the given object.  The new
     * reference is not registered with any queue.
     *
     * @param referent object the new weak reference will refer to
     */
    public ComparableWeakReference(T referent) {
        super(referent);
    }

    public int hashCode() {
        return this.get() != null ? this.get().hashCode() : 0;
    }

    public boolean equals(Object o) {
        if (this.get() == null && o == null) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof ComparableWeakReference)) {
            return false;
        }
        try {
            return this.get().equals(((ComparableWeakReference) o).get());
        } catch (Exception e) {}
        return false;
    }
}
