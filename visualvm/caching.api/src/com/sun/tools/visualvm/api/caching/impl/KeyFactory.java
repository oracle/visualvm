package com.sun.tools.visualvm.api.caching.impl;

import java.lang.ref.Reference;

interface KeyFactory<K> {

    Reference<K> createKey(K obj);
    static final KeyFactory DEFAULT = new KeyFactory() {

        @Override
        public Reference createKey(Object obj) {
            return new SoftReferenceEx(obj);
        }
    };
}
