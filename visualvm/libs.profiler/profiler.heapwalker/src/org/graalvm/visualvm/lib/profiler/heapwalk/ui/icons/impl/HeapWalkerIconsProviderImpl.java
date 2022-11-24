/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.impl;

import java.util.Map;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.lib.profiler.spi.IconsProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=IconsProvider.class)
public final class HeapWalkerIconsProviderImpl extends IconsProvider.Basic {

    @Override
    protected final void initStaticImages(Map<String, String> cache) {
        cache.put(HeapWalkerIcons.CLASSES, "classes.png"); // NOI18N
        cache.put(HeapWalkerIcons.DATA, "data.png"); // NOI18N
        cache.put(HeapWalkerIcons.GC_ROOT, "gcRoot.png"); // NOI18N
        cache.put(HeapWalkerIcons.GC_ROOTS, "gcRoots.png"); // NOI18N
        cache.put(HeapWalkerIcons.INCOMING_REFERENCES, "incomingRef.png"); // NOI18N
        cache.put(HeapWalkerIcons.INSTANCES, "instances.png"); // NOI18N
        cache.put(HeapWalkerIcons.LOOP, "loop.png"); // NOI18N
        cache.put(HeapWalkerIcons.MEMORY_LINT, "memoryLint.png"); // NOI18N
        cache.put(HeapWalkerIcons.PROGRESS, "progress.png"); // NOI18N
        cache.put(HeapWalkerIcons.PROPERTIES, "properties.png"); // NOI18N
        cache.put(HeapWalkerIcons.RULES, "rules.png"); // NOI18N
        cache.put(HeapWalkerIcons.SAVED_OQL_QUERIES, "savedOQL.png"); // NOI18N
        cache.put(HeapWalkerIcons.STATIC, "static.png"); // NOI18N
        cache.put(HeapWalkerIcons.SYSTEM_INFO, "sysinfo.png"); // NOI18N
        cache.put(HeapWalkerIcons.WINDOW, "window.png"); // NOI18N
        cache.put(HeapWalkerIcons.BIGGEST_OBJECTS, "biggestObjects.png"); // NOI18N
        cache.put(HeapWalkerIcons.OQL_CONSOLE, "oqlConsole.png"); // NOI18N
    }

}
