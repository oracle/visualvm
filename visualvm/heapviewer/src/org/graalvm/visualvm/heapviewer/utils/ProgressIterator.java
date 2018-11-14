/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.utils;

import java.util.Iterator;
import org.graalvm.visualvm.heapviewer.model.Progress;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProgressIterator<T> implements Iterator<T> {
    
    private final Iterator<T> iterator;
    private final Progress progress;
    
    
    public ProgressIterator(Iterator<T> iterator, Progress progress) {
        this(iterator, 0, false, progress);
    }
    
    public ProgressIterator(Iterator<T> iterator, int index, boolean needsForward, Progress progress) {
        this.iterator = iterator;
        this.progress = progress;
        
        if (index > 0) {
            if (needsForward) forward(index);
            else progress.steps(index);
        }
    }
    
    
    public boolean hasNext() {
        return iterator.hasNext();
    }

    public T next() {
        progress.step();
        return iterator.next();
    }
    
    
    private void forward(int steps) {
        while (steps-- > 0)
            if (hasNext()) next();
    }
    
}
