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
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Support class allowing customization of DataSourceViews by ViewPlugins.
 *
 * @author Jiri Sedlacek
 */
public abstract class PluggableViewSupport<A extends DataSource> {

    private Map<ViewPlugin<? extends A>, Class<? extends A>> plugins = Collections.synchronizedMap(new HashMap());


    /**
     * Registers new ViewPlugin.
     * 
     * @param plugin ViewPlugin to add.
     * @param scope scope of DataSources for whose views the plugin plugs into.
     */
    public <X extends A> void addPlugin(ViewPlugin<X> plugin, Class<X> scope) {
        plugins.put(plugin, scope);
    }

    /**
     * Unregisters ViewPlugin.
     * 
     * @param plugin ViewPlugin to remove.
     */
    public <X extends A> void removePlugin(ViewPlugin<X> plugin) {
        plugins.remove(plugin);
    }


    /**
     * Returns true if the pluggable view allows defining new areas for given DataSource instance at given location, false otherwise.
     * 
     * @param dataSource DataSource for which to define new area,
     * @param location location of new area.
     * @return true if the pluggable view allows defining new areas for given DataSource instance at given location, false otherwise.
     */
    public abstract <X extends A> boolean allowsNewArea(X dataSource, int location);

    /**
     * Returns true if the pluggable view allows adding new view for given DataSource at given location. false otherwise.
     * 
     * @param dataSource DataSource for which to add new view,
     * @param location location of new view.
     * @return true if the pluggable view allows adding new view for given DataSource at given location. false otherwise.
     */
    public abstract <X extends A> boolean allowsNewView(X dataSource, int location);
    
    
    // TODO: check generics
    /**
     * Customizes given view of given DataSource by registered ViewPlugins.
     * 
     * @param view DataViewComponent to customize, 
     * @param dataSource DataSource for which to customize the view.
     */
    protected <X extends A> void customizeView(DataViewComponent view, X dataSource) {
        
        List<ViewPlugin<X>> compatiblePlugins = getCompatiblePlugins(dataSource);
        
        for (ViewPlugin<X> plugin : compatiblePlugins) {
            Set<? extends ViewPlugin.AreaDescriptor> areaDescriptors = plugin.getAreasFor(dataSource);
            for (ViewPlugin.AreaDescriptor areaDescriptor : areaDescriptors)
                if (allowsNewArea(dataSource, areaDescriptor.getLocation())) view.configureDetailsArea(areaDescriptor.getArea(), areaDescriptor.getLocation());
                else throw new RuntimeException("View area at " + areaDescriptor.getLocation() + " does not allow customizations");
            
            Set<? extends ViewPlugin.ViewDescriptor> viewDescriptors = plugin.getViewsFor(dataSource);
            for (ViewPlugin.ViewDescriptor viewDescriptor : viewDescriptors)
                if (allowsNewView(dataSource, viewDescriptor.getLocation())) {
                    view.addDetailsView(viewDescriptor.getView(), viewDescriptor.getLocation(), viewDescriptor.getPreferredPosition());
                }
                else throw new RuntimeException("View area at " + viewDescriptor.getLocation() + " does not allow plugins");
        }
        
    }
    
    /**
     * Returns list of ViewPlugins registered to customize view of given DataSource.
     * 
     * @param dataSource DataSource for which to customize the view.
     * @return list of ViewPlugins registered to customize view of given DataSource.
     */
    protected <X extends A> List<ViewPlugin<X>> getCompatiblePlugins(X dataSource) {
        List<ViewPlugin<X>> compatiblePlugins = new ArrayList();
        
        Set<ViewPlugin<? extends A>> pluginsSet = plugins.keySet();
        for (ViewPlugin<? extends A> plugin : pluginsSet)
            if (plugins.get(plugin).isInstance(dataSource)) compatiblePlugins.add((ViewPlugin<X>)plugin);
        
        // TODO: sort plugins in some stable way (classname)
        
        return compatiblePlugins;
    }

}
