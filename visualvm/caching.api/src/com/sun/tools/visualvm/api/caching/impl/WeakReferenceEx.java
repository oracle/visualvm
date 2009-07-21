/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.api.caching.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Extended {@linkplain SoftReference} to allow euqals() and hashCode()
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
