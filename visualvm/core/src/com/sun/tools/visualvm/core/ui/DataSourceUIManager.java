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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class managing UIs (windows, TopComponents) of DataSource instances.
 *
 * @author Jiri Sedlacek
 */
// TODO: needs to be properly synchronized (use RequestProcessors)!
// TODO: don't force the user to call these methods in EDT, use SwingUtilities.invokeLater when neccessary
public class DataSourceUIManager {

    private static DataSourceUIManager sharedInstance;

    private final Map<DataSource, DataSourceUI> openedWindows = Collections.synchronizedMap(new HashMap());
    // TODO: implement some better data structure for cheaper listeners query
    private final Map<DataSourceUIListener, Class<? extends DataSource>> uiListeners = Collections.synchronizedMap(new HashMap());


    /**
     * Returns singleton instance of DataSourceUIManager.
     * 
     * @return singleton instance of DataSourceUIManager.
     */
    public static synchronized DataSourceUIManager sharedInstance() {
        if (sharedInstance == null) sharedInstance = new DataSourceUIManager();
        return sharedInstance;
    }
    
    
    /**
     * Opens UI for given DataSource or selects it if already opened.
     * 
     * @param dataSource DataSource to open the UI for.
     */
    public void openWindow(DataSource dataSource) {
        openWindow(dataSource, true, true);
    }
    
    /**
     * Opens UI for given DataSource or selects it if already opened.
     * 
     * @param dataSource DataSource to open the UI for,
     * @param select true if the opened UI should be made visible,
     * @param toFront true if the opened UI should be moved to front of all other UIs.
     */
    public void openWindow(DataSource dataSource, boolean select, boolean toFront) {
        DataSourceUI window = getWindowFor(dataSource);
        if (window == null) {
            fireUIWillOpen(dataSource);
            window = getWindow(dataSource);
            window.open();
            registerOpenedWindow(window, dataSource);
            fireUIOpened(dataSource);
        }
        if (select) window.requestVisible();
        if (toFront) window.toFront();
    }
    
    /**
     * Selects an already opened UI of given DataSource.
     * Throws a runtime exception if the UI isn't open.
     * 
     * @param dataSource DataSource to select the UI for,
     * @param toFront true if the selected UI should be moved to front of all other UIs.
     */
    public void selectOpenedWindow(DataSource dataSource, boolean toFront) {
        DataSourceUI window = getOpenedWindow(dataSource);
        window.requestVisible();
        if (toFront) window.toFront();
    }
    
    /**
     * Returns true if the UI of given DataSource is currently open, false otherwise.
     * 
     * @param dataSource DataSource to check the UI for,
     * @return true if the UI of given DataSource is currently open, false otherwise.
     */
    public boolean isWindowOpened(DataSource dataSource) {
        return getWindowFor(dataSource) != null;
    }
    
    /**
     * Adds an additional view to the UI of given DataSource or selects it if already added.
     * 
     * @param owner DataSource to add the view to,
     * @param view DataSourceView to be added.
     */
    public void addView(DataSource owner, DataSourceView view) {
        addView(owner, view, true, true, true);
    }
    
    /**
     * Adds additional views to a DataSource to the UI of viewMaster DataSource or selects it if already added.
     * 
     * @param owner viewMaster of the DataSource,
     * @param dataSource DataSource to add the views for.
     */
    public void addView(DataSource owner, DataSource dataSource) {
        addViews(owner, DataSourceUIFactory.sharedInstance().getViews(dataSource), true, true, true);
    }
    
    /**
     * Adds additional view to a DataSource to the UI of viewMaster DataSource or selects it if already added.
     * 
     * @param owner viewMaster of the DataSource,
     * @param dataSourceView DataSourceView to be added,
     * @param selectWindow true if the opened UI should be made visible,
     * @param toFront true if the opened UI should be moved to front of all other UIs,
     * @param selectView true if the added view should be selected within the UI.
     */
    public void addView(DataSource owner, DataSourceView dataSourceView, boolean selectWindow, boolean toFront, boolean selectView) {
        addViews(owner, Collections.singletonList(dataSourceView), selectWindow, toFront, selectView);
    }
    
    /**
     * Adds additional views to a DataSource to the UI of viewMaster DataSource or selects it if already added.
     * 
     * @param owner viewMaster of the DataSource,
     * @param dataSource DataSource to add the views for.
     * @param selectWindow true if the opened UI should be made visible,
     * @param toFront true if the opened UI should be moved to front of all other UIs,
     * @param selectView true if the added view should be selected within the UI.
     */
    public void addView(DataSource owner, DataSource dataSource, boolean selectWindow, boolean toFront, boolean selectView) {
        addViews(owner, DataSourceUIFactory.sharedInstance().getViews(dataSource), selectWindow, toFront, selectView);
    }
    
