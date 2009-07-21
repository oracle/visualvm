/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.api.caching;

import com.sun.tools.visualvm.api.caching.impl.CacheFactoryImpl;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Bachorik
 */
final public class CacheFactory {
    final private static class Singleton {
        final private static CacheFactory INSTANCE = new CacheFactory();
    }
    final private CacheFactoryImpl delegate;

    private CacheFactory() {
        delegate = Lookup.getDefault().lookup(CacheFactoryImpl.class);
    }

    public static CacheFactory getInstance() {
        return Singleton.INSTANCE;
    }

    public <K,V> Cache<K,V> weakMapCache() {
        return delegate.weakMapCache();
    }
    public <K,V> Cache<K,V> weakMapCache(EntryFactory<K,V> resolver, Persistor<K,V> persistor) {
        return delegate.weakMapCache(resolver, persistor);
    }
    public <K,V> Cache<K,V> weakMapCache(Persistor<K,V> persistor) {
        return delegate.weakMapCache(persistor);
    }
    public <K,V> Cache<K,V> weakMapCache(EntryFactory<K,V> resolver) {
        return delegate.weakMapCache(resolver);
    }

    public <K,V> Cache<K,V> softMapCache() {
        return delegate.softMapCache();
    }
    public <K,V> Cache<K,V> softMapCache(EntryFactory<K,V> resolver, Persistor<K,V> persistor) {
        return delegate.softMapCache(resolver, persistor);
    }
    public <K,V> Cache<K,V> softMapCache(Persistor<K,V> persistor) {
        return delegate.softMapCache(persistor);
    }
    public <K,V> Cache<K,V> softMapCache(EntryFactory<K,V> resolver) {
        return delegate.softMapCache(resolver);
    }
}
