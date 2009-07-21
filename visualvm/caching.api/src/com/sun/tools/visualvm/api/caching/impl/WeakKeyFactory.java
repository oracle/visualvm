/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.api.caching.impl;

import java.lang.ref.Reference;

/**
 *
 * @author Jaroslav Bachorik
 */
public class WeakKeyFactory<K> implements KeyFactory<K> {

    @Override
    public Reference<K> createKey(K obj) {
        return new WeakReferenceEx<K>(obj);
    }

}
