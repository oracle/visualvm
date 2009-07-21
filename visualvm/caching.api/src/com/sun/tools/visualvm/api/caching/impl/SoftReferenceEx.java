/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
