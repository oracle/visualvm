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
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Class responsible for DataSourceViews manipulation.
 *
 * @author Jiri Sedlacek
 */
public final class DataSourceWindowManager {
    
    private static final RequestProcessor processor = new RequestProcessor("DataSourceWindowManager Processor");    // NOI18N
    private static final Logger LOGGER = Logger.getLogger(DataSourceWindowManager.class.getName());
    
    private static DataSourceWindowManager sharedInstance;

    private final Map<DataSource, DataSourceWindow> openedWindows = Collections.synchronizedMap(new HashMap());
    private final Map<DataSource, Set<DataSourceView>> openedViews = Collections.synchronizedMap(new HashMap());


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
     * Returns true if there is at least one provider providing at least one view for given DataSource, false otherwise.
     * 
     * @param dataSource DataSource to open.
     * @return true if there is at least one provider providing at least one view for given DataSource, false otherwise.
     */
    public boolean canOpenDataSource(DataSource dataSource) {
        return DataSourceViewsManager.sharedInstance().hasViewsFor(dataSource);
    }
    
    /**
     * Opens the DataSource.
     * 
     * @param dataSource DataSource to open.
     */
    public void openDataSource(final DataSource dataSource) {
        processor.post(new Runnable() {
            public void run() {
                openWindowAndSelectView(dataSource, null, true, true);
            }
        });
    }
    
    /**
     * Closes the DataSource.
     * 
     * @param dataSource DataSource to close.
     */
    public void closeDataSource(final DataSource dataSource) {
        processor.post(new Runnable() {
            public void run() {
                // Resolve viewmaster
                DataSource viewMaster = getViewMaster(dataSource);

                // Resolve cached window
                final DataSourceWindow window = openedWindows.get(viewMaster);
                if (window == null) return; // Window not opened
                
                if (dataSource == viewMaster) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            window.removeAllViews();
                        }
                    });
                } else {
                    // Remove all views of the dataSource
                    Set<DataSourceView> views = openedViews.get(dataSource);
                    if (views != null) {
                        final Set<DataSourceView> viewsF = new HashSet(views);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                for (DataSourceView view : viewsF)
                                    if (window.containsView(view)) window.removeView(view);
                            }
                        });
                    }
                }
            }
        });
    }
    
    /**
     * Opens the DataSource if needed and selects the DataSourceView.
     * 
     * @param view DataSourceView to select.
     */
    public void selectView(final DataSourceView view) {
        processor.post(new Runnable() {
            public void run() {
                openWindowAndSelectView(view.getDataSource(), view, true, true);
            }
        });
    }
    
    
    private void openWindowAndSelectView(DataSource dataSource, DataSourceView viewToSelect, final boolean selectWindow, final boolean windowToFront) {
        // Resolve viewmaster
        DataSource viewMaster = getViewMaster(dataSource);

        // Resolve cached window
        DataSourceWindow window = openedWindows.get(viewMaster);
        final boolean wasOpened = window != null;
        
        final ProgressHandle pHandle = !wasOpened ?
            ProgressHandleFactory.createHandle(NbBundle.getMessage(DataSourceWindowManager.class, "LBL_Opening",    // NOI18N
            DataSourceDescriptorFactory.getDescriptor(dataSource).getName())) : null;
        
        try {

            // Viewmaster's window not cached (opened), create
            if (!wasOpened) {
                // Setup progress
                pHandle.setInitialDelay(0);
                pHandle.start();
                
                window = new DataSourceWindow(viewMaster);
                openedWindows.put(viewMaster, window);

                List<? extends DataSourceView> views = DataSourceViewsManager.sharedInstance().getViews(viewMaster);
                addViews(window, views);
            }

            // Viewmaster opened, add views for the dataSource
            if (dataSource != viewMaster) {
                List<? extends DataSourceView> views = DataSourceViewsManager.sharedInstance().getViews(dataSource);
                addViews(window, views);
                if (viewToSelect == null && !views.isEmpty()) viewToSelect = views.get(0);
            }

            // Open window
            final DataSourceWindow windowF = window;
            final DataSourceView viewToSelectF = viewToSelect;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (viewToSelectF != null) {
                        if (windowF.containsView(viewToSelectF)) windowF.selectView(viewToSelectF);
                        else {
                            if (LOGGER.isLoggable(Level.WARNING)) {
                                LOGGER.warning("Tried to select not opened view " + viewToSelectF); // NOI18N
                            }
                        }
                    }
                    if (!wasOpened) windowF.open();
                    if (selectWindow) windowF.requestActive();
                    if (windowToFront) windowF.toFront();
                }
            });
        
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { if (pHandle != null) pHandle.finish(); }
            });
        }
    }
    
    
    private DataSource getViewMaster(DataSource dataSource) {
        DataSource master = dataSource.getMaster();
        while (master != null && master != DataSource.ROOT) {
            dataSource = master;
            master = dataSource.getMaster();
        }
        return dataSource;
    }
    
    
    private <X extends DataSourceView> void addViews(final DataSourceWindow window, final List<X> views) {
        // Compute views to add
        final List<X> newViews = new ArrayList();
        for (X view : views)
            if (!window.containsView(view))
                newViews.add(view);
        
        // Blocking notification that the view will be added
        for (DataSourceView view : newViews) {
            DataSource dataSource = view.getDataSource();
            Set<DataSourceView> cachedViews = openedViews.get(dataSource);
            if (cachedViews == null) {
                cachedViews = new HashSet();
                openedViews.put(dataSource, cachedViews);
            }
            cachedViews.add(view);
            
            view.viewWillBeAdded();
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // Blocking adding of views to the window
                    for (DataSourceView view : newViews) window.addView(view);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize views for " + window.getDataSource(), e);    // NOI18N
        }

        // Blocking notification that the view has been added
        for (DataSourceView view : newViews) view.viewAdded();
    }
    
    void unregisterClosedWindow(DataSourceWindow window) {
        openedWindows.remove(window.getDataSource());
    }
    
    void unregisterClosedView(DataSourceView view) {
        DataSource dataSource = view.getDataSource();
        Set<DataSourceView> views = openedViews.get(dataSource);
        if (views != null) {
            views.remove(view);
            if (views.isEmpty()) openedViews.remove(dataSource);
        } else if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning("Tried to unregister not opened view " + view);  // NOI18N
        }
    }
    
    
    private DataSourceWindowManager() {}

}
