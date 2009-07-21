/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.api.caching.impl;


import com.sun.tools.visualvm.api.caching.Cache;
import com.sun.tools.visualvm.api.caching.EntryFactory;
import com.sun.tools.visualvm.api.caching.Persistor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jaroslav Bachorik
 */
@ServiceProvider(service=CacheFactoryImpl.class)
public class CacheFactoryImpl {
    public <K,V> Cache<K,V> weakMapCache() {
        return new CacheImpl<K, V>(new WeakKeyFactory<K>());
    }
    public <K,V> Cache<K,V> weakMapCache(EntryFactory<K,V> resolver, Persistor<K,V> persistor) {
        return new CacheImpl<K, V>(resolver, new WeakKeyFactory<K>(), persistor);
    }
    public <K,V> Cache<K,V> weakMapCache(Persistor<K,V> persistor) {
        return new CacheImpl<K, V>(new WeakKeyFactory<K>(), persistor);
    }
    public <K,V> Cache<K,V> weakMapCache(EntryFactory<K,V> resolver) {
        return new CacheImpl<K, V>(resolver, new WeakKeyFactory<K>());
    }

    public <K,V> Cache<K,V> softMapCache() {
        return new CacheImpl<K, V>(new SoftKeyFactory<K>());
    }
    public <K,V> Cache<K,V> softMapCache(EntryFactory<K,V> resolver, Persistor<K,V> persistor) {
        return new CacheImpl<K, V>(resolver, new SoftKeyFactory<K>(), persistor);
    }
    public <K,V> Cache<K,V> softMapCache(Persistor<K,V> persistor) {
        return new CacheImpl<K, V>(new SoftKeyFactory<K>(), persistor);
    }
    public <K,V> Cache<K,V> softMapCache(EntryFactory<K,V> resolver) {
        return new CacheImpl<K, V>(resolver, new SoftKeyFactory<K>());
    }
}
