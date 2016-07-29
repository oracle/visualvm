/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView.Alert;
import com.sun.tools.visualvm.uisupport.UISupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jiri Sedlacek
 */
class DataSourceWindow extends TopComponent implements PropertyChangeListener {

    private static final RequestProcessor PROCESSOR =
            new RequestProcessor("DataSourceWindow Processor", 5); // NOI18N
    
    private int viewsCount = 0;
    private DataSource dataSource;
    private DataSourceDescriptor dataSourceDescriptor;
    private DataSourceWindowTabbedPane.ViewContainer singleViewContainer;
    private JPanel multiViewContainer;
    private AlertListener alertListener;

    public DataSourceWindow(DataSource dataSource) {
        this.dataSource = dataSource;
        initAppearance();
        initComponents();
    }
    
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void addView(DataSourceView view) {
        if (viewsCount == 0) {
            singleViewContainer = new DataSourceWindowTabbedPane.ViewContainer(new DataSourceCaption(dataSource), view);
            add(singleViewContainer, BorderLayout.CENTER);
            doLayout();
            alertListener = new AlertListener();
        } else if (viewsCount == 1) {
            remove(singleViewContainer);

            add(multiViewContainer, BorderLayout.CENTER);
            tabbedContainer.addViewTab(dataSource, singleViewContainer.getView());
            tabbedContainer.addViewTab(dataSource, view);
// Use after switching to new implementation of DataSourceWindowTabbedPane
//            tabbedContainer.addView(dataSource, singleViewContainer.getView());
//            tabbedContainer.addView(dataSource, view);
            doLayout();
            singleViewContainer.getCaption().finish();
            singleViewContainer = null;
        } else {
            tabbedContainer.addViewTab(dataSource, view);
// Use after switching to new implementation of DataSourceWindowTabbedPane
//            tabbedContainer.addView(dataSource, view);
        }
        viewsCount++;
        view.addPropertyChangeListener(WeakListeners.propertyChange(alertListener,view));
    }
    
    public void selectView(DataSourceView view) {
        if (viewsCount > 1) {
            int viewIndex = indexOf(view);
            if (viewIndex == -1) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this);   // NOI18N
            else tabbedContainer.setSelectedIndex(viewIndex);
// Use after switching to new implementation of DataSourceWindowTabbedPane
//            else tabbedContainer.setViewIndex(viewIndex);
        }
    }
    
    public void removeView(final DataSourceView view) {
        if (viewsCount == 1) {
            if (view != singleViewContainer.getView()) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this); // NOI18N
            remove(singleViewContainer);
            singleViewContainer.getCaption().finish();
            singleViewContainer = null;
        } else {
            int viewIndex = indexOf(view);
            if (viewIndex == -1) throw new RuntimeException("View " + view + " not present in DataSourceWindow " + this);   // NOI18N
            else tabbedContainer.removeTabAt(viewIndex);
// Use after switching to new implementation of DataSourceWindowTabbedPane
//            else tabbedContainer.removeView(viewIndex);
            
            if (viewsCount == 2) {
                singleViewContainer = new DataSourceWindowTabbedPane.ViewContainer(new DataSourceCaption(dataSource), tabbedContainer.getViews().iterator().next());
                remove(multiViewContainer);
                tabbedContainer.removeTabAt(0);
// Use after switching to new implementation of DataSourceWindowTabbedPane
//                tabbedContainer.removeView(0);
                add(singleViewContainer, BorderLayout.CENTER);
                doLayout();
            }
        }
        
        PROCESSOR.post(new Runnable() {
            public void run() { view.viewRemoved(); }
        });
        
        DataSourceWindowManager.sharedInstance().unregisterClosedView(view);
        viewsCount--;
        if (viewsCount == 0 && isOpened()) close();
    }
    
    public void removeAllViews() {
        Set<DataSourceView> views = getViews();
        for (DataSourceView view : views) removeView(view);
    }
    
    public Set<DataSourceView> getViews() {
        if (viewsCount == 1) {
            return Collections.singleton(singleViewContainer.getView());
        } else {
            return tabbedContainer.getViews();
        }
    }
    
    public boolean containsView(DataSourceView view) {
        return indexOf(view) != -1;
    }
    
    
    private int indexOf(DataSourceView view) {
        if (viewsCount == 1) {
            return view == singleViewContainer.getView() ? 0 : -1;
        } else {
            return tabbedContainer.indexOfView(view);
        }
    }


    protected final void componentActivated() {
        super.componentActivated();
        if (singleViewContainer != null) singleViewContainer.requestFocusInWindow();
        else if (getComponentCount() > 0) getComponent(0).requestFocusInWindow();
    }
    
    protected final void componentClosed() {
        dataSourceDescriptor.removePropertyChangeListener(this);
        removeAllViews();
        DataSourceWindowManager.sharedInstance().unregisterClosedWindow(this);
        super.componentClosed();
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
        } else if (DataSourceWindowTabbedPane.PROP_CLOSE.equals(propertyName)) {
            removeView(tabbedContainer.getDataSourceView((DataSourceWindowTabbedPane.ViewContainer)evt.getNewValue()));
// Use after switching to new implementation of DataSourceWindowTabbedPane
//        } else if (tabbedContainer.isCloseEvent(evt)) {
//            removeView(tabbedContainer.getView((DataSourceWindowTabbedPane.ViewContainer)evt.getNewValue()));
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
        tabbedContainer.addPropertyChangeListener(DataSourceWindowTabbedPane.PROP_CLOSE, this);
// Use after switching to new implementation of DataSourceWindowTabbedPane
//        tabbedContainer.addCloseListener(this);

        // multiViewContainer
        multiViewContainer = new JPanel(new BorderLayout());
        if (UISupport.isAquaLookAndFeel()) {
            multiViewContainer.setOpaque(true);
            multiViewContainer.setBackground(UISupport.getDefaultBackground());
        }
        multiViewContainer.add(tabbedContainer, BorderLayout.CENTER);

        add(multiViewContainer, BorderLayout.CENTER);
    }
    
    private void setAlert(DataSourceView view, Alert alert) {
        int viewIndex = tabbedContainer.indexOfView(view);
        
        tabbedContainer.setBackgroundAt(viewIndex,getAlertColor(alert));
// Use after switching to new implementation of DataSourceWindowTabbedPane
//        tabbedContainer.setViewBackground(viewIndex,getAlertColor(alert));
        if (alert != Alert.OK) {
            requestAttention(false);
        } else if (getApplicationAlert(alert) == Alert.OK) {
            cancelRequestAttention();
        }
    }

    private Color getAlertColor(final Alert alert) {
        Color color = null;
        
        switch (alert) {
            case ERROR: 
                color = Color.RED;
                break;
            case WARNING:
                color = Color.YELLOW;
                break;
            case OK:
                color = null;
                break;
        }
        return color;
    }

    private Alert getApplicationAlert(Alert alert) {
        if (alert == Alert.ERROR) {
            return alert;
        }
        for (DataSourceView view : getViews()) {
            Alert a = view.getAlert();
            if (a == Alert.ERROR) {
                return a;
            }
            if (a == Alert.WARNING) {
                alert = a;
            }
        }
        return alert;
    }
    
    private DataSourceWindowTabbedPane tabbedContainer;
    
    
    public int getPersistenceType() { return TopComponent.PERSISTENCE_NEVER; }
    protected String preferredID() { return getClass().getName(); }

    private class AlertListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (DataSourceView.ALERT_PROP.equals(evt.getPropertyName())) {
                setAlert((DataSourceView) evt.getSource(), (Alert) evt.getNewValue());
            }
        }
    }
}
