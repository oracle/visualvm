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
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.Image;

/**
 * Definition of a subtab representing a concrete view of a DataSource in DataSource Window.
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSourceView implements Positionable {

    private DataSource dataSource;
    private String name;
    private Image icon;
    private int preferredPosition;
    private boolean isClosable;
    private DataViewComponent component;
    private DataSourceViewProvider controller;


    /**
     * Creates new DataSourceView.
     * 
     * Order of the notifications/queries is as follows:
     * willBeAdded() -> getView() -> added() -> removed()
     * 
     * @param dataSource dataSource of the view
     * @param name name of the view as it appears in the subtab,
     * @param icon icon of the view as it appears in the subtab,
     * @param preferredPosition preferred position of the view among all other views for the DataSource,
     * @param isClosable true if the user is allowed to close the view, false otherwise.
     */
    public DataSourceView(DataSource dataSource, String name, Image icon, int preferredPosition, boolean isClosable) {
        if (dataSource == null) throw new IllegalArgumentException("DataSource cannot be null");    // NOI18N
        if (name == null) throw new IllegalArgumentException("Name cannot be null");    // NOI18N
        if (icon == null) throw new IllegalArgumentException("Icon cannot be null");    // NOI18N

        this.dataSource = dataSource;
        this.name = name;
        this.icon = icon;
        this.preferredPosition = preferredPosition;
        this.isClosable = isClosable;
    }


    /**
     * Returns dataSource of the view.
     * 
     * @return dataSource of the view.
     */
    public final DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns name of the view as it appears in the subtab.
     * 
     * @return name of the view as it appears in the subtab.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns icon of the view as it appears in the subtab.
     * 
     * @return icon of the view as it appears in the subtab.
     */
    public final Image getImage() {
        return icon;
    }
    
    /**
     * Returns DataViewComponent implementing the view.
     * Called from EDT.
     * 
     * @return DataViewComponent implementing the view.
     */
    protected abstract DataViewComponent createComponent();

    /**
     * Returns preferred position of the view among all other views for the DataSource.
     * 
     * @return preferred position of the view among all other views for the DataSource.
     */
    public final int getPreferredPosition() {
        return preferredPosition;
    }
  
    /**
     * Returns true if the user is allowed to close the view, false otherwise.
     * 
     * @return true if the user is allowed to close the view, false otherwise.
     */
    public final boolean isClosable() {
        return isClosable;
    }
    
    
    /**
     * Notification when the view is about to be added to DataSourceWindow.
     * This notification comes from a thread other than EDT and its main intention
     * is to provide a possibility to do some models inits before the actual UI is displayed.
     * This call is blocking (blocks opening the view, progress bar is shown) but long-running initializations should
     * still use separate thread and update the UI after the models are ready.
     */
    protected void willBeAdded() {
    }
    
    /**
     * Notification when the view has been added to DataSourceWindow.
     * This notification comes from a thread other than EDT.
     */
    protected void added() {
    }
    
    /**
     * Notification when the view has been either programatically removed from tabbed pane or closed by the user by clicking the X.
     * This notification comes from a thread other than EDT
     */
    protected void removed() {
    }
    
    
    DataViewComponent getView() {
        if (component == null) {
            component = createComponent();
            if (component == null) throw new NullPointerException("Null view component from " + this);  // NOI18N
            controller.processCreatedComponent(this, component);
        }
        return component;
    }
    
    void viewWillBeAdded() {
        willBeAdded();
        controller.viewWillBeAdded(this);
    }
    
    void viewAdded() {
        added();
        controller.viewAdded(this);
    }
    
    void viewRemoved() {
        removed();
        controller.viewRemoved(this);
    }
    
    void setController(DataSourceViewProvider controller) {
        this.controller = controller;
    }

}
