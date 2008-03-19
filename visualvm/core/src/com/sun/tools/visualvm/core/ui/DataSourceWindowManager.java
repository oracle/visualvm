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
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
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
    
    
    public void openDataSource(final DataSource dataSource) {
        processor.post(new Runnable() {
            public void run() {
                openWindowAndSelectView(dataSource, null, true, true);
            }
        });
    }
    
    public void closeDataSource(final DataSource dataSource) {
        processor.post(new Runnable() {
            public void run() {
                // Resolve viewmaster
                DataSource viewMaster = getViewMaster(dataSource);

                // Resolve cached window
                final DataSourceWindow window = openedWindows.get(viewMaster);
                if (window == null) return; // Window not opened
                
                // Remove all views of the dataSource
                final List<? extends DataSourceView> views = DataSourceViewsFactory.sharedInstance().getViews(dataSource);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (DataSourceView view : views)
                            if (window.containsView(view)) window.removeView(view);
                    }
                });
            }
        });
    }
    
    public void selectView(final DataSource dataSource, final DataSourceView view) {
        processor.post(new Runnable() {
            public void run() {
                openWindowAndSelectView(dataSource, view, true, true);
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
            ProgressHandleFactory.createHandle("Opening " +
            DataSourceDescriptorFactory.getDescriptor(dataSource).getName() + "...") : null;
        
        try {

            // Viewmaster's window not cached (opened), create
            if (!wasOpened) {
                // Setup progress
                pHandle.setInitialDelay(0);
                pHandle.start();
                
                window = new DataSourceWindow(viewMaster);
                openedWindows.put(viewMaster, window);

                List<? extends DataSourceView> views = DataSourceViewsFactory.sharedInstance().getViews(viewMaster);
                addViews(window, views);
            }

            // Viewmaster opened, add views for the dataSource
            if (dataSource != viewMaster) {
                List<? extends DataSourceView> views = DataSourceViewsFactory.sharedInstance().getViews(dataSource);
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
                        else System.err.println("Tried to select not opened view " + viewToSelectF);
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
        for (DataSourceView view : newViews) view.willBeAdded();

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // Blocking adding of views to the window
                    for (DataSourceView view : newViews) window.addView(view);
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to initialize views for " + window.getDataSource());
            e.printStackTrace();
        }

        // Blocking notification that the view has been added
        for (DataSourceView view : newViews) view.added();
    }
    
    void unregisterClosedWindow(final DataSourceWindow window) {
        openedWindows.remove(window.getDataSource());
    }
    
    
    private DataSourceWindowManager() {}

}
