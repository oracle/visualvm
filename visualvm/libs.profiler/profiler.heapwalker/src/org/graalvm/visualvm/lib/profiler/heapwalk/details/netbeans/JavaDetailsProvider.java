/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.netbeans;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class JavaDetailsProvider extends DetailsProvider.Basic {

    private static final String FO_INDEXABLE = "org.netbeans.modules.parsing.impl.indexing.FileObjectIndexable"; // NOI18N
    private static final String INDEXABLE = "org.netbeans.modules.parsing.spi.indexing.Indexable"; // NOI18N
    private static final String CLASSPATH_ENTRY = "org.netbeans.api.java.classpath.ClassPath$Entry";    // NOI18N

    private long lastHeapId;
    private String lastSeparator;

    public JavaDetailsProvider() {
        super(FO_INDEXABLE,INDEXABLE,CLASSPATH_ENTRY);
    }

    @Override
    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case FO_INDEXABLE: {
                String root = DetailsUtils.getInstanceFieldString(instance, "root"); // NOI18N
                String relpath = DetailsUtils.getInstanceFieldString(instance, "relativePath"); // NOI18N
                if (root != null && relpath != null) {
                    Heap heap = instance.getJavaClass().getHeap();
                    return root.concat(getFileSeparator(heap)).concat(relpath);
                }
                break;
            }
            case INDEXABLE:
                return DetailsUtils.getInstanceFieldString(instance, "delegate"); // NOI18N
            case CLASSPATH_ENTRY:
                return DetailsUtils.getInstanceFieldString(instance, "url");  // NOI18N
            default:
                break;
        }
        return null;
    }

    private String getFileSeparator(Heap heap) {
        if (lastHeapId != System.identityHashCode(heap)) {
            lastSeparator = heap.getSystemProperties().getProperty("file.separator","/"); // NOI18N
            lastHeapId = System.identityHashCode(heap);
        }
        return lastSeparator;
    }
}
