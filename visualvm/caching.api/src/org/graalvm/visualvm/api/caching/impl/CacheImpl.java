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

package org.graalvm.visualvm.api.caching.impl;


import org.graalvm.visualvm.api.caching.Cache;
import org.graalvm.visualvm.api.caching.Entry;
import org.graalvm.visualvm.api.caching.EntryFactory;
import org.graalvm.visualvm.api.caching.Persistor;
import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;

/**
 * Default class implementation - should be used as a base for caching functionality
 * @author Jaroslav Bachorik
 */
final class CacheImpl<K, V> extends Cache<K,V> {
    final private Map<Reference<K>, Entry<V>> objectCache = new HashMap<Reference<K>, Entry<V>>();

    private long update_interval = 60480000; // 7 days in milliseconds

    private Persistor<K, V> persistor = Persistor.DEFAULT;
    private KeyFactory<K> keyFactory = KeyFactory.DEFAULT;
    private EntryFactory<K,V> resolver = EntryFactory.DEFAULT;

    CacheImpl() {};

    CacheImpl(EntryFactory<K,V> resolver) {
        this.resolver = resolver;
    }

    CacheImpl(Persistor<K,V> persistor) {
        this.persistor = persistor;
    }

    CacheImpl(KeyFactory<K> keyFactory) {
        this.keyFactory = keyFactory;
    }

    CacheImpl(EntryFactory<K,V> resolver, Persistor<K,V> persistor) {
        this.resolver = resolver;
        this.persistor = persistor;
    }

    CacheImpl(EntryFactory<K,V> resolver, KeyFactory<K> keyFactory) {
        this.resolver = resolver;
        this.keyFactory = keyFactory;
    }

    CacheImpl(KeyFactory<K> keyFactory, Persistor<K,V> persistor) {
        this.persistor = persistor;
        this.keyFactory = keyFactory;
    }

    CacheImpl(EntryFactory<K,V> resolver, KeyFactory<K> keyFactory, Persistor<K,V> persistor) {
        this.resolver = resolver;
        this.persistor = persistor;
        this.keyFactory = keyFactory;
    }

    /**
     * Retrieves an object from the cache by the given key
     * <p>
     * If there is no cached version then a registered instance of {@linkplain EntryFactory}
     * is used to invoke its {@linkplain EntryFactory#createEntry(java.lang.Object)} method.<br/>
     * Also, a {@linkplain Persistor} instance is used to retrieve and store the cached value in
     * a dedicated storage.
     * </p>
     * @param key The key identifying the object to be retrieved
     * @return Returns the cached object or NULL
     */
    @Override
    final public V retrieveObject(K key) {
        Reference<K> softKey = keyFactory.createKey(key);
        synchronized(objectCache) {
            Entry<V> entry = objectCache.get(softKey);
            if (entry == null) {
                entry = persistor.retrieve(key);
            }
            if (entry == null) {
                entry = cacheMiss(key);
                if (entry != null && entry.getContent() != null) {
                    persistor.store(key, entry);
                    objectCache.put(softKey, entry);
                }
            } else {
                long timestamp = System.currentTimeMillis();
                if ((timestamp - entry.getUpdateTimeStamp()) > update_interval) {
                    Entry<V> newEntry = cacheMiss(key);
                    if (newEntry != null && newEntry.getContent() != null) {
                        persistor.store(key, entry);
                        objectCache.put(softKey, newEntry);
                        entry = newEntry;
                    }
                }
            }
            return entry != null ? entry.getContent() : null;
        }
    }
    
    @Override
    final public V invalidateObject(K key) {
        Reference<K> softKey = keyFactory.createKey(key);
        synchronized(objectCache) {
            Entry<V> entry = objectCache.remove(softKey);
            return entry != null ? entry.getContent() : null;
        }
    }

    /**
     * Property getter
     * @return Returns TTL interval in milliseconds
     */
    @Override
    final public long getTTL() {
        return update_interval;
    }

    /**
     * Property setter
     * @param ttl TTL interval in milliseconds
     */
    @Override
    final public void setTTL(long ttl) {
        this.update_interval = ttl;
    }

    /**
     * This method is called in case of cache-miss
     * It can return NULL if it's not possible to resolve the missing instance
     * @param key The key of the missing object
     * @return Returns the resolved object or NULL
     */
    private Entry<V> cacheMiss(K key) {
        return resolver.createEntry(key);
    }
}
