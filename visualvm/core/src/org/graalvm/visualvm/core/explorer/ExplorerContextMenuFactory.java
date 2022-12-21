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

package org.graalvm.visualvm.core.explorer;

import org.graalvm.visualvm.core.datasource.DataSource;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import org.openide.awt.Mnemonics;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.actions.Presenter;

/**
 * Class responsible for building explorer context menu.
 *
 * @author Jiri Sedlacek
 */
final class ExplorerContextMenuFactory {

    private static final Logger LOGGER = Logger.getLogger(ExplorerContextMenuFactory.class.getName());
    private static final String SELECTION_ACTIONS_FILE = "VisualVM/ExplorerPopupSelection"; // NOI18N
    private static final String NOSELECTION_ACTIONS_FILE = "VisualVM/ExplorerPopupNoSelection"; // NOI18N

    private static ExplorerContextMenuFactory sharedInstance;


    /**
     * Returns singleton instance of ExplorerContextMenuFactory.
     * 
     * @return singleton instance of ExplorerContextMenuFactory.
     */
    static synchronized ExplorerContextMenuFactory instance() {
        if (sharedInstance == null) sharedInstance = new ExplorerContextMenuFactory();
        return sharedInstance;
    }


    JPopupMenu createPopupMenu() {
        // Get actions for the node
        List<Action>[] actionsArray = getActions();
        List<Action> defaultActions = actionsArray[0];
        List<Action> actions = actionsArray[1];

        // Return if there are no actions to display
        if (defaultActions.isEmpty() && actions.isEmpty()) return null;

        // Create a popup menu
        JPopupMenu popupMenu = new JPopupMenu();

        // Insert default actions
        boolean realDefaultAction = true;
        if (!defaultActions.isEmpty()) {
            for (Action defaultAction : defaultActions) {
                JMenuItem defaultItem = new DataSourceItem(defaultAction);
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
                else popupMenu.add(createItem(action));
            }
        }
        
        return popupMenu;
    }
    
    
    Action getDefaultActionFor(Set<DataSource> dataSources) {
        if (dataSources.isEmpty()) return null;
        List<Action> defaultActions = getActions()[0];
        return defaultActions.isEmpty() ? null : defaultActions.get(0);
    }
        
    private List<Action>[] getActions() {
        if (ExplorerSupport.sharedInstance().getSelectedDataSources().isEmpty())
            return getNoSelectionActions();
        else
            return getSelectionActions();
    }
        
    private List<Action>[] getSelectionActions() {
        // Find entrypoint into layer
        FileObject actionsFO = FileUtil.getConfigFile(SELECTION_ACTIONS_FILE);
        return getActions(actionsFO, true);
    }
        
    private List<Action>[] getNoSelectionActions() {
        // Find entrypoint into layer
        FileObject actionsFO = FileUtil.getConfigFile(NOSELECTION_ACTIONS_FILE);
        return getActions(actionsFO, false);
    }
    
    private List<Action>[] getActions(FileObject actionsFO, boolean allowDefaultActions) {
        // Init caches for default and regular context menu actions
        List<Action> defaultActions = new ArrayList<>();
        List<Action> actions = new ArrayList<>();
        
        if (actionsFO != null) {
            
            DataFolder actionsDF = DataFolder.findFolder(actionsFO);
            DataObject[] menuItems = actionsDF.getChildren();
            
            for (DataObject menuItemDO : menuItems) {
                
                FileObject fobj = menuItemDO.getPrimaryFile();
                
                if (fobj.isFolder()) {
                    LOGGER.log(Level.WARNING, "Nested menus not supported for Applications context menu: " + fobj, fobj);   // NOI18N
                } else {
                    InstanceCookie menuItemCookie = (InstanceCookie)menuItemDO.getCookie(InstanceCookie.class);
                    try {
                        Object menuItem = menuItemCookie.instanceCreate();
                        
                        boolean isDefaultAction = false;
                        Object isDefaultActionObj = fobj.getAttribute("default");   // NOI18N
                        if (isDefaultActionObj != null) try {
                            isDefaultAction = (Boolean)isDefaultActionObj;
                            if (!allowDefaultActions && isDefaultAction)
                                LOGGER.log(Level.WARNING, "Default actions not supported for " + actionsFO.getPath() + ": " + menuItem, menuItem);  // NOI18N
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Cannot determine whether context menu action is default: " + isDefaultActionObj, isDefaultActionObj);    // NOI18N
                        }
        
                        List<Action> actionsList = isDefaultAction ? defaultActions : actions;
        
                        if (menuItem instanceof Action) {
                            Action action = (Action)menuItem;
                            if (action.isEnabled()) actionsList.add(action);
                        } else if (menuItem instanceof JSeparator) {
                            if (isDefaultAction) {
                                LOGGER.log(Level.WARNING, "Separator cannot be added to default actions " + menuItem, menuItem);    // NOI18N
                            } else {
                                actionsList.add(null);
                            }
                        } else {
                            LOGGER.log(Level.WARNING, "Unsupported context menu item: " + menuItem, menuItem);  // NOI18N
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Unable to resolve context menu action: " + menuItemDO, menuItemDO);   // NOI18N
                    }
                }
            }
        
        }
        
        // Return actions
        return new List[] { cleanupActions(defaultActions), cleanupActions(actions) };
    }
    
    
    private List<Action> cleanupActions(List<Action> actions) {
        boolean leadingNull = true;
        Action lastAction = null;
        List<Action> cleanActions = new ArrayList<>();
        
        for (Action action : actions) {
            if (action == null) {
                if (!leadingNull && lastAction != null)
                    cleanActions.add(null);
            } else {
                cleanActions.add(action);
                leadingNull = false;
            }
            lastAction = action;
        }
    
        if (!cleanActions.isEmpty()) {
            int lastItemIndex = cleanActions.size() - 1;
            Action lastCleanAction = cleanActions.get(lastItemIndex);
            if (lastCleanAction == null) cleanActions.remove(lastItemIndex);
        }
    
        return cleanActions;
    }
    
    
    private static JMenuItem createItem(Action action) {
        if (action instanceof Presenter.Popup) return ((Presenter.Popup)action).getPopupPresenter();
        else return new DataSourceItem(action);
    }
    
    
    private static class DataSourceItem extends JMenuItem {
        DataSourceItem(Action action) {
            super(action);
            setIcon(null);
            setToolTipText(null);
            String name = (String)action.getValue(Action.NAME);
            if (name != null) Mnemonics.setLocalizedText(this, name); // NOI18N
        }
    }    
}
