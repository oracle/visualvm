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

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.datasupport.Utils;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Class responsible for building explorer context menu.
 *
 * @author Jiri Sedlacek
 */
public final class ExplorerContextMenuFactory {

    private static ExplorerContextMenuFactory sharedInstance;

    private final Map<ExplorerActionsProvider, Class<? extends DataSource>> providers = Collections.synchronizedMap(new HashMap());


    /**
     * Returns singleton instance of ExplorerContextMenuFactory.
     * 
     * @return singleton instance of ExplorerContextMenuFactory.
     */
    public static synchronized ExplorerContextMenuFactory sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ExplorerContextMenuFactory();
        return sharedInstance;
    }


    /**
     * Adds a new ExplorerActionsProvider for DataSource instances compatible with given scope.
     * 
     * @param provider ExplorerActionsProvider to be added,
     * @param scope scope of ExplorerNode instances for which the ExplorerActionsProvider provides the actions.
     */
    public void addExplorerActionsProvider(ExplorerActionsProvider provider, Class<? extends DataSource> scope) {
        providers.put(provider, scope);
    }
    
    /**
     * Removes a ExplorerActionsProvider.
     * 
     * @param provider ExplorerActionsProvider to be removed.
     */
    public void removeExplorerActionsProvider(ExplorerActionsProvider provider) {
        providers.remove(provider);
    }
    
    
    JPopupMenu createPopupMenuFor(Set<DataSource> dataSources) {
        // Get actions for the node
        List<Action>[] actionsArray = getActions(dataSources);
        List<Action> defaultActions = actionsArray[0];
        List<Action> actions = actionsArray[1];

        // Return if there are no actions to display
        if (defaultActions.size() == 0 && actions.size() == 0) return null;

        // Create a popup menu
        JPopupMenu popupMenu = new JPopupMenu();

        // Insert default actions
        boolean realDefaultAction = true;
        if (!defaultActions.isEmpty()) {
            for (Action defaultAction : defaultActions) {
                JMenuItem defaultItem = new DataSourceItem(dataSources, defaultAction);
                if (realDefaultAction) {
                    defaultItem.setFont(defaultItem.getFont().deriveFont(Font.BOLD));
                    realDefaultAction = false;
                }
                popupMenu.add(defaultItem);
            }
        }

        // Insert separator between default action and other actions
        if (!defaultActions.isEmpty() && !actions.isEmpty()) popupMenu.addSeparator();

        // Insert other actions
        if (!actions.isEmpty()) {
            for (Action action : actions) {
                if (action == null) popupMenu.addSeparator();
                else popupMenu.add(new DataSourceItem(dataSources, action));
            }
        }
        
        return popupMenu;
    }
    
    
    Action getDefaultActionFor(Set<DataSource> dataSources) {
        if (dataSources.isEmpty()) return null;
        
        Set<ExplorerActionsProvider> filteredProviders = getProvidersFor(dataSources);
        List<ExplorerActionDescriptor> defaultActionsDescriptors = new ArrayList();
        
        // Create lists of ExplorerActionDescriptors
        for (ExplorerActionsProvider provider : filteredProviders) {
            ExplorerActionDescriptor defaultAction = provider.getDefaultAction(dataSources);
            if (defaultAction != null) defaultActionsDescriptors.add(defaultAction);
        }
        
        // Sort ExplorerActionDescriptors according to actionOrder
        Collections.sort(defaultActionsDescriptors, Positionable.COMPARATOR);
        
        return defaultActionsDescriptors.isEmpty() ? null : defaultActionsDescriptors.get(0).getAction();
    }
    
    private List<Action>[] getActions(Set<DataSource> dataSources) {
        Set<ExplorerActionsProvider> filteredProviders = getProvidersFor(dataSources);
        List<ExplorerActionDescriptor> defaultActionsDescriptors = new ArrayList();
        List<ExplorerActionDescriptor> actionsDescriptors = new ArrayList();
        
        // Create lists of ExplorerActionDescriptors
        for (ExplorerActionsProvider provider : filteredProviders) {
            ExplorerActionDescriptor defaultAction = provider.getDefaultAction(dataSources);
            if (defaultAction != null) defaultActionsDescriptors.add(defaultAction);
            actionsDescriptors.addAll(provider.getActions(dataSources));
        }
        
        // Sort ExplorerActionDescriptors according to actionOrder
        Collections.sort(defaultActionsDescriptors, Positionable.COMPARATOR);
        Collections.sort(actionsDescriptors, Positionable.COMPARATOR);
        
        // Create sorted lists of actions
        List<Action> defaultActions = new ArrayList(defaultActionsDescriptors.size());
        for (ExplorerActionDescriptor defaultActionDescriptor : defaultActionsDescriptors) {
            Action defaultAction = defaultActionDescriptor.getAction();
            if (defaultAction.isEnabled()) defaultActions.add(defaultAction);
        }
        
        List<Action> actions = new ArrayList(actionsDescriptors.size());
        for (ExplorerActionDescriptor actionDescriptor : actionsDescriptors) {
            Action action = actionDescriptor.getAction();
            if (action == null || action.isEnabled()) actions.add(action);
        }
        
        return new List[] { defaultActions, actions };
    }
    
    
    private Set<ExplorerActionsProvider> getProvidersFor(Set<DataSource> dataSources) {
        Map<ExplorerActionsProvider, Class<? extends DataSource>> currentProviders = new HashMap(providers);
        Set<ExplorerActionsProvider> currentProvidersSet = currentProviders.keySet();
        Set<ExplorerActionsProvider> filteredProviders = new HashSet();
        for (ExplorerActionsProvider provider : currentProvidersSet)
            if (Utils.getFilteredSet(dataSources, currentProviders.get(provider)).size() == dataSources.size())
                filteredProviders.add(provider);
        return filteredProviders;
    }
    
    
    private ExplorerContextMenuFactory() {
    }
    
    
    private static class DataSourceItem extends JMenuItem {
        
        private Set<DataSource> dataSources;
        
        public DataSourceItem(Set<DataSource> dataSources, Action action) {
            super(action);
            setIcon(null);
            setToolTipText(null);
            
            this.dataSources = dataSources;
        }
        
        protected void fireActionPerformed(ActionEvent event) {
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            ActionEvent e = null;
            // Process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==ActionListener.class) {
                    // Lazily create the event:
                    if (e == null) {
                          String actionCommand = event.getActionCommand();
                          if(actionCommand == null) {
                             actionCommand = getActionCommand();
                          }
                          e = new ActionEvent(dataSources,
                                              ActionEvent.ACTION_PERFORMED,
                                              actionCommand,
                                              event.getWhen(),
                                              event.getModifiers());
                    }
                    ((ActionListener)listeners[i+1]).actionPerformed(e);
                }          
            }
        }
        
    }

}
