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
    
    
    JPopupMenu createPopupMenuFor(DataSource dataSource) {
        // Get actions for the node
        List<Action>[] actionsArray = getActions(dataSource);
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
                if (realDefaultAction) {
                    JMenuItem defaultItem = new DataSourceItem(dataSource, defaultAction);
                    defaultItem.setFont(defaultItem.getFont().deriveFont(Font.BOLD));
                    popupMenu.add(defaultItem);
                    realDefaultAction = false;
                } else {
                    popupMenu.add(defaultAction);
                }
            }
        }

        // Insert separator between default action and other actions
        if (!defaultActions.isEmpty() && !actions.isEmpty()) popupMenu.addSeparator();

        // Insert other actions
        if (!actions.isEmpty()) {
            for (Action action : actions) {
                if (action != null) popupMenu.add(new DataSourceItem(dataSource, action));
                else popupMenu.addSeparator();
            }
        }
        
        return popupMenu;
    }
    
    
    Action getDefaultActionFor(DataSource dataSource) {
        Set<ExplorerActionsProvider> filteredProviders = getProvidersFor(dataSource);
        List<ExplorerActionDescriptor> defaultActionsDescriptors = new ArrayList();
        
        // Create lists of ExplorerActionDescriptors
        for (ExplorerActionsProvider provider : filteredProviders) {
            ExplorerActionDescriptor defaultAction = provider.getDefaultAction(dataSource);
            if (defaultAction != null) defaultActionsDescriptors.add(defaultAction);
        }
        
        // Sort ExplorerActionDescriptors according to actionOrder
        Collections.sort(defaultActionsDescriptors);
        
        return defaultActionsDescriptors.isEmpty() ? null : defaultActionsDescriptors.get(0).getAction();
    }
    
    private List<Action>[] getActions(DataSource dataSource) {
        Set<ExplorerActionsProvider> filteredProviders = getProvidersFor(dataSource);
        List<ExplorerActionDescriptor> defaultActionsDescriptors = new ArrayList();
        List<ExplorerActionDescriptor> actionsDescriptors = new ArrayList();
        
        // Create lists of ExplorerActionDescriptors
        for (ExplorerActionsProvider provider : filteredProviders) {
            ExplorerActionDescriptor defaultAction = provider.getDefaultAction(dataSource);
            if (defaultAction != null) defaultActionsDescriptors.add(defaultAction);
            actionsDescriptors.addAll(provider.getActions(dataSource));
        }
        
        // Sort ExplorerActionDescriptors according to actionOrder
        Collections.sort(defaultActionsDescriptors);
        Collections.sort(actionsDescriptors);
        
        // Create sorted lists of actions
        List<Action> defaultActions = new ArrayList(defaultActionsDescriptors.size());
        for (ExplorerActionDescriptor defaultActionDescriptor : defaultActionsDescriptors) defaultActions.add(defaultActionDescriptor.getAction());
        
        List<Action> actions = new ArrayList(actionsDescriptors.size());
        for (ExplorerActionDescriptor actionDescriptor : actionsDescriptors) actions.add(actionDescriptor.getAction());
        
        return new List[] { defaultActions, actions };
    }
    
    
    private Set<ExplorerActionsProvider> getProvidersFor(DataSource dataSource) {
        Map<ExplorerActionsProvider, Class<? extends DataSource>> currentProviders = new HashMap(providers);
        Set<ExplorerActionsProvider> currentProvidersSet = currentProviders.keySet();
        Set<ExplorerActionsProvider> filteredProviders = new HashSet();
        for (ExplorerActionsProvider provider : currentProvidersSet)
            if (currentProviders.get(provider).isInstance(dataSource)) filteredProviders.add(provider);
        return filteredProviders;
    }
    
    
    private ExplorerContextMenuFactory() {
    }
    
    
    private static class DataSourceItem extends JMenuItem {
        
        private DataSource dataSource;
        
        public DataSourceItem(DataSource dataSource, Action action) {
            super(action);
            this.dataSource = dataSource;
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
                          e = new ActionEvent(dataSource,
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
