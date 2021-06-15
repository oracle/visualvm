/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.oql.engine.api.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * Provides an iterator over instances of <I> using instances of <T> for traversal
 * @author Jaroslav Bachorik
 */
abstract public class TreeIterator<I, T> implements Iterator<I> {
    private Stack<T> toInspect = new Stack<T>();
    private Set<T> inspected = new HashSet<T>();

    private T popped = null;
    private Iterator<I> inspecting = null;

    public TreeIterator(T root) {
        toInspect.push(root);
        inspected.add(root);
    }

    public boolean hasNext() {
        setupIterator();
        return inspecting != null && inspecting.hasNext();
    }

    public I next() {
        setupIterator();

        if (inspecting == null || !inspecting.hasNext()) {
            throw new NoSuchElementException();
        }

        I retVal = inspecting.next();
        return retVal;
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    abstract protected Iterator<I> getSameLevelIterator(T popped);
    abstract protected Iterator<T> getTraversingIterator(T popped);

    private void setupIterator() {
        while (!toInspect.isEmpty() && (inspecting == null || !inspecting.hasNext())) {
            popped = toInspect.pop();
            if (popped != null) {
                inspecting = getSameLevelIterator(popped);
                Iterator<T> recurseIter = getTraversingIterator(popped);
                while (recurseIter.hasNext()) {
                    T inspectNext = recurseIter.next();
                    if (inspectNext == null) continue;
                    if (!inspected.contains(inspectNext)) {
                        toInspect.push(inspectNext);
                        inspected.add(inspectNext);
                    }
                }
            } else {
                inspecting = null;
            }
        }
    }
}
