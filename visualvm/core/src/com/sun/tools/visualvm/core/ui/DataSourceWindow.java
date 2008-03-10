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
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.awt.BorderLayout;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jiri Sedlacek
 */
class DataSourceWindow extends TopComponent implements PropertyChangeListener {

    private int viewsCount = 0;

    private DataSource dataSource;
    private DataSourceDescriptor dataSourceDescriptor;


    // Doesn't need to be called from EDT
    public DataSourceWindow(DataSource dataSource) {
        this.dataSource = dataSource;
        
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    initAppearance();
                    initComponents();
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to create window for " + dataSource);
        }
    }
    
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void addView(DataSourceView view) {
        tabbedContainer.addViewTab(dataSource, view);
        viewsCount++;
    }
    
    public void selectView(DataSourceView view) {
        int viewIndex = indexOf(view);
        if (viewIndex == -1) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this);
        else tabbedContainer.setSelectedIndex(viewIndex);
    }
    
    public void removeView(final DataSourceView view) {
        int viewIndex = indexOf(view);
        if (viewIndex == -1) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this);
        else tabbedContainer.removeTabAt(viewIndex);
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() { view.removed(); }
        });
            
        viewsCount--;
        if (viewsCount == 0 && isOpened()) close();
    }
    
    public void removeAllViews() {
        Set<DataSourceView> views = getViews();
        for (DataSourceView view : views) removeView(view);
    }
    
    public Set<DataSourceView> getViews() {
        return tabbedContainer.getViews();
    }
    
    public boolean containsView(DataSourceView view) {
        return indexOf(view) != -1;
    }
    
    
    private int indexOf(DataSourceView view) {
        return tabbedContainer.indexOfView(view);
    }
    
    
    protected final void componentClosed() {
        dataSourceDescriptor.removePropertyChangeListener(this);
        removeAllViews();
        DataSourceWindowManager.sharedInstance().unregisterClosedWindow(this);
    }
    
    
    public void propertyChange(final PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (DataSourceDescriptor.PROPERTY_NAME.equals(propertyName)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { setName((String)evt.getNewValue()); }
            });
        } else if (DataSourceDescriptor.PROPERTY_ICON.equals(propertyName)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { setIcon((Image)evt.getNewValue()); }
            });
        } else if ("close".equals(propertyName)) {
            removeView(tabbedContainer.getDataSourceView((DataSourceWindowTabbedPane.DataSourceViewContainer)evt.getNewValue()));
        }
    }
    
    
    private void initAppearance() {
        dataSourceDescriptor = DataSourceDescriptorFactory.getDescriptor(dataSource);
        
        dataSourceDescriptor.addPropertyChangeListener(this);
        
        setName(dataSourceDescriptor.getName());
        setIcon(dataSourceDescriptor.getIcon());
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());

        // tabbedContainer
        tabbedContainer = new DataSourceWindowTabbedPane();
        tabbedContainer.addPropertyChangeListener("close", this);
        add(tabbedContainer, BorderLayout.CENTER);
    }
    
    private DataSourceWindowTabbedPane tabbedContainer;
    
    
    public int getPersistenceType() { return TopComponent.PERSISTENCE_NEVER; }
    protected String preferredID() { return getClass().getName(); }

}
