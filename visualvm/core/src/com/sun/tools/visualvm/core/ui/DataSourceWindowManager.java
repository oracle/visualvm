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
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import java.util.ArrayList;
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
 * Class managing DataSourceWindows (TopComponents).
 *
 * @author Jiri Sedlacek
 */
public final class DataSourceWindowManager {
    
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
                        pHandle = ProgressHandleFactory.createHandle("Opening " + DataSourceDescriptorFactory.getDescriptor(dataSource).getName() + "...");
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
    
    /**
     * Selects opened window of given DataSource.
     * 
     * @param dataSource DataSource to select the view for.
     */
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
     * @param master DataSource to add the view to,
     * @param view DataSourceView to be added.
     */
    // NOTE: display own progress...
    public void addView(DataSource master, DataSourceView view) {
        addViews(master, Collections.singletonList(view), true, true, true);
    }
    
    /**
     * Adds additional views to a DataSource to the Window of viewMaster DataSource or selects it if already added.
     * 
     * @param master viewMaster of the DataSource,
     * @param dataSource DataSource to add the views for.
     */
    // NOTE: display own progress...
    public void addViews(DataSource master, DataSource dataSource) {
        addViews(master, DataSourceWindowFactory.sharedInstance().getViews(dataSource), true, true, true);
    }
    
    /**
     * Adds additional views to a DataSource to the Window of viewMaster DataSource or selects it if already added.
     * 
     * @param master viewMaster of the DataSource,
     * @param views DataSourceView instances to be added,
     * @param selectWindow true if the opened Window should be made visible,
     * @param toFront true if the opened Window should be moved to front of all other Windows,
     * @param selectFirstView true if the added view should be selected within the Window.
     */
    // NOTE: display own progress...
    public <X extends DataSourceView> void addViews(final DataSource master, final List<X> views, final boolean selectWindow, final boolean toFront, final boolean selectFirstView) {
        processor.post(new Runnable() {
            public void run() {
                DataSourceWindow window = getOpenedWindow(master);
                boolean wasOpened = window != null;
                if (!wasOpened) window = createNewWindow(master);
                if (window == null) return;
                
                final List<X> newViews = new ArrayList();
                for (X view : views) if (!window.containsView(view)) newViews.add(view);
                
                for (X view : newViews) view.willBeAdded();
                try {
                    final DataSourceWindow windowF = window;
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() { for (X view : newViews) windowF.addView(view); }
                    });
                } catch (Exception e) {}
                for (X view : newViews) view.added();
                
                DataSourceView view = (selectFirstView && !views.isEmpty()) ? views.iterator().next() : null;
                displayWindow(window, view, !wasOpened, selectWindow, toFront);
            }
        });
    }
    
    /**
     * Select opened view in opened window of master DataSource.
     * 
     * @param master DataSource whose window contains the view to select,
     * @param view view to select.
     */
    public void selectView(DataSource master, DataSourceView view) {
        selectView(master, view, true, true);
    }
    
    /**
     * Selects already opened view in already opened DataSource Window.
     * 
     * @param master DataSource to select the view for,
     * @param view DataSourceView to select,
     * @param selectWindow true if the opened Window should be made visible,
     * @param toFront true if the opened Window should be moved to front of all other Windows,
     */
    public void selectView(final DataSource master, final DataSourceView view, final boolean selectWindow, final boolean toFront) {
        processor.post(new Runnable() {
            public void run() {
                DataSourceWindow window = getOpenedWindow(master);
                if (window == null) return;
                displayWindow(window, view, false, selectWindow, toFront);
            }
        });
    }
    
    /**
     * Removes a view from a DataSource Window.
     * 
     * @param master DataSource from which to remove the Window,
     * @param view DataSourceView to remove.
     */
    public void removeView(final DataSource master, final DataSourceView view) {
        final DataSourceWindow window = getOpenedWindow(master);
        if (window == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                window.removeView(view);
            }
        });
    }
    
    /**
     * Returns true if the Window of given DataSource contains the view, false otherwise.
     * 
     * @param master DataSource to check the view for,
     * @param view DataSourceView to check,
     * @return true if the Window of given DataSource contains the view, false otherwise.
     */
    public boolean containsView(DataSource master, DataSourceView view) {
        // NOTE: Should be called from EDT!!!
        DataSourceWindow window = getOpenedWindow(master);
        if (window == null) return false;
        return window.containsView(view);
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
        window.removeAllViews();
    }
    
    
    private DataSourceWindowManager() {}

}
