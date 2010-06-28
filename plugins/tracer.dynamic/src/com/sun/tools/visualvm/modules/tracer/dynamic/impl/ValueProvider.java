/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.impl;

/**
 *
 * @author jb198685
 */
public interface ValueProvider {
    long getValue(long timestamp);

    final public static ValueProvider NULL = new ValueProvider() {
        @Override
        public long getValue(long timestamp) {
            return 0L; // 0 value
        }
    };
}
