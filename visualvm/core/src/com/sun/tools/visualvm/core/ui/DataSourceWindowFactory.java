/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 * Class responsible for creating UIs (windows, TopComponents) for DataSources.
 *
 * @author Jiri Sedlacek
 */
// TODO: synchronize!!!
public final class DataSourceWindowFactory {

    private static DataSourceWindowFactory sharedInstance;

    // TODO: implement some better data structure for cheaper providers query
    private final Map<DataSourceViewsProvider, Class<? extends DataSource>> providers = Collections.synchronizedMap(new HashMap());
    
    
    /**
     * Returns singleton instance of DataSourceWindowFactory.
     * 
     * @return singleton instance of DataSourceWindowFactory.
     */
    public static synchronized DataSourceWindowFactory sharedInstance() {
        if (sharedInstance == null) sharedInstance = new DataSourceWindowFactory();
        return sharedInstance;
    }
    
    
    /**
     * Registers new DataSourceViewProvider for given DataSource scope.
     * 
     * @param provider DataSourceViewProvider to be added,
     * @param scope scope of DataSource types for which the provider provides views.
     */
    public void addViewProvider(DataSourceViewsProvider provider, Class<? extends DataSource> scope) {
        providers.put(provider, scope);
    }
    
    /**
     * Unregisters DataSourceViewProvider.
     * 
     * @param provider DataSourceViewProvider to be removed.
     */
    public void removeViewProvider(DataSourceViewsProvider provider) {
        providers.remove(provider);
    }
    
    /**
     * Returns true if there is at least one provider providing at least one view for given DataSource, false otherwise.
     * 
     * @param dataSource DataSource to create Window for.
     * @return true if there is at least one provider providing at least one view for given DataSource, false otherwise.
     */
    public boolean canCreateWindowFor(DataSource dataSource) {
        Set<DataSourceViewsProvider> compatibleProviders = getCompatibleProviders(dataSource);
        if (compatibleProviders.isEmpty()) return false;
        for (DataSourceViewsProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewFor(dataSource)) return true;
        return false;
    }
    
    
    DataSourceWindow createWindowFor(final DataSource dataSource) {
        // Create window for the DataSource
        final DataSourceWindow window = new DataSourceWindow(dataSource);

        // Resolve all views to be displayed
        final List<? extends DataSourceView> views = getViews(dataSource);

        // Blocking notification that the view will be added
        for (DataSourceView view : views) view.willBeAdded();

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // Blocking adding of views to the window
                    for (DataSourceView view : views) window.addView(view);

                    // Decorate the window according to the DataSource
                    DataSourceDescriptor descriptor = DataSourceDescriptorFactory.getDescriptor(dataSource);
                    window.setName(descriptor.getName());
                    window.setIcon(descriptor.getIcon());
                }
            });
        } catch (Exception e) {}

        // Blocking notification that the view has been added
        for (DataSourceView view : views) view.added();

        // Return window with all views
        return window;
    }
    
    List<? extends DataSourceView> getViews(DataSource dataSource) {
        List<DataSourceView> views = new ArrayList();
        Set<DataSourceViewsProvider> compatibleProviders = getCompatibleProviders(dataSource);
        for (DataSourceViewsProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewFor(dataSource))
                views.addAll(compatibleProvider.getViews(dataSource));
        Collections.sort(views, Positionable.COMPARATOR);
        return views;
    }
    
    private Set<DataSourceViewsProvider> getCompatibleProviders(DataSource dataSource) {
        Set<DataSourceViewsProvider> compatibleProviders = new HashSet();
        Set<DataSourceViewsProvider> providersSet = providers.keySet();
        for (DataSourceViewsProvider provider : providersSet)
            if (providers.get(provider).isInstance(dataSource))
                compatibleProviders.add(provider);
        return compatibleProviders;
    }
    
    
    private DataSourceWindowFactory() {
    }

}
