/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.api.caching;

/**
 *
 * @author Jaroslav Bachorik
 */
abstract public class Cache<K,V> {
    /**
     * Retrieves an object from the cache by the given key<br/>
     * If there is no cached version of the object {@linkplain #cacheMiss(java.lang.Object) } is invoked
     * <p>
     * To obtain a cache one needs to call one of {@linkplain CacheFactory} methods
     * </p>
     * @param key The key identifying the object to be retrieved
     * @return Returns the cached object or NULL
     */
    abstract public V retrieveObject(K key);

    abstract public V invalidateObject(K key);

    /**
     * Property getter
     * @return Returns TTL interval in milliseconds
     */
    abstract public long getTTL();

    /**
     * Property setter
     * @param ttl TTL interval in milliseconds
     */
    abstract public void setTTL(long ttl);

}
