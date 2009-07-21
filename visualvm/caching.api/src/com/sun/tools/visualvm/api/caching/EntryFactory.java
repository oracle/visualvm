/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.api.caching;

/**
 * @author Jaroslav Bachorik
 */
public interface EntryFactory<K,V> {
    /**
     * This method will map the given key value to an entry value
     * It can return NULL if it's not possible to map the given key to
     * an instance of {@linkplain Entry}
     * @param key The key to create entry from
     * @return Returns the resolved object or NULL
     **/
    Entry<V> createEntry(K key);

    final public static EntryFactory DEFAULT = new EntryFactory() {

        @Override
        public Entry createEntry(Object key) {
            return new Entry(key);
        }
    };
}
