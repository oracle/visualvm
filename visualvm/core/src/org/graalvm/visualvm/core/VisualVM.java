/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.core;

import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class VisualVM {
    
    private static final VisualVM INSTANCE = new VisualVM();
    
    
    private final int PARALLEL_PROCESSOR_THROUGHPUT_DEFAULT = 10;
    private final int PARALLEL_PROCESSOR_THROUGHPUT = Integer.getInteger("org.graalvm.visualvm.core.parallelProcessorThroughput", // NOI18N
                                                                         PARALLEL_PROCESSOR_THROUGHPUT_DEFAULT);
    
    
    private final RequestProcessor parallelProcessor;
    private final RequestProcessor sequentialProcessor;
    
    
    private VisualVM() {
        parallelProcessor = new RequestProcessor("VisualVM Parallel RequestProcessor", PARALLEL_PROCESSOR_THROUGHPUT); // NOI18N
        sequentialProcessor = new RequestProcessor("VisualVM Sequential RequestProcessor"); // NOI18N
    }
    
    
    public static VisualVM getInstance() { return INSTANCE; }
    
    
    public final void runTask(Runnable task) {
        parallelProcessor.post(task);
    }
    
    public final void runSequentialTask(Runnable task) {
        sequentialProcessor.post(task);
    }
    
}
