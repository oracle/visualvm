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

package org.graalvm.visualvm.core.properties;

import org.graalvm.visualvm.core.datasource.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entrypoint for customization of DataSource properties. PropertiesProvider
 * instances should be registered/unregistered using the registerPropertiesProvider
 * and unregisterPropertiesProvider methods.
 * 
 * DataSource providers displaying a dialog supporting user-customizable properties
 * should use getCustomizer(Class) method to get the properties UI.
 *
 * @author Jiri Sedlacek
 */
public final class PropertiesSupport {

    private static PropertiesSupport INSTANCE;

    private final Map<PropertiesProvider, Class<? extends DataSource>> providers =
            Collections.synchronizedMap(new HashMap<>());


    /**
     * Returns singleton instance of PropertiesSupport.
     *
     * @return singleton instance of PropertiesSupport
     */
    public static synchronized PropertiesSupport sharedInstance() {
        if (INSTANCE == null) INSTANCE = new PropertiesSupport();
        return INSTANCE;
    }


    /**
     * Registers a PropertiesProvider for a concrete DataSource type.
     *
     * @param <X> any DataSource type
     * @param provider PropertiesProvider to be registered
     * @param scope type of DataSource supported by the PropertiesProvider
     */
    public <X extends DataSource> void registerPropertiesProvider(
                                  PropertiesProvider<X> provider, Class<X> scope) {
        providers.put(provider, scope);
    }

    /**
     * Unregisters the PropertiesProvider.
     *
     * @param <X> any DataSource type
     * @param provider PropertiesProvider to unregister
     */
    public <X extends DataSource> void unregisterPropertiesProvider(
                                  PropertiesProvider<X> provider) {
        providers.remove(provider);
    }


    /**
     * Returns true if there's at least one PropertiesProvider providing properties
     * for the DataSource type, false otherwise.
     *
     * @param <X> any DataSource type
     * @param type type of the DataSource to be checked
     * @return true if there's at least one PropertiesProvider providing properties for the DataSource type, false otherwise
     */
    public <X extends DataSource> boolean hasProperties(Class<X> type) {
        return hasProperties(null, type);
    }

    /**
     * Returns true if there's at least one PropertiesProvider providing properties
     * for the DataSource, false otherwise.
     *
     * @param <X> any DataSource type
     * @param dataSource DataSource to be checked
     * @return true if there's at least one PropertiesProvider providing properties for the DataSource, false otherwise
     */
    public <X extends DataSource> boolean hasProperties(X dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("DataSource cannot be null"); // NOI18N
        return hasProperties(dataSource, (Class<X>)dataSource.getClass());
    }

    /**
     * Returns an UI component to display user-customizable properties for the
     * provided DataSource type. Use hasProperties(Class) method to check if there
     * are any customizable properties for the given DataSource type. For no
     * properties available the created PropertiesCustomizer is empty.
     *
     * @param <X> any DataSource type
     * @param type type of the DataSource to customize
     * @return UI component to display user-customizable properties for the provided DataSource type
     */
    public <X extends DataSource> PropertiesCustomizer<X> getCustomizer(Class<X> type) {
        return getCustomizer(null, type);
    }

    /**
     * Opens Properites window of the DataSource. The first category is selected.
     *
     * @param dataSource DataSource for which to open the Properties window
     */
    public void openProperties(DataSource dataSource) {
        openProperties(dataSource, PropertiesProvider.CATEGORY_GENERAL);
    }

    /**
     * Opens Properites window of the DataSource and selects the provided category
     * if available.
     *
     * @param dataSource DataSource for which to open the Properties window
     */
    public void openProperties(DataSource dataSource, int propertiesCategory) {
        if (dataSource == null) throw new IllegalArgumentException("DataSource cannot be null"); // NOI18N
        PropertiesConfigurator.editProperties(dataSource, propertiesCategory);
    }


    <X extends DataSource> PropertiesCustomizer<X> getCustomizer(X dataSource, Class<X> type) {
        return new PropertiesCustomizer<>(dataSource, type);
    }


    <X extends DataSource> List<PropertiesProvider<X>> getProviders(X dataSource, Class<X> type) {
        Map<PropertiesProvider, Class<? extends DataSource>> providersCopy = new HashMap<>();
        synchronized(providers) { providersCopy.putAll(providers); }

        if (dataSource != null) type = (Class<X>)dataSource.getClass();

        List<PropertiesProvider<X>> compatibleProviders = new ArrayList<>();
        Set<Map.Entry<PropertiesProvider,Class<? extends DataSource>>> providersSet = providersCopy.entrySet();
        for (Map.Entry<PropertiesProvider,Class<? extends DataSource>> entry : providersSet) {
            PropertiesProvider provider = entry.getKey();
            if (entry.getValue().isAssignableFrom(type) &&
                provider.supportsDataSource(dataSource))
                    compatibleProviders.add(provider);
        }
        return compatibleProviders;
    }

    private <X extends DataSource> boolean hasProperties(X dataSource, Class<X> type) {
        return !getProviders(dataSource, type).isEmpty();
    }


    private PropertiesSupport() {}

}
