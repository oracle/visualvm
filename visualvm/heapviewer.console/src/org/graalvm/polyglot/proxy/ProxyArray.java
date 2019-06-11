/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graalvm.polyglot.proxy;

import org.graalvm.polyglot.Value;

/**
 *
 * @author thurka
 */
public interface ProxyArray {

    public Object get(long index);

    public void set(long index, Value value);

    public long getSize();
}
