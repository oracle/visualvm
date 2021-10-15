/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.Charset;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public final class NioDetailsProvider extends DetailsProvider.Basic {

    private static final String UNIXPATH_MASK = "sun.nio.fs.UnixPath"; // NOI18N
    private static final String WINDOWSPATH_MASK = "sun.nio.fs.WindowsPath"; // NOI18N

    private long lastHeapId;
    private Charset lastJnuEncoding;

    public NioDetailsProvider() {
        super(UNIXPATH_MASK, WINDOWSPATH_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        if (UNIXPATH_MASK.equals(className)) {
            String path = DetailsUtils.getInstanceFieldString(instance, "stringValue");   // NOI18N
            if (path != null) {
                return path;
            }
            Charset encoding = getJnuEncoding(instance.getJavaClass().getHeap());
            List<String> pathItems = DetailsUtils.getPrimitiveArrayFieldValues(instance, "path");                  // NOI18N
            byte[] pathArr = DetailsUtils.getByteArray(pathItems);
            if (pathArr != null) {
                return new String(pathArr, encoding);
            }
        } else if (WINDOWSPATH_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "path");   // NOI18N
        }
        return null;
    }

    private Charset getJnuEncoding(Heap heap) {
        if (lastHeapId != System.identityHashCode(heap)) {
            String encoding = heap.getSystemProperties().getProperty("sun.jnu.encoding", "UTF-8"); // NOI18N
            lastJnuEncoding = Charset.forName(encoding);
            lastHeapId = System.identityHashCode(heap);
        }
        return lastJnuEncoding;
    }

}
