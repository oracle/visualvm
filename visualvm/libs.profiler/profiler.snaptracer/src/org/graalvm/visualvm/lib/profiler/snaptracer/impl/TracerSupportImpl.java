/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.graalvm.visualvm.lib.profiler.snaptracer.Positionable;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerPackage;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerPackageProvider;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.packages.TestPackageProvider;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TracerSupportImpl {

    private static TracerSupportImpl INSTANCE;
    private static RequestProcessor PROCESSOR;

    private final Set<TracerPackageProvider> providers;


    public static synchronized TracerSupportImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TracerSupportImpl();
            PROCESSOR = new RequestProcessor("Tracer Processor", 5); // NOI18N
        }
        return INSTANCE;
    }


    public synchronized void registerPackageProvider(TracerPackageProvider provider) {
        providers.add(provider);
    }

    public synchronized void unregisterPackageProvider(TracerPackageProvider provider) {
        providers.remove(provider);
    }


    public synchronized boolean hasPackages(Object target) {
        for (TracerPackageProvider provider : providers)
            if (provider.getScope().isInstance(target))
                return true;
        return false;
    }

    public synchronized List<TracerPackage> getPackages(IdeSnapshot snapshot) {
        List<TracerPackage> packages = new ArrayList();
        for (TracerPackageProvider provider : providers)
            packages.addAll(Arrays.asList(provider.getPackages(snapshot)));
        Collections.sort(packages, Positionable.COMPARATOR);
        return packages;
    }
    
    
    public void perform(Runnable task) {
        PROCESSOR.post(task);
    }


    private TracerSupportImpl() {
        providers = new HashSet();
        registerPackageProvider(new TestPackageProvider());
    }

}
