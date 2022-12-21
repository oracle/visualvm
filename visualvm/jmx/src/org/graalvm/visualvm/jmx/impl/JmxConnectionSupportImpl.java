/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jmx.impl;

import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.jmx.EnvironmentProvider;
import org.graalvm.visualvm.jmx.JmxConnectionCustomizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JmxConnectionSupportImpl {

    private static final Map<String, EnvironmentProvider> providers =
            Collections.synchronizedMap(new HashMap<>());

    private static final Map<String, JmxConnectionCustomizer> customizers =
            Collections.synchronizedMap(new HashMap<>());

    private static JmxConnectionCustomizer defaultCustomizer;
    private static boolean defaultHidden = false;


    // --- EnvironmentProvider stuff -------------------------------------------

    public static void registerProviderImpl(EnvironmentProvider provider) {
        String providerId = provider.getId();
        if (providers.containsKey(providerId))
            throw new UnsupportedOperationException("Provider with id '" + providerId + // NOI18N
                                                    "' already registered"); // NOI18N
        providers.put(providerId, provider);
    }

    public static void unregisterProviderImpl(EnvironmentProvider provider) {
        providers.remove(provider.getId());
    }

    public static EnvironmentProvider getProvider(String providerId) {
        return providers.get(providerId);
    }


    // --- JmxConnectionCustomizer stuff ---------------------------------------

    public static void registerCustomizer(JmxConnectionCustomizer customizer) {
        String customizerId = customizer.getId();
        if (customizers.containsKey(customizerId))
            throw new UnsupportedOperationException("Customizer with id '" + customizerId + // NOI18N
                                                    "' already registered"); // NOI18N
        customizers.put(customizerId, customizer);
        defaultHidden = defaultHidden || customizer.hidesDefault();
    }

    public static void unregisterCustomizer(JmxConnectionCustomizer customizer) {
        customizers.remove(customizer.getId());
        if (customizer.hidesDefault()) updateDefaultHidden();
    }

    public static JmxConnectionCustomizer getCustomizer(String customizerId) {
        return customizers.get(customizerId);
    }

    public static List<JmxConnectionCustomizer> getCustomizers() {
        List<JmxConnectionCustomizer> list = customizers();
        if (defaultHidden && defaultCustomizer != null)
            list.remove(defaultCustomizer);
        Collections.sort(list, Positionable.COMPARATOR);
        return list;
    }

    public static void setDefaultCustomizer(JmxConnectionCustomizer customizer) {
        if (defaultCustomizer != null)
            throw new UnsupportedOperationException("Default customizer already set"); // NOI18N
        defaultCustomizer = customizer;
    }
    
    static JmxConnectionCustomizer getDefaultCustomizer() {
        return defaultCustomizer;
    }


    private static List<JmxConnectionCustomizer> customizers() {
        List<JmxConnectionCustomizer> list = new ArrayList<>();
        synchronized(customizers) { list.addAll(customizers.values()); }
        return list;
    }

    private static void updateDefaultHidden() {
        boolean newDefaultHidden = false;
        List<JmxConnectionCustomizer> list = customizers();
        for (JmxConnectionCustomizer customizer : list)
            if (customizer.hidesDefault()) {
                newDefaultHidden = true;
                break;
            }
        defaultHidden = newDefaultHidden;
    }

}
