/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.api.caching;

/**
 * Generic representation of a cache entry
 * Supports operations based on TTL (time-to-live)
 * @author Jaroslav Bachorik
 */
final public class Entry<T> {

    private long accessTs;
    private long updateTs;
    private T object;

    public Entry(T content) {
        this(content, System.currentTimeMillis());
    }

    public Entry(T content, long lastModified) {
        this.object = content;
        accessTs = lastModified;
        updateTs = accessTs;
    }

    /**
     * 
     * @return Returns the "last-accessed" value (in {@linkplain System#currentTimeMillis()} form)
     */
    public long getAccessTimeStamp() {
        return accessTs;
    }

    public T getContent() {
        return object;
    }

    /**
     * 
     * @return Returns the "last-updated" value (in {@linkplain System#currentTimeMillis()} form)
     */
    public long getUpdateTimeStamp() {
        return updateTs;
    }

    /**
     * Sets the "last-accessed" value (in {@linkplain System#currentTimeMillis()} form)
     * @param ts Timestamp in {@linkplain System#currentTimeMillis()} form
     */
    void setAccessTimeStamp(long ts) {
        accessTs = ts;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Entry other = (Entry) obj;
        if (this.accessTs != other.accessTs) {
            return false;
        }
        if (this.updateTs != other.updateTs) {
            return false;
        }
        if (this.object != other.object && (this.object == null || !this.object.equals(other.object))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + (int) (this.accessTs ^ (this.accessTs >>> 32));
        hash = 61 * hash + (int) (this.updateTs ^ (this.updateTs >>> 32));
        hash = 61 * hash + (this.object != null ? this.object.hashCode() : 0);
        return hash;
    }
}
