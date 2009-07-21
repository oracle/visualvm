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
public class SoftKeyFactory<K> implements KeyFactory<K> {

    @Override
    public Reference<K> createKey(K obj) {
        return new SoftReferenceEx<K>(obj);
    }

}
