/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapFragment {
    
    protected final String id;
    protected final String name;
    protected final String description;

    protected final Heap heap;


    public HeapFragment(String id, String name, String description, Heap heap) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.heap = heap;
    }


    public String getID() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public Heap getHeap() { return heap; }

    public static abstract class Provider {
    
        public abstract List<HeapFragment> getFragments(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException;

    }
    
}
