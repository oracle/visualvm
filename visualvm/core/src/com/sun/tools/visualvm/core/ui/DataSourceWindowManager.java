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
import com.sun.tools.visualvm.core.explorer.ExplorerModelSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;

/**
 * Class managing Windows (windows, TopComponents) of DataSource instances.
 *
 * @author Jiri Sedlacek
 */
// TODO: don't force the user to call these methods in EDT, use SwingUtilities.invokeLater when neccessary
public class DataSourceWindowManager {
    
    private static final RequestProcessor processor = new RequestProcessor("DataSourceWindowManager Processor");

    private static DataSourceWindowManager sharedInstance;

    private final Map<DataSource, DataSourceWindow> openedWindows = Collections.synchronizedMap(new HashMap());


    /**
     * Returns singleton instance of DataSourceWindowManager.
     * 
     * @return singleton instance of DataSourceWindowManager.
     */
    public static synchronized DataSourceWindowManager sharedInstance() {
        if (sharedInstance == null) sharedInstance = new DataSourceWindowManager();
        return sharedInstance;
    }
    
    
    /**
     * Opens Window for given DataSource or selects it if already opened.
     * 
     * @param dataSource DataSource to open the Window for.
     */
    public void openWindow(DataSource dataSource) {
        openWindow(dataSource, true, true);
    }
    
