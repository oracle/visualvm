/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.profiler.heapwalk.details.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 *
 * @author Jiri Sedlacek
 */
public final class DetailsSupport {
    
    public static String getDetailsString(Instance instance, Heap heap) {
        // TODO [Tomas]: cache computed string per heap
        Collection<ProviderClassPair> pairs = getCompatibleProviders(instance.getJavaClass());
        for (ProviderClassPair pair : pairs) {
            String classKey = pair.classKey;
            if (pair.subclasses) classKey += "+";                               // NOI18N
            String string = pair.provider.getDetailsString(classKey, instance, heap);
            if (string != null) return string;
        }
        return null;
    }
    
    public static DetailsProvider.View getDetailsView(Instance instance, Heap heap) {
        Collection<ProviderClassPair> pairs = getCompatibleProviders(instance.getJavaClass());
        for (ProviderClassPair pair : pairs) {
            String classKey = pair.classKey;
            if (pair.subclasses) classKey += "+";                               // NOI18N
            DetailsProvider.View view = pair.provider.getDetailsView(classKey, instance, heap);
            if (view != null) return view;
        }
        return null;
    }
    
    
    private static final LinkedHashMap<String, List<ProviderClassPair>> PROVIDERS_CACHE =
            new LinkedHashMap<String, List<ProviderClassPair>>(10000) {
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
        String className = cls.getName();

        // Query the cache for already computed DetailsProviders
        List<ProviderClassPair> cachedPairs = PROVIDERS_CACHE.get(className);
        if (cachedPairs != null) return cachedPairs;
        
        // All registered className|DetailsProvider pairs
        List<ProviderClassPair> allPairs = new ArrayList();
        List<ProviderClassPair> simplePairs = new ArrayList();
        Collection<? extends DetailsProvider> providers = getProviders();
        for (DetailsProvider provider : providers) {
            String[] classes = provider.getSupportedClasses();
            if (classes != null && classes.length > 0)
                for (String classs : classes)
                    allPairs.add(new ProviderClassPair(provider, classs));
            else simplePairs.add(new ProviderClassPair(provider, null));
        }
        
        List<ProviderClassPair> pairs = new ArrayList();
        
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
        PROVIDERS_CACHE.put(className, pairs);
        
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
