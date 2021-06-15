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

package org.graalvm.visualvm.lib.profiler.v2.impl;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class WeakProcessor {

    private Reference<RequestProcessor> processor;

    private final String name;
    private final int throughput;

    public WeakProcessor(String name) {
        this(name, 1);
    }

    public WeakProcessor(String name, int throughput) {
        this.name = name;
        this.throughput = throughput;
    }

    public void post(Runnable task) {
        processor().post(task);
    }

    public void post(Runnable task, int delay) {
        processor().post(task, delay);
    }

    private synchronized RequestProcessor processor() {
        RequestProcessor p = processor != null ? processor.get() : null;

        if (p == null) {
            p = new RequestProcessor(name, throughput);
            processor = new WeakReference(p);
        }

        return p;
    }

}
