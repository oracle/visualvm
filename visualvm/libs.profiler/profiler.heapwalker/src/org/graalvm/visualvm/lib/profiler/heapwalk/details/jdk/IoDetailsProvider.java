/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public final class IoDetailsProvider extends DetailsProvider.Basic {

    private static final String FILE_MASK = "java.io.File+";                    // NOI18N
    private static final String ZIPFILE_MASK = "java.util.zip.ZipFile+";        // NOI18N
    private static final String RAF_MASK = "java.io.RandomAccessFile";          // NOI18N
    private static final String FIS_MASK = "java.io.FileInputStream";           // NOI18N
    private static final String FOS_MASK = "java.io.FileOutputStream";          // NOI18N
    private static final String FD_MASK = "java.io.FileDescriptor";             // NOI18N
    private static final String FD_RAF_CLASS = "java.io.RandomAccessFile";      // NOI18N
    private static final String FD_FIS_CLASS = "java.io.FileInputStream";       // NOI18N
    private static final String FD_FOS_CLASS = "java.io.FileOutputStream";      // NOI18N
    private static final String FCI_MASK = "sun.nio.ch.FileChannelImpl";        // NOI18N
    private static final String HEAPCHARBUFFER_MASK = "java.nio.HeapCharBuffer";// NOI18N

    private static final Object CACHE_LOCK = new Object();
    private static WeakHashMap<Heap,Map<Long,String>> CACHE;

    public IoDetailsProvider() {
        super(FILE_MASK, ZIPFILE_MASK, RAF_MASK, FIS_MASK, FOS_MASK, FD_MASK, FCI_MASK,
              HEAPCHARBUFFER_MASK);
    }
    
    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case FILE_MASK: // File+
                return DetailsUtils.getInstanceFieldString(instance, "path"); // NOI18N
            case ZIPFILE_MASK: // ZipFile+
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case RAF_MASK: // RandomAccessFile
                return DetailsUtils.getInstanceFieldString(instance, "path"); // NOI18N
            case FIS_MASK: // FileInputStrea
                return DetailsUtils.getInstanceFieldString(instance, "path"); // NOI18N
            case FOS_MASK: // FileOutputStream
                return DetailsUtils.getInstanceFieldString(instance, "path"); // NOI18N
            case FD_MASK: // FileDescriptor
                synchronized (CACHE_LOCK) {
                    if (CACHE == null) {
                        CACHE = new WeakHashMap();
                    }
                    Heap heap = instance.getJavaClass().getHeap();
                    Map<Long,String> heapCache = CACHE.get(heap);
                    if (heapCache == null) {
                        heapCache = computeFDCache(heap, instance.getJavaClass());
                        CACHE.put(heap, heapCache);
                    }
                    return heapCache.get(instance.getInstanceId());
                }
            case FCI_MASK: // FileChannelImpl
                return DetailsUtils.getInstanceFieldString(instance, "path"); // NOI18N
            case HEAPCHARBUFFER_MASK: {
                int position = DetailsUtils.getIntFieldValue(instance, "position", -1); // NOI18N                                 // NOI18N
                int limit = DetailsUtils.getIntFieldValue(instance, "limit", -1);       // NOI18N                // NOI18N
                int offset = DetailsUtils.getIntFieldValue(instance, "offset", -1);       // NOI18N                // NOI18N
                return DetailsUtils.getPrimitiveArrayFieldString(instance, "hb", position + offset, limit - position, null, "...");
            }
            default:
                break;
        }
        
        return null;
    }

    private Map<Long, String> computeFDCache(Heap heap, JavaClass fdClass) {
        Map<Long, String> cache = new HashMap();
        computeFDCacheForClass(heap, FD_RAF_CLASS, "fd", cache);                // NOI18N
        computeFDCacheForClass(heap, FD_FIS_CLASS, "fd", cache);                // NOi18N
        computeFDCacheForClass(heap, FD_FOS_CLASS, "fd", cache);                // NOI18N
        computeStdDescriptor(fdClass, "in", "Standard Input", cache);           // NOI18N
        computeStdDescriptor(fdClass, "out", "Standard Output", cache);         // NOi18N
        computeStdDescriptor(fdClass, "err", "Standard Error", cache);          // NOi18N
        return cache;
    }

    private void computeFDCacheForClass(Heap heap, String className, String fieldName, Map<Long, String> cache) {
        JavaClass rafClass = heap.getJavaClassByName(className);
        if (rafClass != null) {
            for (Instance raf : rafClass.getInstances()) {
                Instance fd = (Instance)raf.getValueOfField(fieldName);
                if (fd != null) {
                    String details = getDetailsString(className,raf);
                    if (details != null) {
                        cache.put(fd.getInstanceId(), details);
                    }
                }
            }
        }
    }

    private void computeStdDescriptor(JavaClass fdClass, String field, String text, Map<Long, String> cache) {
        Instance stdFd = (Instance) fdClass.getValueOfStaticField(field);
        
        if (stdFd != null) {
            cache.put(stdFd.getInstanceId(), text);
        }
    }
    
}