    /**
     * Opens Window for given DataSource or selects it if already opened.
     * 
     * @param dataSource DataSource to open the Window for,
     * @param select true if the opened Window should be made visible,
     * @param toFront true if the opened Window should be moved to front of all other Windows.
     */
    public void openWindow(final DataSource dataSource, final boolean select, final boolean toFront) {
        processor.post(new Runnable() {
            public void run() {
                DataSourceWindow window = getOpenedWindow(dataSource);
                if (window == null) {
                    ProgressHandle pHandle = null;
                    try {
                        // Setup progress bar
                        pHandle = ProgressHandleFactory.createHandle("Opening " + ExplorerModelSupport.sharedInstance().getNodeFor(dataSource).getName() + "...");
                        pHandle.setInitialDelay(0);
                        pHandle.start();
                        
                        // Create the window
                        window = createNewWindow(dataSource);
                        } finally {
                            final ProgressHandle pHandleF = pHandle;
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() { if (pHandleF != null) pHandleF.finish(); }
                            });
                        }
                    if (window != null) displayWindow(window, null, true, select, toFront);
                } else {
                    displayWindow(window, null, false, select, toFront);
                }
            }
        });
    }
    
    public void selectWindow(DataSource dataSource) {
        selectWindow(dataSource, true);
    }
    
    /**
     * Selects an already opened Window of given DataSource.
     * Throws a runtime exception if the Window isn't open.
     * 
     * @param dataSource DataSource to select the Window for,
     * @param toFront true if the selected Window should be moved to front of all other Windows.
     */
    public void selectWindow(final DataSource dataSource, final boolean toFront) {
        processor.post(new Runnable() {
            public void run() {
                DataSourceWindow window = getOpenedWindow(dataSource);
                if (window != null) displayWindow(window, null, true, true, toFront);
            }
        });
    }
    
    /**
     * Returns true if the Window of given DataSource is currently open, false otherwise.
     * 
     * @param dataSource DataSource to check the Window for,
     * @return true if the Window of given DataSource is currently open, false otherwise.
     */
    public boolean isWindowOpened(DataSource dataSource) {
        return getOpenedWindow(dataSource) != null;
    }
    
    
    /**
     * Adds an additional view to the Window of given DataSource or selects it if already added.
     * 
     * @param owner DataSource to add the view to,
     * @param view DataSourceView to be added.
     */
    // NOTE: display own progress...
    public void addView(DataSource owner, DataSourceView view) {
        addViews(owner, Collections.singletonList(view), true, true, true);
    }
    
    /**
     * Adds additional views to a DataSource to the Window of viewMaster DataSource or selects it if already added.
     * 
     * @param owner viewMaster of the DataSource,
     * @param dataSource DataSource to add the views for.
     */
    // NOTE: display own progress...
    public void addViews(DataSource owner, DataSource dataSource) {
        addViews(owner, DataSourceWindowFactory.sharedInstance().getViews(dataSource), true, true, true);
    }
    
    /**
     * Adds additional views to a DataSource to the Window of viewMaster DataSource or selects it if already added.
     * 
     * @param owner viewMaster of the DataSource,
     * @param dataSourceViews DataSourceView instances to be added,
     * @param selectWindow true if the opened Window should be made visible,
     * @param toFront true if the opened Window should be moved to front of all other Windows,
     * @param selectLastView true if the added view should be selected within the Window.
     */
    // NOTE: display own progress...
    public void addViews(final DataSource owner, final List<? extends DataSourceView> views, final boolean selectWindow, final boolean toFront, final boolean selectFirstView) {
        processor.post(new Runnable() {
            public void run() {
                DataSourceWindow window = getOpenedWindow(owner);
                boolean wasOpened = window != null;
                if (!wasOpened) window = createNewWindow(owner);
                if (window == null) return;
                
                for (DataSourceView dataSourceView : views)
                    if (!window.containsView(dataSourceView)) window.addView(dataSourceView);
                
                DataSourceView view = selectFirstView ? views.iterator().next() : null;
                displayWindow(window, view, !wasOpened, selectWindow, toFront);
            }
        });
    }
    
    public void selectView(DataSource owner, DataSourceView dataSourceView) {
        selectView(owner, dataSourceView, true, true);
    }
    
    /**
     * Selects already opened view in already opened DataSource Window.
     * 
     * @param dataSource DataSource to select the view for,
     * @param dataSourceView DataSourceView to select,
     * @param selectWindow true if the opened Window should be made visible,
     * @param toFront true if the opened Window should be moved to front of all other Windows,
     */
    public void selectView(final DataSource owner, final DataSourceView view, final boolean selectWindow, final boolean toFront) {
        processor.post(new Runnable() {
            public void run() {
                DataSourceWindow window = getOpenedWindow(owner);
                if (window == null) return;
                displayWindow(window, view, false, selectWindow, toFront);
            }
        });
    }
    
    /**
     * Removes a view from a DataSource Window.
     * 
     * @param dataSource DataSource from which to remove the Window,
     * @param dataSourceView DataSourceView to remove.
     */
    public void removeView(final DataSource dataSource, final DataSourceView dataSourceView) {
        final DataSourceWindow window = getOpenedWindow(dataSource);
        if (window == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                window.removeView(dataSourceView);
            }
        });
    }
    
    /**
     * Returns true if the Window of given DataSource contains the view, false otherwise.
     * 
     * @param dataSource DataSource to check the view for,
     * @param dataSourceView DataSourceView to check,
     * @return true if the Window of given DataSource contains the view, false otherwise.
     */
    public boolean containsView(DataSource dataSource, DataSourceView dataSourceView) {
        // NOTE: Should be called from EDT!!!
        DataSourceWindow window = getOpenedWindow(dataSource);
        if (window == null) return false;
        return window.containsView(dataSourceView);
    }
    
    
    private void displayWindow(final DataSourceWindow window, final DataSourceView view, final boolean open, final boolean select, final boolean toFront) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (view != null) window.selectView(view);
                if (open) window.open();
                if (select) window.requestActive();
                if (toFront) window.toFront();
            }
        });
    }
    
    // Returns opened or created window
    private DataSourceWindow createNewWindow(DataSource dataSource) {
        DataSourceWindow window = DataSourceWindowFactory.sharedInstance().createWindowFor(dataSource);
        if (window != null) registerOpenedWindow(window, dataSource);
        else System.err.println("Cannot create window for " + dataSource);
        return window;
    }
    
    // Returns window from cache of opened windows or null if not cached
    private DataSourceWindow getOpenedWindow(DataSource dataSource) {
        return openedWindows.get(dataSource);
    }
    
    private DataSource getDataSourceFor(DataSourceWindow dataSourceWindow) {
        Set<Map.Entry<DataSource, DataSourceWindow>> entries = openedWindows.entrySet();
        for (Map.Entry<DataSource, DataSourceWindow> entry : entries)
            if (entry.getValue().equals(dataSourceWindow)) return entry.getKey();
        return null;
    }
    
    private void registerOpenedWindow(DataSourceWindow window, DataSource dataSource) {
        openedWindows.put(dataSource, window);
    }
    
    void unregisterClosedWindow(final DataSourceWindow window) {
        DataSource dataSource = getDataSourceFor(window);
        openedWindows.remove(dataSource);
        
        final Set<DataSourceView> views = window.getViews();
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                for (DataSourceView view : views) view.removed();
            }
        });
    }
    
    
    private DataSourceWindowManager() {}

}
