/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.jfluid.utils;


/**
 * A Vector of ints. Implements a subset of standard java.util.Vector class
 *
 * @author Misha Dmitriev
 */
public class IntVector {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int[] vec;
    private int size;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public IntVector() {
        this(10);
    }

    public IntVector(int capacity) {
        vec = new int[capacity];
        size = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void add(int val) {
        if (size == vec.length) {
            int[] oldVec = vec;
            vec = new int[oldVec.length * 2];
            System.arraycopy(oldVec, 0, vec, 0, oldVec.length);
        }

        vec[size++] = val;
    }

    public void clear() {
        size = 0;
    }

    public int get(int idx) {
        return vec[idx];
    }

    public int size() {
        return size;
    }
}
