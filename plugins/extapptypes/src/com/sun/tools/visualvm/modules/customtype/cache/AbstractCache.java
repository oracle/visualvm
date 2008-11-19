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

package com.sun.tools.visualvm.modules.customtype.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract cache manager - should be used as a base for caching functionality
 * @author Jaroslav Bachorik
 */
abstract public class AbstractCache<K, V> {
    final private Map<K, Entry<V>> objectCache = new HashMap<K, Entry<V>>();

    private long update_interval = 60480000; // 7 days in milliseconds

    private Persistor<K, V> persistor = Persistor.DEFAULT;

    /**
     * Retrieves an object from the cache by the given key
     * If there is no cached version of the object {@linkplain #cacheMiss(java.lang.Object) } is invoked
     * @param key The key identifying the object to be retrieved
     * @return Returns the cached object or NULL
     */
    public V retrieveObject(K key) {
        synchronized(objectCache) {
            Entry<V> entry = objectCache.get(key);
            if (entry == null) {
                entry = persistor.retrieve(key);
            }
            if (entry == null) {
                entry = cacheMiss(key);
                if (entry != null && entry.getContent() != null) {
                    persistor.store(key, entry);
                    objectCache.put(key, entry);
                }
            } else {
                long timestamp = System.currentTimeMillis();
                if ((timestamp - entry.getUpdateTimeStamp()) > update_interval) {
                    Entry<V> newEntry = cacheMiss(key);
                    if (newEntry != null && newEntry.getContent() != null) {
                        persistor.store(key, entry);
                        objectCache.put(key, newEntry);
                        entry = newEntry;
                    }
                }
            }
            return entry != null ? entry.getContent() : null;
        }
    }
    
    public V invalidateObject(K key) {
        synchronized(objectCache) {
            Entry<V> entry = objectCache.remove(key);
            return entry != null ? entry.getContent() : null;
        }
    }
    /**
     * Property getter
     * @return Returns {@linkplain Persistor} instance used by the cache
     */
    public Persistor<K, V> getPersistor() {
        return persistor;
    }

    /**
     * Property setter
     * @param persistor Sets the {@linkplain Persistor} instance to be used
     */
    public void setPersistor(Persistor<K, V> persistor) {
        this.persistor = persistor;
    }

    /**
     * Property getter
     * @return Returns TTL interval in milliseconds
     */
    public long getTTL() {
        return update_interval;
    }

    /**
     * Property setter
     * @param ttl TTL interval in milliseconds
     */
    public void setTTL(long ttl) {
        this.update_interval = ttl;
    }

    /**
     * This method is called in case of cache-miss
     * The subclass should take care of resolving the missing instance
     * It can return NULL if it's not possible to resolve the missing instance
     * @param key The key of the missing object
     * @return Returns the resolved object or NULL
     */
    abstract protected Entry<V> cacheMiss(K key);
}
