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

package org.graalvm.visualvm.api.caching;

/**
 * Generic key/value persistor
 * Supports retrieving a value by the given key and storing a value with the given key
 * @author Jaroslav Bachorik
 */
public interface Persistor<K, V> {
    /**
     * Retrieves {@linkplain Entry} for the given key
     * @param key The key to retrieve {@linkplain Entry} instance
     * @return Returns the retrieved {@linkplain Entry} instance or NULL
     */
    Entry<V> retrieve(K key);
    /**
     * Stores the {@linkplain Entry} together with its key
     * @param key The key to be used
     * @param value The value to be used
     */
    void store(K key, Entry<V> value);

    /**
     * The default (NULL-value) instance
     */
    final public static Persistor DEFAULT = new Persistor() {

        @Override
        public Entry retrieve(Object key) {
            // do nothing
            return null;
        }

        @Override
        public void store(Object key, Entry value) {
            // do nothing
        }

    };
}
