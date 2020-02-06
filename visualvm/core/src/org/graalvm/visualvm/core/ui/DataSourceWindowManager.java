/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
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
    
    private final Map<DataSource, List<DataSourceWindowListener>> windowListeners = new WeakHashMap();

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
     * Opens the DataSource and selects the view.
     * 
     * @param dataSource DataSource to open
     */
    public void openDataSource(final DataSource dataSource) {
        openDataSource(dataSource, true);
    }

    /**
     * Opens the DataSource and optionally selects the view.
     *
     * @param dataSource DataSource to open
     * @param selectView true if the view should be selected, false otherwise
     */
    public void openDataSource(final DataSource dataSource, final boolean selectView) {
        processor.post(new Runnable() {
            public void run() {
                openDataSource(dataSource, selectView, 0);
            }
        });
    }
    
    /**
     * Opens the DataSource and optionally selects the view.
     *
     * @param dataSource DataSource to open
     * @param selectView true if the view should be selected, false otherwise
     * @param viewIndex index of the view to select
     */
    public void openDataSource(final DataSource dataSource, final boolean selectView, final int viewIndex) {
        processor.post(new Runnable() {
            public void run() {
                openWindowAndAddView(dataSource, null, viewIndex, selectView, selectView, selectView);
            }
        });
    }
    
    /**
     * Checks whether the DataSource window is currently opened.
     *
     * @param dataSource DataSource to check
     * @return true if the DataSource window is currently opened, false otherwise.
     */
    public boolean isDataSourceOpened(DataSource dataSource) {
        return openedWindows.get(dataSource) != null;
    }
    
    public <D extends DataSource> void addWindowListener(D dataSource, DataSourceWindowListener<D> listener) {
        synchronized (windowListeners) {
            List<DataSourceWindowListener> listeners = windowListeners.get(dataSource);
            if (listeners == null) {
                listeners = new ArrayList();
                windowListeners.put(dataSource, listeners);
            }
            listeners.add(listener);
        }
    }
    
    public <D extends DataSource> void removeWindowListener(D dataSource, DataSourceWindowListener<D> listener) {
        synchronized (windowListeners) {
            List<DataSourceWindowListener> listeners = windowListeners.get(dataSource);
            if (listeners != null) listeners.remove(listener);
        }
    }
    
    private <D extends DataSource> void notifyWindowOpened(D dataSource) {
        synchronized (windowListeners) {
            List<DataSourceWindowListener> listeners = windowListeners.get(dataSource);
            if (listeners != null)
                for (DataSourceWindowListener listener : new ArrayList<DataSourceWindowListener>(listeners))
                    listener.windowOpened(dataSource);
        }
    }
    
    private <D extends DataSource> void notifyWindowClosed(D dataSource) {
        synchronized (windowListeners) {
            List<DataSourceWindowListener> listeners = windowListeners.get(dataSource);
            if (listeners != null)
                for (DataSourceWindowListener listener : new ArrayList<DataSourceWindowListener>(listeners))
                    listener.windowClosed(dataSource);
        }
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
                openWindowAndAddView(view.getDataSource(), view, 0, true, true, true);
            }
        });
    }
    
    
    private boolean reloadingView;
    
    private void openWindowAndAddView(DataSource dataSource, DataSourceView view, int viewIndex, final boolean selectView, final boolean selectWindow, final boolean windowToFront) {
        // Resolve viewmaster
        final DataSource viewMaster = getViewMaster(dataSource);

        // Resolve cached window
        final DataSourceWindow[] window = new DataSourceWindow[1];
        window[0] = openedWindows.get(viewMaster);
        final boolean wasOpened = window[0] != null;
        
        final ProgressHandle pHandle = !wasOpened || reloadingView ?
            ProgressHandleFactory.createHandle(NbBundle.getMessage(DataSourceWindowManager.class, "LBL_Opening",    // NOI18N
            DataSourceDescriptorFactory.getDescriptor(dataSource).getName())) : null;
        
        try {

            // Viewmaster's window not cached (opened), create
            if (!wasOpened || reloadingView) {
                // Setup progress
                pHandle.setInitialDelay(0);
                pHandle.start();
                
                if (!reloadingView) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                window[0] = new DataSourceWindow(viewMaster);
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.severe("Failed to create window for " + dataSource); // NOI18N
                    }
                    openedWindows.put(viewMaster, window[0]);
                } else {
                    reloadingView = false;
                }

                List<? extends DataSourceView> views = DataSourceViewsManager.sharedInstance().getViews(viewMaster);
                addViews(window[0], views);
            }

            // Viewmaster opened, add views for the dataSource
            if (dataSource != viewMaster) {
                List<? extends DataSourceView> views = DataSourceViewsManager.sharedInstance().getViews(dataSource);
                addViews(window[0], views);
                if (selectView && view == null && viewIndex >= 0) {
                    if (viewIndex >= views.size()) viewIndex = -1;
                    if (viewIndex != -1) view = views.get(viewIndex);
                }
            }

            // Resolve view to select
            if (selectView && view == null && viewIndex > 0) {
                List<DataSourceView> views = window[0].getViews();
                if (viewIndex >= views.size()) viewIndex = -1;
                if (viewIndex != -1) view = views.get(viewIndex);
            }
            
            // Open window
            final DataSourceView viewToSelectF = selectView ? view : null;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (viewToSelectF != null) {
                        if (window[0].containsView(viewToSelectF)) {
                            window[0].selectView(viewToSelectF);
                        } else {
                            if (LOGGER.isLoggable(Level.WARNING)) {
                                LOGGER.warning("Tried to select not opened view " + viewToSelectF); // NOI18N
                            }
                        }
                    }
                    if (!wasOpened) window[0].open();
                    if (selectWindow) window[0].requestActive();
                    if (windowToFront) window[0].toFront();
                    
                    if (!wasOpened) notifyWindowOpened(dataSource);
                }
            });
        
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { if (pHandle != null) pHandle.finish(); }
            });
        }
    }
    
    
    static DataSource getViewMaster(DataSource dataSource) {
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
            try {
                DataSource dataSource = view.getDataSource();
                Set<DataSourceView> cachedViews = openedViews.get(dataSource);
                if (cachedViews == null) {
                    cachedViews = new HashSet();
                    openedViews.put(dataSource, cachedViews);
                }
                cachedViews.add(view);

                view.viewWillBeAdded();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to pre-initialize view " + view, e);    // NOI18N
            }
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // Blocking adding of views to the window
                    for (DataSourceView view : newViews) {
                        try {
                            window.addView(view);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed to initialize view " + view, e);    // NOI18N
                        }
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize views for " + window.getDataSource(), e);    // NOI18N
        }

        // Blocking notification that the view has been added
        for (DataSourceView view : newViews) {
            try {
                view.viewAdded();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to post-initialize view " + view, e);    // NOI18N
            }
        }
    }
    
    
    void reopenDataSource(final DataSource dataSource) {
        processor.post(new Runnable() {
            public void run() {
                DataSource viewMaster = getViewMaster(dataSource);
                final DataSourceWindow window = viewMaster == null ? null : openedWindows.get(viewMaster);
                
                if (window == null) return;
                
                Set<DataSourceView> _views = openedViews.get(dataSource);
                if (_views == null) return;
                
                final Map<String, DataSourceView> oldViews = new HashMap();
                for (DataSourceView view : _views) oldViews.put(view.getName(), view);
                SwingUtilities.invokeLater(new Runnable () {
                    public void run() {
                        final Set<DataSourceView> opened = openedViews.get(dataSource);
                        for (DataSourceView view : oldViews.values()) {
                            window.clearView(view, processor);
                            opened.remove(view);
                        }
                        
                        processor.post(new Runnable() {
                            public void run() {
                                final List<? extends DataSourceView> newViews = DataSourceViewsManager.sharedInstance().getViews(dataSource);
                                for (DataSourceView view : newViews) {
                                    opened.add(view);
                                    try { view.viewWillBeAdded(); }
                                    catch (Exception e) { LOGGER.log(Level.SEVERE, "Failed to pre-initialize view " + view, e); } // NOI18N
                                    oldViews.remove(view.getName());
                                }
                                if (opened.isEmpty()) openedViews.remove(dataSource);
                                
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        for (DataSourceView view : oldViews.values()) window.closeUnregisteredView(view);
                                        for (int i = 0; i < newViews.size(); i++) window.updateView(newViews.get(i), i);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }
    
    
    void unregisterClosedWindow(DataSourceWindow window) {
        DataSource dataSource = window.getDataSource();
        openedWindows.remove(dataSource);
        notifyWindowClosed(dataSource);
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
