/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 *
 * @author Jiri Sedlacek
 */
public final class DetailsSupport {

    public static String getDetailsString(Instance instance) {
        // TODO [Tomas]: cache computed string per heap
        Collection<ProviderClassPair> pairs = getCompatibleProviders(instance.getJavaClass());
        for (ProviderClassPair pair : pairs) {
            String classKey = pair.classKey;
            if (pair.subclasses) classKey += "+";                               // NOI18N
            String string = pair.provider.getDetailsString(classKey, instance);
            if (string != null) return string;
        }
        return null;
    }

    public static DetailsProvider.View getDetailsView(Instance instance) {
        Collection<ProviderClassPair> pairs = getCompatibleProviders(instance.getJavaClass());
        for (ProviderClassPair pair : pairs) {
            String classKey = pair.classKey;
            if (pair.subclasses) classKey += "+";                               // NOI18N
            DetailsProvider.View view = pair.provider.getDetailsView(classKey, instance);
            if (view != null) return view;
        }
        return null;
    }


    private static final LinkedHashMap<Long, List<ProviderClassPair>> PROVIDERS_CACHE =
            new LinkedHashMap<Long, List<ProviderClassPair>>(10000) {
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > 5000;
                }
            };
    
    private static Lookup.Result<DetailsProvider> PROVIDERS;
    private static Collection<? extends DetailsProvider> getProviders() {
        if (PROVIDERS == null) {
            PROVIDERS = Lookup.getDefault().lookupResult(DetailsProvider.class);
            PROVIDERS.addLookupListener(new LookupListener() {
                public void resultChanged(LookupEvent ev) { PROVIDERS_CACHE.clear(); }
            });
        }
        return PROVIDERS.allInstances();
    }
    
    private static List<ProviderClassPair> getCompatibleProviders(JavaClass cls) {
        Long classId = cls.getJavaClassId();

        // Query the cache for already computed DetailsProviders
        List<ProviderClassPair> cachedPairs = PROVIDERS_CACHE.get(classId);
        if (cachedPairs != null) return cachedPairs;
        
        // All registered className|DetailsProvider pairs
        List<ProviderClassPair> allPairs = new ArrayList<>();
        List<ProviderClassPair> simplePairs = new ArrayList<>();
        Collection<? extends DetailsProvider> providers = getProviders();
        for (DetailsProvider provider : providers) {
            String[] classes = provider.getSupportedClasses();
            if (classes != null && classes.length > 0)
                for (String classs : classes)
                    allPairs.add(new ProviderClassPair(provider, classs));
            else simplePairs.add(new ProviderClassPair(provider, null));
        }
        
        List<ProviderClassPair> pairs = new ArrayList<>();
        
        // Only compatible className|DetailsProvider pairs
        if (!allPairs.isEmpty()) {
            boolean superClass = false;
            while (cls != null) {
                String clsName = cls.getName();
                for (ProviderClassPair pair : allPairs)
                    if ((pair.subclasses || !superClass) &&
                        clsName.equals(pair.classKey))
                        pairs.add(pair);
                cls = cls.getSuperClass();
                superClass = true;
            }
        }
        
        // DetailsProviders without className definitions
        pairs.addAll(simplePairs);
        
        // Cache the computed DetailsProviders
        PROVIDERS_CACHE.put(classId, pairs);
        
        return pairs;
    }
    
    
    private static class ProviderClassPair {
        
        final DetailsProvider provider;
        final String classKey;
        final boolean subclasses;
        
        ProviderClassPair(DetailsProvider provider, String classKey) {
            subclasses = classKey != null && classKey.endsWith("+");            // NOI18N
            this.provider = provider;
            this.classKey = !subclasses ? classKey :
                            classKey.substring(0, classKey.length() - 1);
        }
        
    }
    
}