    /**
     * Adds additional views to a DataSource to the UI of viewMaster DataSource or selects it if already added.
     * 
     * @param owner viewMaster of the DataSource,
     * @param dataSourceViews DataSourceView instances to be added,
     * @param selectWindow true if the opened UI should be made visible,
     * @param toFront true if the opened UI should be moved to front of all other UIs,
     * @param selectLastView true if the added view should be selected within the UI.
     */
    public void addViews(DataSource owner, List<? extends DataSourceView> dataSourceViews, boolean selectWindow, boolean toFront, boolean selectLastView) {
        DataSourceUI window = getWindowFor(owner);
        if (window == null) {
            window = getWindow(owner);
            window.open();
            registerOpenedWindow(window, owner);
        }
        for (DataSourceView dataSourceView : dataSourceViews)
            if (!window.containsView(dataSourceView)) window.addView(dataSourceView);
        if (selectWindow) window.requestActive();
        if (toFront) window.toFront();
        if (selectLastView && !dataSourceViews.isEmpty()) window.selectView((DataSourceView)dataSourceViews.toArray()[dataSourceViews.size() - 1]);
    }
    
    /**
     * Selects already opened view in already opened DataSource UI.
     * 
     * @param dataSource DataSource to select the view for,
     * @param dataSourceView DataSourceView to select,
     * @param selectWindow true if the opened UI should be made visible,
     * @param toFront true if the opened UI should be moved to front of all other UIs,
     */
    public void selectView(DataSource dataSource, DataSourceView dataSourceView, boolean selectWindow, boolean toFront) {
        DataSourceUI window = getOpenedWindow(dataSource);
        if (selectWindow) window.requestActive();
        if (toFront) window.toFront();
        window.selectView(dataSourceView);
    }
    
    /**
     * Removes a view from a DataSource UI.
     * 
     * @param dataSource DataSource from which to remove the UI,
     * @param dataSourceView DataSourceView to remove.
     */
    public void removeView(final DataSource dataSource, final DataSourceView dataSourceView) {
        DataSourceUI window = getOpenedWindow(dataSource);
        window.removeView(dataSourceView);
    }
    
    /**
     * Returns true if the UI of given DataSource contains the view, false otherwise.
     * 
     * @param dataSource DataSource to check the view for,
     * @param dataSourceView DataSourceView to check,
     * @return true if the UI of given DataSource contains the view, false otherwise.
     */
    public boolean containsView(DataSource dataSource, DataSourceView dataSourceView) {
        return getOpenedWindow(dataSource).containsView(dataSourceView);
    }
    
    
    /**
     * Adds a DataSourceUIListener which will get UI state notifications for given scope of DataSources.
     * 
     * @param listener DataSourceUIListener to add,
     * @param scope scope of DataSource types for which the listeners registers.
     */
    public void addDataSourceUIListener(final DataSourceUIListener listener, final Class<? extends DataSource> scope) {
        uiListeners.put(listener, scope);
    }
    
    /**
     * Removes a DataSourceUIListener.
     * 
     * @param listener DataSourceUIListener to remove.
     */
    public void removeDataSourceUIListener(final DataSourceUIListener listener) {
        uiListeners.remove(listener);
    }
    
    
    private DataSourceUI getWindow(DataSource dataSource) {
        DataSourceUI window = getWindowFor(dataSource);
        if (window == null) window = DataSourceUIFactory.sharedInstance().createWindowFor(dataSource);
        if (window == null) throw new RuntimeException("Window for " + dataSource + " not available");
        return window;
    }
    
    private DataSourceUI getOpenedWindow(DataSource dataSource) {
        DataSourceUI window = getWindowFor(dataSource);
        if (window == null) throw new RuntimeException("Window for " + dataSource + " not opened");
        return window;
    }
    
    private DataSourceUI getWindowFor(DataSource dataSource) {
        return openedWindows.get(dataSource);
    }
    
    private DataSource getDataSourceFor(DataSourceUI dataSourceUI) {
        Set<Map.Entry<DataSource, DataSourceUI>> entries = openedWindows.entrySet();
        for (Map.Entry<DataSource, DataSourceUI> entry : entries)
            if (entry.getValue().equals(dataSourceUI)) return entry.getKey();
        return null;
    }
    
    private void registerOpenedWindow(DataSourceUI window, DataSource dataSource) {
        openedWindows.put(dataSource, window);
    }
    
    void unregisterClosedWindow(final DataSourceUI window) {
        DataSource dataSource = getDataSourceFor(window);
        openedWindows.remove(dataSource);
        fireUIClosed(dataSource);
    }
    
    
    private void fireUIWillOpen(DataSource dataSource) {
        Set<DataSourceUIListener> compatibleListeners = getCompatibleListeners(dataSource);
        for (DataSourceUIListener listener : compatibleListeners) listener.uiWillOpen(dataSource);
    }
    
    private void fireUIOpened(DataSource dataSource) {
        Set<DataSourceUIListener> compatibleListeners = getCompatibleListeners(dataSource);
        for (DataSourceUIListener listener : compatibleListeners) listener.uiOpened(dataSource);
    }
    
    private void fireUIClosed(DataSource dataSource) {
        Set<DataSourceUIListener> compatibleListeners = getCompatibleListeners(dataSource);
        for (DataSourceUIListener listener : compatibleListeners) listener.uiClosed(dataSource);
    }
    
    private Set<DataSourceUIListener> getCompatibleListeners(DataSource dataSource) {
        Set<DataSourceUIListener> compatibleListeners = new HashSet();
        Set<DataSourceUIListener> listenersSet = uiListeners.keySet();
        for (DataSourceUIListener listener : listenersSet)
            if (uiListeners.get(listener).isInstance(dataSource))
                compatibleListeners.add(listener);
        return compatibleListeners;
    }
    
    
    private DataSourceUIManager() {}

}
